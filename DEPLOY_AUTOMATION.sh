#!/bin/bash
set -e  # 出错时停止执行

echo "====================================="
echo "  Scratch 社区平台 - 自动化部署脚本"
echo "====================================="

# 检查是否为 root 用户
if [ "$EUID" -ne 0 ]; then 
    echo "请以 root 用户运行此脚本"
    exit 1
fi

# 第 1 步：定位项目目录
echo ""
echo "[1/6] 定位项目目录..."
PROJECT_DIR=""
for dir in \
    "/root/scratch-community-platform" \
    "/home/scratch-community-platform" \
    "/scratch-community-platform"; do
    if [ -d "$dir" ]; then
        PROJECT_DIR="$dir"
        break
    fi
done

if [ -z "$PROJECT_DIR" ]; then
    echo "错误：未找到项目目录！"
    echo "请先克隆或放置项目到合适位置"
    exit 1
fi

echo "✅ 项目目录: $PROJECT_DIR"
cd "$PROJECT_DIR"
ls -la

# 第 2 步：更新 Git 仓库
echo ""
echo "[2/6] 更新 Git 仓库..."
git status
echo ""
git fetch --all
echo ""
echo "正在合并最新分支..."
git merge origin/trae/solo-agent-4qXB9U --no-edit || true
git merge origin/main --no-edit || true
echo ""
git log --oneline -5

# 第 3 步：重新构建后端
echo ""
echo "[3/6] 重新构建后端应用..."
cd "$PROJECT_DIR/backend"

# 检查 Maven 版本
echo "检查 Maven 版本..."
mvn -version

# 清理并构建
echo "开始 Maven 构建（跳过测试）..."
mvn clean package -DskipTests

# 验证构建
if [ ! -f "$PROJECT_DIR/backend/scratch-app/target/scratch-app-0.1.0-SNAPSHOT.jar" ]; then
    echo "❌ 构建失败！"
    exit 1
fi
echo "✅ 构建成功"
ls -lh "$PROJECT_DIR/backend/scratch-app/target/"

# 第 4 步：重启后端服务
echo ""
echo "[4/6] 重启后端服务..."
cd "$PROJECT_DIR"

# 停止旧服务
echo "停止旧服务..."
if systemctl is-active --quiet scratch-app; then
    systemctl stop scratch-app
fi

# 等待完全停止
sleep 3

# 启动新服务
echo "启动新服务..."
systemctl start scratch-app
sleep 5

# 检查服务状态
echo ""
echo "[5/6] 检查服务状态..."
systemctl status scratch-app --no-pager

# 检查端口
echo ""
echo "检查端口监听..."
ss -tlnp | grep 8080 || echo "端口 8080 未监听（可能还在启动中...）"

# 第 5 步：验证服务健康检查
echo ""
echo "[6/6] 服务健康检查..."
for i in {1..30}; do
    if curl -s "http://localhost:8080/actuator/health" > /dev/null 2>&1; then
        echo ""
        echo "✅ 服务启动成功！"
        curl -s "http://localhost:8080/actuator/health"
        break
    fi
    echo "等待服务启动... ($i/30)"
    sleep 2
done

echo ""
echo "====================================="
echo "  部署完成！"
echo "====================================="
echo ""
echo "下一步："
echo "1. 查看服务日志：journalctl -u scratch-app -f"
echo "2. 在浏览器中访问 http://8.134.252.67/feed 验证"
echo "3. 登录管理员账户并检查作品管理功能"
echo ""
