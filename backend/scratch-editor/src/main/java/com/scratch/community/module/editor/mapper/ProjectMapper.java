package com.scratch.community.module.editor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scratch.community.module.editor.entity.Project;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目 Mapper
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
    // 简单 CRUD 继承 BaseMapper
    // 复杂查询（如关联用户信息）在此定义方法 + XML 实现
}
