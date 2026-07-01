<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Calendar,
  Clock,
  CheckCircle,
  Video,
  Sparkles,
  ArrowRight,
  ExternalLink,
  Copy,
  ClipboardList,
} from 'lucide-vue-next'
import {
  fetchMyInterviewPage,
  fetchMyInterviewStats,
  type InterviewListItem,
  type InterviewStats,
} from '@/api/interview'
import {
  INTERVIEW_STATUS,
  formatInterviewDateTime,
  interviewStatusClass,
} from '@/constants/interview'
import {
  buildMeetingCopyText,
  canJoinMeeting,
  isScheduledToday,
  openMeeting,
  copyMeetingInfo,
} from '@/utils/interviewMeeting'
import { getErrorMessage } from '@/utils/validators'

const router = useRouter()
const loading = ref(false)
const errorMsg = ref('')
const copyTip = ref('')
const stats = ref<InterviewStats | null>(null)
const pendingInterviews = ref<InterviewListItem[]>([])

const statCards = computed(() => [
  {
    label: '今日待面试',
    value: stats.value?.todayPending ?? 0,
    icon: Calendar,
    color: 'text-brand-blue bg-brand-tint',
  },
  {
    label: '本周合计',
    value: stats.value?.weekTotal ?? 0,
    icon: Clock,
    color: 'text-brand-purple bg-brand-tint-2',
  },
  {
    label: '已完成',
    value: stats.value?.completed ?? 0,
    icon: CheckCircle,
    color: 'text-brand-green bg-green-50',
  },
])

const todaySchedule = computed(() =>
  pendingInterviews.value
    .filter((iv) => isScheduledToday(iv.scheduledStart))
    .sort((a, b) => (a.scheduledStart ?? '').localeCompare(b.scheduledStart ?? '')),
)

const upcomingPending = computed(() =>
  pendingInterviews.value
    .filter((iv) => !isScheduledToday(iv.scheduledStart))
    .slice(0, 5),
)

const headerHint = computed(() => {
  const count = todaySchedule.value.length
  if (count > 0) return `今天有 ${count} 场线上面试，记得提前进入会议`
  const pending = stats.value?.todayPending ?? 0
  return pending > 0 ? `您有 ${pending} 场待面试` : '今日暂无面试安排，可查看待办或准备下一场'
})

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const [statsData, pageData] = await Promise.all([
      fetchMyInterviewStats(),
      fetchMyInterviewPage({ page: 1, size: 50, status: INTERVIEW_STATUS.PENDING }),
    ])
    stats.value = statsData
    pendingInterviews.value = pageData.records ?? []
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '工作台数据加载失败')
  } finally {
    loading.value = false
  }
}

function openDetail(interviewId: number) {
  router.push({ path: '/interviewer/detail', query: { id: String(interviewId) } })
}

function openPrep(interviewId: number) {
  router.push({ path: '/interviewer/prep', query: { id: String(interviewId) } })
}

function handleJoinMeeting(item: InterviewListItem, event: Event) {
  event.stopPropagation()
  if (!item.meetingUrl) return
  openMeeting(item.meetingUrl)
}

async function handleCopyMeeting(item: InterviewListItem, event: Event) {
  event.stopPropagation()
  const ok = await copyMeetingInfo(buildMeetingCopyText(item))
  copyTip.value = ok ? '会议信息已复制' : '复制失败，请手动复制'
  setTimeout(() => {
    copyTip.value = ''
  }, 2000)
}

onMounted(loadData)
</script>

<template>
  <div data-cmp="InterviewerWorkbench" class="h-full overflow-y-auto scrollbar-thin p-8">
    <div class="max-w-5xl mx-auto">
      <div class="mb-8">
        <h1 class="text-2xl font-black text-foreground">工作台</h1>
        <p class="text-muted-foreground mt-1">{{ headerHint }}</p>
      </div>

      <p v-if="errorMsg" class="text-xs text-red-600 mb-4">{{ errorMsg }}</p>
      <p v-if="copyTip" class="text-xs text-brand-green mb-4">{{ copyTip }}</p>

      <div class="flex gap-4 mb-8">
        <div
          v-for="s in statCards"
          :key="s.label"
          class="flex-1 bg-card p-5 shadow-card border border-border"
        >
          <div :class="['w-10 h-10 rounded-xl flex items-center justify-center mb-3', s.color]">
            <component :is="s.icon" :size="18" />
          </div>
          <div class="text-2xl font-black text-foreground">{{ loading ? '—' : s.value }}</div>
          <div class="text-sm text-muted-foreground mt-1">{{ s.label }}</div>
        </div>
      </div>

      <div class="bg-card p-5 shadow-card border border-border mb-6">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-2">
            <Calendar :size="16" class="text-brand-blue" />
            <span class="text-base font-bold text-foreground">今日日程</span>
          </div>
          <button
            type="button"
            class="text-xs text-brand-blue hover:underline"
            @click="router.push('/interviewer/interviews')"
          >
            查看全部
          </button>
        </div>

        <p v-if="loading" class="text-sm text-muted-foreground py-6 text-center">加载中...</p>
        <div
          v-else-if="todaySchedule.length === 0"
          class="text-sm text-muted-foreground py-8 text-center border border-dashed border-border rounded-xl"
        >
          今日暂无面试安排
        </div>
        <div v-else class="space-y-3">
          <div
            v-for="iv in todaySchedule"
            :key="iv.interviewId"
            class="flex items-center gap-4 p-4 rounded-xl border border-border hover:border-brand-blue/30 transition-colors cursor-pointer"
            @click="openDetail(iv.interviewId)"
          >
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <span class="text-sm font-bold text-foreground">{{ iv.candidateName }}</span>
                <span
                  :class="['text-xs px-2 py-0.5 rounded-full border', interviewStatusClass(iv.status)]"
                >
                  {{ iv.roundTypeLabel }}
                </span>
              </div>
              <div class="text-xs text-muted-foreground flex flex-wrap gap-3">
                <span>{{ iv.jobTitle }}</span>
                <span>{{ formatInterviewDateTime(iv.scheduledStart) }}</span>
                <span class="flex items-center gap-1"><Video :size="11" />{{ iv.interviewModeLabel }}</span>
              </div>
            </div>
            <div class="flex items-center gap-2 flex-shrink-0">
              <button
                v-if="canJoinMeeting(iv)"
                type="button"
                class="flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium gradient-blue text-white"
                @click="handleJoinMeeting(iv, $event)"
              >
                <ExternalLink :size="12" />
                进入会议
              </button>
              <button
                v-if="iv.meetingUrl"
                type="button"
                class="flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium border border-border hover:bg-muted"
                @click="handleCopyMeeting(iv, $event)"
              >
                <Copy :size="12" />
                复制
              </button>
              <button
                type="button"
                class="flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium border border-brand-purple/30 text-brand-purple hover:bg-brand-tint-2"
                @click.stop="openPrep(iv.interviewId)"
              >
                <Sparkles :size="12" />
                准备
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="upcomingPending.length" class="bg-card p-5 shadow-card border border-border">
        <div class="flex items-center gap-2 mb-4">
          <ClipboardList :size="16" class="text-brand-purple" />
          <span class="text-base font-bold text-foreground">近期待面试</span>
        </div>
        <div class="space-y-2">
          <button
            v-for="iv in upcomingPending"
            :key="iv.interviewId"
            type="button"
            class="w-full flex items-center justify-between p-3 rounded-xl border border-border hover:border-brand-blue/30 text-left transition-colors"
            @click="openDetail(iv.interviewId)"
          >
            <div class="min-w-0">
              <div class="text-sm font-medium text-foreground truncate">
                {{ iv.candidateName }} · {{ iv.jobTitle }}
              </div>
              <div class="text-xs text-muted-foreground mt-0.5">
                {{ formatInterviewDateTime(iv.scheduledStart) }} · {{ iv.interviewModeLabel }}
              </div>
            </div>
            <ArrowRight :size="14" class="text-muted-foreground flex-shrink-0" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
