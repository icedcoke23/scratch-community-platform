package com.scratch.community.module.judge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scratch.community.module.judge.entity.Problem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题目 Mapper
 */
@Mapper
public interface ProblemMapper extends BaseMapper<Problem> {
}
