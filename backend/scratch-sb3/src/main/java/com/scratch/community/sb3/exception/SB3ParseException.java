package com.scratch.community.sb3.exception;

/**
 * sb3 解析异常
 */
public class SB3ParseException extends RuntimeException {

    public SB3ParseException(String message) {
        super(message);
    }

    public SB3ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
