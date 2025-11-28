/**
 * Vue Router 路由配置
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('@/components/Layout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue')
      },
      {
        path: 'warehouses',
        name: 'Warehouses',
        component: () => import('@/views/Warehouses.vue')
      },
      {
        path: 'agents',
        name: 'Agents',
        component: () => import('@/views/Agents.vue')
      },
      {
        path: 'repository/:id',
        name: 'Repository',
        component: () => import('@/views/Repository.vue')
      },
      {
        path: 'document/:warehouseId/:path*',
        name: 'Document',
        component: () => import('@/views/Document.vue')
      },
      {
        path: 'repository/:id/ai-documents',
        name: 'AIDocuments',
        component: () => import('@/views/AIDocuments.vue')
      },
      {
        path: 'repository/:id/services',
        name: 'ServiceDocuments',
        component: () => import('@/views/ServiceDocuments.vue')
      },
      {
        path: 'ai-documents/:id',
        name: 'AIDocumentDetail',
        component: () => import('@/views/AIDocumentDetail.vue')
      },
      {
        path: 'business-flow',
        name: 'BusinessFlow',
        component: () => import('@/views/BusinessFlow.vue')
      },
      {
        path: 'search',
        name: 'GlobalSearch',
        component: () => import('@/views/GlobalSearch.vue')
      },
      {
        path: 'clusters',
        name: 'ClusterList',
        redirect: '/clusters/default'
      },
      {
        path: 'clusters/:clusterId',
        name: 'ClusterOverview',
        component: () => import('@/views/ClusterOverview.vue')
      },
      {
        path: 'clusters/:clusterId/domains/:code',
        name: 'DomainDetail',
        component: () => import('@/views/DomainDetail.vue')
      },
      {
        path: 'clusters/:clusterId/interfaces',
        name: 'InterfaceList',
        component: () => import('@/views/InterfaceList.vue')
      },
      {
        path: 'clusters/:clusterId/interfaces/:interfaceId',
        name: 'InterfaceDetail',
        component: () => import('@/views/InterfaceDetail.vue')
      },
      {
        path: 'admin/clusters',
        name: 'ClusterAdmin',
        component: () => import('@/views/ClusterAdmin.vue')
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
