<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Calendar, X } from 'lucide-vue-next'
import {
  fetchInterviewers,
  fetchInterviewsByApplication,
  fetchScheduleableApplications,
  scheduleInterview,
  type ScheduleableApplicationOption,
} from '@/api/interview'
import type { InterviewerOption, InterviewListItem } from '@/api/interview'
import {
  INTERVIEW_MODE,
  INTERVIEW_ROUND_TYPE,
  INTERVIEW_STATUS,
  INTERVIEW_MODE_OPTIONS,
  MAX_INTERVIEW_ROUNDS,
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
const loadingApplications = ref(false)
const formError = ref('')
const interviewers = ref<InterviewerOption[]>([])
const scheduleableApplications = ref<ScheduleableApplicationOption[]>([])
const pickedApplicationId = ref<number | ''>('')

const formInterviewerId = ref<number | ''>('')
const formRoundType = ref<number>(INTERVIEW_ROUND_TYPE.TECH_FIRST)
const formRoundNo = ref(1)
const formInterviewMode = ref<number>(INTERVIEW_MODE.VIDEO)
const formScheduledStart = ref('')
const formMeetingUrl = ref('')
const formLocation = ref('')

const applicationInterviews = ref<InterviewListItem[]>([])

const needsApplicationPick = computed(() => !props.applicationId || props.applicationId <= 0)

const activeApplicationId = computed(() => {
  if (!needsApplicationPick.value) return props.applicationId!
  const picked = Number(pickedApplicationId.value)
  return Number.isFinite(picked) && picked > 0 ? picked : null
})

const pickedApplication = computed(() =>
  scheduleableApplications.value.find((item) => item.applicationId === activeApplicationId.value) ?? null,
)

const canSubmit = computed(() => !!activeApplicationId.value && activeApplicationId.value > 0)

const subtitle = computed(() => {
  if (needsApplicationPick.value) {
    if (pickedApplication.value) {
      return `${pickedApplication.value.candidateName} · ${pickedApplication.value.jobTitle}`
    }
    return '请选择要安排面试的投递记录'
  }
  const parts = [props.candidateName, props.jobTitle].filter(Boolean)
  return parts.length ? parts.join(' · ') : `投递单 #${props.applicationId}`
})

/** 已取消的轮次可复用；待面试/待安排/面试完成 视为占用该轮次序号 */
function suggestNextRoundNo(interviews: InterviewListItem[]): number | null {
  const occupied = new Set<number>()
  for (const item of interviews) {
    if (item.status === INTERVIEW_STATUS.CANCELLED) continue
    const round = Number(item.roundNo)
    if (Number.isFinite(round) && round > 0) occupied.add(round)
  }
  let next = 1
  while (occupied.has(next) && next <= MAX_INTERVIEW_ROUNDS) next++
  if (next > MAX_INTERVIEW_ROUNDS) return null
  return next
}

const suggestedRoundNo = computed(() => suggestNextRoundNo(applicationInterviews.value))

const roundTypeOptions = ROUND_TYPE_OPTIONS

const scheduleBlocked = computed(() => suggestedRoundNo.value == null)

const roundScheduleHint = computed(() => {
  const round = formRoundNo.value || 1
  return `第 ${round} 轮 · 每份投递最多 ${MAX_INTERVIEW_ROUNDS} 轮（可只安排 1 轮）；面试类型由 HR 自由选择，AI 将按所选类型生成题目`
})

async function loadSuggestedRoundNo(applicationId: number) {
  try {
    const records = await fetchInterviewsByApplication(applicationId)
    applicationInterviews.value = records ?? []
    const next = suggestNextRoundNo(applicationInterviews.value)
    if (next == null) {
      formRoundNo.value = MAX_INTERVIEW_ROUNDS
      formError.value = '该投递已完成最多 3 轮面试安排，无法继续新增'
    } else {
      formRoundNo.value = next
    }
  } catch {
    applicationInterviews.value = []
    formRoundNo.value = 1
    formRoundType.value = INTERVIEW_ROUND_TYPE.TECH_FIRST
  }
}

watch(
  () => props.open,
  async (visible) => {
    if (!visible) return
    formError.value = ''
    pickedApplicationId.value = ''
    formInterviewerId.value = ''
    formRoundType.value = INTERVIEW_ROUND_TYPE.TECH_FIRST
    formRoundNo.value = 1
    formInterviewMode.value = INTERVIEW_MODE.VIDEO
    formScheduledStart.value = ''
    formMeetingUrl.value = ''
    formLocation.value = ''
    await Promise.all([loadInterviewers(), loadScheduleableApplications()])
    if (activeApplicationId.value) {
      await loadSuggestedRoundNo(activeApplicationId.value)
    }
  },
)

watch(activeApplicationId, async (applicationId) => {
  if (!props.open || !applicationId) return
  await loadSuggestedRoundNo(applicationId)
})

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

async function loadScheduleableApplications() {
  if (!needsApplicationPick.value) {
    scheduleableApplications.value = []
    return
  }
  loadingApplications.value = true
  try {
    scheduleableApplications.value = await fetchScheduleableApplications()
  } catch (e) {
    formError.value = getErrorMessage(e, '可安排投递列表加载失败')
    scheduleableApplications.value = []
  } finally {
    loadingApplications.value = false
  }
}

async function handleSubmit() {
  if (!canSubmit.value || saving.value || scheduleBlocked.value) return
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
      applicationId: activeApplicationId.value!,
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
          <div v-if="needsApplicationPick">
            <label class="text-xs font-medium text-muted-foreground mb-1.5 block">投递记录</label>
            <select
              v-model="pickedApplicationId"
              class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-brand-blue/50"
              :disabled="loadingApplications"
            >
              <option value="">
                {{ loadingApplications ? '加载投递列表...' : '请选择候选人投递单' }}
              </option>
              <option
                v-for="item in scheduleableApplications"
                :key="item.applicationId"
                :value="item.applicationId"
              >
                {{ item.candidateName }} · {{ item.jobTitle }}
                <template v-if="item.matchScore != null && item.matchScore > 0">
                  （AI {{ item.matchScore }} 分）
                </template>
              </option>
            </select>
            <p
              v-if="!loadingApplications && scheduleableApplications.length === 0"
              class="text-xs text-orange-600 mt-2"
            >
              暂无可安排面试的投递记录，请先在简历库完成初筛或确认候选人已投递岗位。
            </p>
          </div>
          <p
            v-else-if="!canSubmit"
            class="text-xs text-orange-600 bg-orange-50 border border-orange-100 rounded-lg px-3 py-2"
          >
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

          <div class="rounded-xl border border-brand-border/60 bg-accent/40 px-3 py-2 text-xs text-muted-foreground">
            {{ roundScheduleHint }}
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="text-xs font-medium text-muted-foreground mb-1.5 block">面试类型</label>
              <select
                v-model="formRoundType"
                class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none"
                :disabled="!canSubmit || scheduleBlocked"
              >
                <option v-for="opt in roundTypeOptions" :key="opt.value" :value="opt.value">
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
                :max="MAX_INTERVIEW_ROUNDS"
                class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none"
                :disabled="!canSubmit || scheduleBlocked"
              />
              <p class="text-[11px] text-muted-foreground mt-1">
                序号 1～{{ MAX_INTERVIEW_ROUNDS }}，可跳过；已取消的序号可重新使用
              </p>
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
            :disabled="!canSubmit || saving || scheduleBlocked"
            @click="handleSubmit"
          >
            {{ saving ? '提交中...' : '确认安排' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
