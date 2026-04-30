package com.scratch.community.module.judge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.module.judge.entity.CompetitionRanking;
import com.scratch.community.module.judge.vo.CompetitionRankingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CompetitionRankingMapper extends BaseMapper<CompetitionRanking> {

    @Select("SELECT cr.*, u.username, u.nickname, u.avatar_url " +
            "FROM competition_ranking cr " +
            "JOIN user u ON cr.user_id = u.id AND u.deleted = 0 " +
            "WHERE cr.competition_id = #{competitionId} " +
            "ORDER BY cr.total_score DESC, cr.penalty ASC, cr.last_submit_time ASC")
    Page<CompetitionRankingVO> selectRankingByCompetitionId(
            Page<CompetitionRankingVO> page,
            @Param("competitionId") Long competitionId);
}
