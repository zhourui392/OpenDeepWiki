package ai.opendw.koalawiki.infra.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * AI客户端测试器
 * 仅在开启测试模式时运行
 * 启动参数: --koalawiki.ai.test-on-startup=true
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "koalawiki.ai", name = "test-on-startup", havingValue = "true")
public class AIClientTester implements CommandLineRunner {

    private final IAIClient aiClient;

    public AIClientTester(IAIClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public void run(String... args) {
        log.info("开始测试AI客户端...");

        try {
            // 测试1: 简单补全
            log.info("测试1: 简单文本补全");
            String result1 = aiClient.complete("Say 'Hello, OpenDeepWiki!'");
            log.info("结果: {}", result1);

            // 测试2: 聊天补全 (JDK 1.8兼容)
            log.info("测试2: 聊天补全");
            List<ChatMessage> messages = Arrays.asList(
                    ChatMessage.system("You are a helpful assistant."),
                    ChatMessage.user("What is OpenDeepWiki?")
            );
            String result2 = aiClient.chat(messages);
            log.info("结果: {}", result2);

            // 测试3: 检查可用性
            log.info("测试3: 服务可用性");
            boolean available = aiClient.isAvailable();
            log.info("服务可用: {}", available);

            // 测试4: 获取模型名称
            log.info("测试4: 当前模型");
            String model = aiClient.getModelName();
            log.info("模型: {}", model);

            log.info("AI客户端测试完成！");
        } catch (Exception e) {
            log.error("AI客户端测试失败", e);
        }
    }
}
