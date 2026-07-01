import request from '@/utils/request'

/* ---- 类型定义 ---- */

export interface RoleItem {
  id: number
  roleCode: string
  roleName: string
  description?: string
  status: number
  userCount: number
  permCount: number
}

export interface PermItem {
  id: number
  permCode: string
  permName: string
}

export interface PermModule {
  module: string
  perms: PermItem[]
}

export interface RoleSavePayload {
  roleCode: string
  roleName: string
  description?: string
}

/* ---- 角色 API ---- */

/** 角色列表（含用户数、权限数） */
export function listRoles() {
  return request.get<RoleItem[]>('/api/admin/permissions/roles') as Promise<RoleItem[]>
}

/** 权限项（按模块分组） */
export function listPermTree() {
  return request.get<PermModule[]>('/api/admin/permissions/tree') as Promise<PermModule[]>
}

/** 某角色已有的权限ID列表 */
export function getRolePermissions(roleId: number) {
  return request.get<number[]>(`/api/admin/permissions/role/${roleId}`) as Promise<number[]>
}

/** 保存角色权限 */
export function saveRolePermissions(roleId: number, permissionIds: number[]) {
  return request.put<null>(`/api/admin/permissions/role/${roleId}`, { permissionIds }) as Promise<null>
}

/** 新建角色 */
export function createRole(data: RoleSavePayload) {
  return request.post<RoleItem>('/api/admin/permissions/roles', data) as Promise<RoleItem>
}

/** 编辑角色 */
export function updateRole(id: number, data: RoleSavePayload) {
  return request.put<null>(`/api/admin/permissions/roles/${id}`, data) as Promise<null>
}

/** 删除角色（逻辑删，同时清理权限关联） */
export function deleteRole(id: number) {
  return request.delete<null>(`/api/admin/permissions/roles/${id}`) as Promise<null>
}