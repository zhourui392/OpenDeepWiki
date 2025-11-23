import apiClient from './client'

export interface EntryPoint {
  type: string
  path: string
  className: string
  methodName: string
}

export interface CallNode {
  service: string
  className: string
  method: string
  type: string
  depth: number
  children: CallNode[]
}

export interface CallChain {
  chainId: string
  root: CallNode
  nodes: CallNode[]
  maxDepth: number
}

export interface BusinessFlowResult {
  flowId: string
  entryPoint: EntryPoint
  callChain: CallChain
  mermaidDiagram: string
  nodeCount: number
  maxDepth: number
}

export interface ServiceDependency {
  sourceService: string
  targetService: string
  interfaceName: string
  type: string
  sourceClass: string
  sourceMethod: string
}

export interface ServiceNode {
  serviceName: string
  providedInterfaces: string[]
  requiredInterfaces: string[]
}

export interface ServiceDependencyGraph {
  services: Record<string, ServiceNode>
  dependencies: ServiceDependency[]
}

export const businessFlowApi = {
  analyzeDependencies(projectPaths: string[]): Promise<ServiceDependencyGraph> {
    return apiClient.post('/v1/business-flow/dependencies', { projectPaths })
  },

  generateFlow(params: {
    projectPaths: string[]
    projectPath: string
    entryPoint: EntryPoint
    maxDepth?: number
  }): Promise<BusinessFlowResult> {
    return apiClient.post('/v1/business-flow/generate', params)
  },

  generateAllFlows(params: {
    projectPaths: string[]
    projectPath: string
    maxDepth?: number
  }): Promise<BusinessFlowResult[]> {
    return apiClient.post('/v1/business-flow/generate-all', params)
  }
}
