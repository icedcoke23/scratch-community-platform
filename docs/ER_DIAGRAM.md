# 实体关系图 (ER Diagram)

> 本文档使用 Mermaid 语法绘制 Scratch 社区平台的完整实体关系图。
> 最后更新：2026-04-25

## 完整 ER 图

```mermaid
erDiagram
    %% ==================== 用户模块 ====================
    user {
        bigint id PK "用户ID"
        varchar username UK "用户名"
        varchar password "密码(BCrypt)"
        varchar nickname "昵称"
        varchar avatar_url "头像URL"
        varchar bio "个人简介"
        varchar email "邮箱"
        tinyint role "角色: 0学生 1教师 2管理员"
        tinyint status "状态: 0正常 1禁用"
        int points "积分"
        int level "等级"
        datetime last_login_at "最后登录时间"
        int login_count "登录次数"
        datetime created_at "创建时间"
        datetime updated_at "更新时间"
        tinyint deleted "逻辑删除"
    }

    user_follow {
        bigint id PK "关注ID"
        bigint follower_id FK "关注者ID"
        bigint following_id FK "被关注者ID"
        datetime created_at "关注时间"
    }

    %% ==================== 教室模块 ====================
    class {
        bigint id PK "教室ID"
        varchar name "教室名称"
        varchar description "教室描述"
        bigint teacher_id FK "教师ID"
        varchar invite_code UK "邀请码"
        varchar grade "年级"
        int student_count "学生数"
        datetime created_at "创建时间"
        datetime updated_at "更新时间"
        tinyint deleted "逻辑删除"
    }

    class_student {
        bigint id PK "记录ID"
        bigint class_id FK "教室ID"
        bigint student_id FK "学生ID"
        datetime joined_at "加入时间"
    }

    %% ==================== 创作模块 ====================
    project {
        bigint id PK "项目ID"
        bigint user_id FK "作者ID"
        varchar title "项目标题"
        varchar description "项目描述"
        varchar cover_url "封面URL"
        varchar sb3_url "SB3文件URL"
        tinyint status "状态: 0草稿 1公开 2审核中"
        int block_count "积木数量"
        decimal complexity_score "复杂度评分"
        int like_count "点赞数"
        int comment_count "评论数"
        int view_count "浏览数"
        json parse_result "解析结果"
        varchar tags "标签(逗号分隔)"
        bigint remix_project_id FK "原创项目ID"
        int remix_count "再创作次数"
        datetime created_at "创建时间"
        datetime updated_at "更新时间"
        tinyint deleted "逻辑删除"
    }

    %% ==================== 社区模块 ====================
    project_like {
        bigint id PK "点赞ID"
        bigint user_id FK "用户ID"
        bigint project_id FK "项目ID"
        datetime created_at "点赞时间"
    }

    project_comment {
        bigint id PK "评论ID"
        bigint user_id FK "用户ID"
        bigint project_id FK "项目ID"
        text content "评论内容"
        datetime created_at "评论时间"
        tinyint deleted "逻辑删除"
    }

    %% ==================== 判题模块 ====================
    problem {
        bigint id PK "题目ID"
        varchar title "题目标题"
        text description "题目描述"
        tinyint type "类型: 0选择 1编程 2填空"
        tinyint difficulty "难度: 1简单 2中等 3困难"
        int score "分值"
        varchar tags "标签"
        json options "选项(JSON)"
        varchar answer "答案"
        text expected_output "期望输出"
        varchar template_sb3_url "模板SB3"
        tinyint status "状态: 0草稿 1发布"
        int submit_count "提交数"
        int accept_count "通过数"
        bigint creator_id FK "创建者ID"
        datetime created_at "创建时间"
        datetime updated_at "更新时间"
        tinyint deleted "逻辑删除"
    }

    submission {
        bigint id PK "提交ID"
        bigint user_id FK "用户ID"
        bigint problem_id FK "题目ID"
        bigint competition_id FK "竞赛ID"
        tinyint submit_type "提交类型: 0练习 1竞赛"
        varchar answer "答案"
        varchar sb3_url "SB3文件URL"
        tinyint verdict "判定: 0待判 1通过 2错误 3超时"
        json judge_detail "判题详情"
        int runtime_ms "运行时间(ms)"
        int memory_kb "内存占用(KB)"
        datetime created_at "提交时间"
        tinyint deleted "逻辑删除"
    }

    competition {
        bigint id PK "竞赛ID"
        varchar title "竞赛标题"
        text description "竞赛描述"
        bigint creator_id FK "创建者ID"
        tinyint type "类型: 0限时 1开放式"
        datetime start_time "开始时间"
        datetime end_time "结束时间"
        int total_score "总分"
        int participant_count "参与人数"
        tinyint status "状态: 0未开始 1进行中 2已结束"
        tinyint is_public "是否公开"
        datetime created_at "创建时间"
        datetime updated_at "更新时间"
        tinyint deleted "逻辑删除"
    }

    competition_registration {
        bigint id PK "报名ID"
        bigint competition_id FK "竞赛ID"
        bigint user_id FK "用户ID"
        datetime created_at "报名时间"
    }

    competition_ranking {
        bigint id PK "排名ID"
        bigint competition_id FK "竞赛ID"
        bigint user_id FK "用户ID"
        int total_score "总分"
        int solved_count "解题数"
        int penalty "罚时(秒)"
        int rank "排名"
        json problem_details "题目详情(JSON)"
        datetime last_submit_time "最后提交时间"
        datetime created_at "创建时间"
        datetime updated_at "更新时间"
    }

    competition_problem {
        bigint id PK "记录ID"
        bigint competition_id FK "竞赛ID"
        bigint problem_id FK "题目ID"
        int sort_order "排序序号"
        int score "分值"
    }

    %% ==================== 教学模块 ====================
    homework {
        bigint id PK "作业ID"
        bigint class_id FK "教室ID"
        bigint teacher_id FK "教师ID"
        varchar title "作业标题"
        text description "作业描述"
        tinyint type "类型: 0编程 1选择 2混合"
        datetime deadline "截止时间"
        int total_score "总分"
        tinyint status "状态: 0草稿 1发布 2已截止"
        int submit_count "提交数"
        int graded_count "已批改数"
        datetime created_at "创建时间"
        datetime updated_at "更新时间"
        tinyint deleted "逻辑删除"
    }

    homework_submission {
        bigint id PK "提交ID"
        bigint homework_id FK "作业ID"
        bigint student_id FK "学生ID"
        bigint project_id FK "项目ID"
        json answers "答案(JSON)"
        int score "得分"
        varchar comment "批改评语"
        tinyint status "状态: 0待批改 1已批改 2退回"
        datetime created_at "提交时间"
        datetime graded_at "批改时间"
        tinyint deleted "逻辑删除"
    }

    homework_problem {
        bigint id PK "记录ID"
        bigint homework_id FK "作业ID"
        bigint problem_id FK "题目ID"
        int sort_order "排序序号"
        int score "分值"
    }

    %% ==================== 积分模块 ====================
    point_log {
        bigint id PK "记录ID"
        bigint user_id FK "用户ID"
        varchar type "积分类型"
        int points "变动积分"
        int total_points "积分余额"
        varchar ref_type "关联类型"
        bigint ref_id "关联ID"
        varchar remark "备注"
        datetime created_at "记录时间"
    }

    %% ==================== AI 模块 ====================
    ai_review {
        bigint id PK "评审ID"
        bigint project_id FK "项目ID"
        bigint user_id FK "用户ID"
        decimal overall_score "综合评分"
        json dimension_scores "维度评分(JSON)"
        text summary "评审摘要"
        text detail "详细评审"
        text strengths "优点"
        text suggestions "改进建议"
        int block_count "积木数量"
        int sprite_count "角色数量"
        decimal complexity_score "复杂度评分"
        varchar provider "AI服务商"
        datetime created_at "评审时间"
    }

    %% ==================== 系统模块 ====================
    notification {
        bigint id PK "通知ID"
        bigint user_id FK "用户ID"
        varchar type "通知类型"
        varchar title "通知标题"
        text content "通知内容"
        json data "附加数据(JSON)"
        tinyint is_read "是否已读"
        datetime created_at "创建时间"
    }

    content_audit_log {
        bigint id PK "审核ID"
        varchar content_type "内容类型"
        bigint content_id "内容ID"
        text content_text "内容文本"
        tinyint status "审核状态"
        varchar reason "审核原因"
        bigint operator_id FK "操作者ID"
        datetime created_at "审核时间"
    }

    system_config {
        bigint id PK "配置ID"
        varchar config_key UK "配置键"
        text config_value "配置值"
        varchar description "配置描述"
        datetime updated_at "更新时间"
    }

    %% ==================== 关系定义 ====================

    %% 用户自关注关系 (多对多通过中间表)
    user ||--o{ user_follow : "关注他人"
    user ||--o{ user_follow : "被关注"

    %% 教室关系
    user ||--o{ class : "教师创建教室"
    user ||--o{ class_student : "学生加入教室"
    class ||--o{ class_student : "包含学生"

    %% 项目关系
    user ||--o{ project : "创建项目"
    project ||--o{ project : "remix关系"

    %% 社区关系
    user ||--o{ project_like : "点赞"
    project ||--o{ project_like : "被点赞"
    user ||--o{ project_comment : "评论"
    project ||--o{ project_comment : "被评论"

    %% 判题关系
    user ||--o{ problem : "创建题目"
    user ||--o{ submission : "提交解答"
    problem ||--o{ submission : "被提交"

    %% 竞赛关系
    user ||--o{ competition : "创建竞赛"
    competition ||--o{ competition_registration : "报名记录"
    user ||--o{ competition_registration : "报名参赛"
    competition ||--o{ competition_ranking : "排名记录"
    user ||--o{ competition_ranking : "参与排名"
    competition ||--o{ competition_problem : "包含题目"
    problem ||--o{ competition_problem : "被竞赛引用"
    submission }o--o| competition : "属于竞赛"

    %% 教学关系
    class ||--o{ homework : "布置作业"
    user ||--o{ homework : "教师布置"
    homework ||--o{ homework_submission : "提交记录"
    user ||--o{ homework_submission : "学生提交"
    project ||--o{ homework_submission : "关联项目"
    homework ||--o{ homework_problem : "包含题目"
    problem ||--o{ homework_problem : "被作业引用"

    %% 积分关系
    user ||--o{ point_log : "积分记录"

    %% AI 关系
    project ||--o| ai_review : "AI评审"
    user ||--o{ ai_review : "发起评审"

    %% 系统关系
    user ||--o{ notification : "接收通知"
    user ||--o{ content_audit_log : "审核操作"
```

## 关系说明

### 一对多关系

| 关系 | 说明 |
|------|------|
| user → class | 一个教师可创建多个教室 |
| user → project | 一个用户可创建多个项目 |
| user → problem | 一个用户可创建多个题目 |
| class → homework | 一个教室可布置多个作业 |
| competition → submission | 一个竞赛可有多个提交 |
| user → notification | 一个用户可有多条通知 |
| user → point_log | 一个用户可有多条积分记录 |

### 多对多关系 (通过中间表)

| 关系 | 中间表 | 说明 |
|------|--------|------|
| user ↔ user | user_follow | 用户互相关注 |
| class ↔ user | class_student | 学生加入教室 |
| competition ↔ problem | competition_problem | 竞赛包含题目 |
| homework ↔ problem | homework_problem | 作业包含题目 |
| competition ↔ user | competition_registration | 用户报名竞赛 |

### 一对一关系

| 关系 | 说明 |
|------|------|
| project → ai_review | 每个项目最多一条 AI 评审 |
