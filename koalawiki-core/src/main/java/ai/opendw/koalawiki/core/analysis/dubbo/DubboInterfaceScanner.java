package ai.opendw.koalawiki.core.analysis.dubbo;

import ai.opendw.koalawiki.core.analysis.JavaCodeAnalyzer;
import ai.opendw.koalawiki.core.analysis.model.AnnotationInfo;
import ai.opendw.koalawiki.core.analysis.model.ClassInfo;
import ai.opendw.koalawiki.core.analysis.model.FieldInfo;
import ai.opendw.koalawiki.core.analysis.model.MethodInfo;
import ai.opendw.koalawiki.domain.dubbo.DubboInterfaceConsumer;
import ai.opendw.koalawiki.domain.dubbo.DubboInterfaceRegistry;
import ai.opendw.koalawiki.domain.dubbo.DubboMethodInfo;
import ai.opendw.koalawiki.domain.dubbo.MethodParameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Dubbo接口扫描器
 * 扫描Java源代码，识别Dubbo服务提供者和消费者
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DubboInterfaceScanner {

    private static final Set<String> DUBBO_SERVICE_ANNOTATIONS = new HashSet<>(Arrays.asList(
            "DubboService", "Service", "org.apache.dubbo.config.annotation.DubboService",
            "org.apache.dubbo.config.annotation.Service", "com.alibaba.dubbo.config.annotation.Service"
    ));

    private static final Set<String> DUBBO_REFERENCE_ANNOTATIONS = new HashSet<>(Arrays.asList(
            "DubboReference", "Reference", "org.apache.dubbo.config.annotation.DubboReference",
            "org.apache.dubbo.config.annotation.Reference", "com.alibaba.dubbo.config.annotation.Reference"
    ));

    private static final Pattern JAVADOC_PATTERN = Pattern.compile(
            "/\\*\\*([^*]|\\*(?!/))*\\*/", Pattern.DOTALL
    );

    private static final Pattern PARAM_PATTERN = Pattern.compile(
            "@param\\s+(\\w+)\\s+(.+?)(?=@|$)", Pattern.DOTALL
    );

    private static final Pattern RETURN_PATTERN = Pattern.compile(
            "@return\\s+(.+?)(?=@|$)", Pattern.DOTALL
    );

    private final JavaCodeAnalyzer javaCodeAnalyzer;

    /**
     * 扫描仓库中的Dubbo接口
     *
     * @param repoPath    仓库路径
     * @param clusterId   集群ID
     * @param warehouseId 仓库ID
     * @param serviceName 服务名
     * @return 扫描结果
     */
    public ScanResult scanRepository(Path repoPath, String clusterId, String warehouseId, String serviceName) {
        log.info("开始扫描Dubbo接口: repoPath={}, warehouseId={}", repoPath, warehouseId);

        ScanResult result = new ScanResult();
        result.setWarehouseId(warehouseId);
        result.setServiceName(serviceName);

        Map<String, ClassInfo> interfaceDefinitions = new HashMap<>();
        Map<String, ClassInfo> implementationClasses = new HashMap<>();
        List<ConsumerInfo> consumerInfos = new ArrayList<>();

        try (Stream<Path> pathStream = Files.walk(repoPath)) {
            pathStream.filter(this::isJavaFile)
                    .forEach(javaFilePath -> {
                        ClassInfo classInfo = javaCodeAnalyzer.analyzeFile(javaFilePath.toFile());
                        if (classInfo == null) {
                            return;
                        }

                        if (classInfo.isInterface()) {
                            interfaceDefinitions.put(classInfo.getFullClassName(), classInfo);
                        }

                        if (isDubboServiceProvider(classInfo)) {
                            implementationClasses.put(classInfo.getFullClassName(), classInfo);
                        }

                        List<ConsumerInfo> consumers = extractConsumerInfos(classInfo, warehouseId, serviceName);
                        consumerInfos.addAll(consumers);
                    });
        } catch (IOException e) {
            log.error("扫描仓库失败: repoPath={}", repoPath, e);
        }

        List<DubboInterfaceRegistry> registries = buildInterfaceRegistries(
                clusterId, warehouseId, serviceName, interfaceDefinitions, implementationClasses, repoPath
        );
        result.setInterfaceRegistries(registries);

        List<DubboInterfaceConsumer> consumers = buildInterfaceConsumers(consumerInfos);
        result.setInterfaceConsumers(consumers);

        log.info("Dubbo接口扫描完成: warehouseId={}, 提供接口数={}, 消费接口数={}",
                warehouseId, registries.size(), consumers.size());

        return result;
    }

    /**
     * 判断是否是Java文件
     */
    private boolean isJavaFile(Path path) {
        return Files.isRegularFile(path) && path.toString().endsWith(".java");
    }

    /**
     * 判断是否是Dubbo服务提供者
     */
    private boolean isDubboServiceProvider(ClassInfo classInfo) {
        for (AnnotationInfo annotation : classInfo.getAnnotations()) {
            if (DUBBO_SERVICE_ANNOTATIONS.contains(annotation.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 提取消费者信息
     */
    private List<ConsumerInfo> extractConsumerInfos(ClassInfo classInfo, String warehouseId, String serviceName) {
        List<ConsumerInfo> consumers = new ArrayList<>();

        for (FieldInfo field : classInfo.getFields()) {
            for (AnnotationInfo annotation : field.getAnnotations()) {
                if (DUBBO_REFERENCE_ANNOTATIONS.contains(annotation.getName())) {
                    ConsumerInfo consumerInfo = new ConsumerInfo();
                    consumerInfo.setInterfaceName(resolveFullTypeName(field.getType(), classInfo));
                    consumerInfo.setConsumerWarehouseId(warehouseId);
                    consumerInfo.setConsumerServiceName(serviceName);
                    consumerInfo.setSourceClass(classInfo.getFullClassName());
                    consumerInfo.setSourceField(field.getName());

                    String version = annotation.getAttributeValue("version");
                    String group = annotation.getAttributeValue("group");
                    consumerInfo.setVersion(cleanAttributeValue(version));
                    consumerInfo.setGroup(cleanAttributeValue(group));

                    consumers.add(consumerInfo);
                    log.debug("发现Dubbo消费者: class={}, field={}, interface={}",
                            classInfo.getFullClassName(), field.getName(), consumerInfo.getInterfaceName());
                }
            }
        }

        return consumers;
    }

    /**
     * 构建接口注册信息
     */
    private List<DubboInterfaceRegistry> buildInterfaceRegistries(
            String clusterId,
            String warehouseId,
            String serviceName,
            Map<String, ClassInfo> interfaceDefinitions,
            Map<String, ClassInfo> implementationClasses,
            Path repoPath) {

        List<DubboInterfaceRegistry> registries = new ArrayList<>();

        for (ClassInfo implClass : implementationClasses.values()) {
            for (String interfaceName : implClass.getInterfaces()) {
                String fullInterfaceName = resolveFullTypeName(interfaceName, implClass);

                ClassInfo interfaceClass = interfaceDefinitions.get(fullInterfaceName);
                if (interfaceClass == null) {
                    interfaceClass = findInterfaceBySimpleName(interfaceName, interfaceDefinitions);
                }

                DubboInterfaceRegistry registry = new DubboInterfaceRegistry();
                registry.setClusterId(clusterId);
                registry.setInterfaceName(fullInterfaceName);
                registry.setProviderWarehouseId(warehouseId);
                registry.setProviderServiceName(serviceName);

                AnnotationInfo serviceAnnotation = findDubboServiceAnnotation(implClass);
                if (serviceAnnotation != null) {
                    registry.setVersion(cleanAttributeValue(serviceAnnotation.getAttributeValue("version")));
                    registry.setGroupName(cleanAttributeValue(serviceAnnotation.getAttributeValue("group")));
                }

                if (interfaceClass != null) {
                    registry.setDescription(extractJavadocDescription(interfaceClass.getFilePath()));
                    registry.setSourceFile(getRelativePath(repoPath, interfaceClass.getFilePath()));
                    registry.setDeprecated(interfaceClass.hasAnnotation("Deprecated"));

                    List<DubboMethodInfo> methods = extractMethodInfos(interfaceClass);
                    registry.setMethods(methods);
                } else {
                    registry.setSourceFile(getRelativePath(repoPath, implClass.getFilePath()));
                    registry.setDeprecated(implClass.hasAnnotation("Deprecated"));
                }

                registries.add(registry);
                log.debug("发现Dubbo提供者: interface={}, provider={}", fullInterfaceName, serviceName);
            }
        }

        return registries;
    }

    /**
     * 构建接口消费者信息
     */
    private List<DubboInterfaceConsumer> buildInterfaceConsumers(List<ConsumerInfo> consumerInfos) {
        List<DubboInterfaceConsumer> consumers = new ArrayList<>();

        for (ConsumerInfo info : consumerInfos) {
            DubboInterfaceConsumer consumer = new DubboInterfaceConsumer();
            consumer.setConsumerWarehouseId(info.getConsumerWarehouseId());
            consumer.setConsumerServiceName(info.getConsumerServiceName());
            consumer.setSourceClass(info.getSourceClass());
            consumer.setSourceField(info.getSourceField());
            consumer.setCreatedAt(new Date());

            consumers.add(consumer);
        }

        return consumers;
    }

    /**
     * 提取方法信息
     */
    private List<DubboMethodInfo> extractMethodInfos(ClassInfo interfaceClass) {
        List<DubboMethodInfo> methodInfos = new ArrayList<>();
        Map<String, String> javadocComments = parseJavadocComments(interfaceClass.getFilePath());

        for (MethodInfo method : interfaceClass.getMethods()) {
            if (!method.isPublic()) {
                continue;
            }

            DubboMethodInfo methodInfo = new DubboMethodInfo();
            methodInfo.setName(method.getName());
            methodInfo.setReturnType(method.getReturnType());
            methodInfo.setDeprecated(method.hasAnnotation("Deprecated"));

            String methodKey = method.getName();
            String javadoc = javadocComments.get(methodKey);
            if (javadoc != null) {
                methodInfo.setDescription(extractDescription(javadoc));
            }

            List<MethodParameter> parameters = extractParameters(method, javadoc);
            parameters.forEach(methodInfo::addParameter);

            methodInfos.add(methodInfo);
        }

        return methodInfos;
    }

    /**
     * 提取方法参数
     */
    private List<MethodParameter> extractParameters(MethodInfo method, String javadoc) {
        List<MethodParameter> parameters = new ArrayList<>();
        Map<String, String> paramDescriptions = parseParamDescriptions(javadoc);

        int index = 0;
        for (String param : method.getParameters()) {
            String[] parts = param.split("\\s+", 2);
            String type = parts.length > 0 ? parts[0] : "Object";
            String name = parts.length > 1 ? parts[1] : "arg" + index;

            MethodParameter parameter = new MethodParameter();
            parameter.setIndex(index);
            parameter.setType(type);
            parameter.setName(name);
            parameter.setDescription(paramDescriptions.get(name));
            parameter.setRequired(true);

            parameters.add(parameter);
            index++;
        }

        return parameters;
    }

    /**
     * 查找Dubbo服务注解
     */
    private AnnotationInfo findDubboServiceAnnotation(ClassInfo classInfo) {
        for (AnnotationInfo annotation : classInfo.getAnnotations()) {
            if (DUBBO_SERVICE_ANNOTATIONS.contains(annotation.getName())) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * 根据简单名查找接口
     */
    private ClassInfo findInterfaceBySimpleName(String simpleName, Map<String, ClassInfo> interfaceDefinitions) {
        for (Map.Entry<String, ClassInfo> entry : interfaceDefinitions.entrySet()) {
            if (entry.getKey().endsWith("." + simpleName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 解析完整类型名
     */
    private String resolveFullTypeName(String typeName, ClassInfo classInfo) {
        if (typeName == null || typeName.isEmpty()) {
            return typeName;
        }

        if (typeName.contains(".")) {
            return typeName;
        }

        if (classInfo.getPackageName() != null) {
            return classInfo.getPackageName() + "." + typeName;
        }

        return typeName;
    }

    /**
     * 获取相对路径
     */
    private String getRelativePath(Path basePath, String filePath) {
        if (filePath == null) {
            return null;
        }
        try {
            Path file = Paths.get(filePath);
            return basePath.relativize(file).toString();
        } catch (Exception e) {
            return filePath;
        }
    }

    /**
     * 清理注解属性值
     */
    private String cleanAttributeValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.replace("\"", "").replace("'", "").trim();
    }

    /**
     * 提取Javadoc描述
     */
    private String extractJavadocDescription(String filePath) {
        if (filePath == null) {
            return null;
        }

        try {
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            String content = new String(bytes, StandardCharsets.UTF_8);
            Matcher matcher = JAVADOC_PATTERN.matcher(content);
            if (matcher.find()) {
                String javadoc = matcher.group();
                return extractDescription(javadoc);
            }
        } catch (IOException e) {
            log.debug("读取Javadoc失败: {}", filePath);
        }
        return null;
    }

    /**
     * 解析Javadoc注释
     */
    private Map<String, String> parseJavadocComments(String filePath) {
        Map<String, String> comments = new HashMap<>();

        if (filePath == null) {
            return comments;
        }

        try {
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            String content = new String(bytes, StandardCharsets.UTF_8);
            Matcher matcher = JAVADOC_PATTERN.matcher(content);

            while (matcher.find()) {
                String javadoc = matcher.group();
                int javadocEnd = matcher.end();

                String afterJavadoc = content.substring(javadocEnd, Math.min(javadocEnd + 500, content.length()));
                String methodName = extractMethodName(afterJavadoc);
                if (methodName != null) {
                    comments.put(methodName, javadoc);
                }
            }
        } catch (IOException e) {
            log.debug("解析Javadoc失败: {}", filePath);
        }

        return comments;
    }

    /**
     * 从Javadoc后的代码中提取方法名
     */
    private String extractMethodName(String codeAfterJavadoc) {
        Pattern methodPattern = Pattern.compile("\\w+\\s+(\\w+)\\s*\\(");
        Matcher matcher = methodPattern.matcher(codeAfterJavadoc);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 提取描述信息
     */
    private String extractDescription(String javadoc) {
        if (javadoc == null) {
            return null;
        }

        String cleaned = javadoc
                .replaceAll("/\\*\\*", "")
                .replaceAll("\\*/", "")
                .replaceAll("\\s*\\*\\s*", " ")
                .replaceAll("@\\w+.*", "")
                .trim();

        if (cleaned.length() > 500) {
            cleaned = cleaned.substring(0, 497) + "...";
        }

        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * 解析@param注释
     */
    private Map<String, String> parseParamDescriptions(String javadoc) {
        Map<String, String> descriptions = new HashMap<>();

        if (javadoc == null) {
            return descriptions;
        }

        Matcher matcher = PARAM_PATTERN.matcher(javadoc);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            String paramDesc = matcher.group(2).replaceAll("\\s*\\*\\s*", " ").trim();
            descriptions.put(paramName, paramDesc);
        }

        return descriptions;
    }

    /**
     * 消费者信息（内部类）
     */
    private static class ConsumerInfo {
        private String interfaceName;
        private String consumerWarehouseId;
        private String consumerServiceName;
        private String sourceClass;
        private String sourceField;
        private String version;
        private String group;

        public String getInterfaceName() {
            return interfaceName;
        }

        public void setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
        }

        public String getConsumerWarehouseId() {
            return consumerWarehouseId;
        }

        public void setConsumerWarehouseId(String consumerWarehouseId) {
            this.consumerWarehouseId = consumerWarehouseId;
        }

        public String getConsumerServiceName() {
            return consumerServiceName;
        }

        public void setConsumerServiceName(String consumerServiceName) {
            this.consumerServiceName = consumerServiceName;
        }

        public String getSourceClass() {
            return sourceClass;
        }

        public void setSourceClass(String sourceClass) {
            this.sourceClass = sourceClass;
        }

        public String getSourceField() {
            return sourceField;
        }

        public void setSourceField(String sourceField) {
            this.sourceField = sourceField;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }
    }

    /**
     * 扫描结果
     */
    public static class ScanResult {
        private String warehouseId;
        private String serviceName;
        private List<DubboInterfaceRegistry> interfaceRegistries = new ArrayList<>();
        private List<DubboInterfaceConsumer> interfaceConsumers = new ArrayList<>();

        public String getWarehouseId() {
            return warehouseId;
        }

        public void setWarehouseId(String warehouseId) {
            this.warehouseId = warehouseId;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public List<DubboInterfaceRegistry> getInterfaceRegistries() {
            return interfaceRegistries;
        }

        public void setInterfaceRegistries(List<DubboInterfaceRegistry> interfaceRegistries) {
            this.interfaceRegistries = interfaceRegistries;
        }

        public List<DubboInterfaceConsumer> getInterfaceConsumers() {
            return interfaceConsumers;
        }

        public void setInterfaceConsumers(List<DubboInterfaceConsumer> interfaceConsumers) {
            this.interfaceConsumers = interfaceConsumers;
        }

        public int getProviderCount() {
            return interfaceRegistries.size();
        }

        public int getConsumerCount() {
            return interfaceConsumers.size();
        }
    }
}
