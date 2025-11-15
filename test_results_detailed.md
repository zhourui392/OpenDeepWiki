# OpenDeepWiki API 测试报告

**测试时间**: 2025-11-15 10:44:55
**Base URL**: http://localhost:18091

---

# 测试用例执行

## TC-001: 健康检查

- **接口**: `GET /api/health`
- **HTTP状态码**: 200 (期望: 200)
- **响应**: ```json
```
- **结果**: ✅ PASS

## TC-002: 获取仓库列表-默认参数

- **接口**: `GET /api/Warehouse/WarehouseList?page=1&pageSize=12`
- **HTTP状态码**: 200 (期望: 200)
- **响应**: ```json
```
- **结果**: ✅ PASS

## TC-003: 获取仓库列表-关键字搜索

- **接口**: `GET /api/Warehouse/WarehouseList?page=1&pageSize=12&keyword=test`
- **HTTP状态码**: 200 (期望: 200)
- **响应**: ```json
```
- **结果**: ✅ PASS

## TC-004: 提交仓库-无效URL

- **接口**: `POST /api/Warehouse/SubmitWarehouse`
- **HTTP状态码**: 200 (期望: 200)
- **响应**: ```json
```
- **结果**: ✅ PASS

## TC-005: 获取仓库统计信息

- **接口**: `GET /api/Warehouse/Stats`
- **HTTP状态码**: 200 (期望: 200)
- **响应**: ```json
```
- **结果**: ✅ PASS

## TC-006: 获取最后一个仓库

- **接口**: `GET /api/Warehouse/LastWarehouse`
- **HTTP状态码**: 200 (期望: 200)
- **响应**: ```json
```
- **结果**: ✅ PASS

## TC-007: 获取仓库详情-不存在的ID

- **接口**: `GET /api/Repository/Repository?id=non-existent-id`
- **HTTP状态码**: 200 (期望: 200)
- **响应**: ```json
```
- **结果**: ✅ PASS

## TC-008: 获取分支列表

- **接口**: `GET /api/Warehouse/BranchList?address=https://github.com/octocat/Hello-World.git`
- **HTTP状态码**: 200 (期望: 200)
- **响应**: ```json
```
- **结果**: ✅ PASS

## TC-009: 边界测试-页码为0

- **接口**: `GET /api/Warehouse/WarehouseList?page=0&pageSize=12`
- **HTTP状态码**: 500 (期望: 400)
- **响应**: ```json
```
- **结果**: ❌ FAIL

## TC-010: 提交仓库-空请求体

- **接口**: `POST /api/Warehouse/SubmitWarehouse`
- **HTTP状态码**: 400 (期望: 200)
- **响应**: ```json
```
- **结果**: ❌ FAIL

---

# 测试总结

- **总计**: 10 个测试用例
- **通过**: 8 个 (80.0%)
- **失败**: 2 个 (20.0%)

## ⚠️ 存在失败的测试用例

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)
via [Happy](https://happy.engineering)
