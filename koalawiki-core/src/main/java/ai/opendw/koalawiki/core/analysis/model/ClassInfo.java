package ai.opendw.koalawiki.core.analysis.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 类信息
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Data
public class ClassInfo {

    /**
     * 类名（简单名称）
     */
    private String className;

    /**
     * 包名
     */
    private String packageName;

    /**
     * 全限定类名
     */
    private String fullClassName;

    /**
     * 类注解列表
     */
    private List<AnnotationInfo> annotations = new ArrayList<>();

    /**
     * 字段列表
     */
    private List<FieldInfo> fields = new ArrayList<>();

    /**
     * 方法列表
     */
    private List<MethodInfo> methods = new ArrayList<>();

    /**
     * 父类
     */
    private String superClass;

    /**
     * 实现的接口
     */
    private List<String> interfaces = new ArrayList<>();

    /**
     * 是否是接口
     */
    private boolean isInterface;

    /**
     * 是否是抽象类
     */
    private boolean isAbstract;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 添加注解
     */
    public void addAnnotation(AnnotationInfo annotation) {
        this.annotations.add(annotation);
    }

    /**
     * 添加字段
     */
    public void addField(FieldInfo field) {
        this.fields.add(field);
    }

    /**
     * 添加方法
     */
    public void addMethod(MethodInfo method) {
        this.methods.add(method);
    }

    /**
     * 检查是否有指定注解
     */
    public boolean hasAnnotation(String annotationName) {
        return annotations.stream()
                .anyMatch(a -> a.getName().equals(annotationName) ||
                              a.getName().endsWith("." + annotationName));
    }
}
