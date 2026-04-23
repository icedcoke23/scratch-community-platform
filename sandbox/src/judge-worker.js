const VirtualMachine = require('scratch-vm');
const AdmZip = require('adm-zip');
const fs = require('fs');
const path = require('path');
const http = require('http');
const https = require('https');
const os = require('os');

/**
 * Scratch 判题 Worker
 * 加载 sb3 项目 → 注入测试用例 → 执行 → 捕获输出 → 比对结果
 */
class JudgeWorker {
  constructor(logger) {
    this.logger = logger;
  }

  /**
   * 执行判题
   * @param {Object} task 判题任务
   * @returns {Object} 判题结果
   */
  async judge(task) {
    const { projectUrl, testCases, timeLimit } = task;
    const results = [];
    let allPassed = true;
    let totalScore = 0;
    const log = [];

    // 下载 sb3 文件
    const sb3Path = await this.downloadFile(projectUrl);
    log.push(`下载 sb3: ${projectUrl}`);

    for (let i = 0; i < testCases.length; i++) {
      const tc = testCases[i];
      log.push(`\n--- 测试用例 ${i + 1} ---`);
      log.push(`输入: ${tc.input}`);
      log.push(`期望输出: ${tc.expectedOutput}`);

      try {
        const result = await this.runSingleTest(sb3Path, tc, timeLimit);
        const passed = this.compareOutput(result.output, tc.expectedOutput);

        results.push({
          testCaseId: i + 1,
          input: tc.input,
          expected: tc.expectedOutput,
          actual: result.output,
          passed,
          timeUsed: result.timeUsed
        });

        log.push(`实际输出: ${result.output}`);
        log.push(`用时: ${result.timeUsed}ms`);
        log.push(`结果: ${passed ? '✓ PASS' : '✗ FAIL'}`);

        if (passed) {
          totalScore += tc.score || (100 / testCases.length);
        } else {
          allPassed = false;
        }
      } catch (err) {
        results.push({
          testCaseId: i + 1,
          input: tc.input,
          expected: tc.expectedOutput,
          actual: null,
          passed: false,
          error: err.message
        });
        allPassed = false;
        log.push(`错误: ${err.message}`);
      }
    }

    // 清理临时文件
    this.cleanup(sb3Path);

    return {
      status: allPassed ? 'AC' : 'WA',
      score: Math.round(totalScore),
      details: results,
      log: log.join('\n')
    };
  }

  /**
   * 运行单个测试用例
   */
  async runSingleTest(sb3Path, testCase, timeLimit) {
    return new Promise((resolve, reject) => {
      const startTime = Date.now();
      const vm = new VirtualMachine();

      // 读取 sb3
      const sb3Buffer = fs.readFileSync(sb3Path);

      // 超时控制
      const timer = setTimeout(() => {
        vm.stop();
        reject(new Error('Time Limit Exceeded'));
      }, timeLimit);

      // 监听问答事件 (注入测试输入)
      let answered = false;
      vm.runtime.on('QUESTION', (question) => {
        if (!answered) {
          answered = true;
          vm.runtime.emit('ANSWER', testCase.input);
        }
      });

      // 监听项目停止
      vm.runtime.on('PROJECT_STOP', () => {
        clearTimeout(timer);
        const timeUsed = Date.now() - startTime;
        const output = this.captureOutput(vm);
        resolve({ output, timeUsed });
      });

      // 加载并运行
      vm.loadProject(sb3Buffer).then(() => {
        vm.greenFlag();
        // 如果项目很快结束，给一个最小等待
        setTimeout(() => {
          if (vm.runtime.threads.length === 0) {
            clearTimeout(timer);
            const timeUsed = Date.now() - startTime;
            const output = this.captureOutput(vm);
            resolve({ output, timeUsed });
          }
        }, 1000);
      }).catch(err => {
        clearTimeout(timer);
        reject(new Error(`加载项目失败: ${err.message}`));
      });
    });
  }

  /**
   * 捕获 VM 输出
   * 从舞台上的变量和外观状态提取输出
   */
  captureOutput(vm) {
    const targets = vm.runtime.targets;
    const outputs = [];

    // 遍历所有目标，提取全局变量的值
    for (const target of targets) {
      if (target.isStage) {
        // 舞台变量 — Scratch 变量 type 为 '' (scalar) 或 'list'
        const variables = target.variables;
        for (const [id, variable] of Object.entries(variables)) {
          if (variable.type === 'list') {
            // 列表取所有元素
            const items = variable.value || [];
            outputs.push(items.join(' '));
          } else if (variable.value !== undefined && variable.value !== '') {
            // 标量变量
            outputs.push(String(variable.value));
          }
        }
      }
    }

    // 如果没有变量输出，尝试捕获 say/think 消息
    if (outputs.length === 0) {
      for (const target of targets) {
        if (!target.isStage && target.visible) {
          // scratch-vm 的 say/think 文本存储在 target 的 _sayText 属性
          const sayText = target._sayText || '';
          if (sayText && sayText !== '') {
            outputs.push(sayText);
          }
        }
      }
    }

    // 合并输出
    return outputs.length > 0 ? outputs.join('\n') : '';
  }

  /**
   * 比对输出
   */
  compareOutput(actual, expected) {
    if (actual == null || expected == null) return false;
    // 去除首尾空白后比较
    return actual.trim() === expected.trim();
  }

  /**
   * 下载文件到临时目录
   */
  async downloadFile(url) {
    const tmpDir = os.tmpdir();
    const fileName = `scratch_${Date.now()}.sb3`;
    const filePath = path.join(tmpDir, fileName);

    return new Promise((resolve, reject) => {
      const client = url.startsWith('https') ? https : http;
      client.get(url, (res) => {
        if (res.statusCode !== 200) {
          reject(new Error(`下载失败: HTTP ${res.statusCode}`));
          return;
        }
        const stream = fs.createWriteStream(filePath);
        res.pipe(stream);
        stream.on('finish', () => {
          stream.close();
          resolve(filePath);
        });
      }).on('error', reject);
    });
  }

  /**
   * 清理临时文件
   */
  cleanup(filePath) {
    try {
      if (fs.existsSync(filePath)) {
        fs.unlinkSync(filePath);
      }
    } catch (err) {
      this.logger.warn(`清理临时文件失败: ${filePath}`, err);
    }
  }
}

module.exports = { JudgeWorker };
