package com.scratch.community.module.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scratch.community.module.social.entity.ProjectLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 点赞 Mapper
 */
@Mapper
public interface ProjectLikeMapper extends BaseMapper<ProjectLike> {

    /**
     * 检查用户是否已点赞某项目
     */
    @Select("SELECT COUNT(*) FROM project_like WHERE user_id = #{userId} AND project_id = #{projectId}")
    int countByUserAndProject(@Param("userId") Long userId, @Param("projectId") Long projectId);
}
