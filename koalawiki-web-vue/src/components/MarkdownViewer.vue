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
@import 'highlight.js/styles/github.css';
@import 'katex/dist/katex.min.css';

.markdown-body {
  color: #1f2937;
  line-height: 1.75;
}

.markdown-body h1 {
  font-size: 1.875rem;
  font-weight: 700;
  margin-top: 2rem;
  margin-bottom: 1rem;
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 0.5rem;
}

.markdown-body h2 {
  font-size: 1.5rem;
  font-weight: 700;
  margin-top: 1.5rem;
  margin-bottom: 0.75rem;
}

.markdown-body h3 {
  font-size: 1.25rem;
  font-weight: 600;
  margin-top: 1rem;
  margin-bottom: 0.5rem;
}

.markdown-body pre {
  background-color: #f9fafb;
  border-radius: 0.5rem;
  padding: 1rem;
  overflow-x: auto;
}

.markdown-body code {
  background-color: #f3f4f6;
  padding: 0.125rem 0.25rem;
  border-radius: 0.25rem;
  font-size: 0.875rem;
}

.markdown-body pre code {
  background-color: transparent;
  padding: 0;
}

.markdown-body blockquote {
  border-left: 4px solid #3b82f6;
  padding-left: 1rem;
  font-style: italic;
  color: #4b5563;
}

.markdown-body table {
  width: 100%;
  border-collapse: collapse;
}

.markdown-body th {
  background-color: #f3f4f6;
  font-weight: 600;
  padding: 0.5rem;
  border: 1px solid #e5e7eb;
}

.markdown-body td {
  padding: 0.5rem;
  border: 1px solid #e5e7eb;
}

.toc-sidebar a {
  color: #4b5563;
}

.toc-sidebar a:hover {
  color: #2563eb;
}
</style>
