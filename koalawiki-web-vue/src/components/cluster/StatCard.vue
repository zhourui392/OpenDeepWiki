<template>
  <div
    class="stat-card rounded-lg p-4 shadow-sm"
    :class="colorClasses"
  >
    <div class="flex items-center justify-between">
      <div>
        <p class="text-sm text-gray-500">{{ title }}</p>
        <p class="text-2xl font-bold mt-1">{{ formattedValue }}</p>
      </div>
      <div class="text-3xl">{{ icon }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 统计卡片组件
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import { computed } from 'vue'

interface Props {
  title: string
  value: number
  icon: string
  color?: 'blue' | 'green' | 'purple' | 'orange' | 'red'
}

const props = withDefaults(defineProps<Props>(), {
  color: 'blue'
})

const colorClasses = computed(() => {
  const colors: Record<string, string> = {
    blue: 'bg-blue-50 border-l-4 border-blue-500',
    green: 'bg-green-50 border-l-4 border-green-500',
    purple: 'bg-purple-50 border-l-4 border-purple-500',
    orange: 'bg-orange-50 border-l-4 border-orange-500',
    red: 'bg-red-50 border-l-4 border-red-500'
  }
  return colors[props.color] || colors.blue
})

const formattedValue = computed(() => {
  if (props.value >= 1000) {
    return `${(props.value / 1000).toFixed(1)}k`
  }
  return props.value.toString()
})
</script>
