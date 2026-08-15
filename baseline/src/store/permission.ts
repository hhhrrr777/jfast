import { reactive } from 'vue'
import request from '@/utils/request'
import type { LoginUser } from '@/api/auth'

/**
 * 权限域 store:登录用户、角色、权限标识集合与动态路由数据。
 * 权限集合来自 /auth/info(登录时拉取),v-hasPermi 与路由构建消费。
 */

export interface RouterNode {
  name: string
  path: string
  component: string
  title: string
  icon: string
  children: RouterNode[]
}

interface PermissionState {
  user: LoginUser | null
  roles: string[]
  permissions: string[]
  routers: RouterNode[]
  loaded: boolean
}

export const permissionState = reactive<PermissionState>({
  user: null,
  roles: [],
  permissions: [],
  routers: [],
  loaded: false
})

/** 超管全量标识。 */
const ALL_PERMISSION = '*:*:*'

export function isSuperAdmin(): boolean {
  return permissionState.permissions.includes(ALL_PERMISSION)
}

export function hasPermi(perms: string | string[]): boolean {
  if (permissionState.permissions.includes(ALL_PERMISSION)) return true
  const list = Array.isArray(perms) ? perms : [perms]
  return list.some((p) => permissionState.permissions.includes(p))
}

/** 拉取当前用户信息(含 roles/permissions)与动态路由,登录守卫与初始化时调用。 */
export async function loadPermissions(user: LoginUser): Promise<void> {
  permissionState.user = user
  permissionState.roles = user.roles ?? []
  permissionState.permissions = user.permissions ?? []
  const res = await request.get<{ data: RouterNode[] }>('/auth/routers')
  permissionState.routers = res.data.data
  permissionState.loaded = true
}

export function resetPermissions(): void {
  permissionState.user = null
  permissionState.roles = []
  permissionState.permissions = []
  permissionState.routers = []
  permissionState.loaded = false
}
