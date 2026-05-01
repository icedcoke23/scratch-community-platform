# Scratch 社区平台 - 服务器部署指南

## 问题现状
您当前部署在 http://8.134.252.67/feed 的代码版本缺少以下 API 接口，导致作品管理功能无法正常使用：
- `/api/v1/admin/project/stats` 404
- `/api/v1/admin/project` 404

## 部署步骤

### 第 1 步：登录到服务器并定位项目目录
```bash
ssh root@8.134.252.67
# 密码: Aa2485204216

# 检查项目目录
cd /root/scratch-community-platform  # 或根据实际情况找到项目目录
ls -la
```

### 第 2 步：更新 Git 仓库到最新代码
```bash
cd /root/scratch-community-platform
git status
git pull origin main

# 合并最新的功能分支（如果还没合并）
git fetch origin
git merge origin/trae/solo-agent-4qXB9U --no-edit
```

### 第 3 步：重新构建后端应用
```bash
cd /root/scratch-community-platform/backend

# 检查是否有 Maven
mvn -version

# 清理并构建
mvn clean package -DskipTests

# 检查构建产物
ls -lh scratch-app/target/
```

### 第 4 步：重启后端服务
```bash
cd /root/scratch-community-platform

# 方式 A：使用 systemd 服务（推荐）
systemctl stop scratch-app
systemctl start scratch-app
systemctl status scratch-app --no-pager

# 方式 B：使用部署脚本（如果配置了）
cd /root/scratch-community-platform/deploy
chmod +x restart_with_jwt.sh
./restart_with_jwt.sh

# 检查日志
tail -f /var/log/scratch.log
# 或者
journalctl -u scratch-app -f
```

### 第 5 步：验证 API 接口是否正常
```bash
# 检查健康状态
curl -s http://localhost:8080/actuator/health

# 检查登录（可选）
curl -s -X POST http://localhost:8080/api/v1/user/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"icedcoke","password":"Aa2485204216!"}'

# 或者在浏览器中重新登录并访问作品管理页面验证
```

### 第 6 步：（可选）重新构建并部署前端
如果前端也有更新：
```bash
cd /root/scratch-community-platform/frontend-vue
npm install
npm run build

# 将构建产物部署到 Web 服务器目录（如 /var/www/html 或 Nginx 配置的目录）
# 或使用 Docker 重新构建前端镜像
```

## 快速参考命令

### 检查服务状态
```bash
# 后端进程
ps aux | grep scratch-app

# 端口监听
ss -tlnp | grep 8080

# systemd 服务
systemctl status scratch-app --no-pager

# 日志
journalctl -u scratch-app -n 100 --no-pager
tail -100f /var/log/scratch.log
```

### 数据库相关
```bash
# 连接数据库
mysql -u root -p scratch_community

# 查看表
SHOW TABLES;
```

## 故障排查

### 如果构建失败
1. 检查 Maven 和 Java 版本：
```bash
java -version
mvn -version
```
2. 清理本地仓库并重新下载依赖：
```bash
mvn clean install -U -DskipTests
```

### 如果服务无法启动
1. 检查环境变量配置文件：
```bash
cat /etc/profile.d/scratch.sh
cat /etc/profile.d/scratch.conf
```
2. 查看完整的错误日志：
```bash
journalctl -u scratch-app -xe
```

## 部署完成后的验证
- [ ] 首页正常显示
- [ ] 登录功能正常
- [ ] 用户管理功能正常（`/admin/users`）
- [ ] 作品管理功能正常（`/admin/projects`，不再有 404 错误）
- [ ] 作品列表正常加载
- [ ] 作品统计正常显示
