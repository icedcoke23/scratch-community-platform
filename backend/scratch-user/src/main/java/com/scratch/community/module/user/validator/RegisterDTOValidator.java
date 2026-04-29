package com.scratch.community.module.user.validator;

import com.scratch.community.module.user.dto.RegisterDTO;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * 用户注册 DTO 验证器
 */
@Component
public class RegisterDTOValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return RegisterDTO.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        RegisterDTO dto = (RegisterDTO) target;

        // 用户名验证
        if (dto.getUsername() == null || dto.getUsername().length() < 4 || dto.getUsername().length() > 20) {
            errors.rejectValue("username", "user.username.invalid", "用户名长度需 4-20 个字符");
        }

        // 密码验证
        if (dto.getPassword() == null || dto.getPassword().length() < 8) {
            errors.rejectValue("password", "user.password.invalid", "密码长度至少 8 位");
        }

        // 昵称验证
        if (dto.getNickname() == null || dto.getNickname().trim().isEmpty()) {
            errors.rejectValue("nickname", "user.nickname.empty", "昵称不能为空");
        }

        // 角色验证
        if (dto.getRole() == null) {
            errors.rejectValue("role", "user.role.empty", "角色不能为空");
        }
    }
}
