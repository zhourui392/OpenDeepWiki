<template>
  <div class="flex h-screen bg-gray-50">
    <aside class="w-64 bg-white shadow-lg">
      <div class="p-4 border-b">
        <router-link to="/" class="text-xl font-bold text-blue-600">
          OpenDeepWiki
        </router-link>
      </div>
      <nav class="p-4">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="block px-4 py-2 mb-2 rounded hover:bg-gray-100"
          :class="{ 'bg-blue-50 text-blue-600': $route.path === item.path }"
        >
          {{ item.label }}
        </router-link>
      </nav>
    </aside>
    <main class="flex-1 overflow-auto">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
const menuItems = [
  { path: '/', label: '首页' },
  { path: '/warehouses', label: '仓库管理' },
  { path: '/agents', label: 'Agent 管理' },
  { path: '/business-flow', label: '业务流程追踪' }
]
</script>
