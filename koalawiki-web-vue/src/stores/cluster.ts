/**
 * 集群状态管理
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { clusterApi, type ClusterResponse, type ClusterStatisticsResponse } from '@/api/cluster'
import { domainApi, type DomainResponse } from '@/api/domain'

/**
 * 集群 Store
 */
export const useClusterStore = defineStore('cluster', () => {
  /**
   * 集群列表
   */
  const clusters = ref<ClusterResponse[]>([])

  /**
   * 当前选中的集群
   */
  const currentCluster = ref<ClusterResponse | null>(null)

  /**
   * 当前集群的领域列表
   */
  const domains = ref<DomainResponse[]>([])

  /**
   * 当前集群统计信息
   */
  const statistics = ref<ClusterStatisticsResponse | null>(null)

  /**
   * 加载状态
   */
  const loading = ref(false)

  /**
   * 错误信息
   */
  const error = ref<string | null>(null)

  /**
   * 当前集群ID
   */
  const currentClusterId = computed(() => currentCluster.value?.id || null)

  /**
   * 是否有集群选中
   */
  const hasCluster = computed(() => currentCluster.value !== null)

  /**
   * 加载集群列表
   */
  async function loadClusters() {
    loading.value = true
    error.value = null

    try {
      const response = await clusterApi.getActiveClusters() as unknown as ClusterResponse[]
      clusters.value = response

      // 如果有持久化的集群ID，尝试恢复选中状态
      const savedClusterId = localStorage.getItem('currentClusterId')
      if (savedClusterId) {
        const savedCluster = clusters.value.find(c => c.id === savedClusterId)
        if (savedCluster) {
          await selectCluster(savedCluster)
        } else if (clusters.value.length > 0) {
          await selectCluster(clusters.value[0])
        }
      } else if (clusters.value.length > 0) {
        await selectCluster(clusters.value[0])
      }
    } catch (e) {
      error.value = '加载集群列表失败'
      console.error('Failed to load clusters:', e)
    } finally {
      loading.value = false
    }
  }

  /**
   * 选择集群
   */
  async function selectCluster(cluster: ClusterResponse) {
    currentCluster.value = cluster
    localStorage.setItem('currentClusterId', cluster.id)

    // 加载集群的领域列表和统计信息
    await Promise.all([
      loadDomains(cluster.id),
      loadStatistics(cluster.id)
    ])
  }

  /**
   * 根据ID选择集群
   */
  async function selectClusterById(clusterId: string) {
    const cluster = clusters.value.find(c => c.id === clusterId)
    if (cluster) {
      await selectCluster(cluster)
    } else {
      try {
        const response = await clusterApi.getById(clusterId) as unknown as ClusterResponse
        await selectCluster(response)
      } catch (e) {
        error.value = '集群不存在'
        console.error('Failed to load cluster:', e)
      }
    }
  }

  /**
   * 加载领域列表
   */
  async function loadDomains(clusterId: string) {
    try {
      const response = await domainApi.getAll(clusterId) as unknown as DomainResponse[]
      domains.value = response
    } catch (e) {
      console.error('Failed to load domains:', e)
      domains.value = []
    }
  }

  /**
   * 加载统计信息
   */
  async function loadStatistics(clusterId: string) {
    try {
      const response = await clusterApi.getStatistics(clusterId) as unknown as ClusterStatisticsResponse
      statistics.value = response
    } catch (e) {
      console.error('Failed to load statistics:', e)
      statistics.value = null
    }
  }

  /**
   * 刷新当前集群数据
   */
  async function refreshCurrentCluster() {
    if (currentCluster.value) {
      await selectCluster(currentCluster.value)
    }
  }

  /**
   * 清除选中状态
   */
  function clearSelection() {
    currentCluster.value = null
    domains.value = []
    statistics.value = null
    localStorage.removeItem('currentClusterId')
  }

  return {
    clusters,
    currentCluster,
    currentClusterId,
    domains,
    statistics,
    loading,
    error,
    hasCluster,
    loadClusters,
    selectCluster,
    selectClusterById,
    loadDomains,
    loadStatistics,
    refreshCurrentCluster,
    clearSelection
  }
})
