package ai.opendw.koalawiki.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA（单页应用）路由控制器
 *
 * 功能：将所有非API、非静态资源的请求转发到index.html
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
     * 1. 匹配根路径 "/"
     * 2. 匹配一级路径（排除API等）
     * 3. 匹配多级路径
     *
     * 排除路径：
     * - /api/**        - 后端API
     * - /actuator/**   - Spring Actuator端点
     * - /h2-console/** - H2控制台
     * - /static/**     - 静态资源
     * - *.js, *.css, *.ico, *.png 等 - 静态文件
     *
     * @return 转发到index.html
     */
    @GetMapping(value = {
        "/",
        "/{path:^(?!api|actuator|h2-console|static).*}",
        "/{path:^(?!api|actuator|h2-console|static).*}/**"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
