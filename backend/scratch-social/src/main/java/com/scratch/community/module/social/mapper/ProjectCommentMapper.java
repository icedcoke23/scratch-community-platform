package com.scratch.community.module.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.module.social.entity.ProjectComment;
import com.scratch.community.module.social.vo.CommentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 评论 Mapper
 */
@Mapper
public interface ProjectCommentMapper extends BaseMapper<ProjectComment> {

    /**
     * 查询项目评论（分页，含用户信息）
     */
    @Select("SELECT c.id, c.user_id, c.project_id, c.content, c.created_at, " +
            "u.username, u.nickname, u.avatar_url " +
            "FROM project_comment c JOIN user u ON c.user_id = u.id " +
            "WHERE c.project_id = #{projectId} AND c.deleted = 0 " +
            "ORDER BY c.created_at DESC")
    Page<CommentVO> selectCommentsByProjectId(Page<CommentVO> page, @Param("projectId") Long projectId);
}
