package com.scratch.community.sb3.parser;

import com.scratch.community.common.util.FileConstants;
import com.scratch.community.sb3.exception.SB3ParseException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * sb3 内存解压器
 * 将 sb3 (ZIP) 文件在内存中解压，返回 project.json 字符串和资源文件列表
 */
public class SB3Unzipper {

    /** 最大文件大小 100MB（引用 FileConstants 统一常量） */
    private static final long MAX_SB3_SIZE = FileConstants.SB3_MAX_SIZE;

    /** 最大资源文件数（引用 FileConstants 统一常量） */
    private static final int MAX_ENTRY_COUNT = FileConstants.SB3_MAX_ENTRY_COUNT;

    /** 单个 ZIP 条目最大大小 50MB（引用 FileConstants 统一常量） */
    private static final long MAX_ENTRY_SIZE = FileConstants.ZIP_ENTRY_MAX_SIZE;

    /**
     * 解压结果
     */
    public static class UnzipResult {
        private final String projectJson;
        private final Map<String, byte[]> assets;

        public UnzipResult(String projectJson, Map<String, byte[]> assets) {
            this.projectJson = projectJson;
            this.assets = assets;
        }

        public String getProjectJson() { return projectJson; }
        public Map<String, byte[]> getAssets() { return assets; }
    }

    /**
     * 内存解压 sb3 文件
     * @param sb3Bytes sb3 文件字节流
     * @return 解压结果
     * @throws SB3ParseException 格式错误时抛出
     */
    public UnzipResult unzip(byte[] sb3Bytes) throws SB3ParseException {
        if (sb3Bytes == null || sb3Bytes.length == 0) {
            throw new SB3ParseException("sb3 文件为空");
        }

        if (sb3Bytes.length > MAX_SB3_SIZE) {
            throw new SB3ParseException("sb3 文件超过 100MB 限制");
        }

        String projectJson = null;
        Map<String, byte[]> assets = new HashMap<>();
        int entryCount = 0;

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(sb3Bytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_ENTRY_COUNT) {
                    throw new SB3ParseException("sb3 文件包含超过 " + MAX_ENTRY_COUNT + " 个条目");
                }

                if (entry.isDirectory()) {
                    continue;
                }

                String name = entry.getName();

                // 安全检查：防止 ZIP 路径遍历攻击 (Zip Slip)
                // 1. 拒绝包含 ../ 的路径（相对路径遍历）
                // 2. 拒绝以 / 开头的路径（绝对路径遍历）
                // 3. 规范化后再次检查（防止 ..%2F 等编码绕过）
                if (name.contains("..") || name.startsWith("/")) {
                    throw new SB3ParseException("sb3 文件包含非法路径: " + name);
                }
                // 额外安全：规范化路径后检查是否逃逸根目录
                java.nio.file.Path normalizedPath = java.nio.file.Paths.get(name).normalize();
                String normalizedName = normalizedPath.toString().replace('\\', '/');
                if (normalizedName.startsWith("..") || normalizedName.startsWith("/")) {
                    throw new SB3ParseException("sb3 文件包含非法路径（规范化后）: " + name);
                }

                byte[] data = readAll(zis);

                if ("project.json".equals(name)) {
                    projectJson = new String(data, StandardCharsets.UTF_8);
                } else {
                    assets.put(name, data);
                }

                zis.closeEntry();
            }
        } catch (SB3ParseException e) {
            throw e;
        } catch (Exception e) {
            throw new SB3ParseException("解压 sb3 文件失败: " + e.getMessage(), e);
        }

        if (projectJson == null || projectJson.isBlank()) {
            throw new SB3ParseException("sb3 文件中缺少 project.json");
        }

        return new UnzipResult(projectJson, assets);
    }

    private byte[] readAll(ZipInputStream zis) throws IOException, SB3ParseException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(8192);
        byte[] buffer = new byte[4096];
        int totalRead = 0;
        int len;
        while ((len = zis.read(buffer)) != -1) {
            totalRead += len;
            if (totalRead > MAX_ENTRY_SIZE) {
                throw new SB3ParseException("ZIP 条目超过单文件 50MB 限制（可能为 ZIP 炸弹）");
            }
            bos.write(buffer, 0, len);
        }
        return bos.toByteArray();
    }
}
