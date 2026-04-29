#!/bin/bash
# 配置防火墙（firewalld）：仅开放必要端口
# 目标：22(SSH) 80(HTTP) 443(HTTPS)，其他全部拒绝

systemctl enable firewalld
systemctl start firewalld

# 重置为默认（drop 模式）
firewall-cmd --permanent --set-target=default

# 允许 SSH（防止锁死）
firewall-cmd --permanent --add-service=ssh

# 允许 HTTP/HTTPS
firewall-cmd --permanent --add-service=http
firewall-cmd --permanent --add-service=https

# 允许本地回环（已存在，确保）
firewall-cmd --permanent --add-rich-rule='rule family="ipv4" source address="127.0.0.1/8" accept'

# 移除其他所有规则（清理）
firewall-cmd --permanent --remove-service=dhcpv6-client 2>/dev/null || true
firewall-cmd --permanent --remove-service=mdns 2>/dev/null || true

# 重载
firewall-cmd --reload

echo "===== 当前规则 ====="
firewall-cmd --list-all

echo "Done."
