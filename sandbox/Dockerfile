FROM node:18-alpine

# 安装 curl 用于健康检查
RUN apk add --no-cache curl

WORKDIR /app

# 安装依赖
COPY package.json package-lock.json* ./
RUN npm install --production && npm cache clean --force

# 复制源码
COPY src/ ./src/

# 创建非 root 用户
RUN addgroup -S sandbox && adduser -S sandbox -G sandbox
RUN chown -R sandbox:sandbox /app
USER sandbox

# 健康检查
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8081/health || exit 1

EXPOSE 8081

# 内存限制：主进程 1GB，子进程由 fork 参数控制
CMD ["node", "--max-old-space-size=1024", "src/server.js"]
