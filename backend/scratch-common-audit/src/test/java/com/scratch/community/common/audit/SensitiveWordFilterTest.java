package com.scratch.community.common.audit;

import com.scratch.community.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SensitiveWordFilter DFA 敏感词过滤器测试
 */
class SensitiveWordFilterTest {

    private SensitiveWordFilter filter;

    @BeforeEach
    void setUp() {
        filter = new SensitiveWordFilter();
        // 使用内置词库初始化
        filter.reload(List.of("赌博", "色情", "暴力", "毒品", "枪支", "诈骗", "传销"));
    }

    @Nested
    @DisplayName("match() 匹配测试")
    class MatchTests {

        @Test
        @DisplayName("精确匹配单个敏感词")
        void match_singleWord() {
            List<String> hits = filter.match("这个内容包含赌博信息");
            assertTrue(hits.contains("赌博"));
            assertEquals(1, hits.size());
        }

        @Test
        @DisplayName("匹配多个敏感词")
        void match_multipleWords() {
            List<String> hits = filter.match("赌博和色情都是违法的");
            assertEquals(2, hits.size());
            assertTrue(hits.contains("赌博"));
            assertTrue(hits.contains("色情"));
        }

        @Test
        @DisplayName("无敏感词返回空列表")
        void match_noHits() {
            List<String> hits = filter.match("这是一段正常的内容");
            assertTrue(hits.isEmpty());
        }

        @Test
        @DisplayName("空字符串返回空列表")
        void match_empty() {
            assertTrue(filter.match("").isEmpty());
            assertTrue(filter.match(null).isEmpty());
            assertTrue(filter.match("   ").isEmpty());
        }

        @Test
        @DisplayName("忽略大小写")
        void match_caseInsensitive() {
            filter.reload(List.of("abc", "XYZ"));
            List<String> hits = filter.match("ABC and XYZ");
            assertEquals(2, hits.size());
        }

        @Test
        @DisplayName("连续敏感词")
        void match_consecutive() {
            List<String> hits = filter.match("赌博色情暴力");
            assertEquals(3, hits.size());
        }
    }

    @Nested
    @DisplayName("containsSensitiveWord() 测试")
    class ContainsTests {

        @Test
        @DisplayName("包含敏感词返回 true")
        void contains_true() {
            assertTrue(filter.containsSensitiveWord("这里有赌博内容"));
        }

        @Test
        @DisplayName("不包含敏感词返回 false")
        void contains_false() {
            assertFalse(filter.containsSensitiveWord("正常内容"));
        }
    }

    @Nested
    @DisplayName("filter() 替换测试")
    class FilterTests {

        @Test
        @DisplayName("敏感词替换为 *")
        void filter_replace() {
            String result = filter.filter("这里有赌博内容");
            assertEquals("这里有**内容", result);
        }

        @Test
        @DisplayName("多个敏感词全部替换")
        void filter_multiple() {
            String result = filter.filter("赌博和色情");
            assertEquals("**和**", result);
        }

        @Test
        @DisplayName("无敏感词原样返回")
        void filter_noChange() {
            String input = "正常内容";
            assertEquals(input, filter.filter(input));
        }
    }

    @Nested
    @DisplayName("check() 抛异常测试")
    class CheckTests {

        @Test
        @DisplayName("包含敏感词抛 BizException")
        void check_throws() {
            assertThrows(BizException.class, () -> filter.check("这里有赌博内容"));
        }

        @Test
        @DisplayName("无敏感词不抛异常")
        void check_noThrow() {
            assertDoesNotThrow(() -> filter.check("正常内容"));
        }

        @Test
        @DisplayName("空内容不抛异常")
        void check_empty() {
            assertDoesNotThrow(() -> filter.check(null));
            assertDoesNotThrow(() -> filter.check(""));
        }
    }

    @Nested
    @DisplayName("reload() 热更新测试")
    class ReloadTests {

        @Test
        @DisplayName("reload 后使用新词库")
        void reload_updatesWords() {
            filter.reload(List.of("新词库"));
            List<String> hits = filter.match("包含新词库");
            assertEquals(1, hits.size());
            // 旧词库不再生效
            assertTrue(filter.match("赌博").isEmpty());
        }

        @Test
        @DisplayName("词库大小正确")
        void reload_wordCount() {
            filter.reload(List.of("a", "b", "c"));
            assertEquals(3, filter.getWordCount());
        }
    }
}
