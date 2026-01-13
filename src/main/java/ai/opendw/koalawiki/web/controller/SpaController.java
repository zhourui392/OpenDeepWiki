package ai.opendw.koalawiki.web.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * SPA（单页应用）路由控制器
 *
 * 功能：将所有非API、非静态资源的前端路由请求直接返回index.html内容
 * 让React Router处理前端路由
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
@Controller
public class SpaController {

    /**
     * 处理所有前端路由
     *
     * 匹配规则：
     * 1. 匹配一级路径（排除API、静态资源等）
     * 2. 匹配多级路径
     *
     * 排除路径：
     * - /api/**        - 后端API
     * - /actuator/**   - Spring Actuator端点
     * - /h2-console/** - H2控制台
     * - /static/**     - 静态资源
     * - 文件扩展名的请求（如 .js, .css, .png 等）
     *
     * 直接返回index.html的内容，避免使用forward造成的无限循环
     *
     * @param response HTTP响应
     * @throws IOException 读取文件异常
     */
    @GetMapping(value = {
        "/{path:^(?!api|actuator|h2-console|static|js|css|favicon\\.|.*\\.png$|.*\\.jpg$|.*\\.jpeg$|.*\\.gif$|.*\\.ico$|.*\\.svg$|.*\\.md$|.*\\.js$|.*\\.css$).*}",
        "/{path:^(?!api|actuator|h2-console|static|js|css).*}/**"
    })
    public void forwardToIndex(HttpServletResponse response) throws IOException {
        // 读取index.html文件
        Resource resource = new ClassPathResource("static/index.html");

        // 设置响应类型
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 设置缓存控制，index.html不应该被缓存
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        // 将index.html内容写入响应
        String html = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        response.getWriter().write(html);
        response.getWriter().flush();
    }
}
