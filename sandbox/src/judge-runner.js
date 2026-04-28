/**
 * 判题执行器（子进程）
 *
 * 此文件在独立子进程中运行，通过 fork() 启动。
 * 通过临时文件和环境变量接收参数，通过 process.send() 返回结果。
 *
 * 环境变量:
 * - SB3_PATH: sb3 文件路径
 * - TEST_CASE_FILE: 测试用例 JSON 文件路径（文件传递，避免环境变量长度限制）
 * - TIME_LIMIT: 超时时间(ms)
 *
 * 注意: 测试用例必须通过 TEST_CASE_FILE 文件传递，不再支持环境变量方式。
 * 原因: 环境变量有长度限制（128KB-2MB），大量测试用例会导致 E2BIG 错误。
 */

const VirtualMachine = require('scratch-vm');
const fs = require('fs');

const sb3Path = process.env.SB3_PATH;
const testCaseFile = process.env.TEST_CASE_FILE;

// 测试用例必须通过文件传递
if (!testCaseFile || !fs.existsSync(testCaseFile)) {
    console.error('错误: 测试用例文件不存在。必须通过 TEST_CASE_FILE 环境变量指定文件路径。');
    process.exit(1);
}

const testCases = JSON.parse(fs.readFileSync(testCaseFile, 'utf-8'));
const timeLimit = parseInt(process.env.TIME_LIMIT || '30000', 10);

async function runJudge() {
  if (!sb3Path || !fs.existsSync(sb3Path)) {
    throw new Error('sb3 文件不存在: ' + sb3Path);
  }

  if (testCases.length === 0) {
    throw new Error('没有测试用例');
  }

  const results = [];
  let allPassed = true;
  let totalScore = 0;
  const log = [];

  for (let i = 0; i < testCases.length; i++) {
    const tc = testCases[i];
    log.push(`\n--- 测试用例 ${i + 1} ---`);
    log.push(`输入: ${tc.input}`);
    log.push(`期望输出: ${tc.expectedOutput}`);

    try {
      const result = await runSingleTest(sb3Path, tc, timeLimit);
      const passed = compareOutput(result.output, tc.expectedOutput);

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

  return {
    status: allPassed ? 'AC' : 'WA',
    score: Math.round(totalScore),
    details: results,
    log: log.join('\n')
  };
}

/**
 * 运行单个测试用例
 * 所有超时/解析互斥，防止多次 resolve/reject
 */
async function runSingleTest(sb3Path, testCase, timeLimit) {
  return new Promise((resolve, reject) => {
    const startTime = Date.now();
    const vm = new VirtualMachine();
    const sb3Buffer = fs.readFileSync(sb3Path);
    let settled = false;

    // 安全的单次 settle
    function settle(fn) {
      if (settled) return;
      settled = true;
      clearTimeout(tleTimer);
      clearTimeout(minCheck);
      clearTimeout(absTimer);
      try { vm.quit(); } catch (e) { /* 忽略 */ }
      fn();
    }

    // TLE 超时
    const tleTimer = setTimeout(() => {
      settle(() => reject(new Error('Time Limit Exceeded')));
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
      const timeUsed = Date.now() - startTime;
      const output = captureOutput(vm);
      settle(() => resolve({ output, timeUsed }));
    });

    // 加载并运行
    vm.loadProject(sb3Buffer).then(() => {
      vm.greenFlag();

      // 最小等待后检查是否已自然停止
      const minCheck = setTimeout(() => {
        if (vm.runtime.threads.length === 0) {
          const timeUsed = Date.now() - startTime;
          const output = captureOutput(vm);
          settle(() => resolve({ output, timeUsed }));
        }
      }, 1000);

      // 绝对超时（TLE + 2s 缓冲），防止 Scratch 项目永不停止
      const absTimer = setTimeout(() => {
        const timeUsed = Date.now() - startTime;
        const output = captureOutput(vm);
        vm.stop();
        settle(() => resolve({ output, timeUsed }));
      }, timeLimit + 2000);

    }).catch(err => {
      settle(() => reject(new Error(`加载项目失败: ${err.message}`)));
    });
  });
}

/**
 * 捕获 VM 输出
 *
 * 输出优先级:
 * 1. Stage 变量（排除内部变量如 cloud variables）
 * 2. Stage 列表
 * 3. 角色 "说"/"思考" 文本（sayText/thinkText）
 * 4. 打印队列（如果有）
 *
 * 注意: Scratch 3.0 的 "说" 积木会设置 target.sayText，
 * "思考" 积木会设置 target.thinkText。
 * 当 sayText 为 "" 或 null 时，表示角色已停止说话。
 */
function captureOutput(vm) {
  const targets = vm.runtime.targets;
  const outputs = [];

  // 1. 优先收集 Stage 变量（这是最常见的输出方式）
  for (const target of targets) {
    if (target.isStage) {
      const variables = target.variables || {};
      for (const [id, variable] of Object.entries(variables)) {
        // 跳过云变量（以 cloud 前缀标识）
        if (id.startsWith('cloud')) continue;

        if (variable.type === 'list') {
          // 列表类型：将所有项用空格连接
          const items = variable.value || [];
          if (items.length > 0) {
            outputs.push(items.join(' '));
          }
        } else if (variable.value !== undefined && variable.value !== null && String(variable.value) !== '') {
          outputs.push(String(variable.value));
        }
      }
    }
  }

  // 2. 如果 Stage 没有变量输出，收集角色的 "说"/"思考" 文本
  if (outputs.length === 0) {
    for (const target of targets) {
      if (target.isStage) continue;

      // "说" 积木的输出
      const sayText = target.sayText;
      if (sayText && typeof sayText === 'string' && sayText.trim() !== '') {
        outputs.push(sayText.trim());
        continue;
      }

      // "思考" 积木的输出
      const thinkText = target.thinkText;
      if (thinkText && typeof thinkText === 'string' && thinkText.trim() !== '') {
        outputs.push(thinkText.trim());
      }
    }
  }

  // 3. 兜底：检查打印队列（部分 Scratch 扩展可能使用）
  if (outputs.length === 0 && vm.runtime._printLog) {
    const printLog = vm.runtime._printLog;
    if (Array.isArray(printLog) && printLog.length > 0) {
      outputs.push(...printLog.map(String));
    }
  }

  return outputs.length > 0 ? outputs.join('\n') : '';
}

/**
 * 比对输出
 *
 * 匹配策略（从严格到宽松）:
 * 1. 精确匹配
 * 2. 数字比较（处理 3.0 vs 3、浮点精度问题）
 * 3. 忽略大小写
 * 4. 忽略首尾空白 + 多余空格
 * 5. 多行输出：逐行比对（任一行匹配即通过）
 */
function compareOutput(actual, expected) {
  if (actual == null || expected == null) return false;
  const a = actual.trim();
  const e = expected.trim();

  // 空字符串匹配
  if (a === '' && e === '') return true;
  if (a === '' || e === '') return false;

  // 1. 精确匹配
  if (a === e) return true;

  // 2. 数字比较（处理 3.0 vs 3、-0 vs 0）
  const numA = parseFloat(a);
  const numE = parseFloat(e);
  if (!isNaN(numA) && !isNaN(numE)) {
    // 浮点数近似比较（处理精度问题）
    if (Math.abs(numA - numE) < 1e-9) return true;
  }

  // 3. 忽略大小写
  if (a.toLowerCase() === e.toLowerCase()) return true;

  // 4. 规范化空白后比较（处理多空格、Tab、换行差异）
  const normalizeWhitespace = (s) => s.replace(/\s+/g, ' ').trim();
  if (normalizeWhitespace(a) === normalizeWhitespace(e)) return true;

  // 5. 多行输出：逐行比对（任一行匹配即通过）
  const aLines = a.split('\n').map(l => l.trim()).filter(l => l !== '');
  const eLines = e.split('\n').map(l => l.trim()).filter(l => l !== '');
  if (aLines.length > 0 && eLines.length > 0) {
    // 检查期望输出的每一行是否都能在实际输出中找到匹配
    const allMatch = eLines.every(el =>
      aLines.some(al => al === el || al.toLowerCase() === el.toLowerCase())
    );
    if (allMatch) return true;
  }

  return false;
}

// ==================== 执行 ====================
(async () => {
  try {
    const result = await runJudge();
    if (process.send) {
      process.send({ type: 'result', data: result });
    } else {
      process.stdout.write(JSON.stringify(result));
    }
    process.exit(0);
  } catch (err) {
    const errorResult = {
      status: 'RE',
      score: 0,
      details: [],
      log: `执行错误: ${err.message}`
    };
    if (process.send) {
      process.send({ type: 'result', data: errorResult });
    } else {
      process.stdout.write(JSON.stringify(errorResult));
    }
    process.exit(1);
  }
})();
