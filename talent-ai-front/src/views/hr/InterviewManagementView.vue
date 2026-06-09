<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Plus, Search, User, Clock, Video, MapPin, Sparkles } from 'lucide-vue-next'
import InterviewScheduleDialog from '@/components/hr/InterviewScheduleDialog.vue'
import {
  cancelInterview,
  fetchHrInterviewPage,
  fetchHrInterviewStats,
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

const loading = ref(false)
const errorMsg = ref('')
const keyword = ref('')
const statusFilter = ref<number | ''>('')
const interviews = ref<InterviewListItem[]>([])
const stats = ref<InterviewStats | null>(null)
const scheduleOpen = ref(false)
const cancellingId = ref<number | null>(null)

const statusOptions = [
  { value: '' as const, label: '全部状态' },
  { value: INTERVIEW_STATUS.PENDING, label: '待进行' },
  { value: INTERVIEW_STATUS.COMPLETED, label: '已完成' },
  { value: INTERVIEW_STATUS.TO_SCHEDULE, label: '待安排' },
  { value: INTERVIEW_STATUS.CANCELLED, label: '已取消' },
]

const statCards = computed(() => [
  { label: '今日待进行', value: stats.value?.todayPending ?? 0, color: 'text-brand-blue' },
  { label: '本周总计', value: stats.value?.weekTotal ?? 0, color: 'text-brand-purple' },
  { label: '已完成', value: stats.value?.completed ?? 0, color: 'text-brand-green' },
  { label: '待安排', value: stats.value?.toSchedule ?? 0, color: 'text-brand-orange' },
])

const headerSubtitle = computed(() => {
  const today = stats.value?.todayPending ?? 0
  const week = stats.value?.weekTotal ?? 0
  return `今日 ${today} 场待进行 · 本周 ${week} 场`
})

const sidebarItems = computed(() =>
  interviews.value.filter((x) => x.status === INTERVIEW_STATUS.PENDING).slice(0, 3),
)

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const [pageData, statsData] = await Promise.all([
      fetchHrInterviewPage({
        page: 1,
        size: 50,
        keyword: keyword.value.trim() || undefined,
        status: statusFilter.value === '' ? undefined : Number(statusFilter.value),
      }),
      fetchHrInterviewStats(),
    ])
    interviews.value = pageData.records ?? []
    stats.value = statsData
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '面试列表加载失败')
  } finally {
    loading.value = false
  }
}

async function handleCancel(interviewId: number) {
  if (cancellingId.value) return
  if (!window.confirm('确定取消这场面试吗？')) return
  cancellingId.value = interviewId
  try {
    await cancelInterview(interviewId)
    await loadData()
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '取消失败')
  } finally {
    cancellingId.value = null
  }
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(keyword, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => loadData(), 300)
})

watch(statusFilter, () => loadData())

onMounted(() => loadData())
</script>

<template>
  <div data-cmp="InterviewManagement" class="flex h-full" style="height: calc(100vh - 64px)">
    <div class="flex-1 p-6 flex flex-col gap-4 overflow-auto scrollbar-thin min-w-0">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-xl font-bold text-foreground">面试管理</h1>
          <p class="text-sm text-muted-foreground mt-0.5">{{ headerSubtitle }}</p>
        </div>
        <button
          type="button"
          class="flex items-center gap-2 px-4 py-2.5 rounded-control gradient-blue text-white text-sm font-medium"
          @click="scheduleOpen = true"
        >
          <Plus :size="16" />
          <span>安排面试</span>
        </button>
      </div>

      <div class="flex gap-4">
        <div
          v-for="s in statCards"
          :key="s.label"
          class="flex-1 bg-card rounded-xl p-4 shadow-card"
        >
          <div :class="['text-2xl font-bold', s.color]">{{ s.value }}</div>
          <div class="text-xs text-muted-foreground mt-1">{{ s.label }}</div>
        </div>
      </div>

      <div class="flex items-center gap-3">
        <div class="flex items-center gap-2 bg-card rounded-lg px-3 py-2 border border-border flex-1 max-w-sm">
          <Search :size="14" class="text-muted-foreground" />
          <input
            v-model="keyword"
            class="bg-transparent text-sm outline-none flex-1 placeholder:text-muted-foreground"
            placeholder="搜索候选人或岗位"
          />
        </div>
        <select
          v-model="statusFilter"
          class="px-3 py-2 rounded-lg bg-card border border-border text-xs text-muted-foreground outline-none"
        >
          <option v-for="opt in statusOptions" :key="String(opt.value)" :value="opt.value">
            {{ opt.label }}
          </option>
        </select>
      </div>

      <p v-if="errorMsg" class="text-xs text-red-600">{{ errorMsg }}</p>
      <p v-if="loading" class="text-sm text-muted-foreground">加载中...</p>

      <div class="bg-card shadow-card overflow-hidden">
        <table class="w-full">
          <thead>
            <tr class="border-b border-border bg-muted/50">
              <th
                v-for="h in ['候选人', '应聘岗位', '面试轮次', '面试官', '时间', '方式', '状态', '评分', '操作']"
                :key="h"
                class="text-left text-xs font-medium text-muted-foreground px-4 py-3"
              >
                {{ h }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!loading && interviews.length === 0">
              <td colspan="9" class="px-4 py-8 text-center text-sm text-muted-foreground">暂无面试记录</td>
            </tr>
            <tr
              v-for="iv in interviews"
              :key="iv.interviewId"
              class="border-b border-border hover:bg-muted/30 transition-colors"
            >
              <td class="px-4 py-3">
                <div class="flex items-center gap-2">
                  <div class="w-7 h-7 rounded-full gradient-blue flex items-center justify-center">
                    <User :size="12" class="text-white" />
                  </div>
                  <span class="text-sm font-medium text-foreground">{{ iv.candidateName }}</span>
                </div>
              </td>
              <td class="px-4 py-3 text-sm text-muted-foreground">{{ iv.jobTitle }}</td>
              <td class="px-4 py-3">
                <span class="text-xs px-2 py-1 rounded-full bg-accent text-accent-foreground">
                  {{ iv.roundTypeLabel }}
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-muted-foreground">{{ iv.interviewerName }}</td>
              <td class="px-4 py-3">
                <div class="flex items-center gap-1 text-xs text-foreground">
                  <Clock :size="11" class="text-muted-foreground" />
                  <span>{{ formatInterviewDateTime(iv.scheduledStart) }}</span>
                </div>
              </td>
              <td class="px-4 py-3">
                <div class="flex items-center gap-1 text-xs text-muted-foreground">
                  <Video v-if="iv.interviewMode === 1" :size="12" />
                  <MapPin v-else-if="iv.interviewMode === 2" :size="12" />
                  <Video v-else :size="12" />
                  <span>{{ iv.interviewModeLabel }}</span>
                </div>
              </td>
              <td class="px-4 py-3">
                <span
                  :class="[
                    'text-xs px-2 py-1 rounded-full border',
                    interviewStatusClass(iv.status),
                  ]"
                >
                  {{ iv.statusLabel || interviewStatusLabel(iv.status) }}
                </span>
              </td>
              <td class="px-4 py-3">
                <div v-if="iv.totalScore != null && Number(iv.totalScore) > 0" class="flex items-center gap-1">
                  <span class="text-sm font-bold text-brand-purple">{{ iv.totalScore }}</span>
                  <span class="text-xs text-muted-foreground">分</span>
                </div>
                <span v-else class="text-xs text-muted-foreground">—</span>
              </td>
              <td class="px-4 py-3">
                <button
                  v-if="iv.status === INTERVIEW_STATUS.PENDING"
                  type="button"
                  class="text-xs text-red-600 hover:underline disabled:opacity-50"
                  :disabled="cancellingId === iv.interviewId"
                  @click="handleCancel(iv.interviewId)"
                >
                  {{ cancellingId === iv.interviewId ? '取消中...' : '取消' }}
                </button>
                <span v-else class="text-xs text-muted-foreground">—</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="w-72 flex-shrink-0 border-l border-border bg-card p-5 flex flex-col gap-4 overflow-y-auto scrollbar-thin">
      <div class="flex items-center gap-2">
        <Sparkles :size="16" class="text-brand-purple" />
        <span class="text-sm font-semibold text-foreground">待进行面试</span>
      </div>
      <div class="bg-accent rounded-xl p-4 border border-brand-border/50">
        <div v-if="sidebarItems.length === 0" class="text-xs text-muted-foreground">暂无待进行面试</div>
        <div v-else class="space-y-3">
          <div
            v-for="iv in sidebarItems"
            :key="iv.interviewId"
            class="border-b border-brand-border/50 pb-3 last:border-0 last:pb-0"
          >
            <div class="flex items-center justify-between mb-1">
              <span class="text-xs font-medium text-foreground">{{ iv.candidateName }}</span>
              <span class="text-xs text-brand-blue">待进行</span>
            </div>
            <div class="text-xs text-muted-foreground">{{ iv.jobTitle }} · {{ iv.roundTypeLabel }}</div>
            <div class="text-xs text-muted-foreground">{{ formatInterviewDateTime(iv.scheduledStart) }}</div>
          </div>
        </div>
      </div>
    </div>

    <InterviewScheduleDialog
      :open="scheduleOpen"
      @close="scheduleOpen = false"
      @success="loadData"
    />
  </div>
</template>
