package com.scratch.community.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.common.audit.SensitiveWordFilter;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.result.ErrorCode;
import com.scratch.community.module.system.entity.ContentAuditLog;
import com.scratch.community.module.system.mapper.ContentAuditLogMapper;
import com.scratch.community.module.system.dto.AuditActionDTO;
import com.scratch.community.module.system.vo.AuditLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 内容审核服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final ContentAuditLogMapper auditLogMapper;
    private final SensitiveWordFilter sensitiveWordFilter;

    /**
     * 提交内容审核（自动敏感词检测）
     * @return 审核记录 ID，如果自动通过则 status=passed
     */
    @Transactional
    public Long submitAudit(String contentType, Long contentId, String contentText) {
        ContentAuditLog auditLog = new ContentAuditLog();
        auditLog.setContentType(contentType);
        auditLog.setContentId(contentId);
        auditLog.setContentText(contentText);

        // 自动敏感词检测
        if (sensitiveWordFilter.containsSensitiveWord(contentText)) {
            auditLog.setStatus("rejected");
            auditLog.setReason("自动检测: 内容包含敏感词");
            log.warn("内容审核自动拒绝: type={}, id={}", contentType, contentId);
        } else {
            auditLog.setStatus("passed");
        }

        auditLogMapper.insert(auditLog);
        return auditLog.getId();
    }

    /**
     * 查询审核记录列表（分页，管理员）
     */
    @Transactional(readOnly = true)
    public Page<AuditLogVO> listAuditLogs(String status, Page<ContentAuditLog> page) {
        LambdaQueryWrapper<ContentAuditLog> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(ContentAuditLog::getStatus, status);
        }
        wrapper.orderByDesc(ContentAuditLog::getCreatedAt);

        Page<ContentAuditLog> result = auditLogMapper.selectPage(page, wrapper);
        Page<AuditLogVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    /**
     * 管理员审核操作（通过/拒绝）
     */
    @Transactional
    public void auditAction(Long auditId, Long operatorId, AuditActionDTO dto) {
        ContentAuditLog auditLog = auditLogMapper.selectById(auditId);
        if (auditLog == null) {
            throw new BizException(ErrorCode.CONFIG_NOT_FOUND);
        }

        String action = dto.getAction();
        if (!"passed".equals(action) && !"rejected".equals(action)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "操作类型必须为 passed 或 rejected");
        }

        if ("rejected".equals(action) && (dto.getReason() == null || dto.getReason().isBlank())) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "拒绝时必须填写原因");
        }

        auditLog.setStatus(action);
        auditLog.setOperatorId(operatorId);
        if ("rejected".equals(action)) {
            auditLog.setReason(dto.getReason());
        }
        auditLogMapper.updateById(auditLog);
        log.info("审核操作: auditId={}, action={}, operatorId={}", auditId, action, operatorId);
    }

    private AuditLogVO toVO(ContentAuditLog entity) {
        AuditLogVO vo = new AuditLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
