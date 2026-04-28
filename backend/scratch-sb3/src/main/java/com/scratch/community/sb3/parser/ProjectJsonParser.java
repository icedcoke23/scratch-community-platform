package com.scratch.community.sb3.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scratch.community.sb3.exception.SB3ParseException;

import java.util.*;

/**
 * project.json 解析器
 * 解析 Scratch 3.0 项目 JSON，提取 targets、variables、broadcasts、lists
 */
public class ProjectJsonParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析结果
     */
    public static class ProjectData {
        private final String projectName;
        private final JsonNode targets;
        private final Map<String, String> variables;     // id → name
        private final Map<String, String> lists;         // id → name
        private final Map<String, String> broadcasts;    // id → name
        private final Map<String, JsonNode> allBlocks;   // blockId → block 对象

        public ProjectData(String projectName, JsonNode targets,
                           Map<String, String> variables, Map<String, String> lists,
                           Map<String, String> broadcasts, Map<String, JsonNode> allBlocks) {
            this.projectName = projectName;
            this.targets = targets;
            this.variables = variables;
            this.lists = lists;
            this.broadcasts = broadcasts;
            this.allBlocks = allBlocks;
        }

        public String getProjectName() { return projectName; }
        public JsonNode getTargets() { return targets; }
        public Map<String, String> getVariables() { return variables; }
        public Map<String, String> getLists() { return lists; }
        public Map<String, String> getBroadcasts() { return broadcasts; }
        public Map<String, JsonNode> getAllBlocks() { return allBlocks; }
    }

    /**
     * 解析 project.json
     * @param json project.json 字符串
     * @return 解析结果
     * @throws SB3ParseException 格式错误时抛出
     */
    public ProjectData parse(String json) throws SB3ParseException {
        if (json == null || json.isBlank()) {
            throw new SB3ParseException("project.json 内容为空");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new SB3ParseException("project.json 格式错误: " + e.getMessage(), e);
        }

        if (root == null || root.isNull()) {
            throw new SB3ParseException("project.json 解析结果为空");
        }

        // 提取 targets
        JsonNode targets = root.get("targets");
        if (targets == null || !targets.isArray()) {
            throw new SB3ParseException("project.json 缺少 targets 字段");
        }

        // 提取项目名称（从舞台的 meta 或变量中推断）
        String projectName = extractProjectName(root, targets);

        // 收集所有变量、列表、广播
        Map<String, String> variables = new LinkedHashMap<>();
        Map<String, String> lists = new LinkedHashMap<>();
        Map<String, String> broadcasts = new LinkedHashMap<>();
        Map<String, JsonNode> allBlocks = new LinkedHashMap<>();

        for (int i = 0; i < targets.size(); i++) {
            JsonNode target = targets.get(i);
            if (target == null || target.isNull()) continue;

            // 收集变量 (格式: {id: [name, value]})
            JsonNode vars = target.get("variables");
            if (vars != null && vars.isObject()) {
                var iter = vars.fields();
                while (iter.hasNext()) {
                    var entry = iter.next();
                    JsonNode varArray = entry.getValue();
                    if (varArray != null && varArray.isArray() && varArray.size() >= 2) {
                        variables.put(entry.getKey(), varArray.get(0).asText());
                    }
                }
            }

            // 收集列表 (格式: {id: [name, [...values...]]})
            JsonNode listVars = target.get("lists");
            if (listVars != null && listVars.isObject()) {
                var iter = listVars.fields();
                while (iter.hasNext()) {
                    var entry = iter.next();
                    JsonNode listArray = entry.getValue();
                    if (listArray != null && listArray.isArray() && listArray.size() >= 2) {
                        lists.put(entry.getKey(), listArray.get(0).asText());
                    }
                }
            }

            // 收集广播 (格式: {id: name})
            JsonNode bcVars = target.get("broadcasts");
            if (bcVars != null && bcVars.isObject()) {
                var iter = bcVars.fields();
                while (iter.hasNext()) {
                    var entry = iter.next();
                    broadcasts.put(entry.getKey(), entry.getValue().asText());
                }
            }

            // 收集积木 (格式: {blockId: {opcode: ..., ...}})
            JsonNode blocks = target.get("blocks");
            if (blocks != null && blocks.isObject()) {
                var iter = blocks.fields();
                while (iter.hasNext()) {
                    var entry = iter.next();
                    JsonNode blockObj = entry.getValue();
                    if (blockObj != null && blockObj.isObject()) {
                        allBlocks.put(entry.getKey(), blockObj);
                    }
                }
            }
        }

        return new ProjectData(projectName, targets, variables, lists, broadcasts, allBlocks);
    }

    /**
     * 提取项目名称
     */
    private String extractProjectName(JsonNode root, JsonNode targets) {
        // 1. 尝试从 meta.projectName 读取（Scratch 3.0 标准字段）
        JsonNode meta = root.get("meta");
        if (meta != null && meta.isObject()) {
            JsonNode nameNode = meta.get("projectName");
            if (nameNode != null && !nameNode.isNull()) {
                String name = nameNode.asText();
                if (name != null && !name.isBlank()) {
                    return name.trim();
                }
            }
        }

        // 2. 从舞台的变量中查找常见的项目名变量（如 "projectName"）
        if (targets != null && targets.size() > 0) {
            JsonNode stage = targets.get(0);
            if (stage != null) {
                JsonNode vars = stage.get("variables");
                if (vars != null && vars.isObject()) {
                    var iter = vars.fields();
                    while (iter.hasNext()) {
                        JsonNode value = iter.next().getValue();
                        if (value != null && value.isArray() && value.size() >= 2) {
                            String varName = value.get(0).asText();
                            if ("projectName".equals(varName) || "project_name".equals(varName)) {
                                String varValue = value.get(1).asText();
                                if (varValue != null && !varValue.isBlank()) {
                                    return varValue.trim();
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. 默认名称
        return "Untitled Project";
    }
}
