<template>
  <div class="cluster-overview p-6">
    <!-- 头部区域 -->
    <div class="flex justify-between items-center mb-6">
      <div class="flex items-center gap-4">
        <h1 class="text-2xl font-bold">{{ currentCluster?.name || '服务集群' }}</h1>
        <el-select
          v-model="selectedClusterId"
          placeholder="选择集群"
          class="w-48"
          @change="handleClusterChange"
        >
          <el-option
            v-for="cluster in clusters"
            :key="cluster.id"
            :label="cluster.name"
            :value="cluster.id"
          />
        </el-select>
      </div>
      <div class="flex gap-2">
        <el-button @click="refreshData">
          <span class="mr-1">🔄</span> 刷新
        </el-button>
        <el-button type="primary" @click="goToAdmin">
          <span class="mr-1">⚙️</span> 管理
        </el-button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex justify-center items-center h-64">
      <el-icon class="is-loading text-4xl text-blue-500">
        <Loading />
      </el-icon>
    </div>

    <!-- 主内容区域 -->
    <div v-else-if="currentCluster" class="space-y-6">
      <!-- 统计卡片 -->
      <div class="grid grid-cols-4 gap-4">
        <StatCard
          title="服务数"
          :value="statistics?.warehouseCount || 0"
          icon="📦"
          color="blue"
        />
        <StatCard
          title="领域数"
          :value="statistics?.domainCount || 0"
          icon="🏷️"
          color="green"
        />
        <StatCard
          title="接口数"
          :value="statistics?.interfaceCount || 0"
          icon="🔌"
          color="purple"
        />
        <StatCard
          title="文档数"
          :value="statistics?.documentCount || 0"
          icon="📄"
          color="orange"
        />
      </div>

      <!-- 集群信息 -->
      <div class="bg-white rounded-lg shadow p-4">
        <h2 class="text-lg font-semibold mb-3">集群信息</h2>
        <div class="grid grid-cols-2 gap-4 text-sm">
          <div>
            <span class="text-gray-500">编码：</span>
            <span class="font-mono">{{ currentCluster.code }}</span>
          </div>
          <div>
            <span class="text-gray-500">负责人：</span>
            <span>{{ currentCluster.owner || '-' }}</span>
          </div>
          <div class="col-span-2">
            <span class="text-gray-500">技术栈：</span>
            <span>{{ currentCluster.techStack || '-' }}</span>
          </div>
          <div class="col-span-2">
            <span class="text-gray-500">描述：</span>
            <span>{{ currentCluster.description || '-' }}</span>
          </div>
        </div>
      </div>

      <!-- 服务拓扑图 -->
      <div class="bg-white rounded-lg shadow p-4">
        <div class="flex justify-between items-center mb-4">
          <h2 class="text-lg font-semibold">服务拓扑</h2>
          <div class="flex gap-2">
            <el-radio-group v-model="topologyView" size="small">
              <el-radio-button value="domain">按领域</el-radio-button>
              <el-radio-button value="service">按服务</el-radio-button>
            </el-radio-group>
          </div>
        </div>
        <ServiceTopology
          :cluster-id="currentCluster.id"
          :domains="domains"
          :view-type="topologyView"
          @node-click="handleNodeClick"
        />
      </div>

      <!-- 领域列表 -->
      <div class="bg-white rounded-lg shadow p-4">
        <h2 class="text-lg font-semibold mb-4">业务领域</h2>
        <div class="grid grid-cols-5 gap-4">
          <DomainCard
            v-for="domain in domains"
            :key="domain.id"
            :domain="domain"
            @click="goToDomain(domain)"
          />
          <div
            v-if="domains.length === 0"
            class="col-span-5 text-center py-8 text-gray-400"
          >
            暂无领域数据，请先在管理页面创建领域
          </div>
        </div>
      </div>

      <!-- 热门接口 -->
      <div class="bg-white rounded-lg shadow p-4">
        <div class="flex justify-between items-center mb-4">
          <h2 class="text-lg font-semibold">热门接口 Top 10</h2>
          <el-button text type="primary" @click="goToInterfaces">
            查看全部 →
          </el-button>
        </div>
        <HotInterfaceList
          :cluster-id="currentCluster.id"
          :limit="10"
          @interface-click="goToInterface"
        />
      </div>
    </div>

    <!-- 无集群提示 -->
    <div v-else class="flex flex-col items-center justify-center h-64 text-gray-400">
      <p class="text-lg mb-4">暂无服务集群</p>
      <el-button type="primary" @click="goToAdmin">创建集群</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 集群概览页面
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import { useClusterStore } from '@/stores/cluster'
import type { DomainResponse } from '@/api/domain'
import StatCard from '@/components/cluster/StatCard.vue'
import DomainCard from '@/components/cluster/DomainCard.vue'
import ServiceTopology from '@/components/cluster/ServiceTopology.vue'
import HotInterfaceList from '@/components/cluster/HotInterfaceList.vue'

const router = useRouter()
const route = useRoute()
const clusterStore = useClusterStore()

const selectedClusterId = ref<string>('')
const topologyView = ref<'domain' | 'service'>('domain')

const clusters = computed(() => clusterStore.clusters)
const currentCluster = computed(() => clusterStore.currentCluster)
const domains = computed(() => clusterStore.domains)
const statistics = computed(() => clusterStore.statistics)
const loading = computed(() => clusterStore.loading)

onMounted(async () => {
  await clusterStore.loadClusters()

  // 如果路由中有集群ID，则选中该集群
  const clusterId = route.params.clusterId as string
  if (clusterId) {
    await clusterStore.selectClusterById(clusterId)
  }

  if (currentCluster.value) {
    selectedClusterId.value = currentCluster.value.id
  }
})

watch(currentCluster, (newCluster) => {
  if (newCluster) {
    selectedClusterId.value = newCluster.id
  }
})

async function handleClusterChange(clusterId: string) {
  await clusterStore.selectClusterById(clusterId)
  router.replace({ params: { clusterId } })
}

async function refreshData() {
  await clusterStore.refreshCurrentCluster()
}

function goToAdmin() {
  router.push('/admin/clusters')
}

function goToDomain(domain: DomainResponse) {
  if (currentCluster.value) {
    router.push(`/clusters/${currentCluster.value.id}/domains/${domain.code}`)
  }
}

function goToInterfaces() {
  if (currentCluster.value) {
    router.push(`/clusters/${currentCluster.value.id}/interfaces`)
  }
}

function goToInterface(interfaceId: string) {
  if (currentCluster.value) {
    router.push(`/clusters/${currentCluster.value.id}/interfaces/${interfaceId}`)
  }
}

function handleNodeClick(node: { type: string; id: string; code?: string }) {
  if (!currentCluster.value) {
    return
  }

  if (node.type === 'domain' && node.code) {
    router.push(`/clusters/${currentCluster.value.id}/domains/${node.code}`)
  } else if (node.type === 'service') {
    router.push(`/repository/${node.id}/ai-documents`)
  }
}
</script>

<style scoped>
.cluster-overview {
  min-height: calc(100vh - 64px);
}
</style>
