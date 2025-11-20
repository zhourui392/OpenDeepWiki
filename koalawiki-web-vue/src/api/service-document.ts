import apiClient from './client'

export interface ServiceDocumentLibrary {
  id: string
  warehouseId: string
  serviceId: string
  serviceName: string
  description?: string
  docType: string
  promptTemplateId?: string
  agentType?: string
  sourceGlobs?: string[]
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateServiceRequest {
  serviceId: string
  serviceName: string
  description?: string
  docType: string
  promptTemplateId?: string
  agentType?: string
  sourceGlobs?: string[]
  enabled?: boolean
}

export interface UpdateServiceRequest {
  serviceName: string
  description?: string
  docType: string
  promptTemplateId?: string
  agentType?: string
  sourceGlobs?: string[]
  enabled: boolean
}

export const serviceDocumentApi = {
  /**
   * 获取仓库下的所有服务配置
   */
  listServices(warehouseId: string) {
    return apiClient.get<ServiceDocumentLibrary[]>(
      `/v1/warehouses/${warehouseId}/services`
    )
  },

  /**
   * 获取单个服务配置
   */
  getService(warehouseId: string, serviceId: string) {
    return apiClient.get<ServiceDocumentLibrary>(
      `/v1/warehouses/${warehouseId}/services/${serviceId}`
    )
  },

  /**
   * 创建服务配置
   */
  createService(warehouseId: string, request: CreateServiceRequest) {
    return apiClient.post<ServiceDocumentLibrary>(
      `/v1/warehouses/${warehouseId}/services`,
      request
    )
  },

  /**
   * 更新服务配置
   */
  updateService(warehouseId: string, serviceId: string, request: UpdateServiceRequest) {
    return apiClient.put<ServiceDocumentLibrary>(
      `/v1/warehouses/${warehouseId}/services/${serviceId}`,
      request
    )
  },

  /**
   * 删除服务配置
   */
  deleteService(warehouseId: string, serviceId: string) {
    return apiClient.delete(
      `/v1/warehouses/${warehouseId}/services/${serviceId}`
    )
  }
}
