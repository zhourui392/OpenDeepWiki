/**
 * Dubbo 接口 API
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import apiClient from './client'

/**
 * 方法参数响应
 */
export interface MethodParameterResponse {
  name: string
  type: string
  description?: string
  required: boolean
  index: number
}

/**
 * Dubbo方法响应
 */
export interface DubboMethodResponse {
  name: string
  returnType: string
  parameters?: MethodParameterResponse[]
  description?: string
  signature: string
  deprecated: boolean
  exceptions?: string[]
}

/**
 * Dubbo接口响应
 */
export interface DubboInterfaceResponse {
  id: string
  interfaceName: string
  simpleName: string
  version: string
  groupName: string
  providerWarehouseId: string
  providerServiceName: string
  description?: string
  methods?: DubboMethodResponse[]
  deprecated: boolean
  deprecatedReason?: string
  sourceFile?: string
  methodCount: number
  consumerCount: number
  consumerServiceIds?: string[]
  createdAt: string
  updatedAt: string
}

/**
 * Dubbo接口列表响应
 */
export interface DubboInterfaceListResponse {
  items: DubboInterfaceResponse[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

/**
 * Dubbo消费者响应
 */
export interface DubboConsumerResponse {
  id: string
  consumerWarehouseId: string
  consumerServiceName: string
  sourceClass: string
  sourceField: string
  createdAt: string
}

/**
 * 调用链响应
 */
export interface CallChainResponse {
  interfaceId: string
  interfaceName: string
  providerServiceName: string
  upstreamCount: number
  downstreamCount: number
  mermaidCode: string
}

/**
 * 接口统计响应
 */
export interface InterfaceStatisticsResponse {
  clusterId: string
  totalCount: number
}

/**
 * Dubbo 接口 API
 */
export const dubboApi = {
  /**
   * 获取接口列表
   */
  list: (clusterId: string, page = 1, pageSize = 20) => {
    return apiClient.get<DubboInterfaceListResponse>(`/v1/clusters/${clusterId}/dubbo-interfaces`, {
      params: { page, pageSize }
    })
  },

  /**
   * 搜索接口
   */
  search: (clusterId: string, keyword: string, page = 1, pageSize = 20, version?: string) => {
    const params: Record<string, unknown> = { keyword, page, pageSize }
    if (version) {
      params.version = version
    }
    return apiClient.get<DubboInterfaceListResponse>(`/v1/clusters/${clusterId}/dubbo-interfaces/search`, {
      params
    })
  },

  /**
   * 获取接口详情
   */
  getById: (clusterId: string, interfaceId: string) => {
    return apiClient.get<DubboInterfaceResponse>(`/v1/clusters/${clusterId}/dubbo-interfaces/${interfaceId}`)
  },

  /**
   * 获取接口消费者
   */
  getConsumers: (clusterId: string, interfaceId: string) => {
    return apiClient.get<DubboConsumerResponse[]>(`/v1/clusters/${clusterId}/dubbo-interfaces/${interfaceId}/consumers`)
  },

  /**
   * 根据服务获取提供的接口
   */
  getByService: (clusterId: string, warehouseId: string) => {
    return apiClient.get<DubboInterfaceResponse[]>(`/v1/clusters/${clusterId}/dubbo-interfaces/by-service/${warehouseId}`)
  },

  /**
   * 获取热门接口
   */
  getTopInterfaces: (clusterId: string, limit = 20) => {
    return apiClient.get<DubboInterfaceResponse[]>(`/v1/clusters/${clusterId}/dubbo-interfaces/top`, {
      params: { limit }
    })
  },

  /**
   * 获取服务消费的接口
   */
  getConsumedInterfaces: (clusterId: string, warehouseId: string) => {
    return apiClient.get<DubboInterfaceResponse[]>(`/v1/clusters/${clusterId}/dubbo-interfaces/consumed-by/${warehouseId}`)
  },

  /**
   * 刷新接口注册表
   */
  refresh: (clusterId: string) => {
    return apiClient.post(`/v1/clusters/${clusterId}/dubbo-interfaces/refresh`)
  },

  /**
   * 获取接口统计
   */
  getStatistics: (clusterId: string) => {
    return apiClient.get<InterfaceStatisticsResponse>(`/v1/clusters/${clusterId}/dubbo-interfaces/statistics`)
  },

  /**
   * 获取接口调用链
   */
  getCallChain: (clusterId: string, interfaceId: string, maxDepth = 3) => {
    return apiClient.get<CallChainResponse>(`/v1/clusters/${clusterId}/dubbo-interfaces/${interfaceId}/call-chain`, {
      params: { maxDepth }
    })
  }
}

export default dubboApi
