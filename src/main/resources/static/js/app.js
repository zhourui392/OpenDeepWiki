/**
 * 主应用入口
 */
const { createApp } = Vue;
const { createRouter, createWebHistory } = VueRouter;

// 路由配置
const routes = [
  {
    path: '/',
    component: Layout,
    children: [
      { path: '', component: Home },
      { path: 'warehouses', component: Warehouses },
      { path: 'agents', component: Agents },
      { path: 'repository/:id', component: Repository },
      { path: 'repository/:id/domains', component: DomainManagement },
      { path: 'repository/:id/ai-documents', component: AIDocuments },
      { path: 'document/:warehouseId/:path(.*)*', component: Document },
      { path: 'ai-documents/:id', component: AIDocumentDetail }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 创建应用
const app = createApp({});

// 注册Element Plus
app.use(ElementPlus);

// 注册路由
app.use(router);

// 注册全局组件
app.component('markdown-viewer', MarkdownViewer);

// 挂载应用
app.mount('#app');
