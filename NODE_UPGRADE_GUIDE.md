# Node.js 升级指南

## 问题说明

Vite 7.x 要求 Node.js 版本 **20.19+** 或 **22.12+**

如果您的 Node.js 版本低于 20，会出现以下错误：
```
You are using Node.js 18.20.8. Vite requires Node.js version 20.19+ or 22.12+.
error during build:
[vite:vue] crypto.hash is not a function
```

## 检查当前版本

```bash
node -v
# 输出示例: v18.20.8 (需要升级)
# 目标版本: v20.x.x 或更高
```

---

## 升级方法

### 方式 1: 使用 nvm (推荐)

nvm (Node Version Manager) 可以轻松管理多个 Node.js 版本。

#### 安装 nvm

```bash
# 下载并安装 nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash

# 加载 nvm
source ~/.bashrc
# 或
source ~/.zshrc

# 验证安装
nvm --version
```

#### 安装 Node.js 20

```bash
# 安装 Node.js 20 LTS
nvm install 20

# 设置为默认版本
nvm alias default 20

# 使用 Node.js 20
nvm use 20

# 验证版本
node -v  # 应该显示 v20.x.x
npm -v
```

#### nvm 常用命令

```bash
# 列出所有已安装版本
nvm list

# 列出所有可用版本
nvm list-remote

# 切换到指定版本
nvm use 20

# 卸载某个版本
nvm uninstall 18

# 查看当前使用版本
nvm current
```

---

### 方式 2: 使用 NodeSource 仓库 (Ubuntu/Debian)

#### 安装 Node.js 20

```bash
# 添加 NodeSource 仓库
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -

# 安装 Node.js
sudo apt-get install -y nodejs

# 验证版本
node -v  # 应该显示 v20.x.x
npm -v
```

#### 安装 Node.js 22 (可选)

```bash
# 添加 NodeSource 仓库
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -

# 安装 Node.js
sudo apt-get install -y nodejs

# 验证版本
node -v  # 应该显示 v22.x.x
```

---

### 方式 3: 从官网下载二进制包

#### Ubuntu/Debian

```bash
# 下载 Node.js 20 LTS
cd /tmp
wget https://nodejs.org/dist/v20.11.0/node-v20.11.0-linux-x64.tar.xz

# 解压
tar -xJf node-v20.11.0-linux-x64.tar.xz

# 移动到系统目录
sudo mv node-v20.11.0-linux-x64 /usr/local/node-20

# 创建软链接
sudo ln -sf /usr/local/node-20/bin/node /usr/local/bin/node
sudo ln -sf /usr/local/node-20/bin/npm /usr/local/bin/npm
sudo ln -sf /usr/local/node-20/bin/npx /usr/local/bin/npx

# 验证版本
node -v
npm -v
```

---

### 方式 4: 使用 n (Node 版本管理器)

```bash
# 安装 n
sudo npm install -g n

# 安装 Node.js 20 LTS
sudo n lts
# 或安装 Node.js 20 稳定版
sudo n 20

# 验证版本
node -v
npm -v
```

---

## 升级后重新部署

升级 Node.js 后，需要重新安装依赖并构建：

```bash
cd OpenDeepWiki/koalawiki-web-vue

# 清理旧的依赖和构建产物
rm -rf node_modules package-lock.json dist

# 重新安装依赖
npm install

# 构建前端
npm run build

# 返回项目根目录
cd ..

# 重新部署
./deploy.sh restart
```

或者使用一键部署脚本：

```bash
cd OpenDeepWiki
./quick-deploy.sh
```

---

## 验证安装

```bash
# 检查 Node.js 版本
node -v
# 预期输出: v20.x.x 或更高

# 检查 npm 版本
npm -v
# 预期输出: 10.x.x 或更高

# 测试构建
cd koalawiki-web-vue
npm run build
# 应该成功构建，无错误
```

---

## 常见问题

### Q1: 升级后 npm 命令找不到

**A:** 确保 Node.js 的 bin 目录在 PATH 中

```bash
# 临时添加
export PATH="/usr/local/node-20/bin:$PATH"

# 永久添加（添加到 ~/.bashrc 或 ~/.zshrc）
echo 'export PATH="/usr/local/node-20/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

### Q2: 权限错误

**A:** 使用 nvm 可以避免权限问题（推荐）

```bash
# 或者修复 npm 权限
mkdir ~/.npm-global
npm config set prefix '~/.npm-global'
echo 'export PATH=~/.npm-global/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

### Q3: 多版本共存

**A:** 使用 nvm 管理多个版本

```bash
# 安装多个版本
nvm install 18
nvm install 20
nvm install 22

# 切换版本
nvm use 20  # 用于 OpenDeepWiki
nvm use 18  # 用于其他项目

# 为项目指定版本（在项目根目录创建 .nvmrc）
echo "20" > .nvmrc
nvm use  # 自动使用 .nvmrc 指定的版本
```

### Q4: CentOS/RHEL 如何升级

**A:** 使用 NodeSource 仓库

```bash
# Node.js 20
curl -fsSL https://rpm.nodesource.com/setup_20.x | sudo bash -
sudo yum install -y nodejs

# 验证
node -v
```

---

## 推荐配置

### 创建 .nvmrc 文件

在项目根目录创建 `.nvmrc` 指定 Node.js 版本：

```bash
cd OpenDeepWiki
echo "20" > .nvmrc
```

使用时：
```bash
cd OpenDeepWiki
nvm use  # 自动读取 .nvmrc
```

### 配置 npm 镜像（可选）

如果下载速度慢，可以使用国内镜像：

```bash
# 使用淘宝镜像
npm config set registry https://registry.npmmirror.com

# 恢复官方镜像
npm config set registry https://registry.npmjs.org
```

---

## 卸载旧版本

### Ubuntu/Debian

```bash
# 卸载通过 apt 安装的 Node.js
sudo apt remove nodejs npm

# 清理配置
sudo apt autoremove
```

### 手动安装的版本

```bash
# 删除二进制文件
sudo rm -rf /usr/local/node-18
sudo rm /usr/local/bin/node
sudo rm /usr/local/bin/npm
sudo rm /usr/local/bin/npx
```

---

## 版本选择建议

| 版本 | 状态 | 支持期 | 推荐场景 |
|------|------|--------|---------|
| Node.js 20 | **LTS** (推荐) | 至 2026 年 4 月 | 生产环境 |
| Node.js 22 | Current | 至 2027 年 10 月 | 尝鲜新特性 |
| Node.js 18 | 维护中 | 至 2025 年 4 月 | ❌ 不支持 Vite 7 |

**推荐使用 Node.js 20 LTS**，稳定且长期支持。

---

## 参考链接

- Node.js 官网: https://nodejs.org/
- nvm GitHub: https://github.com/nvm-sh/nvm
- NodeSource 仓库: https://github.com/nodesource/distributions
- Vite 文档: https://vitejs.dev/guide/

---

**最后更新**: 2025-01-21
