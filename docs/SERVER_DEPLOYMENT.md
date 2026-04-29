# 服务器部署记录 — Alibaba Cloud Linux 3 (VPS)

**部署时间**: 2026-04-29 22:00-23:10  
**服务器 IP**: 8.134.252.67  
**部署方式**: 手动安装（非 Docker）  
**部署状态**: ✅ 后端运行中，前端待部署

---

## 🖥️ 服务器环境

### 操作系统
- **发行版**: Alibaba Cloud Linux 3 (Anolis 8 兼容)
- **内核**: 4.18.0-553.19.1.al8.x86_64
- **包管理器**: yum / dnf
- **防火墙**: firewalld（默认开启，已放行 80/8080 端口）

### 已安装软件

| 软件 | 版本 | 安装方式 | 用途 |
|------|------|----------|------|
| **OpenJDK 17** | 17.0.18 LTS | dnf | 后端运行时 |
| **Apache Maven** | 3.6.2 | dnf | 项目构建 |
| **Node.js** | v22.22.2 | nvm | 前端构建 |
| **npm** | 10.9.7 | nvm | 前端依赖 |
| **MySQL** | 8.0.45 | dnf (mysql-community) | 业务数据库 |
| **Redis** | 6.2.20 | dnf | 缓存/限流/锁 |
| **Git** | 2.43.7 | dnf | 代码拉取 |
| **Nginx** | - | - | 待安装（前端代理） |

### 目录结构

```
/root/scratch-community-platform/    # 项目代码
/root/.nvm/                          # Node 版本管理
/var/lib/mysql/                      # MySQL 数据
/var/log/mysqld.log                  # MySQL 日志
/etc/redis.conf                      # Redis 配置
```

---

## ⚙️ 数据库配置

### MySQL

```bash
# 服务管理
systemctl enable --now mysqld
systemctl status mysqld

# 连接
mysql -u root -pRoot@2485204216

# 数据库
- scratch_community     (生产库)
- scratch_community_test (测试库)

# 用户
- root     : Root@2485204216 (mysql_native_password)
- scratch  : scratch123       (mysql_native_password, %/localhost/127.0.0.1)
```

### Redis

```bash
systemctl enable --now redis
redis-cli ping    # -> PONG
```

---

## 🚀 后端部署

### 构建

```bash
cd /root/scratch-community-platform/backend
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-17.0.18.0.8-1.0.2.1.al8.x86_64
export PATH=$JAVA_HOME/bin:$PATH
mvn clean package -DskipTests
```

### 运行

```bash
cd backend/scratch-app
nohup java -jar target/scratch-app-0.1.0-SNAPSHOT.jar \
  --spring.profiles.active=dev \
  > /tmp/backend.log 2>&1 &

# 检查
ps aux | grep scratch-app
curl http://localhost:8080/actuator/health
tail -50 /tmp/backend.log
```

### 当前状态

| 指标 | 值 |
|------|-----|
| **PID** | 67999 |
| **端口** | 8080 |
| **状态** | ✅ UP |
| **Flyway** | 19 个迁移完成 |
| **Redis** | 已连接 |
| **JWT** | 警告：使用默认密钥（生产环境需配置） |
| **MinIO** | 警告：未运行（需安装） |

---

## 📦 待部署

- [ ] **前端构建与 Nginx 部署**
- [ ] **MinIO 安装与配置**（存储 sb3/头像）
- [ ] **Node.js 判题沙箱部署**（端口 8081）
- [ ] **生产环境变量配置**（JWT 密钥等）
- [ ] **SSL 证书与 HTTPS**（Let's Encrypt）
- [ ] **系统服务守护**（systemd unit）

---

## 🔧 待解决问题

| 问题 | 优先级 | 状态 |
|------|--------|------|
| 1. MinIO 未启动（文件存储缺失） | P0 | ⏳ 待安装 |
| 2. 前端未部署（无法访问 UI） | P0 | ⏳ 待部署 |
| 3. 沙箱未运行（判题失败） | P0 | ⏳ 待部署 |
| 4. JWT 使用默认密钥（安全风险） | P0 | ⚠️ 警告 |
| 5. 未配置防火墙（安全风险） | P1 | ⚠️ 警告 |
| 6. 无 HTTPS（生产必需） | P1 | ⏳ 待配置 |
| 7. Testcontainers 未配置（集成测试） | P2 | ⏳ 待配置 |
| 8. 大表分区（point_log/notification） | P2 | ⏳ 待优化 |
| 9. 数据归档策略 | P2 | ⏳ 待设计 |

---

## 📝 后续开发计划

1. **立即（本会话）**:
   - 安装 Nginx + MinIO + 沙箱
   - 部署前端
   - 验证完整流程

2. **短期（Sprint 33）**:
   - 配置 Testcontainers 集成测试
   - 实现数据分区（point_log 按时间分区）
   - 添加数据归档 Job

3. **中期（生产就绪）**:
   - 配置 HTTPS（Let's Encrypt）
   - 设置 JWT 生产密钥
   - 配置防火墙规则
   - 添加系统服务（systemd）
   - 配置日志轮转

4. **长期**:
   - 监控告警（Prometheus + Grafana）
   - 备份策略（数据库 + 文件）
   - 灾难恢复方案
   - 多节点部署

---

**文档更新**: 2026-04-29 23:10  
**下次部署**: 待前端完成
