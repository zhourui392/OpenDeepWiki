# KoalaWiki - 快速开始指南

## 环境要求

- JDK 1.8+
- Maven 3.6+

## 项目结构

```
OpenDeepWiki/
├── src/                           # Java源码
│   └── main/
│       ├── java/                  # Java代码
│       └── resources/             # 配置和静态资源
├── pom.xml                        # Maven配置
├── deploy.sh                      # 部署脚本
├── quick-deploy.sh                # 快速部署脚本
└── README.md                      # 项目说明
```

## 快速启动

### 1. 克隆项目

```bash
git clone https://github.com/zhourui392/OpenDeepWiki.git
cd OpenDeepWiki
```

### 2. 编译项目

```bash
# 清理并编译
mvn clean compile

# 打包（跳过测试）
mvn clean package -DskipTests
```

### 3. 运行应用

```bash
# 方式1：直接运行JAR
java -jar target/koalawiki-0.1.0-SNAPSHOT.jar

# 方式2：使用部署脚本
./deploy.sh deploy

# 方式3：使用快速部署脚本
./quick-deploy.sh
```

### 4. 访问应用

- 应用首页: http://localhost:18081
- 健康检查: http://localhost:18081/actuator/health

## 配置说明

### 默认配置（SQLite数据库）

配置文件位于 `src/main/resources/application.yml`

```yaml
server:
  port: 18081

spring:
  datasource:
    url: jdbc:sqlite:./data/koalawiki.db
```

### AI配置（可选）

如果需要使用AI文档生成功能:

```bash
# Claude CLI
export CLAUDE_API_KEY=your_key
```

## 使用示例

### 1. 添加仓库

```bash
curl -X POST http://localhost:18081/api/warehouse/SubmitWarehouse \
  -H "Content-Type: application/json" \
  -d '{
    "address": "https://github.com/username/repo.git",
    "branch": "main"
  }'
```

### 2. 生成文档

```bash
curl -X POST http://localhost:18081/api/v1/warehouses/{warehouseId}/generate-docs \
  -H "Content-Type: application/json" \
  -d '{
    "agentType": "claude",
    "language": "chinese"
  }'
```

### 3. 查看文档

```bash
# 文档列表
curl "http://localhost:18081/api/v1/warehouses/{warehouseId}/documents?page=0&size=20"

# 文档详情
curl "http://localhost:18081/api/v1/documents/{documentId}"
```

## 部署脚本

```bash
# 完整部署（编译+启动）
./deploy.sh deploy

# 仅编译
./deploy.sh build

# 启动/停止/重启
./deploy.sh start
./deploy.sh stop
./deploy.sh restart

# 查看状态和日志
./deploy.sh status
./deploy.sh tail
```

## IDE 配置

### IntelliJ IDEA

1. 安装 Lombok 插件
2. 启用注解处理: Settings → Build → Compiler → Annotation Processors → Enable
3. 导入项目: File → Open → 选择 pom.xml

## 常见问题

### Q: 编译失败，提示找不到 lombok
A: 确保在IDE中安装了Lombok插件并启用了注解处理。

### Q: 端口被占用
A: 修改 `application.yml` 中的端口号:
```yaml
server:
  port: 18082
```

### Q: AI文档生成失败
A:
1. 检查是否安装了Claude CLI
2. 检查环境变量配置
3. 查看日志获取详细错误信息

## 故障排查

```bash
# 查看应用日志
tail -f logs/koalawiki.log

# 检查健康状态
curl http://localhost:18081/actuator/health
```

---

**最后更新**: 2025-01-13
