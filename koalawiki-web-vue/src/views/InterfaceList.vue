<template>
  <div class="interface-list p-6">
    <!-- 头部区域 -->
    <div class="flex justify-between items-center mb-6">
      <div class="flex items-center gap-4">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h1 class="text-2xl font-bold">Dubbo 接口列表</h1>
      </div>
      <el-button type="primary" @click="refreshInterfaces">
        刷新注册表
      </el-button>
    </div>

    <!-- 搜索和过滤 -->
    <div class="bg-white rounded-lg shadow p-4 mb-6">
      <div class="flex items-center gap-4">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索接口名称..."
          clearable
          class="w-80"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button @click="handleSearch">搜索</el-button>
        <el-button text @click="clearSearch">清除</el-button>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="grid grid-cols-3 gap-4 mb-6">
      <div class="bg-white rounded-lg shadow p-4">
        <p class="text-gray-500 text-sm">总接口数</p>
        <p class="text-2xl font-bold">{{ total }}</p>
      </div>
      <div class="bg-white rounded-lg shadow p-4">
        <p class="text-gray-500 text-sm">当前页</p>
        <p class="text-2xl font-bold">{{ interfaces.length }}</p>
      </div>
      <div class="bg-white rounded-lg shadow p-4">
        <p class="text-gray-500 text-sm">总页数</p>
        <p class="text-2xl font-bold">{{ totalPages }}</p>
      </div>
    </div>

    <!-- 接口表格 -->
    <div class="bg-white rounded-lg shadow">
      <el-table :data="interfaces" style="width: 100%" v-loading="loading">
        <el-table-column prop="simpleName" label="接口名" min-width="200">
          <template #default="{ row }">
            <router-link
              :to="`/clusters/${clusterId}/interfaces/${row.id}`"
              class="text-blue-600 hover:underline font-medium"
            >
              {{ row.simpleName }}
            </router-link>
            <div class="text-xs text-gray-400 truncate">{{ row.interfaceName }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="providerServiceName" label="提供者" width="150">
          <template #default="{ row }">
            <router-link
              v-if="row.providerWarehouseId"
              :to="`/repository/${row.providerWarehouseId}/ai-documents`"
              class="text-blue-600 hover:underline"
            >
              {{ row.providerServiceName }}
            </router-link>
            <span v-else>{{ row.providerServiceName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="80">
          <template #default="{ row }">
            {{ row.version || '默认' }}
          </template>
        </el-table-column>
        <el-table-column prop="methodCount" label="方法数" width="80" align="center" />
        <el-table-column prop="consumerCount" label="消费者" width="80" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.deprecated" type="warning" size="small">废弃</el-tag>
            <el-tag v-else type="success" size="small">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              text
              type="primary"
              size="small"
              @click="goToDetail(row.id)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="p-4 flex justify-end">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadInterfaces"
          @current-change="loadInterfaces"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 接口列表页面
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { dubboApi, type DubboInterfaceResponse } from '@/api/dubbo'

const route = useRoute()
const router = useRouter()

const clusterId = computed(() => route.params.clusterId as string)

const interfaces = ref<DubboInterfaceResponse[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const totalPages = ref(0)

async function loadInterfaces() {
  if (!clusterId.value) {
    return
  }

  loading.value = true
  try {
    let response
    if (searchKeyword.value.trim()) {
      response = await dubboApi.search(clusterId.value, searchKeyword.value, currentPage.value, pageSize.value) as unknown as {
        items: DubboInterfaceResponse[]
        total: number
        totalPages: number
      }
    } else {
      response = await dubboApi.list(clusterId.value, currentPage.value, pageSize.value) as unknown as {
        items: DubboInterfaceResponse[]
        total: number
        totalPages: number
      }
    }
    interfaces.value = response.items || []
    total.value = response.total
    totalPages.value = response.totalPages
  } catch (e) {
    console.error('Failed to load interfaces:', e)
    ElMessage.error('加载接口列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadInterfaces()
}

function clearSearch() {
  searchKeyword.value = ''
  currentPage.value = 1
  loadInterfaces()
}

async function refreshInterfaces() {
  try {
    await dubboApi.refresh(clusterId.value)
    ElMessage.success('刷新任务已启动')
  } catch (e) {
    console.error('Failed to refresh interfaces:', e)
    ElMessage.error('刷新失败')
  }
}

function goBack() {
  router.push(`/clusters/${clusterId.value}`)
}

function goToDetail(interfaceId: string) {
  router.push(`/clusters/${clusterId.value}/interfaces/${interfaceId}`)
}

onMounted(() => {
  loadInterfaces()
})

watch(clusterId, () => {
  loadInterfaces()
})
</script>
