const { fork } = require('child_process');
const fs = require('fs');
const path = require('path');
const os = require('os');
const http = require('http');
const https = require('https');

/**
 * Scratch 判题 Worker（进程隔离版）
 * 
 * 安全改进:
 * - 每个判题任务在独立子进程中执行，防止恶意 sb3 影响主服务
 * - 子进程有独立的内存限制和超时控制
 * - 子进程崩溃不影响沙箱主服务
 */
class JudgeWorker {
  constructor(logger) {
    this.logger = logger;
    this.activeProcesses = new Map(); // taskId -> child process
  }

  /**
   * 执行判题（在独立子进程中）
   * @param {Object} task 判题任务
   * @returns {Promise<Object>} 判题结果
   */
  async judge(task) {
    const { projectUrl, testCases, timeLimit } = task;

    // 下载 sb3 文件到临时目录
    const sb3Path = await this.downloadFile(projectUrl);

    try {
      // 在独立子进程中执行判题
      const result = await this.runInIsolatedProcess(sb3Path, testCases, timeLimit);
      return result;
    } finally {
      // 清理临时文件
      this.cleanup(sb3Path);
    }
  }

  /**
   * 在隔离子进程中运行判题
   * 子进程有独立的 V8 堆限制，防止 OOM
   */
  runInIsolatedProcess(sb3Path, testCases, timeLimit) {
    return new Promise((resolve, reject) => {
      const workerScript = path.join(__dirname, 'judge-runner.js');
      const startTime = Date.now();

      // 确保 runner 脚本存在
      if (!fs.existsSync(workerScript)) {
        return reject(new Error('判题执行脚本不存在: ' + workerScript));
      }

      // 将测试用例写入临时 JSON 文件，避免环境变量长度限制
      const testCaseFile = path.join(os.tmpdir(), `test_cases_${Date.now()}_${Math.random().toString(36).slice(2, 8)}.json`);
      fs.writeFileSync(testCaseFile, JSON.stringify(testCases), 'utf-8');

      const child = fork(workerScript, [], {
        silent: true,
        env: {
          ...process.env,
          SB3_PATH: sb3Path,
          TEST_CASE_FILE: testCaseFile,
          TIME_LIMIT: String(timeLimit || 30000)
        },
        // 子进程内存限制 512MB，防止恶意项目 OOM
        execArgv: ['--max-old-space-size=512']
      });

      let stdout = '';
      let stderr = '';
      let settled = false;
      const taskId = task_id_counter++;
      const cleanupTestCaseFile = () => {
        try { if (fs.existsSync(testCaseFile)) fs.unlinkSync(testCaseFile); } catch (e) { /* ignore */ }
      };

      this.activeProcesses.set(taskId, child);

      child.stdout.on('data', (data) => { stdout += data.toString(); });
      child.stderr.on('data', (data) => { stderr += data.toString(); });

      // 总超时：判题超时 + 10 秒缓冲
      const totalTimeout = (timeLimit || 30000) + 10000;
      const timer = setTimeout(() => {
        if (!settled) {
          settled = true;
          child.kill('SIGKILL');
          this.activeProcesses.delete(taskId);
          cleanupTestCaseFile();
          reject(new Error('判题进程超时，已强制终止'));
        }
      }, totalTimeout);

      child.on('message', (msg) => {
        if (!settled && msg && msg.type === 'result') {
          settled = true;
          clearTimeout(timer);
          this.activeProcesses.delete(taskId);
          cleanupTestCaseFile();
          resolve(msg.data);
        }
      });

      child.on('exit', (code, signal) => {
        if (!settled) {
          settled = true;
          clearTimeout(timer);
          this.activeProcesses.delete(taskId);
          cleanupTestCaseFile();
          if (signal === 'SIGKILL') {
            reject(new Error('判题进程被终止（内存超限或超时）'));
          } else if (code !== 0) {
            reject(new Error(`判题进程异常退出: code=${code}, stderr=${stderr.slice(0, 500)}`));
          } else {
            // 尝试解析 stdout
            try {
              const result = JSON.parse(stdout);
              resolve(result);
            } catch {
              reject(new Error('判题结果解析失败'));
            }
          }
        }
      });

      child.on('error', (err) => {
        if (!settled) {
          settled = true;
          clearTimeout(timer);
          this.activeProcesses.delete(taskId);
          cleanupTestCaseFile();
          reject(new Error(`判题进程启动失败: ${err.message}`));
        }
      });
    });
  }

  /**
   * 终止所有活跃的判题进程（优雅关闭时调用）
   */
  killAll() {
    for (const [taskId, child] of this.activeProcesses) {
      try {
        child.kill('SIGKILL');
        this.logger.warn(`强制终止判题进程: taskId=${taskId}`);
      } catch (e) { /* 忽略 */ }
    }
    this.activeProcesses.clear();
  }

  /**
   * 下载文件到临时目录
   */
  async downloadFile(url) {
    const tmpDir = os.tmpdir();
    const fileName = `scratch_${Date.now()}_${Math.random().toString(36).slice(2, 8)}.sb3`;
    const filePath = path.join(tmpDir, fileName);

    return new Promise((resolve, reject) => {
      const client = url.startsWith('https') ? https : http;
      const timer = setTimeout(() => reject(new Error('下载超时')), 30000);

      client.get(url, (res) => {
        if (res.statusCode !== 200) {
          clearTimeout(timer);
          reject(new Error(`下载失败: HTTP ${res.statusCode}`));
          return;
        }
        const stream = fs.createWriteStream(filePath);
        res.pipe(stream);
        stream.on('finish', () => {
          clearTimeout(timer);
          stream.close();
          resolve(filePath);
        });
        stream.on('error', (err) => {
          clearTimeout(timer);
          reject(err);
        });
      }).on('error', (err) => {
        clearTimeout(timer);
        reject(err);
      });
    });
  }

  /**
   * 清理临时文件
   */
  cleanup(filePath) {
    try {
      if (filePath && fs.existsSync(filePath)) {
        fs.unlinkSync(filePath);
      }
    } catch (err) {
      this.logger.warn(`清理临时文件失败: ${filePath}`, err.message);
    }
  }
}

let task_id_counter = 0;

module.exports = { JudgeWorker };
