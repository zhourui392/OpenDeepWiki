package ai.opendw.koalawiki.core.analysis.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * README文档生成上下文
 *
 * @author zhourui(V33215020)
 * @since 2025/11/21
 */
@Data
public class ReadmeContext {

    private String projectName;
    private String description;
    private List<MavenModule> modules = new ArrayList<>();
    private List<PackageInfo> packages = new ArrayList<>();
    private List<Feature> features = new ArrayList<>();
    private String startupGuide;
    private String testGuide;
    private List<DataModel> dataModels = new ArrayList<>();

    @Data
    public static class MavenModule {
        private String name;
        private String artifactId;
        private String description;
        private List<String> dependencies = new ArrayList<>();
    }

    @Data
    public static class PackageInfo {
        private String packageName;
        private String purpose;
        private int classCount;
    }

    @Data
    public static class Feature {
        private String name;
        private String description;
        private String endpoint;
    }

    @Data
    public static class DataModel {
        private String entityName;
        private String tableName;
        private List<String> fields = new ArrayList<>();
        private List<String> relations = new ArrayList<>();
    }
}
