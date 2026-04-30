package com.scratch.community.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scratch.community.module.user.entity.ClassStudent;
import com.scratch.community.module.user.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 班级学生关系 Mapper
 */
@Mapper
public interface ClassStudentMapper extends BaseMapper<ClassStudent> {

    /**
     * 查询班级成员列表
     */
    @Select("SELECT u.id, u.username, u.nickname, u.avatar_url, u.bio, u.role " +
            "FROM class_student cs JOIN user u ON cs.student_id = u.id " +
            "WHERE cs.class_id = #{classId} AND u.deleted = 0 " +
            "ORDER BY cs.joined_at DESC")
    List<UserVO> selectClassMembers(@Param("classId") Long classId);

    /**
     * 查询用户加入的班级 ID 列表
     */
    @Select("SELECT class_id FROM class_student WHERE student_id = #{studentId}")
    List<Long> selectClassIdsByStudentId(@Param("studentId") Long studentId);

    /**
     * 检查学生是否已在班级中
     */
    @Select("SELECT COUNT(*) FROM class_student WHERE class_id = #{classId} AND student_id = #{studentId}")
    int countByClassIdAndStudentId(@Param("classId") Long classId, @Param("studentId") Long studentId);
}
