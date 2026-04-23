package com.scratch.community.common.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 敏感词过滤器 (简化版 - 基于 Set 匹配)
 * MVP 阶段用简单实现，后续可替换为 DFA/AC 自动机
 */
@Slf4j
@Component
public class SensitiveWordFilter {

    private volatile Set<String> sensitiveWords = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void init() {
        // TODO: 从数据库加载敏感词库
        // MVP 阶段内置基础敏感词
        sensitiveWords.addAll(List.of(
                "赌博", "色情", "暴力", "毒品", "枪支",
                "诈骗", "传销", "邪教", "恐怖"
        ));
        log.info("敏感词过滤器初始化完成，共 {} 个敏感词", sensitiveWords.size());
    }

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
                    60001, "内容包含违规信息: " + hits.get(0)
            );
        }
    }

    /**
     * 过滤内容（将敏感词替换为 *）
     */
    public String filter(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }
        String result = content;
        for (String word : sensitiveWords) {
            if (result.contains(word)) {
                String replacement = "*".repeat(word.length());
                result = result.replace(word, replacement);
            }
        }
        return result;
    }

    /**
     * 匹配敏感词
     */
    public List<String> match(String content) {
        List<String> hits = new ArrayList<>();
        String lower = content.toLowerCase();
        for (String word : sensitiveWords) {
            if (lower.contains(word.toLowerCase())) {
                hits.add(word);
            }
        }
        return hits;
    }

    /**
     * 刷新词库
     */
    public void reload(List<String> words) {
        Set<String> newSet = ConcurrentHashMap.newKeySet();
        newSet.addAll(words);
        this.sensitiveWords = newSet;
        log.info("敏感词库已刷新，共 {} 个敏感词", sensitiveWords.size());
    }
}
