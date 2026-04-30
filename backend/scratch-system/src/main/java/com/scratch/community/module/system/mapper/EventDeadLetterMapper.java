package com.scratch.community.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scratch.community.module.system.entity.EventDeadLetter;
import org.apache.ibatis.annotations.Mapper;

/**
 * 事件死信队列 Mapper
 */
@Mapper
public interface EventDeadLetterMapper extends BaseMapper<EventDeadLetter> {
}
