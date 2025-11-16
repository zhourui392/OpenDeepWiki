# OpenDeepWiki Vue迁移技术方案

> **文档版本**: 1.0
> **创建日期**: 2025-11-15
> **状态**: 待实施
> **预计周期**: 1-2周

---

## 📋 目录

1. [方案概述](#方案概述)
2. [后端API分析](#后端api分析)
3. [技术选型](#技术选型)
4. [核心技术设计](#核心技术设计)
5. [项目结构](#项目结构)
6. [实施步骤](#实施步骤)
7. [Maven集成](#maven集成)
8. [开发规范](#开发规范)

---

## 🎯 方案概述

### 目标

将现有React前端完全替换为Vue3，实现：
- ✅ 完全替换React技术栈
- ✅ Markdown只读展示 + 数学公式支持（KaTeX）
- ✅ 保持现有Spring Boot集成方式（单JAR部署）
- ✅ 1-2周快速迁移
- ✅ 保持Tailwind CSS样式体系

### 当前状态

**现有技术栈**：
- 前端：React 19.1.1 + Vite 7.1.2 + TypeScript + Tailwind CSS
- 后端：Spring Boot 2.7.18 + JDK 1.8
- 部署：构建产物输出到 `koalawiki-web/src/main/resources/static/`
- 集成：frontend-maven-plugin自动化构建

**迁移原因**：
- 技术栈统一
- 无React源码，需从零实现
- 快速迁移，保留核心功能

---

## 📡 后端API分析

### API端点总览

通过分析后端Controller，系统共提供以下API端点：

#### 1. 仓库管理 API (`/api/Warehouse`)

| 方法 | 端点 | 功能 | 请求参数 |
|------|------|------|----------|
| POST | `/SubmitWarehouse` | 提交Git仓库 | `SubmitWarehouseRequest` |
| POST | `/CustomSubmitWarehouse` | 自定义提交仓库 | `CustomSubmitWarehouseRequest` |
| POST | `/UploadAndSubmitWarehouse` | 上传文件并提交仓库 | MultipartFile |
| GET | `/WarehouseList` | 获取仓库列表（分页） | page, pageSize, keyword |
| GET | `/LastWarehouse` | 获取最后一个仓库 | - |
| GET | `/ChangeLog` | 获取Git变更日志 | address, limit |
| GET | `/BranchList` | 获取Git分支列表 | address, gitUserName, gitPassword |
| GET | `/GetGitBranches` | 获取Git分支（别名） | address, gitUserName, gitPassword |
| GET | `/FileContent` | 获取文件内容 | warehouseId, path |
| GET | `/filecontentLine` | 按行获取文件内容 | warehouseId, path |
| POST | `/ExportMarkdownZip` | 导出Markdown压缩包 | warehouseId |
| GET | `/Stats` | 获取所有仓库统计信息 | - |
| GET | `/GetDocumentTree` | 获取文档树结构 | warehouseId |
| GET | `/minimap` | 获取思维导图数据 | warehouseId |
| POST | `/BatchOperate` | 批量操作仓库 | `BatchOperateRequest` |

#### 2. 仓库详情管理 API (`/api/Repository`)

| 方法 | 端点 | 功能 | 请求参数 |
|------|------|------|----------|
| GET | `/Repository` | 获取仓库详情 | id |
| DELETE | `/Repository` | 删除仓库 | id |
| GET | `/RepositoryList` | 获取仓库列表 | page, pageSize, keyword, status |
| POST | `/GitRepository` | 创建Git仓库 | `CustomSubmitWarehouseRequest` |
| PUT | `/UpdateWarehouse` | 更新仓库信息 | id, `UpdateWarehouseRequest` |
| POST | `/ResetRepository` | 重置仓库 | id |
| GET | `/Files` | 获取文件列表 | warehouseId, path |
| GET | `/FileContent` | 获取文件内容 | warehouseId, path 或 id |
| POST | `/FileContent` | 保存文件内容 | id, content |
| GET | `/DocumentCatalogs` | 获取文档目录 | warehouseId |
| POST | `/ManualSync` | 手动触发同步 | warehouseId |
| POST | `/UpdateSync` | 更新同步配置 | id, enableSync |
| GET | `/SyncRecords` | 获取同步记录（分页） | warehouseId, page, pageSize |
| GET | `/Export` | 导出仓库（Markdown Zip） | id |
| GET | `/RepositoryStats` | 获取仓库统计信息 | id |
| GET | `/RepositoryLogs` | 获取仓库日志 | warehouseId, page, pageSize |

#### 3. AI功能 API (`/api/ai`)

| 方法 | 端点 | 功能 | 请求参数 |
|------|------|------|----------|
| POST | `/readme/generate` | 生成README文档 | `GenerateReadmeRequest` |
| GET | `/readme/generate-simple` | 生成简单README | warehouseId, name, description |
| POST | `/catalog/optimize` | 优化文档目录 | `OptimizeCatalogRequest` |
| POST | `/document/summarize` | 生成文档摘要 | `SummarizeDocumentRequest` |
| POST | `/document/batch-summarize` | 批量生成文档摘要 | documentIds, maxLength |
| POST | `/qa/ask` | AI问答 | `AskQuestionRequest` |
| POST | `/tags/generate` | 生成标签 | content, maxTags |
| POST | `/tech-stack/analyze` | 分析技术栈 | fileList |
| GET | `/project/describe` | 生成项目描述 | name, techStack |

#### 4. 统计数据 API (`/api/statistics`)

| 方法 | 端点 | 功能 | 请求参数 |
|------|------|------|----------|
| GET | `/daily/{warehouseId}` | 获取每日统计 | warehouseId, date |
| GET | `/trend/{warehouseId}` | 获取趋势数据 | warehouseId, days |
| GET | `/summary/{warehouseId}` | 获取统计摘要 | warehouseId |

#### 5. 健康检查 API (`/api`)

| 方法 | 端点 | 功能 | 请求参数 |
|------|------|------|----------|
| GET | `/health` | 健康检查 | - |

### 核心数据模型

#### WarehouseResponse（仓库响应）
```typescript
interface WarehouseResponse {
  id: string
  name: string
  organizationName?: string
  description?: string
  address: string
  branch: string
  status: 'PENDING' | 'SYNCING' | 'READY' | 'FAILED' | 'CANCELED'
  error?: string
  stars: number
  forks: number
  createdAt: Date
  updatedAt?: Date
  version?: string
  isEmbedded: boolean
  isRecommended: boolean
  userId: string
}
```

#### FileContentResponse（文件内容响应）
```typescript
interface FileContentResponse {
  path: string
  content: string
  size: number
  fileType: string
  isBinary: boolean
  encoding: string
}
```

#### DocumentCatalogResponse（文档目录响应）
```typescript
interface DocumentCatalogResponse {
  id: string
  name: string
  url?: string
  description?: string
  parentId?: string
  order: number
  warehouseId: string
  isCompleted: boolean
}
```

### 前端需要实现的核心页面

根据API分析，前端需要实现以下核心页面：

1. **首页/仓库列表页** (`/`)
   - 展示仓库列表（分页）
   - 搜索仓库
   - 创建新仓库

2. **仓库详情页** (`/repository/:id`)
   - 仓库基本信息
   - 文件树导航
   - 文档目录
   - 统计信息
   - 同步记录

3. **文档展示页** (`/document/:warehouseId/:path`)
   - Markdown文档渲染
   - 目录导航
   - 代码高亮
   - 数学公式支持

4. **仓库管理页** (`/repository/:id/settings`)
   - 编辑仓库信息
   - 同步配置
   - 删除仓库

5. **AI功能页** (可选)
   - README生成
   - 文档摘要
   - AI问答

---

## 🛠️ 技术选型

### 前端技术栈

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 框架 | Vue | 3.5+ | Composition API |
| 构建工具 | Vite | 7.x | 保持一致 |
| 语言 | TypeScript | 5.8+ | 类型安全 |
| 样式 | Tailwind CSS | 4.x | 保持现有样式体系 |
| 路由 | Vue Router | 4.x | 官方路由 |
| 状态管理 | Pinia | 2.x | 官方推荐 |
| HTTP客户端 | Axios | 1.x | API调用 |

### Markdown渲染方案

**核心库**：markdown-it（轻量、可扩展、插件丰富）

| 依赖 | 版本 | 功能 |
|------|------|------|
| markdown-it | ^14.0.0 | Markdown解析器 |
| markdown-it-katex | ^2.0.3 | 数学公式支持 |
| katex | ^0.16.9 | KaTeX渲染引擎 |
| highlight.js | ^11.9.0 | 代码高亮 |
| markdown-it-anchor | ^9.0.0 | 标题锚点（可选） |

**为什么选择markdown-it？**
- ✅ 轻量级（~50KB）
- ✅ 插件生态丰富
- ✅ 性能优秀
- ✅ 支持自定义渲染规则
- ✅ 与Vue集成简单

---

## 🏗️ 核心技术设计

### 3.1 Markdown渲染核心

#### useMarkdown组合式函数

```typescript
// src/composables/useMarkdown.ts
import MarkdownIt from 'markdown-it'
import katex from 'markdown-it-katex'
import anchor from 'markdown-it-anchor'
import hljs from 'highlight.js'
import { computed, ref } from 'vue'

interface MarkdownOptions {
  enableKatex?: boolean
  enableAnchor?: boolean
  enableHighlight?: boolean
}

export function useMarkdown(options: MarkdownOptions = {}) {
  const {
    enableKatex = true,
    enableAnchor = true,
    enableHighlight = true
  } = options

  // 创建Markdown实例
  const md = new MarkdownIt({
    html: true,
    linkify: true,
    typographer: true,
    highlight: enableHighlight ? (str, lang) => {
      if (lang && hljs.getLanguage(lang)) {
        try {
          return hljs.highlight(str, { language: lang }).value
        } catch {}
      }
      return ''
    } : undefined
  })

  // 加载插件
  if (enableKatex) {
    md.use(katex, {
      throwOnError: false,
      errorColor: '#cc0000'
    })
  }

  if (enableAnchor) {
    md.use(anchor, {
      permalink: true,
      permalinkBefore: true,
      permalinkSymbol: '#'
    })
  }

  // 渲染Markdown
  const render = (content: string): string => {
    return md.render(content)
  }

  // 提取目录
  const extractToc = (content: string) => {
    const tokens = md.parse(content, {})
    const headings: Array<{ level: number; text: string; id: string }> = []

    for (let i = 0; i < tokens.length; i++) {
      const token = tokens[i]
      if (token.type === 'heading_open') {
        const level = parseInt(token.tag.slice(1))
        const textToken = tokens[i + 1]
        const text = textToken?.content || ''
        const id = token.attrGet('id') || ''
        headings.push({ level, text, id })
      }
    }

    return headings
  }

  return {
    render,
    extractToc
  }
}
```

#### MarkdownViewer组件

```vue
<!-- src/components/MarkdownViewer.vue -->
<template>
  <div class="markdown-container flex gap-6">
    <!-- 目录侧边栏 -->
    <aside v-if="showToc && toc.length > 0" class="toc-sidebar w-64 sticky top-4 h-fit">
      <nav class="space-y-2">
        <a
          v-for="heading in toc"
          :key="heading.id"
          :href="`#${heading.id}`"
          :class="[
            'block text-sm hover:text-blue-600 transition-colors',
            `pl-${(heading.level - 1) * 4}`
          ]"
        >
          {{ heading.text }}
        </a>
      </nav>
    </aside>

    <!-- Markdown内容 -->
    <article
      class="markdown-body flex-1 prose prose-slate max-w-none"
      v-html="renderedHtml"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useMarkdown } from '@/composables/useMarkdown'

interface Props {
  content: string
  showToc?: boolean
  enableKatex?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showToc: true,
  enableKatex: true
})

const { render, extractToc } = useMarkdown({
  enableKatex: props.enableKatex,
  enableAnchor: props.showToc,
  enableHighlight: true
})

const renderedHtml = computed(() => render(props.content))
const toc = computed(() => props.showToc ? extractToc(props.content) : [])
</script>

<style scoped>
/* Markdown样式 */
@import 'highlight.js/styles/github.css';
@import 'katex/dist/katex.min.css';

.markdown-body {
  @apply text-gray-800 leading-relaxed;
}

.markdown-body :deep(h1) {
  @apply text-3xl font-bold mt-8 mb-4 border-b pb-2;
}

.markdown-body :deep(h2) {
  @apply text-2xl font-bold mt-6 mb-3;
}

.markdown-body :deep(h3) {
  @apply text-xl font-semibold mt-4 mb-2;
}

.markdown-body :deep(pre) {
  @apply bg-gray-50 rounded-lg p-4 overflow-x-auto;
}

.markdown-body :deep(code) {
  @apply bg-gray-100 px-1 py-0.5 rounded text-sm;
}

.markdown-body :deep(pre code) {
  @apply bg-transparent p-0;
}

.markdown-body :deep(blockquote) {
  @apply border-l-4 border-blue-500 pl-4 italic text-gray-600;
}

.markdown-body :deep(table) {
  @apply w-full border-collapse;
}

.markdown-body :deep(th) {
  @apply bg-gray-100 font-semibold p-2 border;
}

.markdown-body :deep(td) {
  @apply p-2 border;
}

.toc-sidebar a {
  @apply text-gray-600;
}

.toc-sidebar a:hover {
  @apply text-blue-600;
}
</style>
```

### 3.2 API封装设计

```typescript
// src/api/client.ts
import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
apiClient.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
apiClient.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      // 跳转登录
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default apiClient
```

```typescript
// src/api/document.ts
import apiClient from './client'

export interface DocumentContent {
  path: string
  content: string
  type: string
}

export const documentApi = {
  getFileContent(repositoryId: string, path: string) {
    return apiClient.get<DocumentContent>('/Repository/FileContent', {
      params: { repositoryId, path }
    })
  },

  getDocumentCatalogs(repositoryId: string) {
    return apiClient.get('/Repository/DocumentCatalogs', {
      params: { repositoryId }
    })
  }
}
```

### 3.3 路由设计

```typescript
// src/router/index.ts
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/repository/:id',
    name: 'Repository',
    component: () => import('@/views/Repository.vue')
  },
  {
    path: '/document/:id',
    name: 'Document',
    component: () => import('@/views/Document.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
```

### 3.4 Vite构建配置

```typescript
// vite.config.ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import viteCompression from 'vite-plugin-compression'

export default defineConfig({
  plugins: [
    vue(),
    // Gzip压缩
    viteCompression({
      verbose: true,
      disable: false,
      threshold: 10240,
      algorithm: 'gzip',
      ext: '.gz'
    }),
    // Brotli压缩
    viteCompression({
      verbose: true,
      disable: false,
      threshold: 10240,
      algorithm: 'brotliCompress',
      ext: '.br'
    })
  ],

  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },

  // 开发服务器配置
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:18091',
        changeOrigin: true
      }
    }
  },

  // 构建配置
  build: {
    // 输出到Spring Boot静态资源目录
    outDir: '../koalawiki-web/src/main/resources/static',
    emptyOutDir: true,
    assetsDir: 'static',
    sourcemap: false,
    cssCodeSplit: true,
    chunkSizeWarningLimit: 1000,

    rollupOptions: {
      output: {
        // 代码分割
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'markdown': ['markdown-it', 'markdown-it-katex', 'katex', 'highlight.js'],
          'utils': ['axios']
        },
        // 文件命名
        chunkFileNames: 'static/js/[name].[hash].js',
        entryFileNames: 'static/js/[name].[hash].js',
        assetFileNames: 'static/[ext]/[name].[hash].[ext]'
      }
    }
  }
})
```

---

## 📁 项目结构

```
OpenDeepWiki/
├── koalawiki-web-vue/              # 新建Vue前端项目
│   ├── src/
│   │   ├── api/                    # API接口封装
│   │   │   ├── client.ts           # Axios客户端
│   │   │   ├── document.ts         # 文档API
│   │   │   ├── warehouse.ts        # 仓库API
│   │   │   └── auth.ts             # 认证API
│   │   ├── components/             # 通用组件
│   │   │   ├── MarkdownViewer.vue  # Markdown展示组件
│   │   │   ├── Layout/             # 布局组件
│   │   │   └── Common/             # 通用组件
│   │   ├── composables/            # 组合式函数
│   │   │   └── useMarkdown.ts      # Markdown渲染逻辑
│   │   ├── views/                  # 页面视图
│   │   │   ├── Home.vue            # 首页
│   │   │   ├── Repository.vue      # 仓库页
│   │   │   ├── Document.vue        # 文档页
│   │   │   └── Login.vue           # 登录页
│   │   ├── router/                 # 路由配置
│   │   │   └── index.ts
│   │   ├── stores/                 # Pinia状态管理
│   │   │   ├── user.ts
│   │   │   └── document.ts
│   │   ├── utils/                  # 工具函数
│   │   ├── styles/                 # 样式文件
│   │   │   └── main.css
│   │   ├── App.vue                 # 根组件
│   │   └── main.ts                 # 入口文件
│   ├── public/
│   │   └── favicon.png
│   ├── index.html
│   ├── vite.config.ts
│   ├── tailwind.config.js
│   ├── tsconfig.json
│   ├── package.json
│   └── .env.development
│
└── koalawiki-web/                  # 后端Web层（保持不变）
    ├── src/main/
    │   ├── java/
    │   └── resources/
    │       ├── static/             # 前端构建产物输出目录
    │       └── application.yml
    └── pom.xml                     # 需更新workingDirectory
```

---

## 🚀 实施步骤

### 第1步：创建Vue项目（1天）

**任务**：
1. 初始化Vue3 + Vite + TypeScript项目
2. 配置Tailwind CSS
3. 安装核心依赖
4. 配置Vite构建输出路径

**命令**：
```bash
cd D:\qpon\ai_space\OpenDeepWiki
npm create vite@latest koalawiki-web-vue -- --template vue-ts
cd koalawiki-web-vue
npm install

# 安装依赖
npm install vue-router pinia axios
npm install markdown-it markdown-it-katex katex highlight.js markdown-it-anchor
npm install -D tailwindcss postcss autoprefixer
npm install -D vite-plugin-compression
npm install -D @types/markdown-it

# 初始化Tailwind
npx tailwindcss init -p
```

**配置文件**：
- `vite.config.ts` - 构建输出路径
- `tailwind.config.js` - Tailwind配置
- `tsconfig.json` - TypeScript配置

---

### 第2步：开发Markdown组件（2天）

**任务**：
1. 实现 `useMarkdown` 组合式函数
2. 开发 `MarkdownViewer` 组件
3. 集成代码高亮（highlight.js）
4. 集成数学公式（KaTeX）
5. 实现目录生成功能
6. 编写组件测试

**交付物**：
- `src/composables/useMarkdown.ts`
- `src/components/MarkdownViewer.vue`
- 测试Markdown文件

---

### 第3步：分析并迁移页面（3-5天）

**任务**：
1. 分析现有API端点（从后端Controller推断）
2. 封装API调用（axios）
3. 实现核心页面：
   - 首页（仓库列表）
   - 仓库详情页
   - 文档展示页
   - 登录页
4. 实现路由和状态管理
5. 实现布局组件

**API分析**（从后端推断）：
```
GET  /api/Warehouse/WarehouseList          - 仓库列表
POST /api/Warehouse/SubmitWarehouse        - 提交仓库
GET  /api/Repository/Repository            - 仓库详情
GET  /api/Repository/FileContent           - 文件内容
GET  /api/Repository/DocumentCatalogs      - 文档目录
POST /api/Repository/ManualSync            - 手动同步
GET  /api/health                            - 健康检查
```

---

### 第4步：集成测试（2天）

**任务**：
1. 本地开发环境测试
   - 前端：`npm run dev` (http://localhost:5173)
   - 后端：Spring Boot (http://localhost:18091)
   - 验证API代理
2. 构建测试
   - 执行 `npm run build`
   - 验证构建产物输出到正确目录
3. 集成测试
   - 启动Spring Boot
   - 访问 http://localhost:18091
   - 验证前端路由、API调用、Markdown渲染

**测试清单**：
- [ ] 首页加载正常
- [ ] 仓库列表显示
- [ ] 文档内容渲染
- [ ] Markdown代码高亮
- [ ] 数学公式渲染
- [ ] 目录导航功能
- [ ] 前端路由刷新不404
- [ ] API调用正常

---

### 第5步：优化部署（1天）

**任务**：
1. 性能优化
   - 路由懒加载
   - 图片懒加载
   - 代码分割优化
2. 更新Maven配置
3. 完整构建测试
4. 替换现有React构建产物
5. 生产环境验证

**Maven配置更新**：
```xml
<!-- koalawiki-web/pom.xml -->
<configuration>
    <workingDirectory>${project.basedir}/../../koalawiki-web-vue</workingDirectory>
    <installDirectory>${project.build.directory}/frontend</installDirectory>
</configuration>
```

---

## 🔧 Maven集成

### 更新POM配置

**文件**：`koalawiki-web/pom.xml`

只需修改 `workingDirectory` 指向新的Vue项目：

```xml
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <version>1.15.0</version>
    <configuration>
        <!-- 修改为Vue项目目录 -->
        <workingDirectory>${project.basedir}/../../koalawiki-web-vue</workingDirectory>
        <installDirectory>${project.build.directory}/frontend</installDirectory>
    </configuration>
    <!-- executions保持不变 -->
</plugin>
```

### 构建命令

```bash
# 开发环境（跳过前端构建）
cd koalawiki-web-vue
npm run dev

# 生产环境（完整构建）
cd java
mvn clean package

# 运行
java -jar koalawiki-web/target/koalawiki-web-0.1.0-SNAPSHOT.jar
```

---

## 📝 开发规范

### 代码规范

1. **遵循Alibaba P3C规范**（后端）
2. **Vue3 Composition API**（前端）
3. **TypeScript严格模式**
4. **ESLint + Prettier**代码格式化

### 命名规范

```typescript
// 组件：PascalCase
MarkdownViewer.vue

// 组合式函数：use开头
useMarkdown.ts

// API文件：小驼峰
documentApi.ts

// 常量：大写下划线
const API_BASE_URL = '/api'
```

### Git提交规范

```
feat: 添加Markdown渲染组件
fix: 修复路由跳转问题
docs: 更新迁移文档
style: 调整样式
refactor: 重构API封装
test: 添加单元测试
```

---

## ✅ 验收标准

### 功能验收

- [ ] Markdown文档正常渲染
- [ ] 代码高亮显示正确
- [ ] 数学公式渲染正确（KaTeX）
- [ ] 目录导航功能正常
- [ ] 前端路由正常工作
- [ ] API调用正常
- [ ] 登录认证功能正常
- [ ] 响应式布局适配

### 性能验收

- [ ] 首页加载时间 < 2秒
- [ ] Markdown渲染流畅
- [ ] 构建产物大小 < 10MB
- [ ] Lighthouse性能评分 > 90

### 兼容性验收

- [ ] Chrome最新版
- [ ] Firefox最新版
- [ ] Edge最新版
- [ ] Safari最新版（macOS）

---

## 📊 预期指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 开发周期 | 1-2周 | 快速迁移 |
| 构建产物大小 | 5-8MB | 压缩后 |
| 首页加载时间 | <2秒 | 优化后 |
| Markdown渲染时间 | <100ms | 中等文档 |
| 代码覆盖率 | >60% | 核心功能 |

---

## 🎯 风险与应对

### 风险1：无React源码参考

**应对**：
- 从后端API推断功能
- 分析现有构建产物
- 参考API测试文档

### 风险2：时间紧张

**应对**：
- 聚焦核心功能
- 延后非关键功能
- 采用最小化实现

### 风险3：Markdown渲染性能

**应对**：
- 使用虚拟滚动（长文档）
- 懒加载图片
- 缓存渲染结果

---

## 📚 参考资料

- [Vue 3 官方文档](https://vuejs.org/)
- [Vite 官方文档](https://vitejs.dev/)
- [markdown-it 文档](https://github.com/markdown-it/markdown-it)
- [KaTeX 文档](https://katex.org/)
- [Tailwind CSS 文档](https://tailwindcss.com/)
- [现有前后端集成文档](./FRONTEND_INTEGRATION_GUIDE.md)

---

## 🎉 总结

### 核心优势

✅ **技术栈现代化**
- Vue 3 Composition API
- TypeScript类型安全
- Vite极速构建

✅ **Markdown能力强大**
- markdown-it轻量高效
- KaTeX数学公式支持
- 代码高亮美观

✅ **集成方案成熟**
- 复用现有Maven配置
- 单JAR部署
- 零CORS问题

✅ **快速交付**
- 1-2周完成迁移
- 最小化实现
- 聚焦核心功能

### 下一步行动

1. **确认方案** - 与团队讨论确认
2. **创建项目** - 初始化Vue项目
3. **开发组件** - 实现Markdown组件
4. **迁移页面** - 逐步迁移功能
5. **测试部署** - 验证并上线

---

**文档版本**: 1.0
**维护**: OpenDeepWiki Team
**最后更新**: 2025-11-15

---

Generated with ❤️ by Claude Code
