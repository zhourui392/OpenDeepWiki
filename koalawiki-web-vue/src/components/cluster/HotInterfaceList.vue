<template>
  <div class="hot-interface-list">
    <div v-if="loading" class="flex justify-center py-4">
      <el-icon class="is-loading text-xl text-blue-500">
        <Loading />
      </el-icon>
    </div>
    <div v-else-if="interfaces.length === 0" class="text-center py-4 text-gray-400">
      暂无接口数据
    </div>
    <div v-else class="space-y-2">
      <div
        v-for="item in interfaces"
        :key="item.id"
        class="interface-item flex items-center justify-between p-3 rounded-lg hover:bg-gray-50 cursor-pointer"
        @click="$emit('interface-click', item.id)"
      >
        <div class="flex-1">
          <div class="flex items-center gap-2">
            <span class="font-mono text-sm text-blue-600">{{ item.simpleName }}</span>
            <el-tag v-if="item.deprecated" type="warning" size="small">已废弃</el-tag>
          </div>
          <p class="text-xs text-gray-400 mt-1 truncate">
            {{ item.interfaceName }}
          </p>
        </div>
        <div class="flex items-center gap-4 text-xs text-gray-500">
          <span title="方法数">{{ item.methodCount }} 方法</span>
          <span title="消费者数">{{ item.consumerCount }} 消费者</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 热门接口列表组件
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import { ref, onMounted, watch } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { dubboApi, type DubboInterfaceResponse } from '@/api/dubbo'

interface Props {
  clusterId: string
  limit?: number
}

const props = withDefaults(defineProps<Props>(), {
  limit: 10
})

defineEmits<{
  'interface-click': [interfaceId: string]
}>()

const interfaces = ref<DubboInterfaceResponse[]>([])
const loading = ref(false)

async function loadInterfaces() {
  if (!props.clusterId) {
    return
  }

  loading.value = true
  try {
    const response = await dubboApi.getTopInterfaces(props.clusterId, props.limit) as unknown as DubboInterfaceResponse[]
    interfaces.value = response
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

watch(() => props.clusterId, () => {
  loadInterfaces()
})
</script>

<style scoped>
.interface-item {
  border: 1px solid #f0f0f0;
}

.interface-item:hover {
  border-color: #e0e0e0;
}
</style>
