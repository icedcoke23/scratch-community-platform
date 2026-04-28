# 🔧 运维脚本

> 最后更新：2026-04-28

---

## 脚本列表

| 脚本 | 用途 | 用法 |
|------|------|------|
| `api-test.sh` | API 联调测试（curl 自动化） | `bash scripts/api-test.sh [BASE_URL]` |
| `backup-database.sh` | 数据库备份（mysqldump + 压缩） | `./scripts/backup-database.sh [env]` |
| `performance-test.js` | k6 性能压测（健康检查/Feed/排行榜/登录） | `k6 run scripts/performance-test.js` |

## 前置条件

- `api-test.sh`: curl, jq
- `backup-database.sh`: mysqldump, gzip
- `performance-test.js`: [k6](https://k6.io/)
