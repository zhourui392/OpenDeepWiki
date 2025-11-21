import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import viteCompression from 'vite-plugin-compression'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // 加载环境变量
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [
      vue(),
      viteCompression({
        verbose: true,
        disable: false,
        threshold: 10240,
        algorithm: 'gzip',
        ext: '.gz'
      }),
      viteCompression({
        verbose: true,
        disable: false,
        threshold: 10240,
        algorithm: 'brotliCompress',
        ext: '.br'
      })
    ],

    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src')
      }
    },

    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: env.VITE_API_BASE_URL || 'http://localhost:18081',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '/api')
        }
      }
    },

    build: {
      outDir: '../koalawiki-web/src/main/resources/static',
      emptyOutDir: true,
      assetsDir: 'static',
      sourcemap: false,
      cssCodeSplit: true,
      chunkSizeWarningLimit: 1000,

      rollupOptions: {
        output: {
          manualChunks: {
            'vue-vendor': ['vue', 'vue-router', 'pinia'],
            'markdown': ['unified', 'remark-parse', 'remark-gfm', 'remark-math', 'remark-rehype', 'rehype-highlight', 'rehype-slug', 'rehype-autolink-headings', 'rehype-katex', 'rehype-stringify', 'katex', 'highlight.js'],
            'utils': ['axios']
          },
          chunkFileNames: 'static/js/[name].[hash].js',
          entryFileNames: 'static/js/[name].[hash].js',
          assetFileNames: 'static/[ext]/[name].[hash].[ext]'
        }
      }
    }
  }
})
