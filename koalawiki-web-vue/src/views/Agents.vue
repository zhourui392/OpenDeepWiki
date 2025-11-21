<template>
  <div class="container mx-auto p-6">
    <div class="flex justify-between items-center mb-6">
      <h1 class="text-2xl font-bold">Agent 管理</h1>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <div
        v-for="agent in agents"
        :key="agent.id"
        @click="editAgent(agent)"
        class="bg-white rounded-lg shadow p-4 cursor-pointer hover:shadow-lg transition"
      >
        <h3 class="font-bold text-lg mb-2">{{ agent.name }}</h3>
        <p class="text-sm text-gray-600 mb-3">{{ agent.description }}</p>
        <div class="flex justify-end gap-2">
          <button @click.stop="editAgent(agent)" class="text-blue-600 hover:underline text-sm">编辑</button>
        </div>
      </div>
    </div>

    <div v-if="showEditor" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4">
      <div class="bg-white rounded-lg w-full max-w-4xl h-5/6 flex flex-col">
        <div class="p-4 border-b flex justify-between items-center">
          <h2 class="text-xl font-bold">{{ editingAgent.id ? '编辑' : '新建' }} Agent</h2>
          <button @click="showEditor = false" class="text-gray-500 hover:text-gray-700">✕</button>
        </div>

        <div class="p-4 space-y-4 flex-1 overflow-auto">
          <div>
            <label class="block text-sm font-medium mb-1">名称</label>
            <input v-model="editingAgent.name" class="w-full px-3 py-2 border rounded" />
          </div>
          <div>
            <label class="block text-sm font-medium mb-1">描述</label>
            <input v-model="editingAgent.description" class="w-full px-3 py-2 border rounded" />
          </div>
          <div class="flex-1">
            <label class="block text-sm font-medium mb-1">模板内容 (Markdown)</label>
            <textarea
              v-model="editingAgent.template"
              class="w-full h-96 px-3 py-2 border rounded font-mono text-sm"
              placeholder="# Agent 模板&#10;&#10;在这里编写 markdown 格式的 agent 模板..."
            />
          </div>
        </div>

        <div class="p-4 border-t flex justify-end gap-2">
          <button @click="showEditor = false" class="px-4 py-2 border rounded hover:bg-gray-50">取消</button>
          <button @click="saveAgent" class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { agentApi, type AgentTemplate } from '@/api/agent'

const agents = ref<AgentTemplate[]>([])
const showEditor = ref(false)
const editingAgent = ref<AgentTemplate>({ name: '', description: '', template: '' })

const loadAgents = async () => {
  agents.value = await agentApi.getAgentList() as any
}

const editAgent = (agent: AgentTemplate) => {
  editingAgent.value = { ...agent }
  showEditor.value = true
}

const saveAgent = async () => {
  if (editingAgent.value.id) {
    await agentApi.updateAgent(editingAgent.value.id, editingAgent.value)
    showEditor.value = false
    loadAgents()
  }
}


onMounted(loadAgents)
</script>
