package ai.opendw.koalawiki.core.analysis.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 字段信息
 *
 * @author zhourui(V33215020)
 * @since 2025/11/22
 */
@Data
public class FieldInfo {

    /**
     * 字段名
     */
    private String name;

    /**
     * 字段类型
     */
    private String type;

    /**
     * 字段注解
     */
    private List<AnnotationInfo> annotations = new ArrayList<>();

    /**
     * 添加注解
     */
    public void addAnnotation(AnnotationInfo annotation) {
        this.annotations.add(annotation);
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
