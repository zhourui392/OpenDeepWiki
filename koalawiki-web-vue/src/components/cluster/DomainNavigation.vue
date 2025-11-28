<template>
  <div class="domain-navigation">
    <!-- 集群选择器 -->
    <div class="cluster-selector p-4 border-b">
      <el-select
        v-model="selectedClusterId"
        placeholder="选择集群"
        class="w-full"
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

    <!-- 领域列表 -->
    <div class="domain-list p-2">
      <div
        v-for="domain in domains"
        :key="domain.id"
        class="domain-group mb-2"
      >
        <!-- 领域头部 -->
        <div
          class="domain-header flex items-center gap-2 px-3 py-2 rounded cursor-pointer hover:bg-gray-100"
          :class="{ 'bg-blue-50': isExpanded(domain.id) }"
          @click="toggleDomain(domain.id)"
        >
          <div
            class="w-2 h-2 rounded-full"
            :style="{ backgroundColor: domain.color }"
          ></div>
          <span class="flex-1 text-sm font-medium">{{ domain.name }}</span>
          <span class="text-xs text-gray-400">{{ domain.serviceCount }}</span>
          <el-icon :class="{ 'rotate-90': isExpanded(domain.id) }">
            <ArrowRight />
          </el-icon>
        </div>

        <!-- 服务列表 -->
        <transition name="expand">
          <div v-if="isExpanded(domain.id)" class="services-list ml-4">
            <div v-if="loadingDomain === domain.id" class="py-2 text-center">
              <el-icon class="is-loading text-blue-500">
                <Loading />
              </el-icon>
            </div>
            <div
              v-else-if="domainServices[domain.id]?.length === 0"
              class="py-2 text-xs text-gray-400 text-center"
            >
              暂无服务
            </div>
            <router-link
              v-else
              v-for="service in domainServices[domain.id]"
              :key="service.id"
              :to="`/repository/${service.warehouseId}/ai-documents`"
              class="service-item flex items-center gap-2 px-3 py-2 text-sm rounded hover:bg-gray-50"
              :class="{ 'bg-blue-50 text-blue-600': isActiveService(service.warehouseId) }"
            >
              <span class="service-type-icon">
                {{ getServiceTypeIcon(service.type) }}
              </span>
              <span class="truncate">{{ service.serviceName }}</span>
            </router-link>
          </div>
        </transition>
      </div>

      <!-- 无领域提示 -->
      <div v-if="domains.length === 0" class="text-center py-4 text-gray-400 text-sm">
        暂无领域数据
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 领域导航组件
 * 在侧边栏展示集群的领域和服务树形结构
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowRight, Loading } from '@element-plus/icons-vue'
import { useClusterStore } from '@/stores/cluster'
import { domainApi, type ServiceInfoResponse, type ServiceType } from '@/api/domain'

const route = useRoute()
const clusterStore = useClusterStore()

const selectedClusterId = ref<string>('')
const expandedDomains = ref<Set<string>>(new Set())
const domainServices = ref<Record<string, ServiceInfoResponse[]>>({})
const loadingDomain = ref<string | null>(null)

const clusters = computed(() => clusterStore.clusters)
const domains = computed(() => clusterStore.domains)

watch(() => clusterStore.currentCluster, (cluster) => {
  if (cluster) {
    selectedClusterId.value = cluster.id
  }
}, { immediate: true })

async function handleClusterChange(clusterId: string) {
  await clusterStore.selectClusterById(clusterId)
  expandedDomains.value.clear()
  domainServices.value = {}
}

function isExpanded(domainId: string): boolean {
  return expandedDomains.value.has(domainId)
}

async function toggleDomain(domainId: string) {
  if (expandedDomains.value.has(domainId)) {
    expandedDomains.value.delete(domainId)
  } else {
    expandedDomains.value.add(domainId)
    await loadDomainServices(domainId)
  }
}

async function loadDomainServices(domainId: string) {
  if (domainServices.value[domainId]) {
    return
  }

  const clusterId = clusterStore.currentClusterId
  if (!clusterId) {
    return
  }

  loadingDomain.value = domainId
  try {
    const services = await domainApi.getServices(clusterId, domainId) as unknown as ServiceInfoResponse[]
    domainServices.value[domainId] = services
  } catch (e) {
    console.error('Failed to load domain services:', e)
    domainServices.value[domainId] = []
  } finally {
    loadingDomain.value = null
  }
}

function isActiveService(warehouseId: string): boolean {
  return route.params.id === warehouseId
}

function getServiceTypeIcon(type: ServiceType): string {
  const icons: Record<ServiceType, string> = {
    PROVIDER: '🔧',
    CONSUMER: '📥',
    GATEWAY: '🚪',
    SCHEDULER: '⏰',
    MIDDLEWARE: '🔗'
  }
  return icons[type] || '📦'
}
</script>

<style scoped>
.domain-navigation {
  height: 100%;
  overflow-y: auto;
}

.expand-enter-active,
.expand-leave-active {
  transition: all 0.2s ease;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
}

.expand-enter-to,
.expand-leave-from {
  opacity: 1;
  max-height: 500px;
}

.rotate-90 {
  transform: rotate(90deg);
}
</style>
