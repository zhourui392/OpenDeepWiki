/**
 * 页面组件
 */

// 首页
const Home = {
  template: `
    <div class="home-page">
      <h1>欢迎使用 KoalaWiki</h1>
      <p>智能代码文档管理平台</p>
      <el-button type="primary" @click="$router.push('/warehouses')">开始使用</el-button>
    </div>
  `
};

// 仓库列表页
const Warehouses = {
  template: `
    <div class="warehouses-page">
      <div class="page-header">
        <h2>仓库列表</h2>
        <el-button type="primary" @click="showCreateDialog = true">添加仓库</el-button>
      </div>
      <el-table :data="warehouses" v-loading="loading">
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="address" label="地址" />
        <el-table-column prop="status" label="状态">
          <template #default="{row}">
            <el-tag :type="row.status === 'COMPLETED' ? 'success' : 'warning'">{{row.status}}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="{row}">
            <el-button size="small" @click="$router.push('/repository/' + row.id)">详情</el-button>
            <el-button size="small" type="success" @click="handleSync(row)">同步</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" @current-change="loadData" />

      <el-dialog v-model="showCreateDialog" title="添加仓库" width="500px">
        <el-form :model="form" label-width="80px">
          <el-form-item label="地址" required>
            <el-input v-model="form.address" placeholder="Git仓库地址" />
          </el-form-item>
          <el-form-item label="分支">
            <el-input v-model="form.branch" placeholder="默认master" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showCreateDialog = false">取消</el-button>
          <el-button type="primary" @click="handleCreate">确定</el-button>
        </template>
      </el-dialog>
    </div>
  `,
  setup() {
    const loading = ref(false);
    const warehouses = ref([]);
    const page = ref(1);
    const pageSize = ref(10);
    const total = ref(0);
    const showCreateDialog = ref(false);
    const form = ref({ address: '', branch: 'master' });

    const loadData = async () => {
      loading.value = true;
      try {
        const res = await api.warehouse.list(page.value, pageSize.value);
        warehouses.value = res.data?.items || [];
        total.value = res.data?.total || 0;
      } catch (e) {
        ElMessage.error('加载失败');
      } finally {
        loading.value = false;
      }
    };

    const handleCreate = async () => {
      if (!form.value.address) {
        ElMessage.warning('请输入仓库地址');
        return;
      }
      try {
        await api.warehouse.create(form.value);
        ElMessage.success('添加成功');
        showCreateDialog.value = false;
        form.value = { address: '', branch: 'master' };
        loadData();
      } catch (e) {
        ElMessage.error('添加失败');
      }
    };

    const handleSync = async (row) => {
      try {
        await api.warehouse.sync(row.id);
        ElMessage.success('同步已触发');
        loadData();
      } catch (e) {
        ElMessage.error('同步失败');
      }
    };

    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm('确定删除该仓库吗？', '提示', { type: 'warning' });
        await api.warehouse.delete(row.id);
        ElMessage.success('删除成功');
        loadData();
      } catch (e) {
        if (e !== 'cancel') ElMessage.error('删除失败');
      }
    };

    onMounted(loadData);
    return { loading, warehouses, page, pageSize, total, showCreateDialog, form, loadData, handleCreate, handleSync, handleDelete };
  }
};

// 仓库详情页
const Repository = {
  template: `
    <div class="repository-page" v-loading="loading">
      <div class="page-header">
        <h2>{{warehouse?.name || '仓库详情'}}</h2>
        <div>
          <el-button @click="$router.push('/repository/' + warehouseId + '/domains')">领域管理</el-button>
          <el-button @click="$router.push('/repository/' + warehouseId + '/ai-documents')">AI文档</el-button>
        </div>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="地址">{{warehouse?.address}}</el-descriptions-item>
        <el-descriptions-item label="分支">{{warehouse?.branch}}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="warehouse?.status === 'COMPLETED' ? 'success' : 'warning'">{{warehouse?.status}}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="描述">{{warehouse?.description}}</el-descriptions-item>
      </el-descriptions>
      <div class="catalog-section" v-if="catalog.length">
        <h3>文档目录</h3>
        <el-tree :data="catalog" :props="{label: 'name', children: 'children'}" @node-click="handleNodeClick" />
      </div>
    </div>
  `,
  setup() {
    const route = VueRouter.useRoute();
    const router = VueRouter.useRouter();
    const warehouseId = computed(() => route.params.id);
    const loading = ref(false);
    const warehouse = ref(null);
    const catalog = ref([]);

    const loadData = async () => {
      loading.value = true;
      try {
        const [wRes, cRes] = await Promise.all([
          api.warehouse.get(warehouseId.value),
          api.document.getCatalog(warehouseId.value)
        ]);
        warehouse.value = wRes.data;
        catalog.value = cRes.data || [];
      } catch (e) {
        ElMessage.error('加载失败');
      } finally {
        loading.value = false;
      }
    };

    const handleNodeClick = (node) => {
      if (node.path) {
        router.push(`/document/${warehouseId.value}/${node.path}`);
      }
    };

    onMounted(loadData);
    return { warehouseId, loading, warehouse, catalog, handleNodeClick };
  }
};

// 文档查看页
const Document = {
  template: `
    <div class="document-page" v-loading="loading">
      <div class="page-header">
        <el-button @click="$router.back()">返回</el-button>
        <h2>{{docPath}}</h2>
      </div>
      <markdown-viewer :content="content" />
    </div>
  `,
  components: { MarkdownViewer },
  setup() {
    const route = VueRouter.useRoute();
    const warehouseId = computed(() => route.params.warehouseId);
    const docPath = computed(() => route.params.path?.join('/') || '');
    const loading = ref(false);
    const content = ref('');

    const loadData = async () => {
      if (!docPath.value) return;
      loading.value = true;
      try {
        const res = await api.document.get(warehouseId.value, docPath.value);
        content.value = res.data?.content || '';
      } catch (e) {
        ElMessage.error('加载失败');
      } finally {
        loading.value = false;
      }
    };

    watch(docPath, loadData, { immediate: true });
    return { docPath, loading, content };
  }
};

// 领域管理页
const DomainManagement = {
  template: `
    <div class="domain-management">
      <div class="page-header">
        <h2>领域管理</h2>
        <el-button type="primary" @click="showCreateDomainDialog = true">新建领域</el-button>
      </div>

      <el-collapse v-model="activeDomains" v-loading="loading">
        <el-collapse-item v-for="domain in domains" :key="domain.id" :name="domain.id">
          <template #title>
            <div class="domain-title">
              <span class="domain-name">{{domain.name}}</span>
              <span class="domain-desc">{{domain.description}}</span>
              <div class="domain-actions" @click.stop>
                <el-button size="small" @click="handleEditDomain(domain)">编辑</el-button>
                <el-button size="small" type="success" @click="handleGenerateDomainDoc(domain)" :loading="generatingDomainDoc === domain.id">生成文档</el-button>
                <el-button size="small" type="danger" @click="handleDeleteDomain(domain)">删除</el-button>
              </div>
            </div>
          </template>

          <div v-if="domain.documentContent" class="domain-doc-preview">
            <el-button size="small" @click="showDomainDoc(domain)">查看领域文档</el-button>
          </div>

          <div class="services-section">
            <div class="services-header">
              <span>服务列表</span>
              <el-button size="small" type="primary" @click="handleAddService(domain)">添加服务</el-button>
            </div>
            <el-table :data="domain.services || []" size="small">
              <el-table-column prop="serviceName" label="服务名称" />
              <el-table-column prop="serviceId" label="服务ID" />
              <el-table-column prop="description" label="描述" />
              <el-table-column label="操作" width="280">
                <template #default="{row}">
                  <el-button size="small" @click="handleEditService(domain, row)">编辑</el-button>
                  <el-button size="small" type="success" @click="handleGenerateServiceDoc(domain, row)" :loading="generatingServiceDoc === row.id">生成文档</el-button>
                  <el-button size="small" type="danger" @click="handleDeleteService(domain, row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-collapse-item>
      </el-collapse>

      <el-empty v-if="!loading && domains.length === 0" description="暂无领域" />

      <!-- 创建领域对话框 -->
      <el-dialog v-model="showCreateDomainDialog" title="新建领域" width="500px">
        <el-form :model="domainForm" label-width="80px">
          <el-form-item label="名称" required><el-input v-model="domainForm.name" placeholder="领域名称" /></el-form-item>
          <el-form-item label="代码" required><el-input v-model="domainForm.code" placeholder="领域代码，用于Git路径" /></el-form-item>
          <el-form-item label="描述"><el-input v-model="domainForm.description" type="textarea" placeholder="领域描述" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showCreateDomainDialog = false">取消</el-button>
          <el-button type="primary" @click="handleCreateDomain">确定</el-button>
        </template>
      </el-dialog>

      <!-- 编辑领域对话框 -->
      <el-dialog v-model="showEditDomainDialog" title="编辑领域" width="500px">
        <el-form :model="editDomainForm" label-width="80px">
          <el-form-item label="名称" required><el-input v-model="editDomainForm.name" /></el-form-item>
          <el-form-item label="代码" required><el-input v-model="editDomainForm.code" placeholder="领域代码，用于Git路径" /></el-form-item>
          <el-form-item label="描述"><el-input v-model="editDomainForm.description" type="textarea" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showEditDomainDialog = false">取消</el-button>
          <el-button type="primary" @click="handleUpdateDomain">确定</el-button>
        </template>
      </el-dialog>

      <!-- 创建服务对话框 -->
      <el-dialog v-model="showCreateServiceDialog" title="添加服务" width="600px">
        <el-form :model="serviceForm" label-width="100px">
          <el-form-item label="服务ID" required><el-input v-model="serviceForm.serviceId" placeholder="英文标识" /></el-form-item>
          <el-form-item label="服务名称" required><el-input v-model="serviceForm.serviceName" /></el-form-item>
          <el-form-item label="描述"><el-input v-model="serviceForm.description" type="textarea" /></el-form-item>
          <el-form-item label="源码匹配"><el-input v-model="serviceGlobsInput" type="textarea" placeholder="每行一个规则，如: src/main/java/**/*.java" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showCreateServiceDialog = false">取消</el-button>
          <el-button type="primary" @click="handleCreateService">确定</el-button>
        </template>
      </el-dialog>

      <!-- 编辑服务对话框 -->
      <el-dialog v-model="showEditServiceDialog" title="编辑服务" width="600px">
        <el-form :model="editServiceForm" label-width="100px">
          <el-form-item label="服务名称" required><el-input v-model="editServiceForm.serviceName" /></el-form-item>
          <el-form-item label="描述"><el-input v-model="editServiceForm.description" type="textarea" /></el-form-item>
          <el-form-item label="源码匹配"><el-input v-model="editServiceGlobsInput" type="textarea" placeholder="每行一个规则" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showEditServiceDialog = false">取消</el-button>
          <el-button type="primary" @click="handleUpdateService">确定</el-button>
        </template>
      </el-dialog>

      <!-- 文档预览对话框 -->
      <el-dialog v-model="showDocDialog" :title="docDialogTitle" width="80%" top="5vh">
        <div class="doc-content" v-html="renderedDoc"></div>
      </el-dialog>
    </div>
  `,
  setup() {
    const route = VueRouter.useRoute();
    const warehouseId = computed(() => route.params.id);

    const loading = ref(false);
    const domains = ref([]);
    const activeDomains = ref([]);

    const showCreateDomainDialog = ref(false);
    const showEditDomainDialog = ref(false);
    const domainForm = ref({ name: '', description: '', code: '' });
    const editDomainForm = ref({ id: '', name: '', description: '', code: '' });

    const showCreateServiceDialog = ref(false);
    const showEditServiceDialog = ref(false);
    const currentDomainId = ref('');
    const currentServiceId = ref('');
    const serviceForm = ref({ serviceId: '', serviceName: '', description: '' });
    const editServiceForm = ref({ serviceName: '', description: '' });
    const serviceGlobsInput = ref('');
    const editServiceGlobsInput = ref('');

    const generatingDomainDoc = ref('');
    const generatingServiceDoc = ref('');

    const showDocDialog = ref(false);
    const docDialogTitle = ref('');
    const docContent = ref('');
    const renderedDoc = computed(() => marked.parse(docContent.value || ''));

    const loadDomains = async () => {
      loading.value = true;
      try {
        const res = await api.domain.list(warehouseId.value);
        const domainList = res.data || [];
        for (const domain of domainList) {
          const detailRes = await api.domain.get(warehouseId.value, domain.id);
          domain.services = detailRes.data?.services || [];
        }
        domains.value = domainList;
      } catch (e) {
        ElMessage.error('加载领域列表失败');
      } finally {
        loading.value = false;
      }
    };

    const handleCreateDomain = async () => {
      if (!domainForm.value.name) {
        ElMessage.warning('请输入领域名称');
        return;
      }
      try {
        await api.domain.create(warehouseId.value, domainForm.value);
        ElMessage.success('创建成功');
        showCreateDomainDialog.value = false;
        domainForm.value = { name: '', description: '', code: '' };
        loadDomains();
      } catch (e) {
        ElMessage.error('创建失败');
      }
    };

    const handleEditDomain = (domain) => {
      editDomainForm.value = { id: domain.id, name: domain.name, description: domain.description || '', code: domain.code || '' };
      showEditDomainDialog.value = true;
    };

    const handleUpdateDomain = async () => {
      try {
        await api.domain.update(warehouseId.value, editDomainForm.value.id, {
          name: editDomainForm.value.name,
          description: editDomainForm.value.description,
          code: editDomainForm.value.code
        });
        ElMessage.success('更新成功');
        showEditDomainDialog.value = false;
        loadDomains();
      } catch (e) {
        ElMessage.error('更新失败');
      }
    };

    const handleDeleteDomain = async (domain) => {
      try {
        await ElMessageBox.confirm('确定删除该领域吗？', '提示', { type: 'warning' });
        await api.domain.delete(warehouseId.value, domain.id);
        ElMessage.success('删除成功');
        loadDomains();
      } catch (e) {
        if (e !== 'cancel') ElMessage.error('删除失败');
      }
    };

    const handleGenerateDomainDoc = async (domain) => {
      generatingDomainDoc.value = domain.id;
      try {
        await api.domain.generateDoc(warehouseId.value, domain.id);
        ElMessage.success('领域文档生成成功');
        loadDomains();
      } catch (e) {
        ElMessage.error('生成失败');
      } finally {
        generatingDomainDoc.value = '';
      }
    };

    const showDomainDoc = (domain) => {
      docDialogTitle.value = `${domain.name} - 领域文档`;
      docContent.value = domain.documentContent || '';
      showDocDialog.value = true;
    };

    const handleAddService = (domain) => {
      currentDomainId.value = domain.id;
      serviceForm.value = { serviceId: '', serviceName: '', description: '' };
      serviceGlobsInput.value = '';
      showCreateServiceDialog.value = true;
    };

    const handleCreateService = async () => {
      if (!serviceForm.value.serviceId || !serviceForm.value.serviceName) {
        ElMessage.warning('请填写必填项');
        return;
      }
      try {
        const sourceGlobs = serviceGlobsInput.value.split('\n').map(s => s.trim()).filter(s => s.length > 0);
        await api.service.create(warehouseId.value, currentDomainId.value, { ...serviceForm.value, sourceGlobs });
        ElMessage.success('创建成功');
        showCreateServiceDialog.value = false;
        loadDomains();
      } catch (e) {
        ElMessage.error('创建失败');
      }
    };

    const handleEditService = (domain, service) => {
      currentDomainId.value = domain.id;
      currentServiceId.value = service.id;
      editServiceForm.value = { serviceName: service.serviceName, description: service.description || '' };
      editServiceGlobsInput.value = (service.sourceGlobs || []).join('\n');
      showEditServiceDialog.value = true;
    };

    const handleUpdateService = async () => {
      try {
        const sourceGlobs = editServiceGlobsInput.value.split('\n').map(s => s.trim()).filter(s => s.length > 0);
        await api.service.update(warehouseId.value, currentDomainId.value, currentServiceId.value, { ...editServiceForm.value, sourceGlobs });
        ElMessage.success('更新成功');
        showEditServiceDialog.value = false;
        loadDomains();
      } catch (e) {
        ElMessage.error('更新失败');
      }
    };

    const handleDeleteService = async (domain, service) => {
      try {
        await ElMessageBox.confirm('确定删除该服务吗？', '提示', { type: 'warning' });
        await api.service.delete(warehouseId.value, domain.id, service.id);
        ElMessage.success('删除成功');
        loadDomains();
      } catch (e) {
        if (e !== 'cancel') ElMessage.error('删除失败');
      }
    };

    const handleGenerateServiceDoc = async (domain, service) => {
      generatingServiceDoc.value = service.id;
      try {
        await api.service.generateDoc(warehouseId.value, domain.id, service.id);
        ElMessage.success('服务文档生成成功');
        loadDomains();
      } catch (e) {
        ElMessage.error('生成失败');
      } finally {
        generatingServiceDoc.value = '';
      }
    };

    onMounted(loadDomains);

    return {
      warehouseId, loading, domains, activeDomains,
      showCreateDomainDialog, showEditDomainDialog, domainForm, editDomainForm,
      showCreateServiceDialog, showEditServiceDialog, currentDomainId, currentServiceId,
      serviceForm, editServiceForm, serviceGlobsInput, editServiceGlobsInput,
      generatingDomainDoc, generatingServiceDoc,
      showDocDialog, docDialogTitle, docContent, renderedDoc,
      loadDomains, handleCreateDomain, handleEditDomain, handleUpdateDomain, handleDeleteDomain,
      handleGenerateDomainDoc, showDomainDoc,
      handleAddService, handleCreateService, handleEditService, handleUpdateService, handleDeleteService,
      handleGenerateServiceDoc
    };
  }
};

// AI文档列表页
const AIDocuments = {
  template: `
    <div class="ai-documents-page">
      <div class="page-header">
        <el-button @click="$router.back()">返回</el-button>
        <h2>AI文档</h2>
      </div>
      <el-table :data="documents" v-loading="loading">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="type" label="类型" />
        <el-table-column prop="createdAt" label="创建时间" />
        <el-table-column label="操作" width="120">
          <template #default="{row}">
            <el-button size="small" @click="$router.push('/ai-documents/' + row.id)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  `,
  setup() {
    const route = VueRouter.useRoute();
    const warehouseId = computed(() => route.params.id);
    const loading = ref(false);
    const documents = ref([]);

    const loadData = async () => {
      loading.value = true;
      try {
        const res = await api.aiDocument.list(warehouseId.value);
        documents.value = res.data || [];
      } catch (e) {
        ElMessage.error('加载失败');
      } finally {
        loading.value = false;
      }
    };

    onMounted(loadData);
    return { loading, documents };
  }
};

// AI文档详情页
const AIDocumentDetail = {
  template: `
    <div class="ai-document-detail" v-loading="loading">
      <div class="page-header">
        <el-button @click="$router.back()">返回</el-button>
        <h2>{{document?.title || 'AI文档详情'}}</h2>
      </div>
      <markdown-viewer :content="document?.content" />
    </div>
  `,
  components: { MarkdownViewer },
  setup() {
    const route = VueRouter.useRoute();
    const docId = computed(() => route.params.id);
    const loading = ref(false);
    const document = ref(null);

    const loadData = async () => {
      loading.value = true;
      try {
        const res = await api.aiDocument.get(docId.value);
        document.value = res.data;
      } catch (e) {
        ElMessage.error('加载失败');
      } finally {
        loading.value = false;
      }
    };

    onMounted(loadData);
    return { loading, document };
  }
};

// Agents页
const Agents = {
  template: `
    <div class="agents-page">
      <div class="page-header">
        <h2>Agents</h2>
      </div>
      <el-table :data="agents" v-loading="loading">
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="type" label="类型" />
        <el-table-column prop="status" label="状态">
          <template #default="{row}">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{row.status}}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" />
      </el-table>
    </div>
  `,
  setup() {
    const loading = ref(false);
    const agents = ref([]);

    const loadData = async () => {
      loading.value = true;
      try {
        const res = await api.agent.list();
        agents.value = res.data || [];
      } catch (e) {
        ElMessage.error('加载失败');
      } finally {
        loading.value = false;
      }
    };

    onMounted(loadData);
    return { loading, agents };
  }
};

// 全局领域管理页
const GlobalDomains = {
  template: `
    <div class="global-domains-page">
      <div class="page-header">
        <h2>领域管理</h2>
        <el-button type="primary" @click="showCreateDialog = true">新建领域</el-button>
      </div>

      <el-collapse v-model="activeDomains" v-loading="loading">
        <el-collapse-item v-for="domain in domains" :key="domain.id" :name="domain.id">
          <template #title>
            <div class="domain-title">
              <span class="domain-name">{{domain.name}}</span>
              <el-tag v-if="domain.code" size="small" type="info">{{domain.code}}</el-tag>
              <span class="domain-desc">{{domain.description}}</span>
              <div class="domain-actions" @click.stop>
                <el-button size="small" @click="handleEditDomain(domain)">编辑</el-button>
                <el-button size="small" type="success" @click="handleGenerateDomainDoc(domain)" :loading="generatingDomainDoc === domain.id">生成文档</el-button>
                <el-button size="small" type="danger" @click="handleDeleteDomain(domain)">删除</el-button>
              </div>
            </div>
          </template>

          <div v-if="domain.documentContent" class="domain-doc-preview">
            <el-button size="small" @click="showDomainDoc(domain)">查看领域文档</el-button>
          </div>

          <div class="services-section">
            <div class="services-header">
              <span>服务列表</span>
              <el-button size="small" type="primary" @click="handleAddService(domain)">添加服务</el-button>
            </div>
            <el-table :data="domain.services || []" size="small">
              <el-table-column prop="serviceName" label="服务名称" />
              <el-table-column prop="serviceId" label="服务ID" />
              <el-table-column label="关联仓库">
                <template #default="{row}">
                  <span>{{getWarehouseName(row.warehouseId)}}</span>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="描述" />
              <el-table-column label="操作" width="200">
                <template #default="{row}">
                  <el-button size="small" type="success" @click="handleGenerateServiceDoc(domain, row)" :loading="generatingServiceDoc === row.id">生成文档</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-collapse-item>
      </el-collapse>

      <el-empty v-if="!loading && domains.length === 0" description="暂无领域，请先创建领域" />

      <!-- 创建领域对话框 -->
      <el-dialog v-model="showCreateDialog" title="新建领域" width="500px">
        <el-form :model="domainForm" label-width="80px">
          <el-form-item label="名称" required><el-input v-model="domainForm.name" placeholder="领域名称" /></el-form-item>
          <el-form-item label="代码" required><el-input v-model="domainForm.code" placeholder="领域代码，用于Git路径，如: domain-a" /></el-form-item>
          <el-form-item label="描述"><el-input v-model="domainForm.description" type="textarea" placeholder="领域描述" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showCreateDialog = false">取消</el-button>
          <el-button type="primary" @click="handleCreateDomain">确定</el-button>
        </template>
      </el-dialog>

      <!-- 编辑领域对话框 -->
      <el-dialog v-model="showEditDialog" title="编辑领域" width="500px">
        <el-form :model="editForm" label-width="80px">
          <el-form-item label="名称" required><el-input v-model="editForm.name" /></el-form-item>
          <el-form-item label="代码" required><el-input v-model="editForm.code" placeholder="领域代码" /></el-form-item>
          <el-form-item label="描述"><el-input v-model="editForm.description" type="textarea" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showEditDialog = false">取消</el-button>
          <el-button type="primary" @click="handleUpdateDomain">确定</el-button>
        </template>
      </el-dialog>

      <!-- 添加服务对话框 -->
      <el-dialog v-model="showServiceDialog" title="添加服务" width="600px">
        <el-form :model="serviceForm" label-width="100px">
          <el-form-item label="关联仓库" required>
            <el-select v-model="serviceForm.warehouseId" placeholder="选择仓库" style="width: 100%">
              <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id">
                <span>{{w.name}}</span>
                <span style="color: #999; font-size: 12px; margin-left: 8px;">{{w.address}}</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="服务ID" required><el-input v-model="serviceForm.serviceId" placeholder="英文标识" /></el-form-item>
          <el-form-item label="服务名称" required><el-input v-model="serviceForm.serviceName" /></el-form-item>
          <el-form-item label="描述"><el-input v-model="serviceForm.description" type="textarea" /></el-form-item>
          <el-form-item label="源码匹配"><el-input v-model="serviceGlobsInput" type="textarea" placeholder="每行一个规则，如: src/main/java/**/*.java" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showServiceDialog = false">取消</el-button>
          <el-button type="primary" @click="handleCreateService">确定</el-button>
        </template>
      </el-dialog>

      <!-- 文档预览对话框 -->
      <el-dialog v-model="showDocDialog" :title="docDialogTitle" width="80%" top="5vh">
        <div class="doc-content" v-html="renderedDoc"></div>
      </el-dialog>
    </div>
  `,
  setup() {
    const loading = ref(false);
    const domains = ref([]);
    const activeDomains = ref([]);
    const warehouses = ref([]);

    const showCreateDialog = ref(false);
    const showEditDialog = ref(false);
    const showServiceDialog = ref(false);
    const domainForm = ref({ name: '', description: '', code: '' });
    const editForm = ref({ id: '', name: '', description: '', code: '' });
    const currentDomainId = ref('');
    const serviceForm = ref({ warehouseId: '', serviceId: '', serviceName: '', description: '' });
    const serviceGlobsInput = ref('');

    const generatingDomainDoc = ref('');
    const generatingServiceDoc = ref('');

    const showDocDialog = ref(false);
    const docDialogTitle = ref('');
    const docContent = ref('');
    const renderedDoc = computed(() => marked.parse(docContent.value || ''));

    const getWarehouseName = (warehouseId) => {
      const w = warehouses.value.find(w => w.id === warehouseId);
      return w ? w.name : warehouseId;
    };

    const loadData = async () => {
      loading.value = true;
      try {
        const [domainRes, warehouseRes] = await Promise.all([
          api.globalDomain.list(),
          api.globalDomain.listWarehouses()
        ]);
        const domainList = domainRes.data || [];
        warehouses.value = warehouseRes.data || [];

        for (const domain of domainList) {
          const detailRes = await api.globalDomain.get(domain.id);
          domain.services = detailRes.data?.services || [];
        }
        domains.value = domainList;
      } catch (e) {
        ElMessage.error('加载失败');
      } finally {
        loading.value = false;
      }
    };

    const handleCreateDomain = async () => {
      if (!domainForm.value.name || !domainForm.value.code) {
        ElMessage.warning('请填写名称和代码');
        return;
      }
      try {
        await api.globalDomain.create(domainForm.value);
        ElMessage.success('创建成功');
        showCreateDialog.value = false;
        domainForm.value = { name: '', description: '', code: '' };
        loadData();
      } catch (e) {
        ElMessage.error('创建失败');
      }
    };

    const handleEditDomain = (domain) => {
      editForm.value = { id: domain.id, name: domain.name, description: domain.description || '', code: domain.code || '' };
      showEditDialog.value = true;
    };

    const handleUpdateDomain = async () => {
      try {
        await api.globalDomain.update(editForm.value.id, {
          name: editForm.value.name,
          description: editForm.value.description,
          code: editForm.value.code
        });
        ElMessage.success('更新成功');
        showEditDialog.value = false;
        loadData();
      } catch (e) {
        ElMessage.error('更新失败');
      }
    };

    const handleDeleteDomain = async (domain) => {
      try {
        await ElMessageBox.confirm('确定删除该领域吗？', '提示', { type: 'warning' });
        await api.globalDomain.delete(domain.id);
        ElMessage.success('删除成功');
        loadData();
      } catch (e) {
        if (e !== 'cancel') ElMessage.error('删除失败');
      }
    };

    const handleGenerateDomainDoc = async (domain) => {
      generatingDomainDoc.value = domain.id;
      try {
        await api.globalDomain.generateDoc(domain.id);
        ElMessage.success('领域文档生成成功');
        loadData();
      } catch (e) {
        ElMessage.error('生成失败');
      } finally {
        generatingDomainDoc.value = '';
      }
    };

    const showDomainDoc = (domain) => {
      docDialogTitle.value = domain.name + ' - 领域文档';
      docContent.value = domain.documentContent || '';
      showDocDialog.value = true;
    };

    const handleAddService = (domain) => {
      currentDomainId.value = domain.id;
      serviceForm.value = { warehouseId: '', serviceId: '', serviceName: '', description: '' };
      serviceGlobsInput.value = '';
      showServiceDialog.value = true;
    };

    const handleCreateService = async () => {
      if (!serviceForm.value.warehouseId || !serviceForm.value.serviceId || !serviceForm.value.serviceName) {
        ElMessage.warning('请填写必填项');
        return;
      }
      try {
        const sourceGlobs = serviceGlobsInput.value.split('\\n').map(s => s.trim()).filter(s => s.length > 0);
        await api.globalDomain.createService(currentDomainId.value, { ...serviceForm.value, sourceGlobs });
        ElMessage.success('创建成功');
        showServiceDialog.value = false;
        loadData();
      } catch (e) {
        ElMessage.error('创建失败');
      }
    };

    const handleGenerateServiceDoc = async (domain, service) => {
      generatingServiceDoc.value = service.id;
      try {
        await api.globalDomain.generateServiceDoc(domain.id, service.id);
        ElMessage.success('服务文档生成成功');
        loadData();
      } catch (e) {
        ElMessage.error('生成失败');
      } finally {
        generatingServiceDoc.value = '';
      }
    };

    onMounted(loadData);

    return {
      loading, domains, activeDomains, warehouses,
      showCreateDialog, showEditDialog, showServiceDialog,
      domainForm, editForm, currentDomainId, serviceForm, serviceGlobsInput,
      generatingDomainDoc, generatingServiceDoc,
      showDocDialog, docDialogTitle, docContent, renderedDoc,
      getWarehouseName, loadData,
      handleCreateDomain, handleEditDomain, handleUpdateDomain, handleDeleteDomain,
      handleGenerateDomainDoc, showDomainDoc,
      handleAddService, handleCreateService, handleGenerateServiceDoc
    };
  }
};
