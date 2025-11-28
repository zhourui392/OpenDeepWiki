<template>
  <div class="domain-detail p-6">
    <!-- 头部区域 -->
    <div class="flex items-center gap-4 mb-6">
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <div
        class="w-4 h-4 rounded-full"
        :style="{ backgroundColor: domain?.color || '#1890ff' }"
      ></div>
      <h1 class="text-2xl font-bold">{{ domain?.name || '领域详情' }}</h1>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex justify-center items-center h-64">
      <el-icon class="is-loading text-4xl text-blue-500">
        <Loading />
      </el-icon>
    </div>

    <!-- 领域内容 -->
    <div v-else-if="domain" class="space-y-6">
      <!-- 领域信息 -->
      <div class="bg-white rounded-lg shadow p-4">
        <h2 class="text-lg font-semibold mb-3">领域信息</h2>
        <div class="grid grid-cols-2 gap-4 text-sm">
          <div>
            <span class="text-gray-500">编码：</span>
            <span class="font-mono">{{ domain.code }}</span>
          </div>
          <div>
            <span class="text-gray-500">负责人：</span>
            <span>{{ domain.owner || '-' }}</span>
          </div>
          <div>
            <span class="text-gray-500">服务数：</span>
            <span>{{ domain.serviceCount }}</span>
          </div>
          <div class="col-span-2">
            <span class="text-gray-500">描述：</span>
            <span>{{ domain.description || '-' }}</span>
          </div>
        </div>
      </div>

      <!-- 领域拓扑图 -->
      <div class="bg-white rounded-lg shadow p-4">
        <h2 class="text-lg font-semibold mb-4">领域拓扑</h2>
        <DomainTopology
          :cluster-id="clusterId"
          :domain-id="domain.id"
          :services="services"
          @service-click="goToService"
        />
      </div>

      <!-- 服务列表 -->
      <div class="bg-white rounded-lg shadow p-4">
        <div class="flex justify-between items-center mb-4">
          <h2 class="text-lg font-semibold">服务列表</h2>
          <el-button type="primary" size="small" @click="showAddServiceDialog = true">
            添加服务
          </el-button>
        </div>
        <el-table :data="services" style="width: 100%">
          <el-table-column prop="serviceName" label="服务名称">
            <template #default="{ row }">
              <router-link
                :to="`/repository/${row.warehouseId}/ai-documents`"
                class="text-blue-600 hover:underline"
              >
                {{ row.serviceName }}
              </router-link>
            </template>
          </el-table-column>
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="getTypeTagType(row.type)" size="small">
                {{ row.type }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" />
          <el-table-column prop="isPrimary" label="主服务" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.isPrimary" type="success" size="small">是</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button
                text
                type="danger"
                size="small"
                @click="removeService(row.warehouseId)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 领域接口列表 -->
      <div class="bg-white rounded-lg shadow p-4">
        <h2 class="text-lg font-semibold mb-4">领域接口</h2>
        <DomainInterfaceList
          :cluster-id="clusterId"
          :services="services"
          @interface-click="goToInterface"
        />
      </div>
    </div>

    <!-- 领域不存在 -->
    <div v-else class="flex flex-col items-center justify-center h-64 text-gray-400">
      <p class="text-lg">领域不存在</p>
    </div>

    <!-- 添加服务对话框 -->
    <el-dialog v-model="showAddServiceDialog" title="添加服务" width="400px">
      <el-form :model="addServiceForm" label-width="80px">
        <el-form-item label="仓库">
          <el-select v-model="addServiceForm.warehouseId" placeholder="选择仓库" class="w-full">
            <el-option
              v-for="warehouse in availableWarehouses"
              :key="warehouse.id"
              :label="warehouse.name"
              :value="warehouse.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="服务名">
          <el-input v-model="addServiceForm.serviceName" placeholder="服务名称" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="addServiceForm.serviceType" class="w-full">
            <el-option label="提供者" value="PROVIDER" />
            <el-option label="消费者" value="CONSUMER" />
            <el-option label="网关" value="GATEWAY" />
            <el-option label="调度" value="SCHEDULER" />
            <el-option label="中间件" value="MIDDLEWARE" />
          </el-select>
        </el-form-item>
        <el-form-item label="主服务">
          <el-switch v-model="addServiceForm.isPrimary" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddServiceDialog = false">取消</el-button>
        <el-button type="primary" @click="addService">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 领域详情页面
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Loading } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { domainApi, type DomainResponse, type ServiceInfoResponse, type ServiceType } from '@/api/domain'
import { warehouseApi, type WarehouseResponse } from '@/api/warehouse'
import DomainTopology from '@/components/cluster/DomainTopology.vue'
import DomainInterfaceList from '@/components/cluster/DomainInterfaceList.vue'

const route = useRoute()
const router = useRouter()

const clusterId = computed(() => route.params.clusterId as string)
const domainCode = computed(() => route.params.code as string)

const domain = ref<DomainResponse | null>(null)
const services = ref<ServiceInfoResponse[]>([])
const loading = ref(false)
const showAddServiceDialog = ref(false)
const availableWarehouses = ref<WarehouseResponse[]>([])

const addServiceForm = ref({
  warehouseId: '',
  serviceName: '',
  serviceType: 'PROVIDER' as ServiceType,
  isPrimary: false
})

async function loadDomain() {
  if (!clusterId.value || !domainCode.value) {
    return
  }

  loading.value = true
  try {
    const response = await domainApi.getByCode(clusterId.value, domainCode.value) as unknown as DomainResponse
    domain.value = response
    if (response.services) {
      services.value = response.services
    } else {
      await loadServices()
    }
  } catch (e) {
    console.error('Failed to load domain:', e)
    domain.value = null
  } finally {
    loading.value = false
  }
}

async function loadServices() {
  if (!domain.value) {
    return
  }

  try {
    const response = await domainApi.getServices(clusterId.value, domain.value.id) as unknown as ServiceInfoResponse[]
    services.value = response
  } catch (e) {
    console.error('Failed to load services:', e)
  }
}

async function loadWarehouses() {
  try {
    const response = await warehouseApi.getWarehouseList(1, 100) as unknown as { items: WarehouseResponse[] }
    availableWarehouses.value = response.items || []
  } catch (e) {
    console.error('Failed to load warehouses:', e)
  }
}

async function addService() {
  if (!domain.value || !addServiceForm.value.warehouseId) {
    ElMessage.warning('请选择仓库')
    return
  }

  try {
    await domainApi.addService(clusterId.value, domain.value.id, {
      warehouseId: addServiceForm.value.warehouseId,
      serviceName: addServiceForm.value.serviceName,
      serviceType: addServiceForm.value.serviceType,
      isPrimary: addServiceForm.value.isPrimary
    })
    ElMessage.success('服务添加成功')
    showAddServiceDialog.value = false
    await loadServices()

    // 重置表单
    addServiceForm.value = {
      warehouseId: '',
      serviceName: '',
      serviceType: 'PROVIDER',
      isPrimary: false
    }
  } catch (e) {
    console.error('Failed to add service:', e)
    ElMessage.error('添加服务失败')
  }
}

async function removeService(warehouseId: string) {
  if (!domain.value) {
    return
  }

  try {
    await ElMessageBox.confirm('确定要从领域中移除该服务吗？', '提示', {
      type: 'warning'
    })

    await domainApi.removeService(clusterId.value, domain.value.id, warehouseId)
    ElMessage.success('服务已移除')
    await loadServices()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('Failed to remove service:', e)
      ElMessage.error('移除服务失败')
    }
  }
}

function goBack() {
  router.push(`/clusters/${clusterId.value}`)
}

function goToService(warehouseId: string) {
  router.push(`/repository/${warehouseId}/ai-documents`)
}

function goToInterface(interfaceId: string) {
  router.push(`/clusters/${clusterId.value}/interfaces/${interfaceId}`)
}

function getTypeTagType(type: ServiceType): '' | 'success' | 'warning' | 'info' | 'danger' {
  const types: Record<ServiceType, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    PROVIDER: 'success',
    CONSUMER: 'info',
    GATEWAY: 'warning',
    SCHEDULER: '',
    MIDDLEWARE: 'danger'
  }
  return types[type] || ''
}

onMounted(() => {
  loadDomain()
  loadWarehouses()
})

watch([clusterId, domainCode], () => {
  loadDomain()
})
</script>
