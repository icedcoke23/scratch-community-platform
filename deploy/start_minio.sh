#!/bin/bash
set -e

# 停止旧进程
pkill -f "minio server" 2>/dev/null || true
sleep 1

echo "Starting MinIO server..."
nohup /usr/local/bin/minio server /data/minio/data --console-address :9001 > /var/log/minio.log 2>&1 &
sleep 5

# 检查进程
if pgrep -f "minio server" > /dev/null; then
    echo "MinIO process: OK"
    pgrep -af minio | grep -v grep
else
    echo "MinIO failed to start. Log:"
    tail -20 /var/log/minio.log
    exit 1
fi

# 检查端口
if ss -tlnp | grep -q ":9000"; then
    echo "Port 9000: LISTENING"
else
    echo "Port 9000: NOT LISTENING (but process may still be starting)"
fi

if ss -tlnp | grep -q ":9001"; then
    echo "Port 9001 (console): LISTENING"
fi

# 健康检查
echo "Health check:"
curl -s http://localhost:9000/minio/health/live || echo "Health check failed"

echo ""
echo "MinIO credentials (default, CHANGE IN PROD):"
echo "  Access: minioadmin"
echo "  Secret: minioadmin"
echo ""
echo "Console: http://localhost:9001"
echo "API: http://localhost:9000"
