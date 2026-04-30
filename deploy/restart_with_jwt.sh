#!/bin/bash
# 配置 JWT 密钥并重启后端
source /etc/profile.d/scratch.sh

# 停止旧进程
oldpid=$(ps aux | grep scratch-app | grep -v grep | awk '{print $2}')
if [ -n "$oldpid" ]; then
    kill "$oldpid"
    echo "Killed old PID: $oldpid"
    sleep 2
fi

# 启动新进程
nohup java -jar /root/scratch-community-platform/backend/scratch-app/target/scratch-app-0.1.0-SNAPSHOT.jar --spring.profiles.active=dev > /var/log/scratch.log 2>&1 &
sleep 5

# 验证
newpid=$(ps aux | grep scratch-app | grep -v grep | awk '{print $2}')
echo "New PID: $newpid"

echo "===== Health ====="
curl -s http://localhost:8080/actuator/health

echo -e "\n===== Register ====="
curl -s -X POST http://localhost:8080/api/v1/user/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"keytest","nickname":"密钥测试","password":"Test1234!","confirmPassword":"Test1234!"}' | python3 -m json.tool
