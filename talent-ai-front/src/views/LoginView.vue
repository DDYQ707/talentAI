<script setup lang="ts">
import { ref, computed, useId, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { ChevronDown, Brain, Calendar, FileText, Users, Repeat2, Sparkles, Zap, Shield, Eye, EyeOff } from 'lucide-vue-next'
import { useAuthStore, type PortalRole } from '@/stores/auth'
import { loginByPassword, loginByOtp, register as registerApi, sendLoginOtp } from '@/api/auth'
import { PORTAL_ROLES, SELF_REGISTER_ROLE, roleOptionLabel } from '@/constants/portal'
import { AUTH_INPUT_CLASS, AUTH_SAGE, ENTERPRISE_ACCOUNT_HINT } from '@/constants/auth-ui'
import {
  getErrorMessage,
  isPhoneOrEmail,
  validateAccount,
  validateLoginAccount,
  validatePassword,
  validatePasswordConfirm,
} from '@/utils/validators'
import LoginHeroArt from '@/components/login/LoginHeroArt.vue'

const sage = AUTH_SAGE
const inputGradClass = AUTH_INPUT_CLASS

// 更新features列表，使其更丰富
const features = [
  { label: '智能匹配', icon: Brain, desc: 'AI精准推荐' },
  { label: '流程自动化', icon: Calendar, desc: '高效流转' },
  { label: '人才分析', icon: FileText, desc: '深度洞察' },
  { label: '团队协同', icon: Users, desc: '无缝协作' },
  { label: '循环优化', icon: Repeat2, desc: '持续迭代' },
]

const formId = useId()
const roleFieldId = `login-role-${formId}`
const accountFieldId = `login-account-${formId}`
const passwordFieldId = `login-password-${formId}`
const otpAccountFieldId = `login-otp-account-${formId}`
const otpCodeFieldId = `login-otp-code-${formId}`
const regAccountFieldId = `reg-ac-${formId}`
const regPasswordFieldId = `reg-pw-${formId}`
const regConfirmFieldId = `reg-cf-${formId}`

// 统一统计数据，展示更全面
const statsList = [
  { num: '500+', label: '数据源数', icon: Zap },
  { num: '200+', label: '人才源数', icon: Users },
  { num: '300+', label: '人才基数', icon: Sparkles },
  { num: '100+', label: '支援辅助', icon: Shield },
]

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const { selectedRole } = storeToRefs(auth)

const roleSelectModel = computed({
  get: (): PortalRole => selectedRole.value,
  set: (v: PortalRole) => auth.setSelectedRole(v),
})

const authPanel = ref<'login' | 'register'>('login')
const authCardRef = ref<HTMLElement | null>(null)

type LoginMethod = 'password' | 'otp'

const loginMethod = ref<LoginMethod>('password')
const accountInput = ref('')
const passwordInput = ref('')
const showPassword = ref(false)

const otpAccountInput = ref('')
const otpCodeInput = ref('')
const otpCountdown = ref(0)
const otpSending = ref(false)
const otpDevHint = ref('')
let otpTimer: ReturnType<typeof setInterval> | null = null

const OTP_COUNTDOWN_SEC = 60

const registerAccountInput = ref('')
const registerPasswordInput = ref('')
const registerConfirmInput = ref('')
const regShowPwd = ref(false)
const regShowConfirm = ref(false)
const registerFormError = ref('')
const loginFormError = ref('')
const loginSubmitting = ref(false)
const registerSubmitting = ref(false)
const registerFormSuccess = ref('')
const portalMismatchRole = ref<PortalRole | null>(null)

const PUBLIC_REDIRECT_PATHS = new Set(['/login', '/register'])

const canSelfRegister = computed(() => selectedRole.value === SELF_REGISTER_ROLE)

const otpSendDisabled = computed(
  () => otpCountdown.value > 0 || otpSending.value || !isPhoneOrEmail(otpAccountInput.value),
)

onMounted(() => {
  const q = route.query.account
  if (typeof q === 'string' && q.trim()) {
    const v = q.trim()
    accountInput.value = v
    otpAccountInput.value = v
  }
  if (route.query.registered === '1') {
    auth.setSelectedRole(SELF_REGISTER_ROLE)
    registerFormSuccess.value = '注册成功，请使用求职者门户登录'
  }
})

onUnmounted(() => {
  if (otpTimer) clearInterval(otpTimer)
})

function startOtpCountdown() {
  otpCountdown.value = OTP_COUNTDOWN_SEC
  if (otpTimer) clearInterval(otpTimer)
  otpTimer = setInterval(() => {
    otpCountdown.value -= 1
    if (otpCountdown.value <= 0 && otpTimer) {
      clearInterval(otpTimer)
      otpTimer = null
    }
  }, 1000)
}

async function handleSendOtp() {
  loginFormError.value = ''
  otpDevHint.value = ''
  if (otpCountdown.value > 0 || otpSending.value) return
  if (!isPhoneOrEmail(otpAccountInput.value)) {
    loginFormError.value = '请输入正确的手机号或邮箱'
    return
  }
  otpSending.value = true
  try {
    const res = await sendLoginOtp(otpAccountInput.value)
    startOtpCountdown()
    if (res.devCode) {
      otpDevHint.value = `开发环境验证码：${res.devCode}（${res.expireSeconds ?? 300} 秒内有效）`
    }
  } catch (e) {
    loginFormError.value = getErrorMessage(e, '验证码发送失败，请稍后重试')
  } finally {
    otpSending.value = false
  }
}

async function completeLogin(res: Awaited<ReturnType<typeof loginByPassword>>) {
  if (!res.token || !res.userInfo) {
    loginFormError.value = res.msg || '登录失败，请重试'
    return
  }

  const accountRole = auth.portalRoleFromUserType(res.userInfo.userType)
  if (!accountRole) {
    loginFormError.value = '账号类型未知，请联系管理员'
    return
  }
  if (accountRole !== selectedRole.value) {
    portalMismatchRole.value = accountRole
    loginFormError.value = `该账号属于「${roleOptionLabel(accountRole)}」，与当前门户不一致`
    return
  }

  auth.setSession(res.token, res.userInfo)
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect.trim() : ''
  if (redirect && redirect.startsWith('/') && !PUBLIC_REDIRECT_PATHS.has(redirect.split('?')[0])) {
    await router.push(redirect)
  } else {
    await router.push(auth.pathForRole(accountRole))
  }
}

async function openRegisterPanel() {
  auth.setSelectedRole(SELF_REGISTER_ROLE)
  registerFormError.value = ''
  authPanel.value = 'register'
  await nextTick()
  if (typeof window !== 'undefined' && window.matchMedia('(max-width: 1023px)').matches) {
    authCardRef.value?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }
}

function closeRegisterPanel() {
  authPanel.value = 'login'
  registerFormError.value = ''
}

function applyPortalMismatch() {
  if (!portalMismatchRole.value) return
  auth.setSelectedRole(portalMismatchRole.value)
  portalMismatchRole.value = null
  loginFormError.value = ''
  registerFormSuccess.value = `已切换为「${roleOptionLabel(selectedRole.value)}」，请再次登录`
}

async function handleRegisterSubmit() {
  registerFormError.value = ''
  registerFormSuccess.value = ''
  const account = registerAccountInput.value.trim()
  const accountErr = validateAccount(account)
  if (accountErr) {
    registerFormError.value = accountErr
    return
  }
  const pwdErr = validatePassword(registerPasswordInput.value)
  if (pwdErr) {
    registerFormError.value = pwdErr
    return
  }
  const confirmErr = validatePasswordConfirm(registerPasswordInput.value, registerConfirmInput.value)
  if (confirmErr) {
    registerFormError.value = confirmErr
    return
  }

  registerSubmitting.value = true
  try {
    await registerApi(account, registerPasswordInput.value)
    accountInput.value = account
    passwordInput.value = registerPasswordInput.value
    registerAccountInput.value = ''
    registerPasswordInput.value = ''
    registerConfirmInput.value = ''
    regShowPwd.value = false
    regShowConfirm.value = false
    authPanel.value = 'login'
    registerFormError.value = ''
    auth.setSelectedRole(SELF_REGISTER_ROLE)
    registerFormSuccess.value = '注册成功，请使用求职者门户登录'
  } catch (e) {
    registerFormError.value = getErrorMessage(e, '注册失败，请稍后重试')
  } finally {
    registerSubmitting.value = false
  }
}

async function handleLogin() {
  loginFormError.value = ''
  portalMismatchRole.value = null
  if (loginMethod.value === 'otp') {
    if (!isPhoneOrEmail(otpAccountInput.value)) {
      loginFormError.value = '请输入正确的手机号或邮箱'
      return
    }
    if (!/^\d{6}$/.test(otpCodeInput.value.trim())) {
      loginFormError.value = '请输入 6 位数字验证码'
      return
    }
    loginSubmitting.value = true
    try {
      const res = await loginByOtp(otpAccountInput.value, otpCodeInput.value)
      await completeLogin(res)
    } catch (e) {
      loginFormError.value = getErrorMessage(e, '登录失败，请检查验证码或稍后重试')
    } finally {
      loginSubmitting.value = false
    }
    return
  }

  const accountErr = validateLoginAccount(accountInput.value)
  if (accountErr) {
    loginFormError.value = accountErr
    return
  }
  const pwdErr = validatePassword(passwordInput.value)
  if (pwdErr) {
    loginFormError.value = pwdErr
    return
  }

  loginSubmitting.value = true
  try {
    const res = await loginByPassword(accountInput.value, passwordInput.value)
    await completeLogin(res)
  } catch (e) {
    loginFormError.value = getErrorMessage(e, '登录失败，请检查网络或稍后重试')
  } finally {
    loginSubmitting.value = false
  }
}
</script>

<template>
  <div
    data-cmp="Login"
    class="login-page login-page--fixed-viewport relative flex h-[100dvh] w-full flex-col overflow-hidden bg-gradient-to-br from-[#E9F5F3] to-[#F8F1E7] antialiased"
  >
    <!-- 顶栏：品牌标识 -->
    <header class="relative z-40">
      <div class="relative mx-auto w-full max-w-[min(1520px,calc(100vw-0.5rem))] px-2 sm:px-3 lg:px-4">
        <div class="relative flex min-h-[4rem] items-center pt-4 pb-2 sm:min-h-[4.5rem] sm:pt-5 sm:pb-3">
          <div class="flex shrink-0 items-baseline text-xl font-bold tracking-tight sm:text-2xl">
            <span class="bg-gradient-to-r from-[#2a2a2a] to-[#5a8a82] bg-clip-text text-transparent">Talent</span>
            <span class="text-[#5a8a82]">AI</span>
          </div>
        </div>
      </div>
    </header>

    <!-- 主内容区域优化 -->
    <div class="flex min-h-0 flex-1 flex-col overflow-hidden">
      <main
        class="relative z-10 mx-auto flex min-h-0 w-full max-w-[min(1520px,calc(100vw-0.5rem))] flex-1 flex-col overflow-hidden px-2 pb-4 pt-2 sm:px-3 sm:pb-6 lg:px-4 lg:pt-8"
      >
        <div
          class="mx-auto flex min-h-0 w-full max-w-full flex-1 flex-col gap-8 lg:grid lg:grid-cols-[minmax(0,0.88fr)_minmax(0,1.14fr)] lg:items-center lg:gap-6 xl:gap-8"
        >
          <!-- 左侧内容优化 -->
          <div class="flex min-h-0 min-w-0 flex-col justify-center overflow-hidden pb-4 lg:pb-0">
            <!-- 标题区域优化 -->
            <div class="mb-6">
              <div class="inline-flex items-center gap-2 rounded-full bg-[#85A185]/10 px-3 py-1 text-xs font-medium text-[#3d8b7a]">
                <span class="relative flex h-2 w-2">
                  <span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-[#3d8b7a] opacity-75"></span>
                  <span class="relative inline-flex h-2 w-2 rounded-full bg-[#3d8b7a]"></span>
                </span>
                新一代智能招聘平台
              </div>
              <h1 class="mt-5 leading-tight">
                <span class="block text-[clamp(2.2rem,5vw,3.5rem)] font-bold tracking-tight text-[#3d8b7a]">TalentAI</span>
                <span class="mt-3 block text-[clamp(1.3rem,3vw,1.8rem)] font-semibold text-[#1a1a1a]">智能招聘与人才分析平台</span>
              </h1>
              <p class="mt-4 max-w-lg text-[15px] leading-relaxed text-[#6f6f6f] sm:text-[16px]">
                全流程驱动，人才分析等，让招聘决策更智能、更高效。
              </p>
            </div>

            <!-- Features 优化：增加描述文字，卡片感更强 -->
            <div class="mb-8 grid max-w-full grid-cols-5 gap-3 sm:gap-4">
              <div
                v-for="(f, fi) in features"
                :key="fi"
                class="group flex flex-col items-center gap-2 rounded-2xl p-2 transition-all duration-300 hover:-translate-y-1 hover:bg-white/40"
              >
                <div
                  class="flex h-12 w-12 items-center justify-center rounded-full bg-gradient-to-br from-[#d8d8d8] to-[#c0c0c0] text-[#454545] shadow-sm transition-all duration-300 group-hover:shadow-md"
                >
                  <component :is="f.icon" :size="22" stroke-width="1.5" />
                </div>
                <div class="text-center">
                  <span class="block text-[12px] font-semibold text-[#1a1a1a] sm:text-[13px]">{{ f.label }}</span>
                  <span class="hidden text-[10px] text-[#8a9a96] sm:block">{{ f.desc }}</span>
                </div>
              </div>
            </div>

            <!-- 右侧装饰图（仅移动端显示） -->
            <div class="mb-6 flex max-h-[180px] justify-center overflow-hidden opacity-90 lg:hidden">
              <div class="relative aspect-[4/3] w-[min(100%,260px)] shrink-0">
                <LoginHeroArt />
              </div>
            </div>

            <!-- 统一统计数据区域，更现代美观 -->
            <div class="grid grid-cols-2 gap-3 sm:grid-cols-4 sm:gap-4">
              <div
                v-for="s in statsList"
                :key="s.label"
                class="group rounded-2xl border border-white/50 bg-white/60 p-4 text-center shadow-sm backdrop-blur-sm transition-all duration-300 hover:-translate-y-1 hover:bg-white/80 hover:shadow-md"
              >
                <component :is="s.icon" class="mx-auto mb-2 h-5 w-5 text-[#5a8a82]" />
                <div class="text-2xl font-bold tracking-tight text-[#3d8b7a]">{{ s.num }}</div>
                <div class="mt-1 text-[11px] font-medium text-[#6f6f6f]">{{ s.label }}</div>
              </div>
            </div>
          </div>

          <!-- 登录 / 注册：横向滑轨，整页切换 -->
          <div class="mx-auto w-full min-w-0 shrink-0 lg:mx-0 lg:min-h-0">
            <div
              ref="authCardRef"
              class="relative min-h-0 overflow-hidden rounded-3xl border border-white/50 bg-gradient-to-b from-[#FFFCF8] via-[#FFF8EC] to-[#F8E4B8] shadow-2xl backdrop-blur-sm lg:min-h-[min(460px,60vh)]"
            >
              <div
                class="flex w-[200%] transition-transform duration-500 ease-[cubic-bezier(0.22,1,0.36,1)] will-change-transform"
                :class="authPanel === 'register' ? '-translate-x-1/2' : 'translate-x-0'"
              >
                <!-- 登录整页（左滑移出视野） -->
                <div
                  class="flex w-1/2 shrink-0 flex-col lg:min-h-[min(460px,60vh)] lg:flex-row lg:items-stretch"
                  :class="authPanel === 'login' ? 'pointer-events-auto' : 'pointer-events-none'"
                  :aria-hidden="authPanel === 'register'"
                >
                  <div class="min-w-0 shrink-0 p-6 sm:p-8 lg:flex lg:w-1/2 lg:flex-col lg:justify-center lg:px-10 xl:px-12">
                <div class="mb-2 flex items-center gap-2">
                  <div class="h-8 w-1 rounded-full bg-[#85A185]"></div>
                  <h2 class="text-2xl font-bold tracking-tight text-[#1a1a1a]">欢迎回来</h2>
                </div>
                <p class="mb-6 text-[13px] text-[#6f6f6f] sm:mb-8 sm:text-[14px]">
                  登录您的账户，开启智能招聘体验
                </p>

                <form class="space-y-5" @submit.prevent="handleLogin">
                  <div>
                    <label :for="roleFieldId" class="mb-2 block text-[12px] font-medium text-[#3a3a3a]">选择登录门户</label>
                    <div class="relative">
                      <select
                        :id="roleFieldId"
                        v-model="roleSelectModel"
                        :class="[inputGradClass, 'cursor-pointer appearance-none pr-10']"
                      >
                        <option v-for="r in PORTAL_ROLES" :key="r.id" :value="r.id">{{ r.optionLabel }}</option>
                      </select>
                      <ChevronDown
                        :size="18"
                        class="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-[#888]"
                        aria-hidden="true"
                      />
                    </div>
                  </div>

                  <div
                    class="flex rounded-xl border border-[#c5d8d4]/60 bg-white/50 p-1"
                    role="tablist"
                    aria-label="登录方式"
                  >
                    <button
                      type="button"
                      role="tab"
                      :aria-selected="loginMethod === 'password'"
                      class="flex-1 rounded-lg py-2 text-[13px] font-medium transition-all duration-200"
                      :class="
                        loginMethod === 'password'
                          ? 'bg-white text-[#1a1a1a] shadow-sm'
                          : 'text-[#6f6f6f] hover:text-[#3a3a3a]'
                      "
                      @click="loginMethod = 'password'; loginFormError = ''"
                    >
                      密码登录
                    </button>
                    <button
                      type="button"
                      role="tab"
                      :aria-selected="loginMethod === 'otp'"
                      class="flex-1 rounded-lg py-2 text-[13px] font-medium transition-all duration-200"
                      :class="
                        loginMethod === 'otp'
                          ? 'bg-white text-[#1a1a1a] shadow-sm'
                          : 'text-[#6f6f6f] hover:text-[#3a3a3a]'
                      "
                      @click="loginMethod = 'otp'; loginFormError = ''; otpDevHint = ''"
                    >
                      验证码登录
                    </button>
                  </div>

                  <div v-if="loginMethod === 'password'" class="space-y-4">
                    <div>
                      <label :for="accountFieldId" class="mb-2 block text-[12px] font-medium text-[#3a3a3a]">用户名 / 手机号 / 邮箱</label>
                      <input
                        :id="accountFieldId"
                        v-model="accountInput"
                        type="text"
                        name="account"
                        autocomplete="username"
                        :class="inputGradClass"
                        placeholder="如 admin、手机号或邮箱"
                      />
                    </div>
                    <div>
                      <label :for="passwordFieldId" class="mb-2 block text-[12px] font-medium text-[#3a3a3a]">密码</label>
                      <div class="relative">
                        <input
                          :id="passwordFieldId"
                          v-model="passwordInput"
                          :type="showPassword ? 'text' : 'password'"
                          name="password"
                          autocomplete="current-password"
                          :class="[inputGradClass, 'pr-12']"
                          placeholder="请输入密码"
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
                  </div>

                  <div v-else class="space-y-4">
                    <div>
                      <label :for="otpAccountFieldId" class="mb-2 block text-[12px] font-medium text-[#3a3a3a]">手机号 / 邮箱</label>
                      <input
                        :id="otpAccountFieldId"
                        v-model="otpAccountInput"
                        type="text"
                        name="otp-account"
                        autocomplete="username"
                        :class="inputGradClass"
                        placeholder="请输入手机号或邮箱"
                      />
                    </div>
                    <div>
                      <label :for="otpCodeFieldId" class="mb-2 block text-[12px] font-medium text-[#3a3a3a]">验证码</label>
                      <div class="flex gap-2">
                        <input
                          :id="otpCodeFieldId"
                          v-model="otpCodeInput"
                          type="text"
                          inputmode="numeric"
                          maxlength="6"
                          name="otp-code"
                          autocomplete="one-time-code"
                          :class="[inputGradClass, 'min-w-0 flex-1']"
                          placeholder="请输入验证码"
                        />
                        <button
                          type="button"
                          class="shrink-0 rounded-xl border border-[#85A185]/50 px-3 py-3.5 text-[12px] font-semibold text-[#5a8a82] transition-all duration-200 hover:border-[#85A185] hover:bg-[#85A185]/10 disabled:cursor-not-allowed disabled:opacity-50 sm:px-4 sm:text-[13px]"
                          :disabled="otpSendDisabled"
                          @click="handleSendOtp"
                        >
                          <span v-if="otpSending">发送中…</span>
                          <span v-else-if="otpCountdown > 0">{{ otpCountdown }}s 后重发</span>
                          <span v-else>获取验证码</span>
                        </button>
                      </div>
                      <p v-if="otpDevHint" class="mt-2 text-[12px] font-medium text-[#3d8b7a]" role="status">{{ otpDevHint }}</p>
                    </div>
                  </div>

                  <p v-if="registerFormSuccess" class="text-[13px] font-medium text-[#3d8b7a]" role="status">{{ registerFormSuccess }}</p>
                  <div v-if="loginFormError" class="space-y-2" role="alert">
                    <p class="text-[13px] font-medium text-red-600">{{ loginFormError }}</p>
                    <button
                      v-if="portalMismatchRole"
                      type="button"
                      class="w-full rounded-lg border border-[#85A185]/50 bg-[#85A185]/10 py-2 text-[12px] font-semibold text-[#3d8b7a] transition-colors hover:bg-[#85A185]/20 sm:text-[13px]"
                      @click="applyPortalMismatch"
                    >
                      切换为「{{ roleOptionLabel(portalMismatchRole) }}」并重新登录
                    </button>
                  </div>

                  <button
                    type="submit"
                    class="group relative mt-6 w-full overflow-hidden rounded-xl py-3.5 text-[14px] font-bold text-white shadow-lg transition-all duration-300 hover:-translate-y-0.5 hover:shadow-xl active:translate-y-0 disabled:cursor-not-allowed disabled:opacity-70 disabled:hover:translate-y-0 sm:mt-8 sm:text-[15px]"
                    :disabled="loginSubmitting"
                    :style="{
                      backgroundColor: sage,
                      boxShadow: `0 8px 20px -6px ${sage}`,
                    }"
                  >
                    <span class="relative z-10 flex items-center justify-center gap-2">
                      {{ loginSubmitting ? '登录中…' : '立即登录' }}
                      <svg class="h-4 w-4 transition-transform duration-300 group-hover:translate-x-1" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M17 8l4 4m0 0l-4 4m4-4H3" />
                      </svg>
                    </span>
                    <div class="absolute inset-0 -translate-x-full bg-gradient-to-r from-transparent via-white/20 to-transparent transition-transform duration-500 group-hover:translate-x-full"></div>
                  </button>

                  <p v-if="canSelfRegister" class="text-center text-[13px] text-[#6f6f6f]">
                    还没有账号？
                    <button
                      type="button"
                      class="font-semibold text-[#5a8a82] underline-offset-2 transition-colors hover:text-[#3d8b7a] hover:underline"
                      @click="openRegisterPanel"
                    >
                      立即注册
                    </button>
                  </p>
                  <p v-else class="text-center text-[12px] leading-relaxed text-[#8a9a96] sm:text-[13px]">
                    {{ ENTERPRISE_ACCOUNT_HINT }}
                  </p>
                </form>
                  </div>

                  <div
                    class="relative min-h-[200px] min-w-0 flex-1 overflow-hidden rounded-b-3xl bg-[#e8ecf0] lg:min-h-0 lg:w-1/2 lg:flex-none lg:rounded-b-none lg:rounded-r-3xl"
                  >
                    <img
                      src="/images/login-hero.png"
                      class="pointer-events-none absolute inset-0 h-full w-full select-none object-center"
                      alt="TalentAI 智能招聘与人才分析平台"
                      decoding="async"
                      fetchpriority="high"
                    />
                  </div>
                </div>

                <!-- 注册整页（从右侧滑入；返回登录时整体右滑隐藏） -->
                <div
                  class="flex w-1/2 shrink-0 flex-col lg:min-h-[min(460px,60vh)] lg:flex-row lg:items-stretch"
                  :class="authPanel === 'register' ? 'pointer-events-auto' : 'pointer-events-none'"
                  :aria-hidden="authPanel === 'login'"
                >
                  <div class="min-w-0 shrink-0 p-6 sm:p-8 lg:flex lg:w-1/2 lg:flex-col lg:justify-center lg:px-10 xl:px-12">
                    <div class="mb-4 flex items-start justify-between gap-3">
                      <div class="flex items-center gap-2">
                        <div class="h-8 w-1 rounded-full bg-[#85A185]"></div>
                        <div>
                          <h2 class="text-xl font-bold tracking-tight text-[#1a1a1a] sm:text-2xl">创建账号</h2>
                          <p class="mt-1 text-[12px] text-[#6f6f6f]">仅开放求职者自助注册，企业账号请联系管理员</p>
                        </div>
                      </div>
                      <button
                        type="button"
                        class="shrink-0 rounded-lg px-2 py-1.5 text-[12px] font-medium text-[#5a8a82] transition-colors hover:bg-black/5 hover:text-[#3d8b7a] sm:text-[13px]"
                        @click="closeRegisterPanel"
                      >
                        返回登录
                      </button>
                    </div>

                    <form class="space-y-4" @submit.prevent="handleRegisterSubmit">
                      <div>
                        <label :for="regAccountFieldId" class="mb-2 block text-[12px] font-medium text-[#3a3a3a]">手机号 / 邮箱</label>
                        <input
                          :id="regAccountFieldId"
                          v-model="registerAccountInput"
                          type="text"
                          name="reg-account"
                          autocomplete="username"
                          :class="inputGradClass"
                          placeholder="请输入手机号或邮箱"
                        />
                      </div>
                      <div>
                        <label :for="regPasswordFieldId" class="mb-2 block text-[12px] font-medium text-[#3a3a3a]">密码</label>
                        <div class="relative">
                          <input
                            :id="regPasswordFieldId"
                            v-model="registerPasswordInput"
                            :type="regShowPwd ? 'text' : 'password'"
                            name="new-password"
                            autocomplete="new-password"
                            :class="[inputGradClass, 'pr-12']"
                            placeholder="至少 6 位"
                          />
                          <button
                            type="button"
                            class="absolute right-2 top-1/2 -translate-y-1/2 rounded-lg p-2 text-[#6f6f6f] transition-colors hover:bg-black/5 hover:text-[#1a1a1a]"
                            :aria-label="regShowPwd ? '隐藏密码' : '显示密码'"
                            @click="regShowPwd = !regShowPwd"
                          >
                            <EyeOff v-if="regShowPwd" :size="18" />
                            <Eye v-else :size="18" />
                          </button>
                        </div>
                      </div>
                      <div>
                        <label :for="regConfirmFieldId" class="mb-2 block text-[12px] font-medium text-[#3a3a3a]">确认密码</label>
                        <div class="relative">
                          <input
                            :id="regConfirmFieldId"
                            v-model="registerConfirmInput"
                            :type="regShowConfirm ? 'text' : 'password'"
                            name="confirm-password"
                            autocomplete="new-password"
                            :class="[inputGradClass, 'pr-12']"
                            placeholder="再次输入密码"
                          />
                          <button
                            type="button"
                            class="absolute right-2 top-1/2 -translate-y-1/2 rounded-lg p-2 text-[#6f6f6f] transition-colors hover:bg-black/5 hover:text-[#1a1a1a]"
                            :aria-label="regShowConfirm ? '隐藏确认密码' : '显示确认密码'"
                            @click="regShowConfirm = !regShowConfirm"
                          >
                            <EyeOff v-if="regShowConfirm" :size="18" />
                            <Eye v-else :size="18" />
                          </button>
                        </div>
                      </div>
                      <p v-if="registerFormError" class="text-[13px] font-medium text-red-600" role="alert">{{ registerFormError }}</p>
                      <button
                        type="submit"
                        class="mt-2 w-full rounded-xl py-3.5 text-[14px] font-bold text-white shadow-lg transition-all duration-300 hover:-translate-y-0.5 hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-70 disabled:hover:translate-y-0 sm:text-[15px]"
                        :disabled="registerSubmitting"
                        :style="{
                          backgroundColor: sage,
                          boxShadow: `0 8px 20px -6px ${sage}`,
                        }"
                      >
                        {{ registerSubmitting ? '注册中…' : '完成注册' }}
                      </button>
                    </form>
                  </div>

                  <div
                    class="relative min-h-[200px] min-w-0 flex-1 overflow-hidden rounded-b-3xl bg-[#e8ecf0] lg:min-h-0 lg:w-1/2 lg:flex-none lg:rounded-b-none lg:rounded-r-3xl"
                  >
                    <img
                      src="/images/login-hero.png"
                      class="pointer-events-none absolute inset-0 h-full w-full select-none object-center"
                      alt=""
                      decoding="async"
                      fetchpriority="low"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>

      <!-- 底部优化 -->
      <footer
        class="relative z-10 mx-auto w-full max-w-[min(1520px,calc(100vw-0.5rem))] shrink-0 px-2 py-5 text-center sm:px-3 lg:px-4"
      >
        <p class="text-[11px] text-[#a8a8a8]/80 sm:text-[12px]">
          Copyright © 2026 TalentAI, 智能招聘实训平台. All rights reserved.
        </p>
      </footer>
    </div>
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