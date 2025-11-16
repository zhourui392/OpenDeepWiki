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

export const warehouseApi = {
  getWarehouseList(page: number, pageSize: number, keyword?: string) {
    return apiClient.get<WarehouseResponse[]>('/Warehouse/WarehouseList', {
      params: { page, pageSize, keyword }
    })
  },

  getWarehouse(id: string) {
    return apiClient.get<WarehouseResponse>('/Repository/Repository', {
      params: { id }
    })
  },

  getDocumentTree(warehouseId: string) {
    return apiClient.get('/Warehouse/GetDocumentTree', {
      params: { warehouseId }
    })
  }
}
