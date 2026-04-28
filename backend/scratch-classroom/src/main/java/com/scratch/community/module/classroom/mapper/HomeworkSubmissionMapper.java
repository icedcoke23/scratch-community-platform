package com.scratch.community.module.classroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.module.classroom.entity.HomeworkSubmission;
import com.scratch.community.module.classroom.vo.HomeworkSubmissionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HomeworkSubmissionMapper extends BaseMapper<HomeworkSubmission> {

    /**
     * 查询作业提交列表（含学生信息）
     */
    @Select("SELECT hs.id, hs.homework_id, hs.student_id, hs.project_id, hs.score, " +
            "hs.comment, hs.status, hs.created_at, hs.graded_at, " +
            "u.username, u.nickname, u.avatar_url " +
            "FROM homework_submission hs JOIN user u ON hs.student_id = u.id " +
            "WHERE hs.homework_id = #{homeworkId} AND hs.deleted = 0 " +
            "ORDER BY hs.created_at DESC")
    Page<HomeworkSubmissionVO> selectByHomeworkId(Page<HomeworkSubmissionVO> page,
                                                   @Param("homeworkId") Long homeworkId);

    /**
     * 检查学生是否已提交某作业
     */
    @Select("SELECT COUNT(*) FROM homework_submission WHERE homework_id = #{homeworkId} AND student_id = #{studentId} AND deleted = 0")
    int countByHomeworkAndStudent(@Param("homeworkId") Long homeworkId, @Param("studentId") Long studentId);
}
