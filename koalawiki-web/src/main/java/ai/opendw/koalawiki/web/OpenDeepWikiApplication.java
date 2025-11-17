package ai.opendw.koalawiki.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * OpenDeepWiki Java 应用入口
 */
@SpringBootApplication
@ComponentScan(basePackages = "ai.opendw.koalawiki")
@EntityScan(basePackages = "ai.opendw.koalawiki.infra.entity")
@EnableJpaRepositories(basePackages = "ai.opendw.koalawiki.infra.repository")
@EnableCaching
public class OpenDeepWikiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenDeepWikiApplication.class, args);
    }
}
