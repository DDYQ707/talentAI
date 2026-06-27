<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search, User, Clock, Video, MapPin, Sparkles, X } from 'lucide-vue-next'
import InterviewScheduleDialog from '@/components/hr/InterviewScheduleDialog.vue'
import InterviewReassignDialog from '@/components/hr/InterviewReassignDialog.vue'
import HrInterviewEvaluationPanel from '@/components/hr/HrInterviewEvaluationPanel.vue'
import {
  cancelInterview,
  fetchHrInterviewDetail,
  fetchHrInterviewPage,
  fetchHrInterviewStats,
  type InterviewDetail,
  type InterviewListItem,
  type InterviewStats,
} from '@/api/interview'
import { updateHrScreenStatus } from '@/api/hrResume'
import { archiveTalent } from '@/api/talentPool'
import { RESUME_SCREEN_STATUS } from '@/constants/resume'
import {
  INTERVIEW_CONCLUSION,
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
const statusFilter = ref<number | ''>('')
const interviews = ref<InterviewListItem[]>([])
const stats = ref<InterviewStats | null>(null)
const scheduleOpen = ref(false)
const cancellingId = ref<number | null>(null)
const reassignOpen = ref(false)
const reassignTarget = ref<InterviewListItem | null>(null)
const actionLoadingId = ref<number | null>(null)
const evaluationOpen = ref(false)
const evaluationLoading = ref(false)
const evaluationDetail = ref<InterviewDetail | null>(null)

const statusOptions = [
  { value: '' as const, label: '全部状态' },
  { value: INTERVIEW_STATUS.PENDING, label: '待面试' },
  { value: INTERVIEW_STATUS.COMPLETED, label: '已完成' },
  { value: INTERVIEW_STATUS.TO_SCHEDULE, label: '待安排' },
  { value: INTERVIEW_STATUS.CANCELLED, label: '已取消' },
]

const statCards = computed(() => [
  { label: '今日待面试', value: stats.value?.todayPending ?? 0, color: 'text-brand-blue' },
  { label: '本周总计', value: stats.value?.weekTotal ?? 0, color: 'text-brand-purple' },
  { label: '已完成', value: stats.value?.completed ?? 0, color: 'text-brand-green' },
  { label: '待安排', value: stats.value?.toSchedule ?? 0, color: 'text-brand-orange' },
])

const headerSubtitle = computed(() => {
  const today = stats.value?.todayPending ?? 0
  const week = stats.value?.weekTotal ?? 0
  return `今日 ${today} 场待面试 · 本周 ${week} 场`
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
  if (!window.confirm('确定取消这场面试吗？若无其他待面试安排，候选人将回退为「待筛选」。')) return
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

function openReassign(item: InterviewListItem) {
  reassignTarget.value = item
  reassignOpen.value = true
}

function closeReassign() {
  reassignOpen.value = false
  reassignTarget.value = null
}

function isInterviewPass(item: InterviewListItem) {
  if (item.status !== INTERVIEW_STATUS.COMPLETED) return false
  const conclusion = Number(item.evaluationConclusion)
  if (conclusion === INTERVIEW_CONCLUSION.PASS) return true
  if (item.evaluationConclusionLabel === '通过') return true
  // 兼容列表接口未回填 conclusion 时：有评分且非淘汰/待定
  if (item.totalScore != null && Number(item.totalScore) > 0) {
    return conclusion !== INTERVIEW_CONCLUSION.REJECT && conclusion !== INTERVIEW_CONCLUSION.HOLD
  }
  return false
}

function goCandidateDetail(item: InterviewListItem) {
  if (item.resumeId) {
    router.push({ path: '/hr/resumes/detail', query: { id: String(item.resumeId) } })
    return
  }
  errorMsg.value = `未找到「${item.candidateName}」的简历，请从左侧「简历管理」搜索进入`
  router.push({ path: '/hr/resumes' })
}

function goOffer(item: InterviewListItem) {
  router.push({
    path: '/hr/offers',
    query: {
      applicationId: String(item.applicationId),
      candidateName: item.candidateName ?? '',
      candidateId: String(item.candidateId),
      jobId: String(item.jobId),
      jobTitle: item.jobTitle ?? '',
    },
  })
}

async function handleReject(item: InterviewListItem) {
  if (!item.resumeId) {
    errorMsg.value = `未找到「${item.candidateName}」的简历，无法淘汰`
    return
  }
  if (!window.confirm(`确定将「${item.candidateName}」标记为已淘汰？`)) return
  actionLoadingId.value = item.interviewId
  errorMsg.value = ''
  try {
    await updateHrScreenStatus(item.resumeId, RESUME_SCREEN_STATUS.REJECTED)
    await loadData()
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '淘汰失败')
  } finally {
    actionLoadingId.value = null
  }
}

async function handleArchive(item: InterviewListItem) {
  const reason = window.prompt('请输入归档原因（可选）', '面试后归档')
  if (reason === null) return
  actionLoadingId.value = item.interviewId
  errorMsg.value = ''
  try {
    await archiveTalent({
      candidateId: item.candidateId,
      candidateName: item.candidateName,
      resumeId: item.resumeId,
      sourceApplicationId: item.applicationId,
      archiveReason: reason.trim() || '面试后归档',
    })
    await loadData()
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '存入人才库失败')
  } finally {
    actionLoadingId.value = null
  }
}

function isActionLoading(interviewId: number) {
  return actionLoadingId.value === interviewId
}

function hasInterviewEvaluation(item: InterviewListItem) {
  return item.status === INTERVIEW_STATUS.COMPLETED && (!!item.evaluation || (item.totalScore != null && Number(item.totalScore) > 0))
}

async function openEvaluation(item: InterviewListItem) {
  evaluationOpen.value = true
  evaluationLoading.value = true
  evaluationDetail.value = null
  errorMsg.value = ''
  try {
    evaluationDetail.value = await fetchHrInterviewDetail(item.interviewId)
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '加载面试评价失败')
    evaluationOpen.value = false
  } finally {
    evaluationLoading.value = false
  }
}

function closeEvaluation() {
  evaluationOpen.value = false
  evaluationDetail.value = null
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
                <div v-if="iv.totalScore != null && Number(iv.totalScore) > 0" class="space-y-1">
                  <div class="flex items-center gap-1">
                    <span class="text-sm font-bold text-brand-purple">{{ iv.totalScore }}</span>
                    <span class="text-xs text-muted-foreground">分</span>
                  </div>
                  <span
                    v-if="iv.evaluationConclusionLabel"
                    :class="[
                      'text-[10px] px-1.5 py-0.5 rounded-full border inline-block',
                      isInterviewPass(iv)
                        ? 'bg-green-50 text-brand-green border-green-200'
                        : iv.evaluationConclusion === INTERVIEW_CONCLUSION.REJECT
                          ? 'bg-red-50 text-red-600 border-red-200'
                          : 'bg-orange-50 text-brand-orange border-orange-200',
                    ]"
                  >
                    {{ iv.evaluationConclusionLabel }}
                  </span>
                </div>
                <span v-else class="text-xs text-muted-foreground">—</span>
              </td>
              <td class="px-4 py-3">
                <div v-if="iv.status === INTERVIEW_STATUS.PENDING" class="flex items-center gap-2">
                  <button
                    type="button"
                    class="text-xs text-brand-blue hover:underline"
                    @click="openReassign(iv)"
                  >
                    改派
                  </button>
                  <button
                    type="button"
                    class="text-xs text-red-600 hover:underline disabled:opacity-50"
                    :disabled="cancellingId === iv.interviewId"
                    @click="handleCancel(iv.interviewId)"
                  >
                    {{ cancellingId === iv.interviewId ? '取消中...' : '取消' }}
                  </button>
                </div>
                <div v-else-if="iv.status === INTERVIEW_STATUS.COMPLETED" class="flex flex-wrap items-center gap-x-2 gap-y-1">
                  <button
                    v-if="hasInterviewEvaluation(iv)"
                    type="button"
                    class="text-xs text-brand-blue hover:underline font-medium"
                    @click="openEvaluation(iv)"
                  >
                    查看评价
                  </button>
                  <button
                    type="button"
                    class="text-xs text-red-600 hover:underline disabled:opacity-50"
                    :disabled="isActionLoading(iv.interviewId)"
                    @click="handleReject(iv)"
                  >
                    {{ isActionLoading(iv.interviewId) ? '处理中...' : '淘汰' }}
                  </button>
                  <button
                    v-if="isInterviewPass(iv)"
                    type="button"
                    class="text-xs text-brand-purple hover:underline font-medium"
                    @click="goOffer(iv)"
                  >
                    发送Offer
                  </button>
                  <button
                    type="button"
                    class="text-xs text-muted-foreground hover:underline disabled:opacity-50"
                    :disabled="isActionLoading(iv.interviewId)"
                    @click="handleArchive(iv)"
                  >
                    存入人才库
                  </button>
                  <button
                    type="button"
                    class="text-xs text-brand-blue hover:underline"
                    @click="goCandidateDetail(iv)"
                  >
                    查看候选人
                  </button>
                </div>
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
        <span class="text-sm font-semibold text-foreground">待面试</span>
      </div>
      <div class="bg-accent rounded-xl p-4 border border-brand-border/50">
        <div v-if="sidebarItems.length === 0" class="text-xs text-muted-foreground">暂无待面试</div>
        <div v-else class="space-y-3">
          <div
            v-for="iv in sidebarItems"
            :key="iv.interviewId"
            class="border-b border-brand-border/50 pb-3 last:border-0 last:pb-0"
          >
            <div class="flex items-center justify-between mb-1">
              <span class="text-xs font-medium text-foreground">{{ iv.candidateName }}</span>
              <span class="text-xs text-brand-blue">待面试</span>
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

    <InterviewReassignDialog
      :open="reassignOpen"
      :interview-id="reassignTarget?.interviewId ?? null"
      :candidate-name="reassignTarget?.candidateName"
      :current-interviewer-name="reassignTarget?.interviewerName"
      @close="closeReassign"
      @success="loadData"
    />

    <div
      v-if="evaluationOpen"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      @click.self="closeEvaluation"
    >
      <div class="bg-card rounded-2xl shadow-panel w-full max-w-lg p-5 border border-border max-h-[85vh] overflow-y-auto">
        <div class="flex items-center justify-between mb-4">
          <div>
            <h3 class="text-sm font-bold text-foreground">面试官评价</h3>
            <p v-if="evaluationDetail" class="text-xs text-muted-foreground mt-0.5">
              {{ evaluationDetail.candidateName }} · {{ evaluationDetail.jobTitle }}
            </p>
          </div>
          <button type="button" class="p-1 rounded-lg hover:bg-muted text-muted-foreground" @click="closeEvaluation">
            <X :size="16" />
          </button>
        </div>
        <p v-if="evaluationLoading" class="text-sm text-muted-foreground py-8 text-center">加载中...</p>
        <HrInterviewEvaluationPanel
          v-else-if="evaluationDetail"
          :evaluation="evaluationDetail.evaluation"
          :interviewer-name="evaluationDetail.interviewerName"
          :round-type-label="evaluationDetail.roundTypeLabel"
          :scheduled-start="evaluationDetail.scheduledStart"
        />
      </div>
    </div>
  </div>
</template>
