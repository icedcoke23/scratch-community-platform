package com.scratch.community.sb3.parser;

import com.scratch.community.sb3.exception.SB3ParseException;
import com.scratch.community.sb3.model.SB3ParseResult;
import com.scratch.community.sb3.model.SpriteInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SB3Parser 单元测试
 */
class SB3ParserTest {

    private SB3Parser parser;

    @BeforeEach
    void setUp() {
        parser = new SB3Parser();
    }

    @Test
    @DisplayName("正常 sb3 文件 - 基本解析")
    void testParseNormalSb3() throws IOException {
        byte[] sb3 = createTestSb3();

        SB3ParseResult result = parser.parse(sb3);

        assertNotNull(result);
        assertEquals("Test Project", result.getProjectName());
        assertEquals(1, result.getSpriteCount());
        assertTrue(result.getBlockCount() > 0);
        assertTrue(result.getVariableCount() >= 1);
        assertNotNull(result.getSprites());
        assertEquals(1, result.getSprites().size());
        assertEquals("Sprite1", result.getSprites().get(0).getName());
    }

    @Test
    @DisplayName("空文件 - 抛出异常")
    void testParseEmptyBytes() {
        assertThrows(SB3ParseException.class, () -> parser.parse(new byte[0]));
    }

    @Test
    @DisplayName("null 输入 - 抛出异常")
    void testParseNull() {
        assertThrows(SB3ParseException.class, () -> parser.parse(null));
    }

    @Test
    @DisplayName("非 ZIP 文件 - 抛出异常")
    void testParseInvalidZip() {
        byte[] garbage = "this is not a zip file".getBytes(StandardCharsets.UTF_8);
        assertThrows(SB3ParseException.class, () -> parser.parse(garbage));
    }

    @Test
    @DisplayName("ZIP 内无 project.json - 抛出异常")
    void testParseMissingProjectJson() throws IOException {
        byte[] sb3 = createSb3WithoutProjectJson();
        assertThrows(SB3ParseException.class, () -> parser.parse(sb3));
    }

    @Test
    @DisplayName("project.json 格式错误 - 抛出异常")
    void testParseMalformedJson() throws IOException {
        byte[] sb3 = createSb3WithMalformedJson();
        assertThrows(SB3ParseException.class, () -> parser.parse(sb3));
    }

    @Test
    @DisplayName("角色信息提取正确")
    void testSpriteExtraction() throws IOException {
        byte[] sb3 = createTestSb3();
        SB3ParseResult result = parser.parse(sb3);

        SpriteInfo sprite = result.getSprites().get(0);
        assertEquals("Sprite1", sprite.getName());
        assertFalse(sprite.isStage());
        assertNotNull(sprite.getCostumes());
        assertTrue(sprite.getCostumes().size() > 0);
    }

    @Test
    @DisplayName("复杂度评分在 0-100 范围内")
    void testComplexityScoreRange() throws IOException {
        byte[] sb3 = createTestSb3();
        SB3ParseResult result = parser.parse(sb3);

        assertTrue(result.getComplexityScore() >= 0);
        assertTrue(result.getComplexityScore() <= 100);
    }

    @Test
    @DisplayName("积木类型统计不为空")
    void testBlockTypeCounts() throws IOException {
        byte[] sb3 = createTestSb3();
        SB3ParseResult result = parser.parse(sb3);

        assertNotNull(result.getBlockTypeCounts());
        assertFalse(result.getBlockTypeCounts().isEmpty());
    }

    @Test
    @DisplayName("空项目 - 零积木零角色")
    void testParseEmptyProject() throws IOException {
        byte[] sb3 = createEmptyProjectSb3();
        SB3ParseResult result = parser.parse(sb3);

        assertEquals(0, result.getSpriteCount());
        assertEquals(0, result.getBlockCount());
        assertEquals(0, result.getComplexityScore());
    }

    // ==================== 测试工具方法 ====================

    /**
     * 创建一个包含舞台 + 1 个角色 + 积木 + 变量的测试 sb3
     */
    private byte[] createTestSb3() throws IOException {
        String projectJson = """
                {
                  "targets": [
                    {
                      "isStage": true,
                      "name": "Stage",
                      "variables": {"var1": ["score", 0]},
                      "lists": {},
                      "broadcasts": {},
                      "blocks": {},
                      "costumes": [
                        {
                          "name": "backdrop1",
                          "md5ext": "cd21514d0531fdffb22204e0ec5ed84a.svg",
                          "dataFormat": "svg",
                          "rotationCenterX": 240,
                          "rotationCenterY": 180
                        }
                      ],
                      "currentCostume": 0
                    },
                    {
                      "isStage": false,
                      "name": "Sprite1",
                      "variables": {"var2": ["myVar", 10]},
                      "lists": {"list1": ["myList", ["a", "b", "c"]]},
                      "broadcasts": {"bc1": "message1"},
                      "blocks": {
                        "block1": {
                          "opcode": "event_whenflagclicked",
                          "next": "block2",
                          "parent": null,
                          "topLevel": true,
                          "shadow": false,
                          "inputs": {},
                          "fields": {}
                        },
                        "block2": {
                          "opcode": "motion_movesteps",
                          "next": "block3",
                          "parent": "block1",
                          "topLevel": false,
                          "shadow": false,
                          "inputs": {
                            "STEPS": [1, [4, "10"]]
                          },
                          "fields": {}
                        },
                        "block3": {
                          "opcode": "control_repeat",
                          "next": null,
                          "parent": "block2",
                          "topLevel": false,
                          "shadow": false,
                          "inputs": {
                            "TIMES": [1, [6, "5"]],
                            "SUBSTACK": [2, "block4"]
                          },
                          "fields": {}
                        },
                        "block4": {
                          "opcode": "looks_say",
                          "next": null,
                          "parent": "block3",
                          "topLevel": false,
                          "shadow": false,
                          "inputs": {
                            "MESSAGE": [1, [10, "Hello!"]]
                          },
                          "fields": {}
                        }
                      },
                      "costumes": [
                        {
                          "name": "costume1",
                          "md5ext": "bcf454acf82e4504149f7ffe07081dbc.svg",
                          "dataFormat": "svg",
                          "rotationCenterX": 48,
                          "rotationCenterY": 50
                        }
                      ],
                      "currentCostume": 0,
                      "x": 0,
                      "y": 0,
                      "visible": true,
                      "size": 100,
                      "direction": 90
                    }
                  ],
                  "monitors": [],
                  "extensions": [],
                  "meta": {
                    "semver": "3.0.0",
                    "vm": "0.2.0",
                    "agent": "",
                    "projectName": "Test Project"
                  }
                }
                """;
        return createZipBytes("project.json", projectJson);
    }

    /**
     * 创建一个空项目的 sb3
     */
    private byte[] createEmptyProjectSb3() throws IOException {
        String projectJson = """
                {
                  "targets": [
                    {
                      "isStage": true,
                      "name": "Stage",
                      "variables": {},
                      "lists": {},
                      "broadcasts": {},
                      "blocks": {},
                      "costumes": [],
                      "currentCostume": 0
                    }
                  ],
                  "monitors": [],
                  "extensions": [],
                  "meta": {"semver": "3.0.0"}
                }
                """;
        return createZipBytes("project.json", projectJson);
    }

    /**
     * 创建不含 project.json 的 ZIP
     */
    private byte[] createSb3WithoutProjectJson() throws IOException {
        return createZipBytes("readme.txt", "this zip has no project.json");
    }

    /**
     * 创建 project.json 为非法 JSON 的 sb3
     */
    private byte[] createSb3WithMalformedJson() throws IOException {
        return createZipBytes("project.json", "{ this is not valid json }");
    }

    @Test
    @DisplayName("ZIP 路径遍历攻击 - 包含 ../ 的路径应被拒绝")
    void testZipPathTraversal() throws IOException {
        byte[] sb3 = createSb3WithMaliciousPath();
        assertThrows(SB3ParseException.class, () -> parser.parse(sb3));
    }

    /**
     * 创建包含路径遍历攻击的 sb3
     */
    private byte[] createSb3WithMaliciousPath() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            // 正常的 project.json
            zos.putNextEntry(new ZipEntry("project.json"));
            zos.write("""
                    {
                      "targets": [],
                      "monitors": [],
                      "extensions": [],
                      "meta": {"semver": "3.0.0"}
                    }
                    """.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 恶意路径 - 路径遍历
            zos.putNextEntry(new ZipEntry("../../../etc/passwd"));
            zos.write("malicious".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return bos.toByteArray();
    }

    /**
     * 创建一个 ZIP 文件字节流
     */
    private byte[] createZipBytes(String entryName, String content) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            zos.putNextEntry(new ZipEntry(entryName));
            zos.write(content.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return bos.toByteArray();
    }
}
