import apiClient from './client'

export interface AIDocument {
  id: string
  warehouseId: string
  sourceFile: string
  title: string
  content: string
  status: 'DRAFT' | 'COMPLETED' | 'FAILED'
  agentType: string
  errorMessage?: string
  createdAt: string
  updatedAt: string
}

export interface GenerationTask {
  id: string
  warehouseId: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'
  totalFiles: number
  completedFiles: number
  failedFiles: number
  agentType: string
  startedAt?: string
  completedAt?: string
  createdAt: string
}

export interface DocStats {
  totalCount: number
  completedCount: number
  failedCount: number
  successRate: number
  agentUsage: Record<string, number>
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface GenerateDocsRequest {
  agentType?: string
}

export const aiDocumentApi = {
  /**
   * 生成项目架构文档(新)
   */
  generateProjectDoc(warehouseId: string, request?: GenerateDocsRequest) {
    return apiClient.post<{
      documentId: string
      title: string
      message: string
    }>(
      `/v1/warehouses/${warehouseId}/generate-project-doc`,
      request || {}
    )
  },

  /**
   * 触发文档生成(旧-按文件批量生成)
   */
  generateDocs(warehouseId: string, request?: GenerateDocsRequest) {
    return apiClient.post<string>(
      `/v1/warehouses/${warehouseId}/generate-docs`,
      request || {}
    )
  },

  /**
   * 获取文档列表(分页)
   */
  listDocuments(warehouseId: string, params?: {
    page?: number
    size?: number
    status?: string
  }) {
    return apiClient.get<PageResponse<AIDocument>>(
      `/v1/warehouses/${warehouseId}/ai-documents`,
      { params }
    )
  },

  /**
   * 获取文档详情
   */
  getDocument(id: string) {
    return apiClient.get<AIDocument>(`/v1/ai-documents/${id}`)
  },

  /**
   * 获取统计信息
   */
  getDocStats(warehouseId: string) {
    return apiClient.get<DocStats>(`/v1/warehouses/${warehouseId}/doc-stats`)
  }
}
