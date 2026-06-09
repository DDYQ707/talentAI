<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Calendar, X } from 'lucide-vue-next'
import { fetchInterviewers, scheduleInterview } from '@/api/interview'
import type { InterviewerOption } from '@/api/interview'
import {
  INTERVIEW_MODE,
  INTERVIEW_ROUND_TYPE,
  INTERVIEW_MODE_OPTIONS,
  ROUND_TYPE_OPTIONS,
  toLocalDateTimeString,
} from '@/constants/interview'
import { getErrorMessage } from '@/utils/validators'

const props = withDefaults(
  defineProps<{
    open: boolean
    applicationId?: number | null
    candidateName?: string
    jobTitle?: string
  }>(),
  {
    applicationId: null,
    candidateName: '',
    jobTitle: '',
  },
)

const emit = defineEmits<{
  close: []
  success: []
}>()

const saving = ref(false)
const loadingInterviewers = ref(false)
const formError = ref('')
const interviewers = ref<InterviewerOption[]>([])

const formInterviewerId = ref<number | ''>('')
const formRoundType = ref<number>(INTERVIEW_ROUND_TYPE.TECH_FIRST)
const formRoundNo = ref(1)
const formInterviewMode = ref<number>(INTERVIEW_MODE.VIDEO)
const formScheduledStart = ref('')
const formMeetingUrl = ref('')
const formLocation = ref('')

const canSubmit = computed(() => !!props.applicationId && props.applicationId > 0)

const subtitle = computed(() => {
  if (!canSubmit.value) return '该候选人暂无投递记录，无法安排面试'
  const parts = [props.candidateName, props.jobTitle].filter(Boolean)
  return parts.length ? parts.join(' · ') : `投递单 #${props.applicationId}`
})

watch(
  () => props.open,
  async (visible) => {
    if (!visible) return
    formError.value = ''
    formInterviewerId.value = ''
    formRoundType.value = INTERVIEW_ROUND_TYPE.TECH_FIRST
    formRoundNo.value = 1
    formInterviewMode.value = INTERVIEW_MODE.VIDEO
    formScheduledStart.value = ''
    formMeetingUrl.value = ''
    formLocation.value = ''
    await loadInterviewers()
  },
)

async function loadInterviewers() {
  loadingInterviewers.value = true
  try {
    interviewers.value = await fetchInterviewers()
  } catch (e) {
    formError.value = getErrorMessage(e, '面试官列表加载失败')
    interviewers.value = []
  } finally {
    loadingInterviewers.value = false
  }
}

async function handleSubmit() {
  if (!canSubmit.value || saving.value) return
  formError.value = ''

  if (!formInterviewerId.value) {
    formError.value = '请选择面试官'
    return
  }
  if (!formScheduledStart.value) {
    formError.value = '请选择面试时间'
    return
  }
  if (formInterviewMode.value === INTERVIEW_MODE.VIDEO && !formMeetingUrl.value.trim()) {
    formError.value = '视频面试请填写会议链接'
    return
  }
  if (formInterviewMode.value === INTERVIEW_MODE.ONSITE && !formLocation.value.trim()) {
    formError.value = '现场面试请填写地址'
    return
  }

  saving.value = true
  try {
    await scheduleInterview({
      applicationId: props.applicationId!,
      interviewerId: Number(formInterviewerId.value),
      roundType: formRoundType.value,
      roundNo: formRoundNo.value,
      interviewMode: formInterviewMode.value,
      scheduledStart: toLocalDateTimeString(formScheduledStart.value),
      meetingUrl: formMeetingUrl.value.trim() || undefined,
      location: formLocation.value.trim() || undefined,
    })
    emit('success')
    emit('close')
  } catch (e) {
    formError.value = getErrorMessage(e, '安排面试失败')
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
      @click.self="emit('close')"
    >
      <div class="w-full max-w-lg bg-card rounded-2xl shadow-xl border border-border overflow-hidden">
        <div class="flex items-center justify-between px-6 py-4 border-b border-border">
          <div class="flex items-center gap-2">
            <div class="w-8 h-8 rounded-lg gradient-blue flex items-center justify-center">
              <Calendar :size="16" class="text-white" />
            </div>
            <div>
              <h2 class="text-base font-bold text-foreground">安排面试</h2>
              <p class="text-xs text-muted-foreground mt-0.5">{{ subtitle }}</p>
            </div>
          </div>
          <button type="button" class="p-2 rounded-lg hover:bg-muted text-muted-foreground" @click="emit('close')">
            <X :size="18" />
          </button>
        </div>

        <div class="px-6 py-5 space-y-4 max-h-[70vh] overflow-y-auto scrollbar-thin">
          <p v-if="formError" class="text-xs text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
            {{ formError }}
          </p>
          <p v-if="!canSubmit" class="text-xs text-orange-600 bg-orange-50 border border-orange-100 rounded-lg px-3 py-2">
            请确认候选人已完成岗位投递后再安排面试。
          </p>

          <div>
            <label class="text-xs font-medium text-muted-foreground mb-1.5 block">面试官</label>
            <select
              v-model="formInterviewerId"
              class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-brand-blue/50"
              :disabled="loadingInterviewers || !canSubmit"
            >
              <option value="">{{ loadingInterviewers ? '加载中...' : '请选择面试官' }}</option>
              <option v-for="item in interviewers" :key="item.id" :value="item.id">
                {{ item.nickname }}（{{ item.account }}）
              </option>
            </select>
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="text-xs font-medium text-muted-foreground mb-1.5 block">面试轮次</label>
              <select
                v-model="formRoundType"
                class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none"
                :disabled="!canSubmit"
              >
                <option v-for="opt in ROUND_TYPE_OPTIONS" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </option>
              </select>
            </div>
            <div>
              <label class="text-xs font-medium text-muted-foreground mb-1.5 block">轮次序号</label>
              <input
                v-model.number="formRoundNo"
                type="number"
                min="1"
                class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none"
                :disabled="!canSubmit"
              />
            </div>
          </div>

          <div>
            <label class="text-xs font-medium text-muted-foreground mb-1.5 block">面试方式</label>
            <select
              v-model="formInterviewMode"
              class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none"
              :disabled="!canSubmit"
            >
              <option v-for="opt in INTERVIEW_MODE_OPTIONS" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </div>

          <div>
            <label class="text-xs font-medium text-muted-foreground mb-1.5 block">计划开始时间</label>
            <input
              v-model="formScheduledStart"
              type="datetime-local"
              class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none"
              :disabled="!canSubmit"
            />
          </div>

          <div v-if="formInterviewMode === INTERVIEW_MODE.VIDEO">
            <label class="text-xs font-medium text-muted-foreground mb-1.5 block">视频会议链接</label>
            <input
              v-model="formMeetingUrl"
              type="url"
              placeholder="https://meet.example.com/room"
              class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none"
              :disabled="!canSubmit"
            />
          </div>

          <div v-if="formInterviewMode === INTERVIEW_MODE.ONSITE">
            <label class="text-xs font-medium text-muted-foreground mb-1.5 block">现场地址</label>
            <input
              v-model="formLocation"
              type="text"
              placeholder="如：总部 A 座 3 层会议室"
              class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none"
              :disabled="!canSubmit"
            />
          </div>
        </div>

        <div class="flex gap-3 px-6 py-4 border-t border-border bg-muted/30">
          <button
            type="button"
            class="flex-1 py-2.5 rounded-xl border border-border text-sm text-muted-foreground hover:bg-muted"
            @click="emit('close')"
          >
            取消
          </button>
          <button
            type="button"
            class="flex-1 py-2.5 rounded-xl gradient-blue text-white text-sm font-medium disabled:opacity-50"
            :disabled="!canSubmit || saving"
            @click="handleSubmit"
          >
            {{ saving ? '提交中...' : '确认安排' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
