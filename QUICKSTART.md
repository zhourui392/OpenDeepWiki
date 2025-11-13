# OpenDeepWiki Java 版本 - 快速开始指南

## 环境要求

- JDK 1.8
- Maven 3.6+
- IDE（推荐 IntelliJ IDEA，需安装 Lombok 插件）

## 项目结构

```
java/
├── pom.xml                      # 父 POM 文件
├── koalawiki-domain/           # 领域模型层
├── koalawiki-core/             # 核心服务层
├── koalawiki-infra/            # 基础设施层
├── koalawiki-app/              # 应用服务层
├── koalawiki-web/              # 接口层（Web入口）
├── README.md                    # 迁移计划
└── PROGRESS.md                  # 开发进度
```

## 编译项目

```bash
# 进入 Java 项目目录
cd /home/ubuntu/workspace/OpenDeepWiki/java

# 清理并编译
mvn clean compile

# 打包（跳过测试）
mvn clean package -DskipTests
```

## 运行项目

```bash
# 方式1：使用 Maven 插件运行
mvn spring-boot:run -pl koalawiki-web

# 方式2：运行打包后的 JAR
java -jar koalawiki-web/target/koalawiki-web-0.1.0-SNAPSHOT.jar
```

## 配置说明

### 默认配置（H2内存数据库）
应用默认使用 H2 内存数据库，配置文件位于 `koalawiki-web/src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:koalawiki
    username: sa
    password:
```

### MySQL 配置（推荐生产环境）
创建 `application-mysql.yml`：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/koalawiki?useUnicode=true&characterEncoding=utf8&useSSL=false
    username: root
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL5InnoDBDialect
```

运行时指定 profile：
```bash
mvn spring-boot:run -pl koalawiki-web -Dspring-boot.run.profiles=mysql
```

### PostgreSQL 配置
创建 `application-postgresql.yml`：

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/koalawiki
    username: postgres
    password: your_password
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

## 访问应用

### 健康检查
```bash
curl http://localhost:8080/api/health
```

响应示例：
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "status": "UP",
    "application": "OpenDeepWiki",
    "version": "0.1.0-SNAPSHOT"
  },
  "timestamp": 1699999999999
}
```

### H2 控制台
访问：http://localhost:8080/h2-console

连接信息：
- JDBC URL: `jdbc:h2:mem:koalawiki`
- Username: `sa`
- Password: (留空)

### Actuator 端点
- 健康检查：http://localhost:8080/actuator/health
- 应用信息：http://localhost:8080/actuator/info

## IDE 配置

### IntelliJ IDEA

1. **安装 Lombok 插件**
   - File → Settings → Plugins
   - 搜索 "Lombok" 并安装
   - 重启 IDE

2. **启用注解处理**
   - File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - 勾选 "Enable annotation processing"

3. **导入项目**
   - File → Open
   - 选择 `java/pom.xml`
   - 作为 Maven 项目导入

### Eclipse

1. **安装 Lombok**
   - 下载 lombok.jar
   - 双击运行安装程序
   - 选择 Eclipse 安装目录

2. **导入项目**
   - File → Import → Maven → Existing Maven Projects
   - 选择 `java` 目录

## 常见问题

### Q: 编译失败，提示找不到 lombok
A: 确保在对应模块的 pom.xml 中添加了 lombok 依赖：
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>
```

### Q: 运行时提示找不到主类
A: 确保在 `koalawiki-web` 模块运行，该模块包含 `@SpringBootApplication` 注解的主类。

### Q: H2 控制台无法访问
A: 检查 `application.yml` 中是否启用了 H2 控制台：
```yaml
spring:
  h2:
    console:
      enabled: true
```

### Q: 端口被占用
A: 修改 `application.yml` 中的端口号：
```yaml
server:
  port: 8081  # 改为其他端口
```

## 开发建议

### 代码规范
- 使用阿里巴巴 Java 开发规范（P3C）
- 执行代码检查：`mvn pmd:check`

### 日志配置
日志级别配置在 `application.yml`：
```yaml
logging:
  level:
    root: INFO
    ai.opendw.koalawiki: DEBUG
```

### 测试
```bash
# 运行所有测试
mvn test

# 运行单个模块的测试
mvn test -pl koalawiki-core
```

## 下一步

查看 [PROGRESS.md](./PROGRESS.md) 了解项目进度和后续计划。

## 技术支持

- 项目文档：[README.md](./README.md)
- 开发进度：[PROGRESS.md](./PROGRESS.md)

---
**最后更新**: 2025-11-13
