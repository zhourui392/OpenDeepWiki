import apiClient from './client'

export interface WarehouseResponse {
  id: string
  name: string
  organizationName?: string
  description?: string
  address: string
  branch: string
  status: 'PENDING' | 'SYNCING' | 'READY' | 'FAILED' | 'CANCELED'
  error?: string
  stars: number
  forks: number
  createdAt: Date
  updatedAt?: Date
  version?: string
  isEmbedded: boolean
  isRecommended: boolean
  userId: string
}

export interface SubmitWarehouseRequest {
  address: string
  branch?: string
  gitUserName?: string
  gitPassword?: string
}

export interface WarehouseListResponse {
  items: WarehouseResponse[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export const warehouseApi = {
  getWarehouseList(page: number, pageSize: number, keyword?: string) {
    return apiClient.get<WarehouseListResponse>('/warehouse/list', {
      params: { page, pageSize, keyword }
    })
  },

  getWarehouse(id: string) {
    return apiClient.get<WarehouseResponse>('/repository/detail', {
      params: { id }
    })
  },

  getDocumentTree(warehouseId: string) {
    return apiClient.get('/warehouse/document-tree', {
      params: { warehouseId }
    })
  },

  submitWarehouse(request: SubmitWarehouseRequest) {
    return apiClient.post<WarehouseResponse>('/warehouse/SubmitWarehouse', request)
  },

  // 手动触发同步
  triggerSync(warehouseId: string, forceSync: boolean = false) {
    return apiClient.post('/warehouse/sync/trigger', { warehouseId, forceSync })
  }
}
