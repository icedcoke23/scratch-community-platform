#!/bin/bash
# 停止旧进程
pkill -f "scratch-app-0.1.0-SNAPSHOT.jar" 2>/dev/null
sleep 2

echo "Stopped old processes."

# PID=91463
# 通过 systemd 启动
systemctl start scratch-app

echo "Waited for systemd..."
sleep 6

echo "=== Service Status ==="
systemctl status scratch-app --no-pager -l

echo ""
echo "=== Java Process ==="
pgrep -af "scratch-app" || echo "No process found"

echo ""
echo "=== Port 8080 ==="
ss -tlnp | grep 8080 || echo "Port 8080 not listening"

echo ""
echo "=== Health ==="
curl -s http://localhost:8080/actuator/health
