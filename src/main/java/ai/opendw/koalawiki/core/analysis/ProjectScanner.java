package ai.opendw.koalawiki.core.analysis;

import ai.opendw.koalawiki.core.analysis.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 项目扫描器
 * 扫描整个Java项目，构建项目结构
 * 支持单模块和多模块Maven项目
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectScanner {

    private final JavaCodeAnalyzer codeAnalyzer;
    private final EntryPointDetector entryPointDetector;
    private final MavenPomParser pomParser;

    /**
     * 扫描项目
     *
     * @param projectPath 项目根路径
     * @return 项目结构
     */
    public ProjectStructure scanProject(String projectPath) {
        log.info("开始扫描项目: {}", projectPath);

        ProjectStructure structure = new ProjectStructure();
        structure.setProjectPath(projectPath);
        structure.setProjectName(extractProjectName(projectPath));

        try {
            // 1. 识别Maven模块
            List<ModuleInfo> modules = pomParser.scanMavenModules(projectPath);
            structure.setModules(modules);

            if (modules.size() > 1) {
                log.info("检测到多模块项目，模块数: {}", modules.size());
            }

            // 2. 扫描所有模块的Java文件
            List<File> allJavaFiles = new ArrayList<>();
            Map<String, String> fileToModuleMap = new HashMap<>();  // 文件路径 -> 模块名

            for (ModuleInfo module : modules) {
                List<File> moduleFiles = scanJavaFiles(module.getPath());
                allJavaFiles.addAll(moduleFiles);

                // 记录文件所属模块
                for (File file : moduleFiles) {
                    fileToModuleMap.put(file.getAbsolutePath(), module.getName());
                }

                log.info("模块 {} 扫描到 {} 个Java文件", module.getName(), moduleFiles.size());
            }

            log.info("总共扫描到 {} 个Java文件", allJavaFiles.size());

            // 3. 解析每个Java文件
            int parsedCount = 0;
            int entryPointCount = 0;

            for (File javaFile : allJavaFiles) {
                ClassInfo classInfo = codeAnalyzer.analyzeFile(javaFile);

                if (classInfo != null) {
                    // 添加模块信息
                    String moduleName = fileToModuleMap.get(javaFile.getAbsolutePath());
                    if (moduleName != null) {
                        // 可以在ClassInfo中添加module字段，或者通过其他方式关联
                        // 这里暂时通过文件路径判断
                    }

                    // 添加到项目结构
                    structure.addClass(classInfo);
                    parsedCount++;

                    // 检测入口点
                    List<EntryPoint> entryPoints = entryPointDetector.detectEntryPoints(classInfo);

                    // 为每个入口点添加模块信息
                    for (EntryPoint ep : entryPoints) {
                        if (moduleName != null) {
                            ep.addAnnotation("module", moduleName);
                        }
                        structure.addEntryPoint(ep);
                        entryPointCount++;
                    }
                }
            }

            log.info("项目扫描完成: 解析{}个类, 检测到{}个入口点", parsedCount, entryPointCount);
            log.info("统计信息: {}", structure.getStatistics());

        } catch (Exception e) {
            log.error("扫描项目失败: {}", projectPath, e);
            throw new RuntimeException("项目扫描失败: " + e.getMessage(), e);
        }

        return structure;
    }

    /**
     * 扫描指定路径下的Java文件
     */
    private List<File> scanJavaFiles(String path) {
        List<File> javaFiles = new ArrayList<>();

        try {
            Path rootPath = Paths.get(path);

            // 只扫描src/main/java目录（如果存在）
            Path srcPath = rootPath.resolve("src/main/java");
            if (!Files.exists(srcPath)) {
                // 如果没有标准Maven结构，扫描整个目录
                srcPath = rootPath;
            }

            try (Stream<Path> paths = Files.walk(srcPath)) {
                paths.filter(Files::isRegularFile)
                     .filter(p -> p.toString().endsWith(".java"))
                     .filter(p -> !p.toString().contains("/test/"))  // 排除测试
                     .filter(p -> !p.toString().contains("\\test\\"))  // 排除测试(Windows)
                     .filter(p -> !p.toString().contains("/target/"))  // 排除编译输出
                     .filter(p -> !p.toString().contains("\\target\\"))
                     .forEach(p -> javaFiles.add(p.toFile()));
            }

        } catch (Exception e) {
            log.error("扫描Java文件失败: {}", path, e);
        }

        return javaFiles;
    }

    /**
     * 从路径中提取项目名称
     */
    private String extractProjectName(String projectPath) {
        Path path = Paths.get(projectPath);
        return path.getFileName().toString();
    }

    /**
     * 过滤关键入口类（用于减少传给AI的数据量）
     *
     * @param structure 项目结构
     * @return 关键类的ClassInfo列表
     */
    public List<ClassInfo> filterKeyClasses(ProjectStructure structure) {
        List<ClassInfo> keyClasses = new ArrayList<>();

        // 1. 所有有入口点的类
        structure.getEntryPoints().forEach(ep -> {
            ClassInfo classInfo = structure.getClasses().get(ep.getClassName());
            if (classInfo != null && !keyClasses.contains(classInfo)) {
                keyClasses.add(classInfo);
            }
        });

        // 2. Service层的接口
        structure.getClasses().values().stream()
                .filter(c -> c.isInterface())
                .filter(c -> c.getFullClassName().contains(".service."))
                .forEach(c -> {
                    if (!keyClasses.contains(c)) {
                        keyClasses.add(c);
                    }
                });

        log.info("过滤后关键类数量: {} (原始: {})", keyClasses.size(), structure.getClasses().size());
        return keyClasses;
    }
}
