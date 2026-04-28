package com.scratch.community.module.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scratch.community.module.social.entity.AiReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiReviewMapper extends BaseMapper<AiReview> {

    /**
     * 查询项目最新的 AI 点评
     */
    @Select("SELECT * FROM ai_review WHERE project_id = #{projectId} ORDER BY created_at DESC LIMIT 1")
    AiReview selectLatestByProjectId(@Param("projectId") Long projectId);
}
