<template>
  <div class="service-topology">
    <div v-if="loading" class="flex justify-center items-center h-64">
      <el-icon class="is-loading text-2xl text-blue-500">
        <Loading />
      </el-icon>
    </div>
    <div v-else-if="domains.length === 0" class="flex justify-center items-center h-64 text-gray-400">
      暂无拓扑数据
    </div>
    <div v-else ref="chartRef" class="topology-chart"></div>
  </div>
</template>

<script setup lang="ts">
/**
 * 服务拓扑可视化组件
 * 使用 ECharts 力导向图展示服务依赖关系
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import type { DomainResponse } from '@/api/domain'

interface Props {
  clusterId: string
  domains: DomainResponse[]
  viewType?: 'domain' | 'service'
}

interface NodeClickEvent {
  type: string
  id: string
  code?: string
}

const props = withDefaults(defineProps<Props>(), {
  viewType: 'domain'
})

const emit = defineEmits<{
  nodeClick: [node: NodeClickEvent]
}>()

const chartRef = ref<HTMLElement | null>(null)
const loading = ref(false)

let chartInstance: unknown = null

async function initChart() {
  if (!chartRef.value || props.domains.length === 0) {
    return
  }

  loading.value = true

  try {
    const echarts = await import('echarts')

    if (chartInstance) {
      (chartInstance as { dispose: () => void }).dispose()
    }

    chartInstance = echarts.init(chartRef.value)

    const nodes = buildNodes()
    const links = buildLinks()

    const option = {
      tooltip: {
        trigger: 'item',
        formatter: (params: { dataType: string; data: { name: string; description?: string; serviceCount?: number } }) => {
          if (params.dataType === 'node') {
            const data = params.data
            if (data.serviceCount !== undefined) {
              return `${data.name}<br/>服务数: ${data.serviceCount}`
            }
            return data.description || data.name
          }
          return ''
        }
      },
      legend: {
        data: props.domains.map(d => d.name),
        orient: 'vertical',
        right: 10,
        top: 20
      },
      series: [
        {
          type: 'graph',
          layout: 'force',
          data: nodes,
          links: links,
          roam: true,
          draggable: true,
          label: {
            show: true,
            position: 'bottom',
            fontSize: 12
          },
          force: {
            repulsion: 300,
            edgeLength: [100, 200],
            gravity: 0.1
          },
          emphasis: {
            focus: 'adjacency',
            lineStyle: {
              width: 3
            }
          },
          lineStyle: {
            color: 'source',
            curveness: 0.3,
            opacity: 0.6
          }
        }
      ]
    }

    const chart = chartInstance as { setOption: (option: unknown) => void; on: (event: string, callback: (params: { data: { id: string; type: string; code?: string } }) => void) => void }
    chart.setOption(option)

    chart.on('click', (params: { data: { id: string; type: string; code?: string } }) => {
      if (params.data) {
        emit('nodeClick', {
          type: params.data.type,
          id: params.data.id,
          code: params.data.code
        })
      }
    })

  } catch (e) {
    console.error('Failed to init chart:', e)
  } finally {
    loading.value = false
  }
}

function buildNodes() {
  const nodes: Array<{
    id: string
    name: string
    type: string
    code?: string
    description?: string
    serviceCount?: number
    symbolSize: number
    category: number
    itemStyle: { color: string }
  }> = []

  if (props.viewType === 'domain') {
    props.domains.forEach((domain, index) => {
      nodes.push({
        id: domain.id,
        name: domain.name,
        type: 'domain',
        code: domain.code,
        description: domain.description,
        serviceCount: domain.serviceCount,
        symbolSize: 40 + domain.serviceCount * 5,
        category: index,
        itemStyle: {
          color: domain.color
        }
      })
    })
  } else {
    props.domains.forEach((domain, domainIndex) => {
      if (domain.services) {
        domain.services.forEach(service => {
          nodes.push({
            id: service.warehouseId,
            name: service.serviceName,
            type: 'service',
            description: service.description,
            symbolSize: 30,
            category: domainIndex,
            itemStyle: {
              color: domain.color
            }
          })
        })
      }
    })
  }

  return nodes
}

function buildLinks() {
  const links: Array<{
    source: string
    target: string
  }> = []

  // 简化版：领域视图下不显示连接线
  // 实际项目中应该从后端获取依赖关系数据
  if (props.viewType === 'service') {
    // 可以根据实际的依赖数据构建连接
  }

  return links
}

function handleResize() {
  if (chartInstance) {
    (chartInstance as { resize: () => void }).resize()
  }
}

onMounted(() => {
  nextTick(() => {
    initChart()
  })
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    (chartInstance as { dispose: () => void }).dispose()
  }
})

watch(() => [props.domains, props.viewType], () => {
  nextTick(() => {
    initChart()
  })
}, { deep: true })
</script>

<style scoped>
.topology-chart {
  width: 100%;
  height: 400px;
}
</style>
