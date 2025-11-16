<template>
  <div class="container mx-auto px-4 py-8">
    <div v-if="loading" class="text-center py-8">加载中...</div>

    <div v-else-if="warehouse">
      <div class="mb-6">
        <h1 class="text-3xl font-bold mb-2">{{ warehouse.name }}</h1>
        <p v-if="warehouse.description" class="text-gray-600 mb-4">{{ warehouse.description }}</p>
        <div class="flex gap-4 text-sm">
          <span>⭐ {{ warehouse.stars }}</span>
          <span>🔱 {{ warehouse.forks }}</span>
          <span>📦 {{ warehouse.branch }}</span>
        </div>
      </div>

      <div class="border-t pt-6">
        <h2 class="text-2xl font-semibold mb-4">文档目录</h2>
        <div v-if="loadingTree" class="text-center py-4">加载目录中...</div>
        <div v-else class="space-y-2">
          <div
            v-for="item in (Array.isArray(documentTree) ? documentTree : []).filter(i => i)"
            :key="item.path"
            class="p-3 border rounded hover:bg-gray-50 cursor-pointer"
            @click="goToDocument(item.path)"
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
    documentTree.value = await warehouseApi.getDocumentTree(id) as any
  } catch (error) {
    console.error('Failed to load document tree:', error)
  } finally {
    loadingTree.value = false
  }
}

const goToDocument = (path: string) => {
  const id = route.params.id as string
  router.push(`/document/${id}/${path}`)
}

onMounted(() => {
  loadWarehouse()
  loadDocumentTree()
})
</script>
