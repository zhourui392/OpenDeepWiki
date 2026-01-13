package ai.opendw.koalawiki.core.analysis.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务入口点
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Data
public class EntryPoint {

    /**
     * 入口类型
     */
    private EntryType type;

    /**
     * 路径或标识 (HTTP路径 / Dubbo接口名 / 定时任务cron)
     */
    private String path;

    /**
     * HTTP方法 (GET/POST等)
     */
    private String httpMethod;

    /**
     * 类名
     */
    private String className;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 方法签名
     */
    private String methodSignature;

    /**
     * 直接调用的方法列表
     */
    private List<String> directCalls = new ArrayList<>();

    /**
     * 注解信息
     */
    private Map<String, Object> annotations = new HashMap<>();

    /**
     * 描述信息
     */
    private String description;

    /**
     * 添加方法调用
     */
    public void addDirectCall(String methodCall) {
        this.directCalls.add(methodCall);
    }

    /**
     * 添加注解信息
     */
    public void addAnnotation(String key, Object value) {
        this.annotations.put(key, value);
    }
}
