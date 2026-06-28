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
import { OFFER_STATUS } from '@/api/offer'
import { RESUME_SCREEN_STATUS } from '@/constants/resume'
import { buildTalentArchivePayload } from '@/utils/talentArchive'
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
const activeTab = ref<'pending' | 'awaitingOffer' | 'awaitingDecision' | 'closed'>('pending')
const schedulePrefill = ref<{
  applicationId: number
  candidateName: string
  jobTitle: string
} | null>(null)
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

const tabOptions = [
  { key: 'pending' as const, label: '待完成' },
  { key: 'awaitingOffer' as const, label: '待录用' },
  { key: 'awaitingDecision' as const, label: '待定' },
  { key: 'closed' as const, label: '已结束' },
]

function isInterviewHold(item: InterviewListItem) {
  if (item.status !== INTERVIEW_STATUS.COMPLETED) return false
  const conclusion = Number(item.evaluationConclusion)
  if (conclusion === INTERVIEW_CONCLUSION.HOLD) return true
  return item.evaluationConclusionLabel === '待定'
}

function isInterviewReject(item: InterviewListItem) {
  if (item.status !== INTERVIEW_STATUS.COMPLETED) return false
  const conclusion = Number(item.evaluationConclusion)
  if (conclusion === INTERVIEW_CONCLUSION.REJECT) return true
  return item.evaluationConclusionLabel === '不通过' || item.evaluationConclusionLabel === '不推荐'
}

function isInterviewPass(item: InterviewListItem) {
  if (item.status !== INTERVIEW_STATUS.COMPLETED) return false
  if (isInterviewHold(item) || isInterviewReject(item)) return false
  const conclusion = Number(item.evaluationConclusion)
  if (conclusion === INTERVIEW_CONCLUSION.PASS) return true
  if (item.evaluationConclusionLabel === '通过' || item.evaluationConclusionLabel === '推荐通过') return true
  if (item.totalScore != null && Number(item.totalScore) > 0) {
    return conclusion !== INTERVIEW_CONCLUSION.REJECT && conclusion !== INTERVIEW_CONCLUSION.HOLD
  }
  return false
}

function hasActiveOffer(item: InterviewListItem) {
  const status = item.offerStatus
  if (status == null) return false
  return (
    status === OFFER_STATUS.PENDING
    || status === OFFER_STATUS.APPROVING
    || status === OFFER_STATUS.APPROVED
    || status === OFFER_STATUS.ISSUED
  )
}

function isTerminalOffer(item: InterviewListItem) {
  const status = item.offerStatus
  if (status == null) return false
  return (
    status === OFFER_STATUS.REJECTED
    || status === OFFER_STATUS.DECLINED
    || status === OFFER_STATUS.ACCEPTED
  )
}

/** 面试通过且尚未进入 Offer 流程（或 HR 撤回后可重新创建） */
function canSendOffer(item: InterviewListItem) {
  if (!isInterviewPass(item)) return false
  const status = item.offerStatus
  if (status == null) return true
  return status === OFFER_STATUS.REVOKED
}

/** 待完成：待面试、待安排 */
const pendingInterviews = computed(() =>
  interviews.value.filter(
    (iv) =>
      iv.status === INTERVIEW_STATUS.PENDING || iv.status === INTERVIEW_STATUS.TO_SCHEDULE,
  ),
)

const completedInterviews = computed(() =>
  interviews.value.filter((iv) => iv.status === INTERVIEW_STATUS.COMPLETED),
)

/** 面试官推荐通过、尚未发放 Offer */
const awaitingOfferInterviews = computed(() =>
  completedInterviews.value.filter((iv) => canSendOffer(iv)),
)

/** 面试官标记待定，等待 HR 决策 */
const awaitingDecisionInterviews = computed(() =>
  completedInterviews.value.filter((iv) => isInterviewHold(iv)),
)

/** 已结束：不推荐，或 Offer 已在流程中 */
const closedInterviews = computed(() =>
  completedInterviews.value.filter(
    (iv) => !canSendOffer(iv) && !isInterviewHold(iv),
  ),
)

const displayedInterviews = computed(() => {
  if (activeTab.value === 'pending') return pendingInterviews.value
  if (activeTab.value === 'awaitingOffer') return awaitingOfferInterviews.value
  if (activeTab.value === 'awaitingDecision') return awaitingDecisionInterviews.value
  return closedInterviews.value
})

const tabCounts = computed(() => ({
  pending: pendingInterviews.value.length,
  awaitingOffer: awaitingOfferInterviews.value.length,
  awaitingDecision: awaitingDecisionInterviews.value.length,
  closed: closedInterviews.value.length,
}))

const statCards = computed(() => [
  { label: '今日待面试', value: stats.value?.todayPending ?? 0, color: 'text-brand-blue' },
  { label: '本周总计', value: stats.value?.weekTotal ?? 0, color: 'text-brand-purple' },
  { label: '待录用', value: awaitingOfferInterviews.value.length, color: 'text-brand-purple' },
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

const sidebarAwaitingOffer = computed(() => awaitingOfferInterviews.value.slice(0, 3))

function emptyTabMessage() {
  if (activeTab.value === 'pending') return '暂无待完成面试'
  if (activeTab.value === 'awaitingOffer') return '暂无待录用候选人（面试官推荐通过后将出现在此处）'
  if (activeTab.value === 'awaitingDecision') return '暂无待定候选人（面试官标记待定后将出现在此处）'
  return '暂无已结束面试（不推荐、Offer 已结束或流程进行中）'
}

function displayStatusLabel(item: InterviewListItem) {
  if (activeTab.value === 'awaitingOffer') {
    return item.evaluationConclusionLabel || '推荐通过'
  }
  if (activeTab.value === 'awaitingDecision' || isInterviewHold(item)) {
    return item.evaluationConclusionLabel || '待定'
  }
  if (isInterviewReject(item)) {
    return item.evaluationConclusionLabel || '不推荐'
  }
  if (hasActiveOffer(item)) {
    return item.offerStatusText || 'Offer 进行中'
  }
  if (item.offerStatus === OFFER_STATUS.DECLINED) {
    return item.offerStatusText || '候选人已拒绝'
  }
  if (item.offerStatus === OFFER_STATUS.ACCEPTED) {
    return item.offerStatusText || '候选人已接受'
  }
  if (item.offerStatus === OFFER_STATUS.REVOKED) {
    return item.offerStatusText || 'Offer 已撤回'
  }
  return item.evaluationConclusionLabel || item.statusLabel || interviewStatusLabel(item.status)
}

function displayStatusClass(item: InterviewListItem) {
  if (activeTab.value === 'awaitingOffer' || canSendOffer(item)) {
    return 'bg-purple-50 text-brand-purple border-purple-200'
  }
  if (activeTab.value === 'awaitingDecision' || isInterviewHold(item)) {
    return 'bg-orange-50 text-brand-orange border-orange-200'
  }
  if (isInterviewReject(item)) {
    return 'bg-red-50 text-red-600 border-red-200'
  }
  if (hasActiveOffer(item)) {
    return 'bg-green-50 text-brand-green border-green-200'
  }
  if (isTerminalOffer(item) || item.offerStatus === OFFER_STATUS.REVOKED) {
    return 'bg-muted text-muted-foreground border-border'
  }
  return interviewStatusClass(item.status)
}

function openScheduleDialog(
  prefill?: { applicationId: number; candidateName: string; jobTitle: string } | null,
) {
  schedulePrefill.value = prefill ?? null
  scheduleOpen.value = true
}

function closeScheduleDialog() {
  scheduleOpen.value = false
  schedulePrefill.value = null
}

function openFollowUpInterview(item: InterviewListItem) {
  openScheduleDialog({
    applicationId: item.applicationId,
    candidateName: item.candidateName,
    jobTitle: item.jobTitle,
  })
}

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const [pageData, statsData] = await Promise.all([
      fetchHrInterviewPage({
        page: 1,
        size: 100,
        keyword: keyword.value.trim() || undefined,
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

function canRejectAfterInterview(item: InterviewListItem) {
  return !hasActiveOffer(item)
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

function buildInterviewSummary(item: InterviewListItem): string | undefined {
  if (!item.evaluationConclusionLabel) return undefined
  return `面试结论：${item.evaluationConclusionLabel}${item.totalScore != null ? `，${item.totalScore}分` : ''}`
}

async function handleReject(item: InterviewListItem) {
  if (!item.resumeId) {
    errorMsg.value = `未找到「${item.candidateName}」的简历，无法淘汰`
    return
  }
  if (!window.confirm(`确定将「${item.candidateName}」标记为已淘汰？`)) return

  let archiveToTalentPool = false
  let archiveReason: string | undefined
  let interviewSummary: string | undefined
  if (window.confirm(`是否将「${item.candidateName}」存入人才库以备后续联系？`)) {
    const reason = window.prompt('请输入归档原因（可选）', '淘汰后归档')
    if (reason !== null) {
      archiveToTalentPool = true
      archiveReason = reason.trim() || '淘汰后归档'
      interviewSummary = buildInterviewSummary(item)
    }
  }

  actionLoadingId.value = item.interviewId
  errorMsg.value = ''
  try {
    await updateHrScreenStatus(item.resumeId, {
      screenStatus: RESUME_SCREEN_STATUS.REJECTED,
      archiveToTalentPool,
      archiveReason,
      interviewSummary,
    })
    await loadData()
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '淘汰失败')
  } finally {
    actionLoadingId.value = null
  }
}

async function archiveInterviewTalent(item: InterviewListItem, defaultReason: string) {
  const reason = window.prompt('请输入归档原因（可选）', defaultReason)
  if (reason === null) return
  try {
    await archiveTalent(
      buildTalentArchivePayload({
        candidateId: item.candidateId,
        candidateName: item.candidateName,
        resumeId: item.resumeId,
        applicationId: item.applicationId,
        appliedJobTitle: item.jobTitle,
        archiveReason: reason.trim() || defaultReason,
        interviewSummary: buildInterviewSummary(item),
      }),
    )
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '存入人才库失败')
  }
}

async function handleArchive(item: InterviewListItem) {
  if (!item.resumeId) {
    errorMsg.value = `未找到「${item.candidateName}」的简历，无法归档`
    return
  }
  actionLoadingId.value = item.interviewId
  errorMsg.value = ''
  try {
    await archiveInterviewTalent(item, '面试后归档')
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
          @click="openScheduleDialog()"
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
          :class="s.label === '待录用' && s.value > 0 ? 'cursor-pointer hover:border-brand-purple/40 border border-transparent transition-colors' : ''"
          @click="s.label === '待录用' && s.value > 0 ? (activeTab = 'awaitingOffer') : undefined"
        >
          <div :class="['text-2xl font-bold', s.color]">{{ s.value }}</div>
          <div class="text-xs text-muted-foreground mt-1">{{ s.label }}</div>
        </div>
      </div>

      <div class="flex items-center gap-3 flex-wrap">
        <div class="flex items-center gap-2 bg-card rounded-lg px-3 py-2 border border-border flex-1 max-w-sm min-w-[200px]">
          <Search :size="14" class="text-muted-foreground" />
          <input
            v-model="keyword"
            class="bg-transparent text-sm outline-none flex-1 placeholder:text-muted-foreground"
            placeholder="搜索候选人或岗位"
          />
        </div>
        <div class="flex items-center gap-1 p-1 bg-muted/50 rounded-lg border border-border">
          <button
            v-for="tab in tabOptions"
            :key="tab.key"
            type="button"
            :class="[
              'px-3 py-1.5 rounded-md text-xs font-medium transition-colors flex items-center gap-1.5',
              activeTab === tab.key
                ? 'bg-card text-foreground shadow-sm'
                : 'text-muted-foreground hover:text-foreground',
            ]"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
            <span
              :class="[
                'text-[10px] px-1.5 py-0.5 rounded-full min-w-[1.25rem] text-center',
                activeTab === tab.key ? 'bg-brand-tint text-brand-blue' : 'bg-muted text-muted-foreground',
              ]"
            >
              {{ tabCounts[tab.key] }}
            </span>
          </button>
        </div>
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
            <tr v-if="!loading && displayedInterviews.length === 0">
              <td colspan="9" class="px-4 py-8 text-center text-sm text-muted-foreground">
                {{ emptyTabMessage() }}
              </td>
            </tr>
            <tr
              v-for="iv in displayedInterviews"
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
                    displayStatusClass(iv),
                  ]"
                >
                  {{ displayStatusLabel(iv) }}
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
                    v-if="isInterviewHold(iv)"
                    type="button"
                    class="text-xs text-brand-orange hover:underline font-medium"
                    @click="openFollowUpInterview(iv)"
                  >
                    安排复试
                  </button>
                  <button
                    v-if="hasInterviewEvaluation(iv)"
                    type="button"
                    class="text-xs text-brand-blue hover:underline font-medium"
                    @click="openEvaluation(iv)"
                  >
                    查看评价
                  </button>
                  <button
                    v-if="canRejectAfterInterview(iv)"
                    type="button"
                    class="text-xs text-red-600 hover:underline disabled:opacity-50"
                    :disabled="isActionLoading(iv.interviewId)"
                    @click="handleReject(iv)"
                  >
                    {{ isActionLoading(iv.interviewId) ? '处理中...' : '淘汰' }}
                  </button>
                  <button
                    v-if="canSendOffer(iv)"
                    type="button"
                    class="text-xs text-brand-purple hover:underline font-medium"
                    @click="goOffer(iv)"
                  >
                    发送Offer
                  </button>
                  <button
                    v-if="hasActiveOffer(iv)"
                    type="button"
                    class="text-xs text-brand-green hover:underline font-medium"
                    @click="goOffer(iv)"
                  >
                    {{ iv.offerStatusText || '查看Offer' }}
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

      <div class="flex items-center justify-between gap-2">
        <div class="flex items-center gap-2">
          <Sparkles :size="16" class="text-brand-purple" />
          <span class="text-sm font-semibold text-foreground">待录用</span>
        </div>
        <button
          v-if="awaitingOfferInterviews.length > 0"
          type="button"
          class="text-[11px] text-brand-purple hover:underline"
          @click="activeTab = 'awaitingOffer'"
        >
          查看全部
        </button>
      </div>
      <div class="bg-purple-50/80 rounded-xl p-4 border border-purple-200/60">
        <div v-if="sidebarAwaitingOffer.length === 0" class="text-xs text-muted-foreground">
          面试官推荐通过后，候选人将出现在此处等待发放 Offer
        </div>
        <div v-else class="space-y-3">
          <div
            v-for="iv in sidebarAwaitingOffer"
            :key="`await-${iv.interviewId}`"
            class="border-b border-purple-200/50 pb-3 last:border-0 last:pb-0"
          >
            <div class="flex items-center justify-between mb-1">
              <span class="text-xs font-medium text-foreground">{{ iv.candidateName }}</span>
              <span class="text-xs text-brand-purple">待录用</span>
            </div>
            <div class="text-xs text-muted-foreground">{{ iv.jobTitle }} · {{ iv.evaluationConclusionLabel || '推荐通过' }}</div>
            <button
              type="button"
              class="mt-1.5 text-xs text-brand-purple hover:underline font-medium"
              @click="goOffer(iv)"
            >
              发送 Offer
            </button>
          </div>
        </div>
      </div>
    </div>

    <InterviewScheduleDialog
      :open="scheduleOpen"
      :application-id="schedulePrefill?.applicationId ?? null"
      :candidate-name="schedulePrefill?.candidateName ?? ''"
      :job-title="schedulePrefill?.jobTitle ?? ''"
      @close="closeScheduleDialog"
      @success="() => { closeScheduleDialog(); loadData() }"
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
