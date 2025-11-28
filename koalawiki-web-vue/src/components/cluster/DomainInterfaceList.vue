<template>
  <div class="domain-interface-list">
    <div v-if="loading" class="flex justify-center py-4">
      <el-icon class="is-loading text-xl text-blue-500">
        <Loading />
      </el-icon>
    </div>
    <div v-else-if="interfaces.length === 0" class="text-center py-4 text-gray-400">
      暂无接口数据
    </div>
    <el-table v-else :data="interfaces" style="width: 100%">
      <el-table-column prop="simpleName" label="接口名">
        <template #default="{ row }">
          <div
            class="cursor-pointer text-blue-600 hover:underline"
            @click="$emit('interface-click', row.id)"
          >
            {{ row.simpleName }}
          </div>
          <div class="text-xs text-gray-400 truncate">{{ row.interfaceName }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="providerServiceName" label="提供者" width="150" />
      <el-table-column prop="methodCount" label="方法数" width="80" align="center" />
      <el-table-column prop="consumerCount" label="消费者" width="80" align="center" />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.deprecated" type="warning" size="small">已废弃</el-tag>
          <el-tag v-else type="success" size="small">正常</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
/**
 * 领域接口列表组件
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import { ref, onMounted, watch } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { dubboApi, type DubboInterfaceResponse } from '@/api/dubbo'
import type { ServiceInfoResponse } from '@/api/domain'

interface Props {
  clusterId: string
  services: ServiceInfoResponse[]
}

const props = defineProps<Props>()

defineEmits<{
  'interface-click': [interfaceId: string]
}>()

const interfaces = ref<DubboInterfaceResponse[]>([])
const loading = ref(false)

async function loadInterfaces() {
  if (!props.clusterId || props.services.length === 0) {
    interfaces.value = []
    return
  }

  loading.value = true
  try {
    const allInterfaces: DubboInterfaceResponse[] = []

    for (const service of props.services) {
      const response = await dubboApi.getByService(props.clusterId, service.warehouseId) as unknown as DubboInterfaceResponse[]
      allInterfaces.push(...response)
    }

    interfaces.value = allInterfaces
  } catch (e) {
    console.error('Failed to load interfaces:', e)
    interfaces.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadInterfaces()
})

watch(() => props.services, () => {
  loadInterfaces()
}, { deep: true })
</script>
