<template>
  <el-drawer
    v-model="visible"
    title="生成任务"
    size="500px"
    @close="handleClose"
  >
    <div class="task-flow">
      <el-timeline>
        <el-timeline-item
          v-for="task in tasks"
          :key="task.id"
          :type="getTimelineType(task.status)"
          :icon="getTimelineIcon(task.status)"
        >
          <div class="task-item">
            <div class="task-header">
              <span class="task-service">{{ task.serviceId }}</span>
              <el-tag :type="getStatusType(task.status)" size="small">
                {{ getStatusText(task.status) }}
              </el-tag>
            </div>
            <div class="task-progress">
              <el-progress
                :percentage="getProgress(task)"
                :status="getProgressStatus(task.status)"
              />
              <span class="task-stats">
                {{ task.completedFiles }}/{{ task.totalFiles }}
                <span v-if="task.failedFiles > 0" class="failed-count">
                  (失败: {{ task.failedFiles }})
                </span>
              </span>
            </div>
            <div class="task-meta">
              <span>Agent: {{ task.agentType }}</span>
              <span>类型: {{ task.docType }}</span>
            </div>
            <div class="task-time">
              开始时间: {{ formatTime(task.startedAt || task.createdAt) }}
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>

      <div v-if="tasks.length === 0" class="empty">
        <el-empty description="暂无任务" />
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { GenerationTask } from '@/api/ai-document'
import { Clock, Check, Close, Loading } from '@element-plus/icons-vue'

interface Props {
  modelValue: boolean
  tasks: GenerationTask[]
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue', 'close'])

const visible = ref(props.modelValue)

watch(() => props.modelValue, (val) => {
  visible.value = val
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const handleClose = () => {
  emit('close')
}

const getProgress = (task: GenerationTask) => {
  if (task.totalFiles === 0) return 0
  return Math.round((task.completedFiles / task.totalFiles) * 100)
}

const getStatusType = (status: string) => {
  const map: Record<string, any> = {
    PENDING: 'info',
    RUNNING: 'warning',
    COMPLETED: 'success',
    FAILED: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    PENDING: '等待中',
    RUNNING: '进行中',
    COMPLETED: '已完成',
    FAILED: '失败'
  }
  return map[status] || status
}

const getTimelineType = (status: string) => {
  const map: Record<string, any> = {
    PENDING: 'info',
    RUNNING: 'primary',
    COMPLETED: 'success',
    FAILED: 'danger'
  }
  return map[status] || 'info'
}

const getTimelineIcon = (status: string) => {
  const map: Record<string, any> = {
    PENDING: Clock,
    RUNNING: Loading,
    COMPLETED: Check,
    FAILED: Close
  }
  return map[status] || Clock
}

const getProgressStatus = (status: string) => {
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'exception'
  return undefined
}

const formatTime = (time?: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}
</script>

<style scoped>
.task-flow {
  padding: 20px;
}

.task-item {
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.task-service {
  font-weight: bold;
  font-size: 14px;
}

.task-progress {
  margin-bottom: 10px;
}

.task-stats {
  font-size: 12px;
  color: #606266;
  margin-left: 10px;
}

.failed-count {
  color: #f56c6c;
}

.task-meta {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 5px;
}

.task-time {
  font-size: 12px;
  color: #909399;
}

.empty {
  text-align: center;
  padding: 40px 0;
}
</style>
