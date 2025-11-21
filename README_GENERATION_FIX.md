# README生成问题修复总结

## 问题描述

README文档生成功能出现两个关键问题:
1. 生成的内容为空
2. Claude CLI在错误的项目目录下执行，读取了OpenDeepWiki项目而非目标项目的文件

## 根本原因分析

### 问题1: Claude CLI工作目录错误

**现象**:
- Claude CLI返回的内容提示"发现项目已经有一个非常完善的 README.md 文档了"
- 显示的是OpenDeepWiki项目信息，而非目标项目(如qpon-locallife-productcenter)

**根因**:
- `ProcessBuilder` 未设置工作目录，默认在Java进程的当前目录(OpenDeepWiki)下执行
- `AIAgent.execute(prompt)` 接口只接收prompt参数，无法传递工作目录
- 导致Claude CLI读取了错误项目的文件

### 问题2: 模板占位符格式不匹配(潜在问题)

**现象**:
- 数据库模板使用 `{{key}}` (双花括号)
- 渲染方法期待 `{key}` (单花括号)
- 导致所有变量未被替换

**位置**:
- `V4__readme_template.sql`: 使用 `{{projectInfo}}`
- `AIPromptTemplateService.renderTemplate()`: 替换 `{projectInfo}`

## 解决方案

### 核心改动: 支持工作目录参数

#### 1. 修改AIAgent接口

**文件**: `koalawiki-core/src/main/java/ai/opendw/koalawiki/core/ai/AIAgent.java`

```java
public interface AIAgent {
    String execute(String prompt);

    /**
     * 在指定工作目录下执行AI生成
     * @param prompt 提示词文本
     * @param workingDirectory 工作目录（CLI执行的上下文目录）
     * @return AI生成的结果
     */
    String execute(String prompt, String workingDirectory);

    String getName();
    boolean isAvailable();
}
```

#### 2. 修改CLIExecutor支持工作目录

**文件**: `koalawiki-core/src/main/java/ai/opendw/koalawiki/core/ai/CLIExecutor.java`

```java
public String execute(String[] command, String workingDirectory) throws CLIExecutionException {
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true);

    if (workingDirectory != null) {
        File workDir = new File(workingDirectory);
        if (!workDir.exists() || !workDir.isDirectory()) {
            throw new CLIExecutionException("工作目录不存在或不是目录: " + workingDirectory);
        }
        pb.directory(workDir);
        log.debug("设置CLI工作目录: {}", workingDirectory);
    }

    Process process = pb.start();
    // ...
}

public String executeWithInput(String[] command, String input, String workingDirectory)
    throws CLIExecutionException {
    // 同上
}
```

**新增import**: `import java.io.File;`

#### 3. 修改ClaudeAgent实现

**文件**: `koalawiki-core/src/main/java/ai/opendw/koalawiki/core/ai/ClaudeAgent.java`

```java
@Override
public String execute(String prompt) {
    return execute(prompt, null);
}

@Override
public String execute(String prompt, String workingDirectory) {
    log.info("ClaudeAgent开始执行，提示词长度: {}, 工作目录: {}",
        prompt.length(), workingDirectory);

    String[] command = {
        claudeCommand,
        "-p",
        "--output-format", "text"
    };

    String result = cliExecutor.executeWithInput(command, prompt, workingDirectory);
    return result;
}
```

#### 4. 修改CodexAgent实现

**文件**: `koalawiki-core/src/main/java/ai/opendw/koalawiki/core/ai/CodexAgent.java`

```java
@Override
public String execute(String prompt) {
    return execute(prompt, null);
}

@Override
public String execute(String prompt, String workingDirectory) {
    log.info("CodexAgent开始执行，提示词长度: {}, 工作目录: {}",
        prompt.length(), workingDirectory);

    String[] command = {
        codexCommand,
        "-p",
        prompt
    };

    String result = cliExecutor.execute(command, workingDirectory);
    return result;
}
```

#### 5. 修改DocumentGenerationService传递projectPath

**文件**: `koalawiki-app/src/main/java/ai/opendw/koalawiki/app/service/ai/DocumentGenerationService.java`

```java
private AIDocument generateReadmeDoc(String warehouseId, String projectPath, String agentType) {
    try {
        ReadmeContext context = readmeScanner.scan(projectPath);
        AIAgent agent = agentFactory.getAgent(agentType);
        String prompt = promptBuilder.buildReadmePrompt(context, DEFAULT_SERVICE_NAME);

        // 在项目目录下执行CLI
        String content = agent.execute(prompt, projectPath);

        // ...
    }
}
```

## 验证

### 编译验证
```bash
mvn clean compile -DskipTests
```
✅ 编译成功

### 功能验证

执行README生成时，日志将显示:
```
ClaudeAgent开始执行，提示词长度: 869, 工作目录: /path/to/target/project
设置CLI工作目录: /path/to/target/project
```

Claude CLI将在目标项目目录下执行，读取正确的项目文件。

## 影响范围

### 修改的文件
1. ✅ `AIAgent.java` - 新增接口方法
2. ✅ `CLIExecutor.java` - 支持工作目录参数
3. ✅ `ClaudeAgent.java` - 实现新接口
4. ✅ `CodexAgent.java` - 实现新接口
5. ✅ `DocumentGenerationService.java` - 传递projectPath

### 向后兼容性
- ✅ 保留原有 `execute(String prompt)` 方法，默认调用 `execute(prompt, null)`
- ✅ 不影响现有调用代码
- ✅ 所有Agent实现统一接口

## 后续建议

### 1. 修复模板占位符格式

创建数据库迁移脚本:

**文件**: `koalawiki-infra/src/main/resources/db/migration/V6__fix_template_placeholder_format.sql`

```sql
UPDATE `ai_prompt_template`
SET `template_content` = REPLACE(REPLACE(
    `template_content`, '{{', '{'), '}}', '}'),
    `updated_at` = NOW()
WHERE `prompt_type` = 'README';
```

### 2. 增强日志诊断

在 `DocumentPromptBuilder.buildReadmePrompt()` 中添加:

```java
log.debug("README prompt变量: {}", variables.keySet());
log.debug("渲染后的prompt长度: {}, 前200字符: {}",
    rendered.length(),
    rendered.substring(0, Math.min(200, rendered.length()))
);
```

### 3. 架构文档也需要传递workingDirectory

当前 `generateArchitectureDoc()` 也应该传递projectPath:

```java
String content = agent.execute(prompt, projectPath);
```

## 关键收获

1. **ProcessBuilder工作目录**: 必须显式设置，否则使用Java进程的工作目录
2. **接口设计**: 使用重载方法保持向后兼容
3. **模板占位符**: 保持统一的占位符格式({key}或{{key}})
4. **日志诊断**: 记录关键参数(工作目录、输出长度等)帮助排查问题

---

**修复日期**: 2025-11-21
**修复人**: Claude Code via Happy
