package com.scratch.community.sb3.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.scratch.community.sb3.model.CostumeInfo;
import com.scratch.community.sb3.model.SpriteInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色信息提取器
 * 从 project.json 的 targets 中提取每个角色的详细信息
 */
public class SpriteExtractor {

    /**
     * 提取所有角色（不含舞台）
     * @param targets project.json 的 targets 数组
     * @return 角色信息列表
     */
    public List<SpriteInfo> extractSprites(JsonNode targets) {
        List<SpriteInfo> sprites = new ArrayList<>();

        for (int i = 0; i < targets.size(); i++) {
            JsonNode target = targets.get(i);
            if (target == null || target.isNull()) continue;

            JsonNode isStageNode = target.get("isStage");
            boolean isStage = isStageNode != null && isStageNode.asBoolean(false);
            if (isStage) continue; // 跳过舞台

            SpriteInfo sprite = new SpriteInfo();
            sprite.setName(target.has("name") ? target.get("name").asText() : null);
            sprite.setStage(false);
            sprite.setX(target.has("x") ? target.get("x").asDouble(0.0) : 0.0);
            sprite.setY(target.has("y") ? target.get("y").asDouble(0.0) : 0.0);
            sprite.setVisible(target.has("visible") ? target.get("visible").asBoolean(true) : true);
            sprite.setSize(target.has("size") ? target.get("size").asDouble(100.0) : 100.0);
            sprite.setDirection(target.has("direction") ? target.get("direction").asDouble(90.0) : 90.0);
            sprite.setCurrentCostume(target.has("currentCostume") ? target.get("currentCostume").asInt(0) : 0);

            // 提取 costumes
            JsonNode costumesNode = target.get("costumes");
            sprite.setCostumes(extractCostumes(costumesNode));

            // 统计该角色的积木数
            JsonNode blocks = target.get("blocks");
            sprite.setBlockCount(blocks != null && blocks.isObject() ? blocks.size() : 0);

            // 统计该角色的变量数和列表数
            JsonNode vars = target.get("variables");
            sprite.setVariableCount(vars != null && vars.isObject() ? vars.size() : 0);

            JsonNode lists = target.get("lists");
            sprite.setListCount(lists != null && lists.isObject() ? lists.size() : 0);

            sprites.add(sprite);
        }

        return sprites;
    }

    /**
     * 提取舞台信息
     * @param targets project.json 的 targets 数组
     * @return 舞台 SpriteInfo，如果没找到返回 null
     */
    public SpriteInfo extractStage(JsonNode targets) {
        for (int i = 0; i < targets.size(); i++) {
            JsonNode target = targets.get(i);
            if (target == null || target.isNull()) continue;

            JsonNode isStageNode = target.get("isStage");
            boolean isStage = isStageNode != null && isStageNode.asBoolean(false);
            if (!isStage) continue;

            SpriteInfo stage = new SpriteInfo();
            stage.setName(target.has("name") ? target.get("name").asText() : null);
            stage.setStage(true);
            JsonNode costumesNode = target.get("costumes");
            stage.setCostumes(extractCostumes(costumesNode));
            stage.setCurrentCostume(target.has("currentCostume") ? target.get("currentCostume").asInt(0) : 0);

            JsonNode blocks = target.get("blocks");
            stage.setBlockCount(blocks != null && blocks.isObject() ? blocks.size() : 0);

            return stage;
        }
        return null;
    }

    /**
     * 提取 costume 列表
     */
    private List<CostumeInfo> extractCostumes(JsonNode costumesArray) {
        List<CostumeInfo> costumes = new ArrayList<>();
        if (costumesArray == null || !costumesArray.isArray()) return costumes;

        for (int i = 0; i < costumesArray.size(); i++) {
            JsonNode obj = costumesArray.get(i);
            if (obj == null || obj.isNull()) continue;

            CostumeInfo costume = new CostumeInfo();
            costume.setName(obj.has("name") ? obj.get("name").asText() : null);
            costume.setMd5ext(obj.has("md5ext") ? obj.get("md5ext").asText() : null);
            costume.setDataFormat(obj.has("dataFormat") ? obj.get("dataFormat").asText() : null);
            costume.setRotationCenterX(obj.has("rotationCenterX") ? obj.get("rotationCenterX").asDouble(0.0) : 0.0);
            costume.setRotationCenterY(obj.has("rotationCenterY") ? obj.get("rotationCenterY").asDouble(0.0) : 0.0);
            costumes.add(costume);
        }

        return costumes;
    }
}
