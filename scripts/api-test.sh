#!/bin/bash
# ============================================================
# Scratch Community Platform - API 联调脚本
# 用法: bash scripts/api-test.sh [BASE_URL]
# 默认: http://localhost:8080
# ============================================================

set -e

BASE_URL="${1:-http://localhost:8080}"
ADMIN_TOKEN=""
TEACHER_TOKEN=""
STUDENT_TOKEN=""
PROJECT_ID=""
PROBLEM_ID=""
SUBMISSION_ID=""
HOMEWORK_ID=""
CLASS_ID=""

# 颜色
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m'

pass=0
fail=0

log_ok() {
    echo -e "${GREEN}✓${NC} $1"
    ((pass++))
}

log_fail() {
    echo -e "${RED}✗${NC} $1"
    ((fail++))
}

log_info() {
    echo -e "${YELLOW}▸${NC} $1"
}

# HTTP 请求封装
api() {
    local method="$1"
    local path="$2"
    local token="$3"
    local data="$4"

    local args=(-s -w "\n%{http_code}" -H "Content-Type: application/json")
    if [ -n "$token" ]; then
        args+=(-H "Authorization: Bearer $token")
    fi
    if [ -n "$data" ]; then
        args+=(-d "$data")
    fi

    local response
    response=$(curl "${args[@]}" -X "$method" "${BASE_URL}${path}")

    local body
    body=$(echo "$response" | head -n -1)
    local code
    code=$(echo "$response" | tail -n 1)

    echo "$body"
    return 0
}

extract_code() {
    echo "$1" | grep -o '"code":[0-9]*' | head -1 | grep -o '[0-9]*$' || echo "-1"
}

extract_id() {
    echo "$1" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*$' || echo ""
}

extract_str() {
    echo "$1" | grep -o "\"$2\":\"[^\"]*\"" | head -1 | sed 's/.*:"\(.*\)"/\1/' || echo ""
}

echo ""
echo "=========================================="
echo " Scratch Community Platform - API 联调"
echo " ${BASE_URL}"
echo "=========================================="
echo ""

# ==================== 健康检查 ====================
log_info "健康检查..."
resp=$(api GET /api/health "")
code=$(extract_code "$resp")
if [ "$code" = "0" ]; then
    log_ok "GET /api/health"
else
    log_fail "GET /api/health (code=$code)"
fi

# ==================== 用户注册 ====================
log_info "注册管理员..."
resp=$(api POST /api/user/register '{"username":"admin_test","password":"Admin123","nickname":"测试管理员","role":"ADMIN"}')
code=$(extract_code "$resp")
if [ "$code" = "0" ]; then
    log_ok "POST /api/user/register (admin)"
else
    log_fail "POST /api/user/register (admin) code=$code"
fi

log_info "注册教师..."
resp=$(api POST /api/user/register '{"username":"teacher_test","password":"Teacher123","nickname":"测试教师","role":"TEACHER"}')
code=$(extract_code "$resp")
if [ "$code" = "0" ]; then
    log_ok "POST /api/user/register (teacher)"
else
    log_fail "POST /api/user/register (teacher) code=$code"
fi

log_info "注册学生..."
resp=$(api POST /api/user/register '{"username":"student_test","password":"Student123","nickname":"测试学生","role":"STUDENT"}')
code=$(extract_code "$resp")
if [ "$code" = "0" ]; then
    log_ok "POST /api/user/register (student)"
else
    log_fail "POST /api/user/register (student) code=$code"
fi

# ==================== 用户登录 ====================
log_info "管理员登录..."
resp=$(api POST /api/user/login '{"username":"admin_test","password":"Admin123"}')
code=$(extract_code "$resp")
ADMIN_TOKEN=$(extract_str "$resp" "token")
if [ "$code" = "0" ] && [ -n "$ADMIN_TOKEN" ]; then
    log_ok "POST /api/user/login (admin) token=${ADMIN_TOKEN:0:20}..."
else
    log_fail "POST /api/user/login (admin) code=$code"
fi

log_info "教师登录..."
resp=$(api POST /api/user/login '{"username":"teacher_test","password":"Teacher123"}')
code=$(extract_code "$resp")
TEACHER_TOKEN=$(extract_str "$resp" "token")
if [ "$code" = "0" ] && [ -n "$TEACHER_TOKEN" ]; then
    log_ok "POST /api/user/login (teacher) token=${TEACHER_TOKEN:0:20}..."
else
    log_fail "POST /api/user/login (teacher) code=$code"
fi

log_info "学生登录..."
resp=$(api POST /api/user/login '{"username":"student_test","password":"Student123"}')
code=$(extract_code "$resp")
STUDENT_TOKEN=$(extract_str "$resp" "token")
if [ "$code" = "0" ] && [ -n "$STUDENT_TOKEN" ]; then
    log_ok "POST /api/user/login (student) token=${STUDENT_TOKEN:0:20}..."
else
    log_fail "POST /api/user/login (student) code=$code"
fi

# ==================== 个人信息 ====================
log_info "获取个人信息..."
resp=$(api GET /api/user/me "" "$ADMIN_TOKEN")
code=$(extract_code "$resp")
if [ "$code" = "0" ]; then
    log_ok "GET /api/user/me"
else
    log_fail "GET /api/user/me code=$code"
fi

# ==================== 班级 ====================
log_info "创建班级..."
resp=$(api POST /api/class "$TEACHER_TOKEN" '{"name":"测试班级","description":"联调测试班级","grade":"三年级"}')
code=$(extract_code "$resp")
CLASS_ID=$(extract_id "$resp")
if [ "$code" = "0" ] && [ -n "$CLASS_ID" ]; then
    log_ok "POST /api/class (id=$CLASS_ID)"
else
    log_fail "POST /api/class code=$code"
fi

log_info "学生加入班级..."
INVITE_CODE=$(extract_str "$resp" "inviteCode")
if [ -n "$INVITE_CODE" ]; then
    resp=$(api POST "/api/class/${CLASS_ID}/join?inviteCode=${INVITE_CODE}" "$STUDENT_TOKEN")
    code=$(extract_code "$resp")
    if [ "$code" = "0" ]; then
        log_ok "POST /api/class/{id}/join"
    else
        log_fail "POST /api/class/{id}/join code=$code"
    fi
fi

# ==================== 项目 ====================
log_info "创建项目..."
resp=$(api POST /api/project "$STUDENT_TOKEN" '{"title":"我的Scratch项目","description":"联调测试项目"}')
code=$(extract_code "$resp")
PROJECT_ID=$(extract_id "$resp")
if [ "$code" = "0" ] && [ -n "$PROJECT_ID" ]; then
    log_ok "POST /api/project (id=$PROJECT_ID)"
else
    log_fail "POST /api/project code=$code"
fi

log_info "发布项目（需先上传 sb3，跳过上传直接测试发布）..."
resp=$(api POST "/api/project/${PROJECT_ID}/publish" "$STUDENT_TOKEN")
code=$(extract_code "$resp")
# 发布可能失败（因为没有 sb3），这是预期行为
if [ "$code" = "0" ] || [ "$code" = "20003" ]; then
    log_ok "POST /api/project/{id}/publish (预期: 需要 sb3)"
else
    log_fail "POST /api/project/{id}/publish code=$code"
fi

# ==================== 题目 ====================
log_info "创建选择题..."
resp=$(api POST /api/problem "$TEACHER_TOKEN" '{"title":"1+1=?","description":"选择正确答案","type":"choice","difficulty":"easy","score":10,"options":[{"key":"A","text":"1"},{"key":"B","text":"2"},{"key":"C","text":"3"}],"answer":"B"}')
code=$(extract_code "$resp")
PROBLEM_ID=$(extract_id "$resp")
if [ "$code" = "0" ] && [ -n "$PROBLEM_ID" ]; then
    log_ok "POST /api/problem (id=$PROBLEM_ID)"
else
    log_fail "POST /api/problem code=$code"
fi

log_info "发布题目..."
resp=$(api POST "/api/problem/${PROBLEM_ID}/publish" "$TEACHER_TOKEN")
code=$(extract_code "$resp")
if [ "$code" = "0" ]; then
    log_ok "POST /api/problem/{id}/publish"
else
    log_fail "POST /api/problem/{id}/publish code=$code"
fi

# ==================== 判题 ====================
log_info "提交答案..."
resp=$(api POST /api/judge/submit "$STUDENT_TOKEN" "{\"problemId\":${PROBLEM_ID},\"answer\":\"B\"}")
code=$(extract_code "$resp")
SUBMISSION_ID=$(extract_id "$resp")
if [ "$code" = "0" ] && [ -n "$SUBMISSION_ID" ]; then
    log_ok "POST /api/judge/submit (id=$SUBMISSION_ID)"
    # 检查判题结果
    verdict=$(extract_str "$resp" "verdict")
    if [ "$verdict" = "AC" ]; then
        log_ok "  → 判题结果: AC ✓"
    else
        log_info "  → 判题结果: $verdict"
    fi
else
    log_fail "POST /api/judge/submit code=$code"
fi

log_info "查询判题结果..."
resp=$(api GET "/api/judge/result/${SUBMISSION_ID}" "$STUDENT_TOKEN")
code=$(extract_code "$resp")
if [ "$code" = "0" ]; then
    log_ok "GET /api/judge/result/{id}"
else
    log_fail "GET /api/judge/result/{id} code=$code"
fi

# ==================== 作业 ====================
log_info "创建作业..."
resp=$(api POST /api/homework "$TEACHER_TOKEN" "{\"classId\":${CLASS_ID},\"title\":\"Scratch 编程作业\",\"description\":\"完成一个计算器项目\",\"type\":\"scratch_project\",\"totalScore\":100}")
code=$(extract_code "$resp")
HOMEWORK_ID=$(extract_id "$resp")
if [ "$code" = "0" ] && [ -n "$HOMEWORK_ID" ]; then
    log_ok "POST /api/homework (id=$HOMEWORK_ID)"
else
    log_fail "POST /api/homework code=$code"
fi

log_info "发布作业..."
resp=$(api POST "/api/homework/${HOMEWORK_ID}/publish" "$TEACHER_TOKEN")
code=$(extract_code "$resp")
if [ "$code" = "0" ]; then
    log_ok "POST /api/homework/{id}/publish"
else
    log_fail "POST /api/homework/{id}/publish code=$code"
fi

# ==================== 社区 ====================
log_info "点赞项目..."
resp=$(api POST "/api/social/project/${PROJECT_ID}/like" "$STUDENT_TOKEN")
code=$(extract_code "$resp")
if [ "$code" = "0" ]; then
    log_ok "POST /api/social/project/{id}/like"
else
    log_fail "POST /api/social/project/{id}/like code=$code"
fi

log_info "添加评论..."
resp=$(api POST /api/social/comment "$STUDENT_TOKEN" "{\"projectId\":${PROJECT_ID},\"content\":\"很棒的项目！\"}")
code=$(extract_code "$resp")
if [ "$code" = "0" ]; then
    log_ok "POST /api/social/comment"
else
    log_fail "POST /api/social/comment code=$code"
fi

log_info "获取 Feed..."
resp=$(api GET "/api/social/feed?sort=latest&page=1&size=10" "")
code=$(extract_code "$resp")
if [ "$code" = "0" ]; then
    log_ok "GET /api/social/feed"
else
    log_fail "GET /api/social/feed code=$code"
fi

# ==================== 管理员 ====================
log_info "数据统计面板..."
resp=$(api GET /api/admin/dashboard "$ADMIN_TOKEN")
code=$(extract_code "$resp")
if [ "$code" = "0" ]; then
    log_ok "GET /api/admin/dashboard"
else
    log_fail "GET /api/admin/dashboard code=$code"
fi

log_info "用户列表..."
resp=$(api GET "/api/admin/user?page=1&size=10" "$ADMIN_TOKEN")
code=$(extract_code "$resp")
if [ "$code" = "0" ]; then
    log_ok "GET /api/admin/user"
else
    log_fail "GET /api/admin/user code=$code"
fi

# ==================== 通知 ====================
log_info "我的通知..."
resp=$(api GET "/api/notification?page=1&size=10" "$STUDENT_TOKEN")
code=$(extract_code "$resp")
if [ "$code" = "0" ]; then
    log_ok "GET /api/notification"
else
    log_fail "GET /api/notification code=$code"
fi

# ==================== 系统配置 ====================
log_info "系统配置列表..."
resp=$(api GET /api/admin/config "$ADMIN_TOKEN")
code=$(extract_code "$resp")
if [ "$code" = "0" ]; then
    log_ok "GET /api/admin/config"
else
    log_fail "GET /api/admin/config code=$code"
fi

# ==================== 搜索 ====================
log_info "搜索用户..."
resp=$(api GET "/api/user/search?q=test&page=1&size=10" "$ADMIN_TOKEN")
code=$(extract_code "$resp")
if [ "$code" = "0" ]; then
    log_ok "GET /api/user/search"
else
    log_fail "GET /api/user/search code=$code"
fi

# ==================== 汇总 ====================
echo ""
echo "=========================================="
echo -e " 测试完成: ${GREEN}${pass} 通过${NC} / ${RED}${fail} 失败${NC}"
echo "=========================================="
echo ""

if [ "$fail" -gt 0 ]; then
    exit 1
fi
