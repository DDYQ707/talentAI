<script setup lang="ts">
import { ref, computed, useId } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { storeToRefs } from 'pinia'
import { ChevronDown, Eye, EyeOff } from 'lucide-vue-next'
import { useAuthStore, type PortalRole } from '@/stores/auth'

const sage = '#85A185'
const inputGradClass =
  'w-full rounded-xl border border-[#c5d8d4]/80 bg-gradient-to-b from-white/80 to-[#F2FAF8] px-4 py-3.5 text-[14px] text-[#1a1a1a] placeholder:text-[#8a9a96]/70 outline-none transition-all duration-200 focus:border-[#85A185] focus:ring-2 focus:ring-[#85A185]/20 focus:bg-white'

type RoleItem = { id: PortalRole; label: string; optionLabel: string }

const roles: RoleItem[] = [
  { id: 'hr', label: 'HR', optionLabel: 'HR（企业招聘）' },
  { id: 'candidate', label: '求职者', optionLabel: '求职者（候选人端）' },
  { id: 'interviewer', label: '面试官', optionLabel: '面试官（评估端）' },
  { id: 'admin', label: '管理员', optionLabel: '系统管理员' },
]

const formId = useId()
const roleFieldId = `reg-role-${formId}`
const accountFieldId = `reg-account-${formId}`
const passwordFieldId = `reg-password-${formId}`
const confirmFieldId = `reg-confirm-${formId}`

const router = useRouter()
const auth = useAuthStore()
const { selectedRole } = storeToRefs(auth)

const roleSelectModel = computed({
  get: (): PortalRole => selectedRole.value,
  set: (v: PortalRole) => auth.setSelectedRole(v),
})

const accountInput = ref('')
const passwordInput = ref('')
const confirmPasswordInput = ref('')
const showPassword = ref(false)
const showConfirm = ref(false)
const formError = ref('')

function roleOptionLabel(id: PortalRole) {
  return roles.find((r) => r.id === id)?.optionLabel ?? id
}

function handleRegister() {
  formError.value = ''
  if (!accountInput.value.trim()) {
    formError.value = '请填写账号或邮箱'
    return
  }
  if (passwordInput.value.length < 6) {
    formError.value = '密码至少 6 位'
    return
  }
  if (passwordInput.value !== confirmPasswordInput.value) {
    formError.value = '两次输入的密码不一致'
    return
  }
  router.push({
    path: '/login',
    query: { account: accountInput.value.trim() },
  })
}
</script>

<template>
  <div
    data-cmp="Register"
    class="login-page relative flex min-h-[100dvh] w-full flex-col bg-gradient-to-br from-[#E9F5F3] to-[#F8F1E7] antialiased"
  >
    <header class="relative z-40 shrink-0">
      <div class="relative mx-auto w-full max-w-[min(1520px,calc(100vw-0.5rem))] px-2 sm:px-3 lg:px-4">
        <div class="relative flex min-h-[4rem] items-center justify-between pt-4 pb-2 sm:min-h-[4.5rem] sm:pt-5 sm:pb-3">
          <RouterLink to="/login" class="flex shrink-0 items-baseline text-xl font-bold tracking-tight sm:text-2xl">
            <span class="bg-gradient-to-r from-[#2a2a2a] to-[#5a8a82] bg-clip-text text-transparent">Talent</span>
            <span class="text-[#5a8a82]">AI</span>
          </RouterLink>
          <RouterLink
            to="/login"
            class="text-[13px] font-medium text-[#5a8a82] underline-offset-4 transition-colors hover:text-[#3d8b7a] hover:underline"
          >
            返回登录
          </RouterLink>
        </div>
      </div>
    </header>

    <main class="relative z-10 flex flex-1 flex-col items-center justify-center px-2 py-6 sm:px-3 lg:px-4">
      <div
        class="w-full max-w-md rounded-3xl border border-white/50 bg-gradient-to-b from-[#FFFCF8] via-[#FFF8EC] to-[#F8E4B8] p-6 shadow-2xl backdrop-blur-sm sm:p-8"
      >
        <div class="mb-2 flex items-center gap-2">
          <div class="h-8 w-1 rounded-full bg-[#85A185]"></div>
          <h1 class="text-2xl font-bold tracking-tight text-[#1a1a1a]">创建账号</h1>
        </div>
        <p class="mb-6 text-[13px] text-[#6f6f6f] sm:text-[14px]">填写信息完成注册，演示环境提交后将跳转至登录页</p>

        <form class="space-y-5" @submit.prevent="handleRegister">
          <div>
            <label :for="roleFieldId" class="mb-2 block text-[12px] font-medium text-[#3a3a3a]">注册门户类型</label>
            <div class="relative">
              <select
                :id="roleFieldId"
                v-model="roleSelectModel"
                :class="[inputGradClass, 'cursor-pointer appearance-none pr-10']"
              >
                <option v-for="r in roles" :key="r.id" :value="r.id">{{ roleOptionLabel(r.id) }}</option>
              </select>
              <ChevronDown
                :size="18"
                class="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-[#888]"
                aria-hidden="true"
              />
            </div>
          </div>

          <div>
            <label :for="accountFieldId" class="mb-2 block text-[12px] font-medium text-[#3a3a3a]">账号 / 邮箱</label>
            <input
              :id="accountFieldId"
              v-model="accountInput"
              type="text"
              name="account"
              autocomplete="username"
              :class="inputGradClass"
              placeholder="请输入用户名或邮箱"
            />
          </div>

          <div>
            <label :for="passwordFieldId" class="mb-2 block text-[12px] font-medium text-[#3a3a3a]">密码</label>
            <div class="relative">
              <input
                :id="passwordFieldId"
                v-model="passwordInput"
                :type="showPassword ? 'text' : 'password'"
                name="new-password"
                autocomplete="new-password"
                :class="[inputGradClass, 'pr-12']"
                placeholder="至少 6 位"
              />
              <button
                type="button"
                class="absolute right-2 top-1/2 -translate-y-1/2 rounded-lg p-2 text-[#6f6f6f] transition-colors hover:bg-black/5 hover:text-[#1a1a1a]"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                @click="showPassword = !showPassword"
              >
                <EyeOff v-if="showPassword" :size="18" />
                <Eye v-else :size="18" />
              </button>
            </div>
          </div>

          <div>
            <label :for="confirmFieldId" class="mb-2 block text-[12px] font-medium text-[#3a3a3a]">确认密码</label>
            <div class="relative">
              <input
                :id="confirmFieldId"
                v-model="confirmPasswordInput"
                :type="showConfirm ? 'text' : 'password'"
                name="confirm-password"
                autocomplete="new-password"
                :class="[inputGradClass, 'pr-12']"
                placeholder="再次输入密码"
              />
              <button
                type="button"
                class="absolute right-2 top-1/2 -translate-y-1/2 rounded-lg p-2 text-[#6f6f6f] transition-colors hover:bg-black/5 hover:text-[#1a1a1a]"
                :aria-label="showConfirm ? '隐藏确认密码' : '显示确认密码'"
                @click="showConfirm = !showConfirm"
              >
                <EyeOff v-if="showConfirm" :size="18" />
                <Eye v-else :size="18" />
              </button>
            </div>
          </div>

          <p v-if="formError" class="text-[13px] font-medium text-red-600" role="alert">{{ formError }}</p>

          <button
            type="submit"
            class="group relative mt-2 w-full overflow-hidden rounded-xl py-3.5 text-[14px] font-bold text-white shadow-lg transition-all duration-300 hover:-translate-y-0.5 hover:shadow-xl active:translate-y-0 sm:text-[15px]"
            :style="{
              backgroundColor: sage,
              boxShadow: `0 8px 20px -6px ${sage}`,
            }"
          >
            <span class="relative z-10">注册并前往登录</span>
            <div
              class="absolute inset-0 -translate-x-full bg-gradient-to-r from-transparent via-white/20 to-transparent transition-transform duration-500 group-hover:translate-x-full"
            ></div>
          </button>
        </form>

        <p class="mt-6 text-center text-[13px] text-[#6f6f6f]">
          已有账号？
          <RouterLink to="/login" class="font-semibold text-[#5a8a82] underline-offset-2 hover:underline">去登录</RouterLink>
        </p>
      </div>
    </main>

    <footer class="relative z-10 mx-auto w-full max-w-[min(1520px,calc(100vw-0.5rem))] shrink-0 px-2 py-5 text-center sm:px-3 lg:px-4">
      <p class="text-[11px] text-[#a8a8a8]/80 sm:text-[12px]">
        Copyright © 2026 TalentAI, 智能招聘实训平台. All rights reserved.
      </p>
    </footer>
  </div>
</template>

<style>
  html:has(.login-page),
  body:has(.login-page) {
    margin: 0;
    padding: 0;
    min-height: 100%;
    background-color: #e9f5f3;
  }

  html:has(.login-page--fixed-viewport),
  body:has(.login-page--fixed-viewport) {
    height: 100%;
    overflow: hidden;
  }

  html:has(.login-page:not(.login-page--fixed-viewport)),
  body:has(.login-page:not(.login-page--fixed-viewport)) {
    overflow-x: hidden;
    overflow-y: auto;
  }

  #app:has(.login-page--fixed-viewport) {
    margin: 0;
    padding: 0;
    width: 100%;
    height: 100vh;
    height: 100dvh;
    overflow: hidden;
  }

  #app:has(.login-page:not(.login-page--fixed-viewport)) {
    margin: 0;
    padding: 0;
    width: 100%;
    min-height: 100dvh;
  }

  .login-page {
    overscroll-behavior: none;
  }
</style>
