#!/bin/bash
# 启动 MinIO（清理旧进程 → 创建目录 → 后台运行）

PID=$(pgrep -f "minio server" || true)
if [ -n "$PID" ]; then
    echo "Killing old MinIO PID: $PID"
    kill -9 $PID
    sleep 2
fi

mkdir -p /data/minio/data
mkdir -p /data/minio/config

echo "MinIO version:"
/usr/local/bin/minio --version

echo "Starting MinIO..."
nohup /usr/local/bin/minio server /data/minio/data --console-address :9001 > /var/log/minio.log 2>&1 &
sleep 4

NEW_PID=$(pgrep -f "minio server" || true)
if [ -n "$NEW_PID" ]; then
    echo "MinIO started ✓ PID=$NEW_PID"
    echo "Console: http://localhost:9001"
    echo "API: http://localhost:9000"
    echo "Credentials: minioadmin / minioadmin (默认，生产必须修改)"
else
    echo "MinIO 启动失败，查看日志:"
    tail -20 /var/log/minio.log
    exit 1
fi
