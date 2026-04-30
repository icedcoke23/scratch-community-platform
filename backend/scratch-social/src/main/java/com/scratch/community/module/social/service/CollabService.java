package com.scratch.community.module.social.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.result.ErrorCode;
import com.scratch.community.module.social.dto.CollabEvent;
import com.scratch.community.module.social.dto.EditOperation;
import com.scratch.community.module.social.entity.CollabParticipant;
import com.scratch.community.module.social.entity.CollabSession;
import com.scratch.community.common.repository.CrossModuleQueryRepository;
import com.scratch.community.module.social.mapper.CollabParticipantMapper;
import com.scratch.community.module.social.mapper.CollabSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协作编辑服务
 *
 * 核心职责:
 * 1. 管理协作会话的生命周期（创建/加入/退出/关闭）
 * 2. 维护会话内参与者的状态（角色/光标/活跃度）
 * 3. 处理编辑操作的冲突检测和广播
 * 4. 通过 WebSocket 推送实时事件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollabService {

    private final CollabSessionMapper sessionMapper;
    private final CollabParticipantMapper participantMapper;
    private final CrossModuleQueryRepository crossModuleQueryRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    /** 会话内版本号（内存维护，用于乐观锁） */
    private final Map<Long, Long> sessionVersions = new ConcurrentHashMap<>();

    /** 会话内最近操作历史（内存缓存，用于冲突检测） */
    private final Map<Long, LinkedList<EditOperation>> sessionHistory = new ConcurrentHashMap<>();

    /** 最大历史记录数 */
    private static final int MAX_HISTORY_SIZE = 100;

    /**
     * 创建协作会话
     */
    @Transactional
    public CollabSession createSession(Long projectId, Long ownerId) {
        // 检查是否已有活跃会话
        CollabSession existing = sessionMapper.selectOne(
                new LambdaQueryWrapper<CollabSession>()
                        .eq(CollabSession::getProjectId, projectId)
                        .eq(CollabSession::getStatus, "active"));

        if (existing != null) {
            return existing;
        }

        CollabSession session = new CollabSession();
        session.setProjectId(projectId);
        session.setOwnerId(ownerId);
        session.setStatus("active");
        session.setMaxEditors(5);
        sessionMapper.insert(session);

        // 初始化版本号
        sessionVersions.put(session.getId(), 0L);
        sessionHistory.put(session.getId(), new LinkedList<>());

        log.info("创建协作会话: sessionId={}, projectId={}, ownerId={}", session.getId(), projectId, ownerId);
        return session;
    }

    /**
     * 加入协作会话
     */
    @Transactional
    public CollabParticipant joinSession(Long sessionId, Long userId, String role) {
        CollabSession session = sessionMapper.selectById(sessionId);
        if (session == null || !"active".equals(session.getStatus())) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "协作会话不存在或已关闭");
        }

        // 检查是否已在会话中
        CollabParticipant existing = participantMapper.selectOne(
                new LambdaQueryWrapper<CollabParticipant>()
                        .eq(CollabParticipant::getSessionId, sessionId)
                        .eq(CollabParticipant::getUserId, userId));

        if (existing != null) {
            // 更新最后活跃时间
            existing.setLastActiveAt(LocalDateTime.now());
            participantMapper.updateById(existing);
            return existing;
        }

        // 检查编辑者数量限制
        if ("editor".equals(role)) {
            long editorCount = participantMapper.selectCount(
                    new LambdaQueryWrapper<CollabParticipant>()
                            .eq(CollabParticipant::getSessionId, sessionId)
                            .eq(CollabParticipant::getRole, "editor"));
            if (editorCount >= session.getMaxEditors()) {
                throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "编辑者数量已达上限");
            }
        }

        CollabParticipant participant = new CollabParticipant();
        participant.setSessionId(sessionId);
        participant.setUserId(userId);
        participant.setRole(role != null ? role : "viewer");
        participant.setCursorX(0);
        participant.setCursorY(0);
        participant.setLastActiveAt(LocalDateTime.now());
        participantMapper.insert(participant);

        // 广播用户加入事件
        Map<String, Object> userInfo = crossModuleQueryRepository.getUserBasicInfo(userId);
        String nickname = userInfo != null ? (String) userInfo.get("nickname") : "unknown";
        broadcastEvent(sessionId, CollabEvent.of("user_joined", sessionId, userId, nickname,
                Map.of("role", participant.getRole(), "nickname", nickname)));

        log.info("用户加入协作会话: sessionId={}, userId={}, role={}", sessionId, userId, role);
        return participant;
    }

    /**
     * 离开协作会话
     */
    @Transactional
    public void leaveSession(Long sessionId, Long userId) {
        int deleted = participantMapper.delete(
                new LambdaQueryWrapper<CollabParticipant>()
                        .eq(CollabParticipant::getSessionId, sessionId)
                        .eq(CollabParticipant::getUserId, userId));

        if (deleted > 0) {
            Map<String, Object> userInfo = crossModuleQueryRepository.getUserBasicInfo(userId);
            String nickname = userInfo != null ? (String) userInfo.get("nickname") : "unknown";
            broadcastEvent(sessionId, CollabEvent.of("user_left", sessionId, userId, nickname, null));
            log.info("用户离开协作会话: sessionId={}, userId={}", sessionId, userId);
        }

        // 如果会话内无人了，自动关闭
        long remaining = participantMapper.selectCount(
                new LambdaQueryWrapper<CollabParticipant>()
                        .eq(CollabParticipant::getSessionId, sessionId));
        if (remaining == 0) {
            closeSession(sessionId);
        }
    }

    /**
     * 关闭协作会话
     */
    @Transactional
    public void closeSession(Long sessionId) {
        CollabSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setStatus("closed");
            sessionMapper.updateById(session);

            // 清理内存缓存
            sessionVersions.remove(sessionId);
            sessionHistory.remove(sessionId);

            // 广播会话关闭
            broadcastEvent(sessionId, CollabEvent.of("session_closed", sessionId, 0L, "system", null));

            log.info("关闭协作会话: sessionId={}", sessionId);
        }
    }

    /**
     * 处理编辑操作
     *
     * 使用乐观锁: 操作携带版本号，与服务端版本对比
     * - 版本匹配 → 应用操作，版本 +1，广播
     * - 版本冲突 → 返回冲突事件，客户端需要同步最新状态
     */
    public EditOperation handleEdit(Long sessionId, Long userId, EditOperation op) {
        // 验证用户是否为编辑者
        CollabParticipant participant = participantMapper.selectOne(
                new LambdaQueryWrapper<CollabParticipant>()
                        .eq(CollabParticipant::getSessionId, sessionId)
                        .eq(CollabParticipant::getUserId, userId));

        if (participant == null || !"editor".equals(participant.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "无编辑权限");
        }

        // 乐观锁版本检查
        Long currentVersion = sessionVersions.getOrDefault(sessionId, 0L);
        if (op.getVersion() != null && !op.getVersion().equals(currentVersion)) {
            // 版本冲突
            Map<String, Object> conflictUserInfo = crossModuleQueryRepository.getUserBasicInfo(userId);
            String nickname = conflictUserInfo != null ? (String) conflictUserInfo.get("nickname") : "unknown";
            messagingTemplate.convertAndSendToUser(
                    userId.toString(), "/queue/collab/conflict",
                    CollabEvent.of("conflict", sessionId, userId, nickname,
                            Map.of("expected", op.getVersion(), "actual", currentVersion,
                                    "history", getRecentHistory(sessionId))));
            log.warn("编辑冲突: sessionId={}, userId={}, expected={}, actual={}", sessionId, userId, op.getVersion(), currentVersion);
            return null;
        }

        // 应用操作：版本 +1
        Long newVersion = currentVersion + 1;
        sessionVersions.put(sessionId, newVersion);

        // 记录操作历史
        op.setVersion(newVersion);
        op.setTimestamp(System.currentTimeMillis());
        addHistory(sessionId, op);

        // 更新参与者活跃时间
        participant.setLastActiveAt(LocalDateTime.now());
        participantMapper.updateById(participant);

        // 广播编辑操作给会话内所有人
        Map<String, Object> editUserInfo = crossModuleQueryRepository.getUserBasicInfo(userId);
        String nickname = editUserInfo != null ? (String) editUserInfo.get("nickname") : "unknown";
        broadcastEvent(sessionId, CollabEvent.of("edit_applied", sessionId, userId, nickname, op));

        log.debug("编辑操作已应用: sessionId={}, userId={}, version={}, type={}", sessionId, userId, newVersion, op.getType());
        return op;
    }

    /**
     * 更新光标位置
     */
    public void updateCursor(Long sessionId, Long userId, int x, int y) {
        CollabParticipant participant = participantMapper.selectOne(
                new LambdaQueryWrapper<CollabParticipant>()
                        .eq(CollabParticipant::getSessionId, sessionId)
                        .eq(CollabParticipant::getUserId, userId));

        if (participant != null) {
            participant.setCursorX(x);
            participant.setCursorY(y);
            participant.setLastActiveAt(LocalDateTime.now());
            participantMapper.updateById(participant);

            Map<String, Object> cursorUserInfo = crossModuleQueryRepository.getUserBasicInfo(userId);
            String nickname = cursorUserInfo != null ? (String) cursorUserInfo.get("nickname") : "unknown";
            broadcastEvent(sessionId, CollabEvent.of("cursor_update", sessionId, userId, nickname,
                    Map.of("x", x, "y", y)));
        }
    }

    /**
     * 发送聊天消息
     */
    public void sendChat(Long sessionId, Long userId, String message) {
        Map<String, Object> chatUserInfo = crossModuleQueryRepository.getUserBasicInfo(userId);
        String nickname = chatUserInfo != null ? (String) chatUserInfo.get("nickname") : "unknown";
        broadcastEvent(sessionId, CollabEvent.of("chat", sessionId, userId, nickname,
                Map.of("message", message, "nickname", nickname)));
    }

    /**
     * 获取会话当前状态（新加入者同步用）
     */
    public Map<String, Object> getSessionState(Long sessionId) {
        CollabSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "会话不存在");
        }

        List<CollabParticipant> participants = participantMapper.selectList(
                new LambdaQueryWrapper<CollabParticipant>()
                        .eq(CollabParticipant::getSessionId, sessionId));

        List<Map<String, Object>> participantList = new ArrayList<>();
        for (CollabParticipant p : participants) {
            Map<String, Object> pUserInfo = crossModuleQueryRepository.getUserBasicInfo(p.getUserId());
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("userId", p.getUserId());
            info.put("nickname", pUserInfo != null ? pUserInfo.get("nickname") : "unknown");
            info.put("avatarUrl", pUserInfo != null ? pUserInfo.get("avatar_url") : null);
            info.put("role", p.getRole());
            info.put("cursorX", p.getCursorX());
            info.put("cursorY", p.getCursorY());
            participantList.add(info);
        }

        Map<String, Object> state = new LinkedHashMap<>();
        state.put("sessionId", session.getId());
        state.put("projectId", session.getProjectId());
        state.put("ownerId", session.getOwnerId());
        state.put("status", session.getStatus());
        state.put("version", sessionVersions.getOrDefault(sessionId, 0L));
        state.put("participants", participantList);
        state.put("recentOperations", getRecentHistory(sessionId));

        return state;
    }

    /**
     * 获取项目关联的活跃会话
     */
    public CollabSession getActiveSession(Long projectId) {
        return sessionMapper.selectOne(
                new LambdaQueryWrapper<CollabSession>()
                        .eq(CollabSession::getProjectId, projectId)
                        .eq(CollabSession::getStatus, "active"));
    }

    /**
     * 清理不活跃的会话（定时任务调用）
     */
    @Transactional
    public int cleanupInactiveSessions(int timeoutMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);

        // 找到所有不活跃参与者的会话
        List<CollabParticipant> inactive = participantMapper.selectList(
                new LambdaQueryWrapper<CollabParticipant>()
                        .lt(CollabParticipant::getLastActiveAt, cutoff));

        Set<Long> sessionIds = new HashSet<>();
        for (CollabParticipant p : inactive) {
            sessionIds.add(p.getSessionId());
            participantMapper.deleteById(p.getId());
        }

        // 关闭没有参与者的会话
        int closed = 0;
        for (Long sessionId : sessionIds) {
            long remaining = participantMapper.selectCount(
                    new LambdaQueryWrapper<CollabParticipant>()
                            .eq(CollabParticipant::getSessionId, sessionId));
            if (remaining == 0) {
                closeSession(sessionId);
                closed++;
            }
        }

        if (closed > 0) {
            log.info("清理不活跃协作会话: {} 个", closed);
        }
        return closed;
    }

    // ==================== 私有方法 ====================

    private void broadcastEvent(Long sessionId, CollabEvent event) {
        messagingTemplate.convertAndSend("/topic/collab/" + sessionId, event);
    }

    private void addHistory(Long sessionId, EditOperation op) {
        LinkedList<EditOperation> history = sessionHistory.computeIfAbsent(sessionId, k -> new LinkedList<>());
        history.addLast(op);
        while (history.size() > MAX_HISTORY_SIZE) {
            history.removeFirst();
        }
    }

    private List<EditOperation> getRecentHistory(Long sessionId) {
        LinkedList<EditOperation> history = sessionHistory.get(sessionId);
        if (history == null) return Collections.emptyList();
        return new ArrayList<>(history);
    }
}
