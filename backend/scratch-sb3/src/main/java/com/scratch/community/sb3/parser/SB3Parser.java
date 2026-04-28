package com.scratch.community.sb3.parser;

import com.scratch.community.sb3.exception.SB3ParseException;
import com.scratch.community.sb3.model.SB3ParseResult;
import com.scratch.community.sb3.model.SpriteInfo;

import java.util.List;
import java.util.Map;

/**
 * sb3 解析器 — 统一入口
 *
 * 使用方式:
 *   SB3Parser parser = new SB3Parser();
 *   SB3ParseResult result = parser.parse(sb3Bytes);
 */
public class SB3Parser {

    private final SB3Unzipper unzipper = new SB3Unzipper();
    private final ProjectJsonParser jsonParser = new ProjectJsonParser();
    private final SpriteExtractor spriteExtractor = new SpriteExtractor();
    private final BlockCounter blockCounter = new BlockCounter();
    private final ComplexityCalculator complexityCalculator = new ComplexityCalculator();

    /**
     * 解析 sb3 文件字节流（内存解压，不写磁盘）
     * @param sb3Bytes sb3 文件内容
     * @return 解析结果
     * @throws SB3ParseException 格式错误时抛出
     */
    public SB3ParseResult parse(byte[] sb3Bytes) throws SB3ParseException {
        if (sb3Bytes == null || sb3Bytes.length == 0) {
            throw new SB3ParseException("sb3 文件为空");
        }

        // 1. 解压
        SB3Unzipper.UnzipResult unzipResult = unzipper.unzip(sb3Bytes);

        // 2. 解析 project.json
        ProjectJsonParser.ProjectData projectData = jsonParser.parse(unzipResult.getProjectJson());

        // 3. 提取角色信息
        List<SpriteInfo> sprites = spriteExtractor.extractSprites(projectData.getTargets());

        // 4. 统计积木
        BlockCounter.CountResult countResult = blockCounter.count(projectData.getAllBlocks());

        // 5. 计算复杂度
        double complexity = complexityCalculator.calculate(
                projectData.getAllBlocks(), countResult.getTotalBlocks());

        // 6. 组装结果
        SB3ParseResult result = new SB3ParseResult();
        result.setProjectName(projectData.getProjectName());
        result.setSpriteCount(sprites.size());
        result.setBlockCount(countResult.getTotalBlocks());
        result.setVariableCount(projectData.getVariables().size());
        result.setListCount(projectData.getLists().size());
        result.setBroadcastCount(projectData.getBroadcasts().size());
        result.setComplexityScore(complexity);
        result.setSprites(sprites);
        result.setBlockTypeCounts(countResult.getTypeCounts());

        return result;
    }
}
