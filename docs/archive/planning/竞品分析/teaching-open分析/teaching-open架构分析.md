# Scratch 编程社区平台 — 竞品分析 & 系统设计草案

> 版本：v0.1 | 日期：2026-04-23 | 状态：头脑风暴阶段

---

## 第一部分：竞品深度分析

### 1. 编程猫 (codemao.cn)

**定位**：国内最大的少儿编程创作社区，偏 C 端创作工具 + UGC 社区

| 维度 | 分析 |
|---|---|
| **创作工具** | 自研 Kitten（类Scratch图形化）、Nemo（移动端图形化）、海龟编辑器（图形化Python）、源码编辑器（代码Python/C++）—— 多工具矩阵，覆盖全年龄段 |
| **社区生态** | 作品发布 → 发现/推荐 → 点赞/收藏/评论 → 关注/粉丝 → 工作室（组队创作）→ 编程论坛（帖子交流） |
| **素材市场** | 独立素材库（角色/背景/声音），用户可上传分享，有分类标签体系 |
| **激励体系** | 作品审核上架、热门推荐、编程猫币（虚拟货币）、创作者等级 |
| **竞赛活动** | 定期举办创意编程赛、1024编程节等官方活动 |
| **教学功能** | 课程学习路径、直播课、录播课（偏商业化，非开源） |
| **技术特点** | 前端 SPA，自研编辑器（非 Scratch 官方魔改），CDN 加速素材分发 |

**值得借鉴**：
- ✅ 多语言多工具矩阵（图形化 + 代码无缝过渡）
- ✅ 工作室/组队创作模式
- ✅ 素材市场生态
- ✅ 推荐算法（热门/最新/小编推荐多维度）

**不足**：
- ❌ 无 OJ/自动判题能力
- ❌ 竞赛功能偏弱，多是活动性质
- ❌ 无系统化的积分/徽章/成就体系
- ❌ 机构/班级管理能力弱（面向 C 端为主）

---

### 2. 小码王 (world.xiaomawang.com)

**定位**：偏教育机构的 Scratch 社区，强调创作 + 社交

| 维度 | 分析 |
|---|---|
| **创作工具** | 基于 Scratch 3.0 在线编辑器，浏览器内直接创作 |
| **社区功能** | 作品展示、点赞、收藏、评论、分享、排行榜 |
| **社交属性** | 关注/粉丝、个人主页、创作动态流 |
| **课程体系** | 与线下课程联动，布置作业 → 提交 → 展示 |
| **技术特点** | 标准 Scratch 3.0 + 自定义社区层 |

**值得借鉴**：
- ✅ 轻量化社区设计，聚焦 Scratch 创作
- ✅ 排行榜激励
- ✅ 与线下教学的联动

**不足**：
- ❌ 功能单一，无刷题/OJ
- ❌ 无竞赛系统
- ❌ AI 能力缺失
- ❌ 积分体系不明显

---

### 3. 斯坦星球 (code.stemstar.com)

**定位**：STEAM 教育品牌，在线编程 + 社区 + 积分系统

| 维度 | 分析 |
|---|---|
| **创作工具** | 在线 Scratch 编辑器 |
| **社区功能** | 作品发布、展示、推荐、讨论区（帖子/话题） |
| **积分系统** | ✅ 新增 — 参与活动/提交作品/完成任务 获得积分 |
| **课程地图** | 可视化学习路径 |
| **推荐机制** | 作品推荐上首页 |
| **技术特点** | 近期升级，新增积分系统和作品推荐 |

**值得借鉴**：
- ✅ 积分系统设计（多维度获取积分）
- ✅ 作品推荐上首页（激励创作）
- ✅ 话题/讨论区

**不足**：
- ❌ 无 OJ/自动判题
- ❌ 竞赛功能缺失
- ❌ AI 能力缺失
- ❌ 扩展性一般

---

### 4. 少儿编程 OJ 系统（好学好教 cncoding.cn 等）

**定位**：面向机构的 Scratch/Python 自动判题系统

| 维度 | 分析 |
|---|---|
| **题目类型** | 算法题（输入/输出比对）、创意题（绘图/作品）、复合题型（多步骤） |
| **Scratch 判题原理** | 方案一：提取积木逻辑 → 转为可执行代码 → 运行比对输出<br>方案二：运行 Scratch 项目 → 截取舞台输出 → 图像比对<br>方案三：测试用例注入 → 检测变量/精灵状态 |
| **特判功能** | SPJ（Special Judge）—— 支持多正确答案、浮点误差、自定义判题逻辑 |
| **竞赛模式** | 定时赛（限时答题）、练习模式（无限制）、组队赛 |
| **分值系统** | 每题可设置分值，支持部分分（按测试点给分） |
| **防作弊** | 代码相似度检测、限时、随机题序 |
| **组队功能** | 2024年新增，支持团队竞赛 |

**值得借鉴**：
- ✅ Scratch 自动判题核心能力
- ✅ SPJ 特判机制
- ✅ 组队赛模式
- ✅ 测试数据图形化生成器
- ✅ 部分分机制（按测试点给分）

**不足**：
- ❌ 无社区/分享功能
- ❌ 无创作工具（纯 OJ）
- ❌ 用户体验偏传统（非现代化 UI）
- ❌ 无积分/成就/徽章体系
- ❌ 无 AI 辅助

---

### 5. 其他参考平台

| 平台 | 亮点 |
|---|---|
| **Scratch 官方社区** (scratch.mit.edu) | 全球最大 Scratch 社区，"Remix" 机制（基于他人作品二次创作）、工作室、标签系统、Curator 推荐 |
| **编程豆 (bcdou.cn)** | 轻量中文 Scratch 社区，自荐卡机制上首页、Markdown 评论 |
| **别针社区 (codingclip.com)** | 分类标签（动画/音乐/游戏/故事/教程）、多语言支持 |
| **优考试** | AI 智能录题（Word→自动切分）、内置 Scratch 编译器、霸屏防作弊、自动阅卷 + 证书发放 |

---

## 第二部分：竞品功能矩阵对比

| 功能维度 | 编程猫 | 小码王 | 斯坦星球 | OJ系统 | Scratch官方 | **我们的目标** |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| 在线创作(Scratch) | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ |
| 在线创作(Python) | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |
| 作品发布/分享 | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ |
| 点赞/收藏/评论 | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ |
| 关注/粉丝 | ✅ | ✅ | ❌ | ❌ | ✅ | ✅ |
| Remix(二次创作) | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |
| 工作室/组队 | ✅ | ❌ | ❌ | ✅ | ✅ | ✅ |
| 素材市场 | ✅ | ❌ | ❌ | ❌ | ✅ | ✅ |
| 课程体系 | ✅ | ✅ | ✅ | ❌ | ❌ | ✅ |
| OJ 刷题 | ❌ | ❌ | ❌ | ✅ | ❌ | ✅ |
| 自动判题 | ❌ | ❌ | ❌ | ✅ | ❌ | ✅ |
| 竞赛系统 | ❌(活动) | ❌ | ❌ | ✅ | ❌ | ✅ |
| 积分/等级/徽章 | 部分 | ❌ | ✅ | ❌ | ❌ | ✅ |
| 排行榜 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| AI 辅助编程 | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| AI 作品点评 | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| 班级/机构管理 | 弱 | 弱 | 弱 | 部分 | ❌ | ✅ |
| 多语言支持 | ✅ | ❌ | ❌ | 部分 | ❌ | ✅ |
| 微信生态 | 部分 | 部分 | 部分 | ❌ | ❌ | ✅ |
| 开源 | ❌ | ❌ | ❌ | 部分 | ✅ | ✅ |

---

## 第三部分：系统设计草案

### 3.1 产品定位

**一句话**：面向 K12 的一站式编程学习社区，融合「创作 + 分享 + 刷题 + 竞赛 + 积分 + AI」。

**核心差异化**：
1. **创作与评测一体化** — 不是割裂的"社区"和"OJ"，而是在同一个平台内完成创作→提交→评测→分享的闭环
2. **AI 原生** — 不是后加的 AI 功能，而是从架构层面就为 AI 设计（辅助编程、智能点评、题目生成、学习路径推荐）
3. **插件化扩展** — 核心平台 + 插件市场，支持后续无限扩展

### 3.2 目标用户角色

| 角色 | 场景 |
|---|---|
| **学生 (Student)** | 刷题、创作、分享、参加竞赛、查看学习路径、获取积分徽章 |
| **教师 (Teacher)** | 布置作业、批改、创建题目、组织竞赛、查看学情报告 |
| **机构管理员 (Admin)** | 管理班级/课程/用户、配置系统、查看运营数据 |
| **访客 (Guest)** | 浏览公开作品、查看排行榜（不登录也能体验部分内容） |
| **AI Agent** | 自动判题、生成点评、推荐学习路径、辅助出题 |

### 3.3 系统功能架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        🌐 前端层 (Vue 3 / React)                 │
├─────────┬──────────┬──────────┬──────────┬──────────┬──────────┤
│  创作    │  社区    │  学习    │  OJ      │  竞赛    │  管理    │
│  Editor │  Social  │  Learn   │  Judge   │  Contest │  Admin   │
└────┬────┴────┬─────┴────┬─────┴────┬─────┴────┬─────┴────┬─────┘
     │         │          │          │          │          │
┌────▼─────────▼──────────▼──────────▼──────────▼──────────▼─────┐
│                     🔌 API Gateway (Kong / Nginx)               │
├─────────────────────────────────────────────────────────────────┤
│                      🧩 微服务层 (Spring Boot / Go)              │
├──────────┬──────────┬──────────┬──────────┬──────────┬─────────┤
│ 用户服务  │ 创作服务  │ 社区服务  │ 题库服务  │ 竞赛服务  │ AI服务  │
│ user-svc │ editor-  │ social-  │ problem- │ contest- │ ai-svc  │
│          │ svc      │ svc      │ svc      │ svc      │         │
├──────────┼──────────┼──────────┼──────────┼──────────┼─────────┤
│ 课程服务  │ 积分服务  │ 消息服务  │ 文件服务  │ 通知服务  │ 分析服务 │
│ course-  │ points-  │ msg-svc  │ file-svc │ notify-  │analytics│
│ svc      │ svc      │          │          │ svc      │ -svc    │
└──────────┴──────────┴──────────┴──────────┴──────────┴─────────┘
     │         │          │          │          │          │
┌────▼─────────▼──────────▼──────────▼──────────▼──────────▼─────┐
│                        💾 数据层                                 │
├──────────┬──────────┬──────────┬──────────┬──────────┬─────────┤
│ MySQL    │ MongoDB  │ Redis    │ ES       │ MinIO    │ MQ      │
│ (核心数据)│ (作品JSON)│ (缓存/   │ (搜索/   │ (文件/   │ (异步   │
│          │          │  实时)    │  日志)    │  素材)    │  消息)   │
└──────────┴──────────┴──────────┴──────────┴──────────┴─────────┘
```

---

### 3.4 核心模块详细设计

#### 模块 A：创作引擎 (Editor Service)

**核心能力**：支持多语言在线创作

| 子系统 | 说明 |
|---|---|
| Scratch 3.0 编辑器 | 基于 scratch-vm + scratch-gui，支持自定义扩展积木 |
| Python 编辑器 | Monaco Editor + Pyodide（浏览器端 Python 运行） |
| 积木 → Python 转换 | 双向转换，帮助学生从图形化过渡到代码 |
| 素材库 | 角色/背景/声音/扩展素材，支持用户上传 + 官方库 |
| 云变量 | WebSocket 实时同步（Redis Hash 存储） |
| 自动保存 | 每 30 秒自动保存 + 版本快照 |
| 项目导出 | .sb3 / .py / 分享链接 / 嵌入代码 |

**关键创新**：
- **Remix 机制**：任何公开作品都可以被 "Remix"，自动记录作品族谱（原作者 → Remix 链）
- **协作编辑**：多人实时编辑同一个 Scratch 项目（基于 OT/CRDT 算法）
- **AI 积木助手**：输入自然语言 → AI 自动生成积木组合（如 "让小猫走到右边碰到边缘就反弹"）

#### 模块 B：社区引擎 (Social Service)

**核心能力**：UGC 内容社区

| 功能 | 说明 |
|---|---|
| 作品发布 | 标题/描述/标签/封面/可见性(公开/仅自己/仅班级) |
| Feed 流 | 关注动态 + 推荐动态 + 热门动态 三栏切换 |
| 互动 | 点赞、收藏、评论（支持 Markdown）、分享 |
| 关注体系 | 关注/粉丝、个人主页、创作统计 |
| 工作室 | 组队创作、团队作品集、协作编辑 |
| 标签系统 | 多级标签（语言/类型/难度/主题） |
| 内容审核 | AI 初筛 + 人工复审（敏感内容/版权） |
| 排行榜 | 周榜/月榜/总榜，多维度（热度/点赞/创作量/刷题量） |

**关键创新**：
- **作品 Remix 链可视化**：展示一个作品从原创到 N 次 Remix 的演化树
- **AI 作品点评**：自动分析作品并给出结构化点评（创意性/代码质量/复杂度/改进建议）
- **自荐卡机制**：用户可花费积分"自荐"作品上首页推荐位

#### 模块 C：OJ 判题引擎 (Judge Service)

**核心能力**：多语言自动评测

| 功能 | 说明 |
|---|---|
| 题目类型 | 选择题、填空题、Scratch 算法题、Scratch 创意题、Python 题、C++ 题 |
| 判题方式 | 标准判题（输出比对）、特判 SPJ（自定义逻辑）、创意题判题（AI 评分） |
| Scratch 判题 | 方案：提取积木 → 注入测试用例 → 运行 → 比对输出 |
| 测试数据 | 图形化生成器（支持数字/字符串/图/数组/树/矩阵等） |
| 部分分 | 按测试点给分（如 10 个测试点通过 7 个得 70%） |
| 代码评测沙箱 | Docker 容器隔离，限制时间/内存/网络 |
| 题解/讨论 | 每题附带官方题解 + 用户讨论区 |
| 错题本 | 自动收集错题，支持重练 |

**Scratch 判题核心原理**：

```
输入: Scratch 项目文件 (.sb3) + 测试用例

1. 解析 .sb3 → 提取积木 JSON
2. 静态分析 → 检查是否使用了禁用积木（如"询问并等待"改用注入方式）
3. 注入测试用例 → 修改"回答"积木的值
4. 运行 scratch-vm（headless） → 执行项目
5. 提取结果 → 比对输出/检测精灵状态/检查变量值
6. 返回 AC/WA/TLE/MLE

对于创意题:
1. 运行项目 → 截取舞台画面
2. AI 模型分析画面 → 评分（创意性/完成度/美观度）
```

**关键创新**：
- **AI 自动出题**：根据知识点和难度，AI 自动生成 Scratch 题目 + 测试用例
- **智能错题分析**：分析错误模式，定位知识薄弱点

#### 模块 D：竞赛引擎 (Contest Service)

**核心能力**：多模式竞赛系统

| 竞赛模式 | 说明 |
|---|---|
| 定时赛（Rated） | 固定时间窗口，按 AC 数 + 用时排名，影响积分 |
| 练习赛（Unrated） | 随时参加，不影响积分 |
| 专题赛 | 围绕特定知识点（如循环、条件、列表） |
| 创意赛 | 给定主题，提交作品，社区投票 + 评委打分 |
| 组队赛 | 2-5 人组队，团队排名 |
| 段位赛 | 按段位匹配对手，1v1 实时对战（解题速度） |

**关键创新**：
- **段位赛 (1v1)**：类似 LeetCode 周赛的实时对战，按段位匹配
- **AI 实时解说**：竞赛过程中 AI 生成实时解说（"选手 A 率先 AC 了第 3 题！"）
- **赛后自动题解**：比赛结束后 AI 自动生成每题的图文题解

#### 模块 E：积分与成就系统 (Points Service)

**积分获取**：

| 行为 | 积分 |
|---|---|
| 每日登录 | +5 |
| 提交作品 | +20 |
| 作品被点赞 | +2/次 |
| 作品被收藏 | +5/次 |
| 作品被 Remix | +10/次 |
| AC 一道题 | +10~50（按难度） |
| 竞赛排名 Top10 | +100~500 |
| 完成每日挑战 | +30 |
| 连续登录 7 天 | +100 |
| 帮助他人（评论被采纳） | +15 |

**积分消耗**：

| 行为 | 消耗 |
|---|---|
| 自荐作品上首页 | -100 |
| 解锁高级素材 | -50~200 |
| 购买头像框/称号 | -100~500 |
| 参加特殊竞赛 | -50 |

**等级体系**：

```
Lv.1  代码新手      (0)
Lv.2  积木学徒      (100)
Lv.3  编程新星      (500)
Lv.4  创意达人      (2000)
Lv.5  算法勇士      (5000)
Lv.6  编程大师      (15000)
Lv.7  代码宗师      (50000)
Lv.8  编程之神      (150000)
```

**徽章系统**：

| 类别 | 示例徽章 |
|---|---|
| 创作类 | 首次发布、10 个作品、100 个作品、作品被 100 人 Remix |
| 刷题类 | 首次 AC、100 AC、全站 Top10、连续 AC 30 天 |
| 竞赛类 | 首次参赛、获得奖牌、段位达到钻石 |
| 社交类 | 首次关注、100 粉丝、帮助 50 人 |
| 特殊类 | 早起鸟（6-8点登录）、夜猫子（23点后登录）、发现 Bug |

#### 模块 F：AI 服务 (AI Service)

| AI 能力 | 说明 |
|---|---|
| **AI 编程助手** | 自然语言 → Scratch 积木/Python 代码，对话式编程辅助 |
| **AI 作品点评** | 分析作品并给出多维度评价（创意/代码质量/复杂度/建议） |
| **AI 自动判题** | 创意题的智能评分（画面分析 + 逻辑分析） |
| **AI 出题助手** | 根据知识点/难度/题型自动生成题目 + 测试用例 |
| **AI 学习路径** | 根据学生能力画像推荐个性化学习路径 |
| **AI 错题分析** | 分析错题模式，生成针对性练习 |
| **AI 代码审查** | 分析 Scratch 积木/Python 代码，给出优化建议 |
| **AI 内容审核** | 自动检测违规内容（文字/图像/代码） |

**AI 架构**：
```
用户请求 → AI Gateway (统一调度)
              ├── LLM (大语言模型) → 对话/出题/点评/代码生成
              ├── CV Model (视觉模型) → 创意题评分/内容审核
              ├── Rec Model (推荐模型) → 学习路径/作品推荐
              └── Code Runner → 代码执行/测试用例验证
```

#### 模块 G：课程与学习路径 (Course Service)

| 功能 | 说明 |
|---|---|
| 课程体系 | 结构化课程（Scratch 入门 → 进阶 → Python → C++） |
| 课程单元 | 视频 + PPT + 案例 + 作业 + 测验 |
| 学习地图 | 可视化学习路径，节点解锁机制 |
| 自适应学习 | 根据答题/创作情况动态调整学习路径 |
| 作业系统 | 教师布置 → 学生提交 → 自动/手动批改 |
| 学情报告 | 学生个人报告 + 班级整体报告 |

---

### 3.5 数据库核心表设计

```sql
-- ========================================
-- 用户系统
-- ========================================
CREATE TABLE `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(32) NOT NULL UNIQUE,
  `nickname` VARCHAR(64),
  `avatar` VARCHAR(512),
  `email` VARCHAR(128),
  `phone` VARCHAR(20),
  `password_hash` VARCHAR(128),
  `role` ENUM('student','teacher','admin') DEFAULT 'student',
  `level` INT DEFAULT 1,
  `exp` BIGINT DEFAULT 0,        -- 经验值
  `points` BIGINT DEFAULT 0,     -- 积分余额
  `title` VARCHAR(64),            -- 当前称号
  `bio` VARCHAR(500),
  `status` TINYINT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_level` (`level`),
  INDEX `idx_points` (`points`)
);

-- ========================================
-- 创作系统
-- ========================================
CREATE TABLE `project` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(128) NOT NULL,
  `description` TEXT,
  `project_type` ENUM('scratch3','scratchjr','python','blockly') NOT NULL,
  `project_data` JSON,            -- Scratch 项目 JSON (sb3 解压后)
  `cover_url` VARCHAR(512),
  `file_url` VARCHAR(512),        -- .sb3 / .py 原始文件
  `visibility` ENUM('public','private','class') DEFAULT 'public',
  `remix_from` BIGINT,            -- Remix 来源项目 ID
  `tags` JSON,                    -- ["游戏","动画","故事"]
  `like_count` INT DEFAULT 0,
  `collect_count` INT DEFAULT 0,
  `remix_count` INT DEFAULT 0,
  `view_count` INT DEFAULT 0,
  `comment_count` INT DEFAULT 0,
  `is_featured` TINYINT DEFAULT 0,
  `status` ENUM('draft','published','reviewing','rejected') DEFAULT 'draft',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_user` (`user_id`),
  INDEX `idx_type_status` (`project_type`, `status`),
  INDEX `idx_likes` (`like_count` DESC),
  FULLTEXT `idx_search` (`title`, `description`)
);

CREATE TABLE `project_like` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `project_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_project` (`user_id`, `project_id`)
);

CREATE TABLE `project_comment` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `project_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `parent_id` BIGINT,             -- 回复的评论 ID
  `content` TEXT NOT NULL,
  `status` TINYINT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_project` (`project_id`)
);

-- ========================================
-- 题库系统
-- ========================================
CREATE TABLE `problem` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `title` VARCHAR(256) NOT NULL,
  `description` TEXT NOT NULL,     -- 题目描述 (Markdown)
  `problem_type` ENUM('choice','fill','scratch_algo','scratch_creative','python','cpp') NOT NULL,
  `difficulty` ENUM('easy','medium','hard','expert') NOT NULL,
  `tags` JSON,                    -- ["循环","条件","列表"]
  `time_limit` INT DEFAULT 1000,  -- ms
  `memory_limit` INT DEFAULT 256, -- MB
  `score` INT DEFAULT 100,        -- 总分
  `test_cases` JSON,              -- 测试用例 (对用户不可见)
  `spj_code` TEXT,                -- 特判代码
  `ai_solution` TEXT,             -- AI 生成的标准解法
  `ai_hint` TEXT,                 -- AI 提示
  `solution_count` INT DEFAULT 0,
  `ac_count` INT DEFAULT 0,
  `submit_count` INT DEFAULT 0,
  `author_id` BIGINT,
  `status` ENUM('draft','published','disabled') DEFAULT 'draft',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_difficulty` (`difficulty`),
  INDEX `idx_tags` ((CAST(`tags` AS CHAR(64) ARRAY)))
);

CREATE TABLE `problem_set` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `title` VARCHAR(128) NOT NULL,
  `description` TEXT,
  `problem_ids` JSON,             -- 题目 ID 列表
  `difficulty` ENUM('easy','medium','hard'),
  `category` VARCHAR(64),
  `created_by` BIGINT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `submission` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `problem_id` BIGINT NOT NULL,
  `contest_id` BIGINT,
  `code` TEXT,                    -- 提交的代码/积木 JSON
  `language` ENUM('scratch3','python','cpp') NOT NULL,
  `result` ENUM('pending','judging','AC','WA','TLE','MLE','RE','CE') DEFAULT 'pending',
  `score` INT DEFAULT 0,
  `time_used` INT,                -- ms
  `memory_used` INT,              -- KB
  `test_results` JSON,            -- 各测试点结果
  `ai_comment` TEXT,              -- AI 代码点评
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_user_problem` (`user_id`, `problem_id`),
  INDEX `idx_contest` (`contest_id`),
  INDEX `idx_result` (`result`)
);

-- ========================================
-- 竞赛系统
-- ========================================
CREATE TABLE `contest` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `title` VARCHAR(256) NOT NULL,
  `description` TEXT,
  `contest_type` ENUM('rated','unrated','themed','team','duel') NOT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `problem_ids` JSON,
  `max_participants` INT,
  `rules` JSON,                   -- 竞赛规则配置
  `status` ENUM('upcoming','running','finished') DEFAULT 'upcoming',
  `created_by` BIGINT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_status_time` (`status`, `start_time`)
);

CREATE TABLE `contest_rank` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `contest_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `team_id` BIGINT,
  `solved` INT DEFAULT 0,         -- AC 题数
  `penalty` INT DEFAULT 0,        -- 罚时(分钟)
  `score` INT DEFAULT 0,
  `rank` INT,
  `problem_details` JSON,         -- 每题的提交详情
  UNIQUE KEY `uk_contest_user` (`contest_id`, `user_id`)
);

-- ========================================
-- 积分与成就
-- ========================================
CREATE TABLE `points_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `amount` INT NOT NULL,          -- 正数获得，负数消耗
  `balance` BIGINT NOT NULL,      -- 变动后余额
  `action` VARCHAR(64) NOT NULL,  -- 'daily_login','submit_work','ac_problem','self_recommend'...
  `ref_id` BIGINT,                -- 关联对象 ID
  `ref_type` VARCHAR(32),         -- 'project','submission','contest'...
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_user_time` (`user_id`, `created_at`)
);

CREATE TABLE `badge` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `description` VARCHAR(256),
  `icon_url` VARCHAR(512),
  `category` ENUM('creation','problem','contest','social','special'),
  `condition_type` VARCHAR(64),   -- 'project_count','ac_count','login_streak'...
  `condition_value` INT,
  `rarity` ENUM('common','rare','epic','legendary'),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `user_badge` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `badge_id` BIGINT NOT NULL,
  `earned_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_badge` (`user_id`, `badge_id`)
);

-- ========================================
-- 课程系统
-- ========================================
CREATE TABLE `course` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `title` VARCHAR(128) NOT NULL,
  `description` TEXT,
  `cover_url` VARCHAR(512),
  `course_type` ENUM('scratch3','python','cpp','mixed') NOT NULL,
  `difficulty` ENUM('beginner','intermediate','advanced'),
  `unit_count` INT DEFAULT 0,
  `student_count` INT DEFAULT 0,
  `is_shared` TINYINT DEFAULT 0,
  `created_by` BIGINT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `course_unit` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `course_id` BIGINT NOT NULL,
  `title` VARCHAR(128) NOT NULL,
  `intro` VARCHAR(512),
  `cover_url` VARCHAR(512),
  `video_url` VARCHAR(512),
  `ppt_url` VARCHAR(512),
  `content` LONGTEXT,             -- 富文本课件
  `example_project_id` BIGINT,    -- 案例作品
  `problem_set_id` BIGINT,        -- 关联题单
  `order_num` INT DEFAULT 0,
  `map_x` INT,                    -- 学习地图坐标
  `map_y` INT,
  INDEX `idx_course` (`course_id`)
);

-- ========================================
-- 学习路径 & 错题本
-- ========================================
CREATE TABLE `learning_path` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `course_id` BIGINT NOT NULL,
  `current_unit_id` BIGINT,
  `progress` DECIMAL(5,2) DEFAULT 0,  -- 百分比
  `mastery` JSON,                 -- 知识点掌握度
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_course` (`user_id`, `course_id`)
);

CREATE TABLE `wrong_book` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `problem_id` BIGINT NOT NULL,
  `wrong_count` INT DEFAULT 1,
  `last_wrong_at` DATETIME,
  `mastered` TINYINT DEFAULT 0,
  UNIQUE KEY `uk_user_problem` (`user_id`, `problem_id`)
);

-- ========================================
-- 素材库
-- ========================================
CREATE TABLE `asset` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL,
  `asset_type` ENUM('sprite','backdrop','sound','costume','extension') NOT NULL,
  `file_url` VARCHAR(512),
  `thumbnail_url` VARCHAR(512),
  `md5` VARCHAR(32),
  `tags` JSON,
  `metadata` JSON,               -- 宽高/时长/格式等
  `source` ENUM('official','user','ai_generated') DEFAULT 'official',
  `download_count` INT DEFAULT 0,
  `created_by` BIGINT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_type` (`asset_type`),
  FULLTEXT `idx_search` (`name`)
);
```

---

### 3.6 技术选型建议

| 层级 | 推荐技术栈 | 理由 |
|---|---|---|
| **前端框架** | Vue 3 + TypeScript + Vite | 现代化、生态好、学习曲线适中 |
| **UI 组件库** | Ant Design Vue 4.x / Naive UI | 成熟稳定、组件丰富 |
| **Scratch 编辑器** | scratch-vm + scratch-gui (魔改) | 官方核心，社区生态好 |
| **代码编辑器** | Monaco Editor | VS Code 同款引擎 |
| **Python 运行** | Pyodide (浏览器端) / 后端沙箱 | 双端支持 |
| **API 网关** | Kong / APISIX | 高性能、插件丰富 |
| **后端框架** | Spring Boot 3.x / Go (Gin/Fiber) | Java 生态成熟 / Go 高性能 |
| **ORM** | MyBatis-Plus / GORM | 按语言选 |
| **数据库** | MySQL 8.0 (核心) + MongoDB (作品JSON) | 关系型 + 文档型互补 |
| **缓存** | Redis 7.x (Cluster) | 积分/排行榜/实时数据 |
| **搜索引擎** | Elasticsearch 8.x | 全文搜索/标签筛选 |
| **消息队列** | RocketMQ / RabbitMQ | 判题任务异步处理 |
| **文件存储** | MinIO (自建) / 阿里云 OSS | 作品/素材/视频 |
| **容器编排** | Docker + Kubernetes | 微服务部署/弹性伸缩 |
| **CI/CD** | GitLab CI / GitHub Actions | 自动化部署 |
| **监控** | Prometheus + Grafana | 全链路监控 |
| **AI 接入** | OpenAI API / 本地 LLM | 通用 AI 能力 |
| **判题沙箱** | Docker 容器隔离 | 安全执行用户代码 |

---

### 3.7 微服务拆分

| 服务名 | 职责 | 数据库 |
|---|---|---|
| `gateway-svc` | API 路由、限流、鉴权 | — |
| `user-svc` | 用户注册/登录/个人信息/关注 | MySQL (user DB) |
| `editor-svc` | 项目保存/加载/云变量/自动保存 | MongoDB + Redis |
| `social-svc` | 点赞/收藏/评论/Feed/排行榜 | MySQL + Redis |
| `problem-svc` | 题目管理/题单/测试用例 | MySQL |
| `judge-svc` | 判题调度/沙箱管理/结果计算 | MySQL + Docker |
| `contest-svc` | 竞赛管理/排名/实时对战 | MySQL + Redis |
| `points-svc` | 积分/等级/徽章/成就 | MySQL + Redis |
| `course-svc` | 课程/单元/学习路径/学情 | MySQL |
| `file-svc` | 文件上传/下载/素材管理 | MinIO |
| `msg-svc` | 站内信/通知/微信推送 | MySQL |
| `ai-svc` | AI 辅助/点评/出题/审核 | — (调用外部 LLM) |
| `analytics-svc` | 数据统计/报表/用户画像 | ES + ClickHouse |

---

### 3.8 关键业务流程

#### 流程 1：学生创作并发布作品

```
学生 → Scratch编辑器创作
  → 自动保存(MongoDB, 每30秒)
  → 点击"发布"
  → 填写标题/描述/标签
  → AI 内容审核(自动, <3秒)
    ├── 通过 → 发布成功 → 进入推荐池 → +20积分 → 推送给粉丝
    └── 拒绝 → 提示修改原因
```

#### 流程 2：刷题并自动判题

```
学生 → 选择题目 → 在线编码/搭建积木
  → 点击"提交"
  → 判题队列(MQ)
  → 判题沙箱(Docker)
    ├── Scratch: 注入测试用例 → 运行 scratch-vm → 比对输出
    ├── Python: 执行代码 → 比对标准输出
    └── 创意题: 运行 → 截取舞台 → AI 评分
  → 返回结果(AC/WA/TLE + 各测试点详情)
    ├── AC → +积分 → 更新排行榜 → 解锁成就检查
    └── WA → +错题本 → AI 提示 → 鼓励重试
```

#### 流程 3：参加竞赛

```
学生 → 浏览竞赛列表 → 报名参赛
  → 竞赛开始 → 进入竞赛页面
  → 逐题作答 → 提交判题(实时)
  → 排名实时更新(Redis Sorted Set)
  → 竞赛结束
    → 最终排名
    → 颁发徽章/积分
    → AI 自动生成题解
    → 竞赛复盘页面
```

---

### 3.9 扩展能力设计

| 扩展点 | 实现方式 |
|---|---|
| **新语言支持** | 插件式判题器：实现 `JudgePlugin` 接口即可新增语言 |
| **新题型** | 题目类型注册机制：新题型通过配置注册，无需改核心代码 |
| **新积木扩展** | Scratch 扩展积木 API：支持自定义积木（如硬件控制、AI 积木） |
| **第三方登录** | JustAuth 统一接入（微信/QQ/GitHub/钉钉） |
| **支付接入** | 抽象支付接口，支持微信/支付宝/积分混合支付 |
| **国际化** | Vue-i18n + 后端 MessageSource，支持中/英/繁 |
| **主题定制** | CSS 变量 + 主题配置文件，机构可自定义品牌色 |
| **数据开放** | GraphQL API + Webhook，支持第三方集成 |
| **硬件扩展** | 扩展积木支持 Micro:bit / Arduino / ESP32 等硬件 |
| **移动端** | 响应式 Web + 小程序（Uni-app 跨端） |

---

## 第四部分：与 Teaching-Open 的关系

### 4.1 可复用部分

| Teaching-Open 组件 | 复用方式 |
|---|---|
| 用户/角色/权限系统 | 直接复用 sys_user/role/permission 体系 |
| 班级/部门管理 | 复用 sys_depart 作为班级管理 |
| 课程/单元结构 | 复用 teaching_course / teaching_course_unit，扩展字段 |
| WebSocket 云变量 | 复用 ScratchWebSocket 方案 |
| 七牛/MinIO 存储 | 复用文件上传方案 |
| Docker 部署方案 | 参考 docker-compose 结构 |

### 4.2 需要重写部分

| 模块 | 原因 |
|---|---|
| 判题引擎 | Teaching-Open 无 OJ 能力，需全新开发 |
| 竞赛系统 | 全新模块 |
| 积分/徽章系统 | 全新模块 |
| AI 服务 | 全新模块 |
| 社区 Feed 流 | Teaching-Open 社区能力弱，需重构 |
| 前端 | Vue 2 → Vue 3 升级，UI 全面重新设计 |

### 4.3 架构升级方向

| 维度 | Teaching-Open | 新平台 |
|---|---|---|
| 架构 | 单体 Spring Boot | 微服务 |
| 前端 | Vue 2 + Ant Design 1.x | Vue 3 + 现代 UI |
| 数据库 | 单一 MySQL | MySQL + MongoDB + Redis + ES |
| 部署 | 单机 Docker Compose | K8s 集群 |
| AI | 无 | 内置 AI 能力 |
| OJ | 无 | 内置判题引擎 |
| 扩展性 | 有限 | 插件化架构 |

---

## 第五部分：MVP 路线图建议

### Phase 1 — 基础社区 (4-6 周)
- [ ] 用户注册/登录
- [ ] Scratch 在线编辑器（基于 scratch-vm）
- [ ] 作品发布/展示/点赞/收藏/评论
- [ ] 个人主页/关注体系
- [ ] 基础排行榜

### Phase 2 — OJ + 刷题 (4-6 周)
- [ ] 题目管理后台
- [ ] Scratch 算法题判题引擎
- [ ] Python 题判题
- [ ] 题单/分类/难度筛选
- [ ] 错题本

### Phase 3 — 竞赛 + 积分 (3-4 周)
- [ ] 定时竞赛
- [ ] 实时排名
- [ ] 积分系统
- [ ] 等级/徽章

### Phase 4 — AI + 课程 (4-6 周)
- [ ] AI 作品点评
- [ ] AI 编程助手
- [ ] 课程体系
- [ ] 学习地图

### Phase 5 — 高级功能 (持续迭代)
- [ ] Remix 机制
- [ ] 协作编辑
- [ ] 段位赛 1v1
- [ ] 移动端/小程序
- [ ] 硬件扩展积木
- [ ] 插件市场

---

*文档结束。下一步：确定技术选型和 MVP 范围，开始架构详细设计。*
