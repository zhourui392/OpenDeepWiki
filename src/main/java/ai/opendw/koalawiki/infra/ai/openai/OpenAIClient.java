package ai.opendw.koalawiki.infra.ai.openai;

import ai.opendw.koalawiki.infra.ai.ChatMessage;
import ai.opendw.koalawiki.infra.ai.IAIClient;
import ai.opendw.koalawiki.infra.ai.config.AIProperties;
import ai.opendw.koalawiki.infra.ai.exception.AIException;
import ai.opendw.koalawiki.infra.ai.exception.RateLimitException;
import ai.opendw.koalawiki.infra.ai.exception.TokenLimitException;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * OpenAI客户端实现
 * 支持GPT-4、GPT-4 Turbo等模型
 */
@Slf4j
@Component
public class OpenAIClient implements IAIClient {

    private final AIProperties aiProperties;
    private OpenAiService openAiService;

    public OpenAIClient(AIProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @PostConstruct
    public void init() {
        // 如果没有配置API key，跳过初始化
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().trim().isEmpty()) {
            log.warn("OpenAI API key未配置，跳过OpenAI客户端初始化。如需使用AI功能，请配置 koalawiki.ai.api-key");
            return;
        }

        log.info("初始化OpenAI客户端, 模型: {}, 端点: {}",
                aiProperties.getChatModel(), aiProperties.getEndpoint());

        try {
            this.openAiService = new OpenAiService(
                    aiProperties.getApiKey(),
                    Duration.ofMillis(aiProperties.getTimeout())
            );
            log.info("OpenAI客户端初始化成功");
        } catch (Exception e) {
            log.error("OpenAI客户端初始化失败", e);
            throw new AIException("OpenAI客户端初始化失败", e);
        }
    }

    @Override
    @Retryable(
            value = {AIException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000)
    )
    public String complete(String prompt) {
        return complete(prompt, null);
    }

    @Override
    @Retryable(
            value = {AIException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000)
    )
    public String complete(String prompt, Map<String, Object> options) {
        log.debug("调用OpenAI complete, prompt长度: {}", prompt.length());

        try {
            // 构建消息列表 (JDK 1.8兼容)
            List<ChatMessage> messages = Collections.singletonList(ChatMessage.user(prompt));
            return chat(messages, options);
        } catch (Exception e) {
            log.error("OpenAI调用失败", e);
            throw handleException(e);
        }
    }

    @Override
    @Retryable(
            value = {AIException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000)
    )
    public String chat(List<ChatMessage> messages) {
        return chat(messages, null);
    }

    @Override
    @Async("aiTaskExecutor")
    public CompletableFuture<String> chatAsync(List<ChatMessage> messages) {
        return CompletableFuture.completedFuture(chat(messages));
    }

    @Override
    @Retryable(
            value = {AIException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000)
    )
    public String chat(List<ChatMessage> messages, Map<String, Object> options) {
        if (openAiService == null) {
            throw new AIException("OpenAI客户端未初始化，请配置 API key");
        }

        log.debug("调用OpenAI chat, 消息数: {}", messages.size());

        try {
            // 转换为OpenAI的消息格式
            List<com.theokanning.openai.completion.chat.ChatMessage> openAiMessages =
                    messages.stream()
                            .map(msg -> new com.theokanning.openai.completion.chat.ChatMessage(
                                    msg.getRole(),
                                    msg.getContent()
                            ))
                            .collect(Collectors.toList());

            // 构建请求
            ChatCompletionRequest.ChatCompletionRequestBuilder builder = ChatCompletionRequest.builder()
                    .model(aiProperties.getChatModel())
                    .messages(openAiMessages)
                    .maxTokens(aiProperties.getMaxTokens())
                    .temperature(aiProperties.getTemperature());

            // 应用自定义选项
            if (options != null) {
                if (options.containsKey("maxTokens")) {
                    builder.maxTokens((Integer) options.get("maxTokens"));
                }
                if (options.containsKey("temperature")) {
                    builder.temperature((Double) options.get("temperature"));
                }
                if (options.containsKey("topP")) {
                    builder.topP((Double) options.get("topP"));
                }
            }

            ChatCompletionRequest request = builder.build();

            // 调用API
            long startTime = System.currentTimeMillis();
            ChatCompletionResult result = openAiService.createChatCompletion(request);
            long duration = System.currentTimeMillis() - startTime;

            log.debug("OpenAI调用成功, 耗时: {}ms, tokens: {}",
                    duration, result.getUsage().getTotalTokens());

            // 提取回复
            if (result.getChoices() != null && !result.getChoices().isEmpty()) {
                return result.getChoices().get(0).getMessage().getContent();
            } else {
                throw new AIException("OpenAI返回空结果");
            }
        } catch (Exception e) {
            log.error("OpenAI调用失败", e);
            throw handleException(e);
        }
    }

    @Override
    public boolean isAvailable() {
        if (openAiService == null) {
            return false;
        }
        try {
            // 简单的健康检查
            String result = complete("test", null);
            return result != null;
        } catch (Exception e) {
            log.warn("OpenAI服务不可用", e);
            return false;
        }
    }

    @Override
    public String getModelName() {
        return aiProperties.getChatModel();
    }

    /**
     * 处理异常
     */
    private AIException handleException(Exception e) {
        String message = e.getMessage();
        if (message != null) {
            if (message.contains("rate_limit") || message.contains("429")) {
                return new RateLimitException("OpenAI API限流，请稍后重试", e);
            } else if (message.contains("token") || message.contains("max_tokens")) {
                return new TokenLimitException("Token超出限制", e);
            }
        }
        return new AIException("OpenAI调用失败: " + message, e);
    }
}
