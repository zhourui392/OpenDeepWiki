# OpenDeepWiki API 测试问题清单

**测试日期**: 2025-11-15
**测试人员**: Claude AI + Playwright MCP
**测试范围**: REST API接口自动化测试

---

## 📊 测试概览

- **总测试用例**: 10个
- **通过**: 8个 (80%)
- **失败**: 2个 (20%)
- **阻断级问题**: 0个
- **主要级问题**: 2个

---

## ❌ 发现的问题

### 问题1: 参数验证不当 - 页码为0时返回500错误

**【接口名称】**: GET /api/Warehouse/WarehouseList
**【测试用例】**: TC-009
**【问题现象】**: 当page=0时,接口返回HTTP 500而非400
**【期望结果】**: 应返回HTTP 400 Bad Request,提示参数验证失败
**【实际结果】**: HTTP 500 Internal Server Error
**【严重程度】**: Major (主要)

**【复现步骤】**:
```bash
curl "http://localhost:18091/api/Warehouse/WarehouseList?page=0&pageSize=12"
```

**【响应详情】**:
```json
{
  "timestamp": "2025-11-15T02:44:55.466+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/Warehouse/WarehouseList"
}
```

**【问题分析】**:
1. Controller使用了`@Min(1)`注解验证page参数
2. 但验证异常未被正确捕获,导致返回500而非400
3. 缺少全局的参数验证异常处理器

**【建议方案】**:

在WarehouseController.java:214添加参数验证:

```java
@GetMapping("/WarehouseList")
public ResponseEntity<Result<Page<WarehouseResponse>>> getWarehouseList(
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "12") @Min(1) int pageSize,
        @RequestParam(required = false) String keyword) {

    log.debug("获取仓库列表: page={}, pageSize={}, keyword={}", page, pageSize, keyword);

    try {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        // ... 后续逻辑
    } catch (Exception e) {
        log.error("获取仓库列表失败", e);
        return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
    }
}
```

**【影响范围】**: 所有分页查询接口

**【代码位置】**: koalawiki-web/src/main/java/ai/opendw/koalawiki/web/controller/WarehouseController.java:213

---

### 问题2: 必填参数校验返回400而非业务错误

**【接口名称】**: POST /api/Warehouse/SubmitWarehouse
**【测试用例】**: TC-010
**【问题现象】**: 提交空请求体时返回HTTP 400而非200+错误提示
**【期望结果】**: HTTP 200 + 业务错误消息(符合统一Result格式)
**【实际结果】**: HTTP 400 Bad Request
**【严重程度】**: Major (主要)

**【复现步骤】**:
```bash
curl -X POST "http://localhost:18091/api/Warehouse/SubmitWarehouse" \
  -H "Content-Type: application/json" \
  -d '{}'
```

**【响应详情】**:
```json
{
  "timestamp": "2025-11-15T02:44:55.501+00:00",
  "status": 400,
  "error": "Bad Request",
  "path": "/api/Warehouse/SubmitWarehouse"
}
```

**【问题分析】**:
1. 使用了`@Valid`注解验证SubmitWarehouseRequest
2. 验证失败时Spring返回默认的400错误
3. 前端无法获取统一格式的错误信息

**【建议方案】**:

添加全局异常处理器 `GlobalExceptionHandler.java`:

```java
package ai.opendw.koalawiki.web.config;

import ai.opendw.koalawiki.web.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理参数验证异常,返回标准的Result格式
 *
 * @author zhourui(V33215020)
 * @since 2025/11/15
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理@Valid验证失败异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(
            MethodArgumentNotValidException e) {

        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("参数验证失败: {}", message);
        return ResponseEntity.ok(Result.error("参数验证失败: " + message));
    }

    /**
     * 处理@Validated验证失败异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolationException(
            ConstraintViolationException e) {

        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));

        log.warn("参数约束验证失败: {}", message);
        return ResponseEntity.ok(Result.error("参数验证失败: " + message));
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("参数绑定失败: {}", message);
        return ResponseEntity.ok(Result.error("参数绑定失败: " + message));
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.ok(Result.error("系统异常: " + e.getMessage()));
    }
}
```

**【影响范围】**: 所有使用@Valid/@Validated的接口

**【代码位置】**: 需要新建文件 koalawiki-web/src/main/java/ai/opendw/koalawiki/web/config/GlobalExceptionHandler.java

---

## ✅ 测试通过的功能

### 核心功能 (P0级别)

1. ✅ **TC-001: 健康检查** - 响应时间<200ms,数据格式正确
2. ✅ **TC-002: 获取仓库列表** - 分页参数正确,返回1个仓库
3. ✅ **TC-003: 关键字搜索** - 搜索功能正常
4. ✅ **TC-004: 无效URL验证** - 正确返回错误提示
5. ✅ **TC-005: 统计信息** - 返回仓库统计数据
6. ✅ **TC-006: 获取最后仓库** - 返回最新创建的仓库
7. ✅ **TC-007: 不存在的仓库** - 正确返回"仓库不存在"
8. ✅ **TC-008: 分支列表** - 返回默认分支列表

---

## 📝 改进建议

### 1. 添加全局异常处理器 (高优先级)

**目的**: 统一API错误响应格式
**实现**: 创建GlobalExceptionHandler类
**效果**: 所有异常都返回统一的Result<T>格式

### 2. 增强参数验证 (高优先级)

**目的**: 在Controller层添加防御性编程
**实现**:
- 添加参数范围检查
- 添加业务规则验证
- 提供友好的错误提示

### 3. 完善错误码体系 (中优先级)

**目的**: 前端可根据错误码做差异化处理
**实现**:
```java
public enum ErrorCode {
    SUCCESS(0, "成功"),
    PARAM_ERROR(400, "参数错误"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器错误"),
    WAREHOUSE_NOT_FOUND(1001, "仓库不存在"),
    WAREHOUSE_EXISTS(1002, "仓库已存在");

    private final int code;
    private final String message;
}
```

### 4. 添加接口文档 (中优先级)

**工具**: Swagger/OpenAPI 3.0
**配置**:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-ui</artifactId>
    <version>1.7.0</version>
</dependency>
```

### 5. 添加API测试用例 (中优先级)

**工具**: Spring Boot Test + MockMvc
**覆盖**: 所有Controller的正常和异常流程

---

## 🎯 下一步行动

### 立即修复 (本周内)
1. ✅ 创建GlobalExceptionHandler - 预计1小时
2. ✅ 修复参数验证问题 - 预计30分钟
3. ⏳ 添加单元测试覆盖 - 预计2小时

### 计划改进 (下周)
1. ⏳ 引入Swagger文档 - 预计1小时
2. ⏳ 完善错误码体系 - 预计2小时
3. ⏳ 增加集成测试 - 预计4小时

---

## 📊 测试覆盖率

### 按优先级分类
- **P0 (核心功能)**: 8/8 测试通过 (100%)
- **P1 (重要功能)**: 0/15 待测试
- **P2 (辅助功能)**: 0/11 待测试

### 按模块分类
- **WarehouseController**: 6/16 接口已测试 (37.5%)
- **RepositoryController**: 1/16 接口已测试 (6.25%)
- **其他Controller**: 0% 未测试

---

## 📌 附录

### 测试环境信息
- **操作系统**: Linux 6.8.0-71-generic
- **JDK版本**: 1.8
- **Spring Boot版本**: 2.7.18
- **数据库**: H2 (内存模式)
- **服务端口**: 18091

### 相关文档
- API测试计划: `API_TEST_PLAN.md`
- 详细测试结果: `test_results_detailed.md`
- 测试脚本: `/tmp/api_test.sh`

---

**报告生成时间**: 2025-11-15 10:45:00
**下次测试计划**: 2025-11-16 完成剩余P1接口测试

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)
via [Happy](https://happy.engineering)

Co-Authored-By: Claude <noreply@anthropic.com>
Co-Authored-By: Happy <yesreply@happy.engineering>
