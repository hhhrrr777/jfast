import { reactive } from 'vue'
import request from '@/utils/request'
import {
  clearStoredTokens,
  getAccessToken,
  getRefreshToken,
  saveTokens
} from '@/utils/tokenStorage'

/**
 * 认证状态与令牌操作。令牌读写走 utils/tokenStorage;
 * 供 request 拦截器与路由守卫使用。
 */

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
  roles?: string[]
  permissions?: string[]
}

export const authState = reactive<{ user: LoginUser | null }>({
  user: null
})

export { getAccessToken, getRefreshToken }

/**
 * 路由守卫用的轻量登录态判断:本地是否持有 refresh token。
 * 注意:仅判断存在性,不校验有效期——过期 token 会在首次接口调用时经 401 刷新或跳登录兜底。
 */
export function hasRefreshToken(): boolean {
  return !!getRefreshToken()
}

export function setTokens(token: TokenResponse): void {
  saveTokens(token.accessToken, token.refreshToken)
}

export function clearTokens(): void {
  clearStoredTokens()
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
