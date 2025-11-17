<template>
  <div class="markdown-container flex gap-6">
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

<style>
@import 'highlight.js/styles/atom-one-dark.css';
@import 'katex/dist/katex.min.css';

.markdown-container {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Noto Sans', Helvetica, Arial, sans-serif;
}

.markdown-body {
  color: #1f2937;
  line-height: 1.75;
  font-size: 16px;
}

/* 标题样式 */
.markdown-body h1 {
  font-size: 2rem;
  font-weight: 700;
  margin-top: 2.5rem;
  margin-bottom: 1.25rem;
  border-bottom: 2px solid #e5e7eb;
  padding-bottom: 0.75rem;
  color: #111827;
  letter-spacing: -0.025em;
}

.markdown-body h2 {
  font-size: 1.625rem;
  font-weight: 700;
  margin-top: 2rem;
  margin-bottom: 1rem;
  color: #111827;
  letter-spacing: -0.025em;
}

.markdown-body h3 {
  font-size: 1.375rem;
  font-weight: 600;
  margin-top: 1.5rem;
  margin-bottom: 0.75rem;
  color: #1f2937;
}

.markdown-body h4 {
  font-size: 1.125rem;
  font-weight: 600;
  margin-top: 1.25rem;
  margin-bottom: 0.5rem;
  color: #374151;
}

/* 段落和文本 */
.markdown-body p {
  margin-bottom: 1.25rem;
  color: #374151;
}

.markdown-body a {
  color: #2563eb;
  text-decoration: none;
  font-weight: 500;
}

.markdown-body a:hover {
  color: #1d4ed8;
  text-decoration: underline;
}

/* 代码块样式 */
.markdown-body pre {
  background-color: #282c34;
  border-radius: 0.625rem;
  padding: 1.25rem;
  overflow-x: auto;
  margin: 1.5rem 0;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.markdown-body code {
  background-color: #f3f4f6;
  color: #e11d48;
  padding: 0.2rem 0.4rem;
  border-radius: 0.25rem;
  font-size: 0.875em;
  font-family: 'Fira Code', 'Consolas', 'Monaco', monospace;
  font-weight: 500;
}

.markdown-body pre code {
  background-color: transparent;
  color: #abb2bf;
  padding: 0;
  font-size: 0.875rem;
  font-weight: 400;
}

/* 引用块样式 */
.markdown-body blockquote {
  border-left: 4px solid #3b82f6;
  background-color: #f0f9ff;
  padding: 1rem 1.5rem;
  margin: 1.5rem 0;
  border-radius: 0.375rem;
  font-style: normal;
  color: #1e40af;
}

/* 列表样式 */
.markdown-body ul,
.markdown-body ol {
  margin: 1rem 0;
  padding-left: 2rem;
}

.markdown-body li {
  margin: 0.5rem 0;
  color: #374151;
}

.markdown-body li::marker {
  color: #6b7280;
}

/* 表格样式 */
.markdown-body table {
  width: 100%;
  border-collapse: collapse;
  margin: 1.5rem 0;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
  border-radius: 0.5rem;
  overflow: hidden;
}

.markdown-body th {
  background: linear-gradient(to bottom, #f9fafb, #f3f4f6);
  font-weight: 600;
  padding: 0.75rem 1rem;
  border: 1px solid #e5e7eb;
  text-align: left;
  color: #111827;
}

.markdown-body td {
  padding: 0.75rem 1rem;
  border: 1px solid #e5e7eb;
  color: #374151;
}

.markdown-body tr:hover {
  background-color: #f9fafb;
}

/* 水平线 */
.markdown-body hr {
  border: none;
  border-top: 2px solid #e5e7eb;
  margin: 2rem 0;
}

/* 目录侧边栏样式 */
.toc-sidebar {
  background-color: #f9fafb;
  border-radius: 0.75rem;
  padding: 1.5rem;
  border: 1px solid #e5e7eb;
}

.toc-sidebar nav {
  font-size: 0.875rem;
}

.toc-sidebar a {
  color: #6b7280;
  transition: all 0.2s ease;
  padding: 0.375rem 0;
  border-left: 2px solid transparent;
  margin-left: -0.5rem;
  padding-left: 0.5rem;
}

.toc-sidebar a:hover {
  color: #2563eb;
  border-left-color: #2563eb;
  background-color: #eff6ff;
}

/* 任务列表样式（GFM） */
.markdown-body input[type="checkbox"] {
  margin-right: 0.5rem;
}

/* 数学公式样式优化 */
.markdown-body .katex {
  font-size: 1.1em;
}

.markdown-body .katex-display {
  margin: 1.5rem 0;
  overflow-x: auto;
  overflow-y: hidden;
}
</style>
