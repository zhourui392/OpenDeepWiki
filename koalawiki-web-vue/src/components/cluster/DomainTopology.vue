<template>
  <div class="domain-topology">
    <div v-if="services.length === 0" class="flex justify-center items-center h-48 text-gray-400">
      暂无服务数据
    </div>
    <div v-else ref="chartRef" class="topology-chart"></div>
  </div>
</template>

<script setup lang="ts">
/**
 * 领域拓扑组件
 * 展示领域内服务的依赖关系
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import type { ServiceInfoResponse } from '@/api/domain'

interface Props {
  clusterId: string
  domainId: string
  services: ServiceInfoResponse[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'service-click': [warehouseId: string]
}>()

const chartRef = ref<HTMLElement | null>(null)

let chartInstance: unknown = null

async function initChart() {
  if (!chartRef.value || props.services.length === 0) {
    return
  }

  try {
    const echarts = await import('echarts')

    if (chartInstance) {
      (chartInstance as { dispose: () => void }).dispose()
    }

    chartInstance = echarts.init(chartRef.value)

    const nodes = props.services.map((service, index) => ({
      id: service.warehouseId,
      name: service.serviceName,
      type: service.type,
      x: 100 + (index % 4) * 150,
      y: 100 + Math.floor(index / 4) * 100,
      symbolSize: service.isPrimary ? 50 : 35,
      itemStyle: {
        color: getServiceColor(service.type)
      }
    }))

    const option = {
      tooltip: {
        trigger: 'item',
        formatter: (params: { data: { name: string; type: string } }) => {
          return `${params.data.name}<br/>类型: ${params.data.type}`
        }
      },
      series: [
        {
          type: 'graph',
          layout: 'none',
          data: nodes,
          links: [],
          roam: true,
          draggable: true,
          label: {
            show: true,
            position: 'bottom',
            fontSize: 11
          },
          emphasis: {
            focus: 'adjacency'
          }
        }
      ]
    }

    const chart = chartInstance as { setOption: (option: unknown) => void; on: (event: string, callback: (params: { data: { id: string } }) => void) => void }
    chart.setOption(option)

    chart.on('click', (params: { data: { id: string } }) => {
      if (params.data?.id) {
        emit('service-click', params.data.id)
      }
    })

  } catch (e) {
    console.error('Failed to init chart:', e)
  }
}

function getServiceColor(type: string): string {
  const colors: Record<string, string> = {
    PROVIDER: '#52c41a',
    CONSUMER: '#1890ff',
    GATEWAY: '#faad14',
    SCHEDULER: '#722ed1',
    MIDDLEWARE: '#eb2f96'
  }
  return colors[type] || '#666'
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

watch(() => props.services, () => {
  nextTick(() => {
    initChart()
  })
}, { deep: true })
</script>

<style scoped>
.topology-chart {
  width: 100%;
  height: 300px;
}
</style>
