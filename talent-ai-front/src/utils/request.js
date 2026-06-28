import axios from 'axios'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'

function clearSessionAndRedirectLogin() {
  try {
    useAuthStore().logout()
  } catch {
    localStorage.removeItem('talent_token')
    localStorage.removeItem('talent_user')
  }
  const current = router.currentRoute.value.fullPath
  if (current !== '/login' && current !== '/register') {
    void router.push({ path: '/login', query: { redirect: current } })
  }
}

// 1. 创建 axios 实例
const request = axios.create({
  // 开发环境走 Vite 代理 /api -> 网关 8080；生产可配置 VITE_API_BASE_URL
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000,
})

// 2. 请求拦截器 (Request Interceptor)
// 作用：在请求发出去之前，自动把 LocalStorage 里的 Token 塞进请求头里
request.interceptors.request.use(
  config => {
    // 从本地存储获取 token
    const token = localStorage.getItem('talent_token')
    if (token) {
      // 按照 OAuth2.0 标准，Token 前面通常要加 'Bearer '
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 3. 响应拦截器 (Response Interceptor)
// 作用：统一处理后端返回的数据和报错信息
request.interceptors.response.use(
  response => {
    const res = response.data
    const code = Number(res?.code)
    if (Number.isNaN(code) || code !== 200) {
      if (code === 401) {
        clearSessionAndRedirectLogin()
      }
      return Promise.reject(new Error(res?.msg || res?.message || res?.error || '请求失败'))
    }
    // 列表等接口：{ code, msg, data: { records, total } } → 返回 data
    // 登录接口：{ code, msg, token, userInfo } → 无 data，返回整包
    if (res.data !== undefined && res.data !== null) {
      return res.data
    }
    return res
  },
  error => {
    const status = error.response?.status
    if (status === 401) {
      clearSessionAndRedirectLogin()
    }
    const msg =
      error.response?.data?.msg ||
      (status === 503 ? '人才库服务不可用(503)，请启动 talent-talent-pool 微服务(8086)后重试' : null) ||
      (status === 404 ? '接口不存在(404)，请重启 talent-auth 与 talent-gateway 后重试' : null) ||
      (status === 403 ? '无权限访问，请使用管理员账号登录' : null) ||
      (error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : null) ||
      (error.message === 'Network Error' ? '无法连接服务器，请确认网关(8080)与 talent-auth 已启动' : null) ||
      error.message ||
      '网络请求失败'
    console.error('网络请求错误:', error)
    return Promise.reject(new Error(msg))
  }
)

export default request