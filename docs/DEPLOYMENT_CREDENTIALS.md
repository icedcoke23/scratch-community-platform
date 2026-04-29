# 🚀 Scratch 社区平台 — 部署凭据与访问信息

**部署时间**: 2026-04-30
**服务器 IP**: 8.134.252.67
**GitHub**: https://github.com/icedcoke23/scratch-community-platform
**分支**: `master`（最新提交 `d4c32e0`）

---

## 🔐 系统登录

| 项目 | 值 |
|------|-----|
| **SSH 主机** | `8.134.252.67` |
| **SSH 端口** | `22` |
| **SSH 用户** | `root` |
| **SSH 密码** | `Aa2485204216` |

---

## 🗄️ 数据库 (MySQL 8.0)

### 管理员账号
- **用户**: `root`
- **密码**: `Aa2485204216`
- **主机**: `localhost:3306`

### 应用账号（后端连接使用）
- **用户**: `scratch`
- **密码**: `scratch123`
- **数据库**: `scratch_community` (主库)
- **测试库**: `scratch_community_test`

### 数据库管理
```bash
# 登录
mysql -u root -p

# 查看所有库
SHOW DATABASES;

# 查看用户
SELECT user, host FROM mysql.user;
```

---

## 📦 MinIO 对象存储

### 控制台访问
- **URL**: http://8.134.252.67:9001
- **Access Key**: `minioadmin`
- **Secret Key**: `minioadmin`
- **API 地址**: http://localhost:9000

### 自动创建的 Buckets
| Bucket 名 | 用途 |
|-----------|------|
| `scratch-sb3` | Scratch 项目文件 |
| `scratch-avatar` | 用户头像 |
| `scratch-cover` | 项目封面 |

### 生产环境必须修改
⚠️ **默认凭证仅用于开发环境**。生产部署请执行：
1. 登录 MinIO Console (`:9001`) → Users → 创建新用户
2. 或使用 `mc` 工具修改密钥
3. 更新后端配置 `MinioConfig.java` 中的 `accessKey`/`secretKey`

---

## 🔑 JWT 密钥（已配置）

生产环境通过系统环境变量注入，**不存储在代码库**中：

**环境变量文件**: `/etc/profile.d/scratch.conf`

- `JWT_SECRET` = `4pnwzc8sQwT4phTslBU/x/IIJeZFowTShugTvcwaigEIJN/+aDxcyMwJ2ZHC15cdYUatHj56FKWPwCNXQb/gew=` (87 字节)
- `JWT_REFRESH_SECRET` = `8MtYwFm3YrIfPaaLTT3PBzBTznJ93+rBjiml8D65584jmkZtFQdtO1VpDS/fla1cw7MOyYySWAvzjlH08Aq7lg==` (87 字节)
- `JWT_EXPIRATION` = `3600000` (1 小时，毫秒)

---

## 🌐 服务访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| **前端首页** | http://8.134.252.67/ | Vue 3 单页应用 |
| **后端 API** | http://8.134.252.67/api/v1/ | RESTful API |
| **Swagger UI** | http://8.134.252.67/swagger-ui/ | API 文档 |
| **健康检查** | http://8.134.252.67/actuator/health | 服务状态 |
| **MinIO 控制台** | http://8.134.252.67:9001 | 对象存储管理 |
| **Nginx 状态** | `systemctl status nginx` | 监听 80 端口 |

---

## 🛠️ Systemd 服务

所有关键服务已启用开机自启：

```bash
# 后端应用
systemctl status scratch-app    # 查看状态
systemctl restart scratch-app   # 重启
journalctl -u scratch-app -f    # 查看日志

# Nginx
systemctl status nginx

# MySQL
systemctl status mysqld

# Redis
systemctl status redis

# MinIO (手动启动，可配置 systemd)
pkill -f "minio server"
nohup /usr/local/bin/minio server /data/minio/data --console-address :9001 > /var/log/minio.log 2>&1 &
```

---

## 📁 关键文件路径

| 文件/目录 | 路径 |
|-----------|------|
| 后端 JAR | `/root/scratch-community-platform/backend/scratch-app/target/scratch-app-0.1.0-SNAPSHOT.jar` |
| 前端静态文件 | `/var/www/html/` |
| Nginx 配置 | `/etc/nginx/conf.d/scratch.conf` |
| Systemd 单元 | `/etc/systemd/system/scratch-app.service` |
| JWT 环境变量 | `/etc/profile.d/scratch.conf` |
| MinIO 二进制 | `/usr/local/bin/minio` |
| MinIO 数据目录 | `/data/minio/data` |
| MinIO 日志 | `/var/log/minio.log` |
| 后端日志 | `/var/log/scratch.log` + `journalctl -u scratch-app` |

---

## 🔧 常用命令

```bash
# === 后端 ===
cd /root/scratch-community-platform/backend/scratch-app
java -jar target/scratch-app-0.1.0-SNAPSHOT.jar --spring.profiles.active=dev

# === 前端构建 ===
cd /root/scratch-community-platform/frontend-vue
npm install
npm run build
# 构建输出: dist/ → /var/www/html/

# === 数据库 ===
mysql -u root -p  # 输入 Aa2485204216

# === 防火墙 ===
firewall-cmd --list-services      # 查看开放服务
firewall-cmd --add-service=https --permanent && firewall-cmd --reload

# === Git ===
cd /root/scratch-community-platform
git status
git log --oneline -5
```

---

## 🧪 API 快速测试

```bash
# 1. 健康检查
curl http://8.134.252.67/actuator/health

# 2. 用户注册
curl -X POST http://8.134.252.67/api/v1/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","nickname":"测试用户","password":"Test1234!","confirmPassword":"Test1234!"}'

# 3. 用户登录
curl -X POST http://8.134.252.67/api/v1/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Test1234!"}'

# 4. 获取个人资料（需要 token）
TOKEN="eyJ..."
curl http://8.134.252.67/api/v1/user/profile -H "Authorization: Bearer $TOKEN"
```

---

## ⚠️ 安全提醒

1. **生产环境必须修改**：
   - `JWT_SECRET` / `JWT_REFRESH_SECRET` 随机生成（已配置强密钥）
   - MinIO 默认凭证 `minioadmin/minioadmin` → 修改为高强度密码
   - MySQL `scratch` 用户密码 `scratch123` → 改为随机字符串

2. **防火墙**：仅开放 22/80/443，8080 不对外暴露

3. **HTTPS**：待域名解析后配置 Let's Encrypt 证书

4. **备份策略**：建议每日备份 MySQL 和 MinIO 数据

---

## 📊 服务状态总览

```
✅ Nginx       : active  (0.0.0.0:80)
✅ Spring Boot : active  (*:8080)  JWT 87B + MinIO connected
✅ MySQL       : active  (3306)    scratch_community
✅ Redis       : active  (6379)
✅ MinIO       : active  (9000/9001)  3 buckets created
✅ Firewall    : active  (services: http https ssh)
✅ Systemd     : enabled (scratch-app auto-restart)
```

---

**最后更新**: 2026-04-30 00:52 (commit `d4c32e0`)
