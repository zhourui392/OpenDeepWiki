<template>
  <div
    class="domain-card rounded-lg p-4 cursor-pointer hover:shadow-md transition-shadow"
    :style="{ borderLeft: `4px solid ${domain.color}` }"
    @click="$emit('click', domain)"
  >
    <div class="flex items-center gap-2 mb-2">
      <div
        class="w-3 h-3 rounded-full"
        :style="{ backgroundColor: domain.color }"
      ></div>
      <h3 class="font-semibold text-gray-800">{{ domain.name }}</h3>
    </div>
    <p class="text-sm text-gray-500 mb-2 line-clamp-2">
      {{ domain.description || '暂无描述' }}
    </p>
    <div class="flex items-center gap-4 text-xs text-gray-400">
      <span>{{ domain.serviceCount }} 个服务</span>
      <span v-if="domain.owner">负责人: {{ domain.owner }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 领域卡片组件
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import type { DomainResponse } from '@/api/domain'

interface Props {
  domain: DomainResponse
}

defineProps<Props>()
defineEmits<{
  click: [domain: DomainResponse]
}>()
</script>

<style scoped>
.domain-card {
  background: white;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
