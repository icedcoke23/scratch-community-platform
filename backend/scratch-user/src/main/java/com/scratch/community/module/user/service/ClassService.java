package com.scratch.community.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.result.ErrorCode;
import com.scratch.community.module.user.dto.CreateClassDTO;
import com.scratch.community.module.user.entity.ClassRoom;
import com.scratch.community.module.user.mapper.ClassMapper;
import com.scratch.community.module.user.vo.ClassVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 班级服务
 */
@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassMapper classMapper;

    /**
     * 创建班级
     */
    public ClassVO createClass(Long teacherId, CreateClassDTO dto) {
        // 检查邀请码是否已存在
        Long count = classMapper.selectCount(
                new LambdaQueryWrapper<ClassRoom>().eq(ClassRoom::getInviteCode, dto.getInviteCode())
        );
        if (count > 0) {
            throw new BizException(9998, "邀请码已被使用");
        }

        ClassRoom classRoom = new ClassRoom();
        classRoom.setName(dto.getName());
        classRoom.setTeacherId(teacherId);
        classRoom.setInviteCode(dto.getInviteCode());
        classRoom.setGrade(dto.getGrade());
        classRoom.setStudentCount(0);
        classMapper.insert(classRoom);

        return toVO(classRoom);
    }

    /**
     * 学生加入班级
     */
    @Transactional
    public void joinClass(Long userId, String inviteCode) {
        ClassRoom classRoom = classMapper.selectOne(
                new LambdaQueryWrapper<ClassRoom>().eq(ClassRoom::getInviteCode, inviteCode)
        );
        if (classRoom == null) {
            throw new BizException(ErrorCode.INVITE_CODE_INVALID);
        }

        // TODO: 插入 class_student 关系表
        // TODO: 更新 student_count
    }

    /**
     * 班级详情
     */
    public ClassVO getClassDetail(Long classId) {
        ClassRoom classRoom = classMapper.selectById(classId);
        if (classRoom == null) {
            throw new BizException(ErrorCode.CLASS_NOT_FOUND);
        }
        return toVO(classRoom);
    }

    /**
     * 我的班级列表
     */
    public List<ClassVO> getMyClasses(Long userId) {
        // TODO: 根据角色查询 (教师查自己创建的，学生查加入的)
        return List.of();
    }

    /**
     * 移除成员
     */
    public void removeMember(Long classId, Long operatorId, Long targetUserId) {
        // TODO: 校验操作者是教师
        // TODO: 从 class_student 删除
    }

    /**
     * 班级成员列表
     */
    public List<Object> getClassMembers(Long classId) {
        // TODO: JOIN user + class_student
        return List.of();
    }

    private ClassVO toVO(ClassRoom classRoom) {
        ClassVO vo = new ClassVO();
        BeanUtils.copyProperties(classRoom, vo);
        return vo;
    }
}
