<template>
  <div class="container mx-auto px-4 py-8">
    <div v-if="loading" class="text-center py-8">加载中...</div>

    <div v-else-if="error" class="text-center py-8 text-red-600">
      {{ error }}
    </div>

    <div v-else-if="fileContent">
      <MarkdownViewer :content="fileContent.content" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { documentApi, type FileContentResponse } from '@/api/document'
import MarkdownViewer from '@/components/MarkdownViewer.vue'

const route = useRoute()
const fileContent = ref<FileContentResponse | null>(null)
const loading = ref(false)
const error = ref('')

const loadDocument = async () => {
  loading.value = true
  error.value = ''
  try {
    const warehouseId = route.params.warehouseId as string
    const path = Array.isArray(route.params.path)
      ? route.params.path.join('/')
      : route.params.path || ''

    fileContent.value = await documentApi.getFileContent(warehouseId, path) as any
  } catch (err) {
    error.value = '加载文档失败'
    console.error('Failed to load document:', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDocument()
})
</script>
