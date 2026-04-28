package com.scratch.community.module.judge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scratch.community.module.judge.entity.CompetitionRegistration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CompetitionRegistrationMapper extends BaseMapper<CompetitionRegistration> {

    @Select("SELECT COUNT(*) FROM competition_registration WHERE competition_id = #{competitionId} AND user_id = #{userId}")
    int countByCompetitionAndUser(@Param("competitionId") Long competitionId, @Param("userId") Long userId);
}
