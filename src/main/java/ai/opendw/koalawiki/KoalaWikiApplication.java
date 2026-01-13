package ai.opendw.koalawiki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * KoalaWiki 应用入口
 * @author zhourui(V33215020)
 */
@SpringBootApplication
@EnableCaching
public class KoalaWikiApplication {

    public static void main(String[] args) {
        SpringApplication.run(KoalaWikiApplication.class, args);
    }
}
