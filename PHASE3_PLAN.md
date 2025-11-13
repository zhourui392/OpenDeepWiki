# OpenDeepWiki Java版本 - Phase 3 详细任务计划

> **文档版本**: 1.0
> **创建时间**: 2025-11-13
> **JDK版本**: 1.8
> **Spring Boot版本**: 2.7.18
> **预计工期**: 2-3周
> **预计工时**: 42小时

---

## 📋 目录

1. [Phase 3 概述](#phase-3-概述)
2. [任务分解清单](#任务分解清单)
3. [OpenAI集成模块](#1-openai集成模块)
4. [文档目录服务模块](#2-文档目录服务模块)
5. [访问日志和统计模块](#3-访问日志和统计模块)
6. [同步统计计算模块](#4-同步统计计算模块)
7. [实施计划](#实施计划)
8. [技术风险和缓解措施](#技术风险和缓解措施)
9. [验收标准](#验收标准)

---

## Phase 3 概述

### 目标定位
Phase 3 是"增强功能"阶段，主要目标是：
- 集成AI能力，提升文档智能化水平
- 完善文档管理服务
- 建立系统监控和统计机制
- 优化用户体验

### 前置条件
- ✅ Phase 1 核心基础已完成（Git集成、仓库同步、文档处理流水线）
- ✅ Phase 2 核心功能已完成（文档处理器、服务层、后台任务）
- ✅ 基础架构稳定运行
- ✅ 数据模型和仓储层完备

### 预期成果
1. **AI赋能**：通过OpenAI集成实现智能文档生成和优化
2. **完善服务**：提供完整的文档目录管理服务
3. **数据洞察**：通过访问日志和统计提供使用分析
4. **质量提升**：优化同步机制，提供详细的同步统计

---

## 任务分解清单

### 模块划分
| 模块 | 任务数 | 预估工时 | 优先级 | 依赖关系 |
|------|--------|----------|--------|----------|
| OpenAI集成 | 8 | 20小时 | P0 | 无 |
| 文档目录服务 | 4 | 6小时 | P1 | 依赖OpenAI |
| 访问日志和统计 | 6 | 12小时 | P1 | 无 |
| 同步统计计算 | 2 | 4小时 | P2 | 无 |
| **总计** | **20** | **42小时** | - | - |

---

## 1. OpenAI集成模块

### 任务 1.1: 添加OpenAI依赖和配置
**优先级**: P0
**预估工时**: 2小时
**负责人**: Backend Developer

#### 详细步骤
1. 在 `koalawiki-infra/pom.xml` 添加依赖：
```xml
<dependency>
    <groupId>com.theokanning.openai-gpt3-java</groupId>
    <artifactId>service</artifactId>
    <version>0.18.2</version>
</dependency>

<!-- 备选：如果上述库有兼容性问题，使用以下替代方案 -->
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-ai-openai</artifactId>
    <version>1.0.0-beta.5</version>
</dependency>
```

2. 创建配置文件：
```yaml
# application.yml
koalawiki:
  openai:
    api-key: ${OPENAI_API_KEY}
    endpoint: ${OPENAI_ENDPOINT:https://api.openai.com/v1}
    chat-model: ${CHAT_MODEL:gpt-4}
    analysis-model: ${ANALYSIS_MODEL:gpt-4}
    max-tokens: ${MAX_TOKENS:4096}
    temperature: ${TEMPERATURE:0.7}
    timeout: ${TIMEOUT:60000}
    retry:
      max-attempts: 3
      backoff-delay: 1000
    proxy:
      enabled: false
      host: ${PROXY_HOST}
      port: ${PROXY_PORT}
```

#### 交付物
- ✅ Maven依赖配置
- ✅ application.yml配置
- ✅ application-dev.yml示例

---

### 任务 1.2: 创建OpenAI客户端封装
**优先级**: P0
**预估工时**: 4小时
**负责人**: Backend Developer

#### 需要创建的类
```java
// koalawiki-infra/src/main/java/ai/opendw/koalawiki/infra/ai/openai/

1. OpenAIConfig.java - 配置属性类
@ConfigurationProperties(prefix = "koalawiki.openai")
public class OpenAIConfig {
    private String apiKey;
    private String endpoint;
    private String chatModel;
    private String analysisModel;
    private Integer maxTokens;
    private Double temperature;
    private Integer timeout;
    private RetryConfig retry;
    private ProxyConfig proxy;
}

2. OpenAIClient.java - 客户端封装
@Component
public class OpenAIClient {
    // 支持同步和异步调用
    CompletableFuture<ChatResponse> chatAsync(ChatRequest request);
    ChatResponse chat(ChatRequest request);
    Stream<ChatResponse> streamChat(ChatRequest request);
}

3. ChatRequest.java - 请求模型
public class ChatRequest {
    private String model;
    private List<Message> messages;
    private Integer maxTokens;
    private Double temperature;
    private List<Function> functions; // Function Calling支持
}

4. ChatResponse.java - 响应模型
public class ChatResponse {
    private String id;
    private List<Choice> choices;
    private Usage usage;
}
```

#### 关键功能
- ✅ 支持Chat Completions API
- ✅ 支持流式响应（SSE）
- ✅ 支持Function Calling
- ✅ 自动重试机制（指数退避）
- ✅ 请求限流（Rate Limiting）
- ✅ Token计算和成本统计
- ✅ 代理支持

#### 交付物
- ✅ OpenAI客户端实现
- ✅ 单元测试
- ✅ 集成测试（Mock）

---

### 任务 1.3: 实现AI服务层
**优先级**: P0
**预估工时**: 3小时
**负责人**: Backend Developer

#### 需要创建的类
```java
// koalawiki-app/src/main/java/ai/opendw/koalawiki/app/ai/

1. IAIService.java - AI服务接口
public interface IAIService {
    String generateReadme(RepositoryContext context);
    CatalogOptimizationResult optimizeCatalog(CatalogContext context);
    String summarizeDocument(String content);
    String answerQuestion(String question, DocumentContext context);
    List<String> generateTags(String content);
}

2. AIServiceImpl.java - AI服务实现
@Service
public class AIServiceImpl implements IAIService {
    @Autowired
    private OpenAIClient openAIClient;

    @Autowired
    private PromptTemplateService promptService;
}

3. PromptTemplateService.java - 提示词模板管理
@Component
public class PromptTemplateService {
    String getReadmePrompt(RepositoryContext context);
    String getCatalogPrompt(CatalogContext context);
    String getSummaryPrompt(String content);
    String getQAPrompt(String question, String context);
}
```

#### 交付物
- ✅ AI服务接口和实现
- ✅ 提示词模板管理
- ✅ 服务测试用例

---

### 任务 1.4: 实现README生成器
**优先级**: P0
**预估工时**: 3小时
**负责人**: Backend Developer

#### 需要创建的类
```java
// koalawiki-app/src/main/java/ai/opendw/koalawiki/app/ai/generators/

1. ReadmeGenerator.java
@Component
public class ReadmeGenerator {
    @Autowired
    private IAIService aiService;

    public String generateReadme(Warehouse warehouse) {
        // 1. 分析仓库结构
        // 2. 提取技术栈
        // 3. 识别项目类型
        // 4. 生成README内容
        // 5. 格式化Markdown
    }
}

2. ReadmeTemplate.java - README模板
public class ReadmeTemplate {
    private String projectName;
    private String description;
    private List<String> features;
    private String techStack;
    private String installation;
    private String usage;
    private String license;
}
```

#### 功能要求
- ✅ 自动识别项目类型（Web、Library、Tool等）
- ✅ 提取技术栈信息
- ✅ 生成特性列表
- ✅ 创建安装和使用说明
- ✅ 支持多语言README

#### 交付物
- ✅ README生成器实现
- ✅ 模板系统
- ✅ 测试用例

---

### 任务 1.5: 实现目录优化器
**优先级**: P0
**预估工时**: 3小时
**负责人**: Backend Developer

#### 需要创建的类
```java
// koalawiki-app/src/main/java/ai/opendw/koalawiki/app/ai/optimizers/

1. CatalogOptimizer.java
@Component
public class CatalogOptimizer {
    @Autowired
    private IAIService aiService;

    public OptimizedCatalog optimize(RawCatalog rawCatalog) {
        // 1. 分析目录结构
        // 2. 识别核心文档
        // 3. 过滤无关文件
        // 4. 生成优化建议
        // 5. 返回优化后的目录
    }
}

2. OptimizationRule.java - 优化规则
public interface OptimizationRule {
    boolean shouldInclude(FileNode node);
    int getPriority(FileNode node);
}

3. OptimizationContext.java - 优化上下文
public class OptimizationContext {
    private int maxFiles;
    private List<String> includePatterns;
    private List<String> excludePatterns;
    private String language;
}
```

#### 功能要求
- ✅ 智能识别重要文件
- ✅ 自动过滤构建产物
- ✅ 保留文档结构完整性
- ✅ 支持自定义规则

#### 交付物
- ✅ 目录优化器实现
- ✅ 优化规则引擎
- ✅ 性能测试

---

### 任务 1.6: 实现文档总结器
**优先级**: P1
**预估工时**: 2小时
**负责人**: Backend Developer

#### 需要创建的类
```java
// koalawiki-app/src/main/java/ai/opendw/koalawiki/app/ai/summarizers/

1. DocumentSummarizer.java
@Component
public class DocumentSummarizer {
    public DocumentSummary summarize(Document document) {
        // 1. 提取关键信息
        // 2. 生成摘要
        // 3. 提取关键词
        // 4. 识别主题
    }
}

2. DocumentSummary.java
public class DocumentSummary {
    private String title;
    private String summary;
    private List<String> keywords;
    private List<String> topics;
    private Integer readingTime; // 预估阅读时间（分钟）
}
```

#### 交付物
- ✅ 文档总结器实现
- ✅ 多语言支持
- ✅ 测试用例

---

### 任务 1.7: 实现问答服务
**优先级**: P1
**预估工时**: 3小时
**负责人**: Backend Developer

#### 需要创建的类
```java
// koalawiki-app/src/main/java/ai/opendw/koalawiki/app/ai/qa/

1. QAService.java
@Service
public class QAService {
    public Answer askQuestion(Question question) {
        // 1. 理解问题
        // 2. 检索相关文档
        // 3. 生成答案
        // 4. 添加引用
    }
}

2. Question.java
public class Question {
    private String text;
    private String warehouseId;
    private String language;
    private List<String> documentIds; // 限定文档范围
}

3. Answer.java
public class Answer {
    private String text;
    private Double confidence;
    private List<Reference> references;
    private List<String> relatedQuestions;
}
```

#### 功能要求
- ✅ 上下文理解
- ✅ 多轮对话支持
- ✅ 引用来源标注
- ✅ 相关问题推荐

#### 交付物
- ✅ 问答服务实现
- ✅ API接口
- ✅ 测试套件

---

### 任务 1.8: 创建AI功能REST API
**优先级**: P1
**预估工时**: 2小时
**负责人**: Backend Developer

#### 需要创建的类
```java
// koalawiki-web/src/main/java/ai/opendw/koalawiki/web/controller/

1. AIController.java
@RestController
@RequestMapping("/api/ai")
public class AIController {
    @PostMapping("/readme/generate")
    public Result<String> generateReadme(@RequestBody GenerateReadmeRequest request);

    @PostMapping("/catalog/optimize")
    public Result<OptimizedCatalog> optimizeCatalog(@RequestBody OptimizeCatalogRequest request);

    @PostMapping("/document/summarize")
    public Result<DocumentSummary> summarizeDocument(@RequestBody SummarizeRequest request);

    @PostMapping("/qa/ask")
    public Result<Answer> askQuestion(@RequestBody QuestionRequest request);
}
```

#### API文档
```yaml
/api/ai/readme/generate:
  post:
    summary: 生成README文档
    parameters:
      - warehouseId: 仓库ID
      - language: 语言（可选）
      - template: 模板类型（可选）

/api/ai/catalog/optimize:
  post:
    summary: 优化文档目录
    parameters:
      - catalogData: 原始目录数据
      - maxFiles: 最大文件数
      - rules: 优化规则

/api/ai/document/summarize:
  post:
    summary: 总结文档
    parameters:
      - documentId: 文档ID
      - maxLength: 摘要最大长度

/api/ai/qa/ask:
  post:
    summary: 问答接口
    parameters:
      - question: 问题文本
      - warehouseId: 仓库ID
      - sessionId: 会话ID（可选）
```

#### 交付物
- ✅ REST API控制器
- ✅ DTO定义
- ✅ Swagger文档
- ✅ Postman集合

---

## 2. 文档目录服务模块

### 任务 2.1: 实现DocumentCatalogService
**优先级**: P1
**预估工时**: 2小时
**负责人**: Backend Developer

#### 需要创建的类
```java
// koalawiki-app/src/main/java/ai/opendw/koalawiki/app/service/

1. IDocumentCatalogService.java
public interface IDocumentCatalogService {
    DocumentCatalog createCatalog(CreateCatalogRequest request);
    DocumentCatalog updateCatalog(String catalogId, UpdateCatalogRequest request);
    void deleteCatalog(String catalogId);
    DocumentCatalog getCatalog(String catalogId);
    Page<DocumentCatalog> listCatalogs(Pageable pageable);
    List<DocumentCatalogI18n> getCatalogI18n(String catalogId);
}

2. DocumentCatalogServiceImpl.java
@Service
@Transactional
public class DocumentCatalogServiceImpl implements IDocumentCatalogService {
    @Autowired
    private DocumentCatalogRepository catalogRepository;

    @Autowired
    private IAIService aiService;
}
```

#### 功能要求
- ✅ CRUD操作
- ✅ 多语言支持
- ✅ 版本管理
- ✅ 批量操作

#### 交付物
- ✅ 服务接口和实现
- ✅ 单元测试
- ✅ 集成测试

---

### 任务 2.2: 实现目录排序和过滤
**优先级**: P1
**预估工时**: 2小时
**负责人**: Backend Developer

#### 需要创建的类
```java
// koalawiki-app/src/main/java/ai/opendw/koalawiki/app/service/catalog/

1. CatalogSortStrategy.java
public interface CatalogSortStrategy {
    List<CatalogNode> sort(List<CatalogNode> nodes);
}

2. AlphabeticalSortStrategy.java
public class AlphabeticalSortStrategy implements CatalogSortStrategy {
    // 按字母顺序排序
}

3. PrioritySortStrategy.java
public class PrioritySortStrategy implements CatalogSortStrategy {
    // 按优先级排序（README优先、文档优先等）
}

4. CatalogFilter.java
public class CatalogFilter {
    List<CatalogNode> filter(List<CatalogNode> nodes, FilterCriteria criteria);
}

5. FilterCriteria.java
public class FilterCriteria {
    private List<String> includeTypes;
    private List<String> excludeTypes;
    private String searchKeyword;
    private Integer maxDepth;
}
```

#### 功能要求
- ✅ 多种排序策略
- ✅ 灵活的过滤条件
- ✅ 性能优化（大目录）
- ✅ 缓存支持

#### 交付物
- ✅ 排序策略实现
- ✅ 过滤器实现
- ✅ 性能测试

---

### 任务 2.3: 实现目录搜索功能
**优先级**: P1
**预估工时**: 1小时
**负责人**: Backend Developer

#### 需要创建的类
```java
// koalawiki-app/src/main/java/ai/opendw/koalawiki/app/service/catalog/

1. CatalogSearchService.java
@Service
public class CatalogSearchService {
    public SearchResult search(SearchRequest request) {
        // 1. 分词处理
        // 2. 模糊匹配
        // 3. 排序结果
        // 4. 高亮关键词
    }
}

2. SearchRequest.java
public class SearchRequest {
    private String keyword;
    private String warehouseId;
    private SearchScope scope; // TITLE, CONTENT, ALL
    private Integer maxResults;
}

3. SearchResult.java
public class SearchResult {
    private List<SearchItem> items;
    private Integer totalCount;
    private String suggestion; // 搜索建议
}
```

#### 交付物
- ✅ 搜索服务实现
- ✅ 测试用例

---

### 任务 2.4: 创建目录管理REST API
**优先级**: P1
**预估工时**: 1小时
**负责人**: Backend Developer

#### 需要创建的类
```java
// koalawiki-web/src/main/java/ai/opendw/koalawiki/web/controller/

1. DocumentCatalogController.java
@RestController
@RequestMapping("/api/catalog")
public class DocumentCatalogController {
    @GetMapping("/{warehouseId}")
    public Result<DocumentCatalog> getCatalog(@PathVariable String warehouseId);

    @PostMapping("/{warehouseId}/refresh")
    public Result<Void> refreshCatalog(@PathVariable String warehouseId);

    @GetMapping("/{warehouseId}/search")
    public Result<SearchResult> search(@PathVariable String warehouseId,
                                      @RequestParam String keyword);

    @PostMapping("/{warehouseId}/sort")
    public Result<DocumentCatalog> sort(@PathVariable String warehouseId,
                                       @RequestBody SortRequest request);
}
```

#### 交付物
- ✅ REST API控制器
- ✅ API文档

---

## 3. 访问日志和统计模块

### 任务 3.1: 创建访问日志实体和仓储
**优先级**: P1
**预估工时**: 2小时
**负责人**: Backend Developer

#### 需要创建的类
```java
// koalawiki-domain/src/main/java/ai/opendw/koalawiki/domain/log/

1. AccessLog.java
@Entity
public class AccessLog extends BaseEntity {
    private String userId;
    private String warehouseId;
    private String documentId;
    private String action; // VIEW, DOWNLOAD, SEARCH
    private String ipAddress;
    private String userAgent;
    private Integer responseTime; // 毫秒
    private Integer statusCode;
    private Date accessTime;
}

2. DailyStatistics.java
@Entity
public class DailyStatistics {
    private Date statisticsDate;
    private String warehouseId;
    private Long viewCount;
    private Long uniqueUserCount;
    private Long documentCount;
    private Double avgResponseTime;
    private Map<String, Long> actionCounts;
}

// koalawiki-infra/src/main/java/ai/opendw/koalawiki/infra/repository/

3. AccessLogRepository.java
public interface AccessLogRepository extends JpaRepository<AccessLog, String> {
    Page<AccessLog> findByWarehouseId(String warehouseId, Pageable pageable);
    List<AccessLog> findByAccessTimeBetween(Date start, Date end);
}

4. DailyStatisticsRepository.java
public interface DailyStatisticsRepository extends JpaRepository<DailyStatistics, String> {
    List<DailyStatistics> findByStatisticsDateBetween(Date start, Date end);
}
```

#### 交付物
- ✅ 实体类定义
- ✅ JPA仓储接口
- ✅ 数据库索引优化

---

### 任务 3.2: 实现访问日志收集器
**优先级**: P1
**预估工时**: 2小时
**负责人**: Backend Developer

#### 需要创建的类
```java
// koalawiki-core/src/main/java/ai/opendw/koalawiki/core/log/

1. AccessLogCollector.java
@Component
public class AccessLogCollector {
    private final BlockingQueue<AccessLog> logQueue = new LinkedBlockingQueue<>(10000);

    public void collect(AccessLog log) {
        // 异步收集，避免影响主流程性能
        logQueue.offer(log);
    }
}

2. AccessLogInterceptor.java
@Component
public class AccessLogInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler) {
        // 记录请求开始时间
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        // 收集访问日志
    }
}

3. AccessLogFilter.java
@Component
@Order(1)
public class AccessLogFilter extends OncePerRequestFilter {
    // 收集更底层的访问信息
}
```

#### 功能要求
- ✅ 异步日志收集
- ✅ 批量写入优化
- ✅ 性能影响最小化
- ✅ 敏感信息过滤

#### 交付物
- ✅ 日志收集器实现
- ✅ 拦截器/过滤器
- ✅ 性能测试报告

---

### 任务 3.3: 实现访问日志处理任务
**优先级**: P1
**预估工时**: 3小时
**负责人**: Backend Developer

#### 需要创建的类
```java
// koalawiki-app/src/main/java/ai/opendw/koalawiki/app/task/

1. AccessLogTask.java
@Component
@EnableScheduling
public class AccessLogTask {
    @Scheduled(fixedDelay = 30000) // 每30秒执行一次
    public void processLogs() {
        // 1. 从队列中批量获取日志
        // 2. 批量保存到数据库
        // 3. 更新实时统计
    }

    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点
    public void cleanupOldLogs() {
        // 清理过期日志
    }
}

2. AccessLogProcessor.java
@Service
public class AccessLogProcessor {
    public void processBatch(List<AccessLog> logs) {
        // 1. 数据清洗
        // 2. 异常检测
        // 3. 批量保存
    }
}
```

#### 功能要求
- ✅ 批量处理（提高效率）
- ✅ 定期清理（控制存储）
- ✅ 异常处理
- ✅ 监控告警

#### 交付物
- ✅ 定时任务实现
- ✅ 处理器实现
- ✅ 配置文档

---

### 任务 3.4: 实现统计数据生成任务
**优先级**: P1
**预估工时**: 3小时
**负责人**: Backend Developer

#### 需要创建的类
```java
// koalawiki-app/src/main/java/ai/opendw/koalawiki/app/task/

1. StatisticsTask.java
@Component
public class StatisticsTask {
    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点
    public void generateDailyStatistics() {
        // 1. 计算前一天的统计数据
        // 2. 生成各维度报表
        // 3. 发送统计报告（可选）
    }

    @Scheduled(cron = "0 0 1 ? * MON") // 每周一凌晨1点
    public void generateWeeklyReport() {
        // 生成周报
    }
}

2. StatisticsCalculator.java
@Service
public class StatisticsCalculator {
    public DailyStatistics calculate(Date date) {
        // 1. 查询访问日志
        // 2. 计算各项指标
        // 3. 生成统计结果
    }
}

3. StatisticsAggregator.java
@Component
public class StatisticsAggregator {
    public Map<String, Object> aggregate(List<DailyStatistics> statistics) {
        // 聚合统计数据
    }
}
```

#### 统计指标
- ✅ PV（页面浏览量）
- ✅ UV（独立访客数）
- ✅ 平均响应时间
- ✅ 热门文档排行
- ✅ 搜索关键词统计
- ✅ 错误率统计

#### 交付物
- ✅ 统计任务实现
- ✅ 计算器和聚合器
- ✅ 测试数据集

---

### 任务 3.5: 实现统计数据查询服务
**优先级**: P1
**预估工时**: 1小时
**负责人**: Backend Developer

#### 需要创建的类
```java
// koalawiki-app/src/main/java/ai/opendw/koalawiki/app/service/

1. IStatisticsService.java
public interface IStatisticsService {
    DailyStatistics getDailyStatistics(String warehouseId, Date date);
    List<DailyStatistics> getRangeStatistics(String warehouseId, Date start, Date end);
    TrendData getTrend(String warehouseId, TrendPeriod period);
    TopData getTopDocuments(String warehouseId, Integer limit);
}

2. StatisticsServiceImpl.java
@Service
public class StatisticsServiceImpl implements IStatisticsService {
    // 实现统计查询逻辑
}
```

#### 交付物
- ✅ 统计服务接口和实现
- ✅ 缓存优化
- ✅ 测试用例

---

### 任务 3.6: 创建统计数据REST API
**优先级**: P1
**预估工时**: 1小时
**负责人**: Backend Developer

#### 需要创建的类
```java
// koalawiki-web/src/main/java/ai/opendw/koalawiki/web/controller/

1. StatisticsController.java
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {
    @GetMapping("/daily/{warehouseId}")
    public Result<DailyStatistics> getDailyStatistics(@PathVariable String warehouseId,
                                                      @RequestParam String date);

    @GetMapping("/trend/{warehouseId}")
    public Result<TrendData> getTrend(@PathVariable String warehouseId,
                                      @RequestParam TrendPeriod period);

    @GetMapping("/top/{warehouseId}/documents")
    public Result<List<TopDocument>> getTopDocuments(@PathVariable String warehouseId,
                                                    @RequestParam(defaultValue = "10") Integer limit);

    @GetMapping("/summary/{warehouseId}")
    public Result<StatisticsSummary> getSummary(@PathVariable String warehouseId);
}
```

#### API响应示例
```json
{
  "code": 200,
  "data": {
    "date": "2025-11-13",
    "pv": 10234,
    "uv": 892,
    "avgResponseTime": 235.5,
    "topDocuments": [
      {"id": "doc1", "title": "README", "views": 523},
      {"id": "doc2", "title": "API Guide", "views": 412}
    ],
    "trend": {
      "labels": ["11-07", "11-08", "11-09", "11-10", "11-11", "11-12", "11-13"],
      "pv": [8234, 9123, 9567, 8900, 9234, 9876, 10234],
      "uv": [723, 812, 856, 789, 823, 867, 892]
    }
  }
}
```

#### 交付物
- ✅ REST API控制器
- ✅ 响应DTO定义
- ✅ API文档

---

## 4. 同步统计计算模块

### 任务 4.1: 实现同步统计计算服务
**优先级**: P2
**预估工时**: 2小时
**负责人**: Backend Developer

#### 需要创建的类
```java
// koalawiki-core/src/main/java/ai/opendw/koalawiki/core/service/sync/

1. SyncStatisticsCalculator.java
@Component
public class SyncStatisticsCalculator {
    public SyncStatistics calculate(WarehouseSyncRecord record) {
        // 1. 计算文件变更统计
        // 2. 计算同步耗时
        // 3. 分析失败原因
        // 4. 生成优化建议
    }
}

2. SyncStatistics.java
public class SyncStatistics {
    private Integer totalFiles;
    private Integer addedFiles;
    private Integer modifiedFiles;
    private Integer deletedFiles;
    private Long syncDuration; // 毫秒
    private Double successRate;
    private List<FileChange> changes;
    private List<String> errors;
    private List<String> suggestions;
}

3. FileChange.java
public class FileChange {
    private String path;
    private ChangeType type; // ADD, MODIFY, DELETE
    private Long sizeDelta; // 文件大小变化
    private String oldVersion;
    private String newVersion;
}
```

#### 功能要求
- ✅ 详细的变更统计
- ✅ 性能分析
- ✅ 错误诊断
- ✅ 优化建议

#### 交付物
- ✅ 统计计算器实现
- ✅ 数据模型定义
- ✅ 单元测试

---

### 任务 4.2: 集成同步统计到同步服务
**优先级**: P2
**预估工时**: 2小时
**负责人**: Backend Developer

#### 修改现有类
```java
// 修改 WarehouseSyncExecutorImpl.java
@Component
public class WarehouseSyncExecutorImpl implements IWarehouseSyncExecutor {
    @Autowired
    private SyncStatisticsCalculator statisticsCalculator;

    @Override
    public void executeSync(String warehouseId) {
        // ... 现有同步逻辑 ...

        // 新增：计算同步统计
        SyncStatistics statistics = statisticsCalculator.calculate(syncRecord);
        syncRecord.setStatistics(statistics);

        // 新增：发布同步完成事件
        eventPublisher.publishEvent(new SyncCompletedEvent(warehouseId, statistics));
    }
}

// 新增事件类
public class SyncCompletedEvent extends ApplicationEvent {
    private String warehouseId;
    private SyncStatistics statistics;
}

// 新增事件监听器
@Component
public class SyncEventListener {
    @EventListener
    public void handleSyncCompleted(SyncCompletedEvent event) {
        // 1. 发送通知
        // 2. 更新缓存
        // 3. 触发后续流程
    }
}
```

#### 交付物
- ✅ 同步服务增强
- ✅ 事件机制
- ✅ 集成测试

---

## 实施计划

### 第一周（Sprint 1）：OpenAI集成
**时间**: 2025-11-14 ~ 2025-11-20
**目标**: 完成AI能力集成，实现智能文档处理

#### 日程安排
| 日期 | 任务 | 负责人 | 预估工时 |
|------|------|--------|----------|
| 11-14 | 任务1.1-1.2：OpenAI依赖和客户端 | Dev A | 6h |
| 11-15 | 任务1.3-1.4：AI服务层和README生成 | Dev A | 6h |
| 11-16 | 任务1.5-1.6：目录优化和文档总结 | Dev A | 5h |
| 11-18 | 任务1.7：问答服务 | Dev B | 3h |
| 11-19 | 任务1.8：AI功能API | Dev B | 2h |
| 11-20 | 集成测试和修复 | Team | 4h |

**交付物**：
- ✅ OpenAI集成完成
- ✅ AI功能可用
- ✅ API文档完整

### 第二周（Sprint 2）：文档和统计服务
**时间**: 2025-11-21 ~ 2025-11-27
**目标**: 完善文档管理和统计分析能力

#### 日程安排
| 日期 | 任务 | 负责人 | 预估工时 |
|------|------|--------|----------|
| 11-21 | 任务2.1-2.2：文档目录服务 | Dev A | 4h |
| 11-22 | 任务2.3-2.4：搜索和API | Dev A | 2h |
| 11-23 | 任务3.1-3.2：访问日志实体和收集 | Dev B | 4h |
| 11-25 | 任务3.3-3.4：日志处理和统计生成 | Dev B | 6h |
| 11-26 | 任务3.5-3.6：统计查询和API | Dev C | 2h |
| 11-27 | 任务4.1-4.2：同步统计 | Dev C | 4h |

**交付物**：
- ✅ 文档服务完善
- ✅ 统计功能上线
- ✅ 监控体系建立

### 第三周（Sprint 3）：集成测试和优化
**时间**: 2025-11-28 ~ 2025-12-04
**目标**: 系统集成测试，性能优化，文档完善

#### 主要工作
1. **集成测试**（2天）
   - 端到端测试
   - 性能测试
   - 压力测试

2. **性能优化**（2天）
   - 缓存优化
   - 查询优化
   - 并发优化

3. **文档完善**（1天）
   - API文档
   - 部署文档
   - 用户手册

---

## 技术风险和缓解措施

### 风险1：OpenAI API限流
**风险等级**: 高
**影响**: AI功能不可用
**缓解措施**:
- 实现请求队列和限流
- 添加本地缓存机制
- 支持降级到规则引擎
- 准备备用AI服务（Azure OpenAI）

### 风险2：大数据量性能问题
**风险等级**: 中
**影响**: 系统响应慢
**缓解措施**:
- 实现分页和懒加载
- 添加多级缓存
- 异步处理长任务
- 数据库索引优化

### 风险3：JDK 1.8兼容性
**风险等级**: 低
**影响**: 部分库不兼容
**缓解措施**:
- 严格版本控制
- 充分的兼容性测试
- 准备替代方案

### 风险4：并发同步冲突
**风险等级**: 中
**影响**: 数据不一致
**缓解措施**:
- 实现分布式锁
- 任务队列去重
- 乐观锁控制

---

## 验收标准

### 功能验收
1. **OpenAI集成**
   - [ ] README自动生成成功率 > 90%
   - [ ] 目录优化准确率 > 85%
   - [ ] 问答响应时间 < 3秒
   - [ ] 支持中英文双语

2. **文档目录服务**
   - [ ] 目录CRUD功能完整
   - [ ] 搜索响应时间 < 500ms
   - [ ] 支持1000+文件的目录

3. **访问日志和统计**
   - [ ] 日志收集准确率 > 99%
   - [ ] 统计延迟 < 1小时
   - [ ] 支持7种统计维度

4. **同步统计**
   - [ ] 统计信息完整准确
   - [ ] 实时计算无明显延迟

### 性能验收
- [ ] API平均响应时间 < 200ms
- [ ] 支持100并发用户
- [ ] 内存占用 < 2GB
- [ ] CPU使用率 < 70%

### 质量验收
- [ ] 单元测试覆盖率 > 70%
- [ ] 集成测试通过率 100%
- [ ] 代码审查通过
- [ ] 无P0/P1级别Bug

### 文档验收
- [ ] API文档完整
- [ ] 部署文档可执行
- [ ] 配置说明清晰
- [ ] 故障排查指南

---

## 附录

### A. 配置文件模板
```yaml
# application-phase3.yml
koalawiki:
  # OpenAI配置
  openai:
    api-key: ${OPENAI_API_KEY}
    endpoint: https://api.openai.com/v1
    chat-model: gpt-4
    analysis-model: gpt-4-turbo-preview
    max-tokens: 4096
    temperature: 0.7
    timeout: 60000
    retry:
      max-attempts: 3
      backoff-delay: 1000

  # 访问日志配置
  access-log:
    enabled: true
    batch-size: 100
    flush-interval: 30000
    retention-days: 90

  # 统计配置
  statistics:
    enabled: true
    daily-cron: "0 0 1 * * ?"
    weekly-cron: "0 0 1 ? * MON"
    monthly-cron: "0 0 1 1 * ?"

  # 同步配置
  sync:
    statistics-enabled: true
    notification-enabled: false
```

### B. 测试数据准备脚本
```sql
-- 创建测试仓库
INSERT INTO warehouse (id, name, owner, status) VALUES
('test-warehouse-1', 'test-repo-1', 'testuser', 'ACTIVE'),
('test-warehouse-2', 'test-repo-2', 'testuser', 'ACTIVE');

-- 创建测试文档
INSERT INTO document (id, warehouse_id, title, content) VALUES
('test-doc-1', 'test-warehouse-1', 'README', '# Test Repository'),
('test-doc-2', 'test-warehouse-1', 'API Guide', '## API Documentation');

-- 创建测试访问日志
INSERT INTO access_log (id, warehouse_id, action, access_time) VALUES
('log-1', 'test-warehouse-1', 'VIEW', NOW()),
('log-2', 'test-warehouse-1', 'SEARCH', NOW());
```

### C. 监控指标
```yaml
metrics:
  - name: ai_request_total
    type: counter
    description: AI请求总数

  - name: ai_request_duration
    type: histogram
    description: AI请求耗时

  - name: catalog_optimization_rate
    type: gauge
    description: 目录优化成功率

  - name: sync_statistics_calculation_time
    type: histogram
    description: 同步统计计算时间

  - name: access_log_queue_size
    type: gauge
    description: 访问日志队列大小
```

### D. 相关文档链接
- [OpenAI API文档](https://platform.openai.com/docs/api-reference)
- [Spring Boot 2.7文档](https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/)
- [JGit使用指南](https://www.eclipse.org/jgit/documentation/)
- [项目进度跟踪](./PROGRESS.md)
- [Phase 1-2总结](./TASK_LIST.md)

---

**文档版本**: 1.0
**最后更新**: 2025-11-13
**编写者**: OpenDeepWiki Team
**审核者**: Project Manager
**状态**: 待实施