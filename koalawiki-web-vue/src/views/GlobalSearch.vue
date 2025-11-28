<template>
  <div class="global-search p-6">
    <!-- 搜索区域 -->
    <div class="search-header mb-6">
      <div class="flex items-center gap-4 mb-4">
        <el-input
          v-model="searchQuery"
          size="large"
          placeholder="搜索服务、接口、文档..."
          clearable
          class="flex-1"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" size="large" @click="handleSearch">
          搜索
        </el-button>
      </div>

      <!-- 快捷过滤 -->
      <div class="flex items-center gap-2">
        <span class="text-sm text-gray-500">类型：</span>
        <el-radio-group v-model="typeFilter" size="small" @change="handleSearch">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="SERVICE">服务</el-radio-button>
          <el-radio-button value="INTERFACE">接口</el-radio-button>
          <el-radio-button value="DOCUMENT">文档</el-radio-button>
        </el-radio-group>

        <span class="text-sm text-gray-500 ml-4">集群：</span>
        <el-select
          v-model="clusterFilter"
          placeholder="全部集群"
          size="small"
          clearable
          class="w-40"
          @change="handleSearch"
        >
          <el-option
            v-for="cluster in clusters"
            :key="cluster.id"
            :label="cluster.name"
            :value="cluster.id"
          />
        </el-select>
      </div>
    </div>

    <!-- 搜索历史 -->
    <div v-if="!hasSearched && searchHistory.length > 0" class="search-history mb-6">
      <div class="flex items-center justify-between mb-2">
        <h3 class="text-sm font-medium text-gray-500">搜索历史</h3>
        <el-button text size="small" @click="clearHistory">清除</el-button>
      </div>
      <div class="flex flex-wrap gap-2">
        <el-tag
          v-for="(item, index) in searchHistory"
          :key="index"
          class="cursor-pointer"
          closable
          @click="searchFromHistory(item.query)"
          @close="removeFromHistory(item.query)"
        >
          {{ item.query }}
        </el-tag>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex justify-center items-center h-64">
      <el-icon class="is-loading text-4xl text-blue-500">
        <Loading />
      </el-icon>
    </div>

    <!-- 搜索结果 -->
    <div v-else-if="hasSearched" class="search-results">
      <!-- 结果统计 -->
      <div class="result-stats mb-4 text-sm text-gray-500">
        找到 {{ total }} 条结果，耗时 {{ took }}ms
      </div>

      <!-- 结果列表 -->
      <div v-if="results.length > 0" class="result-list space-y-4">
        <SearchResultCard
          v-for="item in results"
          :key="item.id"
          :result="item"
          @click="handleResultClick(item)"
        />
      </div>

      <!-- 无结果 -->
      <div v-else class="flex flex-col items-center justify-center h-48 text-gray-400">
        <el-icon class="text-5xl mb-4"><Search /></el-icon>
        <p>没有找到相关结果</p>
        <p class="text-sm mt-2">尝试使用不同的关键词搜索</p>
      </div>
    </div>

    <!-- 初始状态 -->
    <div v-else class="initial-state">
      <div class="flex flex-col items-center justify-center h-64 text-gray-400">
        <el-icon class="text-6xl mb-4"><Search /></el-icon>
        <p class="text-lg">输入关键词开始搜索</p>
        <p class="text-sm mt-2">支持搜索服务名称、接口名称、文档内容</p>
      </div>

      <!-- 热门搜索 -->
      <div class="hot-search mt-8">
        <h3 class="text-sm font-medium text-gray-500 mb-3">热门搜索</h3>
        <div class="flex flex-wrap gap-2">
          <el-tag
            v-for="keyword in hotKeywords"
            :key="keyword"
            class="cursor-pointer"
            @click="searchFromHistory(keyword)"
          >
            {{ keyword }}
          </el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 全局搜索页面
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Loading } from '@element-plus/icons-vue'
import { useSearchStore } from '@/stores/search'
import { useClusterStore } from '@/stores/cluster'
import type { SearchResultItem, SearchResultType } from '@/api/search'
import SearchResultCard from '@/components/search/SearchResultCard.vue'

const router = useRouter()
const searchStore = useSearchStore()
const clusterStore = useClusterStore()

const searchQuery = ref('')
const typeFilter = ref<SearchResultType | ''>('')
const clusterFilter = ref<string>('')
const hasSearched = ref(false)

const clusters = computed(() => clusterStore.clusters)
const results = computed(() => searchStore.results)
const total = computed(() => searchStore.total)
const took = computed(() => searchStore.took)
const loading = computed(() => searchStore.loading)
const searchHistory = computed(() => searchStore.history)

const hotKeywords = ref([
  'OrderService',
  'PaymentFacade',
  '创建订单',
  '商品查询',
  '用户认证'
])

onMounted(() => {
  clusterStore.loadClusters()
})

async function handleSearch() {
  if (!searchQuery.value.trim()) {
    return
  }

  hasSearched.value = true
  await searchStore.search(
    searchQuery.value,
    typeFilter.value || undefined,
    clusterFilter.value || undefined
  )
}

function searchFromHistory(query: string) {
  searchQuery.value = query
  handleSearch()
}

function clearHistory() {
  searchStore.clearHistory()
}

function removeFromHistory(query: string) {
  searchStore.removeFromHistory(query)
}

function handleResultClick(item: SearchResultItem) {
  switch (item.type) {
    case 'SERVICE':
      router.push(`/repository/${item.warehouseId}/ai-documents`)
      break
    case 'INTERFACE':
      if (item.clusterId) {
        router.push(`/clusters/${item.clusterId}/interfaces/${item.id}`)
      }
      break
    case 'DOCUMENT':
      router.push(`/ai-documents/${item.id}`)
      break
    case 'DOMAIN':
      if (item.clusterId) {
        router.push(`/clusters/${item.clusterId}/domains/${item.id}`)
      }
      break
  }
}
</script>

<style scoped>
.global-search {
  max-width: 1000px;
  margin: 0 auto;
}
</style>
