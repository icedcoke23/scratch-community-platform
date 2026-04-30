package com.scratch.community.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scratch.community.module.user.entity.PointLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PointLogMapper extends BaseMapper<PointLog> {

    /**
     * 检查今天是否已签到
     */
    @Select("SELECT COUNT(*) FROM point_log WHERE user_id = #{userId} AND type = 'DAILY_CHECKIN' AND DATE(created_at) = CURDATE()")
    int countTodayCheckin(@Param("userId") Long userId);

    /**
     * 检查今天是否已为某对象获得积分
     */
    @Select("SELECT COUNT(*) FROM point_log WHERE user_id = #{userId} AND type = #{type} AND ref_type = #{refType} AND ref_id = #{refId}")
    int countByTypeAndRef(@Param("userId") Long userId, @Param("type") String type,
                          @Param("refType") String refType, @Param("refId") Long refId);
}
