# OpenDeepWiki API 测试计划

## 测试环境
- **Base URL**: http://localhost:18091
- **测试工具**: Playwright MCP
- **测试时间**: 2025-11-15
- **测试目标**: 验证所有REST API接口的可用性和正确性

---

## 测试优先级分类

### P0 - 核心功能 (必须测试)
关键业务流程,系统无法运行则标记为严重问题

### P1 - 重要功能 (应该测试)
主要功能模块,问题会影响用户体验

### P2 - 辅助功能 (可以测试)
增强功能,问题影响较小

---

## 测试用例清单

### 1. 健康检查接口 (P0)

#### TC-001: 健康检查
- **接口**: `GET /api/health`
- **期望**:
  - HTTP 200
  - 返回包含 status="UP", application="OpenDeepWiki"
- **测试数据**: 无需参数
- **验证点**:
  - 响应时间 < 200ms
  - JSON格式正确
  - 包含version字段

---

### 2. 仓库管理接口 (WarehouseController) - P0

#### TC-002: 获取仓库列表(空列表)
- **接口**: `GET /api/Warehouse/WarehouseList`
- **参数**: `page=1&pageSize=12`
- **期望**:
  - HTTP 200
  - 返回分页数据结构
  - code=0 或 success=true
- **验证点**:
  - 分页参数正确
  - 空列表时totalElements=0

#### TC-003: 获取仓库列表(带关键字)
- **接口**: `GET /api/Warehouse/WarehouseList`
- **参数**: `page=1&pageSize=12&keyword=test`
- **期望**: HTTP 200,支持关键字搜索

#### TC-004: 提交Git仓库(无效URL)
- **接口**: `POST /api/Warehouse/SubmitWarehouse`
- **请求体**:
```json
{
  "address": "invalid-url",
  "branch": "main"
}
```
- **期望**: 返回错误提示"无法解析仓库地址"

#### TC-005: 提交Git仓库(有效URL)
- **接口**: `POST /api/Warehouse/SubmitWarehouse`
- **请求体**:
```json
{
  "address": "https://github.com/octocat/Hello-World.git",
  "branch": "master",
  "email": "test@example.com"
}
```
- **期望**:
  - HTTP 200
  - 返回仓库ID
  - 状态为PENDING
- **验证点**:
  - 仓库记录创建成功
  - 返回的warehouseId不为空

#### TC-006: 重复提交相同仓库
- **接口**: `POST /api/Warehouse/SubmitWarehouse`
- **前置**: 先执行TC-005
- **期望**: 返回"仓库已存在"错误

#### TC-007: 自定义提交仓库
- **接口**: `POST /api/Warehouse/CustomSubmitWarehouse`
- **请求体**:
```json
{
  "organization": "TestOrg",
  "repositoryName": "TestRepo",
  "address": "https://github.com/TestOrg/TestRepo.git",
  "branch": "main"
}
```
- **期望**: HTTP 200,仓库创建成功

#### TC-008: 获取最后一个仓库
- **接口**: `GET /api/Warehouse/LastWarehouse`
- **前置**: 至少创建一个仓库
- **期望**:
  - HTTP 200
  - 返回最新创建的仓库信息

#### TC-009: 获取仓库统计信息
- **接口**: `GET /api/Warehouse/Stats`
- **期望**:
  - HTTP 200
  - 返回统计列表
  - 包含totalFiles, documentFiles等字段

#### TC-010: 获取分支列表
- **接口**: `GET /api/Warehouse/BranchList`
- **参数**: `address=https://github.com/octocat/Hello-World.git`
- **期望**:
  - HTTP 200
  - 返回分支数组
  - 包含defaultBranch字段

---

### 3. 仓库操作接口 (RepositoryController) - P0

#### TC-011: 获取仓库详情(不存在的ID)
- **接口**: `GET /api/Repository/Repository`
- **参数**: `id=non-existent-id`
- **期望**: 返回"仓库不存在"错误

#### TC-012: 获取仓库详情(有效ID)
- **接口**: `GET /api/Repository/Repository`
- **参数**: `id={warehouseId}` (从TC-005获取)
- **期望**:
  - HTTP 200
  - 返回完整的仓库信息

#### TC-013: 获取仓库列表(带状态过滤)
- **接口**: `GET /api/Repository/RepositoryList`
- **参数**: `page=1&pageSize=12&status=PENDING`
- **期望**: 返回指定状态的仓库列表

#### TC-014: 更新仓库信息
- **接口**: `PUT /api/Repository/UpdateWarehouse`
- **参数**: `id={warehouseId}`
- **请求体**:
```json
{
  "name": "Updated Name",
  "description": "Updated Description"
}
```
- **期望**: HTTP 200,更新成功

#### TC-015: 删除仓库(不存在的ID)
- **接口**: `DELETE /api/Repository/Repository`
- **参数**: `id=non-existent-id`
- **期望**: 返回"仓库不存在"错误

---

### 4. 文件操作接口 (P1)

#### TC-016: 获取文件列表(根目录)
- **接口**: `GET /api/Repository/Files`
- **参数**: `warehouseId={warehouseId}&path=`
- **期望**:
  - HTTP 200
  - 返回文件列表
  - 包含isDirectory, size等字段

#### TC-017: 获取文件内容(不存在的文件)
- **接口**: `GET /api/Repository/FileContent`
- **参数**: `warehouseId={warehouseId}&path=non-existent.md`
- **期望**: 返回"文件不存在"错误

#### TC-018: 获取文件内容(使用ID格式)
- **接口**: `GET /api/Repository/FileContent`
- **参数**: `id={warehouseId}:README.md`
- **期望**:
  - HTTP 200
  - 返回文件内容
  - 包含content, fileType, encoding字段

---

### 5. 同步管理接口 (P1)

#### TC-019: 手动触发同步(不存在的仓库)
- **接口**: `POST /api/Repository/ManualSync`
- **参数**: `warehouseId=non-existent-id`
- **期望**: 返回"仓库不存在"错误

#### TC-020: 获取同步记录
- **接口**: `GET /api/Repository/SyncRecords`
- **参数**: `warehouseId={warehouseId}&page=1&pageSize=10`
- **期望**:
  - HTTP 200
  - 返回分页的同步记录

#### TC-021: 更新同步配置
- **接口**: `POST /api/Repository/UpdateSync`
- **参数**: `id={warehouseId}`
- **请求体**:
```json
{
  "enableSync": true
}
```
- **期望**: HTTP 200,配置更新成功

---

### 6. 文档目录接口 (P1)

#### TC-022: 获取文档目录
- **接口**: `GET /api/Repository/DocumentCatalogs`
- **参数**: `warehouseId={warehouseId}`
- **期望**:
  - HTTP 200
  - 返回目录树结构

#### TC-023: 获取文档树
- **接口**: `GET /api/Warehouse/GetDocumentTree`
- **参数**: `warehouseId={warehouseId}`
- **期望**:
  - HTTP 200
  - 返回文档树节点

#### TC-024: 获取思维导图
- **接口**: `GET /api/Warehouse/minimap`
- **参数**: `warehouseId={warehouseId}`
- **期望**:
  - HTTP 200
  - 返回思维导图节点
  - 包含topic, direction, children字段

---

### 7. 统计分析接口 (P1)

#### TC-025: 获取仓库统计详情
- **接口**: `GET /api/Repository/RepositoryStats`
- **参数**: `id={warehouseId}`
- **期望**:
  - HTTP 200
  - 包含totalFiles, documentFiles, totalSize等

#### TC-026: 获取仓库日志
- **接口**: `GET /api/Repository/RepositoryLogs`
- **参数**: `warehouseId={warehouseId}&page=1&pageSize=20`
- **期望**:
  - HTTP 200
  - 返回访问日志分页数据

---

### 8. AI功能接口 (P2)

#### TC-027: AI生成README(无仓库ID)
- **接口**: `POST /api/ai/readme/generate`
- **请求体**:
```json
{
  "warehouseId": "non-existent-id"
}
```
- **期望**: 返回错误提示

#### TC-028: AI目录优化(缺少必填参数)
- **接口**: `POST /api/ai/catalog/optimize`
- **请求体**: `{}`
- **期望**: 返回参数验证错误

---

### 9. 批量操作接口 (P1)

#### TC-029: 批量操作(空列表)
- **接口**: `POST /api/Warehouse/BatchOperate`
- **请求体**:
```json
{
  "warehouseIds": [],
  "operation": "sync"
}
```
- **期望**: 返回"仓库ID列表不能为空"错误

#### TC-030: 批量操作(不支持的操作)
- **接口**: `POST /api/Warehouse/BatchOperate`
- **请求体**:
```json
{
  "warehouseIds": ["{warehouseId}"],
  "operation": "invalid-operation"
}
```
- **期望**: 操作失败

---

### 10. 边界和异常测试 (P1)

#### TC-031: 分页参数边界测试
- **接口**: `GET /api/Warehouse/WarehouseList`
- **参数**:
  - `page=0&pageSize=12` (无效页码)
  - `page=1&pageSize=0` (无效页大小)
  - `page=1&pageSize=1000` (过大页大小)
- **期望**:
  - page=0 应该返回验证错误
  - pageSize=0 应该返回验证错误
  - pageSize=1000 可能被限制或正常返回

#### TC-032: 必填参数缺失测试
- **接口**: `POST /api/Warehouse/SubmitWarehouse`
- **请求体**: `{}`
- **期望**: 返回参数验证错误

#### TC-033: 无效的仓库状态
- **接口**: `GET /api/Repository/RepositoryList`
- **参数**: `status=INVALID_STATUS`
- **期望**: 返回"无效的状态值"错误

#### TC-034: 特殊字符处理
- **接口**: `GET /api/Warehouse/WarehouseList`
- **参数**: `keyword=<script>alert('xss')</script>`
- **期望**:
  - 不触发XSS
  - 正常返回结果(无匹配)

---

## 测试执行流程

### 阶段1: 基础验证 (5分钟)
1. TC-001: 健康检查
2. TC-002: 获取空仓库列表
3. TC-009: 获取统计信息(空)

### 阶段2: 仓库管理流程 (10分钟)
1. TC-004: 提交无效URL
2. TC-005: 提交有效Git仓库
3. TC-006: 重复提交测试
4. TC-008: 获取最后一个仓库
5. TC-012: 获取仓库详情
6. TC-014: 更新仓库信息

### 阶段3: 文件操作测试 (8分钟)
1. TC-016: 获取文件列表
2. TC-017: 获取不存在的文件
3. TC-018: 获取文件内容
4. TC-022: 获取文档目录
5. TC-023: 获取文档树

### 阶段4: 同步和统计 (7分钟)
1. TC-020: 获取同步记录
2. TC-021: 更新同步配置
3. TC-025: 获取统计详情
4. TC-026: 获取日志

### 阶段5: 边界测试 (5分钟)
1. TC-031: 分页边界测试
2. TC-032: 必填参数测试
3. TC-033: 无效状态测试
4. TC-034: 特殊字符测试

---

## 测试数据准备

### 测试仓库URL列表
1. **公开小仓库**: `https://github.com/octocat/Hello-World.git`
2. **备选仓库**: `https://github.com/github/docs.git`

### 测试用户信息
- Email: `test@koalawiki.com`
- Organization: `TestOrg`

---

## 预期问题清单

### 可能的问题类型
1. **服务未启动**: 健康检查失败
2. **数据库未初始化**: 空表查询报错
3. **Git存储路径不存在**: `/data/koalawiki/git/` 目录创建
4. **依赖服务**: OpenAI API可能未配置
5. **认证问题**: 部分接口可能需要登录

### 问题判定标准
- **严重(Blocker)**: 核心流程完全无法使用
- **重要(Major)**: 主要功能异常,有替代方案
- **一般(Minor)**: 边界情况或提示不友好
- **建议(Trivial)**: 性能或体验优化建议

---

## 测试报告格式

### 问题记录模板
```
【接口名称】: GET /api/xxx
【测试用例】: TC-XXX
【问题现象】: 描述实际结果
【期望结果】: 描述期望结果
【严重程度】: Blocker/Major/Minor/Trivial
【复现步骤】:
  1. 步骤1
  2. 步骤2
【请求详情】:
  - URL: xxx
  - Method: GET/POST
  - Body: {}
【响应详情】:
  - Status: 200
  - Body: {}
【建议方案】: 修复建议
```

---

## 成功标准

### 通过标准
- **P0用例**: 100% 通过
- **P1用例**: ≥ 90% 通过
- **P2用例**: ≥ 80% 通过
- **无Blocker级问题**

### 测试完成条件
1. 所有P0和P1用例执行完成
2. 发现的问题已记录
3. 生成测试报告
4. 提交问题清单

---

**测试负责人**: Claude AI
**文档版本**: v1.0
**创建时间**: 2025-11-15

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)
via [Happy](https://happy.engineering)

Co-Authored-By: Claude <noreply@anthropic.com>
Co-Authored-By: Happy <yesreply@happy.engineering>
