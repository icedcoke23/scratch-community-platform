package com.scratch.community.module.judge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scratch.community.module.judge.entity.Submission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 提交记录 Mapper
 */
@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {
}
