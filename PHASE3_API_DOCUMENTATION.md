# Phase 3 API文档

> **文档版本**: 1.0
> **创建时间**: 2025-11-13
> **Base URL**: `http://localhost:8080`
> **API版本**: v1

---

## 📋 目录

1. [AI功能API](#ai功能api)
2. [文档目录API](#文档目录api)
3. [统计分析API](#统计分析api)
4. [通用响应格式](#通用响应格式)
5. [错误码说明](#错误码说明)

---

## 🤖 AI功能API

### 1.1 生成README

生成仓库的README文档

**接口地址**: `POST /api/ai/readme/generate`

**请求参数**:
```json
{
  "repositoryName": "my-project",
  "owner": "johndoe",
  "description": "A sample project",
  "language": "en",
  "fileStructure": "src/\n  main/\n    java/",
  "techStack": ["Java", "Spring Boot", "MySQL"]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| repositoryName | String | 是 | 仓库名称 |
| owner | String | 是 | 仓库所有者 |
| description | String | 否 | 仓库描述 |
| language | String | 否 | 语言(en/zh，默认en) |
| fileStructure | String | 否 | 文件结构 |
| techStack | String[] | 否 | 技术栈列表 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "readme": "# my-project\n\nA sample project...",
    "generatedAt": "2025-11-13T10:30:00",
    "tokensUsed": 1250
  }
}
```

---

### 1.2 生成简单README

快速生成简化版README

**接口地址**: `GET /api/ai/readme/generate-simple`

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| warehouseId | String | 是 | 仓库ID |

**示例**: `GET /api/ai/readme/generate-simple?warehouseId=warehouse-123`

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "readme": "# Project Name\n\n## Quick Start\n...",
    "generatedAt": "2025-11-13T10:35:00"
  }
}
```

---

### 1.3 优化文档目录

AI智能优化文档目录结构

**接口地址**: `POST /api/ai/catalog/optimize`

**请求参数**:
```json
{
  "catalogData": "{\"files\": [...]}",
  "maxFiles": 50,
  "rules": ["exclude_build", "exclude_dependencies"]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| catalogData | String | 是 | 原始目录JSON |
| maxFiles | Integer | 是 | 最大文件数 |
| rules | String[] | 否 | 优化规则 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "optimizedCatalog": "{\"files\": [...]}",
    "removedCount": 35,
    "suggestions": [
      "Removed 25 build artifacts",
      "Removed 10 dependency files"
    ]
  }
}
```

---

### 1.4 生成文档摘要

为文档生成智能摘要

**接口地址**: `POST /api/ai/document/summarize`

**请求参数**:
```json
{
  "documentId": "doc-123",
  "maxLength": 200
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| documentId | String | 是 | 文档ID |
| maxLength | Integer | 否 | 最大长度(默认200) |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "summary": "This document describes the REST API...",
    "keywords": ["API", "REST", "Authentication"],
    "readingTime": 5
  }
}
```

---

### 1.5 批量生成摘要

批量为多个文档生成摘要

**接口地址**: `POST /api/ai/document/batch-summarize`

**请求参数**:
```json
{
  "documentIds": ["doc-1", "doc-2", "doc-3"],
  "maxLength": 200
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "summaries": [
      {
        "documentId": "doc-1",
        "summary": "...",
        "keywords": ["..."]
      },
      {
        "documentId": "doc-2",
        "summary": "...",
        "keywords": ["..."]
      }
    ],
    "totalProcessed": 3
  }
}
```

---

### 1.6 AI问答

基于文档内容的智能问答

**接口地址**: `POST /api/ai/qa/ask`

**请求参数**:
```json
{
  "question": "How do I install this project?",
  "warehouseId": "warehouse-123",
  "sessionId": "session-456"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| question | String | 是 | 问题文本 |
| warehouseId | String | 是 | 仓库ID |
| sessionId | String | 否 | 会话ID(用于多轮对话) |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "answer": "To install this project, run `npm install`...",
    "confidence": 0.95,
    "references": [
      {
        "documentId": "doc-readme",
        "title": "README.md",
        "snippet": "## Installation\n\nRun `npm install`..."
      }
    ],
    "relatedQuestions": [
      "How do I run the project?",
      "What are the system requirements?"
    ]
  }
}
```

---

### 1.7 生成标签

为文档内容生成标签

**接口地址**: `POST /api/ai/tags/generate`

**请求参数**:
```json
{
  "content": "This is a Spring Boot REST API project...",
  "maxTags": 5
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "tags": ["Spring Boot", "REST API", "Java", "Backend", "Microservices"]
  }
}
```

---

### 1.8 分析技术栈

识别项目的技术栈

**接口地址**: `POST /api/ai/tech-stack/analyze`

**请求参数**:
```json
{
  "fileList": ["pom.xml", "package.json", "Dockerfile"],
  "warehouseId": "warehouse-123"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "techStack": [
      "Java",
      "Maven",
      "Spring Boot",
      "Node.js",
      "Docker"
    ],
    "primaryLanguage": "Java",
    "frameworks": ["Spring Boot"],
    "tools": ["Maven", "Docker"]
  }
}
```

---

### 1.9 生成项目描述

智能生成项目描述

**接口地址**: `GET /api/ai/project/describe`

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| warehouseId | String | 是 | 仓库ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "description": "A modern web application built with Spring Boot and React, providing REST API services and real-time data processing capabilities."
  }
}
```

---

## 📁 文档目录API

### 2.1 获取目录树

获取仓库的完整文档目录树

**接口地址**: `GET /api/catalog/{warehouseId}`

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| warehouseId | String | 是 | 仓库ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "root",
    "name": "root",
    "type": "directory",
    "children": [
      {
        "id": "catalog-1",
        "name": "README.md",
        "type": "file",
        "path": "/README.md",
        "documentId": "doc-1"
      },
      {
        "id": "catalog-2",
        "name": "src",
        "type": "directory",
        "children": [...]
      }
    ]
  }
}
```

---

### 2.2 刷新目录

重新扫描并刷新目录

**接口地址**: `POST /api/catalog/{warehouseId}/refresh`

**响应示例**:
```json
{
  "code": 200,
  "message": "Catalog refreshed successfully",
  "data": null
}
```

---

### 2.3 搜索目录

在目录中搜索文件/文件夹

**接口地址**: `POST /api/catalog/{warehouseId}/search`

**请求参数**:
```json
{
  "keyword": "README",
  "scope": "TITLE",
  "maxResults": 10
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 是 | 搜索关键词 |
| scope | String | 否 | 搜索范围(TITLE/CONTENT/ALL) |
| maxResults | Integer | 否 | 最大结果数 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "id": "catalog-1",
        "name": "README.md",
        "path": "/README.md",
        "type": "file",
        "relevance": 0.95,
        "highlight": "<em>README</em>.md"
      }
    ],
    "totalCount": 1,
    "suggestion": null
  }
}
```

---

### 2.4 排序目录

按指定策略排序目录

**接口地址**: `POST /api/catalog/{warehouseId}/sort`

**请求参数**:
```json
{
  "strategy": "alphabetical",
  "ascending": true
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| strategy | String | 是 | 排序策略(alphabetical/priority/custom) |
| ascending | Boolean | 否 | 升序(默认true) |

---

### 2.5 创建目录

创建新的目录节点

**接口地址**: `POST /api/catalog`

**请求参数**:
```json
{
  "warehouseId": "warehouse-123",
  "parentId": "parent-1",
  "name": "new-folder",
  "type": "directory",
  "path": "/new-folder"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "Catalog created successfully",
  "data": {
    "id": "catalog-new",
    "name": "new-folder",
    "createdAt": "2025-11-13T10:00:00"
  }
}
```

---

### 2.6 更新目录

更新目录节点信息

**接口地址**: `PUT /api/catalog/{catalogId}`

**请求参数**:
```json
{
  "name": "updated-name",
  "order": 10
}
```

---

### 2.7 删除目录

删除目录节点(软删除)

**接口地址**: `DELETE /api/catalog/{catalogId}`

**响应示例**:
```json
{
  "code": 200,
  "message": "Catalog deleted successfully",
  "data": null
}
```

---

### 2.8 分页查询目录

分页获取目录列表

**接口地址**: `GET /api/catalog/{warehouseId}/list`

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码(从0开始，默认0) |
| size | Integer | 否 | 每页大小(默认20) |
| sort | String | 否 | 排序字段(如:name,asc) |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [...],
    "totalElements": 100,
    "totalPages": 5,
    "number": 0,
    "size": 20
  }
}
```

---

### 2.9 获取子目录

获取指定节点的子目录

**接口地址**: `GET /api/catalog/children/{parentId}`

---

### 2.10 统计目录数量

统计仓库的目录数量

**接口地址**: `GET /api/catalog/{warehouseId}/count`

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 256,
    "files": 180,
    "directories": 76
  }
}
```

---

## 📊 统计分析API

### 3.1 获取每日统计

获取指定日期的统计数据

**接口地址**: `GET /api/statistics/daily/{warehouseId}`

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| date | String | 否 | 日期(yyyy-MM-dd，默认今天) |

**示例**: `GET /api/statistics/daily/warehouse-123?date=2025-11-13`

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "statisticsDate": "2025-11-13",
    "warehouseId": "warehouse-123",
    "viewCount": 1500,
    "uniqueUserCount": 320,
    "uniqueIpCount": 280,
    "documentViewCount": 1200,
    "searchCount": 200,
    "downloadCount": 100,
    "avgResponseTime": 125.5,
    "maxResponseTime": 5000,
    "minResponseTime": 10,
    "totalRequests": 2000,
    "successRequests": 1950,
    "failedRequests": 50,
    "errorRate": 2.5,
    "actionCounts": {
      "VIEW": 1500,
      "SEARCH": 200,
      "DOWNLOAD": 100,
      "CREATE": 50,
      "UPDATE": 100,
      "DELETE": 50
    },
    "topDocuments": [
      {
        "docId": "doc-1",
        "title": "README.md",
        "views": 500
      },
      {
        "docId": "doc-2",
        "title": "API Guide",
        "views": 300
      }
    ]
  }
}
```

---

### 3.2 获取趋势数据

获取最近N天的趋势数据

**接口地址**: `GET /api/statistics/trend/{warehouseId}`

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| days | Integer | 否 | 天数(默认7) |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "labels": ["11-07", "11-08", "11-09", "11-10", "11-11", "11-12", "11-13"],
    "pv": [1200, 1350, 1400, 1100, 1600, 1450, 1500],
    "uv": [250, 280, 290, 220, 340, 310, 320],
    "avgResponseTime": [120, 125, 130, 115, 135, 128, 125]
  }
}
```

---

### 3.3 获取统计摘要

获取综合统计摘要(今天、昨天、增长率、周汇总)

**接口地址**: `GET /api/statistics/summary/{warehouseId}`

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "todayPV": 1500,
    "todayUV": 320,
    "yesterdayPV": 1450,
    "yesterdayUV": 310,
    "pvGrowth": 3.45,
    "uvGrowth": 3.23,
    "week": {
      "totalPV": 9600,
      "avgPV": 1371.4,
      "totalUV": 1970,
      "avgUV": 281.4,
      "totalRequests": 12000,
      "avgErrorRate": 2.3
    },
    "topDocuments": [
      {
        "docId": "doc-1",
        "title": "README.md",
        "views": 3500,
        "growth": 12.5
      }
    ]
  }
}
```

---

## 📋 通用响应格式

所有API遵循统一的响应格式:

### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {...}
}
```

### 错误响应
```json
{
  "code": 400,
  "message": "Invalid parameter: warehouseId is required",
  "data": null,
  "timestamp": "2025-11-13T10:00:00",
  "path": "/api/catalog/invalid"
}
```

---

## 🚨 错误码说明

| 错误码 | 说明 | 示例场景 |
|--------|------|----------|
| 200 | 成功 | 操作成功 |
| 400 | 请求参数错误 | 缺少必填参数、参数类型错误 |
| 401 | 未授权 | Token无效或过期 |
| 403 | 禁止访问 | 无权限访问资源 |
| 404 | 资源不存在 | 仓库ID、文档ID不存在 |
| 429 | 请求过于频繁 | 超过限流阈值 |
| 500 | 服务器内部错误 | 系统异常、数据库错误 |
| 503 | 服务不可用 | AI服务不可用、系统维护 |

### 详细错误码

| 业务错误码 | 说明 |
|------------|------|
| 1001 | AI服务调用失败 |
| 1002 | Token额度不足 |
| 1003 | 文档内容为空 |
| 2001 | 目录不存在 |
| 2002 | 目录名称重复 |
| 3001 | 统计数据不存在 |
| 3002 | 日期格式错误 |

---

## 🔧 API调用示例

### cURL示例

```bash
# 生成README
curl -X POST http://localhost:8080/api/ai/readme/generate \
  -H "Content-Type: application/json" \
  -d '{
    "repositoryName": "my-project",
    "owner": "johndoe",
    "language": "en"
  }'

# 获取目录树
curl -X GET http://localhost:8080/api/catalog/warehouse-123

# 获取统计摘要
curl -X GET http://localhost:8080/api/statistics/summary/warehouse-123
```

### JavaScript示例

```javascript
// 使用fetch API
async function generateReadme(params) {
  const response = await fetch('http://localhost:8080/api/ai/readme/generate', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(params)
  });

  const result = await response.json();
  if (result.code === 200) {
    console.log('README generated:', result.data.readme);
  } else {
    console.error('Error:', result.message);
  }
}
```

### Java示例

```java
// 使用RestTemplate
RestTemplate restTemplate = new RestTemplate();

GenerateReadmeRequest request = new GenerateReadmeRequest();
request.setRepositoryName("my-project");
request.setOwner("johndoe");

Result<ReadmeResponse> result = restTemplate.postForObject(
    "http://localhost:8080/api/ai/readme/generate",
    request,
    new ParameterizedTypeReference<Result<ReadmeResponse>>() {}
);

if (result.getCode() == 200) {
    System.out.println("README: " + result.getData().getReadme());
}
```

---

## 📝 认证和授权

### JWT Token认证

所有API请求需要在Header中携带JWT Token:

```
Authorization: Bearer <your-jwt-token>
```

### 获取Token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password123"
  }'
```

### Token刷新

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Authorization: Bearer <refresh-token>"
```

---

## 🚀 速率限制

| API类型 | 限制 |
|---------|------|
| AI相关API | 10次/分钟 |
| 目录管理API | 100次/分钟 |
| 统计查询API | 60次/分钟 |

超过限制返回429状态码。

---

## 📖 更多资源

- [快速开始指南](./PHASE3_QUICKSTART.md)
- [部署文档](./PHASE3_DEPLOYMENT.md)
- [性能优化](./PHASE3_PERFORMANCE_OPTIMIZATION.md)
- [常见问题](./PHASE3_FAQ.md)

---

**编写者**: OpenDeepWiki Team
**日期**: 2025-11-13
**版本**: 1.0
