package com.scratch.community.module.social.ai;

/**
 * LLM 调用异常
 */
public class LlmException extends RuntimeException {

    private final String provider;
    private final int statusCode;

    public LlmException(String provider, String message) {
        super(message);
        this.provider = provider;
        this.statusCode = 0;
    }

    public LlmException(String provider, String message, Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.statusCode = 0;
    }

    public LlmException(String provider, int statusCode, String message) {
        super(message);
        this.provider = provider;
        this.statusCode = statusCode;
    }

    public String getProvider() { return provider; }
    public int getStatusCode() { return statusCode; }
}
