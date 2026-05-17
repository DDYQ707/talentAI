import { defineStore } from 'pinia'
import { ref } from 'vue'

/** 登录可选角色：企业端（HR/管理员）与候选人端（求职者/面试官） */
export type PortalRole = 'hr' | 'admin' | 'candidate' | 'interviewer'

export const useAuthStore = defineStore('auth', () => {
  const selectedRole = ref<PortalRole>('hr')
  const showPassword = ref(false)

  function setSelectedRole(role: PortalRole) {
    selectedRole.value = role
  }

  function toggleShowPassword() {
    showPassword.value = !showPassword.value
  }

  return { selectedRole, showPassword, setSelectedRole, toggleShowPassword }
})
