# OpenDeepWiki Java 迁移计划（草案）

目标：在不改变核心业务能力的基础上，将 .NET 9 的 OpenDeepWiki 迁移为 Java（Spring 生态），分阶段上线、可回滚、可渐进替换。默认 JDK 17 + Spring Boot 3.3；若必须兼容 JDK 8，可切换 Spring Boot 2.7 并调整API用法。

## 一、模块映射（DDD）
- koalawiki-domain：领域模型（Entity/VO/Enum），与 DB 无关，提供校验注解
- koalawiki-core：领域服务/通用基础（用户上下文、扩展、拦截器接口）
- koalawiki-infra：基础设施（JPA 映射、仓储、数据迁移 Flyway、外部系统 Client）
- koalawiki-app：应用服务（用例编排、事务边界、DTO 映射 MapStruct）
- koalawiki-web：接口层（REST/SSE、认证授权、过滤器、中间件）

目录已创建：`java/` 下包含上述 Maven 多模块骨架（仅依赖与空结构）。

## 二、技术选型
- Web/DI：Spring Boot（Web/Security/Actuator/Validation）
- ORM：Spring Data JPA + Hibernate；迁移 EF Core 模型至 JPA 实体
- 数据迁移：Flyway（多数据库脚本：MySQL/PostgreSQL/SQLServer/Sqlite）
- 安全：Spring Security + JWT（Header 与 Cookie 双读取）；BCrypt 密码；CORS 策略
- HTTP 客户端：Spring WebClient（连接池/超时/重试）
- 映射：MapStruct（替代 Mapster）
- 日志：SLF4J + Logback（JSON/Console）
- 任务：@Scheduled/@Async + TaskExecutor（替代 BackgroundService）
- 文档处理流水线：责任链/管道模式 + Spring 事件或调度器
- 配置：application.yml + 可选 Spring Cloud Config（替代 DynamicOptions）
- 测试：JUnit 5 + Mockito + Testcontainers（多 DB 集成测试）
- 规范：Alibaba P3C（pmd 集成构建校验）

## 三、功能拆分与迁移顺序
1) 基础能力与脚手架
   - 建立模块、应用启动、健康检查、全局异常、日志、CORS、响应压缩
   - JWT 认证/授权策略（Header/Cookie 解析），用户上下文 UserContext
2) 领域与持久化
   - 迁移 Domains：User/Role/Warehouse/Document/Statistics 等实体到 JPA
   - 建立 Repository 接口与查询（等价 EF Core DbSet）
   - 编写 Flyway/V1__init.sql，测试四种数据库驱动的建表与 CRUD
3) 后台任务与访问日志
   - 迁移 AccessLog 队列与中间件 -> Spring Filter + 队列 + @Scheduled 批处理
   - 移植 StatisticsBackgroundService、MiniMapBackgroundService 等
4) 文档处理与仓库同步
   - 迁移 DocumentsHelper 与处理管道 -> 应用服务 + 责任链
   - GitService -> JGit；实现仓库扫描、提交记录、同步记录
   - WarehouseSyncService/Executor -> 事务性用例与事件
5) 外部系统集成
   - Feishu 客户端（HTTP/SSE/上传下载）；重写 FeishuStore、FeishuClient
   - OpenAI/Mem0：如有 Java SDK 则接入，否则用 HTTP；若无 mem0 Java 版，定义接口远程调用现有服务
6) API 层与前端契约
   - 迁移 Minimal API/Controller 到 Spring MVC Controller；保持路由与 JSON 契约
   - SSE 流式输出保持一致（EventSource 兼容）
7) 数据迁移与灰度
   - 双写/校验：.NET 与 Java 共用数据库，逐模块切换
   - Canary：按路径或用户灰度路由到 Java 版

## 四、数据库与配置
- 多数据库支持：
  - MySQL：`mysql-connector-j`；PostgreSQL：`postgresql`；SqlServer：`mssql-jdbc`；Sqlite：`xerial sqlite-jdbc`
  - 以 Spring Profile 切换驱动与方言；Flyway 按 vendor 目录拆分脚本
- 配置映射：将现有 appsettings.json -> application.yml；JWT、OpenAI、Feishu、文档路径、仓库路径等

## 五、关键难点与对策
- EF Core 特性差异 -> JPA 注解与关系映射谨慎验证（值对象、枚举、级联/索引）
- BackgroundService -> @Scheduled/@Async 生命周期管理与幂等
- 文档/仓库大文件处理 -> 流式 IO、断点/重试、并发限制
- 动态配置 -> 先保留静态配置，后引入 DB 配置表 + 缓存 + 后台刷新（或 Nacos/Spring Cloud Config）
- 性能与一致性 -> Testcontainers 做集成回归；必要时引入缓存（Caffeine/Redis）

## 六、测试与质量
- 单元测试：JUnit5 + Mockito，覆盖应用/领域服务
- 集成测试：Testcontainers 启动对应数据库与依赖服务（Feishu mock）
- 端到端：RestAssured 驱动主要 API；契约测试保障前后端不回归
- 代码扫描：p3c-pmd、SpotBugs；CI 执行 mvn verify

## 七、里程碑
- M1（1-2 周）：脚手架 + 基础安全 + Health + 基础实体映射 + H2 内存演示
- M2（2-3 周）：仓库/文档核心流程 + 任务/日志 + MySQL/PostgreSQL 完成
- M3（2 周）：Feishu/OpenAI/Mem0 集成 + 性能优化 + 兼容性对齐
- M4（1 周）：灰度切换与稳定性，文档与运维脚本

## 八、替代方案（若非 Spring）
- Quarkus：更快的冷启动与内存占用；适合容器/原生镜像
- Micronaut：注解少、编译时 DI；适合轻量微服务
- Vert.x：响应式，适合高并发 IO；迁移复杂度较高

## 九、执行建议
- 采用 Strangler 模式，逐功能替换；数据库共存校验
- 先 MySQL/PostgreSQL，后覆盖 SqlServer/Sqlite
- 按模块提交 PR，保持可运行、可回滚

