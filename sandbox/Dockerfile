FROM node:18-alpine

WORKDIR /app

# 安装 scratch-vm 依赖
RUN npm install -g scratch-vm@2.1.0 2>/dev/null || true

COPY package.json ./
RUN npm install --production

COPY src/ ./src/

EXPOSE 8081

CMD ["node", "src/server.js"]
