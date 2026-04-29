package com.scratch.community.module.user.validator;

import com.scratch.community.module.user.dto.LoginDTO;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * 用户登录 DTO 验证器
 */
@Component
public class LoginDTOValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return LoginDTO.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        LoginDTO dto = (LoginDTO) target;

        // 用户名验证
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            errors.rejectValue("username", "user.username.empty", "用户名不能为空");
        }

        // 密码验证
        if (dto.getPassword() == null || dto.getPassword().length() < 8) {
            errors.rejectValue("password", "user.password.invalid", "密码长度至少 8 位");
        }
    }
}
