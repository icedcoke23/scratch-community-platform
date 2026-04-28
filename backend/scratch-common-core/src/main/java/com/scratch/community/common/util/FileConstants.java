package com.scratch.community.common.util;

/**
 * 文件大小限制常量
 *
 * <p>统一定义各类型文件的最大大小，避免同一限制分散在多处导致不一致。
 * 所有涉及文件大小校验的地方（Controller、FileUploadUtils、SB3Unzipper）
 * 都应引用此常量。
 */
public final class FileConstants {

    private FileConstants() {}

    /** 默认最大文件大小：50MB */
    public static final long DEFAULT_MAX_SIZE = 50 * 1024 * 1024L;

    /** SB3 文件最大大小：100MB */
    public static final long SB3_MAX_SIZE = 100 * 1024 * 1024L;

    /** 头像最大大小：5MB */
    public static final long AVATAR_MAX_SIZE = 5 * 1024 * 1024L;

    /** 单个 ZIP 条目最大大小：50MB（防止 ZIP 炸弹） */
    public static final long ZIP_ENTRY_MAX_SIZE = 50 * 1024 * 1024L;

    /** SB3 最大资源文件数 */
    public static final int SB3_MAX_ENTRY_COUNT = 1000;
}
