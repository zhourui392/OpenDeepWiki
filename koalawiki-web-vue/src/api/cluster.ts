/**
 * 服务集群 API
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import apiClient from './client'

/**
 * 集群状态枚举
 */
export type ClusterStatus = 'ACTIVE' | 'INACTIVE' | 'ARCHIVED'

/**
 * 集群响应
 */
export interface ClusterResponse {
  id: string
  name: string
  code: string
  description?: string
  techStack?: string
  owner?: string
  status: ClusterStatus
  warehouseCount: number
  domainCount: number
  domains?: DomainBriefResponse[]
  createdAt: string
  updatedAt: string
}

/**
 * 领域简要响应
 */
export interface DomainBriefResponse {
  id: string
  name: string
  code: string
  color: string
  serviceCount: number
}

/**
 * 集群列表响应
 */
export interface ClusterListResponse {
  items: ClusterResponse[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

/**
 * 创建集群请求
 */
export interface CreateClusterRequest {
  name: string
  code: string
  description?: string
  techStack?: string
  owner?: string
}

/**
 * 更新集群请求
 */
export interface UpdateClusterRequest {
  name: string
  code: string
  description?: string
  techStack?: string
  owner?: string
  status?: ClusterStatus
}

/**
 * 添加仓库请求
 */
export interface AddWarehouseRequest {
  warehouseId: string
  sortOrder?: number
}

/**
 * 集群统计响应
 */
export interface ClusterStatisticsResponse {
  clusterId: string
  warehouseCount: number
  domainCount: number
  interfaceCount: number
  documentCount: number
}

/**
 * 服务集群 API
 */
export const clusterApi = {
  /**
   * 获取集群列表
   */
  list: (page = 1, pageSize = 20, status?: ClusterStatus) => {
    const params: Record<string, unknown> = { page, pageSize }
    if (status) {
      params.status = status
    }
    return apiClient.get<ClusterListResponse>('/v1/clusters', { params })
  },

  /**
   * 获取活跃集群列表
   */
  getActiveClusters: () => {
    return apiClient.get<ClusterResponse[]>('/v1/clusters/active')
  },

  /**
   * 获取集群详情
   */
  getById: (clusterId: string) => {
    return apiClient.get<ClusterResponse>(`/v1/clusters/${clusterId}`)
  },

  /**
   * 根据编码获取集群
   */
  getByCode: (code: string) => {
    return apiClient.get<ClusterResponse>(`/v1/clusters/code/${code}`)
  },

  /**
   * 创建集群
   */
  create: (request: CreateClusterRequest) => {
    return apiClient.post<ClusterResponse>('/v1/clusters', request)
  },

  /**
   * 更新集群
   */
  update: (clusterId: string, request: UpdateClusterRequest) => {
    return apiClient.put<ClusterResponse>(`/v1/clusters/${clusterId}`, request)
  },

  /**
   * 删除集群
   */
  delete: (clusterId: string) => {
    return apiClient.delete(`/v1/clusters/${clusterId}`)
  },

  /**
   * 获取集群仓库ID列表
   */
  getWarehouseIds: (clusterId: string) => {
    return apiClient.get<string[]>(`/v1/clusters/${clusterId}/warehouses`)
  },

  /**
   * 添加仓库到集群
   */
  addWarehouse: (clusterId: string, request: AddWarehouseRequest) => {
    return apiClient.post(`/v1/clusters/${clusterId}/warehouses`, request)
  },

  /**
   * 从集群移除仓库
   */
  removeWarehouse: (clusterId: string, warehouseId: string) => {
    return apiClient.delete(`/v1/clusters/${clusterId}/warehouses/${warehouseId}`)
  },

  /**
   * 获取集群统计信息
   */
  getStatistics: (clusterId: string) => {
    return apiClient.get<ClusterStatisticsResponse>(`/v1/clusters/${clusterId}/statistics`)
  }
}

export default clusterApi
