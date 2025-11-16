import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/repository/:id',
    name: 'Repository',
    component: () => import('@/views/Repository.vue')
  },
  {
    path: '/document/:warehouseId/:path*',
    name: 'Document',
    component: () => import('@/views/Document.vue')
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
