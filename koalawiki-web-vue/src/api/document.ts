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
    return apiClient.get<FileContentResponse>('/repository/FileContent', {
      params: { warehouseId, path }
    })
  },

  getDocumentCatalogs(warehouseId: string) {
    return apiClient.get('/repository/DocumentCatalogs', {
      params: { warehouseId }
    })
  }
}
