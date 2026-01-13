# 领域-服务两层结构与定时文档同步实现计划

## 需求概述

1. 数据库中的仓库按两层结构组织：领域 -> 服务
2. Git工作目录按领域组织：工作空间 -> 领域代码 -> 仓库名称
3. 定时任务：每天凌晨2点更新服务的master分支，有更新则更新服务文档和领域文档
4. 页面上增加手动触发按钮：生成服务文档、生成领域文档（已有）

## 用户选择

- 目录结构：领域使用code字段，服务直接使用关联仓库的name
- 执行时间：凌晨2点
- 更新策略：服务有更新时，同时更新服务文档和所属领域文档

## 目录结构示例

```
工作空间/
├── domain-a/              # 领域代码
│   ├── service-repo-1/    # 仓库名称（warehouse.name）
│   └── service-repo-2/
└── domain-b/
    └── service-repo-3/
```

---

## 实现步骤

### 步骤1: 数据库迁移

创建迁移脚本 `V8__add_domain_code.sql`：

```sql
-- 领域表添加code字段
ALTER TABLE domain_info ADD COLUMN code VARCHAR(64);

-- 服务表添加last_commit_id字段（用于检测更新）
ALTER TABLE service_document_config ADD COLUMN last_commit_id VARCHAR(64);
```

**文件**: `koalawiki/src/main/resources/db/migration/V8__add_domain_code.sql`

---

### 步骤2: 后端领域模型修改

**修改文件**: `koalawiki/src/main/java/ai/opendw/koalawiki/domain/ai/DomainInfo.java`

新增字段：
```java
private String code;  // 领域代码，用于Git路径
```

**修改文件**: `koalawiki/src/main/java/ai/opendw/koalawiki/domain/ai/ServiceDocumentLibrary.java`

新增字段：
```java
private String lastCommitId;  // 上次生成文档时的commit ID
```

---

### 步骤3: 后端实体类修改

**修改文件**: `koalawiki/src/main/java/ai/opendw/koalawiki/infra/entity/DomainInfoEntity.java`

```java
@Column(length = 64)
private String code;
```

**修改文件**: `koalawiki/src/main/java/ai/opendw/koalawiki/infra/entity/ServiceDocumentConfigEntity.java`

```java
@Column(name = "last_commit_id", length = 64)
private String lastCommitId;
```

---

### 步骤4: Repository扩展

**修改文件**: `koalawiki/src/main/java/ai/opendw/koalawiki/infra/repository/ServiceDocumentConfigRepository.java`

新增方法：
```java
List<ServiceDocumentConfigEntity> findByEnabled(Boolean enabled);
List<ServiceDocumentConfigEntity> findByDomainIdAndEnabled(String domainId, Boolean enabled);
```

---

### 步骤5: Git路径解析器扩展

**修改文件**: `koalawiki/src/main/java/ai/opendw/koalawiki/core/git/GitPathResolver.java`

新增方法：
```java
/**
 * 根据领域代码和仓库名称生成本地存储路径
 * 格式: storagePath/domainCode/warehouseName
 */
public String getLocalPathByDomain(String domainCode, String warehouseName) {
    return Paths.get(storagePath, domainCode, warehouseName).toString();
}
```

---

### 步骤6: 服务层修改

**修改文件**: `koalawiki/src/main/java/ai/opendw/koalawiki/app/service/ai/DomainDocumentService.java`

1. 修改 `createDomain` 方法，增加 `code` 参数
2. 修改 `updateDomain` 方法，支持更新 `code`
3. 修改转换方法 `toDomainModel` 和 `toServiceModel`，映射新字段
4. 新增方法：`updateServiceLastCommitId(String serviceId, String commitId)`

---

### 步骤7: 新增定时任务

**新建文件**: `koalawiki/src/main/java/ai/opendw/koalawiki/app/task/DomainDocSyncTask.java`

核心逻辑：
```java
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainDocSyncTask {

    private final DomainInfoRepository domainRepository;
    private final ServiceDocumentConfigRepository serviceRepository;
    private final WarehouseRepository warehouseRepository;
    private final GitService gitService;
    private final GitPathResolver pathResolver;
    private final DomainDocumentService domainDocumentService;

    @Value("${koalawiki.doc-sync.enabled:true}")
    private boolean syncEnabled;

    /**
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "${koalawiki.doc-sync.cron:0 0 2 * * ?}")
    public void executeDailySync() {
        if (!syncEnabled) return;

        // 1. 查询所有领域
        List<DomainInfoEntity> domains = domainRepository.findAll();

        for (DomainInfoEntity domain : domains) {
            if (domain.getCode() == null) continue;

            // 2. 查询领域下的服务
            List<ServiceDocumentConfigEntity> services =
                serviceRepository.findByDomainIdAndEnabled(domain.getId(), true);

            boolean domainNeedsUpdate = false;

            for (ServiceDocumentConfigEntity service : services) {
                // 3. 获取服务关联的仓库
                WarehouseEntity warehouse = warehouseRepository
                    .findById(service.getWarehouseId()).orElse(null);
                if (warehouse == null) continue;

                // 4. 构建Git路径：领域代码/仓库名称
                String localPath = pathResolver.getLocalPathByDomain(
                    domain.getCode(), warehouse.getName());

                // 5. 拉取最新代码
                GitRepositoryInfo repoInfo = gitService.pullRepository(localPath, null);
                String latestCommitId = repoInfo.getLatestCommitId();

                // 6. 检测是否有更新
                if (latestCommitId != null &&
                    !latestCommitId.equals(service.getLastCommitId())) {

                    // 7. 生成服务文档
                    domainDocumentService.generateServiceDocument(service.getId(), "");

                    // 8. 更新lastCommitId
                    service.setLastCommitId(latestCommitId);
                    serviceRepository.save(service);

                    domainNeedsUpdate = true;
                    log.info("服务文档已更新: serviceId={}", service.getId());
                }
            }

            // 9. 如果有服务更新，同时更新领域文档
            if (domainNeedsUpdate) {
                domainDocumentService.generateDomainDocument(domain.getId(), "");
                log.info("领域文档已更新: domainId={}", domain.getId());
            }
        }
    }
}
```

---

### 步骤8: Controller修改

**修改文件**: `koalawiki/src/main/java/ai/opendw/koalawiki/web/controller/DomainController.java`

1. 修改 `CreateDomainRequest` DTO，增加 `code` 字段
2. 修改 `UpdateDomainRequest` DTO，增加 `code` 字段
3. 修改对应的创建和更新方法调用

---

### 步骤9: 配置文件修改

**修改文件**: `koalawiki/src/main/resources/application.yml`

新增配置：
```yaml
koalawiki:
  doc-sync:
    enabled: true
    cron: "0 0 2 * * ?"
```

---

### 步骤10: 前端修改

**修改文件**: `koalawiki-web-vue/src/views/DomainManagement.vue`

1. 在创建领域对话框中增加"领域代码"输入框
2. 在编辑领域对话框中增加"领域代码"输入框
3. 修改表单数据和提交逻辑

---

## 关键文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `koalawiki/src/main/resources/db/migration/V8__add_domain_code.sql` | 新建 | 数据库迁移脚本 |
| `koalawiki/src/main/java/ai/opendw/koalawiki/domain/ai/DomainInfo.java` | 修改 | 添加code字段 |
| `koalawiki/src/main/java/ai/opendw/koalawiki/domain/ai/ServiceDocumentLibrary.java` | 修改 | 添加lastCommitId字段 |
| `koalawiki/src/main/java/ai/opendw/koalawiki/infra/entity/DomainInfoEntity.java` | 修改 | 添加code字段 |
| `koalawiki/src/main/java/ai/opendw/koalawiki/infra/entity/ServiceDocumentConfigEntity.java` | 修改 | 添加lastCommitId字段 |
| `koalawiki/src/main/java/ai/opendw/koalawiki/infra/repository/ServiceDocumentConfigRepository.java` | 修改 | 添加查询方法 |
| `koalawiki/src/main/java/ai/opendw/koalawiki/core/git/GitPathResolver.java` | 修改 | 添加getLocalPathByDomain方法 |
| `koalawiki/src/main/java/ai/opendw/koalawiki/app/service/ai/DomainDocumentService.java` | 修改 | 修改创建/更新方法 |
| `koalawiki/src/main/java/ai/opendw/koalawiki/app/task/DomainDocSyncTask.java` | 新建 | 定时同步任务 |
| `koalawiki/src/main/java/ai/opendw/koalawiki/web/controller/DomainController.java` | 修改 | 修改DTO |
| `koalawiki/src/main/resources/application.yml` | 修改 | 添加doc-sync配置 |
| `koalawiki-web-vue/src/views/DomainManagement.vue` | 修改 | 添加领域代码输入框 |

---

## 验证方案

### 1. 数据库验证
```sql
DESCRIBE domain_info;
DESCRIBE service_document_config;
```

### 2. 后端API验证
```bash
# 创建领域（带code）
curl -X POST http://localhost:8080/api/v1/warehouses/{warehouseId}/domains \
  -H "Content-Type: application/json" \
  -d '{"name":"测试领域","description":"描述","code":"test-domain"}'
```

### 3. 定时任务验证
- 检查日志输出
- 手动触发测试

### 4. 前端验证
- 访问领域管理页面
- 创建领域时填写领域代码
- 点击"生成文档"按钮验证功能

---

## 验证示例

### 测试数据
- **领域名称**: 履约域
- **领域代码**: fulfillment
- **仓库地址**: http://code.oppoer.me/digital-food-server/qpon-fulfillment-center.git
- **仓库名称**: qpon-fulfillment-center

### 预期目录结构
```
工作空间/
└── fulfillment/                    # 领域代码
    └── qpon-fulfillment-center/    # 仓库名称
```

### 验证步骤（使用 Chrome DevTools MCP）

#### 步骤1: 打开领域管理页面
```
mcp_chrome-devtools_navigate_page: url=http://localhost:8080/#/domains
mcp_chrome-devtools_take_snapshot: 获取页面元素
```

#### 步骤2: 创建领域
```
mcp_chrome-devtools_click: uid=创建领域按钮
mcp_chrome-devtools_fill_form:
  - 领域名称: 履约域
  - 领域代码: fulfillment
  - 描述: 履约中心服务
mcp_chrome-devtools_click: uid=确认按钮
mcp_chrome-devtools_wait_for: text=创建成功
```
**校验**: 页面显示"创建成功"提示

#### 步骤3: 添加仓库
```
mcp_chrome-devtools_navigate_page: url=http://localhost:8080/#/warehouses
mcp_chrome-devtools_click: uid=添加仓库按钮
mcp_chrome-devtools_fill_form:
  - 仓库名称: qpon-fulfillment-center
  - 仓库地址: http://code.oppoer.me/digital-food-server/qpon-fulfillment-center.git
  - 分支: master
mcp_chrome-devtools_click: uid=确认按钮
mcp_chrome-devtools_wait_for: text=添加成功
```
**校验**: 仓库列表中出现 qpon-fulfillment-center

#### 步骤4: 关联服务到领域
```
mcp_chrome-devtools_navigate_page: url=http://localhost:8080/#/domains
mcp_chrome-devtools_click: uid=履约域行
mcp_chrome-devtools_click: uid=添加服务按钮
mcp_chrome-devtools_fill: uid=仓库选择框, value=qpon-fulfillment-center
mcp_chrome-devtools_click: uid=确认按钮
```
**校验**: 服务列表中显示 qpon-fulfillment-center

#### 步骤5: 生成服务文档
```
mcp_chrome-devtools_click: uid=qpon-fulfillment-center服务行的生成文档按钮
mcp_chrome-devtools_wait_for: text=文档生成中
mcp_chrome-devtools_wait_for: text=生成完成 (timeout=300000)
```
**校验**: 
- 服务状态变为"已生成"
- 可点击查看文档内容

#### 步骤6: 生成领域文档
```
mcp_chrome-devtools_click: uid=履约域的生成领域文档按钮
mcp_chrome-devtools_wait_for: text=领域文档生成完成 (timeout=300000)
```
**校验**: 领域文档状态更新

#### 步骤7: 验证网络请求
```
mcp_chrome-devtools_list_network_requests: resourceTypes=["fetch","xhr"]
mcp_chrome-devtools_get_network_request: reqid=对应请求ID
```
**校验**: 
- POST /api/v1/domains 返回 200
- POST /api/v1/warehouses 返回 200
- POST /api/v1/services/{id}/generate-doc 返回 200

#### 步骤8: 验证控制台无错误
```
mcp_chrome-devtools_list_console_messages: types=["error"]
```
**校验**: 无 error 级别日志