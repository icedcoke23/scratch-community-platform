# Scratch 社区平台部署指南

本文档提供了 Scratch 社区平台的完整部署指南，涵盖从环境准备到生产部署的全部流程，以及日常运维所需的监控、日志和故障处理方法。通过遵循本指南，运维人员可以在各种环境中成功部署和运行平台，同时确保系统的安全性和可靠性。

## 环境要求与准备

### 硬件环境要求

部署 Scratch 社区平台对服务器硬件有一定的要求，根据预期的用户规模和访问量，需要选择合适的服务器配置。对于开发和测试环境，可以使用较低配置的服务器，甚至在本地机器上运行即可。但对于生产环境，建议按照以下规格选择服务器配置。

开发测试环境的最低要求包括：CPU 至少 2 核心，推荐 4 核心以获得更好的编译性能；内存至少 4GB，推荐 8GB 以上，因为 TurboWarp 的构建过程需要消耗大量内存；硬盘空间至少 20GB，推荐 50GB 以上，用于存储项目文件、日志和备份数据；网络带宽至少 10Mbps，确保用户能够流畅访问和上传下载项目文件。

小型生产环境（支持 100-500 并发用户）的推荐配置为：CPU 4-8 核心，内存 8-16GB，硬盘 100GB SSD，网络带宽 50Mbps。中型生产环境（支持 500-2000 并发用户）建议配置：CPU 8-16 核心，内存 16-32GB，硬盘 200GB SSD，网络带宽 100Mbps。大型生产环境需要根据实际负载进行评估和扩展，可能需要负载均衡、多台应用服务器和独立的数据库服务器。

### 软件环境要求

服务器操作系统推荐使用 Ubuntu 20.04 LTS 或更高版本，也可以使用 CentOS 8、Debian 11 等主流 Linux 发行版。本文档中的命令示例主要针对 Ubuntu/Debian 系统，其他系统的命令可能略有差异。

Node.js 是运行后端服务的核心依赖，必须安装稳定版本。平台要求 Node.js 版本不低于 18.0.0，推荐使用 20.x LTS 版本以获得更好的性能和稳定性。Node.js 可以通过 nvm（Node Version Manager）进行安装和管理，这样可以方便地切换不同版本并避免全局安装带来的权限问题。

```bash
# 安装 nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash

# 加载 nvm
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"

# 安装 Node.js LTS 版本
nvm install --lts
nvm use --lts
nvm alias default --lts

# 验证安装
node --version
npm --version
```

Git 是代码版本控制工具，也是获取平台源码的必要工具。大多数 Linux 发行版已经预装了 Git，如果没有可以通过包管理器安装。

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install git

# CentOS/RHEL
sudo yum install git

# 验证安装
git --version
```

Nginx 是生产环境必需的 Web 服务器，用于提供静态文件服务、反向代理和负载均衡。Nginx 的安装和配置将在后续章节详细说明。

Docker 和 Docker Compose 是可选组件，如果采用容器化部署方式则需要安装。对于不熟悉容器技术的团队，建议使用传统部署方式；如果需要快速部署和扩展，容器化方式更为合适。

### 网络与安全要求

服务器需要配置固定的内网 IP 地址，并确保防火墙正确开放了所需的端口。平台需要开放的端口包括：80 端口用于 HTTP 访问，443 端口用于 HTTPS 访问（生产环境必需），8080 端口用于后端服务（仅内网访问，不需要对外暴露）。

生产环境强烈建议配置域名并申请 SSL 证书。使用 Let's Encrypt 可以免费获取受信任的 SSL 证书，支持自动续期。域名的 DNS 需要正确配置 A 记录或 CNAME 记录指向服务器 IP。

安全方面需要注意的是，服务器应该启用防火墙并限制 SSH 访问来源；定期更新系统软件包以修复安全漏洞；使用强密码或 SSH 密钥进行认证；配置入侵检测和日志审计。

## 开发环境部署

### 源码获取与依赖安装

开发环境的部署相对简单，适合开发者本地调试和新功能测试。首先需要从代码仓库获取平台源码，如果还没有克隆过仓库，可以使用以下命令：

```bash
# 克隆代码仓库
git clone https://github.com/your-repo/scratch-community-platform.git
cd scratch-community-platform

# 如果需要切换到开发分支
git checkout develop
```

获取源码后，分别安装后端和前端的依赖包。后端依赖包括 Express.js 框架、CORS 中间件、文件上传处理库等；前端依赖包括 Vue 3 核心库、Vue Router、Pinia 状态管理、Element Plus 组件库等。

```bash
# 安装后端依赖
cd backend
npm install

# 返回项目根目录
cd ..

# 安装前端依赖
cd frontend-vue
npm install

# 返回项目根目录
cd ..
```

依赖安装过程可能需要几分钟时间，取决于网络速度和服务器性能。如果遇到网络问题，可以考虑使用国内镜像源加速：

```bash
# 使用淘宝镜像
npm config set registry https://registry.npmmirror.com
npm install
```

### TurboWarp 构建

TurboWarp 是平台的核心编辑器组件，需要从源码构建后才能集成到平台中。构建过程会下载 TurboWarp 的源码、安装依赖、执行生产构建，然后将构建产物复制到平台的前端目录。项目提供了自动化构建脚本，可以一键完成整个流程。

```bash
# 赋予构建脚本执行权限
chmod +x scripts/build-turbowarp.sh

# 执行构建
./scripts/build-turbowarp.sh
```

首次构建会下载大量资源，可能需要较长时间（取决于网络速度，通常需要 10-30 分钟）。构建过程中可以看到详细的进度输出，包括依赖安装、代码编译、资源复制等步骤。如果构建过程中断，可以重新执行构建脚本，已下载的资源会被复用。

构建完成后，可以在 `frontend-vue/public/turbowarp/` 目录下看到 TurboWarp 的所有静态文件，包括 editor.html、player.html 和 static 目录下的 JavaScript、CSS 等资源。

### 服务启动与验证

开发环境需要同时运行后端服务和前端开发服务器。后端服务提供 API 接口，前端开发服务器提供热更新的前端界面。两个服务需要在不同的终端中运行，或者使用进程管理工具统一管理。

启动后端服务：

```bash
cd backend
npm run dev
```

后端服务默认监听 8080 端口。服务启动后会输出日志信息，包括监听地址和健康检查端点。可以通过访问 `http://localhost:8080/health` 验证服务是否正常运行，正常的响应应该返回 JSON 格式的状态信息。

启动前端开发服务器：

```bash
cd frontend-vue
npm run dev
```

前端开发服务器默认监听 5173 端口（Vite 默认端口）。服务启动后会自动打开浏览器窗口，如果没有自动打开，可以手动访问显示的地址。Vite 支持热模块替换（HMR），修改代码后浏览器会自动刷新页面，无需手动重启。

如果希望前端能够访问后端 API，需要配置代理。Vite 支持在配置文件中设置代理，将特定路径的请求转发到后端服务：

```typescript
// frontend-vue/vite.config.ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

配置代理后，前端发往 `/api` 路径的请求会被自动转发到后端服务，无需处理跨域问题。

## 生产环境部署

### 服务器初始化

生产环境部署前，需要对服务器进行初始化配置，包括创建部署目录、设置权限、配置环境变量等操作。假设使用 `/var/www/scratch-platform` 作为部署目录。

```bash
# 创建项目目录结构
sudo mkdir -p /var/www/scratch-platform
sudo mkdir -p /var/www/scratch-platform/logs
sudo mkdir -p /var/www/scratch-platform/backups

# 设置目录所有者
sudo chown -R www-data:www-data /var/www/scratch-platform

# 创建符号链接（可选，方便更新）
sudo ln -s /var/www/scratch-platform /opt/scratch-platform
```

创建后端服务的环境配置文件：

```bash
# 创建环境变量文件
sudo nano /var/www/scratch-platform/.env
```

环境变量文件内容示例：

```bash
# 服务配置
PORT=8080
NODE_ENV=production

# 跨域配置（生产环境应限制为具体域名）
CORS_ORIGIN=https://your-domain.com

# 日志级别
LOG_LEVEL=info
```

### 后端服务构建与部署

后端服务需要先构建成 JavaScript 代码，然后才能在生产环境运行。构建过程会编译 TypeScript 代码并进行优化处理。

```bash
cd backend

# 安装生产依赖
npm install --production

# 编译 TypeScript
npm run build
```

构建完成后，将构建产物复制到部署目录：

```bash
# 复制构建产物
sudo cp -r dist /var/www/scratch-platform/backend/
sudo cp package.json /var/www/scratch-platform/backend/
sudo cp .env /var/www/scratch-platform/backend/

# 设置目录权限
sudo chown -R www-data:www-data /var/www/scratch-platform/backend
```

### 前端服务构建与部署

前端服务需要构建成静态文件，由 Nginx 直接提供服务。构建过程会打包所有资源并生成优化后的文件。

```bash
cd frontend-vue

# 安装依赖
npm install

# 复制 TurboWarp 文件到 public 目录
# 如果尚未构建 TurboWarp，执行以下命令
chmod +x ../scripts/build-turbowarp.sh
../scripts/build-turbowarp.sh

# 构建生产版本
npm run build
```

构建完成后，将构建产物复制到部署目录：

```bash
# 复制前端构建产物
sudo cp -r dist /var/www/scratch-platform/frontend/

# 复制 TurboWarp 资源
sudo cp -r public/turbowarp /var/www/scratch-platform/

# 设置目录权限
sudo chown -R www-data:www-data /var/www/scratch-platform/frontend
sudo chown -R www-data:www-data /var/www/scratch-platform/turbowarp
```

### 使用 PM2 管理进程

生产环境中，后端服务应该使用进程管理器运行，以便实现自动重启、日志管理和负载均衡等功能。PM2 是 Node.js 生态中最流行的进程管理器，提供了完善的进程管理能力。

```bash
# 全局安装 PM2
npm install -g pm2

# 创建 PM2 配置文件
sudo nano /var/www/scratch-platform/ecosystem.config.js
```

配置文件内容：

```javascript
module.exports = {
  apps: [{
    name: 'scratch-backend',
    script: 'dist/index.js',
    cwd: '/var/www/scratch-platform/backend',
    instances: 1,
    autorestart: true,
    watch: false,
    max_memory_restart: '1G',
    env: {
      NODE_ENV: 'production',
      PORT: 8080
    },
    env_production: {
      NODE_ENV: 'production',
      PORT: 8080
    },
    error_file: '/var/www/scratch-platform/logs/pm2-error.log',
    out_file: '/var/www/scratch-platform/logs/pm2-out.log',
    log_file: '/var/www/scratch-platform/logs/pm2-combined.log',
    time: true
  }]
};
```

启动后端服务：

```bash
# 使用配置文件启动
cd /var/www/scratch-platform
pm2 start ecosystem.config.js

# 设置开机自启
pm2 startup
pm2 save

# 查看服务状态
pm2 status
pm2 logs scratch-backend
```

常用 PM2 命令：

```bash
# 查看日志
pm2 logs scratch-backend

# 重启服务
pm2 restart scratch-backend

# 停止服务
pm2 stop scratch-backend

# 删除服务
pm2 delete scratch-backend

# 查看实时监控
pm2 monit
```

## Docker 部署

### Docker 环境准备

容器化部署可以简化环境配置和部署流程，适合需要快速扩展或在不同环境中迁移的项目。首先需要在服务器上安装 Docker 和 Docker Compose。

```bash
# 安装 Docker
curl -fsSL https://get.docker.com | sudo sh

# 启动 Docker 服务
sudo systemctl start docker
sudo systemctl enable docker

# 添加当前用户到 docker 用户组（避免每次使用 sudo）
sudo usermod -aG docker $USER

# 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 验证安装
docker --version
docker-compose --version
```

### Docker 配置文件

在项目根目录创建 Docker 相关配置文件。首先是 Dockerfile，用于构建后端和前端的 Docker 镜像。

```dockerfile
# 后端 Dockerfile
FROM node:20-alpine

WORKDIR /app

COPY backend/package*.json ./
RUN npm install --production

COPY backend/dist ./dist
COPY backend/.env ./

EXPOSE 8080

CMD ["node", "dist/index.js"]
```

```dockerfile
# 前端 Dockerfile
FROM node:20-alpine AS builder

WORKDIR /app

COPY frontend-vue/package*.json ./
RUN npm install

COPY frontend-vue ./frontend-vue
COPY scripts ./scripts

RUN chmod +x scripts/build-turbowarp.sh
RUN ./scripts/build-turbowarp.sh

RUN cd frontend-vue && npm run build

FROM nginx:alpine

COPY --from=builder /app/frontend-vue/dist /usr/share/nginx/html
COPY --from=builder /app/frontend-vue/public/turbowarp /usr/share/nginx/html/turbowarp
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

创建 docker-compose.yml 文件，定义服务编排：

```yaml
version: '3.8'

services:
  backend:
    build:
      context: .
      dockerfile: Dockerfile.backend
    container_name: scratch-backend
    restart: unless-stopped
    environment:
      - NODE_ENV=production
      - PORT=8080
    volumes:
      - ./logs/backend:/app/logs
    networks:
      - scratch-network

  frontend:
    build:
      context: .
      dockerfile: Dockerfile.frontend
    container_name: scratch-frontend
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
    depends_on:
      - backend
    networks:
      - scratch-network

networks:
  scratch-network:
    driver: bridge
```

### Docker 部署流程

使用 Docker 部署的完整流程：

```bash
# 构建并启动所有服务
docker-compose up -d --build

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f backend
docker-compose logs -f frontend

# 停止服务
docker-compose down

# 重新构建并启动
docker-compose up -d --build --force-recreate

# 进入容器调试
docker exec -it scratch-backend sh
```

Docker 部署的优势在于环境一致性高，部署简单快速。但需要注意数据持久化问题，当前配置中日志目录做了数据卷挂载，如果需要持久化存储项目数据，还需要添加相应的数据卷配置。

## Nginx 配置详解

### 基本配置

Nginx 是生产环境的核心组件，负责提供静态文件服务、反向代理到后端服务、处理 HTTPS 等功能。以下是平台的基本 Nginx 配置：

```nginx
# /etc/nginx/sites-available/scratch-platform

server {
    listen 80;
    server_name your-domain.com www.your-domain.com;
    
    # 重定向到 HTTPS（稍后配置）
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com www.your-domain.com;
    
    # SSL 证书配置（稍后添加）
    
    # 字符集和语言
    charset utf-8;
    
    # 根目录
    root /var/www/scratch-platform/frontend;
    index index.html;
    
    # 访问日志
    access_log /var/www/scratch-platform/logs/nginx-access.log;
    error_log /var/www/scratch-platform/logs/nginx-error.log;
    
    # Gzip 压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_proxied any;
    gzip_types text/plain text/css text/xml text/javascript application/javascript application/json application/xml;
    
    # 前端路由支持（SPA）
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # TurboWarp 静态资源
    location /turbowarp/ {
        alias /var/www/scratch-platform/turbowarp/;
        expires 30d;
        add_header Cache-Control "public, immutable";
        
        # 禁止访问隐藏文件
        location ~ /\. {
            deny all;
        }
    }
    
    # API 反向代理
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        
        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # CORS 头（如果后端未配置）
        add_header Access-Control-Allow-Origin $http_origin always;
        add_header Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS" always;
        add_header Access-Control-Allow-Headers "Content-Type, Authorization" always;
        add_header Access-Control-Allow-Credentials "true" always;
        
        # 预检请求处理
        if ($request_method = 'OPTIONS') {
            add_header Access-Control-Allow-Origin $http_origin;
            add_header Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS";
            add_header Access-Control-Allow-Headers "Content-Type, Authorization";
            add_header Access-Control-Allow-Credentials "true";
            add_header Access-Control-Max-Age 1728000;
            add_header Content-Type 'text/plain charset=UTF-8';
            add_header Content-Length 0;
            return 204;
        }
    }
    
    # 健康检查端点
    location /health {
        proxy_pass http://localhost:8080/health;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
    }
    
    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
        access_log off;
    }
    
    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    
    # 禁止访问隐藏文件
    location ~ /\. {
        deny all;
    }
    
    # 禁止访问敏感文件
    location ~* \.(env|git|htaccess|htpasswd|ini|log|sh|sql|conf|bak)$ {
        deny all;
    }
}
```

### 配置启用与验证

将配置链接到 sites-enabled 目录并验证：

```bash
# 创建符号链接
sudo ln -s /etc/nginx/sites-available/scratch-platform /etc/nginx/sites-enabled/

# 删除默认站点（如果有）
sudo rm /etc/nginx/sites-enabled/default

# 测试配置语法
sudo nginx -t

# 重载 Nginx
sudo systemctl reload nginx
```

### 静态资源优化配置

对于 TurboWarp 编辑器等大型静态资源，可以进行更精细的优化配置：

```nginx
# TurboWarp 静态资源配置
location /turbowarp/ {
    alias /var/www/scratch-platform/turbowarp/;
    
    # 长期缓存
    expires 365d;
    add_header Cache-Control "public, immutable";
    
    # 启用 Brotli 压缩（如果已安装 ngx_brotli 模块）
    brotli on;
    brotli_types text/css application/javascript;
    brotli_comp_level 6;
    
    # 允许跨域访问（TurboWarp 需要）
    add_header Access-Control-Allow-Origin "*";
    
    # 关闭访问日志以提高性能
    access_log off;
    
    # 最大文件大小
    max_size 100m;
}
```

## HTTPS 配置

### SSL 证书申请

生产环境必须使用 HTTPS 加密传输数据。使用 Let's Encrypt 可以免费获取受信任的 SSL 证书：

```bash
# 安装 Certbot
sudo apt update
sudo apt install certbot python3-certbot-nginx

# 申请证书
sudo certbot --nginx -d your-domain.com -d www.your-domain.com

# 按提示输入邮箱地址并同意服务条款
# 选择是否自动重定向 HTTP 到 HTTPS（建议选择 2 自动重定向）
```

Certbot 会自动修改 Nginx 配置添加 SSL 相关设置。申请成功后，证书会自动续期，无需手动处理。

如果需要手动配置 SSL（不推荐），可以使用以下配置：

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;
    
    # SSL 证书路径
    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
    
    # SSL 配置
    ssl_session_timeout 1d;
    ssl_session_cache shared:SSL:50m;
    ssl_session_tickets off;
    
    # 现代加密套件
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    
    # HSTS
    add_header Strict-Transport-Security "max-age=63072000" always;
    
    # OCSP Stapling
    ssl_stapling on;
    ssl_stapling_verify on;
    resolver 8.8.8.8 8.8.4.4 valid=300s;
    resolver_timeout 5s;
}
```

### 证书自动续期

Let's Encrypt 证书有效期为 90 天，Certbot 会自动配置定时任务进行续期。可以通过以下命令测试自动续期是否正常工作：

```bash
# 测试续期（不会实际续期）
sudo certbot renew --dry-run

# 查看续期定时任务
sudo systemctl list-timers | grep certbot
```

## 性能优化

### Nginx 性能优化

调整 Nginx 工作进程数和连接配置可以提升性能：

```nginx
# /etc/nginx/nginx.conf

user www-data;
worker_processes auto;
pid /run/nginx.pid;

events {
    worker_connections 1024;
    multi_accept on;
    use epoll;
}

http {
    # 基本设置
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;
    
    # 文件缓存
    open_file_cache max=10000 inactive=30s;
    open_file_cache_valid 60s;
    open_file_cache_min_uses 2;
    open_file_cache_errors on;
    
    # Gzip 压缩
    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml application/json application/javascript application/xml application/xml+rss text/javascript application/x-javascript;
    
    # 缓冲区设置
    client_body_buffer_size 10k;
    client_max_body_size 50m;
    proxy_buffer_size 128k;
    proxy_buffers 4 256k;
    proxy_busy_buffers_size 256k;
}
```

### 后端服务优化

PM2 可以在多核服务器上运行多个实例以提高并发处理能力：

```bash
# 使用 4 个实例（根据 CPU 核心数调整）
pm2 start ecosystem.config.js -i 4

# 查看负载均衡状态
pm2 list
pm2 show scratch-backend
```

修改 PM2 配置启用集群模式：

```javascript
module.exports = {
  apps: [{
    name: 'scratch-backend',
    script: 'dist/index.js',
    cwd: '/var/www/scratch-platform/backend',
    instances: 4,
    exec_mode: 'cluster',
    autorestart: true,
    watch: false,
    max_memory_restart: '1G',
    env: {
      NODE_ENV: 'production',
      PORT: 8080
    }
  }]
};
```

### 数据库优化（如后续迁移到数据库）

如果后续将内存存储迁移到 PostgreSQL 或 MySQL 等数据库，需要进行以下优化：

选择合适的数据库引擎和配置参数；创建适当的索引以加速查询；配置连接池避免频繁创建连接；定期进行维护操作如 VACUUM、OPTIMIZE TABLE；配置主从复制实现读写分离和故障转移。

## 监控与日志

### 日志管理

平台会产生多种日志，包括 Nginx 访问日志、Nginx 错误日志、后端服务日志和 PM2 日志。建立统一的日志管理策略对于问题排查和性能分析至关重要。

日志轮转配置（使用 logrotate）：

```bash
# /etc/logrotate.d/scratch-platform
/var/www/scratch-platform/logs/* {
    daily
    missingok
    rotate 14
    compress
    delaycompress
    notifempty
    create 0640 www-data adm
    sharedscripts
    postrotate
        [ -f /var/run/nginx.pid ] && kill -USR1 `cat /var/run/nginx.pid`
        pm2 reloadLogs
    endscript
}
```

### 监控配置

可以使用多种工具监控平台运行状态。以下是使用 PM2 内置监控和 Node.js 监控工具的方案：

```bash
# PM2 Plus 监控（需要注册账号）
pm2 link <key> <id>

# 或使用开源方案 Prometheus + Grafana
# 安装 node_exporter
docker run -d \
  --name node_exporter \
  -p 9100:9100 \
  prom/node-exporter

# 安装 Prometheus 配置文件
cat > /etc/prometheus/prometheus.yml << 'EOF'
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'scratch-backend'
    static_configs:
      - targets: ['localhost:8080']
EOF
```

### 健康检查

配置健康检查脚本定期验证服务状态：

```bash
# 创建健康检查脚本
cat > /usr/local/bin/check-scratch.sh << 'EOF'
#!/bin/bash

# 检查 Nginx
if ! systemctl is-active --quiet nginx; then
    echo "Nginx is not running"
    systemctl restart nginx
fi

# 检查后端服务
if ! pm2 describe scratch-backend | grep -q "status.*online"; then
    echo "Backend is not running"
    pm2 restart scratch-backend
fi

# 检查 API 响应
if ! curl -sf http://localhost:8080/health > /dev/null; then
    echo "API health check failed"
    pm2 restart scratch-backend
fi

# 检查磁盘空间
USAGE=$(df /var/www | tail -1 | awk '{print $5}' | sed 's/%//')
if [ "$USAGE" -gt 90 ]; then
    echo "Disk usage is above 90%"
fi

# 检查内存使用
MEM=$(free | grep Mem | awk '{printf("%.0f"), $3/$2 * 100}')
if [ "$MEM" -gt 90 ]; then
    echo "Memory usage is above 90%"
fi
EOF

chmod +x /usr/local/bin/check-scratch.sh

# 添加到 crontab 每 5 分钟执行一次
(crontab -l 2>/dev/null; echo "*/5 * * * * /usr/local/bin/check-scratch.sh >> /var/www/scratch-platform/logs/health-check.log 2>&1") | crontab -
```

## 故障排查

### 常见问题与解决方案

服务无法启动时，首先检查端口占用情况：

```bash
# 检查端口是否被占用
sudo lsof -i :8080
sudo lsof -i :80

# 查看服务日志
pm2 logs scratch-backend
sudo tail -f /var/log/nginx/error.log
```

页面加载异常时，按以下顺序排查：检查浏览器控制台错误信息；检查 Nginx 配置是否正确；检查静态文件路径是否正确；检查文件权限是否正确设置。

API 请求失败时，首先验证后端服务是否正常运行：

```bash
# 检查后端健康状态
curl http://localhost:8080/health

# 检查 API 端点
curl -v http://localhost:8080/api/v1/project

# 检查 CORS 配置
curl -I -X OPTIONS http://localhost:8080/api/v1/project \
  -H "Origin: http://localhost" \
  -H "Access-Control-Request-Method: GET"
```

TurboWarp 无法加载时，检查以下方面：确认 TurboWarp 文件已正确复制到部署目录；检查文件权限是否允许 Nginx 读取；验证 iframe 的 sandbox 属性是否正确配置；检查浏览器控制台是否有安全策略阻止。

### 性能问题排查

响应缓慢时，可以使用以下命令分析：

```bash
# 查看 CPU 使用
top
htop

# 查看内存使用
free -h

# 查看 IO 等待
iostat -x 1

# 分析慢请求
tail -f /var/www/scratch-platform/logs/nginx-access.log | awk '{print $NF}' | sort | uniq -c | sort -rn | head
```

如果数据库成为瓶颈（后续迁移后），需要分析慢查询并添加索引。PM2 内存泄漏也是常见问题，可以使用以下命令监控：

```bash
# 监控内存使用趋势
pm2 monit

# 查看详细内存信息
pm2 show scratch-backend
```

### 数据备份与恢复

定期备份是防止数据丢失的重要措施：

```bash
# 创建备份脚本
cat > /usr/local/bin/backup-scratch.sh << 'EOF'
#!/bin/bash

BACKUP_DIR="/var/www/scratch-platform/backups"
DATE=$(date +%Y%m%d_%H%M%S)

# 备份 PM2 进程列表
pm2 save

# 备份配置
tar -czf $BACKUP_DIR/config_$DATE.tar.gz \
  /var/www/scratch-platform/.env \
  /etc/nginx/sites-available/scratch-platform \
  /var/www/scratch-platform/ecosystem.config.js

# 备份日志
tar -czf $BACKUP_DIR/logs_$DATE.tar.gz \
  /var/www/scratch-platform/logs

# 删除 7 天前的备份
find $BACKUP_DIR -name "*.tar.gz" -mtime +7 -delete

echo "Backup completed: $DATE"
EOF

chmod +x /usr/local/bin/backup-scratch.sh

# 添加到 crontab 每天执行
(crontab -l 2>/dev/null; echo "0 2 * * * /usr/local/bin/backup-scratch.sh") | crontab -
```

恢复数据时，按照以下步骤操作：

```bash
# 停止服务
pm2 stop scratch-backend
sudo systemctl stop nginx

# 恢复配置
cd /var/www/scratch-platform/backups
tar -xzf config_20240101_120000.tar.gz -C /

# 恢复 PM2 进程
pm2 resurrect

# 重启服务
sudo systemctl start nginx
```

## 相关文档

本部署文档应与以下文档配合使用：架构设计文档详细描述了系统的技术架构和各组件的设计原理；TurboWarp 集成文档说明了编辑器的集成方式和配置选项；开发报告记录了平台的版本变更和已知问题。综合阅读这些文档可以全面了解平台的运行机制和运维要点。
