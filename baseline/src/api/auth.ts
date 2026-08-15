import { reactive } from 'vue'
import request from '@/utils/request'

/**
 * 认证状态与令牌存储。令牌持久化在 localStorage(access/refresh 分键),
 * 供 request 拦截器读取与路由守卫判断登录态。
 */

const ACCESS_KEY = 'access_token'
const REFRESH_KEY = 'refresh_token'

export interface TokenResponse {
  accessToken: string
  expiresIn: number
  refreshToken: string
  refreshExpiresIn: number
}

export interface LoginUser {
  userId: number
  username: string
  nickName: string
}

export const authState = reactive<{ user: LoginUser | null }>({
  user: null
})

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_KEY)
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY)
}

export function isLoggedIn(): boolean {
  return !!getRefreshToken()
}

export function setTokens(token: TokenResponse): void {
  localStorage.setItem(ACCESS_KEY, token.accessToken)
  localStorage.setItem(REFRESH_KEY, token.refreshToken)
}

export function clearTokens(): void {
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
  authState.user = null
}

export async function login(username: string, password: string, deviceId = 'web'): Promise<void> {
  const res = await request.post<{ data: TokenResponse }>('/auth/login', { username, password, deviceId })
  setTokens(res.data.data)
}

export async function fetchCurrentUser(): Promise<LoginUser> {
  const res = await request.get<{ data: LoginUser }>('/auth/info')
  authState.user = res.data.data
  return authState.user
}

export async function logout(): Promise<void> {
  const refreshToken = getRefreshToken()
  try {
    if (refreshToken) {
      await request.post('/auth/logout', { refreshToken })
    }
  } finally {
    clearTokens()
  }
}

/** 供 request 拦截器在 401 时调用:用 refresh token 换新双 token。 */
export async function refreshTokens(): Promise<TokenResponse> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new Error('无 refresh token')
  }
  const res = await request.post<{ data: TokenResponse }>('/auth/refresh', { refreshToken })
  setTokens(res.data.data)
  return res.data.data
}
