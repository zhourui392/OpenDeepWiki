# AI代码文档自动生成系统 - 极简设计方案

> **设计理念**: 利用 Claude Code CLI 和 Codex CLI 的能力,不再依赖 OpenAI SDK
>
> **创建日期**: 2025-11-16
>
> **预计周期**: 4周 MVP

---

## 核心理念

**原方案问题**:
- ❌ 依赖 OpenAI SDK (复杂的 API 封装)
- ❌ 需要实现复杂的 Prompt 管理
- ❌ 需要处理 Token 计算、限流
- ❌ 需要处理重试、降级逻辑

**新方案**:
- ✅ **直接调用 CLI** - `claude -p "生成文档"` 或 `codex "生成文档"`
- ✅ **CLI = Agent** - CLI 本身就是 Agent,包含了完整的能力
- ✅ **Prompt = 命令参数** - 不需要复杂的模板管理
- ✅ **Java 调用 CLI** - 通过 `ProcessBuilder` 执行命令

---

## 架构设计

### 1. 三层架构

```
┌─────────────────────────────────────────┐
│          应用层 (DocumentService)        │
│  - 触发文档生成                          │
│  - 管理任务状态                          │
└────────────────┬────────────────────────┘
                 │
┌────────────────┴────────────────────────┐
│         Agent层 (CLI Wrapper)           │
│  ┌──────────────┐  ┌──────────────┐    │
│  │ ClaudeAgent  │  │ CodexAgent   │    │
│  │ (封装CLI)    │  │ (封装CLI)    │    │
│  └──────────────┘  └──────────────┘    │
└────────────────┬────────────────────────┘
                 │
┌────────────────┴────────────────────────┐
│            CLI层 (命令行工具)            │
│  ┌──────────────┐  ┌──────────────┐    │
│  │ claude       │  │ codex        │    │
│  │ (已安装)     │  │ (已安装)     │    │
│  └──────────────┘  └──────────────┘    │
└─────────────────────────────────────────┘
```

### 2. Agent 封装设计

**Agent = Prompt + CLI**

```java
/**
 * AI Agent 接口
 */
public interface AIAgent {
    /**
     * 生成文档
     * @param code 源代码
     * @param context 上下文信息
     * @return 生成的文档内容
     */
    String generateDocument(String code, Map<String, Object> context);
}

/**
 * Claude Agent 实现
 */
@Component
public class ClaudeAgent implements AIAgent {

    @Override
    public String generateDocument(String code, Map<String, Object> context) {
        // 1. 构建 Prompt
        String prompt = buildPrompt(code, context);

        // 2. 调用 Claude CLI
        String[] command = {
            "claude",
            "-p",  // print模式
            "--output-format", "text",
            "--tools", "Read",  // 只允许读取工具
            prompt
        };

        // 3. 执行命令
        return executeCommand(command);
    }

    private String buildPrompt(String code, Map<String, Object> context) {
        String className = (String) context.get("className");
        return String.format("""
            请为以下Java类生成技术文档:

            类名: %s
            代码:
            ```java
            %s
            ```

            要求:
            1. 生成Markdown格式
            2. 包含类概述、方法说明、使用示例
            3. 语言: 中文
            """, className, code);
    }

    private String executeCommand(String[] command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // 读取输出
            String output = new String(
                process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
            );

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("CLI执行失败: " + output);
            }

            return output;

        } catch (Exception e) {
            throw new RuntimeException("执行CLI失败", e);
        }
    }
}

/**
 * Codex Agent 实现 (类似)
 */
@Component
public class CodexAgent implements AIAgent {

    @Override
    public String generateDocument(String code, Map<String, Object> context) {
        String prompt = buildPrompt(code, context);

        String[] command = {
            "codex",
            "-p",
            prompt
        };

        return executeCommand(command);
    }

    // buildPrompt 和 executeCommand 实现类似
}
```

### 3. Agent 选择策略

```java
/**
 * Agent 工厂
 */
@Component
public class AIAgentFactory {

    private final ClaudeAgent claudeAgent;
    private final CodexAgent codexAgent;

    @Value("${ai.default-agent:claude}")
    private String defaultAgent;

    /**
     * 获取 Agent
     */
    public AIAgent getAgent(String agentType) {
        if (agentType == null) {
            agentType = defaultAgent;
        }

        return switch (agentType) {
            case "claude" -> claudeAgent;
            case "codex" -> codexAgent;
            default -> claudeAgent;
        };
    }
}
```

---

## 极简数据库设计

只需要 **2 张表**:

```sql
-- 1. AI文档表
CREATE TABLE ai_document (
    id VARCHAR(36) PRIMARY KEY,
    warehouse_id VARCHAR(36) NOT NULL,
    source_file VARCHAR(500) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content LONGTEXT NOT NULL,
    status ENUM('GENERATING', 'COMPLETED', 'FAILED') NOT NULL,
    agent_type VARCHAR(20) COMMENT 'claude 或 codex',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_warehouse_file (warehouse_id, source_file),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI生成的文档';

-- 2. 生成任务表
CREATE TABLE generation_task (
    id VARCHAR(36) PRIMARY KEY,
    warehouse_id VARCHAR(36) NOT NULL,
    status ENUM('PENDING', 'RUNNING', 'COMPLETED', 'FAILED') NOT NULL,
    total_files INT DEFAULT 0,
    completed_files INT DEFAULT 0,
    failed_files INT DEFAULT 0,
    agent_type VARCHAR(20),
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档生成任务';
```

**对比原方案**:
- 原方案: 6张表, 38个字段
- 新方案: 2张表, 22个字段
- **简化 70%**

---

## 核心实现

### 1. 文档生成服务

```java
@Service
@Slf4j
public class DocumentGenerationService {

    private final AIAgentFactory agentFactory;
    private final JavaCodeParser codeParser;
    private final AIDocumentRepository documentRepository;

    /**
     * 为单个文件生成文档
     */
    public AIDocument generateForFile(String warehouseId, File javaFile) {
        log.info("开始生成文档: {}", javaFile.getPath());

        // 1. 解析代码
        ParseResult parseResult = codeParser.parse(javaFile);
        String code = Files.readString(javaFile.toPath());

        // 2. 构建上下文
        Map<String, Object> context = Map.of(
            "className", parseResult.getClassName(),
            "packageName", parseResult.getPackageName(),
            "language", "java"
        );

        // 3. 获取 Agent
        AIAgent agent = agentFactory.getAgent("claude");

        // 4. 生成文档
        String content = agent.generateDocument(code, context);

        // 5. 保存到数据库
        AIDocument document = new AIDocument();
        document.setId(UUID.randomUUID().toString());
        document.setWarehouseId(warehouseId);
        document.setSourceFile(javaFile.getPath());
        document.setTitle(parseResult.getClassName());
        document.setContent(content);
        document.setStatus("COMPLETED");
        document.setAgentType("claude");

        documentRepository.save(document);

        log.info("文档生成完成: {}", document.getId());
        return document;
    }

    /**
     * 批量生成文档
     */
    @Async("docGenExecutor")
    public CompletableFuture<Void> generateBatch(String warehouseId,
                                                  List<File> javaFiles) {
        String taskId = UUID.randomUUID().toString();

        // 创建任务
        GenerationTask task = new GenerationTask();
        task.setId(taskId);
        task.setWarehouseId(warehouseId);
        task.setTotalFiles(javaFiles.size());
        task.setStatus("RUNNING");
        taskRepository.save(task);

        int completed = 0;
        int failed = 0;

        for (File file : javaFiles) {
            try {
                generateForFile(warehouseId, file);
                completed++;
            } catch (Exception e) {
                log.error("文件生成失败: {}", file.getPath(), e);
                failed++;
            }

            // 更新进度
            task.setCompletedFiles(completed);
            task.setFailedFiles(failed);
            taskRepository.save(task);
        }

        // 完成任务
        task.setStatus("COMPLETED");
        task.setCompletedAt(new Date());
        taskRepository.save(task);

        return CompletableFuture.completedFuture(null);
    }
}
```

### 2. REST API

```java
@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    private final DocumentGenerationService generationService;
    private final AIDocumentRepository documentRepository;

    /**
     * 触发文档生成
     */
    @PostMapping("/warehouses/{warehouseId}/generate-docs")
    public ApiResponse<String> generateDocs(@PathVariable String warehouseId) {
        // 1. 扫描仓库中的Java文件
        List<File> javaFiles = scanJavaFiles(warehouseId);

        // 2. 异步生成
        CompletableFuture<Void> future = generationService
            .generateBatch(warehouseId, javaFiles);

        // 3. 返回任务ID
        return ApiResponse.success("文档生成任务已启动");
    }

    /**
     * 获取文档列表
     */
    @GetMapping("/warehouses/{warehouseId}/ai-documents")
    public ApiResponse<Page<AIDocument>> listDocuments(
            @PathVariable String warehouseId,
            Pageable pageable) {

        Page<AIDocument> documents = documentRepository
            .findByWarehouseId(warehouseId, pageable);

        return ApiResponse.success(documents);
    }

    /**
     * 获取单个文档
     */
    @GetMapping("/ai-documents/{id}")
    public ApiResponse<AIDocument> getDocument(@PathVariable String id) {
        AIDocument document = documentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("文档不存在"));
        return ApiResponse.success(document);
    }
}
```

---

## 配置文件

```yaml
# application.yml
ai:
  # Agent配置
  default-agent: claude  # claude 或 codex

  # CLI路径配置 (可选,默认从PATH查找)
  cli:
    claude: claude
    codex: codex

  # 超时配置
  timeout: 60000  # 60秒

# 异步任务配置
spring:
  task:
    execution:
      pool:
        core-size: 3
        max-size: 5
        queue-capacity: 100
```

---

## 前端实现

### 1. API Service

```typescript
// src/api/ai-document.ts
import axios from 'axios';

export const aiDocumentApi = {
  // 触发生成
  generate(warehouseId: string) {
    return axios.post(`/api/v1/warehouses/${warehouseId}/generate-docs`);
  },

  // 获取文档列表
  list(warehouseId: string, page: number = 0, size: number = 20) {
    return axios.get(`/api/v1/warehouses/${warehouseId}/ai-documents`, {
      params: { page, size }
    });
  },

  // 获取文档详情
  get(id: string) {
    return axios.get(`/api/v1/ai-documents/${id}`);
  }
};
```

### 2. 文档列表页面

```vue
<!-- src/views/AIDocuments.vue -->
<template>
  <div class="ai-documents">
    <div class="header">
      <h2>AI生成文档</h2>
      <el-button
        type="primary"
        @click="handleGenerate"
        :loading="generating">
        生成文档
      </el-button>
    </div>

    <el-table :data="documents" v-loading="loading">
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="sourceFile" label="源文件" />
      <el-table-column prop="agentType" label="Agent" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="handleView(row)">
            查看
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      :page-size="size"
      :total="total"
      @current-change="loadDocuments" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { aiDocumentApi } from '@/api/ai-document';

const route = useRoute();
const router = useRouter();

const warehouseId = route.params.id as string;
const documents = ref([]);
const loading = ref(false);
const generating = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);

const loadDocuments = async () => {
  loading.value = true;
  try {
    const res = await aiDocumentApi.list(warehouseId, page.value - 1, size.value);
    documents.value = res.data.content;
    total.value = res.data.total;
  } finally {
    loading.value = false;
  }
};

const handleGenerate = async () => {
  generating.value = true;
  try {
    await aiDocumentApi.generate(warehouseId);
    ElMessage.success('文档生成任务已启动,请稍候刷新查看');

    // 5秒后自动刷新
    setTimeout(() => {
      loadDocuments();
    }, 5000);
  } catch (error) {
    ElMessage.error('生成失败: ' + error.message);
  } finally {
    generating.value = false;
  }
};

const handleView = (doc: any) => {
  router.push(`/ai-documents/${doc.id}`);
};

const getStatusType = (status: string) => {
  return {
    'COMPLETED': 'success',
    'GENERATING': 'warning',
    'FAILED': 'danger'
  }[status] || 'info';
};

onMounted(() => {
  loadDocuments();
});
</script>
```

### 3. 文档详情页面

```vue
<!-- src/views/AIDocumentDetail.vue -->
<template>
  <div class="document-detail">
    <div class="header">
      <el-button @click="$router.back()">返回</el-button>
      <h2>{{ document?.title }}</h2>
    </div>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>源文件: {{ document?.sourceFile }}</span>
          <el-tag>{{ document?.agentType }}</el-tag>
        </div>
      </template>

      <div class="markdown-body" v-html="renderedContent"></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { aiDocumentApi } from '@/api/ai-document';
import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';

const route = useRoute();
const document = ref(null);
const md = new MarkdownIt({
  highlight: (str, lang) => {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(str, { language: lang }).value;
    }
    return '';
  }
});

const renderedContent = computed(() => {
  return document.value ? md.render(document.value.content) : '';
});

onMounted(async () => {
  const id = route.params.id as string;
  const res = await aiDocumentApi.get(id);
  document.value = res.data;
});
</script>

<style>
@import 'highlight.js/styles/github.css';
</style>
```

---

## 实施计划

### Week 1: 基础搭建
- Day 1-2: 数据库设计和创建
- Day 3-4: Agent 封装实现
- Day 5: 测试 CLI 调用

### Week 2: 核心功能
- Day 1-2: 文档生成服务
- Day 3-4: REST API 实现
- Day 5: 异步任务处理

### Week 3: 前端开发
- Day 1-3: 文档列表页面
- Day 4-5: 文档详情页面

### Week 4: 测试上线
- Day 1-2: 集成测试
- Day 3-4: Bug修复
- Day 5: 部署上线

---

## 对比总结

| 维度 | 原方案 | 新方案 | 改进 |
|-----|-------|-------|-----|
| **依赖** | OpenAI SDK | CLI (已安装) | ✅ 零依赖 |
| **代码量** | ~5000行 | ~500行 | ✅ 减少90% |
| **数据库** | 6张表,38字段 | 2张表,22字段 | ✅ 简化70% |
| **功能** | 多模型/问答/审核 | 文档生成 | ✅ 聚焦核心 |
| **实施周期** | 14周 | 4周 | ✅ 缩短70% |
| **Token管理** | 复杂限流逻辑 | CLI自带 | ✅ 无需关心 |
| **Prompt** | 模板管理系统 | 字符串拼接 | ✅ 极简 |
| **重试/降级** | 自己实现 | CLI自带 | ✅ 无需关心 |
| **成本** | 需要实现成本统计 | 看账单即可 | ✅ 简单 |

---

## 核心优势

1. **极简实现** - Agent本质就是封装CLI,代码量极少
2. **零学习成本** - 不需要学OpenAI SDK
3. **功能完整** - CLI已包含完整能力(重试、限流、成本控制)
4. **易于扩展** - 增加新Agent只需实现接口
5. **快速交付** - 4周完成MVP

---

## 下一步

1. 创建数据库表
2. 实现 `ClaudeAgent` 和 `CodexAgent`
3. 实现 `DocumentGenerationService`
4. 实现 REST API
5. 实现前端页面

**立即开始开发!**
