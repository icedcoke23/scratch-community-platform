package com.scratch.community.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 轮播图实体
 * 对应 database carousel 表
 */
@Getter
@Setter
@TableName(value = "carousel", autoResultMap = true)
public class Carousel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 轮播图标题 */
    private String title;

    /** 图片 URL */
    private String imageUrl;

    /** 点击跳转链接 */
    private String targetUrl;

    /** 描述信息 */
    private String description;

    /** 排序顺序，数值越小越靠前 */
    private Integer sortOrder;

    /** 是否启用：1-启用，0-禁用 */
    private Integer isEnabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
