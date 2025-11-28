<template>
  <div ref="containerRef" class="mermaid-renderer"></div>
</template>

<script setup lang="ts">
/**
 * Mermaid 图表渲染组件
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import { ref, onMounted, watch, nextTick } from 'vue'

interface Props {
  code: string
}

const props = defineProps<Props>()

const containerRef = ref<HTMLElement | null>(null)

async function renderMermaid() {
  if (!containerRef.value || !props.code) {
    return
  }

  try {
    const mermaid = await import('mermaid')

    mermaid.default.initialize({
      startOnLoad: false,
      theme: 'default',
      securityLevel: 'loose',
      flowchart: {
        useMaxWidth: true,
        htmlLabels: true
      }
    })

    const id = `mermaid-${Date.now()}`
    const { svg } = await mermaid.default.render(id, props.code)
    containerRef.value.innerHTML = svg
  } catch (e) {
    console.error('Failed to render mermaid:', e)
    containerRef.value.innerHTML = `<pre class="text-red-500 text-sm">${props.code}</pre>`
  }
}

onMounted(() => {
  nextTick(() => {
    renderMermaid()
  })
})

watch(() => props.code, () => {
  nextTick(() => {
    renderMermaid()
  })
})
</script>

<style scoped>
.mermaid-renderer {
  display: flex;
  justify-content: center;
  overflow-x: auto;
}

.mermaid-renderer :deep(svg) {
  max-width: 100%;
  height: auto;
}
</style>
