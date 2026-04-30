# 部署进展（2026-04-30 续）

## ✅ 已完成（本批次）

| 任务 | 状态 | 时间 | 备注 |
|------|------|------|------|
| **JWT 生产密钥配置** | ✅ | 00:07 | 密钥 87 字节，非默认，已生效 |
| **firewalld 防火墙** | ✅ | 00:09 | 仅开放 `http(80)`, `https(443)`, `ssh(22)` |
| **systemd 服务单元** | ✅ | 00:12 | `scratch-app.service`，开机自启，已启用 |

## 🔄 进行中

### MinIO 文件存储（P2）
- **状态**: ⏳ 网络下载失败
- **问题**: dl.min.io 二进制文件下载不完整（curl/wget 多次尝试仅得 1–2MB）
- **原因**: 可能服务器网络策略限制大文件下载
- **方案**: 待手动上传或换源安装
- **影响**: 项目封面、头像上传功能不可用（非核心）

### HTTPS/SSL（P1）
- **依赖**: 需要域名（当前仅有 IP `8.134.252.67`）
- **状态**: ⏳ 等待域名解析
- **方案**: Let's Encrypt + Certbot（需域名指向此 IP）

## 📊 当前服务状态

```
Nginx       : active (80)    → 前端 + API 代理
Backend     : active (8080)  → PID 92381, UP
MySQL       : active (3306)  → scratch_community
Redis       : active (6379)  → 已连接
Firewall    : active         → 80/443/22
Systemd     : enabled        → scratch-app (auto-restart)
```

## 🎯 API 验证结果

| 测试 | 结果 | 说明 |
|------|------|------|
| `GET /actuator/health` | ✅ UP | 后端健康 |
| `POST /api/v1/user/register` | ✅ 成功 | 返回 JWT token |
| `GET /` (frontend) | ✅ 200 | Nginx 静态文件 |
| `GET /api/...` 代理 | ✅ 正常 | 路由到后端 |

## 📝 部署文档

- `docs/DEPLOYMENT.md` - 完整部署报告
- `docs/SERVER_DEPLOYMENT.md` - 服务器配置步骤
- `MEMORY.md` - 长期记忆（已更新）
- `memory/2026-04-30.md` - 当日日志

## 🔐 安全加固清单

- [x] JWT 密钥替换为 87 字节强密钥
- [x] 数据库用户 `scratch` 隔离（非 root）
- [x] 防火墙仅开放必要端口
- [x] systemd 守护进程（自动重启）
- [ ] SSL 证书（需域名）
- [ ] JWT 密钥改为环境变量管理（已通过 `/etc/profile.d/scratch.sh`）
- [ ] MySQL 远程访问禁用（默认本地）

## ⚠️ 待办优先级

| 优先级 | 任务 | 阻塞 | 预计工时 |
|--------|------|------|----------|
| P0 | ✅ JWT 密钥 | 无 | 完成 |
| P1 | ⏳ HTTPS 证书 | 域名 | 0.5h |
| P1 | ✅ 防火墙 | 无 | 完成 |
| P2 | ⏳ MinIO 修复 | 网络 | 1h |
| P2 | ✅ systemd 单元 | 无 | 完成 |
| P3 | 📅 监控告警 | 无 | 待排期 |

## 🚀 访问地址

| 用途 | URL |
|------|-----|
| 前端首页 | http://8.134.252.67/ |
| API 接口 | http://8.134.252.67/api/v1/ |
| Swagger UI | http://8.134.252.67/swagger-ui/ |
| 健康检查 | http://8.134.252.67/actuator/health |

## 💡 后续建议

1. **立即**：浏览器打开 http://8.134.252.67/ 确认前端渲染
2. **本周**：
   - 配置域名并申请 HTTPS 证书
   - 手动安装 MinIO（或替换为阿里云 OSS）
3. **下 Sprint**：
   - 集成 Testcontainers 做 E2E 测试
   - 数据分区优化（`point_log` 按时间分表）

---

**更新时间**: 2026-04-30 00:16  
**状态**: 🟢 核心服务已就绪，可投入使用
