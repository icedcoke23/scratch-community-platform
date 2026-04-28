package com.scratch.community.sb3.model;

import lombok.Data;
import java.util.List;

/**
 * 角色（Sprite）信息
 */
@Data
public class SpriteInfo {

    /** 角色名称 */
    private String name;

    /** 是否为舞台 */
    private boolean stage;

    /** X 坐标 */
    private double x;

    /** Y 坐标 */
    private double y;

    /** 是否可见 */
    private boolean visible;

    /** 大小（百分比） */
    private double size;

    /** 方向（角度） */
    private double direction;

    /** 当前 costume 索引 */
    private int currentCostume;

    /** costume 列表 */
    private List<CostumeInfo> costumes;

    /** 该角色的积木数 */
    private int blockCount;

    /** 该角色的变量数 */
    private int variableCount;

    /** 该角色的列表数 */
    private int listCount;
}
