package com.scratch.community.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.result.ErrorCode;
import com.scratch.community.module.user.dto.CreateClassDTO;
import com.scratch.community.module.user.entity.ClassRoom;
import com.scratch.community.module.user.entity.ClassStudent;
import com.scratch.community.module.user.mapper.ClassMapper;
import com.scratch.community.module.user.mapper.ClassStudentMapper;
import com.scratch.community.module.user.vo.ClassVO;
import com.scratch.community.module.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 班级服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassMapper classMapper;
    private final ClassStudentMapper classStudentMapper;

    /**
     * 创建班级
     */
    @Transactional
    public ClassVO create(Long teacherId, CreateClassDTO dto) {
        ClassRoom classRoom = new ClassRoom();
        classRoom.setName(dto.getName());
        classRoom.setDescription(dto.getDescription());
        classRoom.setTeacherId(teacherId);
        classRoom.setInviteCode(UUID.randomUUID().toString().substring(0, 8));
        classRoom.setStudentCount(0);
        classMapper.insert(classRoom);
        return toVO(classRoom);
    }

    /**
     * 获取我的班级列表（作为教师创建的 + 作为学生加入的）
     */
    @Transactional(readOnly = true)
    public List<ClassVO> getMyClasses(Long userId) {
        // 作为教师创建的班级
        List<ClassRoom> teachingClasses = classMapper.selectList(
                new LambdaQueryWrapper<ClassRoom>()
                        .eq(ClassRoom::getTeacherId, userId)
                        .orderByDesc(ClassRoom::getCreatedAt));

        // 作为学生加入的班级 ID
        List<Long> joinedClassIds = classStudentMapper.selectClassIdsByStudentId(userId);
        List<ClassRoom> joinedClasses = joinedClassIds.isEmpty() ? List.of() :
                classMapper.selectList(
                        new LambdaQueryWrapper<ClassRoom>()
                                .in(ClassRoom::getId, joinedClassIds)
                                .orderByDesc(ClassRoom::getCreatedAt));

        // 合并去重
        List<ClassVO> result = new java.util.ArrayList<>();
        for (ClassRoom c : teachingClasses) {
            result.add(toVO(c));
        }
        for (ClassRoom c : joinedClasses) {
            if (result.stream().noneMatch(v -> v.getId().equals(c.getId()))) {
                result.add(toVO(c));
            }
        }
        return result;
    }

    /**
     * 获取班级详情
     */
    @Transactional(readOnly = true)
    public ClassVO getClassDetail(Long classId) {
        ClassRoom classRoom = classMapper.selectById(classId);
        if (classRoom == null) {
            throw new BizException(ErrorCode.CLASS_NOT_FOUND);
        }
        return toVO(classRoom);
    }

    /**
     * 加入班级（通过邀请码）
     */
    @Transactional
    public void joinClass(Long studentId, String inviteCode) {
        // 查找班级
        ClassRoom classRoom = classMapper.selectOne(
                new LambdaQueryWrapper<ClassRoom>().eq(ClassRoom::getInviteCode, inviteCode));
        if (classRoom == null) {
            throw new BizException(ErrorCode.INVITE_CODE_INVALID);
        }

        // 检查是否已在班级中
        int count = classStudentMapper.countByClassIdAndStudentId(classRoom.getId(), studentId);
        if (count > 0) {
            return; // 已在班级中，幂等处理
        }

        // 插入班级-学生关系
        ClassStudent cs = new ClassStudent();
        cs.setClassId(classRoom.getId());
        cs.setStudentId(studentId);
        classStudentMapper.insert(cs);

        log.info("学生加入班级: classId={}, studentId={}", classRoom.getId(), studentId);
    }

    /**
     * 移除班级成员
     */
    @Transactional
    public void removeMember(Long classId, Long teacherId, Long studentId) {
        ClassRoom classRoom = classMapper.selectById(classId);
        if (classRoom == null) {
            throw new BizException(ErrorCode.CLASS_NOT_FOUND);
        }
        if (!classRoom.getTeacherId().equals(teacherId)) {
            throw new BizException(ErrorCode.USER_NO_PERMISSION);
        }
        classStudentMapper.delete(
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getClassId, classId)
                        .eq(ClassStudent::getStudentId, studentId));
    }

    /**
     * 获取班级成员列表
     */
    @Transactional(readOnly = true)
    public List<UserVO> getClassMembers(Long classId) {
        return classStudentMapper.selectClassMembers(classId);
    }

    // ==================== 私有方法 ====================

    private ClassVO toVO(ClassRoom classRoom) {
        ClassVO vo = new ClassVO();
        BeanUtils.copyProperties(classRoom, vo);
        return vo;
    }
}
