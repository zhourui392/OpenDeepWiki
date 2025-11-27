<template>
  <div class="business-flow-container">
    <el-card class="header-card">
      <h2>业务流程追踪</h2>
      <p class="description">分析微服务调用链路，生成可视化时序图</p>
    </el-card>

    <el-card class="form-card">
      <el-form :model="form" label-width="120px">
        <el-form-item label="项目路径">
          <el-input
            v-model="form.projectPath"
            placeholder="例如: /data/koalawiki/git/order-service"
          />
        </el-form-item>

        <el-form-item label="依赖项目">
          <el-select
            v-model="form.projectPaths"
            multiple
            filterable
            allow-create
            placeholder="添加依赖的项目路径"
            style="width: 100%"
          >
            <el-option
              v-for="path in form.projectPaths"
              :key="path"
              :label="path"
              :value="path"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="入口类名">
          <el-input
            v-model="form.entryPoint.className"
            placeholder="例如: com.example.order.controller.OrderController"
          />
        </el-form-item>

        <el-form-item label="入口方法">
          <el-input
            v-model="form.entryPoint.methodName"
            placeholder="例如: createOrder"
          />
        </el-form-item>

        <el-form-item label="API路径">
          <el-input
            v-model="form.entryPoint.path"
            placeholder="例如: /api/order/create"
          />
        </el-form-item>

        <el-form-item label="追踪深度">
          <el-input-number v-model="form.maxDepth" :min="1" :max="10" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="generateFlow" :loading="loading">
            生成流程图
          </el-button>
          <el-button @click="generateAllFlows" :loading="loadingAll">
            生成所有流程
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="result" class="result-card">
      <template #header>
        <div class="card-header">
          <span>调用链路</span>
          <el-tag>节点数: {{ result.nodeCount }}</el-tag>
          <el-tag type="info">深度: {{ result.maxDepth }}</el-tag>
        </div>
      </template>

      <div class="mermaid-container">
        <div ref="mermaidRef" class="mermaid-diagram"></div>
      </div>
    </el-card>

    <el-card v-if="allResults.length > 0" class="all-results-card">
      <template #header>
        <span>所有业务流程 ({{ allResults.length }})</span>
      </template>

      <el-collapse v-model="activeFlows" @change="handleCollapseChange">
        <el-collapse-item
          v-for="(flow, index) in allResults"
          :key="flow.flowId"
          :name="index"
        >
          <template #title>
            <div class="flow-title">
              <span>{{ flow.entryPoint.path }}</span>
              <el-tag size="small">{{ flow.nodeCount }} 节点</el-tag>
            </div>
          </template>
          <div :ref="el => setMermaidRef(el, index)" class="mermaid-diagram"></div>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import mermaid from 'mermaid'
import { businessFlowApi, type BusinessFlowResult, type EntryPoint } from '@/api/business-flow'

mermaid.initialize({
  startOnLoad: false,
  theme: 'default',
  securityLevel: 'loose'
})

const form = ref({
  projectPath: '',
  projectPaths: [] as string[],
  entryPoint: {
    type: 'HTTP',
    path: '',
    className: '',
    methodName: ''
  } as EntryPoint,
  maxDepth: 5
})

const loading = ref(false)
const loadingAll = ref(false)
const result = ref<BusinessFlowResult | null>(null)
const allResults = ref<BusinessFlowResult[]>([])
const activeFlows = ref<number[]>([])
const mermaidRef = ref<HTMLElement>()
const mermaidRefs = ref<Map<number, HTMLElement>>(new Map())

const setMermaidRef = (el: any, index: number) => {
  if (el) {
    mermaidRefs.value.set(index, el)
  }
}

const renderMermaid = async (element: HTMLElement, diagram: string) => {
  try {
    element.innerHTML = diagram
    await mermaid.run({ nodes: [element] })
  } catch (error) {
    console.error('Mermaid渲染失败:', error)
    element.innerHTML = `<pre>${diagram}</pre>`
  }
}

const generateFlow = async () => {
  if (!form.value.projectPath || !form.value.entryPoint.className) {
    ElMessage.warning('请填写必填项')
    return
  }

  loading.value = true
  try {
    const projectPaths = [form.value.projectPath, ...form.value.projectPaths]
    result.value = await businessFlowApi.generateFlow({
      projectPaths,
      projectPath: form.value.projectPath,
      entryPoint: form.value.entryPoint,
      maxDepth: form.value.maxDepth
    })

    await nextTick()
    if (mermaidRef.value && result.value) {
      await renderMermaid(mermaidRef.value, result.value.mermaidDiagram)
    }

    ElMessage.success('流程图生成成功')
  } catch (error: any) {
    ElMessage.error(error.message || '生成失败')
  } finally {
    loading.value = false
  }
}

const generateAllFlows = async () => {
  if (!form.value.projectPath) {
    ElMessage.warning('请填写项目路径')
    return
  }

  loadingAll.value = true
  try {
    const projectPaths = [form.value.projectPath, ...form.value.projectPaths]
    allResults.value = await businessFlowApi.generateAllFlows({
      projectPaths,
      projectPath: form.value.projectPath,
      maxDepth: form.value.maxDepth
    })

    activeFlows.value = []
    await nextTick()

    ElMessage.success(`生成了 ${allResults.value.length} 个流程图`)
  } catch (error: any) {
    ElMessage.error(error.message || '生成失败')
  } finally {
    loadingAll.value = false
  }
}

const renderFlowDiagram = async (index: number) => {
  await nextTick()
  const element = mermaidRefs.value.get(index)
  const flow = allResults.value[index]
  if (element && flow) {
    await renderMermaid(element, flow.mermaidDiagram)
  }
}

// 监听折叠面板展开事件
const handleCollapseChange = (activeNames: number[] | string[] | unknown) => {
  const names = Array.isArray(activeNames) ? activeNames : []
  names.forEach((index: number | string) => {
    const idx = typeof index === 'number' ? index : parseInt(String(index), 10)
    if (!isNaN(idx)) {
      renderFlowDiagram(idx)
    }
  })
}
</script>

<style scoped>
.business-flow-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.header-card {
  margin-bottom: 20px;
}

.header-card h2 {
  margin: 0 0 10px 0;
  color: #303133;
}

.description {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.form-card {
  margin-bottom: 20px;
}

.result-card,
.all-results-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mermaid-container {
  overflow-x: auto;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 4px;
}

.mermaid-diagram {
  min-height: 200px;
}

.flow-title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

:deep(.el-collapse-item__content) {
  padding: 20px;
  background: #f5f7fa;
}
</style>
