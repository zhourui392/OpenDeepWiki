package ai.opendw.koalawiki.infra.ai;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI客户端接口
 * Phase 3: 基础接口定义，支持OpenAI
 * Phase 4: 扩展支持多种AI提供商（Gemini、Claude等）
 */
public interface IAIClient {

    /**
     * 简单的文本补全（同步）
     *
     * @param prompt 提示词
     * @return 生成的文本
     */
    String complete(String prompt);

    /**
     * 带参数的文本补全（同步）
     *
     * @param prompt 提示词
     * @param options 选项参数（temperature、maxTokens等）
     * @return 生成的文本
     */
    String complete(String prompt, Map<String, Object> options);

    /**
     * 聊天补全（同步）
     *
     * @param messages 消息列表
     * @return 助手回复
     */
    String chat(List<ChatMessage> messages);

    /**
     * 聊天补全（异步）
     *
     * @param messages 消息列表
     * @return 助手回复的Future
     */
    CompletableFuture<String> chatAsync(List<ChatMessage> messages);

    /**
     * 带参数的聊天补全
     *
     * @param messages 消息列表
     * @param options 选项参数
     * @return 助手回复
     */
    String chat(List<ChatMessage> messages, Map<String, Object> options);

    /**
     * 检查服务是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable();

    /**
     * 获取当前使用的模型名称
     *
     * @return 模型名称
     */
    String getModelName();
}
