const express = require('express');
const { v4: uuidv4 } = require('uuid');
const winston = require('winston');
const Redis = require('ioredis');
const { JudgeWorker } = require('./judge-worker');

const app = express();
app.use(express.json({ limit: '10mb' }));

// ==================== 并发控制 ====================
const MAX_CONCURRENT_JOBS = parseInt(process.env.MAX_CONCURRENT_JOBS || '5', 10);
let activeJobs = 0;
const pendingQueue = [];

/**
 * 获取执行许可（信号量 acquire）
 * @returns {Promise<void>} 当有空闲槽位时 resolve
 */
function acquireSlot() {
  return new Promise((resolve) => {
    if (activeJobs < MAX_CONCURRENT_JOBS) {
      activeJobs++;
      resolve();
    } else {
      pendingQueue.push(resolve);
    }
  });
}

/**
 * 释放执行许可（信号量 release）
 */
function releaseSlot() {
  if (pendingQueue.length > 0) {
    const next = pendingQueue.shift();
    next();
  } else {
    activeJobs--;
  }
}

// Logger
const logger = winston.createLogger({
  level: 'info',
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.json()
  ),
  transports: [
    new winston.transports.Console(),
    new winston.transports.File({ filename: 'sandbox.log', maxsize: 10 * 1024 * 1024, maxFiles: 3 })
  ]
});

// Redis 连接
const REDIS_URL = process.env.REDIS_URL || 'redis://localhost:6379';
let redis;
let useRedis = false;

try {
  redis = new Redis(REDIS_URL, {
    maxRetriesPerRequest: 3,
    retryStrategy(times) {
      if (times > 3) return null;
      return Math.min(times * 200, 2000);
    },
    lazyConnect: true
  });

  redis.connect().then(() => {
    useRedis = true;
    logger.info('Redis 连接成功，使用 Redis 持久化任务队列');
  }).catch(() => {
    useRedis = false;
    logger.warn('Redis 连接失败，降级为内存任务队列');
  });
} catch (e) {
  useRedis = false;
  logger.warn('Redis 不可用，降级为内存任务队列');
}

// 内存任务队列（降级方案）
const memoryQueue = new Map();
const worker = new JudgeWorker(logger);

const TASK_PREFIX = 'sandbox:task:';
const TASK_TTL = 3600;

// ==================== 任务存储 ====================

async function saveTask(task) {
  if (useRedis) {
    try {
      await redis.setex(TASK_PREFIX + task.id, TASK_TTL, JSON.stringify(task));
    } catch (e) {
      logger.warn('Redis 保存任务失败，降级到内存', e.message);
      memoryQueue.set(task.id, task);
    }
  } else {
    memoryQueue.set(task.id, task);
  }
}

async function getTask(taskId) {
  if (useRedis) {
    try {
      const data = await redis.get(TASK_PREFIX + taskId);
      return data ? JSON.parse(data) : null;
    } catch (e) {
      return memoryQueue.get(taskId) || null;
    }
  }
  return memoryQueue.get(taskId) || null;
}

// ==================== API ====================

/**
 * 健康检查
 */
app.get('/health', (req, res) => {
  res.json({
    status: 'UP',
    service: 'scratch-sandbox',
    version: '0.4.0',
    queue: useRedis ? 'redis' : 'memory',
    concurrency: {
      max: MAX_CONCURRENT_JOBS,
      active: activeJobs,
      queued: pendingQueue.length
    },
    activeTasks: worker.activeProcesses ? worker.activeProcesses.size : 0
  });
});

/**
 * 提交判题任务（兼容旧接口 POST /judge）
 * 后端 JudgeService 调用 POST /judge
 */
app.post('/judge', async (req, res) => {
  const { submissionId, sb3Url, expectedOutput, timeoutMs } = req.body;

  if (!sb3Url) {
    return res.status(400).json({ error: '缺少必要参数: sb3Url' });
  }

  const taskId = uuidv4();
  const task = {
    id: taskId,
    submissionId,
    status: 'pending',
    projectUrl: sb3Url,
    testCases: expectedOutput || [],
    timeLimit: timeoutMs || 30000,
    createdAt: new Date().toISOString()
  };

  await saveTask(task);
  logger.info(`任务创建: ${taskId}, submissionId=${submissionId}`);

  // 异步执行
  executeTask(task);

  res.json({ taskId, status: 'pending' });
});

/**
 * 提交判题任务（新接口 POST /judge/submit）
 */
app.post('/judge/submit', async (req, res) => {
  const { projectUrl, problemId, testCases, timeLimit, memoryLimit } = req.body;

  if (!projectUrl || !testCases || !Array.isArray(testCases)) {
    return res.status(400).json({ error: '缺少必要参数: projectUrl, testCases' });
  }

  const taskId = uuidv4();
  const task = {
    id: taskId,
    status: 'pending',
    projectUrl,
    problemId,
    testCases,
    timeLimit: timeLimit || 30000,
    memoryLimit: memoryLimit || 512,
    createdAt: new Date().toISOString()
  };

  await saveTask(task);
  logger.info(`任务创建: ${taskId}, 测试用例数: ${testCases.length}`);

  // 异步执行
  executeTask(task);

  res.json({ taskId, status: 'pending' });
});

/**
 * 查询任务状态
 */
app.get('/judge/status/:taskId', async (req, res) => {
  const task = await getTask(req.params.taskId);
  if (!task) {
    return res.status(404).json({ error: '任务不存在' });
  }
  res.json({
    id: task.id,
    status: task.status,
    result: task.result
  });
});

/**
 * 获取判题结果
 */
app.get('/judge/result/:taskId', async (req, res) => {
  const task = await getTask(req.params.taskId);
  if (!task) {
    return res.status(404).json({ error: '任务不存在' });
  }
  if (task.status !== 'done' && task.status !== 'error') {
    return res.json({ id: task.id, status: task.status });
  }
  res.json({
    id: task.id,
    status: task.status,
    result: task.result,
    verdict: task.verdict,
    details: task.details,
    executionLog: task.executionLog,
    runtimeMs: task.runtimeMs
  });
});

// ==================== 判题执行 ====================

/**
 * 执行判题任务（带并发控制）
 *
 * <p>使用信号量控制并发数，超过 MAX_CONCURRENT_JOBS 的任务排队等待。
 * acquire 在任务开始前获取许可，release 在任务结束后（无论成功失败）释放许可。
 */
async function executeTask(task) {
  // 等待获取执行许可（排队）
  await acquireSlot();
  logger.info(`任务获取执行许可: ${task.id}, 当前活跃: ${activeJobs}/${MAX_CONCURRENT_JOBS}, 排队: ${pendingQueue.length}`);

  task.status = 'running';
  await saveTask(task);

  try {
    const result = await worker.judge(task);
    task.status = 'done';
    task.verdict = result.status;       // AC / WA / TLE / RE
    task.result = result.status;
    task.details = result.details;
    task.executionLog = result.log;
    task.score = result.score;
    task.runtimeMs = result.details ? result.details.reduce((sum, d) => sum + (d.timeUsed || 0), 0) : 0;
    logger.info(`任务完成: ${task.id}, 结果: ${result.status}, 得分: ${result.score}`);
  } catch (err) {
    task.status = 'error';
    task.verdict = 'RE';
    task.result = 'RE';
    task.executionLog = err.message;
    logger.error(`任务失败: ${task.id}`, err.message);
  } finally {
    // 无论成功失败，都释放执行许可
    releaseSlot();
  }

  await saveTask(task);
}

// ==================== 定期清理 ====================

const cleanupInterval = setInterval(async () => {
  if (!useRedis) {
    const cutoff = Date.now() - 3600000;
    let cleaned = 0;
    for (const [id, task] of memoryQueue) {
      if (new Date(task.createdAt).getTime() < cutoff) {
        memoryQueue.delete(id);
        cleaned++;
      }
    }
    if (cleaned > 0) logger.info(`清理过期任务: ${cleaned} 个`);
  }
}, 600000);

// ==================== 优雅关闭 ====================

function gracefulShutdown(signal) {
  logger.info(`收到 ${signal} 信号，开始优雅关闭...`);

  // 停止接受新连接
  server.close(() => {
    logger.info('HTTP 服务已关闭');
  });

  // 终止所有活跃的判题进程
  worker.killAll();

  // 清理定时器
  clearInterval(cleanupInterval);

  // 关闭 Redis 连接
  if (redis) {
    redis.quit().catch(() => {});
  }

  // 等待 5 秒后强制退出
  setTimeout(() => {
    logger.warn('强制退出');
    process.exit(0);
  }, 5000);
}

process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
process.on('SIGINT', () => gracefulShutdown('SIGINT'));

// 未捕获异常处理
process.on('uncaughtException', (err) => {
  logger.error('未捕获异常:', err);
});
process.on('unhandledRejection', (reason) => {
  logger.error('未处理的 Promise 拒绝:', reason);
});

// ==================== 启动 ====================

const PORT = process.env.PORT || 8081;
const server = app.listen(PORT, () => {
  logger.info(`Scratch 判题沙箱服务启动: http://localhost:${PORT}`);
});
