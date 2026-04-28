package com.scratch.community.common.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * 敏感词过滤器（DFA 自动机版）
 *
 * 使用 Deterministic Finite Automaton 实现高效敏感词匹配，
 * 时间复杂度 O(n)，n 为文本长度，与词库大小无关。
 *
 * <p>DFA 节点使用类型安全的 {@link DfaNode} 内部类替代 {@code Map<Object, Object>}，
 * 消除了运行时的类型强转风险，编译期即可捕获类型错误。
 *
 * <p>支持:
 * <ul>
 *   <li>精确匹配</li>
 *   <li>忽略大小写</li>
 *   <li>词库热更新（reload 方法）</li>
 *   <li>从数据库加载词库（system_config 表，key=sensitive_words）</li>
 * </ul>
 */
@Slf4j
@Component
public class SensitiveWordFilter {

    /** DFA 根节点（类型安全版本） */
    private volatile DfaNode root = new DfaNode();

    /** 词库大小（用于统计） */
    private volatile int wordCount = 0;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    /** 内置基础敏感词（兜底） */
    private static final List<String> BUILTIN_WORDS = List.of(
            "赌博", "色情", "暴力", "毒品", "枪支",
            "诈骗", "传销", "邪教", "恐怖",
            "分裂", "颠覆", "暴动",
            "脏话", "辱骂", "人身攻击",
            "代开发票", "办证", "刷单",
            "外挂", "作弊", "钓鱼"
    );

    /**
     * DFA 节点（类型安全）
     *
     * <p>替代原来的 {@code Map<Object, Object>}，每个节点包含：
     * <ul>
     *   <li>{@code children} — 子节点映射（Character → DfaNode）</li>
     *   <li>{@code isEnd} — 标记该节点是否为某个敏感词的结尾</li>
     * </ul>
     *
     * <p>使用 {@code DfaNode} 后：
     * <ul>
     *   <li>编译期类型安全 — 不再需要 {@code (Map<Object, Object>)} 强转</li>
     *   <li>语义清晰 — {@code isEnd} 字段比 {@code map.get("isEnd")} 更直观</li>
     *   <li>避免 key 冲突 — 不会与 Character 类型的子节点 key 混淆</li>
     * </ul>
     */
    static class DfaNode {
        /** 子节点映射：Character → DfaNode */
        final Map<Character, DfaNode> children = new HashMap<>();
        /** 是否为敏感词结尾 */
        boolean isEnd = false;
    }

    @PostConstruct
    public void init() {
        List<String> words = new ArrayList<>(BUILTIN_WORDS);

        // 尝试从数据库加载扩展词库
        if (jdbcTemplate != null) {
            try {
                String dbWords = jdbcTemplate.queryForObject(
                        "SELECT config_value FROM system_config WHERE config_key = 'sensitive_words'",
                        String.class);
                if (dbWords != null && !dbWords.isBlank()) {
                    for (String word : dbWords.split("[,，\\n]")) {
                        String trimmed = word.trim();
                        if (!trimmed.isEmpty()) {
                            words.add(trimmed);
                        }
                    }
                    log.info("从数据库加载扩展敏感词: {} 个", words.size() - BUILTIN_WORDS.size());
                }
            } catch (Exception e) {
                log.debug("未找到数据库敏感词配置，使用内置词库: {}", e.getMessage());
            }
        }

        reload(words);
        log.info("敏感词过滤器初始化完成（DFA），共 {} 个敏感词", wordCount);
    }

    // ==================== 公开 API ====================

    /**
     * 检查内容是否包含敏感词
     * @param content 待检查内容
     * @throws com.scratch.community.common.exception.BizException 如果包含敏感词
     */
    public void check(String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        List<String> hits = match(content);
        if (!hits.isEmpty()) {
            throw new com.scratch.community.common.exception.BizException(
                    com.scratch.community.common.result.ErrorCode.CONTENT_AUDIT_FAIL
            );
        }
    }

    /**
     * 检查内容是否包含敏感词（返回 boolean）
     */
    public boolean containsSensitiveWord(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        return !match(content).isEmpty();
    }

    /**
     * 过滤内容（将敏感词替换为 *）
     */
    public String filter(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }

        String text = content.toLowerCase();
        StringBuilder result = new StringBuilder(content);
        int i = 0;

        while (i < text.length()) {
            DfaNode current = root;
            int matchLen = 0;

            // 从当前位置尝试匹配最长敏感词
            for (int j = i; j < text.length(); j++) {
                DfaNode next = current.children.get(text.charAt(j));
                if (next == null) {
                    break;
                }
                current = next;
                if (current.isEnd) {
                    matchLen = j - i + 1;
                }
            }

            if (matchLen > 0) {
                // 替换匹配到的敏感词
                for (int k = i; k < i + matchLen; k++) {
                    result.setCharAt(k, '*');
                }
                i += matchLen;
            } else {
                i++;
            }
        }

        return result.toString();
    }

    /**
     * 匹配敏感词（返回所有命中的敏感词列表）
     */
    public List<String> match(String content) {
        if (content == null || content.isBlank()) {
            return Collections.emptyList();
        }

        String text = content.toLowerCase();
        Set<String> hits = new LinkedHashSet<>();
        int i = 0;

        while (i < text.length()) {
            DfaNode current = root;
            StringBuilder matched = new StringBuilder();

            for (int j = i; j < text.length(); j++) {
                DfaNode next = current.children.get(text.charAt(j));
                if (next == null) {
                    break;
                }
                current = next;
                matched.append(text.charAt(j));

                if (current.isEnd) {
                    hits.add(matched.toString());
                }
            }

            i++;
        }

        return new ArrayList<>(hits);
    }

    /**
     * 刷新词库
     */
    public synchronized void reload(List<String> words) {
        DfaNode newRoot = new DfaNode();
        int count = 0;

        for (String word : words) {
            if (word == null || word.isBlank()) continue;
            addWord(newRoot, word.toLowerCase().trim());
            count++;
        }

        this.root = newRoot;
        this.wordCount = count;
        log.info("敏感词库已刷新（DFA），共 {} 个敏感词", count);
    }

    /**
     * 获取词库大小
     */
    public int getWordCount() {
        return wordCount;
    }

    // ==================== 内部方法 ====================

    /**
     * 将一个词添加到 DFA 树中
     *
     * @param root DFA 根节点
     * @param word 待添加的敏感词（已转小写）
     */
    private void addWord(DfaNode root, String word) {
        DfaNode current = root;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            DfaNode node = current.children.get(c);
            if (node == null) {
                node = new DfaNode();
                current.children.put(c, node);
            }
            current = node;
        }
        current.isEnd = true;
    }
}
