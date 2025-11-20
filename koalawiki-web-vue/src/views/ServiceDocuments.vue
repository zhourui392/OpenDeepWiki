<template>
  <div class="service-documents">
    <div class="header">
      <h2>服务文档库</h2>
      <el-button type="primary" @click="showCreateDialog = true">
        新建服务
      </el-button>
    </div>

    <el-table :data="services" v-loading="loading">
      <el-table-column prop="serviceName" label="服务名称" />
      <el-table-column prop="serviceId" label="服务ID" />
      <el-table-column prop="docType" label="文档类型" />
      <el-table-column prop="agentType" label="Agent类型" />
      <el-table-column prop="enabled" label="状态">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">
            {{ row.enabled ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="showCreateDialog"
      title="新建服务"
      width="600px"
    >
      <el-form :model="form" label-width="120px">
        <el-form-item label="服务ID" required>
          <el-input v-model="form.serviceId" placeholder="英文标识" />
        </el-form-item>
        <el-form-item label="服务名称" required>
          <el-input v-model="form.serviceName" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
        <el-form-item label="文档类型" required>
          <el-select v-model="form.docType">
            <el-option label="架构文档" value="ARCHITECTURE" />
            <el-option label="API文档" value="API_GUIDE" />
            <el-option label="模块文档" value="MODULE_GUIDE" />
            <el-option label="自定义" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="Agent类型">
          <el-select v-model="form.agentType">
            <el-option label="Claude" value="claude" />
            <el-option label="Codex" value="codex" />
          </el-select>
        </el-form-item>
        <el-form-item label="源码匹配规则">
          <el-input
            v-model="globsInput"
            type="textarea"
            placeholder="每行一个规则，如: src/main/java/**/*.java"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showEditDialog"
      title="编辑服务"
      width="600px"
    >
      <el-form :model="editForm" label-width="120px">
        <el-form-item label="服务名称" required>
          <el-input v-model="editForm.serviceName" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" />
        </el-form-item>
        <el-form-item label="文档类型" required>
          <el-select v-model="editForm.docType">
            <el-option label="架构文档" value="ARCHITECTURE" />
            <el-option label="API文档" value="API_GUIDE" />
            <el-option label="模块文档" value="MODULE_GUIDE" />
            <el-option label="自定义" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="Agent类型">
          <el-select v-model="editForm.agentType">
            <el-option label="Claude" value="claude" />
            <el-option label="Codex" value="codex" />
          </el-select>
        </el-form-item>
        <el-form-item label="源码匹配规则">
          <el-input
            v-model="editGlobsInput"
            type="textarea"
            placeholder="每行一个规则"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="editForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { serviceDocumentApi, type ServiceDocumentLibrary, type CreateServiceRequest, type UpdateServiceRequest } from '@/api/service-document'

const route = useRoute()
const warehouseId = computed(() => route.params.id as string)

const loading = ref(false)
const services = ref<ServiceDocumentLibrary[]>([])
const showCreateDialog = ref(false)
const showEditDialog = ref(false)

const form = ref<CreateServiceRequest>({
  serviceId: '',
  serviceName: '',
  description: '',
  docType: 'ARCHITECTURE',
  agentType: 'claude',
  sourceGlobs: [],
  enabled: true
})

const editForm = ref<UpdateServiceRequest>({
  serviceName: '',
  description: '',
  docType: 'ARCHITECTURE',
  agentType: 'claude',
  sourceGlobs: [],
  enabled: true
})

const currentServiceId = ref('')
const globsInput = ref('')
const editGlobsInput = ref('')

const loadServices = async () => {
  loading.value = true
  try {
    const response = await serviceDocumentApi.listServices(warehouseId.value)
    services.value = response.data || []
  } catch (error) {
    ElMessage.error('加载服务列表失败')
  } finally {
    loading.value = false
  }
}

const handleCreate = async () => {
  if (!form.value.serviceId || !form.value.serviceName) {
    ElMessage.warning('请填写必填项')
    return
  }

  form.value.sourceGlobs = globsInput.value
    .split('\n')
    .map(s => s.trim())
    .filter(s => s.length > 0)

  try {
    await serviceDocumentApi.createService(warehouseId.value, form.value)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    loadServices()
    resetForm()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '创建失败')
  }
}

const handleEdit = (row: ServiceDocumentLibrary) => {
  currentServiceId.value = row.serviceId
  editForm.value = {
    serviceName: row.serviceName,
    description: row.description || '',
    docType: row.docType,
    agentType: row.agentType || 'claude',
    sourceGlobs: row.sourceGlobs || [],
    enabled: row.enabled
  }
  editGlobsInput.value = (row.sourceGlobs || []).join('\n')
  showEditDialog.value = true
}

const handleUpdate = async () => {
  editForm.value.sourceGlobs = editGlobsInput.value
    .split('\n')
    .map(s => s.trim())
    .filter(s => s.length > 0)

  try {
    await serviceDocumentApi.updateService(
      warehouseId.value,
      currentServiceId.value,
      editForm.value
    )
    ElMessage.success('更新成功')
    showEditDialog.value = false
    loadServices()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '更新失败')
  }
}

const handleDelete = async (row: ServiceDocumentLibrary) => {
  try {
    await ElMessageBox.confirm('确定删除该服务配置吗？', '提示', {
      type: 'warning'
    })
    await serviceDocumentApi.deleteService(warehouseId.value, row.serviceId)
    ElMessage.success('删除成功')
    loadServices()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const resetForm = () => {
  form.value = {
    serviceId: '',
    serviceName: '',
    description: '',
    docType: 'ARCHITECTURE',
    agentType: 'claude',
    sourceGlobs: [],
    enabled: true
  }
  globsInput.value = ''
}

onMounted(() => {
  loadServices()
})
</script>

<style scoped>
.service-documents {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>
