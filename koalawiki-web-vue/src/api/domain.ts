/**
 * 业务领域 API
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import apiClient from './client'

/**
 * 服务类型枚举
 */
export type ServiceType = 'PROVIDER' | 'CONSUMER' | 'GATEWAY' | 'SCHEDULER' | 'MIDDLEWARE'

/**
 * 服务信息响应
 */
export interface ServiceInfoResponse {
  id: string
  warehouseId: string
  serviceName: string
  type: ServiceType
  description?: string
  isPrimary: boolean
  sortOrder: number
}

/**
 * 领域响应
 */
export interface DomainResponse {
  id: string
  clusterId: string
  name: string
  code: string
  description?: string
  owner?: string
  color: string
  sortOrder: number
  serviceCount: number
  services?: ServiceInfoResponse[]
  createdAt: string
  updatedAt: string
}

/**
 * 领域列表响应
 */
export interface DomainListResponse {
  items: DomainResponse[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

/**
 * 创建领域请求
 */
export interface CreateDomainRequest {
  name: string
  code: string
  description?: string
  owner?: string
  color?: string
  sortOrder?: number
}

/**
 * 更新领域请求
 */
export interface UpdateDomainRequest {
  name: string
  code: string
  description?: string
  owner?: string
  color?: string
  sortOrder?: number
}

/**
 * 添加服务请求
 */
export interface AddServiceRequest {
  warehouseId: string
  serviceName?: string
  serviceType?: ServiceType
  isPrimary?: boolean
}

/**
 * 业务领域 API
 */
export const domainApi = {
  /**
   * 获取领域列表
   */
  list: (clusterId: string, page = 1, pageSize = 20) => {
    return apiClient.get<DomainListResponse>(`/v1/clusters/${clusterId}/domains`, {
      params: { page, pageSize }
    })
  },

  /**
   * 获取所有领域（不分页）
   */
  getAll: (clusterId: string) => {
    return apiClient.get<DomainResponse[]>(`/v1/clusters/${clusterId}/domains/all`)
  },

  /**
   * 获取领域详情
   */
  getById: (clusterId: string, domainId: string) => {
    return apiClient.get<DomainResponse>(`/v1/clusters/${clusterId}/domains/${domainId}`)
  },

  /**
   * 根据编码获取领域
   */
  getByCode: (clusterId: string, code: string) => {
    return apiClient.get<DomainResponse>(`/v1/clusters/${clusterId}/domains/code/${code}`)
  },

  /**
   * 创建领域
   */
  create: (clusterId: string, request: CreateDomainRequest) => {
    return apiClient.post<DomainResponse>(`/v1/clusters/${clusterId}/domains`, request)
  },

  /**
   * 更新领域
   */
  update: (clusterId: string, domainId: string, request: UpdateDomainRequest) => {
    return apiClient.put<DomainResponse>(`/v1/clusters/${clusterId}/domains/${domainId}`, request)
  },

  /**
   * 删除领域
   */
  delete: (clusterId: string, domainId: string) => {
    return apiClient.delete(`/v1/clusters/${clusterId}/domains/${domainId}`)
  },

  /**
   * 获取领域内服务列表
   */
  getServices: (clusterId: string, domainId: string) => {
    return apiClient.get<ServiceInfoResponse[]>(`/v1/clusters/${clusterId}/domains/${domainId}/services`)
  },

  /**
   * 添加服务到领域
   */
  addService: (clusterId: string, domainId: string, request: AddServiceRequest) => {
    return apiClient.post(`/v1/clusters/${clusterId}/domains/${domainId}/services`, request)
  },

  /**
   * 从领域移除服务
   */
  removeService: (clusterId: string, domainId: string, warehouseId: string) => {
    return apiClient.delete(`/v1/clusters/${clusterId}/domains/${domainId}/services/${warehouseId}`)
  }
}

export default domainApi
