<script setup lang="ts">
import { ref, onMounted, useId } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { Eye, EyeOff } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { register as registerApi } from '@/api/auth'
import { SELF_REGISTER_ROLE } from '@/constants/portal'
import { AUTH_INPUT_CLASS, AUTH_SAGE } from '@/constants/auth-ui'
import {
  getErrorMessage,
  validateAccount,
  validatePassword,
  validatePasswordConfirm,
} from '@/utils/validators'

const sage = AUTH_SAGE
const inputGradClass = AUTH_INPUT_CLASS

const formId = useId()
const accountFieldId = `reg-account-${formId}`
const passwordFieldId = `reg-password-${formId}`
const confirmFieldId = `reg-confirm-${formId}`

const router = useRouter()
const auth = useAuthStore()

const accountInput = ref('')
const passwordInput = ref('')
const confirmPasswordInput = ref('')
const showPassword = ref(false)
const showConfirm = ref(false)
const formError = ref('')
const submitting = ref(false)

onMounted(() => {
  auth.setSelectedRole(SELF_REGISTER_ROLE)
})

async function handleRegister() {
  formError.value = ''
  const account = accountInput.value.trim()
  const accountErr = validateAccount(account)
  if (accountErr) {
    formError.value = accountErr
    return
  }
  const pwdErr = validatePassword(passwordInput.value)
  if (pwdErr) {
    formError.value = pwdErr
    return
  }
  const confirmErr = validatePasswordConfirm(passwordInput.value, confirmPasswordInput.value)
  if (confirmErr) {
    formError.value = confirmErr
    return
  }

  submitting.value = true
  try {
    await registerApi(account, passwordInput.value)
    router.push({
      path: '/login',
      query: { account, registered: '1' },
    })
  } catch (e) {
    formError.value = getErrorMessage(e, '注册失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <!-- eslint-disable vue/no-multiple-template-root -->
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
          <div class="h-8 w-1 rounded-full bg-[#85A185]" />
          <div>
            <h1 class="text-2xl font-bold tracking-tight text-[#1a1a1a]">创建求职者账号</h1>
            <p class="mt-1 text-[13px] text-[#6f6f6f] sm:text-[14px]">注册成功后将跳转登录页，请使用求职者门户登录</p>
          </div>
        </div>

        <p
          class="mb-5 inline-flex items-center gap-2 rounded-full bg-[#85A185]/12 px-3 py-1 text-[12px] font-medium text-[#3d8b7a]"
        >
          求职者（候选人端）
        </p>

        <form class="space-y-5" @submit.prevent="handleRegister">
          <div>
            <label :for="accountFieldId" class="mb-2 block text-[12px] font-medium text-[#3a3a3a]">手机号 / 邮箱</label>
            <input
              :id="accountFieldId"
              v-model="accountInput"
              type="text"
              name="account"
              autocomplete="username"
              :class="inputGradClass"
              placeholder="请输入手机号或邮箱"
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
            class="group relative mt-2 w-full overflow-hidden rounded-xl py-3.5 text-[14px] font-bold text-white shadow-lg transition-all duration-300 hover:-translate-y-0.5 hover:shadow-xl active:translate-y-0 disabled:cursor-not-allowed disabled:opacity-70 disabled:hover:translate-y-0 sm:text-[15px]"
            :disabled="submitting"
            :style="{
              backgroundColor: sage,
              boxShadow: `0 8px 20px -6px ${sage}`,
            }"
          >
            <span class="relative z-10">{{ submitting ? '注册中…' : '注册并前往登录' }}</span>
            <div
              class="absolute inset-0 -translate-x-full bg-gradient-to-r from-transparent via-white/20 to-transparent transition-transform duration-500 group-hover:translate-x-full"
            />
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

  #app:has(.login-page) {
    margin: 0;
    padding: 0;
    width: 100%;
    min-height: 100dvh;
  }

  .login-page {
    overscroll-behavior: none;
  }
</style>
