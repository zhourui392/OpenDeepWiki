package ai.opendw.koalawiki.core.analysis;

import ai.opendw.koalawiki.core.analysis.model.ModuleInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Maven POM文件解析器
 * 解析pom.xml识别Maven模块结构
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Slf4j
@Component
public class MavenPomParser {

    /**
     * 扫描Maven项目，识别所有模块
     *
     * @param projectPath 项目根路径
     * @return 模块列表
     */
    public List<ModuleInfo> scanMavenModules(String projectPath) {
        List<ModuleInfo> modules = new ArrayList<>();

        try {
            Path rootPath = Paths.get(projectPath);
            File rootPom = rootPath.resolve("pom.xml").toFile();

            if (!rootPom.exists()) {
                log.warn("未找到pom.xml，可能不是Maven项目: {}", projectPath);
                // 如果没有pom.xml，创建单模块结构
                ModuleInfo singleModule = createSingleModule(projectPath);
                modules.add(singleModule);
                return modules;
            }

            // 解析根pom.xml
            List<String> subModuleNames = parseModules(rootPom);

            if (subModuleNames.isEmpty()) {
                // 单模块项目
                ModuleInfo module = parsePomFile(rootPom, projectPath);
                modules.add(module);
                log.info("检测到单模块Maven项目: {}", module.getName());
            } else {
                // 多模块项目
                log.info("检测到多模块Maven项目，子模块数: {}", subModuleNames.size());

                // 解析每个子模块
                for (String moduleName : subModuleNames) {
                    Path modulePath = rootPath.resolve(moduleName);
                    File modulePom = modulePath.resolve("pom.xml").toFile();

                    if (modulePom.exists()) {
                        ModuleInfo module = parsePomFile(modulePom, modulePath.toString());
                        modules.add(module);
                        log.debug("解析子模块: {}", module.getName());
                    } else {
                        log.warn("子模块pom.xml不存在: {}", modulePath);
                    }
                }
            }

        } catch (Exception e) {
            log.error("扫描Maven模块失败: {}", projectPath, e);
        }

        return modules;
    }

    /**
     * 解析pom.xml中的<modules>标签
     */
    private List<String> parseModules(File pomFile) {
        List<String> modules = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(pomFile);

            NodeList moduleNodes = doc.getElementsByTagName("module");
            for (int i = 0; i < moduleNodes.getLength(); i++) {
                Node node = moduleNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    // 检查是否是project下的直接modules子节点
                    Node parent = node.getParentNode();
                    if (parent != null && "modules".equals(parent.getNodeName())) {
                        String moduleName = node.getTextContent().trim();
                        if (!moduleName.isEmpty()) {
                            modules.add(moduleName);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("解析pom.xml失败: {}", pomFile.getPath(), e);
        }

        return modules;
    }

    /**
     * 解析单个pom.xml文件，提取模块信息
     */
    private ModuleInfo parsePomFile(File pomFile, String modulePath) {
        ModuleInfo module = new ModuleInfo();
        module.setPath(modulePath);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(pomFile);

            // 提取artifactId作为模块名
            NodeList artifactIdNodes = doc.getElementsByTagName("artifactId");
            if (artifactIdNodes.getLength() > 0) {
                String artifactId = artifactIdNodes.item(0).getTextContent().trim();
                module.setName(artifactId);
            }

            // 提取name标签作为描述
            NodeList nameNodes = doc.getElementsByTagName("name");
            if (nameNodes.getLength() > 0) {
                String name = nameNodes.item(0).getTextContent().trim();
                module.setDescription(name);
            }

            // 统计Java文件数
            int javaFileCount = countJavaFiles(Paths.get(modulePath));
            module.setClassCount(javaFileCount);

            // 统计包数量（粗略估计：扫描src/main/java下的目录）
            int packageCount = countPackages(Paths.get(modulePath));
            module.setPackageCount(packageCount);

            log.debug("解析模块: name={}, classes={}, packages={}",
                    module.getName(), module.getClassCount(), module.getPackageCount());

        } catch (Exception e) {
            log.error("解析pom.xml失败: {}", pomFile.getPath(), e);
            // 降级：使用路径名作为模块名
            module.setName(Paths.get(modulePath).getFileName().toString());
        }

        return module;
    }

    /**
     * 统计模块中的Java文件数量
     */
    private int countJavaFiles(Path modulePath) {
        try {
            Path srcPath = modulePath.resolve("src/main/java");
            if (!Files.exists(srcPath)) {
                return 0;
            }

            return (int) Files.walk(srcPath)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .count();

        } catch (Exception e) {
            log.warn("统计Java文件失败: {}", modulePath, e);
            return 0;
        }
    }

    /**
     * 统计包数量
     */
    private int countPackages(Path modulePath) {
        try {
            Path srcPath = modulePath.resolve("src/main/java");
            if (!Files.exists(srcPath)) {
                return 0;
            }

            return (int) Files.walk(srcPath)
                    .filter(Files::isDirectory)
                    .filter(p -> !p.equals(srcPath))  // 排除根目录
                    .count();

        } catch (Exception e) {
            log.warn("统计包数量失败: {}", modulePath, e);
            return 0;
        }
    }

    /**
     * 创建单模块结构（非Maven项目）
     */
    private ModuleInfo createSingleModule(String projectPath) {
        ModuleInfo module = new ModuleInfo();
        module.setName(Paths.get(projectPath).getFileName().toString());
        module.setPath(projectPath);
        module.setDescription("单模块项目");
        module.setClassCount(countJavaFiles(Paths.get(projectPath)));
        module.setPackageCount(countPackages(Paths.get(projectPath)));
        return module;
    }

    /**
     * 检查是否是Maven项目
     */
    public boolean isMavenProject(String projectPath) {
        Path pomPath = Paths.get(projectPath).resolve("pom.xml");
        return Files.exists(pomPath);
    }

    /**
     * 检查是否是多模块Maven项目
     */
    public boolean isMultiModuleProject(String projectPath) {
        try {
            Path pomPath = Paths.get(projectPath).resolve("pom.xml");
            if (!Files.exists(pomPath)) {
                return false;
            }

            List<String> modules = parseModules(pomPath.toFile());
            return !modules.isEmpty();

        } catch (Exception e) {
            return false;
        }
    }
}
