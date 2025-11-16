import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 10000,
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
