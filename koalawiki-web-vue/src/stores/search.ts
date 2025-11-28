/**
 * 搜索状态管理
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { searchApi, type SearchResultItem, type SearchResultType } from '@/api/search'

/**
 * 搜索历史项
 */
interface SearchHistoryItem {
  query: string
  type?: SearchResultType
  timestamp: number
}

/**
 * 最大历史记录数
 */
const MAX_HISTORY_SIZE = 20

/**
 * 搜索 Store
 */
export const useSearchStore = defineStore('search', () => {
  /**
   * 搜索结果
   */
  const results = ref<SearchResultItem[]>([])

  /**
   * 当前搜索关键词
   */
  const query = ref('')

  /**
   * 搜索类型过滤
   */
  const typeFilter = ref<SearchResultType | undefined>(undefined)

  /**
   * 集群过滤
   */
  const clusterFilter = ref<string | undefined>(undefined)

  /**
   * 搜索耗时
   */
  const took = ref(0)

  /**
   * 总结果数
   */
  const total = ref(0)

  /**
   * 加载状态
   */
  const loading = ref(false)

  /**
   * 搜索历史
   */
  const history = ref<SearchHistoryItem[]>(loadHistoryFromStorage())

  /**
   * 是否有结果
   */
  const hasResults = computed(() => results.value.length > 0)

  /**
   * 是否正在搜索
   */
  const isSearching = computed(() => loading.value)

  /**
   * 从本地存储加载历史记录
   */
  function loadHistoryFromStorage(): SearchHistoryItem[] {
    try {
      const saved = localStorage.getItem('searchHistory')
      return saved ? JSON.parse(saved) : []
    } catch {
      return []
    }
  }

  /**
   * 保存历史记录到本地存储
   */
  function saveHistoryToStorage() {
    try {
      localStorage.setItem('searchHistory', JSON.stringify(history.value))
    } catch (e) {
      console.error('Failed to save search history:', e)
    }
  }

  /**
   * 添加搜索历史
   */
  function addToHistory(searchQuery: string, type?: SearchResultType) {
    if (!searchQuery.trim()) {
      return
    }

    // 移除重复项
    history.value = history.value.filter(h => h.query !== searchQuery)

    // 添加到头部
    history.value.unshift({
      query: searchQuery,
      type,
      timestamp: Date.now()
    })

    // 限制数量
    if (history.value.length > MAX_HISTORY_SIZE) {
      history.value = history.value.slice(0, MAX_HISTORY_SIZE)
    }

    saveHistoryToStorage()
  }

  /**
   * 执行搜索
   */
  async function search(searchQuery: string, type?: SearchResultType, clusterId?: string) {
    if (!searchQuery.trim()) {
      clearResults()
      return
    }

    query.value = searchQuery
    typeFilter.value = type
    clusterFilter.value = clusterId
    loading.value = true

    try {
      const response = await searchApi.search(searchQuery, type, clusterId) as unknown as {
        items: SearchResultItem[]
        total: number
        took: number
      }

      results.value = response.items
      total.value = response.total
      took.value = response.took

      addToHistory(searchQuery, type)
    } catch (e) {
      console.error('Search failed:', e)
      results.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  /**
   * 搜索服务
   */
  async function searchServices(searchQuery: string) {
    await search(searchQuery, 'SERVICE')
  }

  /**
   * 搜索接口
   */
  async function searchInterfaces(searchQuery: string) {
    await search(searchQuery, 'INTERFACE')
  }

  /**
   * 搜索文档
   */
  async function searchDocuments(searchQuery: string) {
    await search(searchQuery, 'DOCUMENT')
  }

  /**
   * 清除搜索结果
   */
  function clearResults() {
    results.value = []
    query.value = ''
    total.value = 0
    took.value = 0
  }

  /**
   * 清除搜索历史
   */
  function clearHistory() {
    history.value = []
    saveHistoryToStorage()
  }

  /**
   * 从历史记录中删除
   */
  function removeFromHistory(searchQuery: string) {
    history.value = history.value.filter(h => h.query !== searchQuery)
    saveHistoryToStorage()
  }

  return {
    results,
    query,
    typeFilter,
    clusterFilter,
    took,
    total,
    loading,
    history,
    hasResults,
    isSearching,
    search,
    searchServices,
    searchInterfaces,
    searchDocuments,
    clearResults,
    clearHistory,
    removeFromHistory,
    addToHistory
  }
})
