package ai.opendw.koalawiki.core.log;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 访问日志过滤器
 * 在Filter层面收集更底层的访问信息
 *
 * @author OpenDeepWiki Team
 * @since 2025-11-13
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AccessLogFilter extends OncePerRequestFilter {

    @Autowired
    private AccessLogCollector accessLogCollector;

    /**
     * 请求开始时间属性名
     */
    private static final String START_TIME_ATTR = "filterStartTime";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME_ATTR, startTime);

        try {
            // 继续过滤链
            filterChain.doFilter(request, response);
        } finally {
            // 记录请求完成时间（可选，用于监控）
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 慢请求警告（超过3秒）
            if (duration > 3000) {
                log.warn("Slow request detected: {} {}, duration: {}ms",
                        request.getMethod(), request.getRequestURI(), duration);
            }

            // 可以在这里添加更多的监控逻辑
            if (log.isDebugEnabled()) {
                log.debug("Request completed: {} {}, status: {}, duration: {}ms",
                        request.getMethod(), request.getRequestURI(),
                        response.getStatus(), duration);
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 过滤静态资源
        String uri = request.getRequestURI();
        return uri.endsWith(".css")
                || uri.endsWith(".js")
                || uri.endsWith(".jpg")
                || uri.endsWith(".png")
                || uri.endsWith(".gif")
                || uri.endsWith(".ico")
                || uri.endsWith(".woff")
                || uri.endsWith(".woff2")
                || uri.endsWith(".ttf");
    }
}
