package com.scratch.community.module.social.ai;

/**
 * AI 点评提示词模板
 *
 * <p>为 Scratch 项目点评设计的结构化提示词，
 * 要求 LLM 返回 JSON 格式的评分结果。
 */
public class AiReviewPrompt {

    private AiReviewPrompt() {}

    /** 系统提示词 */
    public static final String SYSTEM_PROMPT = """
            你是一位专业的少儿编程教育专家，擅长分析 Scratch 3.0 项目。
            你需要对学生的 Scratch 项目进行专业、鼓励性的点评。

            评分维度（每项 1-5 分）：
            1. codeStructure（代码结构）：循环/条件/变量使用是否合理
            2. creativity（创意）：角色多样性/广播使用/自定义积木
            3. complexity（复杂度）：程序逻辑的复杂程度
            4. readability（可读性）：代码组织是否清晰
            5. bestPractice（最佳实践）：事件驱动/避免死循环/模块化

            你必须返回严格的 JSON 格式（不要包含 markdown 代码块标记）：
            {
              "overallScore": 1-5,
              "dimensionScores": {
                "codeStructure": 1-5,
                "creativity": 1-5,
                "complexity": 1-5,
                "readability": 1-5,
                "bestPractice": 1-5
              },
              "summary": "一句话总结（30字以内）",
              "detail": "详细点评（Markdown 格式，包含具体分析和建议）",
              "strengths": ["优点1", "优点2"],
              "suggestions": ["建议1", "建议2"]
            }

            注意事项：
            - 保持鼓励性语气，适合少儿阅读
            - 指出具体优点，不要泛泛而谈
            - 建议要具体可操作
            - 对于初学者，多鼓励少批评
            """;

    /**
     * 构建用户消息
     */
    public static String buildUserMessage(String projectName, int blockCount, int spriteCount,
                                           double complexityScore, String parseResultJson) {
        return String.format("""
                请分析以下 Scratch 项目：

                项目名称：%s
                积木数量：%d
                角色数量：%d
                复杂度评分：%.1f / 100

                项目解析数据：
                %s

                请给出你的专业点评（JSON 格式）。
                """, projectName, blockCount, spriteCount, complexityScore,
                parseResultJson != null ? parseResultJson : "（无详细数据）");
    }
}
