<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  Clock,
  User,
  Video,
  Sparkles,
  CheckCircle,
  ChevronRight,
  Briefcase,
  Star,
  ExternalLink,
  Copy,
} from 'lucide-vue-next'
import {
  fetchAiMatchScoresByApplications,
  fetchMyInterviewPage,
  type InterviewListItem,
} from '@/api/interview'
import {
  INTERVIEW_MODE,
  INTERVIEW_STATUS,
  formatInterviewDateTime,
  interviewStatusClass,
  interviewStatusLabel,
} from '@/constants/interview'
import { canJoinMeeting, copyMeetingInfo, openMeeting } from '@/utils/interviewMeeting'
import { getErrorMessage } from '@/utils/validators'

const router = useRouter()
const loading = ref(false)
const errorMsg = ref('')
const copyTip = ref('')
const keyword = ref('')
const statusTab = ref<number | ''>('')
const interviews = ref<InterviewListItem[]>([])
const aiMatchScores = ref<Record<number, number>>({})

const tabs = [
  { key: '' as const, label: '全部' },
  { key: INTERVIEW_STATUS.PENDING, label: '待面试' },
  { key: INTERVIEW_STATUS.COMPLETED, label: '已完成' },
]

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const pageData = await fetchMyInterviewPage({
      page: 1,
      size: 50,
      keyword: keyword.value.trim() || undefined,
      status: statusTab.value === '' ? undefined : Number(statusTab.value),
    })
    interviews.value = pageData.records ?? []
    aiMatchScores.value = await fetchAiMatchScoresByApplications(
      interviews.value.map((item) => item.applicationId),
    )
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '面试列表加载失败')
  } finally {
    loading.value = false
  }
}

function getAiMatchScore(applicationId: number): number | null {
  const score = aiMatchScores.value[applicationId]
  return score != null && score > 0 ? score : null
}

function openDetail(interviewId: number) {
  router.push({ path: '/interviewer/detail', query: { id: String(interviewId) } })
}

function openPrep(interviewId: number, event: Event) {
  event.stopPropagation()
  router.push({ path: '/interviewer/prep', query: { id: String(interviewId) } })
}

function handleJoinMeeting(item: InterviewListItem, event: Event) {
  event.stopPropagation()
  if (!item.meetingUrl) return
  openMeeting(item.meetingUrl)
}

async function handleCopyMeeting(item: InterviewListItem, event: Event) {
  event.stopPropagation()
  const text = [item.candidateName, item.jobTitle, formatInterviewDateTime(item.scheduledStart), item.meetingUrl]
    .filter(Boolean)
    .join(' · ')
  const ok = await copyMeetingInfo(text)
  copyTip.value = ok ? '已复制' : '复制失败'
  setTimeout(() => {
    copyTip.value = ''
  }, 2000)
}

watch(statusTab, () => loadData())

onMounted(() => loadData())
</script>

<template>
  <div data-cmp="InterviewList" class="p-8 h-full overflow-y-auto scrollbar-thin">
    <div class="max-w-5xl mx-auto">
      <div class="mb-8">
        <h1 class="text-2xl font-black text-foreground">我的面试</h1>
        <p class="text-muted-foreground mt-1">查看全部面试任务，进入详情提交评价</p>
      </div>

      <div class="flex gap-2 mb-4">
        <button
          v-for="tab in tabs"
          :key="String(tab.key)"
          type="button"
          :class="[
            'px-4 py-2 rounded-xl text-sm font-medium border transition-colors',
            statusTab === tab.key
              ? 'gradient-blue text-white border-transparent shadow-custom'
              : 'bg-card text-muted-foreground border-border hover:border-brand-blue/30',
          ]"
          @click="statusTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <p v-if="errorMsg" class="text-xs text-red-600 mb-4">{{ errorMsg }}</p>
      <p v-if="copyTip" class="text-xs text-brand-green mb-4">{{ copyTip }}</p>
      <p v-if="loading" class="text-sm text-muted-foreground mb-4">加载中...</p>

      <div v-if="!loading && interviews.length === 0" class="text-center text-sm text-muted-foreground py-12">
        暂无面试任务
      </div>

      <div class="space-y-4">
        <div
          v-for="iv in interviews"
          :key="iv.interviewId"
          role="button"
          class="bg-card p-5 shadow-card border border-border hover:shadow-lg hover:border-brand-blue/20 transition-all cursor-pointer text-left w-full"
          @click="openDetail(iv.interviewId)"
        >
          <div class="flex items-start gap-4">
            <div class="w-12 h-12 rounded-control gradient-blue flex items-center justify-center flex-shrink-0">
              <User :size="20" class="text-white" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between mb-1">
                <div class="flex items-center gap-2">
                  <span class="text-base font-bold text-foreground">{{ iv.candidateName }}</span>
                  <span
                    :class="[
                      'text-xs px-2 py-0.5 rounded-full border',
                      interviewStatusClass(iv.status),
                    ]"
                  >
                    {{ iv.statusLabel || interviewStatusLabel(iv.status) }}
                  </span>
                </div>
                <div class="flex items-center gap-2 flex-shrink-0">
                  <div
                    v-if="getAiMatchScore(iv.applicationId) != null"
                    class="flex items-center gap-1 text-xs px-2 py-0.5 rounded-full bg-brand-tint-2 text-brand-purple"
                  >
                    <Sparkles :size="11" />
                    <span class="font-bold">AI {{ getAiMatchScore(iv.applicationId) }}分</span>
                  </div>
                  <div v-if="iv.totalScore != null && Number(iv.totalScore) > 0" class="flex items-center gap-1 text-xs">
                    <Star :size="11" class="text-brand-orange" />
                    <span class="font-bold text-brand-purple">{{ iv.totalScore }}分</span>
                  </div>
                </div>
              </div>
              <div class="flex items-center gap-4 text-sm text-muted-foreground mb-2 flex-wrap">
                <span class="flex items-center gap-1"><Briefcase :size="13" />{{ iv.jobTitle }}</span>
                <span class="flex items-center gap-1"><Clock :size="13" />{{ formatInterviewDateTime(iv.scheduledStart) }}</span>
                <span class="flex items-center gap-1"><Video :size="13" />{{ iv.interviewModeLabel }}</span>
              </div>
              <div v-if="iv.interviewMode === INTERVIEW_MODE.VIDEO && iv.meetingUrl" class="text-xs text-muted-foreground mb-2 truncate">
                会议：{{ iv.meetingUrl }}
              </div>
              <div class="flex items-center justify-between">
                <span class="text-xs px-2 py-0.5 rounded-full bg-accent text-accent-foreground">
                  {{ iv.roundTypeLabel }}
                </span>
                <div class="flex items-center gap-2">
                  <button
                    v-if="iv.status === INTERVIEW_STATUS.PENDING && canJoinMeeting(iv)"
                    type="button"
                    class="flex items-center gap-1 text-xs px-2 py-1 rounded-lg gradient-blue text-white"
                    @click="handleJoinMeeting(iv, $event)"
                  >
                    <ExternalLink :size="11" />
                    进入会议
                  </button>
                  <button
                    v-if="iv.status === INTERVIEW_STATUS.PENDING && iv.meetingUrl"
                    type="button"
                    class="flex items-center gap-1 text-xs px-2 py-1 rounded-lg border border-border hover:bg-muted"
                    @click="handleCopyMeeting(iv, $event)"
                  >
                    <Copy :size="11" />
                  </button>
                  <button
                    v-if="iv.status === INTERVIEW_STATUS.PENDING"
                    type="button"
                    class="text-xs px-2 py-1 rounded-lg border border-brand-purple/30 text-brand-purple hover:bg-brand-tint-2"
                    @click="openPrep(iv.interviewId, $event)"
                  >
                    去准备
                  </button>
                  <button
                    v-if="iv.status === INTERVIEW_STATUS.COMPLETED"
                    type="button"
                    class="text-xs px-2 py-1 rounded-lg border border-brand-green/30 text-brand-green hover:bg-green-50"
                    @click="openDetail(iv.interviewId)"
                  >
                    <CheckCircle :size="11" class="inline mr-0.5" />
                    查看评价
                  </button>
                  <ChevronRight :size="16" class="text-muted-foreground flex-shrink-0" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
