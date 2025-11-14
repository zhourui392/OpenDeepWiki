package ai.opendw.koalawiki.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring Security 配置
 *
 * 配置认证和授权规则
 *
 * @author OpenDeepWiki Team
 * @since 0.1.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            // 配置授权规则
            .authorizeRequests()
                // 允许访问所有页面和静态资源（暂时全部放开，后续可以细化）
                .antMatchers("/**").permitAll()
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            .and()
            // 禁用表单登录（使用前端自定义登录页面）
            .formLogin().disable()
            // 禁用HTTP Basic认证
            .httpBasic().disable()
            // 配置登出
            .logout()
                .logoutUrl("/api/auth/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            .and()
            // 禁用CSRF（开发阶段，生产环境应启用）
            .csrf().disable()
            .headers().frameOptions().disable(); // 允许H2控制台使用iframe
    }

    /**
     * 密码编码器
     * 使用BCrypt加密算法
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
