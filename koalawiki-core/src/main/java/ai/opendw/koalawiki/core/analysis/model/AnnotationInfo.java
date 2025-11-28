package ai.opendw.koalawiki.core.analysis.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 注解信息
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Data
public class AnnotationInfo {

    /**
     * 注解名称（全限定名）
     */
    private String name;

    /**
     * 注解属性
     */
    private Map<String, Object> attributes = new HashMap<>();

    /**
     * 添加属性
     */
    public void addAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    /**
     * 获取属性值
     */
    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }

    /**
     * 获取属性值（字符串形式）
     *
     * @param key 属性名
     * @return 属性值字符串，不存在返回null
     */
    public String getAttributeValue(String key) {
        Object value = this.attributes.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 获取简单名称
     */
    public String getSimpleName() {
        if (name == null) {
            return null;
        }
        int lastDot = name.lastIndexOf('.');
        return lastDot >= 0 ? name.substring(lastDot + 1) : name;
    }
}
