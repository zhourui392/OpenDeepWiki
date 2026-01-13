package ai.opendw.koalawiki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import java.io.File;

/**
 * KoalaWiki 应用入口
 *
 * @author zhourui(V33215020)
 * @since 2025/01/13
 */
@SpringBootApplication
@EnableCaching
public class KoalaWikiApplication {

    public static void main(String[] args) {
        initDataDirectories();
        SpringApplication.run(KoalaWikiApplication.class, args);
    }

    /**
     * 初始化数据目录
     */
    private static void initDataDirectories() {
        String userHome = System.getProperty("user.home");
        String basePath = System.getenv("SQLITE_DB_PATH");

        if (basePath == null || basePath.isEmpty()) {
            basePath = userHome + File.separator + ".qpon" + File.separator + "data";
        } else {
            basePath = new File(basePath).getParent();
        }

        File dataDir = new File(basePath);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        String gitPath = System.getenv("GIT_STORAGE_PATH");
        if (gitPath == null || gitPath.isEmpty()) {
            gitPath = userHome + File.separator + ".qpon" + File.separator + "git";
        }

        File gitDir = new File(gitPath);
        if (!gitDir.exists()) {
            gitDir.mkdirs();
        }
    }
}
