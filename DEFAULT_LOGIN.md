# OpenDeepWiki - 默认登录账号

## 默认账号信息

由于项目当前配置使用 Hibernate 自动创建表结构但不创建默认用户，您需要通过以下方式之一创建登录账号：

---

## 方法一：使用 H2 控制台手动创建用户（推荐）

### 1. 访问 H2 控制台

启动应用后，访问：**http://localhost:18091/h2-console**

**连接信息：**
- JDBC URL: `jdbc:h2:mem:koalawiki`
- 用户名: `sa`
- 密码: (留空)

### 2. 执行以下 SQL 创建管理员账号

```sql
-- 1. 创建默认角色（如果尚未创建）
INSERT INTO roles (id, name, description, is_system, created_at) VALUES
('admin-role-uuid', 'Admin', '系统管理员，拥有所有权限', 1, CURRENT_TIMESTAMP()),
('user-role-uuid', 'User', '普通用户，拥有基本权限', 1, CURRENT_TIMESTAMP()),
('guest-role-uuid', 'Guest', '访客，拥有只读权限', 1, CURRENT_TIMESTAMP());

-- 2. 创建管理员用户
-- 密码: admin123 (MD5: 0192023a7bbd73250516f069df18b500)
INSERT INTO users (id, name, email, password, bio, created_at) VALUES
('default-admin-uuid-0001', 'Administrator', 'admin@koalawiki.com', '0192023a7bbd73250516f069df18b500', '系统默认管理员账号', CURRENT_TIMESTAMP());

-- 3. 为管理员分配 Admin 角色
INSERT INTO user_in_roles (id, user_id, role_id, created_at) VALUES
('admin-role-mapping-0001', 'default-admin-uuid-0001', 'admin-role-uuid', CURRENT_TIMESTAMP());

-- 4. (可选) 创建演示用户
-- 密码: demo123 (MD5: ab86a1e1ef70dff97959067b723c5c24)
INSERT INTO users (id, name, email, password, bio, created_at) VALUES
('default-demo-user-uuid-0001', 'Demo User', 'demo@koalawiki.com', 'ab86a1e1ef70dff97959067b723c5c24', '演示用户账号', CURRENT_TIMESTAMP());

-- 5. 为演示用户分配 User 角色
INSERT INTO user_in_roles (id, user_id, role_id, created_at) VALUES
('demo-role-mapping-0001', 'default-demo-user-uuid-0001', 'user-role-uuid', CURRENT_TIMESTAMP());
```

### 3. 登录信息

创建成功后，您可以使用以下账号登录：

**管理员账号：**
- 邮箱：`admin@koalawiki.com`
- 密码：`admin123`
- 角色：Admin（系统管理员）

**演示账号：**
- 邮箱：`demo@koalawiki.com`
- 密码：`demo123`
- 角色：User（普通用户）

---

## 方法二：使用注册功能

如果应用提供了用户注册功能，您可以：

1. 访问注册页面：http://localhost:18091/register
2. 填写注册信息创建新账号
3. 使用注册的邮箱和密码登录

---

## 密码加密说明

**当前加密方式：** MD5（用于演示，不建议生产环境使用）

**密码加密工具类位置：** `koalawiki-core/src/main/java/ai/opendw/koalawiki/core/util/PasswordUtil.java`

如需生成其他密码的 MD5 值：

```bash
# Linux/Mac
echo -n "your_password" | md5sum

# 或使用 Python
python3 -c "import hashlib; print(hashlib.md5(b'your_password').hexdigest())"
```

### 常见密码 MD5 值

| 密码 | MD5 值 |
|------|--------|
| admin123 | 0192023a7bbd73250516f069df18b500 |
| demo123 | ab86a1e1ef70dff97959067b723c5c24 |
| password | 5f4dcc3b5aa765d61d8327deb882cf99 |
| 123456 | e10adc3949ba59abbe56e057f20f883e |

---

## 安全建议

⚠️ **重要安全提示：**

1. **首次登录后立即修改默认密码**
2. **生产环境请使用 BCrypt 替代 MD5**
   - MD5 已不再安全，容易被彩虹表攻击
   - 建议升级到 Spring Security 的 BCryptPasswordEncoder
3. **删除演示账号**
   - 生产环境部署前删除 demo 账号
4. **启用更强的认证机制**
   - 考虑添加双因素认证（2FA）
   - 实施密码复杂度策略
   - 限制登录失败次数

---

## 故障排除

### 问题1：无法连接到 H2 控制台

**解决方案：**
检查 `application.yml` 确保 H2 控制台已启用：

```yaml
spring:
  h2:
    console:
      enabled: true
      path: /h2-console
```

### 问题2：插入 SQL 失败

**可能原因：**
- 表未创建：确保应用已启动并且 Hibernate 已创建表结构
- 主键冲突：检查 ID 是否已存在

**解决方案：**
```sql
-- 先查询是否存在
SELECT * FROM users WHERE email = 'admin@koalawiki.com';

-- 如果存在，先删除再插入
DELETE FROM users WHERE email = 'admin@koalawiki.com';
```

### 问题3：登录失败

**检查项：**
1. 确认用户已创建：`SELECT * FROM users;`
2. 确认角色已分配：`SELECT * FROM user_in_roles WHERE user_id = 'default-admin-uuid-0001';`
3. 确认密码正确：密码应为 MD5 加密后的值

---

## 下一步

- [快速开始指南](./QUICKSTART.md)
- [项目文档](./README.md)
- [开发进度](./PROGRESS.md)

---

**最后更新**: 2025-11-14
