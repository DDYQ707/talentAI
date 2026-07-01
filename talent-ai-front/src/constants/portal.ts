import type { PortalRole } from '@/stores/auth'

export type PortalRoleItem = {
  id: PortalRole
  label: string
  optionLabel: string
  path: string
}

/** 登录门户列表（与后端 userType 1-4 对应） */
export const PORTAL_ROLES: PortalRoleItem[] = [
  { id: 'hr', label: 'HR', optionLabel: 'HR（企业招聘）', path: '/hr' },
  { id: 'candidate', label: '求职者', optionLabel: '求职者（候选人端）', path: '/candidate' },
  { id: 'interviewer', label: '面试官', optionLabel: '面试官（评估端）', path: '/interviewer/workbench' },
  { id: 'admin', label: '管理员', optionLabel: '系统管理员', path: '/admin/permissions' },
]

/** 仅求职者可自助注册 */
export const SELF_REGISTER_ROLE: PortalRole = 'candidate'

export function roleOptionLabel(id: PortalRole): string {
  return PORTAL_ROLES.find((r) => r.id === id)?.optionLabel ?? id
}

export function rolePath(id: PortalRole): string {
  return PORTAL_ROLES.find((r) => r.id === id)?.path ?? '/login'
}
