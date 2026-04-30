# 🐳 Docker 配置目录

> 最后更新：2026-04-28

---

## 目录结构

```
docker/
├── compose/                    Docker Compose 配置
│   ├── docker-compose.yml      开发环境（MySQL + Redis + MinIO + Backend + Sandbox + Frontend）
│   └── docker-compose.prod.yml 生产环境（含 Prometheus + Grafana + Loki 监控栈）
├── monitoring/                 监控配置
│   ├── prometheus.yml          Prometheus 采集配置
│   ├── alert_rules.yml         告警规则
│   ├── grafana/                Grafana 数据源 + Dashboard
│   ├── loki-config.yml         Loki 日志聚合配置
│   └── promtail-config.yml     Promtail 日志采集配置
├── nginx/                      Nginx 配置
│   ├── nginx.conf              开发环境（前端静态 + API 反向代理）
│   └── nginx.prod.conf         生产环境（HTTPS + 限流 + 安全头）
├── Dockerfile.backend          后端镜像（Maven 构建 + JRE 运行）
├── Dockerfile.frontend         前端镜像（Node 构建 + Nginx 运行）
└── init.sql                    Docker MySQL 初始化脚本（仅首次启动用）
```

## 快速启动

```bash
# 开发环境
cd docker/compose
cp ../../.env.example .env
docker-compose up -d

# 生产环境
cd docker/compose
cp ../../.env.example .env
# 编辑 .env 设置生产密码和密钥
docker-compose -f docker-compose.prod.yml up -d
```

## 注意事项

- `init.sql` 仅用于 Docker MySQL 首次启动时初始化数据库，实际表结构由 Flyway 迁移脚本管理
- 生产环境必须设置 `JWT_SECRET`、`MYSQL_ROOT_PASSWORD`、`MINIO_ROOT_PASSWORD` 等环境变量
- 监控栈（Prometheus + Grafana + Loki）仅在 prod compose 中启用
