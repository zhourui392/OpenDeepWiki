/**
 * 搜索 API
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import apiClient from './client'

/**
 * 搜索结果类型
 */
export type SearchResultType = 'SERVICE' | 'INTERFACE' | 'DOCUMENT' | 'DOMAIN'

/**
 * 搜索结果项
 */
export interface SearchResultItem {
  id: string
  type: SearchResultType
  title: string
  description?: string
  matchedContent?: string
  clusterId?: string
  clusterName?: string
  domainId?: string
  domainName?: string
  warehouseId?: string
  warehouseName?: string
  score: number
  createdAt?: string
  updatedAt?: string
}

/**
 * 搜索结果响应
 */
export interface SearchResponse {
  items: SearchResultItem[]
  total: number
  query: string
  took: number
}

/**
 * 影响分析请求
 */
export interface ImpactAnalysisRequest {
  serviceId?: string
  interfaceName?: string
  changeType: 'ADD_FIELD' | 'REMOVE_FIELD' | 'MODIFY_SIGNATURE' | 'DEPRECATE'
}

/**
 * 影响分析结果
 */
export interface ImpactAnalysisResponse {
  targetService?: string
  targetInterface?: string
  changeType: string
  impactedServices: ImpactedService[]
  totalImpact: number
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  summary: string
}

/**
 * 受影响的服务
 */
export interface ImpactedService {
  warehouseId: string
  serviceName: string
  impactType: 'DIRECT' | 'INDIRECT'
  impactLevel: number
  affectedClasses: string[]
}

/**
 * 搜索 API
 */
export const searchApi = {
  /**
   * 全局搜索
   */
  search: (query: string, type?: SearchResultType, clusterId?: string, limit = 20) => {
    const params: Record<string, unknown> = { q: query, limit }
    if (type) {
      params.type = type
    }
    if (clusterId) {
      params.cluster = clusterId
    }
    return apiClient.get<SearchResponse>('/v1/search', { params })
  },

  /**
   * 搜索服务
   */
  searchServices: (query: string, limit = 20) => {
    return apiClient.get<SearchResponse>('/v1/search/services', {
      params: { q: query, limit }
    })
  },

  /**
   * 搜索接口
   */
  searchInterfaces: (query: string, limit = 20) => {
    return apiClient.get<SearchResponse>('/v1/search/interfaces', {
      params: { q: query, limit }
    })
  },

  /**
   * 搜索文档
   */
  searchDocuments: (query: string, limit = 20) => {
    return apiClient.get<SearchResponse>('/v1/search/documents', {
      params: { q: query, limit }
    })
  },

  /**
   * 获取相关服务推荐
   */
  getRelatedServices: (serviceId: string) => {
    return apiClient.get<SearchResultItem[]>(`/v1/search/related/${serviceId}`)
  },

  /**
   * 影响分析
   */
  analyzeImpact: (request: ImpactAnalysisRequest) => {
    return apiClient.post<ImpactAnalysisResponse>('/v1/search/impact-analysis', request)
  }
}

export default searchApi
