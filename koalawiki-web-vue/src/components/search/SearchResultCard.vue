<template>
  <div
    class="search-result-card bg-white rounded-lg shadow-sm p-4 cursor-pointer hover:shadow-md transition-shadow"
    @click="$emit('click', result)"
  >
    <div class="flex items-start gap-3">
      <!-- 类型图标 -->
      <div class="type-icon text-2xl">
        {{ getTypeIcon(result.type) }}
      </div>

      <!-- 内容区域 -->
      <div class="flex-1 min-w-0">
        <div class="flex items-center gap-2 mb-1">
          <el-tag :type="getTypeTagType(result.type)" size="small">
            {{ getTypeName(result.type) }}
          </el-tag>
          <span v-if="result.clusterName" class="text-xs text-gray-400">
            {{ result.clusterName }}
          </span>
          <span v-if="result.domainName" class="text-xs text-gray-400">
            / {{ result.domainName }}
          </span>
        </div>

        <h3 class="text-base font-medium text-gray-800 truncate">
          {{ result.title }}
        </h3>

        <p v-if="result.description" class="text-sm text-gray-500 mt-1 line-clamp-2">
          {{ result.description }}
        </p>

        <!-- 匹配内容高亮 -->
        <div
          v-if="result.matchedContent"
          class="matched-content mt-2 text-sm bg-yellow-50 p-2 rounded"
          v-html="highlightMatch(result.matchedContent)"
        ></div>
      </div>

      <!-- 分数 -->
      <div class="score text-xs text-gray-400">
        {{ (result.score * 100).toFixed(0) }}%
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 搜索结果卡片组件
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import type { SearchResultItem, SearchResultType } from '@/api/search'

interface Props {
  result: SearchResultItem
}

defineProps<Props>()
defineEmits<{
  click: [result: SearchResultItem]
}>()

function getTypeIcon(type: SearchResultType): string {
  const icons: Record<SearchResultType, string> = {
    SERVICE: '📦',
    INTERFACE: '🔌',
    DOCUMENT: '📄',
    DOMAIN: '🏷️'
  }
  return icons[type] || '📋'
}

function getTypeName(type: SearchResultType): string {
  const names: Record<SearchResultType, string> = {
    SERVICE: '服务',
    INTERFACE: '接口',
    DOCUMENT: '文档',
    DOMAIN: '领域'
  }
  return names[type] || type
}

function getTypeTagType(type: SearchResultType): '' | 'success' | 'warning' | 'info' | 'danger' {
  const types: Record<SearchResultType, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    SERVICE: 'success',
    INTERFACE: '',
    DOCUMENT: 'info',
    DOMAIN: 'warning'
  }
  return types[type] || ''
}

function highlightMatch(content: string): string {
  // 简单的关键词高亮，实际项目中应该从后端返回已标记的内容
  return content.replace(/<em>/g, '<mark class="bg-yellow-200">').replace(/<\/em>/g, '</mark>')
}
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
