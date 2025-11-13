# OpenDeepWiki Phase 3 - 快速启动指南

> **重要**: 本指南专注于Phase 3的核心实施，OpenAI客户端将先实现基础功能，多模型支持（Gemini、Claude等）将在Phase 4中实现。

## 🎯 Phase 3 实施重点

### 核心原则
1. **先完成基础功能**：使用OpenAI API作为默认实现
2. **保持扩展性**：设计接口时考虑未来多模型支持
3. **快速交付**：42小时内完成所有核心功能
4. **稳定优先**：确保每个功能模块稳定可用

---

## 🚀 第一步：环境准备（立即开始）

### 1.1 添加Maven依赖
在 `koalawiki-infra/pom.xml` 中添加：

```xml
<!-- OpenAI Java客户端（基础实现） -->
<dependency>
    <groupId>com.theokanning.openai-gpt3-java</groupId>
    <artifactId>service</artifactId>
    <version>0.18.2</version>
</dependency>

<!-- HTTP客户端（备用，用于自定义实现） -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.11.0</version>
</dependency>

<!-- JSON处理 -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.13.5</version>
</dependency>
```

### 1.2 配置文件设置
创建 `application-phase3.yml`：

```yaml
# Phase 3 基础配置（先使用OpenAI）
koalawiki:
  ai:
    # 基础配置 - Phase 3使用
    provider: openai  # 固定为openai，Phase 4再扩展
    api-key: ${AI_API_KEY}
    endpoint: ${AI_ENDPOINT:https://api.openai.com/v1}
    model: ${AI_MODEL:gpt-3.5-turbo}  # 使用3.5节省成本

    # 通用配置
    max-tokens: 2048  # 降低token限制以加快响应
    temperature: 0.7
    timeout: 30000    # 30秒超时

    # 性能优化
    cache:
      enabled: true
      ttl: 3600  # 缓存1小时

    # 限流配置
    rate-limit:
      requests-per-minute: 20
      tokens-per-minute: 40000
```

---

## 📦 第二步：实现顺序（严格按照优先级）

### Week 1: 基础AI能力（必须完成）

#### Day 1-2: 基础客户端
```java
// Step 1: 创建简单的AI客户端接口
package ai.opendw.koalawiki.infra.ai;

public interface IAIClient {
    // Phase 3: 只实现基础方法
    String complete(String prompt);
    String complete(String prompt, Map<String, Object> options);
}

// Step 2: OpenAI实现（基础版）
@Component
public class OpenAIClientImpl implements IAIClient {
    // 使用现成的OpenAI库，快速实现
}
```

#### Day 3-4: 核心AI功能
优先实现以下3个核心功能：
1. **README生成** - 最重要，用户感知最明显
2. **目录优化** - 第二重要，处理大型仓库必需
3. **文档摘要** - 第三重要，提升用户体验

#### Day 5: API接口
只暴露已实现的功能：
```java
@RestController
@RequestMapping("/api/ai")
public class AIController {
    // Phase 3: 只暴露3个核心接口
    @PostMapping("/readme/generate")
    @PostMapping("/catalog/optimize")
    @PostMapping("/document/summarize")
}
```

### Week 2: 文档服务和统计（核心功能）

#### Day 6-7: 文档目录服务
- 实现基础CRUD
- 简单的排序（字母序）
- 基础搜索（关键词匹配）

#### Day 8-10: 访问日志和统计
- 异步日志收集（不影响性能）
- 每日统计生成（定时任务）
- 简单的统计API

### Week 3: 集成和优化

#### Day 11-12: 集成测试
- 功能测试
- 性能基准测试
- Bug修复

---

## 🔧 第三步：简化实现策略

### 3.1 AI功能简化
```java
// 使用模板方法模式，简化实现
public abstract class BaseAIProcessor {
    protected String buildPrompt(String template, Map<String, Object> params) {
        // 简单的模板替换
        return template.replace("{content}", params.get("content").toString());
    }

    protected String callAI(String prompt) {
        // 直接调用OpenAI，不做复杂处理
        return aiClient.complete(prompt);
    }
}
```

### 3.2 使用内存缓存
```java
// 简单的内存缓存，避免复杂的Redis配置
@Component
public class SimpleAICache {
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public Optional<String> get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return Optional.of(entry.value);
        }
        return Optional.empty();
    }
}
```

### 3.3 同步处理优先
```java
// Phase 3: 先用同步处理，确保稳定
@Service
public class DocumentService {
    public void processDocument(String docId) {
        // 同步处理，简单直接
        String content = readDocument(docId);
        String summary = aiService.summarize(content);
        saveDocumentSummary(docId, summary);
    }
}
```

---

## 📝 第四步：测试数据和验证

### 4.1 准备测试仓库
```bash
# 创建3个测试仓库用于验证
1. 小型仓库（<10个文件）- 测试基础功能
2. 中型仓库（50-100个文件）- 测试正常场景
3. 大型仓库（>500个文件）- 测试性能和优化
```

### 4.2 功能验证清单
```markdown
Phase 3 核心功能验证：
□ README生成 - 能否为无README的仓库生成
□ 目录优化 - 能否将1000个文件优化到100个以内
□ 文档摘要 - 能否生成100字左右的摘要
□ 访问日志 - 能否记录所有API调用
□ 统计生成 - 能否生成每日PV/UV统计
```

---

## 🎯 第五步：Phase 3 完成标准

### 必须完成（P0）
- [ ] OpenAI基础集成可用
- [ ] README自动生成功能
- [ ] 目录优化功能（规则+AI）
- [ ] 访问日志收集
- [ ] 基础统计功能

### 应该完成（P1）
- [ ] 文档摘要功能
- [ ] 文档目录服务
- [ ] 统计API接口
- [ ] 性能优化

### 可选完成（P2）
- [ ] 问答服务
- [ ] 同步统计
- [ ] 高级搜索

---

## ⚡ 快速开发技巧

### 1. 使用Spring Boot Starter
```xml
<!-- 快速集成，减少配置 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

### 2. 利用现有代码
```java
// 复用Phase 2已实现的处理器框架
@Component
public class AIReadmeProcessor extends AbstractDocumentProcessor {
    // 只需要实现process方法
}
```

### 3. 简化错误处理
```java
// 统一异常处理，减少重复代码
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AIException.class)
    public Result<String> handleAIError(AIException e) {
        // 统一处理AI相关错误
        return Result.error("AI服务暂时不可用，请稍后重试");
    }
}
```

---

## 📊 进度追踪

### 每日检查清单
```markdown
每日站会问题：
1. 昨天完成了什么？
2. 今天计划做什么？
3. 有什么阻塞问题？

每日代码提交：
- [ ] 功能代码提交
- [ ] 单元测试提交
- [ ] 文档更新
```

### 周进度里程碑
| 周次 | 目标 | 验收标准 |
|-----|------|---------|
| Week 1 | AI基础集成 | 能调用OpenAI生成文本 |
| Week 2 | 核心功能 | 3个AI功能+统计功能可用 |
| Week 3 | 集成优化 | 所有功能联调通过 |

---

## 🔍 常见问题解决

### Q1: OpenAI API调用失败
```java
// 添加重试机制
@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
public String callOpenAI(String prompt) {
    // API调用逻辑
}
```

### Q2: 响应时间太慢
```java
// 1. 减少token数量
// 2. 使用更快的模型（gpt-3.5-turbo）
// 3. 添加缓存
```

### Q3: 成本控制
```java
// 限制每日调用次数
@Component
public class UsageTracker {
    private AtomicInteger dailyCount = new AtomicInteger(0);
    private static final int DAILY_LIMIT = 1000;
}
```

---

## 🚦 下一步行动

### 立即开始（Now）
1. 配置OpenAI API密钥
2. 添加Maven依赖
3. 创建基础客户端类

### 今日完成（Today）
1. 实现IAIClient接口
2. 完成OpenAI基础调用
3. 测试API连通性

### 本周完成（This Week）
1. README生成功能
2. 目录优化功能
3. 基础API接口

---

## 📌 重要提醒

1. **不要过度设计** - Phase 3只实现OpenAI，多模型支持留给Phase 4
2. **快速迭代** - 先让功能跑起来，再优化
3. **控制范围** - 只做计划内的20个任务
4. **及时沟通** - 遇到问题立即反馈

---

**记住：Phase 3的目标是在2-3周内交付可用的AI增强功能，不是构建完美的多模型AI平台。保持专注，快速交付！**