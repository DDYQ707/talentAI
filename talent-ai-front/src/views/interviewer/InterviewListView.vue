<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  Calendar,
  Clock,
  User,
  Video,
  Sparkles,
  CheckCircle,
  ChevronRight,
  Briefcase,
  Star,
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
  interviewStatusLabel,
} from '@/constants/interview'
import { getErrorMessage } from '@/utils/validators'

const router = useRouter()
const loading = ref(false)
const errorMsg = ref('')
const keyword = ref('')
const statusTab = ref<number | ''>('')
const interviews = ref<InterviewListItem[]>([])
const stats = ref<InterviewStats | null>(null)

const tabs = [
  { key: '' as const, label: '全部' },
  { key: INTERVIEW_STATUS.PENDING, label: '待面试' },
  { key: INTERVIEW_STATUS.COMPLETED, label: '已完成' },
]

const statCards = computed(() => [
  {
    label: '今日待进行',
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
  {
    label: '待进行总数',
    value: stats.value?.todayPending ?? 0,
    icon: Star,
    color: 'text-brand-orange bg-orange-50',
  },
])

const headerHint = computed(() => {
  const today = stats.value?.todayPending ?? 0
  return today > 0 ? `您好，今天有 ${today} 场面试待进行` : '您好，今日暂无待进行面试'
})

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const [pageData, statsData] = await Promise.all([
      fetchMyInterviewPage({
        page: 1,
        size: 50,
        keyword: keyword.value.trim() || undefined,
        status: statusTab.value === '' ? undefined : Number(statusTab.value),
      }),
      fetchMyInterviewStats(),
    ])
    interviews.value = pageData.records ?? []
    stats.value = statsData
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '面试列表加载失败')
  } finally {
    loading.value = false
  }
}

function openDetail(interviewId: number) {
  router.push({ path: '/interviewer/detail', query: { id: String(interviewId) } })
}

watch(statusTab, () => loadData())

onMounted(() => loadData())
</script>

<template>
  <div data-cmp="InterviewList" class="p-8 h-full overflow-y-auto scrollbar-thin">
    <div class="max-w-5xl mx-auto">
      <div class="flex items-center justify-between mb-8">
        <div>
          <h1 class="text-2xl font-black text-foreground">我的面试列表</h1>
          <p class="text-muted-foreground mt-1">{{ headerHint }}</p>
        </div>
        <button
          type="button"
          class="flex items-center gap-2 px-4 py-2 gradient-purple rounded-xl text-white text-sm font-medium shadow-custom"
          @click="router.push('/interviewer/ai-mode')"
        >
          <Sparkles :size="15" />
          <span>AI面试官模式</span>
        </button>
      </div>

      <div class="flex gap-4 mb-8">
        <div
          v-for="s in statCards"
          :key="s.label"
          class="flex-1 bg-card p-5 shadow-card border border-border"
        >
          <div :class="['w-10 h-10 rounded-xl flex items-center justify-center mb-3', s.color]">
            <component :is="s.icon" :size="18" />
          </div>
          <div class="text-2xl font-black text-foreground">{{ s.value }}</div>
          <div class="text-sm text-muted-foreground mt-1">{{ s.label }}</div>
        </div>
      </div>

      <div class="flex gap-2 mb-6">
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
                <div v-if="iv.totalScore != null && Number(iv.totalScore) > 0" class="flex items-center gap-1 text-xs">
                  <Star :size="11" class="text-brand-orange" />
                  <span class="font-bold text-brand-purple">{{ iv.totalScore }}分</span>
                </div>
              </div>
              <div class="flex items-center gap-4 text-sm text-muted-foreground mb-2 flex-wrap">
                <span class="flex items-center gap-1"><Briefcase :size="13" />{{ iv.jobTitle }}</span>
                <span class="flex items-center gap-1"><Clock :size="13" />{{ formatInterviewDateTime(iv.scheduledStart) }}</span>
                <span class="flex items-center gap-1"><Video :size="13" />{{ iv.interviewModeLabel }}</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-xs px-2 py-0.5 rounded-full bg-accent text-accent-foreground">
                  {{ iv.roundTypeLabel }}
                </span>
                <ChevronRight :size="16" class="text-muted-foreground flex-shrink-0" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
