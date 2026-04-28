# 🚀 生产环境部署指南

> 版本：v1.0 | 日期：2026-04-28
> 适用于：Scratch Community Platform v3.1+

---

## 目录

1. [环境要求](#1-环境要求)
2. [部署前准备](#2-部署前准备)
3. [部署步骤](#3-部署步骤)
4. [配置说明](#4-配置说明)
5. [监控与告警](#5-监控与告警)
6. [备份与恢复](#6-备份与恢复)
7. [常见问题](#7-常见问题)

---

## 1. 环境要求

### 硬件要求

| 资源 | 最低配置 | 推荐配置 |
|------|----------|----------|
| CPU | 2 核 | 4 核 |
| 内存 | 4 GB | 8 GB |
| 磁盘 | 50 GB SSD | 100 GB SSD |
| 带宽 | 5 Mbps | 10 Mbps |

### 软件要求

| 软件 | 版本 |
|------|------|
| 操作系统 | Ubuntu 22.04 LTS / CentOS 8+ |
| Docker | 24.0+ |
| Docker Compose | 2.20+ |
| Git | 2.30+ |

---

## 2. 部署前准备

### 2.1 域名准备

1. 注册域名（推荐：阿里云万网、腾讯云 DNSPod）
2. 配置 DNS 解析，添加 A 记录指向服务器 IP
3. 等待 DNS 生效（通常 10 分钟 - 2 小时）

### 2.2 服务器准备

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装 Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 验证安装
docker --version
docker-compose --version
```

### 2.3 安装 Certbot (Let's Encrypt)

```bash
# 安装 Certbot
sudo apt install certbot -y

# 申请证书
sudo certbot certonly --standalone -d your-domain.com -d www.your-domain.com

# 证书会保存在 /etc/letsencrypt/live/your-domain.com/
```

---

## 3. 部署步骤

### 3.1 克隆代码

```bash
# 克隆仓库
git clone https://github.com/icedcoke23/scratch-community-platform.git
cd scratch-community-platform
```

### 3.2 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑环境变量
vim .env
```

**必须配置的环境变量：**

```bash
# 数据库
MYSQL_ROOT_PASSWORD=your_strong_password_here

# Redis
REDIS_PASSWORD=your_redis_password_here

# MinIO
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=your_minio_password_here

# JWT (使用 openssl rand -base64 32 生成)
JWT_SECRET=your_jwt_secret_here

# CORS (生产域名)
CORS_ALLOWED_ORIGINS=https://your-domain.com,https://www.your-domain.com

# 端口配置
MYSQL_PORT=3306
REDIS_PORT=6379
MINIO_API_PORT=9000
MINIO_CONSOLE_PORT=9001
BACKEND_PORT=8080
SANDBOX_PORT=8081
FRONTEND_PORT=3000
```

### 3.3 配置 Nginx (HTTPS)

创建 `docker/nginx-prod.conf`：

```nginx
upstream backend {
    server backend:8080;
    keepalive 32;
}

upstream sandbox {
    server sandbox:8081;
    keepalive 16;
}

# HTTP → HTTPS 重定向
server {
    listen 80;
    server_name your-domain.com www.your-domain.com;
    return 301 https://$server_name$request_uri;
}

# HTTPS 主站
server {
    listen 443 ssl http2;
    server_name your-domain.com www.your-domain.com;

    # SSL 证书
    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
    ssl_trusted_certificate /etc/letsencrypt/live/your-domain.com/chain.pem;

    # SSL 安全配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # HSTS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Gzip
    gzip on;
    gzip_vary on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

    # 前端
    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
        
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }

    # 后端 API
    location /api/ {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 120s;
    }

    # SSE
    location /api/v1/ai-review/ {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_buffering off;
        proxy_read_timeout 3600s;
    }

    # 健康检查
    location /health {
        proxy_pass http://backend/api/health;
    }

    # Actuator (仅内网)
    location /actuator/ {
        allow 10.0.0.0/8;
        allow 172.16.0.0/12;
        allow 192.168.0.0/16;
        allow 127.0.0.1;
        deny all;
        
        proxy_pass http://backend;
    }

    # 文件上传限制
    client_max_body_size 100M;
}
```

### 3.4 启动服务

```bash
# 构建并启动所有服务
docker-compose -f docker/docker-compose.yml up -d --build

# 查看服务状态
docker-compose -f docker/docker-compose.yml ps

# 查看日志
docker-compose -f docker/docker-compose.yml logs -f
```

### 3.5 初始化数据库

```bash
# 数据库会自动通过 Flyway 迁移初始化
# 检查迁移状态
docker exec scratch-backend java -jar app.jar --spring.flyway.info=true
```

---

## 4. 配置说明

### 4.1 环境变量说明

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 | 必填 |
| `REDIS_PASSWORD` | Redis 密码 | 必填 |
| `MINIO_ROOT_USER` | MinIO 管理员用户名 | 必填 |
| `MINIO_ROOT_PASSWORD` | MinIO 管理员密码 | 必填 |
| `JWT_SECRET` | JWT 签名密钥 (32字节) | 必填 |
| `CORS_ALLOWED_ORIGINS` | CORS 允许的域名 | `https://*.scratch-community.com` |
| `AI_ENABLED` | 是否启用 AI 功能 | `false` |
| `AI_API_KEY` | AI API Key | 可选 |

### 4.2 性能调优

**JVM 参数 (在 .env 中配置)：**
```bash
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

**MySQL 参数 (在 docker-compose.yml 中配置)：**
```yaml
command: >
  --character-set-server=utf8mb4
  --collation-server=utf8mb4_unicode_ci
  --default-authentication-plugin=mysql_native_password
  --innodb-buffer-pool-size=256M
  --max-connections=200
  --query-cache-type=OFF
```

---

## 5. 监控与告警

### 5.1 Prometheus 配置

创建 `docker/prometheus.yml`：

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'scratch-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']
        labels:
          application: 'scratch-community'
```

### 5.2 Grafana 配置

1. 访问 `http://your-server:3000` (Grafana 默认端口)
2. 添加 Prometheus 数据源：`http://prometheus:9090`
3. 导入 Spring Boot Dashboard (ID: 12900)

### 5.3 关键指标

| 指标 | 说明 | 告警阈值 |
|------|------|----------|
| `http_server_requests_seconds` | HTTP 请求延迟 | P99 > 5s |
| `jvm_memory_used_bytes` | JVM 内存使用 | > 80% |
| `scratch_user_register_total` | 用户注册数 | 监控趋势 |
| `scratch_judge_submit_total` | 判题提交数 | 监控趋势 |
| `scratch_judge_duration_seconds` | 判题耗时 | P95 > 30s |

---

## 6. 备份与恢复

### 6.1 自动备份

```bash
# 添加定时任务 (每天凌晨 3 点备份)
crontab -e

# 添加以下行：
0 3 * * * /path/to/scripts/backup-database.sh prod >> /var/log/scratch-backup.log 2>&1
```

### 6.2 手动备份

```bash
# 执行备份
./scripts/backup-database.sh prod

# 备份文件保存在 /var/backups/scratch-community/
```

### 6.3 恢复数据库

```bash
# 解压备份
gunzip /var/backups/scratch-community/scratch_community_prod_20260428_030000.sql.gz

# 恢复到数据库
docker exec -i scratch-mysql mysql -u root -p"${MYSQL_ROOT_PASSWORD}" scratch_community < backup.sql
```

---

## 7. 常见问题

### Q1: 服务启动失败

```bash
# 查看日志
docker-compose logs backend

# 常见原因：
# 1. 数据库未就绪 - 等待 MySQL 健康检查通过
# 2. 端口冲突 - 检查端口占用
# 3. 环境变量未设置 - 检查 .env 文件
```

### Q2: 无法访问 HTTPS

```bash
# 检查证书
sudo certbot certificates

# 续期证书
sudo certbot renew

# 检查 Nginx 配置
docker exec scratch-frontend nginx -t
```

### Q3: 数据库连接失败

```bash
# 检查 MySQL 状态
docker exec scratch-mysql mysqladmin ping

# 检查连接
docker exec scratch-backend curl -f http://localhost:8080/api/health
```

### Q4: 磁盘空间不足

```bash
# 清理 Docker 缓存
docker system prune -a

# 清理旧备份
find /var/backups/scratch-community -mtime +30 -delete

# 检查磁盘使用
df -h
```

---

## 附录：部署检查清单

- [ ] 域名已解析到服务器 IP
- [ ] SSL 证书已申请并配置
- [ ] .env 文件已配置所有必填项
- [ ] JWT_SECRET 已生成 (openssl rand -base64 32)
- [ ] 数据库密码已设置强密码
- [ ] MinIO 凭据已配置
- [ ] CORS 域名已配置
- [ ] 防火墙已开放 80/443 端口
- [ ] Docker 服务已启动
- [ ] 数据库迁移已执行
- [ ] 健康检查已通过
- [ ] 监控已配置
- [ ] 备份已设置

---

*本指南随项目更新持续维护。*
