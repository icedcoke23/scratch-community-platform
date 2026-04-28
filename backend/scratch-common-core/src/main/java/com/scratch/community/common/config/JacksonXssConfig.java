package com.scratch.community.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;

/**
 * Jackson XSS 防护配置
 *
 * 对所有反序列化的 String 类型字段自动进行 HTML 转义，
 * 防止存储型 XSS 攻击。
 *
 * <p>原理: 自定义 String 反序列化器，在 JSON 反序列化阶段对用户输入进行清理。
 * 覆盖 {@code @RequestBody} 接收的 JSON 数据，补充 XssFilter 无法覆盖的场景。
 *
 * @see XssFilterConfig URL 参数层面的 XSS 过滤
 */
@Configuration
public class JacksonXssConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.build();
        SimpleModule xssModule = new SimpleModule("XssStringDeserializer");
        xssModule.addDeserializer(String.class, new XssStringDeserializer());
        mapper.registerModule(xssModule);
        return mapper;
    }

    /**
     * XSS 安全的 String 反序列化器
     *
     * 对 HTML 特殊字符进行转义:
     * - {@code &} → {@code &amp;}
     * - {@code <} → {@code &lt;}
     * - {@code >} → {@code &gt;}
     * - {@code "} → {@code &quot;}
     * - {@code '} → {@code &#x27;}
     */
    static class XssStringDeserializer extends JsonDeserializer<String> {

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getValueAsString();
            if (value == null || value.isEmpty()) {
                return value;
            }
            return cleanXss(value);
        }

        /**
         * 清理 XSS 相关字符
         *
         * <p>注意: 只转义最危险的字符，避免过度转义影响正常内容（如数学公式、代码片段）。
         * 对于需要显示原始 HTML 的场景（如富文本编辑器），应在 Controller 层单独处理。
         */
        private String cleanXss(String value) {
            if (value == null) return null;

            StringBuilder sb = new StringBuilder(value.length());
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '&' -> sb.append("&amp;");
                    case '<' -> sb.append("&lt;");
                    case '>' -> sb.append("&gt;");
                    case '"' -> sb.append("&quot;");
                    case '\'' -> sb.append("&#x27;");
                    default -> sb.append(c);
                }
            }
            return sb.toString();
        }

        @Override
        public Class<String> handledType() {
            return String.class;
        }
    }
}
