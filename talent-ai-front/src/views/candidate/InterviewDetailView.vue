<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChevronLeft, Calendar, MapPin, User, Video, ExternalLink, Briefcase } from 'lucide-vue-next'
import { fetchCandidateInterviewDetail, type InterviewDetail } from '@/api/interview'
import {
  INTERVIEW_MODE,
  INTERVIEW_STATUS,
  formatInterviewDateTime,
  interviewStatusClass,
  interviewStatusLabel,
} from '@/constants/interview'
import { getErrorMessage } from '@/utils/validators'

const router = useRouter()
const route = useRoute()

const detail = ref<InterviewDetail | null>(null)
const loading = ref(false)
const errorMsg = ref('')

const interviewId = computed(() => {
  const id = Number(route.query.id)
  return Number.isFinite(id) && id > 0 ? id : null
})

const canJoinMeeting = computed(
  () =>
    detail.value?.interviewMode === INTERVIEW_MODE.VIDEO &&
    !!detail.value?.meetingUrl &&
    detail.value?.status === INTERVIEW_STATUS.PENDING,
)

async function loadDetail() {
  if (!interviewId.value) {
    errorMsg.value = '缺少面试信息'
    detail.value = null
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    detail.value = await fetchCandidateInterviewDetail(interviewId.value)
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '面试详情加载失败')
    detail.value = null
  } finally {
    loading.value = false
  }
}

function openMeeting() {
  const url = detail.value?.meetingUrl
  if (!url) return
  window.open(url, '_blank', 'noopener,noreferrer')
}

onMounted(loadDetail)
watch(interviewId, loadDetail)
</script>

<template>
  <div data-cmp="CandidateInterviewDetail" class="flex h-full flex-col bg-[#EBF4F0]">
    <div class="flex items-center gap-3 px-4 py-3 border-b border-border bg-card flex-shrink-0">
      <button type="button" class="p-1.5 rounded-lg hover:bg-muted" @click="router.back()">
        <ChevronLeft :size="20" class="text-foreground" />
      </button>
      <span class="text-sm font-semibold text-foreground flex-1">面试详情</span>
    </div>

    <div v-if="loading" class="flex-1 flex items-center justify-center text-xs text-muted-foreground">
      加载中...
    </div>

    <template v-else-if="detail">
      <div class="flex-1 overflow-y-auto scrollbar-thin px-4 py-4 pb-24 space-y-4">
        <div class="bg-card p-4 shadow-card border border-border">
          <div class="flex items-start justify-between gap-2 mb-3">
            <div class="min-w-0">
              <h1 class="text-base font-bold text-foreground">{{ detail.jobTitle }}</h1>
              <p class="text-xs text-muted-foreground mt-1">
                第 {{ detail.roundNo }} 轮 · {{ detail.roundTypeLabel || '面试' }}
              </p>
            </div>
            <span
              class="text-[11px] px-2 py-0.5 rounded-full border shrink-0"
              :class="interviewStatusClass(detail.status)"
            >
              {{ detail.statusLabel || interviewStatusLabel(detail.status) }}
            </span>
          </div>
          <div class="text-xs text-muted-foreground flex items-center gap-1.5">
            <Briefcase :size="12" />
            {{ detail.interviewModeLabel || '面试' }}
          </div>
        </div>

        <div class="bg-card p-4 shadow-card border border-border space-y-3">
          <div class="flex items-start gap-3">
            <Calendar :size="16" class="text-brand-blue mt-0.5 shrink-0" />
            <div>
              <div class="text-xs text-muted-foreground">面试时间</div>
              <div class="text-sm font-medium text-foreground mt-0.5">
                {{ formatInterviewDateTime(detail.scheduledStart) }}
                <span v-if="detail.scheduledEnd" class="text-muted-foreground font-normal">
                  — {{ formatInterviewDateTime(detail.scheduledEnd) }}
                </span>
              </div>
            </div>
          </div>

          <div class="flex items-start gap-3">
            <User :size="16" class="text-brand-purple mt-0.5 shrink-0" />
            <div>
              <div class="text-xs text-muted-foreground">面试官</div>
              <div class="text-sm font-medium text-foreground mt-0.5">
                {{ detail.interviewerName || '—' }}
              </div>
            </div>
          </div>

          <div v-if="detail.interviewMode === INTERVIEW_MODE.VIDEO" class="flex items-start gap-3">
            <Video :size="16" class="text-brand-blue mt-0.5 shrink-0" />
            <div class="flex-1 min-w-0">
              <div class="text-xs text-muted-foreground">视频会议</div>
              <p v-if="detail.meetingUrl" class="text-xs text-foreground mt-0.5 break-all">
                {{ detail.meetingUrl }}
              </p>
              <p v-else class="text-xs text-amber-700 mt-0.5">会议链接待 HR 补充</p>
            </div>
          </div>

          <div v-else-if="detail.location" class="flex items-start gap-3">
            <MapPin :size="16" class="text-brand-green mt-0.5 shrink-0" />
            <div>
              <div class="text-xs text-muted-foreground">面试地点</div>
              <div class="text-sm font-medium text-foreground mt-0.5">{{ detail.location }}</div>
            </div>
          </div>
        </div>

        <div
          v-if="detail.status === INTERVIEW_STATUS.CANCELLED"
          class="text-xs text-muted-foreground bg-muted rounded-xl px-3 py-2 border border-border"
        >
          该场面试已取消，如有疑问请联系招聘 HR。
        </div>
      </div>

      <div v-if="canJoinMeeting" class="flex-shrink-0 px-4 py-4 bg-card border-t border-border">
        <button
          type="button"
          class="w-full py-3.5 rounded-control gradient-blue text-white text-sm font-bold shadow-custom flex items-center justify-center gap-2"
          @click="openMeeting"
        >
          <ExternalLink :size="16" />
          进入视频会议
        </button>
      </div>
    </template>

    <div v-else class="flex-1 flex flex-col items-center justify-center px-4 text-center">
      <p class="text-sm text-muted-foreground mb-3">{{ errorMsg || '面试记录不存在' }}</p>
      <button type="button" class="text-xs text-brand-blue" @click="router.push('/candidate/interviews')">
        返回面试列表
      </button>
    </div>
  </div>
</template>
