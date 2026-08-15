import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'

export interface ApiResult<T = unknown> {
  code: number
  msg: string
  data: T
}

/**
 * axios 实例。职责:
 *  - 请求拦截:自动携带 access token;
 *  - 响应拦截:HTTP 200 但业务 code != 200 视为失败;
 *  - 401 时用 refresh token 静默换新并重放原请求(单飞,失败则清令牌跳登录)。
 */

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 延迟引用避免与 api/auth.ts 循环依赖
async function doRefresh(): Promise<string> {
  const { refreshTokens, getAccessToken } = await import('@/api/auth')
  await refreshTokens()
  return getAccessToken() as string
}

// 单飞刷新:并发 401 共享同一个刷新 Promise
let refreshing: Promise<string> | null = null

function refreshSingleFlight(): Promise<string> {
  if (!refreshing) {
    refreshing = doRefresh().finally(() => {
      refreshing = null
    })
  }
  return refreshing
}

function forceReLogin(): void {
  import('@/api/auth').then(({ clearTokens }) => {
    clearTokens()
    if (window.location.pathname !== '/login') {
      window.location.href = '/login'
    }
  })
}

request.interceptors.request.use(
  (config: AxiosRequestConfig) => {
    const token = localStorage.getItem('access_token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: AxiosError) => Promise.reject(error)
)

request.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    const res = response.data
    if (res.code !== 200) {
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return response
  },
  async (error: AxiosError<ApiResult>) => {
    const status = error.response?.status
    const originalConfig = error.config as (AxiosRequestConfig & { _retried?: boolean }) | undefined

    // access token 失效:尝试刷新并重放一次(认证接口自身除外)
    if (status === 401 && originalConfig && !originalConfig._retried && !originalConfig.url?.includes('/auth/')) {
      originalConfig._retried = true
      try {
        const newToken = await refreshSingleFlight()
        originalConfig.headers = {
          ...originalConfig.headers,
          Authorization: `Bearer ${newToken}`
        }
        return request(originalConfig)
      } catch {
        forceReLogin()
        return Promise.reject(error)
      }
    }

    if (status === 401) {
      forceReLogin()
    }
    const msg = error.response?.data?.msg || error.message || '网络异常'
    return Promise.reject(new Error(msg))
  }
)

export default request
