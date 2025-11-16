<template>
  <div id="app" class="min-h-screen bg-gray-50">
    <header class="bg-white shadow-sm">
      <div class="container mx-auto px-4 py-4">
        <router-link to="/" class="text-2xl font-bold text-blue-600">
          OpenDeepWiki
        </router-link>
      </div>
    </header>

    <main class="py-6">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
</script>

<style>
body {
  margin: 0;
  font-family: system-ui, -apple-system, sans-serif;
}
</style>
