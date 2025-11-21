package ai.opendw.koalawiki.core.analysis;

import ai.opendw.koalawiki.core.analysis.model.ReadmeContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * README文档扫描器
 *
 * @author zhourui(V33215020)
 * @since 2025/11/21
 */
@Slf4j
@Component
public class ProjectReadmeScanner {

    public ReadmeContext scan(String projectPath) {
        ReadmeContext context = new ReadmeContext();

        try {
            context.setModules(scanMavenModules(projectPath));
            context.setPackages(scanPackages(projectPath));
            context.setFeatures(scanFeatures(projectPath));
            context.setStartupGuide(scanStartupGuide(projectPath));
            context.setTestGuide(scanTestGuide(projectPath));
            context.setDataModels(scanDataModels(projectPath));
        } catch (Exception e) {
            log.error("扫描项目失败", e);
        }

        return context;
    }

    private List<ReadmeContext.MavenModule> scanMavenModules(String projectPath) {
        List<ReadmeContext.MavenModule> modules = new ArrayList<>();

        try {
            File pomFile = new File(projectPath, "pom.xml");
            if (!pomFile.exists()) {
                return modules;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(pomFile);

            NodeList moduleNodes = doc.getElementsByTagName("module");
            for (int i = 0; i < moduleNodes.getLength(); i++) {
                String moduleName = moduleNodes.item(i).getTextContent().trim();
                File modulePom = new File(projectPath, moduleName + "/pom.xml");

                if (modulePom.exists()) {
                    ReadmeContext.MavenModule module = parseModulePom(modulePom);
                    if (module != null) {
                        modules.add(module);
                    }
                }
            }
        } catch (Exception e) {
            log.error("扫描Maven模块失败", e);
        }

        return modules;
    }

    private ReadmeContext.MavenModule parseModulePom(File pomFile) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(pomFile);

            ReadmeContext.MavenModule module = new ReadmeContext.MavenModule();

            NodeList artifactIdNodes = doc.getElementsByTagName("artifactId");
            if (artifactIdNodes.getLength() > 0) {
                module.setArtifactId(artifactIdNodes.item(0).getTextContent().trim());
                module.setName(module.getArtifactId());
            }

            NodeList nameNodes = doc.getElementsByTagName("name");
            if (nameNodes.getLength() > 0) {
                module.setName(nameNodes.item(0).getTextContent().trim());
            }

            NodeList descNodes = doc.getElementsByTagName("description");
            if (descNodes.getLength() > 0) {
                module.setDescription(descNodes.item(0).getTextContent().trim());
            }

            return module;
        } catch (Exception e) {
            log.error("解析模块pom失败: {}", pomFile, e);
            return null;
        }
    }

    private List<ReadmeContext.PackageInfo> scanPackages(String projectPath) {
        List<ReadmeContext.PackageInfo> packages = new ArrayList<>();

        try {
            Path srcPath = Paths.get(projectPath, "src/main/java");
            if (!Files.exists(srcPath)) {
                return packages;
            }

            Map<String, Integer> packageCounts = new HashMap<>();

            Files.walk(srcPath)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> {
                        String pkg = extractPackage(p, srcPath);
                        if (pkg != null) {
                            packageCounts.put(pkg, packageCounts.getOrDefault(pkg, 0) + 1);
                        }
                    });

            for (Map.Entry<String, Integer> entry : packageCounts.entrySet()) {
                ReadmeContext.PackageInfo info = new ReadmeContext.PackageInfo();
                info.setPackageName(entry.getKey());
                info.setClassCount(entry.getValue());
                info.setPurpose(inferPurpose(entry.getKey()));
                packages.add(info);
            }

        } catch (Exception e) {
            log.error("扫描包结构失败", e);
        }

        return packages;
    }

    private String extractPackage(Path javaFile, Path srcRoot) {
        try {
            Path relative = srcRoot.relativize(javaFile.getParent());
            return relative.toString().replace(File.separator, ".");
        } catch (Exception e) {
            return null;
        }
    }

    private String inferPurpose(String packageName) {
        if (packageName.contains("controller")) return "接口层";
        if (packageName.contains("service")) return "业务层";
        if (packageName.contains("repository") || packageName.contains("dao")) return "数据访问层";
        if (packageName.contains("entity") || packageName.contains("domain")) return "领域模型";
        if (packageName.contains("dto") || packageName.contains("vo")) return "数据传输对象";
        if (packageName.contains("config")) return "配置类";
        if (packageName.contains("util")) return "工具类";
        return "其他";
    }

    private List<ReadmeContext.Feature> scanFeatures(String projectPath) {
        List<ReadmeContext.Feature> features = new ArrayList<>();

        try {
            Path srcPath = Paths.get(projectPath, "src/main/java");
            if (!Files.exists(srcPath)) {
                return features;
            }

            Files.walk(srcPath)
                    .filter(p -> p.toString().endsWith("Controller.java"))
                    .forEach(p -> {
                        ReadmeContext.Feature feature = new ReadmeContext.Feature();
                        String fileName = p.getFileName().toString();
                        feature.setName(fileName.replace("Controller.java", ""));
                        feature.setDescription(feature.getName() + "相关功能");
                        features.add(feature);
                    });

        } catch (Exception e) {
            log.error("扫描功能列表失败", e);
        }

        return features;
    }

    private String scanStartupGuide(String projectPath) {
        StringBuilder guide = new StringBuilder();

        guide.append("```bash\n");
        guide.append("# 1. 克隆项目\n");
        guide.append("git clone <repository-url>\n\n");
        guide.append("# 2. 配置数据库\n");
        guide.append("# 修改 application.yml 中的数据库配置\n\n");
        guide.append("# 3. 编译项目\n");
        guide.append("mvn clean install\n\n");
        guide.append("# 4. 启动应用\n");
        guide.append("mvn spring-boot:run\n");
        guide.append("```\n");

        return guide.toString();
    }

    private String scanTestGuide(String projectPath) {
        StringBuilder guide = new StringBuilder();

        guide.append("```bash\n");
        guide.append("# 运行所有测试\n");
        guide.append("mvn test\n\n");
        guide.append("# 运行单个测试类\n");
        guide.append("mvn test -Dtest=YourTestClass\n");
        guide.append("```\n");

        return guide.toString();
    }

    private List<ReadmeContext.DataModel> scanDataModels(String projectPath) {
        List<ReadmeContext.DataModel> models = new ArrayList<>();

        try {
            Path srcPath = Paths.get(projectPath, "src/main/java");
            if (!Files.exists(srcPath)) {
                return models;
            }

            Files.walk(srcPath)
                    .filter(p -> p.toString().endsWith("Entity.java"))
                    .forEach(p -> {
                        ReadmeContext.DataModel model = new ReadmeContext.DataModel();
                        String fileName = p.getFileName().toString();
                        model.setEntityName(fileName.replace(".java", ""));
                        model.setTableName(toSnakeCase(model.getEntityName().replace("Entity", "")));
                        models.add(model);
                    });

        } catch (Exception e) {
            log.error("扫描数据模型失败", e);
        }

        return models;
    }

    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
