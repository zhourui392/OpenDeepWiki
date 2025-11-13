# OpenDeepWiki Phase 3 - 快速开始

## 🚀 立即开始使用AI功能

### 步骤1: 配置OpenAI API Key

```bash
# 设置环境变量
export OPENAI_API_KEY="your-openai-api-key-here"
export CHAT_MODEL="gpt-4-turbo-preview"
```

或者创建 `.env` 文件（推荐）：
```bash
cd /home/ubuntu/workspace/OpenDeepWiki/java
cp .env.example .env
# 编辑 .env 文件，填入真实的API Key
```

### 步骤2: 编译项目

```bash
cd /home/ubuntu/workspace/OpenDeepWiki/java
mvn clean compile
```

### 步骤3: 测试AI客户端

```bash
# 启用测试模式
mvn spring-boot:run -pl koalawiki-web \
    -Dspring-boot.run.arguments="--koalawiki.ai.test-on-startup=true"
```

**预期输出**:
```
INFO  开始测试AI客户端...
INFO  测试1: 简单文本补全
INFO  结果: Hello, OpenDeepWiki!
INFO  测试2: 聊天补全
INFO  结果: OpenDeepWiki is...
INFO  测试3: 服务可用性
INFO  服务可用: true
INFO  测试4: 当前模型
INFO  模型: gpt-4-turbo-preview
INFO  AI客户端测试完成！
```

### 步骤4: 在代码中使用

```java
@Autowired
private IAIClient aiClient;

public void generateContent() {
    // 简单调用
    String result = aiClient.complete("生成一段关于OpenDeepWiki的介绍");
    System.out.println(result);
}
```

---

## 📚 完整文档

- [Phase 3 详细计划](./PHASE3_PLAN.md)
- [任务跟踪看板](./PHASE3_KANBAN.md)
- [快速启动指南](./PHASE3_QUICKSTART.md)
- [Day 1 进度报告](./PHASE3_DAY1_REPORT.md)

---

## ❓ 常见问题

### Q: 如何切换到GPT-3.5节省成本？
```bash
export CHAT_MODEL="gpt-3.5-turbo"
```

### Q: 如何调整温度和token数？
修改 `application-phase3.yml`:
```yaml
koalawiki:
  ai:
    temperature: 0.5  # 更确定性的输出
    max-tokens: 2000  # 更短的响应
```

### Q: 如何查看调用日志？
设置日志级别：
```yaml
logging:
  level:
    ai.opendw.koalawiki.infra.ai: DEBUG
```

---

**状态**: ✅ 基础设施完成，随时可以开始AI功能开发！