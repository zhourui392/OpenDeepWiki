package ai.opendw.koalawiki.core.analysis;

import ai.opendw.koalawiki.core.analysis.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 入口点检测器
 * 识别HTTP、Dubbo、Scheduled、MQ等服务入口
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-16
 */
@Slf4j
@Component
public class EntryPointDetector {

    /**
     * 从类信息中检测所有入口点
     *
     * @param classInfo 类信息
     * @return 入口点列表
     */
    public List<EntryPoint> detectEntryPoints(ClassInfo classInfo) {
        List<EntryPoint> entryPoints = new ArrayList<>();

        // 检测HTTP入口
        if (isHttpController(classInfo)) {
            entryPoints.addAll(detectHttpEndpoints(classInfo));
        }

        // 检测Dubbo服务
        if (isDubboService(classInfo)) {
            entryPoints.addAll(detectDubboMethods(classInfo));
        }

        // 检测定时任务
        entryPoints.addAll(detectScheduledTasks(classInfo));

        // 检测MQ消费者
        entryPoints.addAll(detectMqConsumers(classInfo));

        return entryPoints;
    }

    /**
     * 检测HTTP端点
     */
    private List<EntryPoint> detectHttpEndpoints(ClassInfo classInfo) {
        List<EntryPoint> endpoints = new ArrayList<>();

        // 获取类级别的RequestMapping
        String classPath = extractRequestMappingPath(classInfo.getAnnotations());

        // 遍历方法
        for (MethodInfo method : classInfo.getMethods()) {
            if (!method.isPublic()) {
                continue;
            }

            // 检查方法级别的请求映射注解
            String methodPath = null;
            String httpMethod = "GET"; // 默认

            for (AnnotationInfo ann : method.getAnnotations()) {
                String annName = ann.getSimpleName();

                if ("RequestMapping".equals(annName)) {
                    methodPath = extractPathFromAnnotation(ann);
                    Object methodAttr = ann.getAttribute("method");
                    if (methodAttr != null) {
                        httpMethod = methodAttr.toString();
                    }
                } else if ("GetMapping".equals(annName)) {
                    methodPath = extractPathFromAnnotation(ann);
                    httpMethod = "GET";
                } else if ("PostMapping".equals(annName)) {
                    methodPath = extractPathFromAnnotation(ann);
                    httpMethod = "POST";
                } else if ("PutMapping".equals(annName)) {
                    methodPath = extractPathFromAnnotation(ann);
                    httpMethod = "PUT";
                } else if ("DeleteMapping".equals(annName)) {
                    methodPath = extractPathFromAnnotation(ann);
                    httpMethod = "DELETE";
                } else if ("PatchMapping".equals(annName)) {
                    methodPath = extractPathFromAnnotation(ann);
                    httpMethod = "PATCH";
                }
            }

            // 如果找到了请求映射
            if (methodPath != null) {
                EntryPoint ep = new EntryPoint();
                ep.setType(EntryType.HTTP);
                ep.setPath(combinePath(classPath, methodPath));
                ep.setHttpMethod(httpMethod);
                ep.setClassName(classInfo.getFullClassName());
                ep.setMethodName(method.getName());
                ep.setMethodSignature(method.getSignature());
                ep.setDirectCalls(method.getCalledMethods());

                endpoints.add(ep);
                log.debug("检测到HTTP入口: {} {}", httpMethod, ep.getPath());
            }
        }

        return endpoints;
    }

    /**
     * 检测Dubbo服务方法
     */
    private List<EntryPoint> detectDubboMethods(ClassInfo classInfo) {
        List<EntryPoint> endpoints = new ArrayList<>();

        for (MethodInfo method : classInfo.getMethods()) {
            if (!method.isPublic()) {
                continue;
            }

            EntryPoint ep = new EntryPoint();
            ep.setType(EntryType.DUBBO);
            ep.setPath(classInfo.getFullClassName());
            ep.setClassName(classInfo.getFullClassName());
            ep.setMethodName(method.getName());
            ep.setMethodSignature(method.getSignature());
            ep.setDirectCalls(method.getCalledMethods());

            endpoints.add(ep);
            log.debug("检测到Dubbo服务: {}.{}", classInfo.getClassName(), method.getName());
        }

        return endpoints;
    }

    /**
     * 检测定时任务
     */
    private List<EntryPoint> detectScheduledTasks(ClassInfo classInfo) {
        List<EntryPoint> endpoints = new ArrayList<>();

        for (MethodInfo method : classInfo.getMethods()) {
            for (AnnotationInfo ann : method.getAnnotations()) {
                if ("Scheduled".equals(ann.getSimpleName())) {
                    EntryPoint ep = new EntryPoint();
                    ep.setType(EntryType.SCHEDULED);
                    ep.setClassName(classInfo.getFullClassName());
                    ep.setMethodName(method.getName());
                    ep.setMethodSignature(method.getSignature());
                    ep.setDirectCalls(method.getCalledMethods());

                    // 提取cron表达式
                    Object cron = ann.getAttribute("cron");
                    if (cron != null) {
                        ep.setPath(cron.toString());
                        ep.setDescription("Cron: " + cron);
                    }

                    endpoints.add(ep);
                    log.debug("检测到定时任务: {}.{}", classInfo.getClassName(), method.getName());
                }
            }
        }

        return endpoints;
    }

    /**
     * 检测MQ消费者
     */
    private List<EntryPoint> detectMqConsumers(ClassInfo classInfo) {
        List<EntryPoint> endpoints = new ArrayList<>();

        for (MethodInfo method : classInfo.getMethods()) {
            for (AnnotationInfo ann : method.getAnnotations()) {
                String annName = ann.getSimpleName();

                if ("RabbitListener".equals(annName) ||
                    "KafkaListener".equals(annName) ||
                    "RocketMQMessageListener".equals(annName) ||
                    "JmsListener".equals(annName)) {

                    EntryPoint ep = new EntryPoint();
                    ep.setType(EntryType.MQ);
                    ep.setClassName(classInfo.getFullClassName());
                    ep.setMethodName(method.getName());
                    ep.setMethodSignature(method.getSignature());
                    ep.setDirectCalls(method.getCalledMethods());

                    // 提取队列/主题信息
                    Object queues = ann.getAttribute("queues");
                    Object topics = ann.getAttribute("topics");
                    Object destination = ann.getAttribute("destination");

                    if (queues != null) {
                        ep.setPath("Queue: " + queues);
                    } else if (topics != null) {
                        ep.setPath("Topic: " + topics);
                    } else if (destination != null) {
                        ep.setPath("Destination: " + destination);
                    }

                    ep.setDescription(annName);

                    endpoints.add(ep);
                    log.debug("检测到MQ消费者: {}.{}", classInfo.getClassName(), method.getName());
                }
            }
        }

        return endpoints;
    }

    /**
     * 判断是否是HTTP Controller
     */
    private boolean isHttpController(ClassInfo classInfo) {
        return classInfo.hasAnnotation("RestController") ||
               classInfo.hasAnnotation("Controller");
    }

    /**
     * 判断是否是Dubbo服务
     */
    private boolean isDubboService(ClassInfo classInfo) {
        return classInfo.hasAnnotation("DubboService") ||
               classInfo.hasAnnotation("Service");  // 可能是org.apache.dubbo.config.annotation.Service
    }

    /**
     * 从类注解中提取RequestMapping路径
     */
    private String extractRequestMappingPath(List<AnnotationInfo> annotations) {
        for (AnnotationInfo ann : annotations) {
            if ("RequestMapping".equals(ann.getSimpleName())) {
                return extractPathFromAnnotation(ann);
            }
        }
        return "";
    }

    /**
     * 从注解中提取路径
     */
    private String extractPathFromAnnotation(AnnotationInfo ann) {
        Object value = ann.getAttribute("value");
        Object path = ann.getAttribute("path");

        String pathStr = null;
        if (value != null) {
            pathStr = value.toString();
        } else if (path != null) {
            pathStr = path.toString();
        }

        if (pathStr != null) {
            // 清理引号和大括号
            pathStr = pathStr.replaceAll("[\"'\\{\\}]", "").trim();
        }

        return pathStr != null ? pathStr : "";
    }

    /**
     * 组合路径
     */
    private String combinePath(String classPath, String methodPath) {
        if (classPath == null) classPath = "";
        if (methodPath == null) methodPath = "";

        // 确保路径以/开头
        if (!classPath.isEmpty() && !classPath.startsWith("/")) {
            classPath = "/" + classPath;
        }
        if (!methodPath.isEmpty() && !methodPath.startsWith("/")) {
            methodPath = "/" + methodPath;
        }

        // 组合路径
        String combined = classPath + methodPath;

        // 去除重复的/
        combined = combined.replaceAll("/+", "/");

        return combined.isEmpty() ? "/" : combined;
    }
}
