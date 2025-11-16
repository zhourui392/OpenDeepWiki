# AI集成方案

> 文档来源：详细说明如何集成各种LLM API
>
> 最后更新：2025-11-16

## 1. LLM供应商对比

| 供应商 | 模型 | 优势 | 劣势 | 成本 | 适用场景 |
|--------|------|------|------|------|----------|
| OpenAI | GPT-4 | 质量最高,API稳定 | 成本较高 | $0.03/1K输入,$0.06/1K输出 | 复杂文档,架构设计 |
| | GPT-3.5-turbo | 性价比高,速度快 | 质量一般 | $0.001/1K输入,$0.002/1K输出 | 简单文档,批量处理 |
| Anthropic | Claude-3-Opus | 上下文长,理解力强 | API较新 | $0.015/1K输入,$0.075/1K输出 | 长代码分析 |
| | Claude-3-Sonnet | 平衡性能和成本 | - | $0.003/1K输入,$0.015/1K输出 | 日常文档生成 |
| Ollama | Llama2/Mistral | 本地部署,无成本 | 质量较低,需GPU | 免费(硬件成本) | 测试环境,私有部署 |

## 2. 推荐配置策略

### 2.1 分层使用策略

```
┌──────────────────────────────────────┐
│         复杂架构文档                  │
│       GPT-4 / Claude-3-Opus         │
│   (高质量要求,低频率)                 │
└──────────────────────────────────────┘
            ↓
┌──────────────────────────────────────┐
│      类/接口文档                      │
│  Claude-3-Sonnet / GPT-3.5-turbo    │
│   (中等质量,高频率)                   │
└──────────────────────────────────────┘
            ↓
┌──────────────────────────────────────┐
│    简单方法注释/README               │
│      GPT-3.5-turbo / Ollama         │
│   (基础质量,超高频率)                 │
└──────────────────────────────────────┘
```

### 2.2 成本控制配置

```yaml
# application.yml
ai:
  llm:
    # 主要模型配置
    primary:
      provider: openai
      model: gpt-3.5-turbo
      apiKey: ${OPENAI_API_KEY}
      baseUrl: https://api.openai.com/v1

    # 高级模型(用于复杂任务)
    premium:
      provider: openai
      model: gpt-4
      apiKey: ${OPENAI_API_KEY}

    # 本地模型(用于开发测试)
    local:
      provider: ollama
      model: mistral
      baseUrl: http://localhost:11434

    # 成本控制
    cost-control:
      daily-token-limit: 1000000  # 每天100万tokens
      per-user-daily-limit: 50000 # 每用户每天5万tokens
      per-file-max-tokens: 8000   # 单文件最大8K tokens
      auto-downgrade: true         # 超限自动降级

    # 模型选择策略
    strategy:
      doc-type-mapping:
        ARCHITECTURE: premium
        API: primary
        CLASS: primary
        METHOD: primary
        README: primary
```

## 3. OpenAI集成实现

### 3.1 依赖配置

```xml
<dependency>
    <groupId>com.theokanning.openai-gpt3-java</groupId>
    <artifactId>service</artifactId>
    <version>0.18.2</version>
</dependency>
```

### 3.2 客户端实现

```java
@Configuration
public class OpenAIConfig {

    @Bean
    public OpenAiService openAiService(@Value("${ai.llm.primary.apiKey}") String apiKey,
                                        @Value("${ai.llm.primary.baseUrl}") String baseUrl) {
        ObjectMapper mapper = defaultObjectMapper();
        OkHttpClient client = defaultClient(apiKey, Duration.ofSeconds(60));

        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(JacksonConverterFactory.create(mapper))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();

        return new OpenAiService(retrofit.create(OpenAiApi.class));
    }
}

@Service
@Slf4j
public class OpenAIDocumentGenerator implements LLMClient {

    private final OpenAiService openAiService;
    private final PromptManager promptManager;
    private final TokenLimiter tokenLimiter;

    @Override
    public String generateDocument(DocumentGenerationRequest request) {
        // 1. 构建Prompt
        String systemPrompt = promptManager.getSystemPrompt(request.getDocType());
        String userPrompt = promptManager.buildUserPrompt(request);

        // 2. Token限流检查
        int estimatedTokens = estimateTokens(systemPrompt + userPrompt);
        tokenLimiter.checkAndConsume(request.getUserId(), estimatedTokens);

        // 3. 调用OpenAI API
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
            .model(request.getModel() != null ? request.getModel() : "gpt-3.5-turbo")
            .messages(Arrays.asList(
                new ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt),
                new ChatMessage(ChatMessageRole.USER.value(), userPrompt)
            ))
            .temperature(0.3)
            .maxTokens(2000)
            .build();

        try {
            ChatCompletionResult result = openAiService.createChatCompletion(completionRequest);

            // 4. 记录Token使用
            Usage usage = result.getUsage();
            recordTokenUsage(request, usage);

            // 5. 返回生成的文档
            return result.getChoices().get(0).getMessage().getContent();

        } catch (Exception e) {
            log.error("OpenAI API call failed", e);
            throw new LLMServiceException("Failed to generate document", e);
        }
    }

    @Override
    public Flux<String> generateDocumentStream(DocumentGenerationRequest request) {
        // 流式生成实现
        return Flux.create(sink -> {
            openAiService.streamChatCompletion(buildRequest(request))
                .doOnNext(chunk -> {
                    String content = chunk.getChoices().get(0).getMessage().getContent();
                    if (content != null) {
                        sink.next(content);
                    }
                })
                .doOnError(sink::error)
                .doOnComplete(sink::complete)
                .subscribe();
        });
    }

    private int estimateTokens(String text) {
        // 简单估算: 1 token ≈ 4 characters
        return text.length() / 4;
    }

    private void recordTokenUsage(DocumentGenerationRequest request, Usage usage) {
        TokenUsageEntity entity = TokenUsageEntity.builder()
            .userId(request.getUserId())
            .warehouseId(request.getWarehouseId())
            .model(request.getModel())
            .operationType("GENERATE_DOC")
            .promptTokens(usage.getPromptTokens())
            .completionTokens(usage.getCompletionTokens())
            .totalTokens(usage.getTotalTokens())
            .estimatedCost(calculateCost(request.getModel(), usage))
            .build();

        tokenUsageRepository.save(entity);
    }

    private BigDecimal calculateCost(String model, Usage usage) {
        // 根据模型定价计算成本
        if ("gpt-4".equals(model)) {
            return BigDecimal.valueOf(usage.getPromptTokens() * 0.03 / 1000 +
                                      usage.getCompletionTokens() * 0.06 / 1000);
        } else {
            return BigDecimal.valueOf(usage.getTotalTokens() * 0.002 / 1000);
        }
    }
}
```

## 4. Claude API集成

```java
@Service
public class ClaudeDocumentGenerator implements LLMClient {

    private final WebClient webClient;

    public ClaudeDocumentGenerator(@Value("${ai.llm.claude.apiKey}") String apiKey) {
        this.webClient = WebClient.builder()
            .baseUrl("https://api.anthropic.com")
            .defaultHeader("x-api-key", apiKey)
            .defaultHeader("anthropic-version", "2023-06-01")
            .build();
    }

    @Override
    public String generateDocument(DocumentGenerationRequest request) {
        ClaudeRequest claudeRequest = ClaudeRequest.builder()
            .model("claude-3-sonnet-20240229")
            .maxTokens(2000)
            .messages(List.of(
                Map.of("role", "user", "content", buildPrompt(request))
            ))
            .build();

        ClaudeResponse response = webClient.post()
            .uri("/v1/messages")
            .bodyValue(claudeRequest)
            .retrieve()
            .bodyToMono(ClaudeResponse.class)
            .block();

        return response.getContent().get(0).getText();
    }
}
```

## 5. Prompt工程最佳实践

### 5.1 类文档Prompt模板

```markdown
# System Prompt
You are a senior software architect and technical writer.
Your task is to generate clear, comprehensive, and professional technical documentation.

# User Prompt Template

## Context
- Programming Language: {{LANGUAGE}}
- Project Type: {{PROJECT_TYPE}}
- Module: {{MODULE_NAME}}

## Source Code
```{{LANGUAGE}}
{{CODE}}
```

## Task
Generate detailed documentation for the class above. Include:

1. **Class Overview** (2-3 sentences)
   - What does this class do?
   - What problem does it solve?

2. **Responsibilities**
   - List key responsibilities
   - Explain core business logic

3. **Public API**
   For each public method, document:
   - Purpose
   - Parameters (type and description)
   - Return value
   - Exceptions
   - Usage example

4. **Design Patterns**
   - Identify any design patterns used
   - Explain why they were chosen

5. **Dependencies**
   - Key dependencies and their purpose
   - External services integration

6. **Thread Safety**
   - Is it thread-safe?
   - Any concurrency considerations?

7. **Performance Considerations**
   - Time/space complexity
   - Scalability notes

8. **Usage Examples**
   Provide 2-3 real-world usage examples

## Output Format
Use Markdown with:
- Clear headings (##, ###)
- Code blocks with language tags
- Tables for parameters/returns
- Bullet lists for key points

## Style Guide
- Be concise but complete
- Use active voice
- Avoid jargon unless necessary
- Include code examples
- Add notes/warnings where appropriate
```

### 5.2 API文档Prompt模板

```markdown
# System Prompt
You are an API documentation expert specializing in REST API design and documentation.

# User Prompt
Generate comprehensive API documentation for this REST controller:

```java
{{CODE}}
```

## Requirements

For each endpoint, document:

### 1. Endpoint Overview
- HTTP Method and Path
- Brief description (1 sentence)
- Authentication required?

### 2. Request
- Path parameters
- Query parameters
- Request headers
- Request body schema (JSON)
- Request example (curl)

### 3. Response
- Success response (200/201)
- Response schema (JSON)
- Response example
- Possible error codes (400/401/404/500)
- Error response format

### 4. Business Logic
- What does this endpoint do?
- Side effects (database changes, events, etc.)

### 5. Examples
- Multiple request examples
- Success response examples
- Error response examples

## Output Format
Use OpenAPI 3.0 style Markdown documentation
```

### 5.3 架构文档Prompt模板

```markdown
# System Prompt
You are a senior system architect with expertise in distributed systems and software architecture.

# User Prompt
Analyze the following codebase structure and generate an architecture document:

## Project Structure
```
{{FILE_TREE}}
```

## Key Files
{{KEY_FILES_SUMMARY}}

## Dependencies
{{DEPENDENCIES}}

## Task
Generate a comprehensive architecture document that includes:

1. **System Overview**
   - What does this system do?
   - Key features
   - Target users

2. **Architecture Diagram**
   - Draw architecture diagram in Mermaid.js format
   - Show key components and their relationships

3. **Technology Stack**
   - Programming languages
   - Frameworks and libraries
   - Databases
   - External services

4. **Module Breakdown**
   For each major module:
   - Purpose
   - Key components
   - Interactions with other modules

5. **Data Flow**
   - How data flows through the system
   - Data flow diagram (Mermaid)

6. **Design Decisions**
   - Key architectural decisions
   - Trade-offs and rationale

7. **Deployment Architecture**
   - How is it deployed?
   - Infrastructure requirements

8. **Scalability and Performance**
   - Scalability strategies
   - Performance considerations

9. **Security**
   - Authentication/Authorization
   - Data protection
   - Security best practices

## Output Format
Professional Markdown document with:
- Executive summary at top
- Table of contents
- Mermaid diagrams
- Code examples where relevant
- Links between sections
```

## 6. Token优化策略

### 6.1 代码压缩

```java
@Component
public class CodeCompressor {

    /**
     * 压缩代码以节省tokens
     */
    public String compress(String code) {
        return code
            // 移除空行
            .replaceAll("(?m)^\\s*$\\n", "")
            // 移除多余空格
            .replaceAll("  +", " ")
            // 移除单行注释(保留JavaDoc)
            .replaceAll("(?<!/)//.*$", "")
            // 保留结构,移除实现细节
            .replaceAll("\\{[^}]{200,}\\}", "{ /* implementation */ }");
    }
}
```

### 6.2 分批处理

```java
/**
 * 大文件分批生成
 */
public class BatchDocumentGenerator {

    public List<AIDocument> generateForLargeFile(File largeFile) {
        ParseResult parseResult = parser.parse(largeFile);

        List<AIDocument> documents = new ArrayList<>();

        // 1. 生成类概览文档
        documents.add(generateClassOverview(parseResult));

        // 2. 分批生成方法文档
        List<List<MethodInfo>> batches = Lists.partition(
            parseResult.getMethods(), 5);

        for (List<MethodInfo> batch : batches) {
            documents.add(generateMethodsBatch(batch));
        }

        return documents;
    }
}
```

## 7. 错误处理和重试

```java
@Component
public class ResilientLLMClient {

    @Retryable(
        value = {LLMServiceException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public String generateWithRetry(DocumentGenerationRequest request) {
        try {
            return llmClient.generateDocument(request);
        } catch (RateLimitException e) {
            // 速率限制,等待后重试
            Thread.sleep(calculateBackoff(e));
            throw e;
        } catch (TimeoutException e) {
            // 超时,记录并重试
            log.warn("LLM request timeout, retrying...");
            throw new LLMServiceException("Timeout", e);
        }
    }

    @Recover
    public String recover(LLMServiceException e, DocumentGenerationRequest request) {
        // 降级策略:使用更简单的模型
        log.warn("Primary LLM failed, falling back to secondary");
        request.setModel("gpt-3.5-turbo");
        return secondaryClient.generateDocument(request);
    }
}
```

## 8. 监控和告警

```java
@Component
public class LLMMetricsCollector {

    private final MeterRegistry meterRegistry;

    public void recordApiCall(String model, long durationMs, boolean success) {
        Timer.builder("llm.api.call")
            .tag("model", model)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordTokenUsage(String model, int tokens) {
        Counter.builder("llm.tokens.used")
            .tag("model", model)
            .register(meterRegistry)
            .increment(tokens);
    }

    public void recordCost(String model, BigDecimal cost) {
        Counter.builder("llm.cost")
            .tag("model", model)
            .register(meterRegistry)
            .increment(cost.doubleValue());
    }
}
```

## 9. 测试策略

```java
@Test
public void testDocumentGeneration() {
    // 使用mock避免真实API调用
    when(llmClient.generateDocument(any()))
        .thenReturn("# Generated Documentation\\n\\nTest content");

    AIDocument doc = generator.generateForFile(testFile);

    assertNotNull(doc);
    assertTrue(doc.getContent().contains("Generated Documentation"));
}

// 集成测试使用本地Ollama
@SpringBootTest
@ActiveProfiles("integration-test")
public class LLMIntegrationTest {

    @Test
    public void testWithLocalLLM() {
        // 使用Ollama进行真实测试
        AIDocument doc = generator.generateForFile(realJavaFile);
        assertNotNull(doc.getContent());
    }
}
```

## 10. 下一步

请继续阅读：
- [API接口设计](./05-api-design.md)
- [实施计划与路线图](./06-implementation-roadmap.md)
