# OpenDeepWiki Vue Frontend

基于Vue 3 + Vite + TypeScript的前端项目。

## 技术栈

- Vue 3.5+ (Composition API)
- Vite 7.x
- TypeScript 5.8+
- Tailwind CSS 4.x
- Vue Router 4.x
- Axios
- remark + rehype (Markdown 渲染)
- KaTeX (数学公式)
- highlight.js (代码高亮 - Atom One Dark 主题)

## 开发

```bash
# 安装依赖
npm install

# 开发模式
npm run dev

# 生产构建
npm run build
```

## Maven集成

在项目根目录执行：

```bash
cd java
mvn clean package
```

构建产物会自动输出到 `koalawiki-web/src/main/resources/static/`
