<template>
  <div class="container mx-auto p-6">
    <div class="flex justify-between items-center mb-6">
      <h1 class="text-2xl font-bold">仓库管理</h1>
      <button @click="showAddDialog = true" class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
        添加仓库
      </button>
    </div>

    <div class="bg-white rounded-lg shadow">
      <table class="w-full">
        <thead class="bg-gray-50 border-b">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">名称</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">地址</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">分支</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">状态</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y">
          <tr v-for="warehouse in warehouses" :key="warehouse.id">
            <td class="px-6 py-4">{{ warehouse.name }}</td>
            <td class="px-6 py-4 text-sm text-gray-600">{{ warehouse.address }}</td>
            <td class="px-6 py-4 text-sm">{{ warehouse.branch }}</td>
            <td class="px-6 py-4">
              <span :class="getStatusClass(warehouse.status)" class="px-2 py-1 rounded text-xs">
                {{ warehouse.status }}
              </span>
            </td>
            <td class="px-6 py-4">
              <button @click="syncWarehouse(warehouse.id)" class="text-blue-600 hover:underline mr-3">同步</button>
              <button @click="deleteWarehouse(warehouse.id)" class="text-red-600 hover:underline">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="showAddDialog" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center">
      <div class="bg-white rounded-lg p-6 w-96">
        <h2 class="text-xl font-bold mb-4">添加仓库</h2>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium mb-1">仓库地址</label>
            <input v-model="newWarehouse.address" class="w-full px-3 py-2 border rounded" placeholder="https://github.com/user/repo" />
          </div>
          <div>
            <label class="block text-sm font-medium mb-1">分支</label>
            <input v-model="newWarehouse.branch" class="w-full px-3 py-2 border rounded" placeholder="main" />
          </div>
          <div class="flex justify-end gap-2">
            <button @click="showAddDialog = false" class="px-4 py-2 border rounded hover:bg-gray-50">取消</button>
            <button @click="addWarehouse" class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">添加</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { warehouseApi, type WarehouseResponse } from '@/api/warehouse'

const warehouses = ref<WarehouseResponse[]>([])
const showAddDialog = ref(false)
const newWarehouse = ref({ address: '', branch: 'main' })

const loadWarehouses = async () => {
  const response = await warehouseApi.getWarehouseList(1, 100) as any
  warehouses.value = response.items || []
}

const addWarehouse = async () => {
  await warehouseApi.submitWarehouse(newWarehouse.value)
  showAddDialog.value = false
  newWarehouse.value = { address: '', branch: 'main' }
  loadWarehouses()
}

const syncWarehouse = async (id: string) => {
  await warehouseApi.triggerSync(id, true)
  loadWarehouses()
}

const deleteWarehouse = async (id: string) => {
  if (confirm('确认删除该仓库？')) {
    try {
      await warehouseApi.deleteWarehouse(id)
      loadWarehouses()
    } catch (error) {
      console.error('删除仓库失败:', error)
      alert('删除仓库失败')
    }
  }
}

const getStatusClass = (status: string) => {
  const classes: Record<string, string> = {
    READY: 'bg-green-100 text-green-800',
    SYNCING: 'bg-blue-100 text-blue-800',
    FAILED: 'bg-red-100 text-red-800',
    PENDING: 'bg-yellow-100 text-yellow-800'
  }
  return classes[status] || 'bg-gray-100 text-gray-800'
}

onMounted(loadWarehouses)
</script>
