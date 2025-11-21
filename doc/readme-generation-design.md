# README文档生成技术方案

## 1. 背景与目标

### 背景
- 当前只能生成架构文档和类文档
- 缺少项目级的README文档
- 需要自动生成包含项目结构、功能、启动、测试等完整信息的README

### 目标
生成完整的README.md文档，包含：
- Maven模块结构
- 包功能说明
- 功能列表
- 快速启动指南
- 测试指南
- 数据模型关系

## 2. 方案设计

### 2.1 总体架构

采用**混合方案C**：核心文档类型用专用方法，扩展类型用模板驱动

```
文档类型：
├── ARCHITECTURE (架构文档) - 已有，generateForProject
├── README (项目说明) - 新增，generateReadmeDoc ⭐
├── API_GUIDE (API文档) - 未来扩展
└── CUSTOM (自定义) - 模板驱动
```

### 2.2 README生成流程

```
输入: warehouseId + serviceId + agentType
     ↓
1. 加载服务配置
     ↓
2. 扫描项目信息 (ProjectReadmeScanner)
   - Maven模块结构 (pom.xml)
   - 包结构和功能 (目录扫描)
   - 功能列表 (Controller/Service扫描)
   - 启动配置 (application.yml)
   - 测试文件 (test目录)
   - 数据模型 (Entity扫描)
     ↓
3. 构建README提示词 (buildReadmePrompt)
   - 使用专用模板
   - 填充扫描信息
     ↓
4. 执行Agent生成
     ↓
5. 保存文档
   - docType = README
   - title = "README.md"
   - sourceFile = "/"
```

### 2.3 数据结构

#### ReadmeContext (扫描结果)
```java
class ReadmeContext {
    String projectName;           // 项目名称
    String description;           // 项目描述
    List<MavenModule> modules;    // Maven模块
    List<PackageInfo> packages;   // 包结构
    List<Feature> features;       // 功能列表
    StartupGuide startupGuide;    // 启动指南
    TestGuide testGuide;          // 测试指南
    List<DataModel> dataModels;   // 数据模型
}

class MavenModule {
    String name;                  // 模块名
    String artifactId;            // artifactId
    String description;           // 描述
    List<String> dependencies;    // 依赖
}

class PackageInfo {
    String packageName;           // 包名
    String purpose;               // 用途
    int classCount;               // 类数量
}

class Feature {
    String name;                  // 功能名
    String description;           // 描述
    String endpoint;              // 接口路径（如有）
}

class DataModel {
    String entityName;            // 实体名
    String tableName;             // 表名
    List<String> fields;          // 字段
    List<String> relations;       // 关联关系
}
```

## 3. 实现细节

### 3.1 ProjectReadmeScanner (新增)

**职责**：扫描项目，提取README所需信息

**位置**：`koalawiki-core/src/main/java/ai/opendw/koalawiki/core/analysis/ProjectReadmeScanner.java`

**核心方法**：
```java
public ReadmeContext scan(String projectPath, List<String> targetPaths) {
    ReadmeContext context = new ReadmeContext();

    // 1. 扫描Maven模块
    context.setModules(scanMavenModules(projectPath));

    // 2. 扫描包结构
    context.setPackages(scanPackages(targetPaths));

    // 3. 扫描功能列表
    context.setFeatures(scanFeatures(targetPaths));

    // 4. 扫描启动配置
    context.setStartupGuide(scanStartupConfig(projectPath));

    // 5. 扫描测试
    context.setTestGuide(scanTests(projectPath));

    // 6. 扫描数据模型
    context.setDataModels(scanDataModels(targetPaths));

    return context;
}
```

**扫描策略**：

1. **Maven模块扫描**
   - 读取根pom.xml和子模块pom.xml
   - 提取artifactId、name、description
   - 分析dependencies

2. **包结构扫描**
   - 遍历src/main/java目录
   - 统计每个包的类数量
   - 根据包名推断用途（controller→接口层，service→业务层）

3. **功能列表扫描**
   - 扫描@RestController类
   - 提取@RequestMapping路径
   - 提取方法注释作为功能描述

4. **启动配置扫描**
   - 读取application.yml/properties
   - 提取端口、数据库配置
   - 生成启动命令

5. **测试扫描**
   - 检查test目录
   - 统计测试类数量
   - 生成测试命令

6. **数据模型扫描**
   - 扫描@Entity类
   - 提取@Table注解
   - 分析@OneToMany/@ManyToOne关系

### 3.2 DocumentPromptBuilder扩展

**新增方法**：
```java
public String buildReadmePrompt(ReadmeContext context, String serviceName) {
    StringBuilder prompt = new StringBuilder();

    prompt.append("# 生成README.md任务\n\n");
    prompt.append("请根据以下项目信息，生成完整的README.md文档。\n\n");

    // 1. 项目信息
    prompt.append("## 项目信息\n");
    prompt.append("- 项目名称: ").append(context.getProjectName()).append("\n");
    prompt.append("- 服务名称: ").append(serviceName).append("\n\n");

    // 2. Maven模块
    prompt.append("## Maven模块结构\n");
    for (MavenModule module : context.getModules()) {
        prompt.append("- ").append(module.getName())
              .append(": ").append(module.getDescription()).append("\n");
    }

    // 3. 包结构
    prompt.append("\n## 包结构\n");
    for (PackageInfo pkg : context.getPackages()) {
        prompt.append("- ").append(pkg.getPackageName())
              .append(" (").append(pkg.getClassCount()).append("个类): ")
              .append(pkg.getPurpose()).append("\n");
    }

    // 4. 功能列表
    prompt.append("\n## 功能列表\n");
    for (Feature feature : context.getFeatures()) {
        prompt.append("- ").append(feature.getName())
              .append(": ").append(feature.getDescription());
        if (feature.getEndpoint() != null) {
            prompt.append(" (").append(feature.getEndpoint()).append(")");
        }
        prompt.append("\n");
    }

    // 5. 启动指南
    prompt.append("\n## 启动配置\n");
    prompt.append(context.getStartupGuide().toMarkdown());

    // 6. 测试指南
    prompt.append("\n## 测试信息\n");
    prompt.append(context.getTestGuide().toMarkdown());

    // 7. 数据模型
    prompt.append("\n## 数据模型\n");
    for (DataModel model : context.getDataModels()) {
        prompt.append("- ").append(model.getEntityName())
              .append(" → ").append(model.getTableName()).append("\n");
    }

    // 8. 生成要求
    prompt.append("\n---\n\n");
    prompt.append("请生成一个专业的README.md，包含以下章节：\n");
    prompt.append("1. 项目简介\n");
    prompt.append("2. 技术栈\n");
    prompt.append("3. 模块说明\n");
    prompt.append("4. 功能特性\n");
    prompt.append("5. 快速开始\n");
    prompt.append("6. 测试\n");
    prompt.append("7. 数据模型\n");
    prompt.append("8. API文档（如有）\n");
    prompt.append("\n使用清晰的Markdown格式，包含代码块和表格。\n");

    return prompt.toString();
}
```

### 3.3 DocumentGenerationService扩展

**新增方法**：
```java
/**
 * 生成README文档
 *
 * @param warehouseId 仓库ID
 * @param serviceId 服务ID
 * @param projectPath 项目路径
 * @param agentType Agent类型
 * @return 生成的文档
 */
@Transactional
public AIDocument generateReadmeDoc(String warehouseId,
                                     String serviceId,
                                     String projectPath,
                                     String agentType) {
    log.info("开始生成README文档: warehouse={}, service={}", warehouseId, serviceId);

    // 1. 加载服务配置
    ServiceDocumentLibrary config = libraryService.getByServiceId(warehouseId, serviceId);
    if (config == null) {
        throw new IllegalArgumentException("服务配置不存在: " + serviceId);
    }

    // 2. 确定扫描路径
    List<String> targetPaths = resolveScanPaths(projectPath, config.getSourceGlobs());

    // 3. 扫描项目信息
    ReadmeContext context = readmeScanner.scan(projectPath, targetPaths);
    context.setProjectName(config.getServiceName());

    // 4. 构建提示词
    String prompt = promptBuilder.buildReadmePrompt(context, config.getServiceName());

    // 5. 执行生成
    AIAgent agent = agentFactory.getAgent(agentType != null ? agentType : config.getAgentType());
    String content = agent.execute(prompt);

    // 6. 保存文档
    AIDocumentEntity entity = new AIDocumentEntity();
    entity.setId(UUID.randomUUID().toString());
    entity.setWarehouseId(warehouseId);
    entity.setServiceId(serviceId);
    entity.setServiceName(config.getServiceName());
    entity.setDocType("README");
    entity.setPromptTemplateId(config.getPromptTemplateId());
    entity.setSourceFile("/");
    entity.setTitle("README.md");
    entity.setContent(content);
    entity.setStatus("COMPLETED");
    entity.setAgentType(agent.getName());

    documentRepository.save(entity);

    log.info("README文档生成成功: documentId={}", entity.getId());
    return toDocument(entity);
}

/**
 * 根据sourceGlobs解析扫描路径
 */
private List<String> resolveScanPaths(String projectPath, List<String> globs) {
    List<String> paths = new ArrayList<>();

    if (globs == null || globs.isEmpty()) {
        // 默认扫描所有src/main/java
        paths.add(projectPath + "/src/main/java");
    } else {
        // 根据glob规则确定扫描目录
        for (String glob : globs) {
            String dir = extractDirectory(glob);
            paths.add(projectPath + "/" + dir);
        }
    }

    return paths;
}
```

### 3.4 Controller接口

**新增接口**：
```java
/**
 * 生成README文档
 */
@PostMapping("/warehouses/{warehouseId}/services/{serviceId}/generate-readme")
public ApiResponse<Map<String, Object>> generateReadme(
        @PathVariable String warehouseId,
        @PathVariable String serviceId,
        @RequestBody(required = false) GenerateRequest request) {

    log.info("生成README文档: warehouseId={}, serviceId={}", warehouseId, serviceId);

    try {
        // 获取仓库信息
        WarehouseEntity warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("仓库不存在"));

        // 克隆/更新仓库
        String localPath = getOrCloneRepository(warehouse);

        // 生成README
        String agentType = request != null ? request.getAgentType() : null;
        AIDocument document = generationService.generateReadmeDoc(
            warehouseId, serviceId, localPath, agentType);

        Map<String, Object> result = new HashMap<>();
        result.put("documentId", document.getId());
        result.put("title", document.getTitle());
        result.put("message", "README文档生成成功");

        return ApiResponse.success(result);

    } catch (Exception e) {
        log.error("README生成失败", e);
        return ApiResponse.error(500, "生成失败: " + e.getMessage());
    }
}
```

## 4. 前端集成

### 4.1 ServiceDocuments.vue

在服务列表页添加"生成README"按钮：

```vue
<el-table-column label="操作" width="300">
  <template #default="{ row }">
    <el-button size="small" @click="handleGenerateReadme(row)">
      生成README
    </el-button>
    <el-button size="small" @click="handleEdit(row)">编辑</el-button>
    <el-button size="small" type="danger" @click="handleDelete(row)">
      删除
    </el-button>
  </template>
</el-table-column>
```

### 4.2 API封装

```typescript
// service-document.ts
export const serviceDocumentApi = {
  // ... 现有方法

  /**
   * 生成README文档
   */
  generateReadme(warehouseId: string, serviceId: string, agentType?: string) {
    return apiClient.post(
      `/v1/warehouses/${warehouseId}/services/${serviceId}/generate-readme`,
      { agentType }
    )
  }
}
```

## 5. 实施步骤

### 阶段1：核心扫描器 (2-3小时)
1. 创建ReadmeContext数据结构
2. 实现ProjectReadmeScanner
   - Maven模块扫描
   - 包结构扫描
   - 功能列表扫描

### 阶段2：生成流程 (1-2小时)
3. 扩展DocumentPromptBuilder
4. 实现generateReadmeDoc方法
5. 添加Controller接口

### 阶段3：前端集成 (1小时)
6. 添加生成按钮
7. API调用
8. 结果展示

### 阶段4：测试优化 (1小时)
9. 测试生成效果
10. 优化提示词
11. 完善错误处理

## 6. 示例输出

生成的README.md示例：

```markdown
# KoalaWiki - AI驱动的代码文档生成平台

## 项目简介
KoalaWiki是一个基于AI的代码文档自动生成平台，支持多种文档类型生成。

## 技术栈
- Java 8
- Spring Boot 2.7.x
- Maven
- MySQL
- Vue 3

## 模块说明
- **koalawiki-domain**: 领域模型层
- **koalawiki-infra**: 基础设施层
- **koalawiki-core**: 核心业务层
- **koalawiki-app**: 应用服务层
- **koalawiki-web**: Web接口层

## 功能特性
- ✅ 仓库管理
- ✅ AI文档生成
- ✅ 服务配置
- ✅ 多Agent支持

## 快速开始
\`\`\`bash
# 1. 克隆项目
git clone https://github.com/xxx/koalawiki.git

# 2. 配置数据库
# 修改 application.yml

# 3. 启动应用
mvn spring-boot:run

# 4. 访问
http://localhost:8080
\`\`\`

## 测试
\`\`\`bash
mvn test
\`\`\`

## 数据模型
- Warehouse → warehouses (仓库)
- AIDocument → ai_document (AI文档)
- ServiceDocumentConfig → service_document_config (服务配置)
```

## 7. 扩展性

### 未来可扩展的文档类型
- API_GUIDE: API文档（扫描Controller）
- MODULE_GUIDE: 模块文档（单个模块详细说明）
- DATABASE_SCHEMA: 数据库文档（ER图+表说明）
- DEPLOYMENT_GUIDE: 部署文档（Docker/K8s配置）

### 扩展方式
1. 新增专用Scanner
2. 新增buildXxxPrompt方法
3. 新增generateXxxDoc方法
4. 添加Controller接口

## 8. 注意事项

1. **性能**：README生成需要扫描整个项目，可能耗时较长，建议异步执行
2. **缓存**：可以缓存扫描结果，避免重复扫描
3. **增量更新**：检测文件变化，只重新扫描变化的部分
4. **错误处理**：扫描失败时提供降级方案，生成基础README
5. **权限**：确保有读取pom.xml和配置文件的权限

## 9. 成功标准

- ✅ 能够扫描Maven多模块项目
- ✅ 准确识别包结构和功能
- ✅ 生成的README包含所有必需章节
- ✅ Markdown格式规范
- ✅ 生成时间 < 30秒
- ✅ 前端操作流畅
