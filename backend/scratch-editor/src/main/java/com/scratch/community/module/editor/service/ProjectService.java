package com.scratch.community.module.editor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.common.event.PointEvent;
import com.scratch.community.common.event.ProjectViewEvent;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.repository.CrossModuleQueryRepository;
import com.scratch.community.common.repository.CrossModuleWriteRepository;
import com.scratch.community.common.result.ErrorCode;
import com.scratch.community.common.util.FileUploadUtils;
import com.scratch.community.module.editor.dto.CreateProjectDTO;
import com.scratch.community.module.editor.dto.UpdateProjectDTO;
import com.scratch.community.module.editor.entity.Project;
import com.scratch.community.module.editor.mapper.ProjectMapper;
import com.scratch.community.module.editor.vo.ProjectDetailVO;
import com.scratch.community.module.editor.vo.ProjectVO;
import com.scratch.community.sb3.model.SB3ParseResult;
import com.scratch.community.sb3.parser.SB3Parser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 项目服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final SB3Parser sb3Parser;
    private final FileUploadUtils fileUploadUtils;
    private final CrossModuleQueryRepository crossModuleQuery;
    private final CrossModuleWriteRepository crossModuleWrite;
    private final com.scratch.community.common.event.EventPublisherHelper eventPublisherHelper;

    /**
     * 创建项目
     */
    @Transactional
    public ProjectVO create(Long userId, CreateProjectDTO dto) {
        Project project = new Project();
        BeanUtils.copyProperties(dto, project);
        project.setUserId(userId);
        project.setStatus("draft");
        project.setBlockCount(0);
        project.setComplexityScore(0.0);
        project.setLikeCount(0);
        project.setCommentCount(0);
        project.setViewCount(0);
        projectMapper.insert(project);
        return toVO(project);
    }

    /**
     * 我的项目列表（分页）
     */
    @Transactional(readOnly = true)
    public Page<ProjectVO> myProjects(Long userId, Page<Project> page) {
        Page<Project> result = projectMapper.selectPage(page,
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getUserId, userId)
                        .orderByDesc(Project::getCreatedAt)
        );
        return toVOPage(result);
    }

    /**
     * 项目详情（浏览量 +1）
     */
    @Transactional
    public ProjectDetailVO getDetail(Long userId, Long projectId) {
        Project project = getAndCheck(projectId);

        // 通过事件驱动更新浏览数（异步，不阻塞主流程）
        eventPublisherHelper.publishEvent(
                new ProjectViewEvent(this, projectId, userId),
                "浏览事件",
                () -> crossModuleWrite.incrementProjectViewCount(projectId)
        );

        ProjectDetailVO vo = new ProjectDetailVO();
        BeanUtils.copyProperties(project, vo);
        // 浏览数 +1 后更新 VO
        vo.setViewCount(project.getViewCount() + 1);

        // 查询作者信息
        try {
            Map<String, Object> authorInfo = crossModuleQuery.getUserBasicInfo(project.getUserId());
            if (authorInfo != null) {
                vo.setAuthorName((String) authorInfo.get("nickname"));
                vo.setAuthorAvatar((String) authorInfo.get("avatar_url"));
            }
        } catch (Exception e) {
            log.warn("查询作者信息失败: userId={}", project.getUserId());
        }

        // 查询当前用户是否已点赞
        if (userId != null) {
            try {
                Set<Long> likedIds = crossModuleQuery.getLikedProjectIds(userId, List.of(projectId));
                vo.setIsLiked(likedIds.contains(projectId));
            } catch (Exception e) {
                log.warn("查询点赞状态失败: userId={}, projectId={}", userId, projectId);
                vo.setIsLiked(false);
            }
        } else {
            vo.setIsLiked(false);
        }

        return vo;
    }

    /**
     * 更新项目信息
     */
    @Transactional
    public void update(Long userId, Long projectId, UpdateProjectDTO dto) {
        Project project = getAndCheckOwner(userId, projectId);
        if (dto.getTitle() != null) project.setTitle(dto.getTitle());
        if (dto.getDescription() != null) project.setDescription(dto.getDescription());
        if (dto.getCoverUrl() != null) project.setCoverUrl(dto.getCoverUrl());
        if (dto.getTags() != null) project.setTags(dto.getTags());
        projectMapper.updateById(project);
    }

    /**
     * 删除项目（软删除）
     */
    @Transactional
    public void delete(Long userId, Long projectId) {
        getAndCheckOwner(userId, projectId);
        projectMapper.deleteById(projectId);
    }

    /**
     * 上传 sb3 文件 + 自动解析
     * 文件大小/类型校验在 Controller 层完成
     */
    @Transactional
    public void uploadSb3(Long userId, Long projectId, MultipartFile file) {
        Project project = getAndCheckOwner(userId, projectId);

        // 1. 上传到 MinIO
        String key = fileUploadUtils.upload(file, "sb3");
        String url = fileUploadUtils.getUrl("sb3", key);

        // 2. 解析 sb3
        SB3ParseResult parseResult;
        try {
            parseResult = sb3Parser.parse(file.getBytes());
        } catch (Exception e) {
            log.warn("sb3 解析失败，仅保存文件: projectId={}, error={}", projectId, e.getMessage());
            // 解析失败不阻断上传，仅保存文件
            project.setSb3Url(url);
            projectMapper.updateById(project);
            return;
        }

        // 3. 更新项目
        project.setSb3Url(url);
        project.setBlockCount(parseResult.getBlockCount());
        project.setComplexityScore(parseResult.getComplexityScore());
        try {
            project.setParseResult(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(parseResult));
        } catch (Exception e) {
            log.warn("序列化解析结果失败: {}", e.getMessage());
            project.setParseResult("{}");
        }
        projectMapper.updateById(project);
    }

    /**
     * 自动保存 sb3（base64 编码数据）
     * <p>编辑器自动保存时调用，不触发解析（解析在首次上传时已完成）
     * <p>自动保存频率高，只更新文件，不重复解析
     */
    @Transactional
    public void autoSaveSb3(Long userId, Long projectId, String base64Data) {
        Project project = getAndCheckOwner(userId, projectId);

        byte[] sb3Bytes;
        try {
            sb3Bytes = Base64.getDecoder().decode(base64Data);
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.SB3_FORMAT_ERROR.getCode(), "sb3 数据格式错误（Base64 解码失败）");
        }

        // 大小校验
        if (sb3Bytes.length > 100 * 1024 * 1024) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "文件大小超过 100MB 限制");
        }

        // 上传到 MinIO
        String key = fileUploadUtils.uploadBytes(sb3Bytes, "sb3", "project_" + projectId + ".sb3");
        String url = fileUploadUtils.getUrl("sb3", key);

        // 更新项目（仅更新 URL，不重复解析）
        project.setSb3Url(url);
        projectMapper.updateById(project);

        log.info("自动保存 sb3: projectId={}, size={}KB", projectId, sb3Bytes.length / 1024);
    }

    /**
     * 获取 sb3 下载链接（短期 presigned URL，有效期 1 小时）
     * <p>用于 TurboWarp project_url 加载等需要公开访问的场景
     */
    @Transactional(readOnly = true)
    public String getSb3Url(Long userId, Long projectId) {
        Project project = getAndCheck(projectId);
        if (project.getSb3Url() == null) {
            throw new BizException(ErrorCode.SB3_FORMAT_ERROR.getCode(), "项目未上传 sb3 文件");
        }
        // 生成 1 小时有效的 presigned URL（TurboWarp 需要可公开访问的链接）
        return fileUploadUtils.refreshPresignedUrl(project.getSb3Url(), 1, java.util.concurrent.TimeUnit.HOURS);
    }

    /**
     * 发布项目
     */
    @Transactional
    public void publish(Long userId, Long projectId) {
        Project project = getAndCheckOwner(userId, projectId);
        if (project.getSb3Url() == null) {
            throw new BizException(ErrorCode.SB3_FORMAT_ERROR.getCode(), "请先上传 sb3 文件再发布");
        }
        project.setStatus("published");
        projectMapper.updateById(project);

        // 发布积分事件
        eventPublisherHelper.publishEvent(
                new PointEvent(this, userId, PointEvent.PointAction.PUBLISH_PROJECT, projectId),
                "发布项目积分事件"
        );
    }

    /**
     * Remix 项目（二次创作）
     * 复制原项目信息，设置 remix 关联，原项目 remixCount +1
     */
    @Transactional
    public ProjectVO remix(Long userId, Long originalProjectId) {
        Project original = getAndCheck(originalProjectId);
        if (!"published".equals(original.getStatus())) {
            throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
        }
        if (original.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "不能 Remix 自己的项目");
        }

        // 创建新项目（复制原项目信息）
        Project remix = new Project();
        remix.setUserId(userId);
        remix.setTitle(original.getTitle() + " (Remix)");
        remix.setDescription(original.getDescription());
        remix.setCoverUrl(original.getCoverUrl());
        remix.setSb3Url(original.getSb3Url()); // 复制 sb3 文件链接
        remix.setStatus("draft");
        remix.setBlockCount(original.getBlockCount());
        remix.setComplexityScore(original.getComplexityScore());
        remix.setLikeCount(0);
        remix.setCommentCount(0);
        remix.setViewCount(0);
        remix.setTags(original.getTags());
        remix.setRemixProjectId(originalProjectId);
        remix.setRemixCount(0);
        remix.setParseResult(original.getParseResult());
        projectMapper.insert(remix);

        // 原项目 remixCount +1
        crossModuleWrite.incrementProjectRemixCount(originalProjectId);

        log.info("Remix 项目: userId={}, originalId={}, newId={}", userId, originalProjectId, remix.getId());
        return toVO(remix);
    }

    /**
     * 获取项目的 Remix 列表（分页）
     */
    @Transactional(readOnly = true)
    public Page<ProjectVO> getRemixes(Long projectId, Page<Project> page) {
        Page<Project> result = projectMapper.selectPage(page,
                new LambdaQueryWrapper<Project>()
                        .eq(Project::getRemixProjectId, projectId)
                        .eq(Project::getStatus, "published")
                        .orderByDesc(Project::getCreatedAt));
        return toVOPage(result);
    }

    /**
     * 获取项目的 Remix 原始链（从当前项目追溯到最原始的原创项目）
     */
    @Transactional(readOnly = true)
    public java.util.List<ProjectVO> getRemixChain(Long projectId) {
        java.util.List<ProjectVO> chain = new java.util.ArrayList<>();
        Long currentId = projectId;
        int maxDepth = 10; // 防止无限循环

        while (currentId != null && maxDepth-- > 0) {
            Project project = projectMapper.selectById(currentId);
            if (project == null) break;
            chain.add(0, toVO(project)); // 插入到头部
            currentId = project.getRemixProjectId();
        }
        return chain;
    }

    // ==================== 私有方法 ====================

    private Project getAndCheck(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
        }
        return project;
    }

    private Project getAndCheckOwner(Long userId, Long projectId) {
        Project project = getAndCheck(projectId);
        if (!project.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.PROJECT_NO_PERMISSION);
        }
        return project;
    }

    private ProjectVO toVO(Project project) {
        ProjectVO vo = new ProjectVO();
        BeanUtils.copyProperties(project, vo);
        return vo;
    }

    private Page<ProjectVO> toVOPage(Page<Project> page) {
        Page<ProjectVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }
}
