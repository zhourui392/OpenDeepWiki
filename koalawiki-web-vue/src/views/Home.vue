<template>
  <div class="container mx-auto px-4 py-8">
    <div class="flex justify-between items-center mb-6">
      <h1 class="text-3xl font-bold">仓库列表</h1>
      <button
        @click="addWarehouse"
        class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600"
      >
        添加仓库
      </button>
    </div>

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

    <!-- 添加仓库对话框 -->
    <div v-if="showDialog" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-md">
        <h2 class="text-xl font-bold mb-4">添加仓库</h2>

        <div class="mb-4">
          <label class="block text-sm font-medium mb-2">仓库地址</label>
          <input
            v-model="repoUrl"
            type="text"
            placeholder="https://github.com/owner/repo.git"
            class="w-full px-3 py-2 border rounded-lg"
          />
        </div>

        <div class="mb-4">
          <label class="block text-sm font-medium mb-2">分支</label>
          <input
            v-model="branch"
            type="text"
            placeholder="main"
            class="w-full px-3 py-2 border rounded-lg"
          />
        </div>

        <div class="mb-4">
          <label class="block text-sm font-medium mb-2">用户名（可选）</label>
          <input
            v-model="gitUserName"
            type="text"
            placeholder="Git用户名"
            class="w-full px-3 py-2 border rounded-lg"
          />
        </div>

        <div class="mb-4">
          <label class="block text-sm font-medium mb-2">密码（可选）</label>
          <input
            v-model="gitPassword"
            type="password"
            placeholder="Git密码或Token"
            class="w-full px-3 py-2 border rounded-lg"
          />
        </div>

        <div class="flex justify-end gap-2">
          <button
            @click="closeDialog"
            class="px-4 py-2 border rounded-lg hover:bg-gray-100"
            :disabled="submitting"
          >
            取消
          </button>
          <button
            @click="submitWarehouse"
            class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600"
            :disabled="submitting"
          >
            {{ submitting ? '提交中...' : '提交' }}
          </button>
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
const showDialog = ref(false)
const repoUrl = ref('')
const branch = ref('main')
const gitUserName = ref('')
const gitPassword = ref('')
const submitting = ref(false)

const loadWarehouses = async () => {
  loading.value = true
  try {
    const response = await warehouseApi.getWarehouseList(1, 20, keyword.value) as any
    warehouses.value = response?.items || []
    console.log('Loaded warehouses:', warehouses.value)
  } catch (error) {
    console.error('Failed to load warehouses:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  loadWarehouses()
}

const addWarehouse = () => {
  showDialog.value = true
  repoUrl.value = ''
  branch.value = 'main'
  gitUserName.value = ''
  gitPassword.value = ''
}

const closeDialog = () => {
  showDialog.value = false
}

const submitWarehouse = async () => {
  if (!repoUrl.value.trim()) {
    alert('请输入仓库地址')
    return
  }

  submitting.value = true
  try {
    const response = await warehouseApi.submitWarehouse({
      address: repoUrl.value,
      branch: branch.value,
      gitUserName: gitUserName.value || undefined,
      gitPassword: gitPassword.value || undefined
    }) as any

    if (response.success) {
      alert('仓库添加成功')
      closeDialog()
      // 跳转到仓库详情页
      if (response.data?.id) {
        router.push(`/repository/${response.data.id}/ai-documents`)
      } else {
        loadWarehouses()
      }
    } else {
      alert(response.message || '添加失败')
    }
  } catch (error: any) {
    alert(error.response?.data?.message || '添加失败')
  } finally {
    submitting.value = false
  }
}

const goToRepository = (id: string) => {
  router.push(`/repository/${id}/ai-documents`)
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
