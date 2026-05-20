<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronLeft } from 'lucide-vue-next'
import {
  fetchMyCandidateProfile,
  saveMyCandidateProfile,
  type CandidateProfileSavePayload,
} from '@/api/candidateProfile'
import { GENDER_OPTIONS, HIGHEST_EDU_OPTIONS, JOB_SEEKING_OPTIONS } from '@/constants/candidateProfile'
import { getErrorMessage, validateAccount } from '@/utils/validators'

const router = useRouter()
const loading = ref(true)
const saving = ref(false)
const errorMsg = ref('')
const successMsg = ref('')

const form = ref({
  realName: '',
  phone: '',
  email: '',
  gender: 0 as number | undefined,
  birthDate: '',
  currentTitle: '',
  workYears: '' as string | number,
  highestEdu: undefined as number | undefined,
  city: '',
  jobSeekingStatus: undefined as number | undefined,
})

async function loadProfile() {
  loading.value = true
  errorMsg.value = ''
  try {
    const p = await fetchMyCandidateProfile()
    form.value = {
      realName: p.realName ?? p.nickname ?? '',
      phone: p.phone ?? '',
      email: p.email ?? '',
      gender: p.gender ?? 0,
      birthDate: p.birthDate ?? '',
      currentTitle: p.currentTitle ?? '',
      workYears: p.workYears != null ? String(p.workYears) : '',
      highestEdu: p.highestEdu,
      city: p.city ?? '',
      jobSeekingStatus: p.jobSeekingStatus,
    }
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '加载个人信息失败')
  } finally {
    loading.value = false
  }
}

function asTrimmedString(value: string | number | null | undefined): string {
  if (value == null || value === '') return ''
  return String(value).trim()
}

function buildPayload(): CandidateProfileSavePayload | null {
  const phoneErr = validateAccount(form.value.phone)
  if (!form.value.realName.trim()) {
    errorMsg.value = '请填写真实姓名'
    return null
  }
  if (phoneErr) {
    errorMsg.value = phoneErr
    return null
  }
  if (!form.value.city.trim()) {
    errorMsg.value = '请填写所在城市'
    return null
  }
  if (!form.value.highestEdu) {
    errorMsg.value = '请选择最高学历'
    return null
  }
  const workYears = asTrimmedString(form.value.workYears)
  let workYearsNum: number | undefined
  if (workYears) {
    const n = Number(workYears)
    if (Number.isNaN(n) || n < 0 || n > 50) {
      errorMsg.value = '工作年限请填写 0–50 之间的数字'
      return null
    }
    workYearsNum = n
  }
  return {
    realName: form.value.realName.trim(),
    phone: form.value.phone.trim(),
    email: form.value.email.trim() || undefined,
    gender: form.value.gender,
    birthDate: form.value.birthDate.trim() || undefined,
    currentTitle: form.value.currentTitle.trim() || undefined,
    workYears: workYearsNum,
    highestEdu: form.value.highestEdu,
    city: form.value.city.trim(),
    jobSeekingStatus: form.value.jobSeekingStatus,
  }
}

async function handleSave() {
  if (saving.value) return
  errorMsg.value = ''
  successMsg.value = ''
  const payload = buildPayload()
  if (!payload) return

  saving.value = true
  try {
    await saveMyCandidateProfile(payload)
    successMsg.value = '保存成功'
    setTimeout(() => router.replace('/candidate/profile'), 500)
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div data-cmp="ProfileEdit" class="flex h-full flex-col bg-[#EBF4F0]">
    <div class="flex items-center gap-3 px-4 py-3 border-b border-border bg-card flex-shrink-0">
      <button type="button" class="p-1.5 rounded-lg hover:bg-muted" @click="router.back()">
        <ChevronLeft :size="20" />
      </button>
      <span class="text-sm font-semibold text-foreground flex-1">编辑个人信息</span>
      <button
        type="button"
        class="text-xs text-brand-blue font-medium px-2 py-1 disabled:opacity-50"
        :disabled="saving || loading"
        @click="handleSave"
      >
        {{ saving ? '保存中...' : '保存' }}
      </button>
    </div>

    <div class="flex-1 overflow-y-auto scrollbar-thin px-4 py-4 pb-6 space-y-4">
      <p v-if="loading" class="text-xs text-muted-foreground">加载中...</p>

      <template v-else>
        <div class="rounded-2xl border border-border bg-card p-4 shadow-card space-y-3">
          <p class="text-xs text-muted-foreground">
            真实姓名、手机号、城市、最高学历为必填项，完善后方可投递简历。
          </p>

          <label class="block">
            <span class="text-xs text-muted-foreground">真实姓名 <span class="text-red-500">*</span></span>
            <input
              v-model="form.realName"
              type="text"
              maxlength="32"
              class="mt-1 w-full rounded-xl border border-border px-3 py-2 text-sm"
              placeholder="与身份证一致"
            />
          </label>

          <label class="block">
            <span class="text-xs text-muted-foreground">手机号 <span class="text-red-500">*</span></span>
            <input
              v-model="form.phone"
              type="tel"
              maxlength="11"
              class="mt-1 w-full rounded-xl border border-border px-3 py-2 text-sm"
              placeholder="11 位大陆手机号"
            />
          </label>

          <label class="block">
            <span class="text-xs text-muted-foreground">邮箱</span>
            <input
              v-model="form.email"
              type="email"
              class="mt-1 w-full rounded-xl border border-border px-3 py-2 text-sm"
              placeholder="选填"
            />
          </label>

          <label class="block">
            <span class="text-xs text-muted-foreground">所在城市 <span class="text-red-500">*</span></span>
            <input
              v-model="form.city"
              type="text"
              maxlength="64"
              class="mt-1 w-full rounded-xl border border-border px-3 py-2 text-sm"
              placeholder="如：北京"
            />
          </label>

          <label class="block">
            <span class="text-xs text-muted-foreground">最高学历 <span class="text-red-500">*</span></span>
            <select
              v-model.number="form.highestEdu"
              class="mt-1 w-full rounded-xl border border-border px-3 py-2 text-sm bg-card"
            >
              <option :value="undefined" disabled>请选择</option>
              <option v-for="o in HIGHEST_EDU_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</option>
            </select>
          </label>

          <label class="block">
            <span class="text-xs text-muted-foreground">当前职位</span>
            <input
              v-model="form.currentTitle"
              type="text"
              maxlength="64"
              class="mt-1 w-full rounded-xl border border-border px-3 py-2 text-sm"
              placeholder="如：高级前端工程师"
            />
          </label>

          <label class="block">
            <span class="text-xs text-muted-foreground">工作年限（年）</span>
            <input
              v-model="form.workYears"
              type="number"
              min="0"
              max="50"
              step="0.5"
              class="mt-1 w-full rounded-xl border border-border px-3 py-2 text-sm"
              placeholder="选填"
            />
          </label>

          <label class="block">
            <span class="text-xs text-muted-foreground">性别</span>
            <select
              v-model.number="form.gender"
              class="mt-1 w-full rounded-xl border border-border px-3 py-2 text-sm bg-card"
            >
              <option v-for="o in GENDER_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</option>
            </select>
          </label>

          <label class="block">
            <span class="text-xs text-muted-foreground">出生日期</span>
            <input
              v-model="form.birthDate"
              type="date"
              class="mt-1 w-full rounded-xl border border-border px-3 py-2 text-sm"
            />
          </label>

          <label class="block">
            <span class="text-xs text-muted-foreground">求职状态</span>
            <select
              v-model.number="form.jobSeekingStatus"
              class="mt-1 w-full rounded-xl border border-border px-3 py-2 text-sm bg-card"
            >
              <option :value="undefined">未设置</option>
              <option v-for="o in JOB_SEEKING_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</option>
            </select>
          </label>
        </div>

        <p v-if="errorMsg" class="text-xs text-red-600" role="alert">{{ errorMsg }}</p>
        <p v-if="successMsg" class="text-xs text-brand-green">{{ successMsg }}</p>
      </template>
    </div>
  </div>
</template>
