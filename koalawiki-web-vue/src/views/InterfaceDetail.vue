<template>
  <div class="interface-detail p-6">
    <!-- 头部区域 -->
    <div class="flex items-center gap-4 mb-6">
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <h1 class="text-2xl font-bold">{{ interfaceData?.simpleName || '接口详情' }}</h1>
      <el-tag v-if="interfaceData?.deprecated" type="warning">已废弃</el-tag>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex justify-center items-center h-64">
      <el-icon class="is-loading text-4xl text-blue-500">
        <Loading />
      </el-icon>
    </div>

    <!-- 接口内容 -->
    <div v-else-if="interfaceData" class="space-y-6">
      <!-- 基本信息 -->
      <div class="bg-white rounded-lg shadow p-4">
        <h2 class="text-lg font-semibold mb-3">基本信息</h2>
        <div class="grid grid-cols-2 gap-4 text-sm">
          <div class="col-span-2">
            <span class="text-gray-500">接口全名：</span>
            <span class="font-mono text-blue-600">{{ interfaceData.interfaceName }}</span>
          </div>
          <div>
            <span class="text-gray-500">版本：</span>
            <span>{{ interfaceData.version || '默认' }}</span>
          </div>
          <div>
            <span class="text-gray-500">分组：</span>
            <span>{{ interfaceData.groupName || '默认' }}</span>
          </div>
          <div>
            <span class="text-gray-500">提供者：</span>
            <router-link
              v-if="interfaceData.providerWarehouseId"
              :to="`/repository/${interfaceData.providerWarehouseId}/ai-documents`"
              class="text-blue-600 hover:underline"
            >
              {{ interfaceData.providerServiceName }}
            </router-link>
            <span v-else>{{ interfaceData.providerServiceName || '-' }}</span>
          </div>
          <div>
            <span class="text-gray-500">方法数：</span>
            <span>{{ interfaceData.methodCount }}</span>
          </div>
          <div>
            <span class="text-gray-500">消费者数：</span>
            <span>{{ interfaceData.consumerCount }}</span>
          </div>
          <div class="col-span-2">
            <span class="text-gray-500">描述：</span>
            <span>{{ interfaceData.description || '-' }}</span>
          </div>
          <div v-if="interfaceData.deprecated" class="col-span-2">
            <span class="text-gray-500">废弃原因：</span>
            <span class="text-orange-600">{{ interfaceData.deprecatedReason || '-' }}</span>
          </div>
        </div>
      </div>

      <!-- 方法列表 -->
      <div class="bg-white rounded-lg shadow p-4">
        <h2 class="text-lg font-semibold mb-4">方法列表</h2>
        <div v-if="interfaceData.methods?.length" class="space-y-4">
          <MethodCard
            v-for="method in interfaceData.methods"
            :key="method.name"
            :method="method"
          />
        </div>
        <div v-else class="text-center py-4 text-gray-400">
          暂无方法信息
        </div>
      </div>

      <!-- 消费者列表 -->
      <div class="bg-white rounded-lg shadow p-4">
        <h2 class="text-lg font-semibold mb-4">消费者列表</h2>
        <div v-if="loadingConsumers" class="flex justify-center py-4">
          <el-icon class="is-loading text-xl text-blue-500">
            <Loading />
          </el-icon>
        </div>
        <el-table v-else-if="consumers.length" :data="consumers" style="width: 100%">
          <el-table-column prop="consumerServiceName" label="消费者服务">
            <template #default="{ row }">
              <router-link
                :to="`/repository/${row.consumerWarehouseId}/ai-documents`"
                class="text-blue-600 hover:underline"
              >
                {{ row.consumerServiceName }}
              </router-link>
            </template>
          </el-table-column>
          <el-table-column prop="sourceClass" label="使用类" />
          <el-table-column prop="sourceField" label="注入字段" width="150" />
        </el-table>
        <div v-else class="text-center py-4 text-gray-400">
          暂无消费者
        </div>
      </div>

      <!-- 调用链 -->
      <div class="bg-white rounded-lg shadow p-4">
        <div class="flex justify-between items-center mb-4">
          <h2 class="text-lg font-semibold">调用链</h2>
          <el-select v-model="callChainDepth" size="small" class="w-24" @change="loadCallChain">
            <el-option :value="2" label="2层" />
            <el-option :value="3" label="3层" />
            <el-option :value="5" label="5层" />
          </el-select>
        </div>
        <div v-if="loadingCallChain" class="flex justify-center py-4">
          <el-icon class="is-loading text-xl text-blue-500">
            <Loading />
          </el-icon>
        </div>
        <div v-else-if="callChain">
          <div class="flex gap-4 mb-4 text-sm">
            <span>上游服务: {{ callChain.upstreamCount }}</span>
            <span>下游服务: {{ callChain.downstreamCount }}</span>
          </div>
          <div v-if="callChain.mermaidCode" class="mermaid-container border rounded p-4 bg-gray-50">
            <MermaidRenderer :code="callChain.mermaidCode" />
          </div>
          <div v-else class="text-center py-4 text-gray-400">
            暂无调用链数据
          </div>
        </div>
      </div>
    </div>

    <!-- 接口不存在 -->
    <div v-else class="flex flex-col items-center justify-center h-64 text-gray-400">
      <p class="text-lg">接口不存在</p>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 接口详情页面
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Loading } from '@element-plus/icons-vue'
import { dubboApi, type DubboInterfaceResponse, type DubboConsumerResponse, type CallChainResponse } from '@/api/dubbo'
import MethodCard from '@/components/interface/MethodCard.vue'
import MermaidRenderer from '@/components/MermaidRenderer.vue'

const route = useRoute()
const router = useRouter()

const clusterId = computed(() => route.params.clusterId as string)
const interfaceId = computed(() => route.params.interfaceId as string)

const interfaceData = ref<DubboInterfaceResponse | null>(null)
const consumers = ref<DubboConsumerResponse[]>([])
const callChain = ref<CallChainResponse | null>(null)
const loading = ref(false)
const loadingConsumers = ref(false)
const loadingCallChain = ref(false)
const callChainDepth = ref(3)

async function loadInterface() {
  if (!clusterId.value || !interfaceId.value) {
    return
  }

  loading.value = true
  try {
    const response = await dubboApi.getById(clusterId.value, interfaceId.value) as unknown as DubboInterfaceResponse
    interfaceData.value = response
  } catch (e) {
    console.error('Failed to load interface:', e)
    interfaceData.value = null
  } finally {
    loading.value = false
  }
}

async function loadConsumers() {
  if (!clusterId.value || !interfaceId.value) {
    return
  }

  loadingConsumers.value = true
  try {
    const response = await dubboApi.getConsumers(clusterId.value, interfaceId.value) as unknown as DubboConsumerResponse[]
    consumers.value = response
  } catch (e) {
    console.error('Failed to load consumers:', e)
    consumers.value = []
  } finally {
    loadingConsumers.value = false
  }
}

async function loadCallChain() {
  if (!clusterId.value || !interfaceId.value) {
    return
  }

  loadingCallChain.value = true
  try {
    const response = await dubboApi.getCallChain(clusterId.value, interfaceId.value, callChainDepth.value) as unknown as CallChainResponse
    callChain.value = response
  } catch (e) {
    console.error('Failed to load call chain:', e)
    callChain.value = null
  } finally {
    loadingCallChain.value = false
  }
}

function goBack() {
  router.back()
}

onMounted(() => {
  loadInterface()
  loadConsumers()
  loadCallChain()
})

watch([clusterId, interfaceId], () => {
  loadInterface()
  loadConsumers()
  loadCallChain()
})
</script>
