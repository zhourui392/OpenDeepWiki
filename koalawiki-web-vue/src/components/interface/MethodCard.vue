<template>
  <div class="method-card border rounded-lg p-4">
    <div class="flex items-start justify-between">
      <div class="flex-1">
        <!-- 方法签名 -->
        <div class="flex items-center gap-2 mb-2">
          <span class="font-mono text-sm font-medium text-blue-600">{{ method.name }}</span>
          <el-tag v-if="method.deprecated" type="warning" size="small">已废弃</el-tag>
        </div>

        <!-- 完整签名 -->
        <code class="block text-xs bg-gray-100 p-2 rounded font-mono text-gray-700">
          {{ method.signature }}
        </code>

        <!-- 描述 -->
        <p v-if="method.description" class="text-sm text-gray-600 mt-2">
          {{ method.description }}
        </p>

        <!-- 参数列表 -->
        <div v-if="method.parameters?.length" class="mt-3">
          <h4 class="text-xs font-medium text-gray-500 mb-2">参数</h4>
          <div class="space-y-1">
            <div
              v-for="param in method.parameters"
              :key="param.index"
              class="flex items-center gap-2 text-xs"
            >
              <span class="font-mono text-blue-500">{{ param.name }}</span>
              <span class="text-gray-400">:</span>
              <span class="font-mono text-purple-600">{{ param.type }}</span>
              <span v-if="param.required" class="text-red-500">*</span>
              <span v-if="param.description" class="text-gray-500">- {{ param.description }}</span>
            </div>
          </div>
        </div>

        <!-- 返回值 -->
        <div class="mt-3">
          <h4 class="text-xs font-medium text-gray-500 mb-1">返回值</h4>
          <span class="text-xs font-mono text-purple-600">{{ method.returnType }}</span>
        </div>

        <!-- 异常 -->
        <div v-if="method.exceptions?.length" class="mt-3">
          <h4 class="text-xs font-medium text-gray-500 mb-1">异常</h4>
          <div class="flex flex-wrap gap-2">
            <el-tag
              v-for="exception in method.exceptions"
              :key="exception"
              type="danger"
              size="small"
            >
              {{ exception }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 方法卡片组件
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import type { DubboMethodResponse } from '@/api/dubbo'

interface Props {
  method: DubboMethodResponse
}

defineProps<Props>()
</script>
