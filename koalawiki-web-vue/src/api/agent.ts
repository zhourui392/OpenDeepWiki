import apiClient from './client'

export interface AgentTemplate {
  id?: string
  name: string
  description?: string
  template: string
  createdAt?: Date
  updatedAt?: Date
}

export const agentApi = {
  getAgentList() {
    return apiClient.get<AgentTemplate[]>('/ai-prompt-template/list')
  },

  getAgent(id: string) {
    return apiClient.get<AgentTemplate>(`/ai-prompt-template/${id}`)
  },

  createAgent(agent: AgentTemplate) {
    return apiClient.post<AgentTemplate>('/ai-prompt-template', agent)
  },

  updateAgent(id: string, agent: AgentTemplate) {
    return apiClient.put<AgentTemplate>(`/ai-prompt-template/${id}`, agent)
  },

  deleteAgent(id: string) {
    return apiClient.delete(`/ai-prompt-template/${id}`)
  }
}
