<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <div class="max-w-5xl mx-auto">
      <!-- Loading -->
      <div v-if="loading" class="bg-white rounded-lg shadow-sm p-12 text-center">
        <div class="text-gray-500">加载中...</div>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="bg-white rounded-lg shadow-sm p-12 text-center">
        <div class="text-red-500">{{ error }}</div>
        <button
          @click="$router.back()"
          class="mt-4 px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700"
        >
          返回
        </button>
      </div>

      <!-- Document Detail -->
      <div v-else-if="document">
        <!-- Header -->
        <div class="bg-white rounded-lg shadow-sm p-6 mb-6">
          <div class="flex items-center justify-between mb-4">
            <button
              @click="$router.back()"
              class="flex items-center gap-2 text-gray-600 hover:text-gray-900"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
              </svg>
              返回
            </button>

            <div class="flex items-center gap-2">
              <span
                :class="getStatusClass(document.status)"
                class="px-3 py-1 text-sm font-medium rounded-full"
              >
                {{ getStatusText(document.status) }}
              </span>
              <span class="px-3 py-1 text-sm font-medium rounded-full bg-purple-100 text-purple-800">
                {{ document.agentType }}
              </span>
            </div>
          </div>

          <h1 class="text-3xl font-bold text-gray-900 mb-2">{{ document.title }}</h1>

          <div class="flex items-center gap-6 text-sm text-gray-600">
            <div class="flex items-center gap-2">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"></path>
              </svg>
              <span class="font-medium">源文件:</span>
              <code class="text-xs bg-gray-100 px-2 py-1 rounded">{{ document.sourceFile }}</code>
            </div>
          </div>

          <div class="flex items-center gap-6 text-sm text-gray-600 mt-2">
            <div>
              <span class="font-medium">创建时间:</span>
              {{ formatDate(document.createdAt) }}
            </div>
            <div>
              <span class="font-medium">更新时间:</span>
              {{ formatDate(document.updatedAt) }}
            </div>
          </div>

          <!-- Error Message -->
          <div v-if="document.status === 'FAILED' && document.errorMessage" class="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg">
            <div class="flex items-start gap-2">
              <svg class="w-5 h-5 text-red-600 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
              </svg>
              <div>
                <div class="font-medium text-red-900">生成失败</div>
                <div class="text-sm text-red-700 mt-1">{{ document.errorMessage }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Content -->
        <div class="bg-white rounded-lg shadow-sm p-8">
          <div
            v-if="document.content"
            class="markdown-body prose prose-slate max-w-none"
            v-html="renderedContent"
          ></div>
          <div v-else class="text-center text-gray-500 py-12">
            暂无内容
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { aiDocumentApi, type AIDocument } from '@/api/ai-document'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'

const route = useRoute()
const router = useRouter()

const document = ref<AIDocument | null>(null)
const loading = ref(false)
const error = ref('')

// Markdown渲染器配置
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: (str: string, lang: string) => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(str, { language: lang, ignoreIllegals: true }).value}</code></pre>`
      } catch (err) {
        console.error('Highlight error:', err)
      }
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  }
})

// 渲染的内容
const renderedContent = computed(() => {
  if (!document.value?.content) return ''
  try {
    return md.render(document.value.content)
  } catch (err) {
    console.error('Markdown render error:', err)
    return '<div class="text-red-500">文档渲染失败</div>'
  }
})

// 加载文档
const loadDocument = async () => {
  loading.value = true
  error.value = ''

  try {
    const id = route.params.id as string
    document.value = await aiDocumentApi.getDocument(id)
  } catch (err: any) {
    console.error('加载文档失败:', err)
    error.value = '加载文档失败: ' + (err.response?.data?.message || err.message)
  } finally {
    loading.value = false
  }
}

// 状态样式
const getStatusClass = (status: string) => {
  const classes = {
    'COMPLETED': 'bg-green-100 text-green-800',
    'DRAFT': 'bg-yellow-100 text-yellow-800',
    'FAILED': 'bg-red-100 text-red-800'
  }
  return classes[status as keyof typeof classes] || 'bg-gray-100 text-gray-800'
}

// 状态文本
const getStatusText = (status: string) => {
  const texts = {
    'COMPLETED': '已完成',
    'DRAFT': '生成中',
    'FAILED': '失败'
  }
  return texts[status as keyof typeof texts] || status
}

// 格式化日期
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 初始化
onMounted(() => {
  loadDocument()
})
</script>

<style scoped>
/* Markdown样式 */
.markdown-body {
  line-height: 1.6;
  color: #333;
}

.markdown-body h1 {
  font-size: 2em;
  font-weight: bold;
  margin-top: 1em;
  margin-bottom: 0.5em;
  border-bottom: 2px solid #e5e7eb;
  padding-bottom: 0.3em;
}

.markdown-body h2 {
  font-size: 1.5em;
  font-weight: bold;
  margin-top: 1em;
  margin-bottom: 0.5em;
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 0.3em;
}

.markdown-body h3 {
  font-size: 1.25em;
  font-weight: bold;
  margin-top: 1em;
  margin-bottom: 0.5em;
}

.markdown-body p {
  margin-top: 0.5em;
  margin-bottom: 0.5em;
}

.markdown-body ul, .markdown-body ol {
  padding-left: 2em;
  margin-top: 0.5em;
  margin-bottom: 0.5em;
}

.markdown-body li {
  margin-top: 0.25em;
  margin-bottom: 0.25em;
}

.markdown-body code {
  background-color: #f3f4f6;
  padding: 0.2em 0.4em;
  border-radius: 0.25rem;
  font-size: 0.875em;
  font-family: 'Courier New', monospace;
}

.markdown-body pre {
  background-color: #1e293b;
  color: #e2e8f0;
  padding: 1em;
  border-radius: 0.5rem;
  overflow-x: auto;
  margin-top: 1em;
  margin-bottom: 1em;
}

.markdown-body pre code {
  background-color: transparent;
  padding: 0;
  color: inherit;
  font-size: 0.875em;
}

.markdown-body blockquote {
  border-left: 4px solid #e5e7eb;
  padding-left: 1em;
  color: #6b7280;
  margin: 1em 0;
}

.markdown-body table {
  border-collapse: collapse;
  width: 100%;
  margin: 1em 0;
}

.markdown-body table th,
.markdown-body table td {
  border: 1px solid #e5e7eb;
  padding: 0.5em 1em;
  text-align: left;
}

.markdown-body table th {
  background-color: #f9fafb;
  font-weight: bold;
}

.markdown-body a {
  color: #3b82f6;
  text-decoration: none;
}

.markdown-body a:hover {
  text-decoration: underline;
}

/* 代码高亮样式 */
.hljs {
  background-color: #1e293b;
  color: #e2e8f0;
  padding: 1em;
  border-radius: 0.5rem;
  overflow-x: auto;
}
</style>
