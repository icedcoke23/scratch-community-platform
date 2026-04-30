#!/bin/bash
# 数据库备份脚本 - Scratch Community Platform
# 用法: ./backup-database.sh [环境]
# 示例: ./backup-database.sh prod

set -e

# 配置
BACKUP_DIR="${BACKUP_DIR:-/var/backups/scratch-community}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
DATE=$(date +%Y%m%d_%H%M%S)
ENV="${1:-dev}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查依赖
check_dependencies() {
    if ! command -v docker &> /dev/null; then
        log_error "docker 未安装"
        exit 1
    fi

    if ! command -v gzip &> /dev/null; then
        log_error "gzip 未安装"
        exit 1
    fi
}

# 创建备份目录
create_backup_dir() {
    if [ ! -d "$BACKUP_DIR" ]; then
        mkdir -p "$BACKUP_DIR"
        log_info "创建备份目录: $BACKUP_DIR"
    fi
}

# 备份数据库
backup_database() {
    local backup_file="$BACKUP_DIR/scratch_community_${ENV}_${DATE}.sql.gz"
    
    log_info "开始备份数据库..."
    log_info "环境: $ENV"
    log_info "备份文件: $backup_file"

    # 从 Docker 容器导出数据库
    docker exec scratch-mysql mysqldump \
        -u root \
        -p"${MYSQL_ROOT_PASSWORD}" \
        --single-transaction \
        --routines \
        --triggers \
        --events \
        --add-drop-table \
        --create-options \
        --extended-insert \
        --quick \
        --set-charset \
        scratch_community | gzip > "$backup_file"

    if [ $? -eq 0 ]; then
        local size=$(du -h "$backup_file" | cut -f1)
        log_info "备份成功: $backup_file ($size)"
    else
        log_error "备份失败"
        exit 1
    fi
}

# 清理旧备份
cleanup_old_backups() {
    log_info "清理 ${RETENTION_DAYS} 天前的备份..."
    
    local deleted=$(find "$BACKUP_DIR" -name "scratch_community_*.sql.gz" -mtime +${RETENTION_DAYS} -delete -print | wc -l)
    
    if [ "$deleted" -gt 0 ]; then
        log_info "已删除 $deleted 个旧备份"
    else
        log_info "没有需要清理的旧备份"
    fi
}

# 验证备份
verify_backup() {
    local backup_file="$BACKUP_DIR/scratch_community_${ENV}_${DATE}.sql.gz"
    
    log_info "验证备份文件..."
    
    # 检查文件是否存在且非空
    if [ ! -s "$backup_file" ]; then
        log_error "备份文件不存在或为空"
        exit 1
    fi
    
    # 检查 gzip 完整性
    if ! gzip -t "$backup_file" 2>/dev/null; then
        log_error "备份文件损坏"
        exit 1
    fi
    
    log_info "备份验证通过"
}

# 显示备份列表
list_backups() {
    log_info "现有备份:"
    ls -lh "$BACKUP_DIR"/scratch_community_*.sql.gz 2>/dev/null || log_warn "没有找到备份文件"
}

# 主流程
main() {
    log_info "=== Scratch Community 数据库备份 ==="
    
    check_dependencies
    create_backup_dir
    backup_database
    verify_backup
    cleanup_old_backups
    list_backups
    
    log_info "=== 备份完成 ==="
}

# 执行
main
