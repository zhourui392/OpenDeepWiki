-- =============================================
-- OpenDeepWiki 手动创建默认用户脚本
-- 使用方法：通过 H2 控制台或其他 SQL 客户端执行此脚本
-- =============================================

-- =============================================
-- 1. 创建默认角色
-- =============================================

-- 删除已存在的角色（如果需要重新创建）
-- DELETE FROM user_in_roles WHERE role_id IN ('admin-role-uuid', 'user-role-uuid', 'guest-role-uuid');
-- DELETE FROM roles WHERE id IN ('admin-role-uuid', 'user-role-uuid', 'guest-role-uuid');

-- 插入角色
INSERT INTO roles (id, name, description, is_system, created_at) VALUES
('admin-role-uuid', 'Admin', '系统管理员，拥有所有权限', 1, CURRENT_TIMESTAMP());

INSERT INTO roles (id, name, description, is_system, created_at) VALUES
('user-role-uuid', 'User', '普通用户，拥有基本权限', 1, CURRENT_TIMESTAMP());

INSERT INTO roles (id, name, description, is_system, created_at) VALUES
('guest-role-uuid', 'Guest', '访客，拥有只读权限', 1, CURRENT_TIMESTAMP());

-- =============================================
-- 2. 创建管理员用户
-- =============================================

-- 管理员账号
-- 邮箱: admin@koalawiki.com
-- 密码: admin123
-- MD5: 0192023a7bbd73250516f069df18b500

INSERT INTO users (
    id,
    name,
    email,
    password,
    bio,
    created_at
) VALUES (
    'default-admin-uuid-0001',
    'Administrator',
    'admin@koalawiki.com',
    '0192023a7bbd73250516f069df18b500',
    '系统默认管理员账号',
    CURRENT_TIMESTAMP()
);

-- 为管理员分配 Admin 角色
INSERT INTO user_in_roles (
    id,
    user_id,
    role_id,
    created_at
) VALUES (
    'admin-role-mapping-0001',
    'default-admin-uuid-0001',
    'admin-role-uuid',
    CURRENT_TIMESTAMP()
);

-- =============================================
-- 3. (可选) 创建演示用户
-- =============================================

-- 演示账号
-- 邮箱: demo@koalawiki.com
-- 密码: demo123
-- MD5: ab86a1e1ef70dff97959067b723c5c24

INSERT INTO users (
    id,
    name,
    email,
    password,
    bio,
    created_at
) VALUES (
    'default-demo-user-uuid-0001',
    'Demo User',
    'demo@koalawiki.com',
    'ab86a1e1ef70dff97959067b723c5c24',
    '演示用户账号',
    CURRENT_TIMESTAMP()
);

-- 为演示用户分配 User 角色
INSERT INTO user_in_roles (
    id,
    user_id,
    role_id,
    created_at
) VALUES (
    'demo-role-mapping-0001',
    'default-demo-user-uuid-0001',
    'user-role-uuid',
    CURRENT_TIMESTAMP()
);

-- =============================================
-- 4. 验证创建结果
-- =============================================

-- 查询所有角色
SELECT * FROM roles;

-- 查询所有用户
SELECT id, name, email, bio, created_at FROM users;

-- 查询用户角色关联
SELECT
    u.name AS user_name,
    u.email,
    r.name AS role_name,
    r.description AS role_description
FROM user_in_roles uir
JOIN users u ON uir.user_id = u.id
JOIN roles r ON uir.role_id = r.id;

-- =============================================
-- 完成！
-- =============================================
-- 现在您可以使用以下账号登录：
--
-- 管理员：
--   邮箱: admin@koalawiki.com
--   密码: admin123
--
-- 演示用户：
--   邮箱: demo@koalawiki.com
--   密码: demo123
-- =============================================
