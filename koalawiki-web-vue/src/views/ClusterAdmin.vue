<template>
  <div class="cluster-admin p-6">
    <div class="flex justify-between items-center mb-6">
      <h1 class="text-2xl font-bold">集群管理</h1>
      <el-button type="primary" @click="showCreateDialog = true">
        创建集群
      </el-button>
    </div>

    <!-- 集群列表 -->
    <div class="bg-white rounded-lg shadow">
      <el-table :data="clusters" style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="集群名称" min-width="150">
          <template #default="{ row }">
            <router-link
              :to="`/clusters/${row.id}`"
              class="text-blue-600 hover:underline font-medium"
            >
              {{ row.name }}
            </router-link>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="编码" width="120">
          <template #default="{ row }">
            <code class="text-sm">{{ row.code }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="techStack" label="技术栈" min-width="200" />
        <el-table-column prop="owner" label="负责人" width="100" />
        <el-table-column prop="warehouseCount" label="服务数" width="80" align="center" />
        <el-table-column prop="domainCount" label="领域数" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="editCluster(row)">
              编辑
            </el-button>
            <el-button text type="primary" size="small" @click="manageDomains(row)">
              领域
            </el-button>
            <el-button text type="danger" size="small" @click="deleteCluster(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 创建/编辑集群对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="editingCluster ? '编辑集群' : '创建集群'"
      width="500px"
    >
      <el-form :model="clusterForm" label-width="80px" :rules="formRules" ref="formRef">
        <el-form-item label="名称" prop="name">
          <el-input v-model="clusterForm.name" placeholder="集群名称" />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input
            v-model="clusterForm.code"
            placeholder="集群编码（唯一标识）"
            :disabled="!!editingCluster"
          />
        </el-form-item>
        <el-form-item label="技术栈" prop="techStack">
          <el-input v-model="clusterForm.techStack" placeholder="如：Spring Boot 2.7 + Dubbo 3.0" />
        </el-form-item>
        <el-form-item label="负责人" prop="owner">
          <el-input v-model="clusterForm.owner" placeholder="负责人" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="clusterForm.description"
            type="textarea"
            :rows="3"
            placeholder="集群描述"
          />
        </el-form-item>
        <el-form-item v-if="editingCluster" label="状态" prop="status">
          <el-select v-model="clusterForm.status" class="w-full">
            <el-option value="ACTIVE" label="活跃" />
            <el-option value="INACTIVE" label="停用" />
            <el-option value="ARCHIVED" label="归档" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" @click="submitCluster" :loading="submitting">
          {{ editingCluster ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 领域管理对话框 -->
    <el-dialog
      v-model="showDomainDialog"
      :title="`${managingCluster?.name} - 领域管理`"
      width="700px"
    >
      <div class="flex justify-end mb-4">
        <el-button type="primary" size="small" @click="showAddDomainDialog = true">
          添加领域
        </el-button>
      </div>
      <el-table :data="clusterDomains" style="width: 100%" v-loading="loadingDomains">
        <el-table-column label="颜色" width="60">
          <template #default="{ row }">
            <div
              class="w-4 h-4 rounded-full"
              :style="{ backgroundColor: row.color }"
            ></div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="领域名称" />
        <el-table-column prop="code" label="编码" />
        <el-table-column prop="serviceCount" label="服务数" width="80" align="center" />
        <el-table-column prop="owner" label="负责人" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="editDomain(row)">
              编辑
            </el-button>
            <el-button text type="danger" size="small" @click="deleteDomain(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 添加/编辑领域对话框 -->
    <el-dialog
      v-model="showAddDomainDialog"
      :title="editingDomain ? '编辑领域' : '添加领域'"
      width="400px"
    >
      <el-form :model="domainForm" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="domainForm.name" placeholder="领域名称" />
        </el-form-item>
        <el-form-item label="编码">
          <el-input
            v-model="domainForm.code"
            placeholder="领域编码"
            :disabled="!!editingDomain"
          />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="domainForm.color" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="domainForm.owner" placeholder="负责人" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="domainForm.description"
            type="textarea"
            :rows="2"
            placeholder="领域描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDomainDialog = false">取消</el-button>
        <el-button type="primary" @click="submitDomain" :loading="submittingDomain">
          {{ editingDomain ? '保存' : '添加' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 集群管理页面
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { clusterApi, type ClusterResponse, type ClusterStatus } from '@/api/cluster'
import { domainApi, type DomainResponse } from '@/api/domain'

const clusters = ref<ClusterResponse[]>([])
const loading = ref(false)
const showCreateDialog = ref(false)
const editingCluster = ref<ClusterResponse | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const clusterForm = reactive({
  name: '',
  code: '',
  techStack: '',
  owner: '',
  description: '',
  status: 'ACTIVE' as ClusterStatus
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入集群名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入集群编码', trigger: 'blur' },
    { pattern: /^[a-z0-9-]+$/, message: '编码只能包含小写字母、数字和横线', trigger: 'blur' }
  ]
}

const showDomainDialog = ref(false)
const managingCluster = ref<ClusterResponse | null>(null)
const clusterDomains = ref<DomainResponse[]>([])
const loadingDomains = ref(false)

const showAddDomainDialog = ref(false)
const editingDomain = ref<DomainResponse | null>(null)
const submittingDomain = ref(false)

const domainForm = reactive({
  name: '',
  code: '',
  color: '#1890ff',
  owner: '',
  description: ''
})

async function loadClusters() {
  loading.value = true
  try {
    const response = await clusterApi.list(1, 100) as unknown as { items: ClusterResponse[] }
    clusters.value = response.items || []
  } catch (e) {
    console.error('Failed to load clusters:', e)
    ElMessage.error('加载集群列表失败')
  } finally {
    loading.value = false
  }
}

function editCluster(cluster: ClusterResponse) {
  editingCluster.value = cluster
  clusterForm.name = cluster.name
  clusterForm.code = cluster.code
  clusterForm.techStack = cluster.techStack || ''
  clusterForm.owner = cluster.owner || ''
  clusterForm.description = cluster.description || ''
  clusterForm.status = cluster.status
  showCreateDialog.value = true
}

function closeDialog() {
  showCreateDialog.value = false
  editingCluster.value = null
  resetForm()
}

function resetForm() {
  clusterForm.name = ''
  clusterForm.code = ''
  clusterForm.techStack = ''
  clusterForm.owner = ''
  clusterForm.description = ''
  clusterForm.status = 'ACTIVE'
}

async function submitCluster() {
  if (!formRef.value) {
    return
  }

  await formRef.value.validate()

  submitting.value = true
  try {
    if (editingCluster.value) {
      await clusterApi.update(editingCluster.value.id, clusterForm)
      ElMessage.success('集群更新成功')
    } else {
      await clusterApi.create(clusterForm)
      ElMessage.success('集群创建成功')
    }
    closeDialog()
    await loadClusters()
  } catch (e) {
    console.error('Failed to save cluster:', e)
    ElMessage.error(editingCluster.value ? '更新失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}

async function deleteCluster(cluster: ClusterResponse) {
  try {
    await ElMessageBox.confirm(`确定要删除集群 "${cluster.name}" 吗？`, '提示', {
      type: 'warning'
    })

    await clusterApi.delete(cluster.id)
    ElMessage.success('删除成功')
    await loadClusters()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('Failed to delete cluster:', e)
      ElMessage.error('删除失败')
    }
  }
}

async function manageDomains(cluster: ClusterResponse) {
  managingCluster.value = cluster
  showDomainDialog.value = true
  await loadDomains()
}

async function loadDomains() {
  if (!managingCluster.value) {
    return
  }

  loadingDomains.value = true
  try {
    const response = await domainApi.getAll(managingCluster.value.id) as unknown as DomainResponse[]
    clusterDomains.value = response
  } catch (e) {
    console.error('Failed to load domains:', e)
  } finally {
    loadingDomains.value = false
  }
}

function editDomain(domain: DomainResponse) {
  editingDomain.value = domain
  domainForm.name = domain.name
  domainForm.code = domain.code
  domainForm.color = domain.color
  domainForm.owner = domain.owner || ''
  domainForm.description = domain.description || ''
  showAddDomainDialog.value = true
}

function resetDomainForm() {
  domainForm.name = ''
  domainForm.code = ''
  domainForm.color = '#1890ff'
  domainForm.owner = ''
  domainForm.description = ''
}

async function submitDomain() {
  if (!managingCluster.value) {
    return
  }

  submittingDomain.value = true
  try {
    if (editingDomain.value) {
      await domainApi.update(managingCluster.value.id, editingDomain.value.id, domainForm)
      ElMessage.success('领域更新成功')
    } else {
      await domainApi.create(managingCluster.value.id, domainForm)
      ElMessage.success('领域创建成功')
    }
    showAddDomainDialog.value = false
    editingDomain.value = null
    resetDomainForm()
    await loadDomains()
  } catch (e) {
    console.error('Failed to save domain:', e)
    ElMessage.error(editingDomain.value ? '更新失败' : '创建失败')
  } finally {
    submittingDomain.value = false
  }
}

async function deleteDomain(domain: DomainResponse) {
  if (!managingCluster.value) {
    return
  }

  try {
    await ElMessageBox.confirm(`确定要删除领域 "${domain.name}" 吗？`, '提示', {
      type: 'warning'
    })

    await domainApi.delete(managingCluster.value.id, domain.id)
    ElMessage.success('删除成功')
    await loadDomains()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('Failed to delete domain:', e)
      ElMessage.error('删除失败')
    }
  }
}

function getStatusType(status: ClusterStatus): '' | 'success' | 'warning' | 'info' | 'danger' {
  const types: Record<ClusterStatus, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    ACTIVE: 'success',
    INACTIVE: 'warning',
    ARCHIVED: 'info'
  }
  return types[status] || ''
}

function getStatusName(status: ClusterStatus): string {
  const names: Record<ClusterStatus, string> = {
    ACTIVE: '活跃',
    INACTIVE: '停用',
    ARCHIVED: '归档'
  }
  return names[status] || status
}

onMounted(() => {
  loadClusters()
})
</script>
