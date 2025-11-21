import axios from 'axios'

// 获取 API 基础 URL
// 开发环境：使用相对路径 /api（通过 Vite proxy 代理到后端）
// 生产环境：使用环境变量配置的完整 URL
const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'

const apiClient = axios.create({
  baseURL,
  timeout: 600000,
  headers: {
    'Content-Type': 'application/json'
  }
})

apiClient.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

apiClient.interceptors.response.use(
  response => {
    // 后端返回的是 Result<T> 结构: { code, message, data, timestamp }
    // 我们提取 data 字段返回
    const result = response.data
    if (result && typeof result === 'object' && 'data' in result) {
      return result.data
    }
    return result
  },
  error => {
    if (error.response?.status === 401) {
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default apiClient
