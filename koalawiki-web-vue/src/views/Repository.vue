<template>
  <div class="container mx-auto px-4 py-8">
    <div v-if="loading" class="text-center py-8">加载中...</div>

    <div v-else-if="warehouse">
      <div class="mb-6">
        <div class="flex justify-between items-start">
          <div class="flex-1">
            <h1 class="text-3xl font-bold mb-2">{{ warehouse.name }}</h1>
            <p v-if="warehouse.description" class="text-gray-600 mb-4">{{ warehouse.description }}</p>
            <div class="flex gap-4 text-sm">
              <span>⭐ {{ warehouse.stars }}</span>
              <span>🔱 {{ warehouse.forks }}</span>
              <span>📦 {{ warehouse.branch }}</span>
              <span :class="statusClass(warehouse.status)">{{ warehouse.status }}</span>
            </div>
          </div>
          <div class="flex gap-2">
            <button
              @click="goToAIDocuments"
              class="px-4 py-2 bg-purple-500 text-white rounded-lg hover:bg-purple-600 flex items-center gap-2"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
              </svg>
              AI文档
            </button>
            <button
              @click="triggerSync"
              :disabled="syncing"
              class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:bg-gray-400"
            >
              {{ syncing ? '同步中...' : '同步仓库' }}
            </button>
          </div>
        </div>
      </div>

      <div class="border-t pt-6">
        <h2 class="text-2xl font-semibold mb-4">文档目录</h2>
        <div v-if="loadingTree" class="text-center py-4">加载目录中...</div>
        <div v-else class="space-y-2">
          <div
            v-for="item in (Array.isArray(documentTree) ? documentTree : []).filter(i => i)"
            :key="item.id"
            class="p-3 border rounded hover:bg-gray-50 cursor-pointer"
            @click="goToDocument(item.id)"
          >
            {{ item.name }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { warehouseApi, type WarehouseResponse } from '@/api/warehouse'

const route = useRoute()
const router = useRouter()
const warehouse = ref<WarehouseResponse | null>(null)
const documentTree = ref<any[]>([])
const loading = ref(false)
const loadingTree = ref(false)
const syncing = ref(false)

const loadWarehouse = async () => {
  loading.value = true
  try {
    const id = route.params.id as string
    warehouse.value = await warehouseApi.getWarehouse(id) as any
  } catch (error) {
    console.error('Failed to load warehouse:', error)
  } finally {
    loading.value = false
  }
}

const loadDocumentTree = async () => {
  loadingTree.value = true
  try {
    const id = route.params.id as string
    const response = await warehouseApi.getDocumentTree(id) as any
    // API interceptor 已经提取了 data,response 就是实际的树节点数据
    console.log('Document tree response:', response)

    // response 是单个根节点,children 包含所有顶层目录
    if (response && response.children) {
      documentTree.value = response.children
    } else {
      documentTree.value = []
    }

    console.log('Document tree loaded:', documentTree.value.length, 'items')
  } catch (error) {
    console.error('Failed to load document tree:', error)
  } finally {
    loadingTree.value = false
  }
}

const goToDocument = (documentId: string) => {
  const warehouseId = route.params.id as string
  router.push(`/document/${warehouseId}/${documentId}`)
}

const goToAIDocuments = () => {
  const warehouseId = route.params.id as string
  router.push(`/repository/${warehouseId}/ai-documents`)
}

const triggerSync = async () => {
  if (!warehouse.value?.id) {
    console.error('Warehouse ID is missing')
    return
  }

  console.log('Triggering sync for warehouse:', warehouse.value.id)
  syncing.value = true

  try {
    const response = await warehouseApi.triggerSync(warehouse.value.id, false) as any
    console.log('Sync response:', response)

    alert('同步任务已触发，正在同步...')

    // 重新加载仓库信息和文档树
    await loadWarehouse()
    await loadDocumentTree()
  } catch (error: any) {
    console.error('Failed to trigger sync:', error)
    alert(error.response?.data?.message || error.message || '触发同步失败')
  } finally {
    syncing.value = false
  }
}

const statusClass = (status: string) => {
  const classes: Record<string, string> = {
    READY: 'text-green-600',
    SYNCING: 'text-blue-600',
    FAILED: 'text-red-600',
    PENDING: 'text-yellow-600'
  }
  return classes[status] || 'text-gray-600'
}

onMounted(() => {
  loadWarehouse()
  loadDocumentTree()
})
</script>
