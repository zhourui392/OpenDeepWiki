# 核心模块详细设计

> 文档来源：基于架构设计，详细说明各核心模块的实现细节
>
> 最后更新：2025-11-16

## 1. 代码解析模块 (Code Parser Module)

### 1.1 模块职责

负责解析多种编程语言的源代码，提取结构化信息供AI文档生成使用。

### 1.2 类图设计

```java
package ai.opendw.koalawiki.core.parser;

/**
 * 代码解析器接口
 */
public interface CodeParser {
    /**
     * 判断是否支持该文件
     */
    boolean supports(String filePath);

    /**
     * 解析代码文件
     */
    ParseResult parse(File codeFile);

    /**
     * 获取支持的文件扩展名
     */
    List<String> getSupportedExtensions();
}

/**
 * Java代码解析器
 */
public class JavaCodeParser implements CodeParser {
    private final JavaParser javaParser;

    @Override
    public ParseResult parse(File codeFile) {
        // 使用JavaParser解析
        CompilationUnit cu = javaParser.parse(codeFile).getResult().get();

        // 提取类信息
        List<ClassDeclaration> classes = cu.findAll(ClassDeclaration.class);

        // 提取方法信息
        List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);

        // 构建ParseResult
        return ParseResult.builder()
            .fileName(codeFile.getName())
            .packageName(extractPackage(cu))
            .classes(convertClasses(classes))
            .methods(convertMethods(methods))
            .build();
    }
}

/**
 * Python代码解析器
 */
public class PythonCodeParser implements CodeParser {
    @Override
    public ParseResult parse(File codeFile) {
        // 使用Jython或调用Python AST模块
        // 提取类、函数、导入等信息
    }
}

/**
 * 解析结果
 */
@Data
@Builder
public class ParseResult {
    private String fileName;
    private String packageName;
    private List<ClassInfo> classes;
    private List<MethodInfo> methods;
    private List<ImportInfo> imports;
    private String fileContent;
    private Map<String, Object> metadata;
}

/**
 * 类信息
 */
@Data
public class ClassInfo {
    private String name;
    private String fullName;
    private String accessModifier;
    private String comment;
    private List<String> annotations;
    private List<String> interfaces;
    private String superClass;
    private List<FieldInfo> fields;
    private List<MethodInfo> methods;
    private int startLine;
    private int endLine;
}

/**
 * 方法信息
 */
@Data
public class MethodInfo {
    private String name;
    private String returnType;
    private String accessModifier;
    private String comment;
    private List<ParameterInfo> parameters;
    private List<String> annotations;
    private List<String> exceptions;
    private int complexity; // 圈复杂度
    private int startLine;
    private int endLine;
}
```

### 1.3 解析流程

```
读取代码文件
     ↓
识别文件类型 → 选择对应解析器
     ↓
语法树解析 → AST构建
     ↓
遍历AST节点 → 提取结构信息
     ↓
提取注释/JavaDoc → 补充文档信息
     ↓
计算代码指标 → 复杂度、行数等
     ↓
构建ParseResult → 返回结构化数据
```

### 1.4 支持的语言

| 语言 | 解析器 | 提取信息 |
|-----|--------|---------|
| Java | JavaParser | 类、接口、方法、字段、注释 |
| Python | Python AST | 类、函数、模块、装饰器 |
| JavaScript | Esprima | 类、函数、导出、注释 |
| TypeScript | TypeScript Compiler | 接口、类型、类、函数 |
| Go | Go Parser | 结构体、函数、接口 |

## 2. AI集成模块 (LLM Integration Module)

### 2.1 模块职责

封装与大语言模型的交互，提供统一的API调用接口。

### 2.2 类图设计

```java
package ai.opendw.koalawiki.core.ai;

/**
 * LLM客户端接口
 */
public interface LLMClient {
    /**
     * 生成文档
     */
    String generateDocument(DocumentGenerationRequest request);

    /**
     * 流式生成文档
     */
    Flux<String> generateDocumentStream(DocumentGenerationRequest request);

    /**
     * 回答问题
     */
    String answerQuestion(QuestionRequest request);
}

/**
 * OpenAI客户端实现
 */
@Service
public class OpenAIClient implements LLMClient {
    private final OpenAiService openAiService;
    private final TokenLimiter tokenLimiter;
    private final PromptManager promptManager;

    @Override
    public String generateDocument(DocumentGenerationRequest request) {
        // 构建Prompt
        String prompt = promptManager.buildPrompt(request);

        // Token限流
        tokenLimiter.checkAndConsume(request.getUserId(),
            estimateTokens(prompt));

        // 调用OpenAI API
        CompletionRequest completionRequest = CompletionRequest.builder()
            .model("gpt-4")
            .messages(List.of(
                new ChatMessage("system", getSystemPrompt()),
                new ChatMessage("user", prompt)
            ))
            .temperature(0.3)
            .maxTokens(2000)
            .build();

        CompletionChoice choice = openAiService
            .createChatCompletion(completionRequest)
            .getChoices()
            .get(0);

        return choice.getMessage().getContent();
    }
}

/**
 * Claude客户端实现
 */
@Service
public class ClaudeClient implements LLMClient {
    // 类似OpenAI实现
}

/**
 * Prompt管理器
 */
@Component
public class PromptManager {
    private final Map<String, String> promptTemplates;

    /**
     * 构建文档生成Prompt
     */
    public String buildPrompt(DocumentGenerationRequest request) {
        String template = promptTemplates.get(request.getDocType());

        return template
            .replace("{{CODE}}", request.getCodeContent())
            .replace("{{CLASS_NAME}}", request.getClassName())
            .replace("{{METHODS}}", formatMethods(request.getMethods()))
            .replace("{{LANGUAGE}}", request.getLanguage());
    }

    /**
     * 加载Prompt模板
     */
    @PostConstruct
    public void loadTemplates() {
        // 从配置文件或数据库加载Prompt模板
    }
}

/**
 * Token限流器 (基于数据库实现,单机部署)
 */
@Component
public class TokenLimiter {
    private final TokenUsageRepository tokenUsageRepository;

    /**
     * 检查并消费Token
     */
    public void checkAndConsume(String userId, int tokens) {
        LocalDate today = LocalDate.now();
        TokenUsageEntity usage = tokenUsageRepository
            .findByUserIdAndDate(userId, today)
            .orElse(new TokenUsageEntity(userId, today));

        if (usage.getTotalTokens() + tokens > DAILY_LIMIT) {
            throw new TokenLimitExceededException(
                "Daily token limit exceeded");
        }

        usage.incrementTokens(tokens);
        tokenUsageRepository.save(usage);
    }
}

/**
 * 文档生成请求
 */
@Data
public class DocumentGenerationRequest {
    private String userId;
    private String warehouseId;
    private String fileId;
    private String codeContent;
    private String className;
    private List<MethodInfo> methods;
    private String language;
    private DocType docType; // CLASS, METHOD, API, ARCHITECTURE
    private Map<String, Object> context;
}
```

### 2.3 Prompt模板设计

#### 2.3.1 类文档生成模板

```markdown
# Role
You are a senior software engineer writing technical documentation.

# Task
Generate comprehensive documentation for the following {{LANGUAGE}} class:

## Class Information
- Name: {{CLASS_NAME}}
- Package: {{PACKAGE_NAME}}
- Extends: {{SUPER_CLASS}}
- Implements: {{INTERFACES}}

## Source Code
```{{LANGUAGE}}
{{CODE}}
```

## Requirements
1. Write a clear class overview (2-3 sentences)
2. Explain the purpose and responsibility of this class
3. Document each public method with:
   - Purpose
   - Parameters
   - Return value
   - Example usage
4. Highlight any important implementation details
5. Mention design patterns used (if any)

## Output Format
Use Markdown format with proper headings, code blocks, and lists.

# Documentation
```

#### 2.3.2 API文档生成模板

```markdown
# Role
You are an API documentation expert.

# Task
Generate REST API documentation for the following controller:

## Controller Code
```java
{{CODE}}
```

## Requirements
1. Document each API endpoint with:
   - HTTP Method and Path
   - Description
   - Request Parameters
   - Request Body (if applicable)
   - Response Format
   - Status Codes
   - Example Request/Response
2. Group endpoints by functionality
3. Add authentication requirements
4. Include error codes

## Output Format
OpenAPI 3.0 style Markdown

# API Documentation
```

### 2.4 多模型支持策略

```java
/**
 * LLM策略管理器
 */
@Component
public class LLMStrategyManager {
    private final List<LLMClient> clients;

    /**
     * 根据配置选择LLM
     */
    public LLMClient selectClient(String preference) {
        return switch (preference) {
            case "openai" -> clients.stream()
                .filter(c -> c instanceof OpenAIClient)
                .findFirst().orElseThrow();
            case "claude" -> clients.stream()
                .filter(c -> c instanceof ClaudeClient)
                .findFirst().orElseThrow();
            case "ollama" -> clients.stream()
                .filter(c -> c instanceof OllamaClient)
                .findFirst().orElseThrow();
            default -> getDefaultClient();
        };
    }

    /**
     * 降级策略：主模型失败时切换备用模型
     */
    public String generateWithFallback(DocumentGenerationRequest request) {
        for (LLMClient client : clients) {
            try {
                return client.generateDocument(request);
            } catch (Exception e) {
                log.warn("LLM {} failed, trying next",
                    client.getClass().getSimpleName(), e);
            }
        }
        throw new AllLLMsFailedException("All LLM providers failed");
    }
}
```

## 3. 文档生成模块 (Document Generator Module)

### 3.1 模块职责

协调代码解析和AI生成，管理文档生命周期。

### 3.2 类图设计

```java
package ai.opendw.koalawiki.core.generator;

/**
 * AI文档生成器
 */
@Service
public class AIDocumentGenerator {
    private final CodeParserFactory parserFactory;
    private final LLMClient llmClient;
    private final DocumentRepository documentRepository;
    private final MessagePublisher messagePublisher;

    /**
     * 为单个文件生成文档
     */
    public AIDocument generateForFile(String warehouseId,
                                       File codeFile) {
        // 1. 解析代码
        CodeParser parser = parserFactory.getParser(codeFile);
        ParseResult parseResult = parser.parse(codeFile);

        // 2. 构建生成请求
        DocumentGenerationRequest request = DocumentGenerationRequest.builder()
            .warehouseId(warehouseId)
            .codeContent(parseResult.getFileContent())
            .className(parseResult.getClasses().get(0).getName())
            .methods(parseResult.getMethods())
            .language(detectLanguage(codeFile))
            .docType(DocType.CLASS)
            .build();

        // 3. 调用AI生成
        String generatedDoc = llmClient.generateDocument(request);

        // 4. 保存文档
        AIDocument document = AIDocument.builder()
            .warehouseId(warehouseId)
            .sourceFile(codeFile.getPath())
            .title(parseResult.getClasses().get(0).getName())
            .content(generatedDoc)
            .docType(DocType.CLASS)
            .status(DocStatus.COMPLETED)
            .build();

        documentRepository.save(document);

        // 5. 发布事件
        messagePublisher.publish(new DocumentGeneratedEvent(document));

        return document;
    }

    /**
     * 批量生成文档
     */
    public void generateBatch(String warehouseId,
                               List<File> codeFiles) {
        // 创建生成任务
        GenerationTask task = createTask(warehouseId, codeFiles);

        // 异步处理
        CompletableFuture.runAsync(() -> {
            for (File file : codeFiles) {
                try {
                    generateForFile(warehouseId, file);
                    updateTaskProgress(task.getId());
                } catch (Exception e) {
                    log.error("Failed to generate doc for {}",
                        file.getPath(), e);
                    recordTaskError(task.getId(), file, e);
                }
            }
            completeTask(task.getId());
        });
    }
}

/**
 * 文档模板引擎
 */
@Component
public class DocumentTemplateEngine {
    /**
     * 应用模板
     */
    public String applyTemplate(String content,
                                Map<String, Object> variables) {
        // 使用Freemarker或Thymeleaf
    }
}

/**
 * 文档版本管理器
 */
@Component
public class DocumentVersionManager {
    /**
     * 创建新版本
     */
    public void createVersion(String documentId, String content) {
        // 保存历史版本
        // 支持版本对比和回滚
    }
}
```

### 3.3 生成流程

```
触发生成请求
     ↓
创建GenerationTask → 状态: PENDING
     ↓
扫描代码文件 → 过滤支持的文件类型
     ↓
批量分组 → 每批10-20个文件
     ↓
[循环处理每批]
     ↓
解析代码 → CodeParser
     ↓
调用AI生成 → LLMClient
     ↓
后处理文档 → 格式化、链接生成
     ↓
保存到数据库 → AIDocument
     ↓
更新任务进度 → WebSocket通知
     ↓
[批次完成]
     ↓
建立索引 → Elasticsearch
     ↓
任务完成 → 状态: COMPLETED
```

## 4. 增量更新模块

### 4.1 变更检测

```java
/**
 * 变更检测器
 */
@Component
public class CodeChangeDetector {
    private final GitClient gitClient;

    /**
     * 检测仓库变更
     */
    public ChangeSet detectChanges(String warehouseId) {
        // 获取上次同步的commit
        String lastCommit = getLastSyncCommit(warehouseId);

        // Git diff
        List<DiffEntry> diffs = gitClient.diff(lastCommit, "HEAD");

        // 分类变更
        List<String> added = new ArrayList<>();
        List<String> modified = new ArrayList<>();
        List<String> deleted = new ArrayList<>();

        for (DiffEntry diff : diffs) {
            switch (diff.getChangeType()) {
                case ADD -> added.add(diff.getNewPath());
                case MODIFY -> modified.add(diff.getNewPath());
                case DELETE -> deleted.add(diff.getOldPath());
            }
        }

        return ChangeSet.builder()
            .added(added)
            .modified(modified)
            .deleted(deleted)
            .build();
    }
}

/**
 * 增量更新处理器
 */
@Service
public class IncrementalUpdateProcessor {
    /**
     * 处理增量更新
     */
    public void processIncrementalUpdate(String warehouseId) {
        ChangeSet changes = changeDetector.detectChanges(warehouseId);

        // 处理新增文件
        for (String path : changes.getAdded()) {
            File file = new File(repoPath, path);
            documentGenerator.generateForFile(warehouseId, file);
        }

        // 处理修改文件
        for (String path : changes.getModified()) {
            File file = new File(repoPath, path);
            // 查找已有文档
            AIDocument existingDoc = documentRepository
                .findBySourceFile(warehouseId, path);
            if (existingDoc != null) {
                // 重新生成并更新
                AIDocument newDoc = documentGenerator
                    .generateForFile(warehouseId, file);
                updateDocument(existingDoc.getId(), newDoc);
            }
        }

        // 处理删除文件
        for (String path : changes.getDeleted()) {
            AIDocument doc = documentRepository
                .findBySourceFile(warehouseId, path);
            if (doc != null) {
                documentRepository.delete(doc.getId());
            }
        }
    }
}
```

## 5. 文档搜索模块

### 5.1 基于JPA的搜索实现 (单机部署)

```java
/**
 * 文档搜索服务
 */
@Service
public class DocumentSearchService {
    private final AIDocumentRepository documentRepository;

    /**
     * 搜索文档
     */
    public Page<AIDocument> searchDocuments(SearchQuery query) {
        Specification<AIDocumentEntity> spec = (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 仓库ID过滤
            predicates.add(cb.equal(root.get("warehouseId"),
                query.getWarehouseId()));

            // 关键词搜索 (标题或内容)
            if (StringUtils.hasText(query.getKeyword())) {
                String pattern = "%" + query.getKeyword() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("title"), pattern),
                    cb.like(root.get("content"), pattern)
                ));
            }

            // 文档类型过滤
            if (query.getDocType() != null) {
                predicates.add(cb.equal(root.get("docType"),
                    query.getDocType()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(
            query.getPage(),
            query.getSize(),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<AIDocumentEntity> entities = documentRepository
            .findAll(spec, pageable);

        return entities.map(this::toDocument);
    }

    /**
     * 简单高亮实现
     */
    private String highlightContent(String content, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return content;
        }
        return content.replaceAll(
            "(?i)" + Pattern.quote(keyword),
            "<mark>$0</mark>"
        );
    }
}
```

## 6. 异步任务处理

### 6.1 基于线程池的异步处理 (单机部署)

```java
/**
 * 文档生成任务
 */
@Data
public class DocGenerationTask {
    private String taskId;
    private String warehouseId;
    private List<String> filePaths;
    private String userId;
}

/**
 * 异步任务执行器
 */
@Service
public class DocGenerationTaskExecutor {
    private final AIDocumentGenerator generator;
    private final GenerationTaskRepository taskRepository;

    @Async("docGenExecutor")
    public CompletableFuture<Void> executeTask(DocGenerationTask task) {
        log.info("Processing doc generation task: {}", task.getTaskId());

        try {
            List<File> files = task.getFilePaths().stream()
                .map(File::new)
                .collect(Collectors.toList());

            generator.generateBatch(task.getWarehouseId(), files);

            // 更新任务状态
            updateTaskStatus(task.getTaskId(), TaskStatus.COMPLETED);

        } catch (Exception e) {
            log.error("Failed to process task: {}", task.getTaskId(), e);
            updateTaskStatus(task.getTaskId(), TaskStatus.FAILED);
        }

        return CompletableFuture.completedFuture(null);
    }

    private void updateTaskStatus(String taskId, TaskStatus status) {
        GenerationTaskEntity task = taskRepository.findById(taskId)
            .orElseThrow();
        task.setStatus(status);
        taskRepository.save(task);
    }
}
```

## 7. WebSocket实时通知

```java
/**
 * 文档生成进度通知
 */
@Component
public class DocGenerationProgressNotifier {
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 发送进度更新
     */
    public void notifyProgress(String userId, ProgressUpdate update) {
        messagingTemplate.convertAndSendToUser(
            userId,
            "/topic/doc-generation-progress",
            update
        );
    }
}

@Data
public class ProgressUpdate {
    private String taskId;
    private int total;
    private int completed;
    private int failed;
    private String currentFile;
    private double progress; // 0-100
}
```

## 8. 下一步

请继续阅读：
- [数据库设计](./03-database-schema.md)
- [AI集成方案](./04-ai-integration.md)
- [API接口设计](./05-api-design.md)
