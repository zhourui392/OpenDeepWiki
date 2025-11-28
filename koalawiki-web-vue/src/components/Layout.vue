<template>
  <div class="flex h-screen bg-gray-50">
    <!-- 左侧导航 -->
    <aside class="w-64 bg-white shadow-lg flex flex-col">
      <!-- Logo -->
      <div class="p-4 border-b">
        <router-link to="/" class="text-xl font-bold text-blue-600">
          OpenDeepWiki
        </router-link>
      </div>

      <!-- 全局搜索 -->
      <div class="p-4 border-b">
        <el-input
          v-model="searchQuery"
          placeholder="搜索..."
          size="small"
          @keyup.enter="goToSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <!-- 主菜单 -->
      <nav class="flex-1 overflow-y-auto p-4">
        <div class="mb-4">
          <p class="text-xs text-gray-400 uppercase mb-2">导航</p>
          <router-link
            v-for="item in mainMenuItems"
            :key="item.path"
            :to="item.path"
            class="flex items-center gap-2 px-4 py-2 mb-1 rounded hover:bg-gray-100"
            :class="{ 'bg-blue-50 text-blue-600': isActive(item.path) }"
          >
            <span>{{ item.icon }}</span>
            <span>{{ item.label }}</span>
          </router-link>
        </div>

        <!-- 集群导航 -->
        <div v-if="hasCluster" class="mb-4">
          <p class="text-xs text-gray-400 uppercase mb-2">服务集群</p>
          <DomainNavigation />
        </div>
      </nav>

      <!-- 底部菜单 -->
      <div class="p-4 border-t">
        <router-link
          v-for="item in bottomMenuItems"
          :key="item.path"
          :to="item.path"
          class="flex items-center gap-2 px-4 py-2 mb-1 rounded hover:bg-gray-100 text-sm"
          :class="{ 'bg-blue-50 text-blue-600': isActive(item.path) }"
        >
          <span>{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </router-link>
      </div>
    </aside>

    <!-- 主内容区域 -->
    <main class="flex-1 overflow-auto">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
/**
 * 布局组件
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { useClusterStore } from '@/stores/cluster'
import DomainNavigation from '@/components/cluster/DomainNavigation.vue'

const route = useRoute()
const router = useRouter()
const clusterStore = useClusterStore()

const searchQuery = ref('')

const hasCluster = computed(() => clusterStore.hasCluster)

const mainMenuItems = [
  { path: '/', label: '首页', icon: '🏠' },
  { path: '/clusters/default', label: '服务集群', icon: '🔗' },
  { path: '/warehouses', label: '仓库管理', icon: '📦' },
  { path: '/agents', label: 'Agent 管理', icon: '🤖' },
  { path: '/business-flow', label: '业务流程', icon: '🔄' },
  { path: '/search', label: '全局搜索', icon: '🔍' }
]

const bottomMenuItems = [
  { path: '/admin/clusters', label: '集群管理', icon: '⚙️' }
]

function isActive(path: string): boolean {
  if (path === '/') {
    return route.path === '/'
  }
  return route.path.startsWith(path)
}

function goToSearch() {
  if (searchQuery.value.trim()) {
    router.push({
      path: '/search',
      query: { q: searchQuery.value }
    })
  }
}

onMounted(() => {
  clusterStore.loadClusters()
})
</script>

<style scoped>
aside {
  min-width: 256px;
  max-width: 256px;
}
</style>
