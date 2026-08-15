import request from '@/utils/request'
import type { ApiResult, TableData } from '@/types/common'

/**
 * 系统管理 API:用户/角色/菜单三模块。
 */

// ---- 用户 ----

export interface UserVO {
  userId: number
  userName: string
  nickName: string
  status: string
  loginIp: string
  loginDate: string | null
  createTime: string | null
  remark: string | null
  roleIds: number[] | null
}

export interface UserQuery {
  pageNum: number
  pageSize: number
  userName?: string
  nickName?: string
  status?: string
}

export interface UserCreateForm {
  userName: string
  nickName: string
  password: string
  status: string
  remark?: string
  roleIds: number[]
}

export interface UserUpdateForm {
  userId: number
  nickName: string
  status: string
  remark?: string
  roleIds: number[]
}

export function listUsers(query: UserQuery) {
  return request.get<TableData<UserVO>>('/system/user/list', { params: query })
}

export function getUser(userId: number) {
  return request.get<ApiResult<UserVO>>(`/system/user/${userId}`)
}

export function addUser(data: UserCreateForm) {
  return request.post<ApiResult<UserVO>>('/system/user', data)
}

export function updateUser(data: UserUpdateForm) {
  return request.put<ApiResult<UserVO>>('/system/user', data)
}

export function deleteUser(userIds: number[]) {
  return request.delete<ApiResult<null>>(`/system/user/${userIds.join(',')}`)
}

export function resetPassword(userId: number, password: string) {
  return request.put<ApiResult<null>>('/system/user/resetPwd', { userId, password })
}

export function changeMyPassword(oldPassword: string, newPassword: string) {
  return request.put<ApiResult<null>>('/system/user/profile/password', { oldPassword, newPassword })
}

// ---- 角色 ----

export interface RoleVO {
  roleId: number
  roleName: string
  roleKey: string
  roleSort: number
  status: string
  createTime: string | null
  remark: string | null
  menuIds: number[] | null
}

export interface RoleQuery {
  pageNum: number
  pageSize: number
  roleName?: string
  roleKey?: string
  status?: string
}

export interface RoleSaveForm {
  roleId?: number
  roleName: string
  roleKey: string
  roleSort: number
  status: string
  remark?: string
  menuIds: number[]
}

export function listRoles(query: RoleQuery) {
  return request.get<TableData<RoleVO>>('/system/role/list', { params: query })
}

export function listAllRoles() {
  return request.get<ApiResult<RoleVO[]>>('/system/role/all')
}

export function getRole(roleId: number) {
  return request.get<ApiResult<RoleVO>>(`/system/role/${roleId}`)
}

export function addRole(data: RoleSaveForm) {
  return request.post<ApiResult<RoleVO>>('/system/role', data)
}

export function updateRole(data: RoleSaveForm) {
  return request.put<ApiResult<RoleVO>>('/system/role', data)
}

export function deleteRole(roleIds: number[]) {
  return request.delete<ApiResult<null>>(`/system/role/${roleIds.join(',')}`)
}

// ---- 菜单 ----

export interface MenuTreeNode {
  menuId: number
  menuName: string
  parentId: number
  orderNum: number
  path: string
  component: string
  menuType: 'M' | 'C' | 'F'
  visible: string
  status: string
  perms: string
  icon: string
  createTime: string | null
  remark: string | null
  children: MenuTreeNode[]
}

export interface MenuSaveForm {
  menuId?: number
  menuName: string
  parentId: number
  orderNum: number
  path?: string
  component?: string
  menuType: 'M' | 'C' | 'F'
  visible: string
  status: string
  perms?: string
  icon?: string
  remark?: string
}

export function getMenuTree() {
  return request.get<ApiResult<MenuTreeNode[]>>('/system/menu/tree')
}

export function getMenu(menuId: number) {
  return request.get<ApiResult<MenuTreeNode>>(`/system/menu/${menuId}`)
}

export function addMenu(data: MenuSaveForm) {
  return request.post<ApiResult<MenuTreeNode>>('/system/menu', data)
}

export function updateMenu(data: MenuSaveForm) {
  return request.put<ApiResult<MenuTreeNode>>('/system/menu', data)
}

export function deleteMenu(menuId: number) {
  return request.delete<ApiResult<null>>(`/system/menu/${menuId}`)
}
