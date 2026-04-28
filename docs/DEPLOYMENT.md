# 🚢 部署指南

> 版本：v2.0 | 日期：2026-04-25
> 生产环境部署说明

---

## 一、环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 推荐 Eclipse Temurin |
| Node.js | 18+ | LTS 版本 |
| Docker | 24+ | 含 Docker Compose V2 |
| Maven | 3.8+ | 构建后端 |

---

## 二、开发环境部署

最简单的方式，使用 Docker Compose 一键启动：

```bash
# 克隆项目
git clone https://github.com/icedcoke23/scratch-community-platform.git
cd scratch-community-platform

# 启动所有服务
cd docker
docker-compose up -d

# 查看状态
docker-compose ps

# 查看日志
docker-compose logs -f backend
```

服务地址：
- 前端：http://localhost:3000
- 后端 API：http://localhost:8080
- Swagger UI：http://localhost:8080/swagger-ui.html
- MinIO 控制台：http://localhost:9001

---

## 三、生产环境部署

### 3.1 安全配置

⚠️ **部署前必须修改以下配置**：

```bash
# 1. 创建 .env 文件
cp .env.example .env

# 2. 修改敏感配置
# - MYSQL_ROOT_PASSWORD：强密码
# - JWT_SECRET：至少 32 字节的随机字符串
# - MINIO_ROOT_USER/PASSWORD：MinIO 管理员密码
vim .env
```

### 3.2 Docker Compose 生产配置

创建 `docker/docker-compose.prod.yml`：

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: scratch_community
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
    # 生产环境不暴露端口到宿主机
    # ports:
    #   - "3306:3306"
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD}
    restart: unless-stopped

  minio:
    image: minio/minio:latest
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD}
    volumes:
      - minio_data:/data
    command: server /data --console-address ":9001"
    restart: unless-stopped

  backend:
    build:
      context: ../backend
      dockerfile: ../docker/Dockerfile.backend
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/scratch_community?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PASSWORD: ${REDIS_PASSWORD}
      MINIO_ENDPOINT: http://minio:9000
      MINIO_ACCESS_KEY: ${MINIO_ROOT_USER}
      MINIO_SECRET_KEY: ${MINIO_ROOT_PASSWORD}
      SANDBOX_URL: http://sandbox:8081
      JWT_SECRET: ${JWT_SECRET}
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
      minio:
        condition: service_healthy
    restart: unless-stopped

  sandbox:
    build:
      context: ../sandbox
      dockerfile: Dockerfile
    environment:
      REDIS_URL: redis://:${REDIS_PASSWORD}@redis:6379
      PORT: "8081"
    depends_on:
      redis:
        condition: service_healthy
    restart: unless-stopped

  frontend:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ../frontend:/usr/share/nginx/html:ro
      - ./nginx.prod.conf:/etc/nginx/conf.d/default.conf:ro
      - /etc/letsencrypt:/etc/letsencrypt:ro  # HTTPS 证书
    depends_on:
      - backend
    restart: unless-stopped

volumes:
  mysql_data:
  redis_data:
  minio_data:
```

### 3.3 Nginx 生产配置

创建 `docker/nginx.prod.conf`：

```nginx
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    root /usr/share/nginx/html;
    index index.html;

    # 前端
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 300s;
    }

    # Swagger UI（生产环境建议限制访问）
    location /swagger-ui/ {
        # allow 10.0.0.0/8;
        # deny all;
        proxy_pass http://backend:8080;
    }

    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 7d;
        add_header Cache-Control "public, immutable";
    }

    # 文件上传大小限制
    client_max_body_size 50m;
}
```

### 3.4 启动

```bash
cd docker

# 使用生产配置启动
docker-compose -f docker-compose.prod.yml --env-file ../.env up -d

# 查看状态
docker-compose -f docker-compose.prod.yml ps

# 查看日志
docker-compose -f docker-compose.prod.yml logs -f backend
```

---

## 四、数据库管理

### 4.1 Flyway 迁移

项目使用 Flyway 管理数据库版本，启动时自动执行迁移：

```
backend/scratch-app/src/main/resources/db/migration/
├── V1__init_schema.sql
├── V2__init_data.sql
├── V3__point_remix_analytics.sql
├── V4__ai_review.sql
└── V5__competition.sql
```

### 4.2 手动备份

```bash
# 备份
docker exec scratch-mysql mysqldump -u root -p scratch_community > backup_$(date +%Y%m%d).sql

# 恢复
docker exec -i scratch-mysql mysql -u root -p scratch_community < backup_20260424.sql
```

### 4.3 定时备份（推荐）

```bash
# 添加 crontab
crontab -e

# 每天凌晨 3 点备份
0 3 * * * docker exec scratch-mysql mysqldump -u root -pYOUR_PASSWORD scratch_community | gzip > /backup/scratch_$(date +\%Y\%m\%d).sql.gz
```

---

## 五、监控

### 5.1 健康检查

```bash
# 后端
curl http://localhost:8080/api/health

# 沙箱
curl http://localhost:8081/health
```

### 5.2 日志查看

```bash
# Docker 日志
docker-compose logs -f backend
docker-compose logs -f sandbox

# 应用日志（挂载卷）
tail -f /var/log/scratch/backend.log
```

---

## 六、常见问题

### 后端启动失败

```bash
# 检查数据库连接
docker exec scratch-mysql mysqladmin -u root -p ping

# 检查端口占用
lsof -i :8080

# 查看详细日志
docker-compose logs backend | tail -50
```

### 沙箱判题超时

```bash
# 检查 Redis 连接
docker exec scratch-redis redis-cli ping

# 检查沙箱日志
docker-compose logs sandbox | grep -i error
```

### 磁盘空间不足

```bash
# 清理 Docker 无用资源
docker system prune -a

# 检查 MinIO 存储
du -sh /var/lib/docker/volumes/scratch-community-platform_minio_data/
```
