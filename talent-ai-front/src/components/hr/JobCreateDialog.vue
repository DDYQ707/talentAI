<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { X, Briefcase } from 'lucide-vue-next'
import { fetchHrJobDetail, publishHrJob, updateHrJob } from '@/api/hrJob'
import type { JobPost } from '@/api/job'
import { JOB_PRIORITY, JOB_STATUS } from '@/constants/job'
import { getErrorMessage } from '@/utils/validators'

const props = withDefaults(
  defineProps<{
    open: boolean
    mode?: 'create' | 'edit'
    jobId?: number | null
  }>(),
  {
    mode: 'create',
    jobId: null,
  },
)

const emit = defineEmits<{
  close: []
  success: []
}>()

const saving = ref(false)
const loading = ref(false)
const formError = ref('')

const formTitle = ref('')
const formDeptName = ref('技术部')
const formWorkCity = ref('')
const formEmploymentType = ref(1)
const formSalaryMinK = ref<number | ''>('')
const formSalaryMaxK = ref<number | ''>('')
const formSalaryNegotiable = ref(false)
const formHeadcount = ref(1)
const formPriority = ref(JOB_PRIORITY.MEDIUM)
const formStatus = ref(JOB_STATUS.OPEN)
const formSkillTags = ref('')
const formJobDescription = ref('')
const formJobRequirements = ref('')

const deptOptions = ['技术部', '产品部', '设计部', '运营部', '市场部', '人力资源部']

const isEdit = computed(() => props.mode === 'edit')
const dialogTitle = computed(() => (isEdit.value ? '编辑岗位' : '新建岗位'))
const dialogSubtitle = computed(() =>
  isEdit.value ? '修改岗位信息并保存' : '发布后岗位将立即进入招聘中状态',
)
const submitLabel = computed(() => {
  if (saving.value) return isEdit.value ? '保存中...' : '发布中...'
  return isEdit.value ? '保存修改' : '确认发布'
})

function resetForm() {
  formTitle.value = ''
  formDeptName.value = '技术部'
  formWorkCity.value = ''
  formEmploymentType.value = 1
  formSalaryMinK.value = ''
  formSalaryMaxK.value = ''
  formSalaryNegotiable.value = false
  formHeadcount.value = 1
  formPriority.value = JOB_PRIORITY.MEDIUM
  formStatus.value = JOB_STATUS.OPEN
  formSkillTags.value = ''
  formJobDescription.value = ''
  formJobRequirements.value = ''
  formError.value = ''
}

function fillFormFromJob(job: JobPost) {
  formTitle.value = job.title
  formDeptName.value = job.deptName || '技术部'
  formWorkCity.value = job.workCity || ''
  formEmploymentType.value = job.employmentType ?? 1
  formSalaryNegotiable.value = !!job.salaryNegotiable
  formSalaryMinK.value = job.salaryMin ? Math.round(job.salaryMin / 1000) : ''
  formSalaryMaxK.value = job.salaryMax ? Math.round(job.salaryMax / 1000) : ''
  formHeadcount.value = job.headcount ?? 1
  formPriority.value = job.priority ?? JOB_PRIORITY.MEDIUM
  formStatus.value = job.status ?? JOB_STATUS.OPEN
  formSkillTags.value = job.skillTags || ''
  formJobDescription.value = job.jobDescription || ''
  formJobRequirements.value = job.jobRequirements || ''
}

async function initForm() {
  formError.value = ''
  if (isEdit.value && props.jobId) {
    loading.value = true
    try {
      const job = await fetchHrJobDetail(props.jobId)
      fillFormFromJob(job)
    } catch (e) {
      formError.value = getErrorMessage(e, '岗位详情加载失败')
      resetForm()
    } finally {
      loading.value = false
    }
  } else {
    resetForm()
  }
}

watch(
  () => [props.open, props.mode, props.jobId] as const,
  ([visible]) => {
    if (visible) initForm()
  },
)

function closeDialog() {
  if (saving.value) return
  emit('close')
}

function validateForm(): string | null {
  if (!formTitle.value.trim()) return '请填写岗位名称'
  if (!formDeptName.value.trim()) return '请选择所属部门'
  if (!formWorkCity.value.trim()) return '请填写工作城市'
  if (!formHeadcount.value || formHeadcount.value < 1) return '招聘人数至少为 1'

  if (!formSalaryNegotiable.value) {
    const minK = Number(formSalaryMinK.value)
    const maxK = Number(formSalaryMaxK.value)
    if (!minK || !maxK) return '请填写薪资范围（单位：K/月）'
    if (minK > maxK) return '薪资下限不能大于上限'
  }

  return null
}

function buildPayload() {
  return {
    title: formTitle.value.trim(),
    deptName: formDeptName.value.trim(),
    workCity: formWorkCity.value.trim(),
    employmentType: formEmploymentType.value,
    headcount: formHeadcount.value,
    priority: formPriority.value,
    salaryNegotiable: formSalaryNegotiable.value,
    skillTags: formSkillTags.value.trim() || undefined,
    jobDescription: formJobDescription.value.trim() || undefined,
    jobRequirements: formJobRequirements.value.trim() || undefined,
    ...(isEdit.value ? { status: formStatus.value } : {}),
    ...(formSalaryNegotiable.value
      ? {}
      : {
          salaryMin: Number(formSalaryMinK.value) * 1000,
          salaryMax: Number(formSalaryMaxK.value) * 1000,
        }),
  }
}

async function submitForm() {
  formError.value = ''
  const err = validateForm()
  if (err) {
    formError.value = err
    return
  }

  saving.value = true
  try {
    if (isEdit.value && props.jobId) {
      await updateHrJob(props.jobId, buildPayload())
    } else {
      await publishHrJob(buildPayload())
    }
    emit('success')
    emit('close')
  } catch (e) {
    formError.value = getErrorMessage(e, isEdit.value ? '岗位更新失败' : '岗位发布失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm"
      @click.self="closeDialog"
    >
      <div
        class="flex max-h-[90vh] w-full max-w-2xl flex-col rounded-2xl border border-border bg-card shadow-2xl"
        role="dialog"
        aria-modal="true"
        aria-labelledby="job-create-dialog-title"
      >
        <div class="flex flex-shrink-0 items-start justify-between gap-3 border-b border-border p-6 pb-4">
          <div class="flex items-center gap-2">
            <div class="gradient-blue flex h-10 w-10 items-center justify-center rounded-xl">
              <Briefcase :size="20" class="text-white" />
            </div>
            <div>
              <h2 id="job-create-dialog-title" class="text-lg font-bold text-foreground">{{ dialogTitle }}</h2>
              <p class="text-xs text-muted-foreground">{{ dialogSubtitle }}</p>
            </div>
          </div>
          <button type="button" class="rounded-lg p-1.5 text-muted-foreground hover:bg-muted" :disabled="saving" @click="closeDialog">
            <X :size="18" />
          </button>
        </div>
        <form class="flex-1 space-y-4 overflow-y-auto scrollbar-thin px-6 py-4" @submit.prevent="submitForm">
          <p v-if="loading" class="text-sm text-muted-foreground text-center py-6">加载中...</p>
          <div v-else class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div class="sm:col-span-2">
              <label class="mb-1.5 block text-xs font-medium text-foreground">岗位名称 <span class="text-brand-red">*</span></label>
              <input v-model="formTitle" type="text" class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185] focus:ring-2 focus:ring-[#85A185]/20" placeholder="例如：高级前端工程师" />
            </div>
            <div>
              <label class="mb-1.5 block text-xs font-medium text-foreground">所属部门 <span class="text-brand-red">*</span></label>
              <select v-model="formDeptName" class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185]">
                <option v-for="d in deptOptions" :key="d" :value="d">{{ d }}</option>
              </select>
            </div>
            <div>
              <label class="mb-1.5 block text-xs font-medium text-foreground">工作城市 <span class="text-brand-red">*</span></label>
              <input v-model="formWorkCity" type="text" class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185] focus:ring-2 focus:ring-[#85A185]/20" placeholder="例如：北京" />
            </div>
            <div>
              <label class="mb-1.5 block text-xs font-medium text-foreground">用工类型</label>
              <select v-model.number="formEmploymentType" class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185]">
                <option :value="1">全职</option>
                <option :value="2">兼职</option>
                <option :value="3">实习</option>
              </select>
            </div>
            <div>
              <label class="mb-1.5 block text-xs font-medium text-foreground">招聘优先级</label>
              <select v-model.number="formPriority" class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185]">
                <option :value="JOB_PRIORITY.HIGH">高</option>
                <option :value="JOB_PRIORITY.MEDIUM">中</option>
                <option :value="JOB_PRIORITY.LOW">低</option>
              </select>
            </div>
            <div v-if="isEdit">
              <label class="mb-1.5 block text-xs font-medium text-foreground">岗位状态</label>
              <select v-model.number="formStatus" class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185]">
                <option :value="JOB_STATUS.OPEN">招聘中</option>
                <option :value="JOB_STATUS.PAUSED">已暂停</option>
                <option :value="JOB_STATUS.CLOSED">已完成</option>
              </select>
            </div>
            <div>
              <label class="mb-1.5 block text-xs font-medium text-foreground">招聘人数 <span class="text-brand-red">*</span></label>
              <input v-model.number="formHeadcount" type="number" min="1" class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185] focus:ring-2 focus:ring-[#85A185]/20" />
            </div>
            <div class="sm:col-span-2">
              <div class="mb-2 flex items-center justify-between">
                <label class="text-xs font-medium text-foreground">薪资范围（K/月）</label>
                <label class="flex cursor-pointer items-center gap-2 text-xs text-muted-foreground">
                  <input v-model="formSalaryNegotiable" type="checkbox" class="rounded border-border" />
                  面议
                </label>
              </div>
              <div v-if="!formSalaryNegotiable" class="grid grid-cols-2 gap-3">
                <input v-model.number="formSalaryMinK" type="number" min="1" class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185] focus:ring-2 focus:ring-[#85A185]/20" placeholder="下限，如 25" />
                <input v-model.number="formSalaryMaxK" type="number" min="1" class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185] focus:ring-2 focus:ring-[#85A185]/20" placeholder="上限，如 40" />
              </div>
              <p v-else class="text-xs text-muted-foreground">已选择面议，无需填写薪资</p>
            </div>
            <div class="sm:col-span-2">
              <label class="mb-1.5 block text-xs font-medium text-foreground">技能标签</label>
              <input v-model="formSkillTags" type="text" class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185] focus:ring-2 focus:ring-[#85A185]/20" placeholder="React,TypeScript" />
            </div>
            <div class="sm:col-span-2">
              <label class="mb-1.5 block text-xs font-medium text-foreground">职位描述</label>
              <textarea v-model="formJobDescription" rows="3" class="w-full resize-none rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185] focus:ring-2 focus:ring-[#85A185]/20" />
            </div>
            <div class="sm:col-span-2">
              <label class="mb-1.5 block text-xs font-medium text-foreground">任职要求</label>
              <textarea v-model="formJobRequirements" rows="3" class="w-full resize-none rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185] focus:ring-2 focus:ring-[#85A185]/20" />
            </div>
          </div>
          <p v-if="formError" class="text-sm font-medium text-red-600" role="alert">{{ formError }}</p>
        </form>
        <div class="flex flex-shrink-0 gap-3 border-t border-border p-6 pt-4">
          <button type="button" class="flex-1 rounded-xl border border-border py-2.5 text-sm font-medium text-foreground hover:bg-muted disabled:opacity-60" :disabled="saving" @click="closeDialog">取消</button>
          <button
            type="button"
            class="flex-1 rounded-xl gradient-blue py-2.5 text-sm font-medium text-white disabled:opacity-60"
            :disabled="saving || loading"
            @click="submitForm"
          >
            {{ submitLabel }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>