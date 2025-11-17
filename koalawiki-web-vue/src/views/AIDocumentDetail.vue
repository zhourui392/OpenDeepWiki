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
import { useRoute } from 'vue-router'
import { aiDocumentApi, type AIDocument } from '@/api/ai-document'
import { useMarkdown } from '@/composables/useMarkdown'

const route = useRoute()

const document = ref<AIDocument | null>(null)
const loading = ref(false)
const error = ref('')

// 使用新的 remark/rehype 渲染器
const { render } = useMarkdown({
  enableKatex: true,
  enableAnchor: false,
  enableHighlight: true
})

// 渲染的内容
const renderedContent = computed(() => {
  if (!document.value?.content) return ''
  try {
    return render(document.value.content)
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
    document.value = await aiDocumentApi.getDocument(id) as any
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

<style>
@import 'highlight.js/styles/atom-one-dark.css';
@import 'katex/dist/katex.min.css';

.markdown-body {
  color: #1f2937;
  line-height: 1.75;
  font-size: 16px;
}

.markdown-body h1 {
  font-size: 2rem;
  font-weight: 700;
  margin-top: 2.5rem;
  margin-bottom: 1.25rem;
  border-bottom: 2px solid #e5e7eb;
  padding-bottom: 0.75rem;
  color: #111827;
}

.markdown-body h2 {
  font-size: 1.625rem;
  font-weight: 700;
  margin-top: 2rem;
  margin-bottom: 1rem;
  color: #111827;
}

.markdown-body h3 {
  font-size: 1.375rem;
  font-weight: 600;
  margin-top: 1.5rem;
  margin-bottom: 0.75rem;
  color: #1f2937;
}

.markdown-body p {
  margin-bottom: 1.25rem;
  color: #374151;
}

.markdown-body pre {
  background-color: #282c34;
  border-radius: 0.625rem;
  padding: 1.25rem;
  overflow-x: auto;
  margin: 1.5rem 0;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.markdown-body code {
  background-color: #f3f4f6;
  color: #e11d48;
  padding: 0.2rem 0.4rem;
  border-radius: 0.25rem;
  font-size: 0.875em;
  font-family: 'Fira Code', 'Consolas', 'Monaco', monospace;
}

.markdown-body pre code {
  background-color: transparent;
  color: #abb2bf;
  padding: 0;
  font-size: 0.875rem;
}

.markdown-body blockquote {
  border-left: 4px solid #3b82f6;
  background-color: #f0f9ff;
  padding: 1rem 1.5rem;
  margin: 1.5rem 0;
  border-radius: 0.375rem;
  color: #1e40af;
}

.markdown-body ul,
.markdown-body ol {
  margin: 1rem 0;
  padding-left: 2rem;
}

.markdown-body li {
  margin: 0.5rem 0;
  color: #374151;
}

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

.markdown-body a {
  color: #2563eb;
  text-decoration: none;
  font-weight: 500;
}

.markdown-body a:hover {
  color: #1d4ed8;
  text-decoration: underline;
}
</style>
