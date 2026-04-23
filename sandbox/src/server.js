const express = require('express');
const { v4: uuidv4 } = require('uuid');
const winston = require('winston');
const { JudgeWorker } = require('./judge-worker');

const app = express();
app.use(express.json());

// Logger
const logger = winston.createLogger({
  level: 'info',
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.json()
  ),
  transports: [
    new winston.transports.Console(),
    new winston.transports.File({ filename: 'sandbox.log' })
  ]
});

// 任务队列 (内存版，生产环境用 Redis/Bull)
const taskQueue = new Map();
const worker = new JudgeWorker(logger);

// ==================== API ====================

/**
 * 健康检查
 */
app.get('/health', (req, res) => {
  res.json({ status: 'UP', service: 'scratch-sandbox', version: '0.1.0' });
});

/**
 * 提交判题任务
 * POST /judge/submit
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

  taskQueue.set(taskId, task);
  logger.info(`任务创建: ${taskId}, 测试用例数: ${testCases.length}`);

  // 异步执行
  executeTask(task);

  res.json({ taskId, status: 'pending' });
});

/**
 * 查询任务状态
 * GET /judge/status/:taskId
 */
app.get('/judge/status/:taskId', (req, res) => {
  const task = taskQueue.get(req.params.taskId);
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
 * GET /judge/result/:taskId
 */
app.get('/judge/result/:taskId', (req, res) => {
  const task = taskQueue.get(req.params.taskId);
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
    details: task.details,
    executionLog: task.executionLog
  });
});

// ==================== 判题执行 ====================

async function executeTask(task) {
  task.status = 'running';
  try {
    const result = await worker.judge(task);
    task.status = 'done';
    task.result = result.status;      // AC / WA / TLE / RE
    task.details = result.details;    // 每个测试用例的结果
    task.executionLog = result.log;
    task.score = result.score;
    logger.info(`任务完成: ${task.id}, 结果: ${result.status}`);
  } catch (err) {
    task.status = 'error';
    task.result = 'RE';
    task.executionLog = err.message;
    logger.error(`任务失败: ${task.id}`, err);
  }
}

// ==================== 启动 ====================

const PORT = process.env.PORT || 8081;
app.listen(PORT, () => {
  logger.info(`Scratch 判题沙箱服务启动: http://localhost:${PORT}`);
});
