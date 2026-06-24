import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { LoginUserInfo } from '@/api/auth'
import { rolePath } from '@/constants/portal'

/** 登录可选角色：企业端（HR/管理员）与候选人端（求职者/面试官） */
export type PortalRole = 'hr' | 'admin' | 'candidate' | 'interviewer'

const USER_TYPE_TO_ROLE: Record<number, PortalRole> = {
  1: 'candidate',
  2: 'hr',
  3: 'interviewer',
  4: 'admin',
}

const ROLE_TO_USER_TYPE: Record<PortalRole, number> = {
  candidate: 1,
  hr: 2,
  interviewer: 3,
  admin: 4,
}

export const useAuthStore = defineStore('auth', () => {
  const selectedRole = ref<PortalRole>('hr')
  const showPassword = ref(false)
  const token = ref<string | null>(localStorage.getItem('talent_token'))
  const userInfo = ref<LoginUserInfo | null>(loadStoredUserInfo())

  function setSelectedRole(role: PortalRole) {
    selectedRole.value = role
  }

  function toggleShowPassword() {
    showPassword.value = !showPassword.value
  }

  function portalRoleFromUserType(userType: number): PortalRole | null {
    return USER_TYPE_TO_ROLE[userType] ?? null
  }

  function userTypeFromPortalRole(role: PortalRole): number {
    return ROLE_TO_USER_TYPE[role]
  }

  function pathForRole(role: PortalRole): string {
    return rolePath(role)
  }

  function setSession(newToken: string, info: LoginUserInfo) {
    token.value = newToken
    userInfo.value = info
    localStorage.setItem('talent_token', newToken)
    localStorage.setItem('talent_user', JSON.stringify(info))
  }

  function logout() {
    token.value = null
    userInfo.value = null
    localStorage.removeItem('talent_token')
    localStorage.removeItem('talent_user')
  }

  function isLoggedIn(): boolean {
    return Boolean(token.value)
  }

  return {
    selectedRole,
    showPassword,
    token,
    userInfo,
    setSelectedRole,
    toggleShowPassword,
    portalRoleFromUserType,
    userTypeFromPortalRole,
    pathForRole,
    setSession,
    logout,
    isLoggedIn,
  }
})

function loadStoredUserInfo(): LoginUserInfo | null {
  try {
    const raw = localStorage.getItem('talent_user')
    if (!raw) return null
    return JSON.parse(raw) as LoginUserInfo
  } catch {
    return null
  }
}
