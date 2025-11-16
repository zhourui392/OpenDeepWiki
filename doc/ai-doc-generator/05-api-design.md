# API接口设计

> 文档来源：定义前后端API接口规范
>
> 最后更新：2025-11-16

## 1. API设计原则

- RESTful风格
- 统一响应格式
- 版本控制 (`/api/v1/...`)
- 权限控制 (JWT Token)
- 限流保护

## 2. 通用响应格式

```typescript
interface ApiResponse<T> {
  code: number;        // 200=成功, 其他=错误码
  message: string;     // 响应消息
  data: T;             // 响应数据
  timestamp: number;   // 时间戳
}

// 分页响应
interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}
```

## 3. AI文档相关API

### 3.1 触发文档生成

```
POST /api/v1/warehouses/{warehouseId}/generate-docs
```

**Request Body**:
```json
{
  "type": "FULL",  // FULL | INCREMENTAL | SINGLE_FILE
  "filePatterns": ["**/*.java", "**/*.py"],
  "excludePatterns": ["**/test/**"],
  "config": {
    "model": "gpt-3.5-turbo",
    "docTypes": ["CLASS", "API"],
    "language": "zh-CN"
  }
}
```

**Response**:
```json
{
  "code": 200,
  "message": "文档生成任务已创建",
  "data": {
    "taskId": "task-uuid-123",
    "status": "PENDING",
    "totalFiles": 150,
    "estimatedTime": 300000
  }
}
```

### 3.2 查询生成任务状态

```
GET /api/v1/generation-tasks/{taskId}
```

**Response**:
```json
{
  "code": 200,
  "data": {
    "id": "task-uuid-123",
    "warehouseId": "warehouse-id",
    "status": "RUNNING",
    "progress": 45.5,
    "totalFiles": 150,
    "completedFiles": 68,
    "failedFiles": 2,
    "currentFile": "src/main/java/UserService.java",
    "startedAt": 1700000000000,
    "estimatedTimeRemaining": 180000
  }
}
```

### 3.3 获取AI文档列表

```
GET /api/v1/warehouses/{warehouseId}/ai-documents?page=1&size=20&docType=CLASS&status=COMPLETED
```

**Query Parameters**:
- `page`: 页码(从1开始)
- `size`: 每页大小
- `docType`: CLASS | METHOD | API | MODULE | ARCHITECTURE
- `status`: GENERATING | COMPLETED | FAILED
- `keyword`: 搜索关键词

**Response**:
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": "doc-id-1",
        "title": "UserService",
        "docType": "CLASS",
        "language": "java",
        "sourceFile": "src/main/java/UserService.java",
        "status": "COMPLETED",
        "qualityScore": 0.95,
        "isReviewed": false,
        "createdAt": 1700000000000
      }
    ],
    "page": 1,
    "size": 20,
    "total": 150,
    "totalPages": 8
  }
}
```

### 3.4 获取单个AI文档详情

```
GET /api/v1/ai-documents/{documentId}
```

**Response**:
```json
{
  "code": 200,
  "data": {
    "id": "doc-id-1",
    "warehouseId": "warehouse-id",
    "sourceFile": "src/main/java/UserService.java",
    "title": "UserService - 用户服务类",
    "content": "# UserService\\n\\n## 概述\\n这是用户服务类...",
    "docType": "CLASS",
    "language": "java",
    "codeElements": {
      "className": "UserService",
      "packageName": "com.example.service",
      "methods": ["createUser", "updateUser", "deleteUser"]
    },
    "generationModel": "gpt-4",
    "generationTokens": 1500,
    "qualityScore": 0.95,
    "status": "COMPLETED",
    "version": 1,
    "isReviewed": false,
    "createdAt": 1700000000000,
    "updatedAt": 1700000000000
  }
}
```

### 3.5 编辑AI文档

```
PUT /api/v1/ai-documents/{documentId}
```

**Request Body**:
```json
{
  "title": "Updated Title",
  "content": "# Updated Content\\n\\n...",
  "tags": "service,user,crud"
}
```

**Response**:
```json
{
  "code": 200,
  "message": "文档已更新",
  "data": {
    "id": "doc-id-1",
    "version": 2,
    "updatedAt": 1700000100000
  }
}
```

### 3.6 重新生成文档

```
POST /api/v1/ai-documents/{documentId}/regenerate
```

**Request Body**:
```json
{
  "model": "gpt-4",  // 可选,使用不同模型
  "reason": "源代码已更新"
}
```

**Response**:
```json
{
  "code": 200,
  "message": "文档重新生成中",
  "data": {
    "taskId": "task-uuid-456",
    "documentId": "doc-id-1"
  }
}
```

### 3.7 获取文档版本历史

```
GET /api/v1/ai-documents/{documentId}/versions
```

**Response**:
```json
{
  "code": 200,
  "data": [
    {
      "id": "version-id-1",
      "version": 2,
      "changeType": "MANUAL_EDIT",
      "changeReason": "修正错误",
      "createdAt": 1700000100000,
      "createdBy": "user-id"
    },
    {
      "id": "version-id-2",
      "version": 1,
      "changeType": "CREATE",
      "createdAt": 1700000000000
    }
  ]
}
```

### 3.8 对比文档版本

```
GET /api/v1/ai-documents/{documentId}/versions/compare?from=1&to=2
```

**Response**:
```json
{
  "code": 200,
  "data": {
    "fromVersion": 1,
    "toVersion": 2,
    "diff": "--- Version 1\\n+++ Version 2\\n@@ -10,7 +10,7 @@\\n...",
    "changes": {
      "added": 15,
      "removed": 8,
      "modified": 23
    }
  }
}
```

### 3.9 搜索AI文档

```
GET /api/v1/ai-documents/search?keyword=UserService&warehouseId=xxx&page=1&size=20
```

**Response**:
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": "doc-id-1",
        "title": "<mark>UserService</mark>",
        "snippet": "...这是<mark>UserService</mark>类的实现...",
        "score": 0.95,
        "highlights": ["UserService", "用户服务"]
      }
    ],
    "total": 5
  }
}
```

## 4. 智能问答API

### 4.1 提问

```
POST /api/v1/qa/ask
```

**Request Body**:
```json
{
  "question": "UserService类是如何处理用户注册的？",
  "warehouseId": "warehouse-id",
  "sessionId": "session-uuid",  // 可选,用于多轮对话
  "context": {
    "documentIds": ["doc-id-1", "doc-id-2"]  // 可选,指定上下文文档
  }
}
```

**Response**:
```json
{
  "code": 200,
  "data": {
    "answer": "根据代码分析,UserService类通过以下步骤处理用户注册:\\n\\n1. 验证用户输入...\\n2. 检查用户名是否存在...\\n3. 加密密码...\\n4. 保存到数据库...",
    "relatedDocuments": [
      {
        "id": "doc-id-1",
        "title": "UserService",
        "relevance": 0.95
      }
    ],
    "codeSnippets": [
      {
        "file": "UserService.java",
        "lineStart": 45,
        "lineEnd": 60,
        "code": "public User register(String username, String password) {..."
      }
    ],
    "confidence": 0.92,
    "sessionId": "session-uuid"
  }
}
```

### 4.2 获取对话历史

```
GET /api/v1/qa/sessions/{sessionId}/history
```

**Response**:
```json
{
  "code": 200,
  "data": {
    "sessionId": "session-uuid",
    "conversations": [
      {
        "id": "conv-id-1",
        "question": "UserService类是做什么的？",
        "answer": "UserService是用户服务类...",
        "createdAt": 1700000000000
      }
    ]
  }
}
```

## 5. 统计和监控API

### 5.1 获取仓库文档统计

```
GET /api/v1/warehouses/{warehouseId}/doc-stats
```

**Response**:
```json
{
  "code": 200,
  "data": {
    "totalDocuments": 150,
    "byType": {
      "CLASS": 80,
      "METHOD": 50,
      "API": 15,
      "MODULE": 5
    },
    "byStatus": {
      "COMPLETED": 145,
      "GENERATING": 3,
      "FAILED": 2
    },
    "byLanguage": {
      "java": 100,
      "python": 30,
      "javascript": 20
    },
    "qualityAverage": 0.88,
    "totalTokensUsed": 500000,
    "estimatedCost": 15.50
  }
}
```

### 5.2 获取Token使用统计

```
GET /api/v1/statistics/token-usage?startDate=2024-01-01&endDate=2024-01-31&groupBy=day
```

**Response**:
```json
{
  "code": 200,
  "data": {
    "period": {
      "start": "2024-01-01",
      "end": "2024-01-31"
    },
    "totalTokens": 5000000,
    "totalCost": 150.00,
    "byModel": {
      "gpt-4": {
        "tokens": 1000000,
        "cost": 90.00
      },
      "gpt-3.5-turbo": {
        "tokens": 4000000,
        "cost": 60.00
      }
    },
    "dailyUsage": [
      {
        "date": "2024-01-01",
        "tokens": 150000,
        "cost": 4.50
      }
    ]
  }
}
```

## 6. WebSocket实时通知

### 6.1 连接

```javascript
// 前端连接WebSocket
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, frame => {
  // 订阅文档生成进度
  stompClient.subscribe('/user/topic/doc-generation-progress', message => {
    const progress = JSON.parse(message.body);
    updateProgress(progress);
  });
});
```

### 6.2 进度消息格式

```json
{
  "taskId": "task-uuid-123",
  "total": 150,
  "completed": 75,
  "failed": 2,
  "currentFile": "src/main/java/UserService.java",
  "progress": 50.0,
  "status": "RUNNING"
}
```

## 7. 错误码定义

| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 200 | 成功 | - |
| 400 | 请求参数错误 | 检查请求参数 |
| 401 | 未授权 | 需要登录 |
| 403 | 无权限 | 联系管理员 |
| 404 | 资源不存在 | 检查资源ID |
| 429 | 请求过于频繁 | 稍后重试 |
| 500 | 服务器错误 | 联系技术支持 |
| 1001 | Token配额不足 | 充值或等待配额重置 |
| 1002 | AI服务调用失败 | 稍后重试 |
| 1003 | 文档生成失败 | 检查源代码 |
| 1004 | 文档正在生成中 | 等待完成 |

## 8. 下一步

请继续阅读：
- [实施计划与路线图](./06-implementation-roadmap.md)
