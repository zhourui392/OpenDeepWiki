<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <div class="max-w-7xl mx-auto">
      <!-- Header -->
      <div class="bg-white rounded-lg shadow-sm p-6 mb-6">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-gray-900">AI生成文档</h1>
            <p class="text-gray-600 mt-1">
              自动生成代码技术文档
            </p>
          </div>
          <div class="flex items-center gap-2">
            <button
              @click="handleGenerateProject"
              :disabled="generating"
              class="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:bg-gray-400 disabled:cursor-not-allowed flex items-center gap-2"
            >
              <svg v-if="generating" class="animate-spin h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              {{ generating ? '生成中...' : '生成架构文档' }}
            </button>
            <button
              @click="handleGenerate"
              :disabled="generating"
              class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed flex items-center gap-2"
            >
              <svg v-if="generating" class="animate-spin h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              {{ generating ? '生成中...' : '批量生成文档' }}
            </button>
          </div>
        </div>

        <!-- Stats -->
        <div v-if="stats" class="grid grid-cols-4 gap-4 mt-6">
          <div class="bg-blue-50 rounded-lg p-4">
            <div class="text-sm text-blue-600 font-medium">总文档数</div>
            <div class="text-2xl font-bold text-blue-900 mt-1">{{ stats.totalCount }}</div>
          </div>
          <div class="bg-green-50 rounded-lg p-4">
            <div class="text-sm text-green-600 font-medium">已完成</div>
            <div class="text-2xl font-bold text-green-900 mt-1">{{ stats.completedCount }}</div>
          </div>
          <div class="bg-red-50 rounded-lg p-4">
            <div class="text-sm text-red-600 font-medium">失败</div>
            <div class="text-2xl font-bold text-red-900 mt-1">{{ stats.failedCount }}</div>
          </div>
          <div class="bg-purple-50 rounded-lg p-4">
            <div class="text-sm text-purple-600 font-medium">成功率</div>
            <div class="text-2xl font-bold text-purple-900 mt-1">{{ (stats.successRate || 0).toFixed(1) }}%</div>
          </div>
        </div>
      </div>

      <!-- Filters -->
      <div class="bg-white rounded-lg shadow-sm p-4 mb-6">
        <div class="flex items-center gap-4">
          <div class="flex-1">
            <input
              v-model="searchKeyword"
              type="text"
              placeholder="搜索文档标题或源文件..."
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              @input="handleSearch"
            />
          </div>
          <select
            v-model="filterService"
            @change="loadDocuments"
            class="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="">全部服务</option>
            <option v-for="svc in services" :key="svc.serviceId" :value="svc.serviceId">
              {{ svc.serviceName }}
            </option>
          </select>
          <select
            v-model="filterStatus"
            @change="loadDocuments"
            class="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="">全部状态</option>
            <option value="COMPLETED">已完成</option>
            <option value="DRAFT">生成中</option>
            <option value="FAILED">失败</option>
          </select>
        </div>
      </div>

      <!-- Documents Table -->
      <div class="bg-white rounded-lg shadow-sm overflow-hidden">
        <div v-if="loading" class="flex items-center justify-center h-64">
          <div class="text-gray-500">加载中...</div>
        </div>

        <div v-else-if="documents.length === 0" class="flex flex-col items-center justify-center h-64 text-gray-500">
          <svg class="w-16 h-16 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
          </svg>
          <p>暂无文档</p>
          <p class="text-sm mt-1">点击"生成文档"按钮开始</p>
        </div>

        <table v-else class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                标题
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                源文件
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Agent
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                状态
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                创建时间
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                操作
              </th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="doc in documents" :key="doc.id" class="hover:bg-gray-50">
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm font-medium text-gray-900">{{ doc.title }}</div>
              </td>
              <td class="px-6 py-4">
                <div class="text-sm text-gray-500 max-w-md truncate" :title="doc.sourceFile">
                  {{ doc.sourceFile }}
                </div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span class="px-2 py-1 text-xs font-medium rounded-full bg-purple-100 text-purple-800">
                  {{ doc.agentType }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span
                  :class="getStatusClass(doc.status)"
                  class="px-2 py-1 text-xs font-medium rounded-full"
                >
                  {{ getStatusText(doc.status) }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {{ formatDate(doc.createdAt) }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm">
                <button
                  @click="handleView(doc)"
                  class="text-blue-600 hover:text-blue-900 font-medium"
                >
                  查看
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="bg-gray-50 px-6 py-4 flex items-center justify-between border-t border-gray-200">
          <div class="text-sm text-gray-700">
            共 {{ totalElements }} 条记录，第 {{ currentPage + 1 }} / {{ totalPages }} 页
          </div>
          <div class="flex gap-2">
            <button
              @click="goToPage(currentPage - 1)"
              :disabled="currentPage === 0"
              class="px-3 py-1 border border-gray-300 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-100"
            >
              上一页
            </button>
            <button
              @click="goToPage(currentPage + 1)"
              :disabled="currentPage >= totalPages - 1"
              class="px-3 py-1 border border-gray-300 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-100"
            >
              下一页
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { aiDocumentApi, type AIDocument, type DocStats } from '@/api/ai-document';
import { serviceDocumentApi, type ServiceDocumentLibrary } from '@/api/service-document';

const route = useRoute();
const router = useRouter();

const warehouseId = computed(() => route.params.id as string);
const documents = ref<AIDocument[]>([]);
const services = ref<ServiceDocumentLibrary[]>([]);
const stats = ref<DocStats | null>(null);
const loading = ref(false);
const generating = ref(false);
const searchKeyword = ref('');
const filterService = ref('');
const filterStatus = ref('');
const currentPage = ref(0);
const pageSize = ref(20);
const totalPages = ref(0);
const totalElements = ref(0);

// 加载文档列表
const loadDocuments = async () => {
  loading.value = true;
  try {
    const response: any = await aiDocumentApi.listDocuments(warehouseId.value, {
      page: currentPage.value,
      size: pageSize.value,
      status: filterStatus.value || undefined
    });

    documents.value = response.content;
    totalPages.value = response.totalPages;
    totalElements.value = response.totalElements;
  } catch (error: any) {
    console.error('加载文档失败:', error);
    alert('加载文档失败: ' + (error.response?.data?.message || error.message));
  } finally {
    loading.value = false;
  }
};

// 加载统计信息
const loadStats = async () => {
  try {
    stats.value = await aiDocumentApi.getDocStats(warehouseId.value) as any;
  } catch (error: any) {
    console.error('加载统计失败:', error);
  }
};

// 生成项目架构文档
const handleGenerateProject = async () => {
  if (!confirm('确定要生成项目架构文档吗？这将扫描整个项目并分析架构...')) {
    return;
  }

  generating.value = true;
  try {
    const response: any = await aiDocumentApi.generateProjectDoc(warehouseId.value, { agentType: 'claude' });
    alert(`架构文档生成成功！\n标题: ${response.title}`);

    // 立即刷新文档列表
    await loadDocuments();
    await loadStats();

    // 自动打开文档
    router.push(`/ai-documents/${response.documentId}`);
  } catch (error: any) {
    console.error('生成失败:', error);
    alert('生成失败: ' + (error.response?.data?.message || error.message));
  } finally {
    generating.value = false;
  }
};

// 批量生成文档
const handleGenerate = async () => {
  if (!confirm('确定要批量生成文档吗？这可能需要一些时间...')) {
    return;
  }

  generating.value = true;
  try {
    await aiDocumentApi.generateDocs(warehouseId.value, { agentType: 'claude' });
    alert('文档生成任务已启动，请稍候刷新查看');

    // 5秒后自动刷新
    setTimeout(() => {
      loadDocuments();
      loadStats();
    }, 5000);
  } catch (error: any) {
    console.error('生成失败:', error);
    alert('生成失败: ' + (error.response?.data?.message || error.message));
  } finally {
    generating.value = false;
  }
};

// 查看文档
const handleView = (doc: AIDocument) => {
  router.push(`/ai-documents/${doc.id}`);
};

// 搜索处理
let searchTimeout: number;
const handleSearch = () => {
  clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => {
    currentPage.value = 0;
    loadDocuments();
  }, 500) as any;
};

// 翻页
const goToPage = (page: number) => {
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page;
    loadDocuments();
  }
};

// 状态样式
const getStatusClass = (status: string) => {
  const classes = {
    'COMPLETED': 'bg-green-100 text-green-800',
    'DRAFT': 'bg-yellow-100 text-yellow-800',
    'FAILED': 'bg-red-100 text-red-800'
  };
  return classes[status as keyof typeof classes] || 'bg-gray-100 text-gray-800';
};

// 状态文本
const getStatusText = (status: string) => {
  const texts = {
    'COMPLETED': '已完成',
    'DRAFT': '生成中',
    'FAILED': '失败'
  };
  return texts[status as keyof typeof texts] || status;
};

// 格式化日期
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// 加载服务列表
const loadServices = async () => {
  try {
    const response: any = await serviceDocumentApi.listServices(warehouseId.value);
    services.value = response.data || [];
  } catch (error: any) {
    console.error('加载服务列表失败:', error);
  }
};

// 初始化
onMounted(() => {
  loadServices();
  loadDocuments();
  loadStats();
});
</script>
