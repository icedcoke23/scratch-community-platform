package com.scratch.community.sb3.model;

import lombok.Data;

/**
 * Costume（造型）信息
 */
@Data
public class CostumeInfo {

    /** 造型名称 */
    private String name;

    /** 资源文件 MD5 */
    private String md5ext;

    /** 数据格式（png, svg, wav, mp3） */
    private String dataFormat;

    /** 旋转中心 X */
    private double rotationCenterX;

    /** 旋转中心 Y */
    private double rotationCenterY;
}
