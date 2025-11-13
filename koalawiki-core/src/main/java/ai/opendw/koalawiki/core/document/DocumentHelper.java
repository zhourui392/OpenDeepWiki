package ai.opendw.koalawiki.core.document;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 文档助手工具类
 * 提供文档处理相关的工具方法
 *
 * 参考C#实现: DocumentsHelper.cs
 */
@Slf4j
public class DocumentHelper {

    // 默认忽略的文件扩展名
    private static final String[] DEFAULT_EXCLUDED_FILES = {
        "*.log", "*.tmp", "*.temp", "*.bak", "*.swp", "*.swo",
        "*.DS_Store", "Thumbs.db", "desktop.ini"
    };

    // 默认忽略的文件夹
    private static final String[] DEFAULT_EXCLUDED_FOLDERS = {
        "node_modules", ".git", ".svn", ".hg", ".idea", ".vscode",
        "target", "build", "dist", "bin", "obj", "__pycache__",
        ".gradle", ".settings", ".metadata"
    };

    // 文件大小限制（800KB）
    private static final long MAX_FILE_SIZE = 800 * 1024;

    /**
     * 读取README文件
     * 支持多种README文件名和格式
     */
    public static String readMeFile(String path) throws IOException {
        String[] readmeFiles = {"README.md", "README.rst", "README.txt", "README"};

        for (String filename : readmeFiles) {
            Path readmePath = Paths.get(path, filename);
            if (Files.exists(readmePath)) {
                // JDK 1.8兼容：使用Files.readAllBytes
                byte[] bytes = Files.readAllBytes(readmePath);
                return new String(bytes, StandardCharsets.UTF_8);
            }
        }

        return "";
    }

    /**
     * 获取忽略文件列表
     * 从.gitignore文件读取 + 默认忽略规则
     */
    public static String[] getIgnoreFiles(String path) {
        List<String> ignoreFiles = new ArrayList<>();

        // 读取.gitignore
        Path gitignorePath = Paths.get(path, ".gitignore");
        if (Files.exists(gitignorePath)) {
            try {
                List<String> lines = Files.readAllLines(gitignorePath, StandardCharsets.UTF_8);
                ignoreFiles.addAll(lines.stream()
                    .filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("#"))
                    .map(String::trim)
                    .collect(Collectors.toList()));
            } catch (IOException e) {
                log.warn("无法读取.gitignore文件: {}", path, e);
            }
        }

        // 添加默认忽略规则
        ignoreFiles.addAll(Arrays.asList(DEFAULT_EXCLUDED_FILES));
        ignoreFiles.addAll(Arrays.asList(DEFAULT_EXCLUDED_FOLDERS));

        return ignoreFiles.toArray(new String[0]);
    }

    /**
     * 获取目录结构（文本格式）
     */
    public static String getCatalogue(String path) {
        String[] ignoreFiles = getIgnoreFiles(path);
        List<PathInfo> pathInfos = new ArrayList<>();

        // 扫描目录
        scanDirectory(path, pathInfos, ignoreFiles);

        // 生成目录字符串
        StringBuilder catalogue = new StringBuilder();
        for (PathInfo info : pathInfos) {
            // 计算相对路径
            String relativePath = info.getPath()
                .replace(path, "")
                .replaceAll("^[/\\\\]+", "");

            // 过滤隐藏文件
            if (relativePath.startsWith(".")) {
                continue;
            }

            catalogue.append(relativePath).append("\n");
        }

        return catalogue.toString();
    }

    /**
     * 获取目录文件列表
     */
    public static List<PathInfo> getCatalogueFiles(String path) {
        String[] ignoreFiles = getIgnoreFiles(path);
        List<PathInfo> pathInfos = new ArrayList<>();
        scanDirectory(path, pathInfos, ignoreFiles);
        return pathInfos;
    }

    /**
     * 扫描目录
     * 使用栈避免递归调用栈溢出
     *
     * @param directoryPath 目录路径
     * @param infoList 输出的路径信息列表
     * @param ignorePatterns 忽略模式数组
     */
    public static void scanDirectory(String directoryPath, List<PathInfo> infoList,
                                     String[] ignorePatterns) {
        // 使用栈来避免递归调用栈溢出
        Stack<String> directoriesToProcess = new Stack<>();
        directoriesToProcess.push(directoryPath);

        while (!directoriesToProcess.isEmpty()) {
            String currentDirectory = directoriesToProcess.pop();
            Path currentPath = Paths.get(currentDirectory);

            try {
                // 遍历当前目录的所有文件
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath)) {
                    for (Path entry : stream) {
                        String fileName = entry.getFileName().toString();

                        if (Files.isRegularFile(entry)) {
                            // 检查是否应该忽略该文件
                            if (shouldIgnore(fileName, ignorePatterns)) {
                                continue;
                            }

                            // 检查文件大小
                            try {
                                long fileSize = Files.size(entry);
                                if (fileSize > MAX_FILE_SIZE) {
                                    log.debug("跳过大文件: {} ({}KB)", entry, fileSize / 1024);
                                    continue;
                                }

                                PathInfo pathInfo = new PathInfo();
                                pathInfo.setPath(entry.toString());
                                pathInfo.setName(fileName);
                                pathInfo.setType("File");
                                pathInfo.setSize(fileSize);
                                infoList.add(pathInfo);

                            } catch (IOException e) {
                                log.warn("无法获取文件大小: {}", entry, e);
                            }

                        } else if (Files.isDirectory(entry)) {
                            // 过滤隐藏目录
                            if (fileName.startsWith(".")) {
                                continue;
                            }

                            // 检查是否应该忽略该目录
                            if (shouldIgnore(fileName, ignorePatterns)) {
                                continue;
                            }

                            // 将子目录推入栈中等待处理
                            directoriesToProcess.push(entry.toString());

                            // 记录目录信息（可选）
                            PathInfo pathInfo = new PathInfo();
                            pathInfo.setPath(entry.toString());
                            pathInfo.setName(fileName);
                            pathInfo.setType("Directory");
                            pathInfo.setSize(0L);
                            // 不添加目录到列表，只添加文件
                            // infoList.add(pathInfo);
                        }
                    }
                }

            } catch (IOException e) {
                log.warn("无法访问目录: {}", currentDirectory, e);
            } catch (Exception e) {
                log.error("扫描目录时发生错误: {}", currentDirectory, e);
            }
        }
    }

    /**
     * 判断文件/目录是否应该被忽略
     * 支持通配符匹配
     */
    private static boolean shouldIgnore(String fileName, String[] patterns) {
        for (String pattern : patterns) {
            if (pattern == null || pattern.trim().isEmpty()) {
                continue;
            }

            String trimmedPattern = pattern.trim();

            // 跳过注释
            if (trimmedPattern.startsWith("#")) {
                continue;
            }

            // 处理目录模式（以/结尾）
            boolean directoryPattern = trimmedPattern.endsWith("/");
            if (directoryPattern) {
                trimmedPattern = trimmedPattern.substring(0, trimmedPattern.length() - 1);
            }

            // 转换通配符为正则表达式
            if (trimmedPattern.contains("*") || trimmedPattern.contains("?")) {
                String regexPattern = convertWildcardToRegex(trimmedPattern);
                if (Pattern.matches(regexPattern, fileName)) {
                    return true;
                }
            } else {
                // 精确匹配
                if (fileName.equalsIgnoreCase(trimmedPattern)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 将通配符模式转换为正则表达式
     */
    private static String convertWildcardToRegex(String wildcard) {
        StringBuilder regex = new StringBuilder("^");
        for (char c : wildcard.toCharArray()) {
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append(".");
                    break;
                case '.':
                case '\\':
                case '+':
                case '[':
                case ']':
                case '(':
                case ')':
                case '{':
                case '}':
                case '^':
                case '$':
                case '|':
                    regex.append("\\").append(c);
                    break;
                default:
                    regex.append(c);
                    break;
            }
        }
        regex.append("$");
        return regex.toString();
    }

    /**
     * 获取模型的最大tokens数量
     * 根据不同模型返回相应的token限制
     */
    public static Integer getMaxTokens(String model) {
        if (model == null || model.isEmpty()) {
            return null;
        }

        String modelLower = model.toLowerCase();

        // 特殊处理某些模型
        if (model.equalsIgnoreCase("kimi-k2-250711")) {
            return 32768;
        }

        if (modelLower.startsWith("kimi-k2")) {
            return 128000;
        }

        if (modelLower.startsWith("deepseek-r1")) {
            return 32768;
        }

        if (modelLower.startsWith("o")) {
            return 65535;
        }

        if (modelLower.startsWith("minimax-m1")) {
            return 40000;
        }

        if (modelLower.startsWith("qwen/qwen3-next-80b-a3b-instruct")) {
            return 32768;
        }

        // 常见模型的token限制
        Map<String, Integer> modelTokens = new HashMap<>();
        modelTokens.put("deepseek-chat", 8192);
        modelTokens.put("deepseek-v3", 16384);
        modelTokens.put("qwq-32b", 8192);
        modelTokens.put("gpt-4.1-mini", 32768);
        modelTokens.put("gpt-4.1", 32768);
        modelTokens.put("gpt-4o", 16384);
        modelTokens.put("o4-mini", 32768);
        modelTokens.put("doubao-1-5-pro-256k-250115", 256000);
        modelTokens.put("o3-mini", 32768);
        modelTokens.put("grok-3", 65536);
        modelTokens.put("qwen2.5-coder-3b-instruct", 65535);
        modelTokens.put("qwen3-235b-a22b", 16384);
        modelTokens.put("claude-sonnet-4-20250514", 63999);
        modelTokens.put("gemini-2.5-pro", 32768);
        modelTokens.put("gemini-2.5-flash", 32768);
        modelTokens.put("qwen3-32b", 32768);
        modelTokens.put("glm-4.5", 32768);
        modelTokens.put("glm-4.5v", 32768);
        modelTokens.put("deepseek-r1:32b-qwen-distill-fp16", 32768);

        return modelTokens.get(modelLower);
    }

    /**
     * 路径信息
     */
    @Data
    public static class PathInfo {
        private String path;
        private String name;
        private String type;  // "File" or "Directory"
        private Long size;
    }
}
