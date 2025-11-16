package ai.opendw.koalawiki.core.analysis.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 方法信息
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Data
public class MethodInfo {

    /**
     * 方法名
     */
    private String name;

    /**
     * 参数列表
     */
    private List<String> parameters = new ArrayList<>();

    /**
     * 返回类型
     */
    private String returnType;

    /**
     * 方法注解
     */
    private List<AnnotationInfo> annotations = new ArrayList<>();

    /**
     * 调用的方法列表
     */
    private List<String> calledMethods = new ArrayList<>();

    /**
     * 是否是public方法
     */
    private boolean isPublic;

    /**
     * 是否是static方法
     */
    private boolean isStatic;

    /**
     * 方法签名
     */
    private String signature;

    /**
     * 添加注解
     */
    public void addAnnotation(AnnotationInfo annotation) {
        this.annotations.add(annotation);
    }

    /**
     * 添加调用的方法
     */
    public void addCalledMethod(String method) {
        this.calledMethods.add(method);
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
