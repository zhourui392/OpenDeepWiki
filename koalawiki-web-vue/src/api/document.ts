import apiClient from './client'

export interface FileContentResponse {
  path: string
  content: string
  size: number
  fileType: string
  isBinary: boolean
  encoding: string
}

export const documentApi = {
  getFileContent(warehouseId: string, path: string) {
    return apiClient.get<FileContentResponse>('/Repository/FileContent', {
      params: { warehouseId, path }
    })
  },

  getDocumentCatalogs(warehouseId: string) {
    return apiClient.get('/Repository/DocumentCatalogs', {
      params: { warehouseId }
    })
  }
}
