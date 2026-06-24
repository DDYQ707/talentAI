<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Calendar, ChevronLeft, ChevronRight, MapPin, User, Video } from 'lucide-vue-next'
import { fetchCandidateInterviewPage, type InterviewListItem } from '@/api/interview'
import {
  INTERVIEW_MODE,
  formatInterviewDateTime,
  interviewStatusClass,
  interviewStatusLabel,
} from '@/constants/interview'
import { getErrorMessage } from '@/utils/validators'

const router = useRouter()
const loading = ref(false)
const errorMsg = ref('')
const items = ref<InterviewListItem[]>([])
const total = ref(0)

async function loadInterviews() {
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await fetchCandidateInterviewPage({ page: 1, size: 50 })
    items.value = data.records ?? []
    total.value = data.total ?? items.value.length
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '面试列表加载失败')
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function goDetail(item: InterviewListItem) {
  router.push({ path: '/candidate/interview', query: { id: String(item.interviewId) } })
}

function goBack() {
  router.push('/candidate/profile')
}

onMounted(loadInterviews)
</script>

<template>
  <div data-cmp="CandidateInterviews" class="flex h-full flex-col bg-[#EBF4F0]">
    <div class="px-4 py-4 bg-card border-b border-border flex-shrink-0 flex items-center gap-2">
      <button type="button" class="p-1 -ml-1 rounded-lg hover:bg-muted" @click="goBack">
        <ChevronLeft :size="20" class="text-foreground" />
      </button>
      <div class="flex-1 min-w-0">
        <h1 class="text-base font-bold text-foreground">我的面试</h1>
        <p class="text-xs text-muted-foreground mt-0.5">
          {{ loading ? '加载中...' : `共 ${total} 场面试安排` }}
        </p>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto px-4 py-4 scrollbar-thin space-y-3">
      <p v-if="errorMsg" class="text-xs text-red-600 bg-red-50 border border-red-100 rounded-xl px-3 py-2">
        {{ errorMsg }}
      </p>

      <div
        v-if="!loading && items.length === 0"
        class="rounded-2xl border border-border bg-card p-8 text-center shadow-card"
      >
        <Calendar :size="32" class="mx-auto text-muted-foreground/50 mb-2" />
        <p class="text-sm text-muted-foreground">暂无面试安排</p>
        <p class="text-xs text-muted-foreground mt-1">通过初筛后，HR 会在这里展示面试时间、地点与会议链接</p>
      </div>

      <button
        v-for="item in items"
        :key="item.interviewId"
        type="button"
        class="w-full text-left rounded-2xl border border-border bg-card p-4 shadow-card hover:border-brand-blue/30 transition-colors"
        @click="goDetail(item)"
      >
        <div class="flex items-start justify-between gap-2 mb-2">
          <div class="min-w-0">
            <div class="text-sm font-bold text-foreground truncate">{{ item.jobTitle }}</div>
            <div class="text-xs text-muted-foreground mt-0.5">
              第 {{ item.roundNo }} 轮 · {{ item.roundTypeLabel || '面试' }}
            </div>
          </div>
          <span
            class="text-[11px] px-2 py-0.5 rounded-full border shrink-0"
            :class="interviewStatusClass(item.status)"
          >
            {{ item.statusLabel || interviewStatusLabel(item.status) }}
          </span>
        </div>

        <div class="space-y-1.5 text-xs text-muted-foreground">
          <div class="flex items-center gap-1.5">
            <Calendar :size="12" class="shrink-0" />
            <span>{{ formatInterviewDateTime(item.scheduledStart) }}</span>
          </div>
          <div v-if="item.interviewMode === INTERVIEW_MODE.VIDEO && item.meetingUrl" class="flex items-center gap-1.5">
            <Video :size="12" class="shrink-0 text-brand-blue" />
            <span class="text-brand-blue truncate">视频面试（详情中可进入会议）</span>
          </div>
          <div v-else-if="item.location" class="flex items-center gap-1.5">
            <MapPin :size="12" class="shrink-0" />
            <span class="truncate">{{ item.location }}</span>
          </div>
          <div class="flex items-center gap-1.5">
            <User :size="12" class="shrink-0" />
            <span>面试官：{{ item.interviewerName || '—' }}</span>
          </div>
        </div>

        <div class="flex items-center justify-end mt-2 text-xs text-brand-blue">
          查看详情
          <ChevronRight :size="14" />
        </div>
      </button>
    </div>
  </div>
</template>
