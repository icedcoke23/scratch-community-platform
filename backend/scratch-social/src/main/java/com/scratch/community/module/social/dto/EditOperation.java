package com.scratch.community.module.social.dto;

import lombok.Data;

/**
 * 编辑操作 DTO
 */
@Data
public class EditOperation {
    /** 操作类型: add_block / remove_block / move_block / modify_block / add_sprite / remove_sprite / modify_variable */
    private String type;
    /** 目标积木/角色 ID */
    private String targetId;
    /** 操作数据 */
    private Object data;
    /** 版本号（乐观锁） */
    private Long version;
    /** 时间戳 */
    private Long timestamp;
}
