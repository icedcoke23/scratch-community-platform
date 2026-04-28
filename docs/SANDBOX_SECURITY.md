# 沙箱安全加固指南

> 版本: v1.0 | 日期: 2026-04-28
> 目标: 防止恶意 Scratch 项目逃逸沙箱，保护宿主系统安全

---

## 一、当前安全措施

### 已实现

| 层级 | 措施 | 说明 |
|------|------|------|
| 进程隔离 | `fork()` 子进程 | 每个判题任务在独立子进程中运行 |
| 内存限制 | `--max-old-space-size=512` | 子进程 V8 堆上限 512MB |
| 超时控制 | TLE + 总超时 | 单任务超时 30s，总超时 40s |
| DNS 隔离 | `dns: 127.0.0.1` | 容器内禁止 DNS 解析 |
| 网络隔离 | Docker internal network | 沙箱仅能访问内部服务 |
| 资源限制 | Docker deploy limits | CPU 2核，内存 2GB |
| 优雅关闭 | SIGTERM 处理 | 关闭时终止所有子进程 |

---

## 二、安全加固方案

### 2.1 cgroup v2 资源限制

在 Linux 系统上使用 cgroup v2 对沙箱进程进行更细粒度的资源控制：

```bash
# 创建 cgroup
sudo mkdir -p /sys/fs/cgroup/scratch-sandbox

# 设置内存限制 (硬限制 1GB，软限制 512MB)
echo "1073741824" | sudo tee /sys/fs/cgroup/scratch-sandbox/memory.max
echo "536870912" | sudo tee /sys/fs/cgroup/scratch-sandbox/memory.high

# 设置 CPU 限制 (最多使用 2 个核心)
echo "200000 100000" | sudo tee /sys/fs/cgroup/scratch-sandbox/cpu.max

# 设置 PIDs 限制 (防止 fork bomb)
echo "64" | sudo tee /sys/fs/cgroup/scratch-sandbox/pids.max

# 设置 I/O 带宽限制 (10MB/s 写入限制)
echo "254:0 wbps=10485760" | sudo tee /sys/fs/cgroup/scratch-sandbox/io.max
```

### 2.2 seccomp 系统调用过滤

创建 `sandbox-seccomp.json` 限制可用的系统调用：

```json
{
  "defaultAction": "SCMP_ACT_ERRNO",
  "architectures": ["SCMP_ARCH_X86_64"],
  "syscalls": [
    {
      "names": [
        "read", "write", "close", "fstat", "lseek", "mmap", "mprotect",
        "munmap", "brk", "access", "getpid", "clone", "execve", "wait4",
        "exit_group", "arch_prctl", "set_tid_address", "clock_gettime",
        "openat", "newfstatat", "readlink", "getdents64", "epoll_wait",
        "epoll_ctl", "epoll_create1", "pipe2", "dup2", "fcntl",
        "futex", "nanosleep", "getrandom", "rseq", "prlimit64",
        "getuid", "getgid", "geteuid", "getegid", "statfs",
        "sigaltstack", "rt_sigaction", "rt_sigprocmask",
        "ioctl", "pread64", "pwrite64", "readv", "writev",
        "socket", "connect", "shutdown", "bind", "listen", "accept",
        "getsockname", "getpeername", "socketpair", "setsockopt",
        "getsockopt", "sendto", "recvfrom", "sendmsg", "recvmsg",
        "eventfd2", "timerfd_create", "timerfd_settime", "timerfd_gettime"
      ],
      "action": "SCMP_ACT_ALLOW"
    }
  ]
}
```

### 2.3 Docker Compose 安全增强

```yaml
sandbox:
  build:
    context: ../sandbox
    dockerfile: Dockerfile
  container_name: scratch-sandbox
  # 安全选项
  security_opt:
    - no-new-privileges:true    # 禁止提权
    - seccomp:sandbox-seccomp.json  # 系统调用过滤
  # 只读文件系统
  read_only: true
  tmpfs:
    - /tmp:size=100M,noexec,nosuid,nodev  # 临时文件限制
  # 禁用 capabilities
  cap_drop:
    - ALL
  cap_add:
    - CHOWN      # 文件权限修改
    - SETUID     # 用户 ID 设置
    - SETGID     # 组 ID 设置
  # PID 限制
  pids_limit: 64
  # ulimit 配置
  ulimits:
    nofile:
      soft: 1024
      hard: 2048
    nproc:
      soft: 64
      hard: 128
    fsize:
      soft: 52428800   # 50MB
      hard: 104857600  # 100MB
  networks:
    - scratch-internal
  dns:
    - "127.0.0.1"
  deploy:
    resources:
      limits:
        memory: 2G
        cpus: "2.0"
      reservations:
        memory: 256M
  restart: unless-stopped
```

### 2.4 sb3 文件验证

在下载和执行 sb3 文件前进行安全检查：

```javascript
// 文件大小限制
const MAX_SB3_SIZE = 50 * 1024 * 1024; // 50MB

// ZIP bomb 检测
const MAX_UNCOMPRESSED_SIZE = 200 * 1024 * 1024; // 200MB
const MAX_COMPRESSION_RATIO = 100;

// 危险文件名检测
const BLOCKED_EXTENSIONS = [
  '.exe', '.bat', '.cmd', '.sh', '.ps1', '.py', '.rb',
  '.js', '.ts', '.dll', '.so', '.dylib'
];

// 最大解压文件数
const MAX_FILE_COUNT = 1000;
```

### 2.5 进程行为监控

监控子进程的异常行为：

```javascript
// 监控内存使用
const checkMemory = setInterval(() => {
  const usage = process.memoryUsage();
  if (usage.heapUsed > MAX_HEAP_SIZE) {
    logger.warn(`内存超限: ${usage.heapUsed} > ${MAX_HEAP_SIZE}`);
    process.exit(1);
  }
}, 1000);

// 监控文件描述符数量
const checkFDs = setInterval(() => {
  try {
    const fds = fs.readdirSync('/proc/self/fd').length;
    if (fds > MAX_FILE_DESCRIPTORS) {
      logger.warn(`文件描述符超限: ${fds}`);
      process.exit(1);
    }
  } catch (e) { /* 非 Linux 系统忽略 */ }
}, 5000);
```

---

## 三、gVisor 方案（高级隔离）

对于生产环境，建议使用 gVisor 进行内核级隔离：

```yaml
# docker-compose.gvisor.yml
sandbox:
  runtime: runsc  # gVisor runtime
  # ... 其他配置同上
```

### 安装 gVisor

```bash
# 安装 runsc
curl -fsSL https://gvisor.dev/archive.key | sudo gpg --dearmor -o /usr/share/keyrings/gvisor-archive-keyring.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/gvisor-archive-keyring.gpg] https://storage.googleapis.com/gvisor/releases release main" | sudo tee /etc/apt/sources.list.d/gvisor.list > /dev/null
sudo apt-get update && sudo apt-get install -y runsc

# 配置 Docker 使用 gVisor
sudo tee /etc/docker/daemon.json <<EOF
{
  "runtimes": {
    "runsc": {
      "path": "/usr/bin/runsc"
    }
  }
}
EOF

sudo systemctl restart docker
```

---

## 四、安全审计清单

- [ ] 沙箱容器以非 root 用户运行
- [ ] 文件系统设置为只读（tmpfs 除外）
- [ ] 禁用所有不必要的 Linux capabilities
- [ ] 配置 seccomp 系统调用白名单
- [ ] 设置 PID 限制防止 fork bomb
- [ ] 设置文件描述符限制
- [ ] 设置文件大小限制
- [ ] DNS 解析被禁用（仅允许 127.0.0.1）
- [ ] 网络隔离（Docker internal network）
- [ ] sb3 文件大小验证（< 50MB）
- [ ] ZIP bomb 检测
- [ ] 子进程内存限制（--max-old-space-size）
- [ ] 子进程超时控制
- [ ] 临时文件清理（任务完成后）
- [ ] 日志记录所有异常退出

---

## 五、事件响应

### 检测到异常行为时

1. **立即终止**相关子进程 (`SIGKILL`)
2. **记录**完整的执行日志和进程状态
3. **通知**管理员（可配置告警规则）
4. **隔离**提交该项目的用户账号
5. **分析**异常 sb3 文件内容

### 告警规则示例 (Prometheus)

```yaml
groups:
  - name: sandbox-alerts
    rules:
      - alert: SandboxProcessOOM
        expr: container_memory_usage_bytes{container="scratch-sandbox"} > 1800000000
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "沙箱内存使用过高"

      - alert: SandboxHighErrorRate
        expr: rate(sandbox_judge_errors_total[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "沙箱判题错误率过高"
```
