#!/bin/bash

# Scratch Community Platform 快速启动脚本
# 用法: ./start.sh

set -e

echo "=========================================="
echo "Scratch Community Platform 启动脚本"
echo "=========================================="
echo ""

# 定义颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 检查 Node.js
if ! command -v node &> /dev/null; then
    echo -e "${RED}错误: Node.js 未安装${NC}"
    exit 1
fi

# 检查端口是否被占用
check_port() {
    if lsof -Pi :$1 -sTCP:LISTEN -t >/dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# 杀掉占用端口的进程
kill_port() {
    if check_port $1; then
        echo -e "${YELLOW}端口 $1 已被占用，正在尝试释放...${NC}"
        lsof -ti :$1 | xargs kill -9 2>/dev/null || true
        sleep 1
    fi
}

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend"
FRONTEND_DIR="$PROJECT_ROOT/frontend-vue"

echo -e "${GREEN}[1/3]${NC} 检查后端依赖..."
cd "$BACKEND_DIR"
if [ ! -d "node_modules" ]; then
    echo "安装后端依赖..."
    npm install
fi

echo -e "${GREEN}[2/3]${NC} 检查前端依赖..."
cd "$FRONTEND_DIR"
if [ ! -d "node_modules" ]; then
    echo "安装前端依赖..."
    npm install
fi

echo -e "${GREEN}[3/3]${NC} 启动服务..."
echo ""

# 启动后端服务
echo -e "${YELLOW}启动后端服务 (端口 8080)...${NC}"
cd "$BACKEND_DIR"
PORT=8080 kill_port 8080
npm run dev &
BACKEND_PID=$!
echo "后端服务 PID: $BACKEND_PID"

# 等待后端启动
echo "等待后端服务启动..."
for i in {1..30}; do
    if curl -s http://localhost:8080/health >/dev/null 2>&1; then
        echo -e "${GREEN}后端服务已就绪${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}后端服务启动失败${NC}"
        exit 1
    fi
    sleep 1
done

# 等待几秒确保后端稳定
sleep 2

# 启动前端服务
echo ""
echo -e "${YELLOW}启动前端服务 (端口 3000)...${NC}"
cd "$FRONTEND_DIR"
PORT=3000 kill_port 3000
npm run dev &
FRONTEND_PID=$!
echo "前端服务 PID: $FRONTEND_PID"

# 等待前端启动
echo "等待前端服务启动..."
for i in {1..30}; do
    if curl -s http://localhost:3000 >/dev/null 2>&1; then
        echo -e "${GREEN}前端服务已就绪${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}前端服务启动失败${NC}"
        exit 1
    fi
    sleep 1
done

echo ""
echo "=========================================="
echo -e "${GREEN}所有服务已成功启动！${NC}"
echo "=========================================="
echo ""
echo "服务地址:"
echo "  - 后端 API:  http://localhost:8080"
echo "  - 前端界面:  http://localhost:3000"
echo "  - 健康检查:  http://localhost:8080/health"
echo ""
echo "按 Ctrl+C 停止所有服务"
echo ""

# 捕获 Ctrl+C 信号
cleanup() {
    echo ""
    echo -e "${YELLOW}正在停止服务...${NC}"
    kill $BACKEND_PID 2>/dev/null || true
    kill $FRONTEND_PID 2>/dev/null || true
    echo -e "${GREEN}所有服务已停止${NC}"
    exit 0
}

trap cleanup SIGINT SIGTERM

# 保持脚本运行
wait
