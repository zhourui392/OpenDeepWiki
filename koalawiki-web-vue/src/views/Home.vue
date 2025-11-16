<template>
  <div class="container mx-auto px-4 py-8">
    <h1 class="text-3xl font-bold mb-6">仓库列表</h1>

    <div class="mb-4">
      <input
        v-model="keyword"
        type="text"
        placeholder="搜索仓库..."
        class="w-full px-4 py-2 border rounded-lg"
        @input="handleSearch"
      />
    </div>

    <div v-if="loading" class="text-center py-8">加载中...</div>

    <div v-else class="grid gap-4">
      <div
        v-for="warehouse in warehouses"
        :key="warehouse.id"
        class="border rounded-lg p-4 hover:shadow-lg transition-shadow cursor-pointer"
        @click="goToRepository(warehouse.id)"
      >
        <h2 class="text-xl font-semibold mb-2">{{ warehouse.name }}</h2>
        <p v-if="warehouse.description" class="text-gray-600 mb-2">{{ warehouse.description }}</p>
        <div class="flex gap-4 text-sm text-gray-500">
          <span>⭐ {{ warehouse.stars }}</span>
          <span>🔱 {{ warehouse.forks }}</span>
          <span :class="statusClass(warehouse.status)">{{ warehouse.status }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { warehouseApi, type WarehouseResponse } from '@/api/warehouse'

const router = useRouter()
const warehouses = ref<WarehouseResponse[]>([])
const loading = ref(false)
const keyword = ref('')

const loadWarehouses = async () => {
  loading.value = true
  try {
    warehouses.value = await warehouseApi.getWarehouseList(1, 20, keyword.value) as any
  } catch (error) {
    console.error('Failed to load warehouses:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  loadWarehouses()
}

const goToRepository = (id: string) => {
  router.push(`/repository/${id}`)
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
  loadWarehouses()
})
</script>
