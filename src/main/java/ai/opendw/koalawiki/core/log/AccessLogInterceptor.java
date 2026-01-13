package ai.opendw.koalawiki.core.log;

import ai.opendw.koalawiki.domain.log.AccessLog;
import ai.opendw.koalawiki.domain.log.ActionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 访问日志拦截器
 * 拦截HTTP请求，记录访问日志
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-13
 */
@Slf4j
@Component
public class AccessLogInterceptor implements HandlerInterceptor {

    @Autowired
    private AccessLogCollector accessLogCollector;

    @Autowired(required = false)
    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 请求开始时间属性名
     */
    private static final String START_TIME_ATTR = "startTime";

    /**
     * 需要忽略的URI前缀
     */
    private static final String[] IGNORED_URIS = {
            "/actuator",
            "/swagger",
            "/v2/api-docs",
            "/webjars",
            "/error",
            "/favicon.ico"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 记录请求开始时间
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        try {
            // 检查是否需要忽略
            String uri = request.getRequestURI();
            if (shouldIgnore(uri)) {
                return;
            }

            // 收集访问日志
            AccessLog accessLog = buildAccessLog(request, response, ex);
            accessLogCollector.collect(accessLog);

        } catch (Exception e) {
            log.error("Failed to collect access log", e);
        }
    }

    /**
     * 构建访问日志对象
     */
    private AccessLog buildAccessLog(HttpServletRequest request, HttpServletResponse response, Exception ex) {
        AccessLog accessLog = new AccessLog();

        // 基本信息
        accessLog.setId(UUID.randomUUID().toString().replace("-", ""));
        accessLog.setAccessTime(new Date());
        accessLog.setCreatedAt(new Date());

        // 请求信息
        accessLog.setRequestUri(request.getRequestURI());
        accessLog.setRequestMethod(request.getMethod());
        accessLog.setIpAddress(getClientIpAddress(request));
        accessLog.setUserAgent(request.getHeader("User-Agent"));
        accessLog.setReferer(request.getHeader("Referer"));
        accessLog.setSessionId(request.getSession(false) != null ? request.getSession().getId() : null);

        // 响应信息
        accessLog.setStatusCode(response.getStatus());
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        if (startTime != null) {
            accessLog.setResponseTime((int) (System.currentTimeMillis() - startTime));
        }

        // 用户信息（从session或header获取）
        String userId = getUserIdFromRequest(request);
        accessLog.setUserId(userId);

        // 仓库和文档信息（从URI解析）
        parseResourceInfo(request.getRequestURI(), accessLog);

        // 动作类型
        accessLog.setAction(determineAction(request));

        // 请求参数（限制大小）
        String params = extractRequestParams(request);
        if (params.length() > 1000) {
            params = params.substring(0, 1000) + "...";
        }
        accessLog.setRequestParams(params);

        // 错误信息
        if (ex != null) {
            accessLog.setErrorMessage(ex.getMessage());
        }

        return accessLog;
    }

    /**
     * 判断是否应该忽略该URI
     */
    private boolean shouldIgnore(String uri) {
        if (uri == null) {
            return true;
        }
        for (String prefix : IGNORED_URIS) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 如果是多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 从请求中获取用户ID
     */
    private String getUserIdFromRequest(HttpServletRequest request) {
        // 优先从session获取
        if (request.getSession(false) != null) {
            Object userId = request.getSession().getAttribute("userId");
            if (userId != null) {
                return userId.toString();
            }
        }

        // 从header获取（用于API token认证）
        String userId = request.getHeader("X-User-Id");
        if (userId != null) {
            return userId;
        }

        return null;
    }

    /**
     * 从URI解析资源信息（仓库ID、文档ID）
     */
    private void parseResourceInfo(String uri, AccessLog accessLog) {
        if (uri == null) {
            return;
        }

        // 示例URI: /api/warehouse/{warehouseId}/document/{documentId}
        String[] parts = uri.split("/");

        for (int i = 0; i < parts.length - 1; i++) {
            if ("warehouse".equals(parts[i]) && i + 1 < parts.length) {
                accessLog.setWarehouseId(parts[i + 1]);
            } else if ("document".equals(parts[i]) && i + 1 < parts.length) {
                accessLog.setDocumentId(parts[i + 1]);
            }
        }
    }

    /**
     * 确定访问动作类型
     */
    private String determineAction(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 根据URI和方法判断动作
        if (uri.contains("/search")) {
            return ActionType.SEARCH.getCode();
        } else if (uri.contains("/download")) {
            return ActionType.DOWNLOAD.getCode();
        } else if (uri.contains("/login")) {
            return ActionType.LOGIN.getCode();
        } else if (uri.contains("/logout")) {
            return ActionType.LOGOUT.getCode();
        } else if (uri.contains("/sync")) {
            return ActionType.SYNC.getCode();
        }

        // 根据HTTP方法判断
        switch (method) {
            case "GET":
                return ActionType.VIEW.getCode();
            case "POST":
                return ActionType.CREATE.getCode();
            case "PUT":
            case "PATCH":
                return ActionType.UPDATE.getCode();
            case "DELETE":
                return ActionType.DELETE.getCode();
            default:
                return ActionType.VIEW.getCode();
        }
    }

    /**
     * 提取请求参数
     */
    private String extractRequestParams(HttpServletRequest request) {
        try {
            Map<String, String> params = new HashMap<>();
            Enumeration<String> paramNames = request.getParameterNames();

            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                String paramValue = request.getParameter(paramName);

                // 过滤敏感参数
                if (isSensitiveParam(paramName)) {
                    paramValue = "***";
                }

                params.put(paramName, paramValue);
            }

            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            log.warn("Failed to extract request params", e);
            return "{}";
        }
    }

    /**
     * 判断是否是敏感参数
     */
    private boolean isSensitiveParam(String paramName) {
        if (paramName == null) {
            return false;
        }
        String lowerName = paramName.toLowerCase();
        return lowerName.contains("password")
                || lowerName.contains("token")
                || lowerName.contains("secret")
                || lowerName.contains("key");
    }
}
