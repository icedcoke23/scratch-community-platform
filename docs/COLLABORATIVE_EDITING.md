# 协作编辑功能设计文档

> 版本: v1.0 | 日期: 2026-04-28
> 状态: 设计阶段

---

## 一、功能概述

支持多人同时编辑同一个 Scratch 项目，实现实时协作创作。

### 核心场景

1. **师生协作**: 教师实时指导学生修改项目
2. **小组合作**: 多名学生共同完成一个项目
3. **代码审查**: 教师在线查看学生编辑过程

---

## 二、技术方案

### 2.1 架构选型

| 方案 | 优点 | 缺点 | 推荐度 |
|------|------|------|--------|
| **OT (Operational Transformation)** | 成熟稳定，Google Docs 使用 | 实现复杂，Scratch 积木适配难 | ⭐⭐⭐ |
| **CRDT (Conflict-free Replicated Data Type)** | 无冲突，天然分布式 | 内存开销大，实现复杂 | ⭐⭐ |
| **WebSocket + 锁定机制** | 实现简单，延迟低 | 并发粒度粗，用户等待 | ⭐⭐⭐⭐ |
| **WebSocket + 乐观锁** | 平衡并发和实现复杂度 | 需要冲突解决策略 | ⭐⭐⭐⭐⭐ |

**推荐方案: WebSocket + 乐观锁**

理由:
- Scratch 项目以积木块为单位，粒度较粗，不需要字符级 OT
- 乐观锁可以处理大多数并发场景
- 实现复杂度适中，适合单人开发团队

### 2.2 系统架构

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  用户 A     │     │  用户 B     │     │  用户 C     │
│  (编辑者)   │     │  (编辑者)   │     │  (观察者)   │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │
       │ WebSocket         │ WebSocket         │ WebSocket
       │                   │                   │
       └───────────────────┼───────────────────┘
                           │
                    ┌──────▼──────┐
                    │  WebSocket  │
                    │   Server    │
                    │  (Spring)   │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │   Redis     │
                    │  (Pub/Sub)  │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │   MySQL     │
                    │  (持久化)   │
                    └─────────────┘
```

### 2.3 数据模型

#### 协作会话表 (collab_session)

```sql
CREATE TABLE collab_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL COMMENT '项目 ID',
    owner_id BIGINT NOT NULL COMMENT '创建者 ID',
    status VARCHAR(20) DEFAULT 'active' COMMENT '会话状态: active/closed',
    max_editors INT DEFAULT 5 COMMENT '最大编辑者数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id),
    INDEX idx_owner_id (owner_id),
    INDEX idx_status (status)
);
```

#### 协作参与者表 (collab_participant)

```sql
CREATE TABLE collab_participant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL COMMENT '会话 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    role VARCHAR(20) DEFAULT 'viewer' COMMENT '角色: editor/viewer',
    cursor_x INT DEFAULT 0 COMMENT '光标 X 坐标',
    cursor_y INT DEFAULT 0 COMMENT '光标 Y 坐标',
    last_active_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_session_user (session_id, user_id),
    INDEX idx_session_id (session_id)
);
```

### 2.4 WebSocket 消息协议

```typescript
// 客户端 → 服务端
interface ClientMessage {
  type: 'join' | 'leave' | 'edit' | 'cursor' | 'chat';
  sessionId: string;
  payload: any;
}

// 服务端 → 客户端
interface ServerMessage {
  type: 'user_joined' | 'user_left' | 'edit_applied' | 'cursor_update' |
        'conflict' | 'error' | 'session_state';
  sessionId: string;
  userId: number;
  payload: any;
}

// 编辑操作
interface EditOperation {
  type: 'add_block' | 'remove_block' | 'move_block' | 'modify_block' |
        'add_sprite' | 'remove_sprite' | 'modify_variable';
  targetId: string;       // 目标积木/角色 ID
  data: any;              // 操作数据
  version: number;        // 版本号（乐观锁）
  timestamp: number;      // 时间戳
}
```

### 2.5 冲突解决策略

```
1. 编辑同一积木块 → 后到者收到冲突提示，选择覆盖或保留
2. 编辑不同积木块 → 自动合并，无冲突
3. 删除被引用的积木 → 提示确认，防止级联破坏
4. 同时添加同名变量 → 自动重命名（添加后缀）
```

---

## 三、实现计划

### Phase 1: 基础协作（2 周）

- [ ] WebSocket 服务搭建（Spring WebSocket + STOMP）
- [ ] 协作会话创建/加入/退出
- [ ] 光标位置同步
- [ ] 在线用户列表
- [ ] 观察者模式（只读查看）

### Phase 2: 实时编辑（2 周）

- [ ] 积木操作同步（添加/删除/移动）
- [ ] 乐观锁版本控制
- [ ] 冲突检测和提示
- [ ] 操作历史记录（撤销/重做）

### Phase 3: 增强功能（1 周）

- [ ] 协作聊天室
- [ ] 编辑权限管理（编辑者/观察者切换）
- [ ] 协作会话录制和回放
- [ ] 教师批注功能

---

## 四、前端组件设计

```vue
<!-- CollabEditor.vue -->
<template>
  <div class="collab-editor">
    <!-- 协作工具栏 -->
    <CollabToolbar
      :session="session"
      :participants="participants"
      @leave="leaveSession"
      @toggle-role="toggleRole"
    />

    <!-- Scratch 编辑器（已有） -->
    <ScratchEditor
      ref="editor"
      :project-id="projectId"
      :readonly="isViewer"
      @change="onEditorChange"
    />

    <!-- 协作光标层 -->
    <CollabCursors :cursors="remoteCursors" />

    <!-- 协作聊天面板 -->
    <CollabChat
      v-if="showChat"
      :messages="messages"
      @send="sendMessage"
    />
  </div>
</template>
```

---

## 五、安全考虑

1. **权限控制**: 只有项目所有者或被邀请者可以加入协作
2. **频率限制**: 编辑操作每秒最多 10 次，防止恶意刷屏
3. **内容校验**: 同步的操作必须通过 sb3 格式校验
4. **会话超时**: 30 分钟无操作自动关闭协作会话
5. **并发限制**: 每个会话最多 5 个编辑者，20 个观察者
