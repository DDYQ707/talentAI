<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { EChartsOption } from 'echarts'
import {
  User,
  Phone,
  Mail,
  MapPin,
  GraduationCap,
  Briefcase,
  Sparkles,
  Calendar,
  TrendingUp,
  FileText,
  Eye,
  ChevronLeft,
  FolderKanban,
  Award,
  RotateCw,
  ClipboardList,
} from 'lucide-vue-next'
import { fetchHrCandidateBrief } from '@/api/hrCandidate'
import { fetchHrResumeDetail, fetchHrResumePreview, type HrResumeDetail } from '@/api/hrResume'
import { fetchHrJobList, type JobPost } from '@/api/hrJob'
import {
  aiTaskStatusLabel,
  fetchAiMatchLatest,
  fetchAiParseLatest,
  fetchAiProfileByApplication,
  formatMatchErrorMessage,
  formatMatchLevel,
  generateAiProfile,
  retryAiParse,
  triggerAiMatch,
  parseDimensionScores,
  parseJsonStringArray,
  type AiMatchResult,
  type AiParseTaskResult,
  type AiTalentProfile,
} from '@/api/ai'
import { openResumePreview } from '@/api/resume'
import { screenStatusLabel } from '@/constants/resume'
import { formatDegree, formatResumePeriod, skillProficiencyLabel, skillProficiencyPercent } from '@/utils/resumeFormat'
import { formatCertType, formatExperienceType } from '@/constants/onlineResume'
import { getErrorMessage } from '@/utils/validators'
import InterviewScheduleDialog from '@/components/hr/InterviewScheduleDialog.vue'
import HrInterviewEvaluationPanel from '@/components/hr/HrInterviewEvaluationPanel.vue'
import {
  fetchHrInterviewDetail,
  fetchInterviewsByApplication,
  type InterviewListItem,
} from '@/api/interview'
import { INTERVIEW_STATUS } from '@/constants/interview'

const route = useRoute()
const router = useRouter()
const detail = ref<HrResumeDetail | null>(null)
const loading = ref(true)
const errorMsg = ref('')
const pdfPreviewUrl = ref('')
const previewLoading = ref(false)
const aiMatch = ref<AiMatchResult | null>(null)
const aiParse = ref<AiParseTaskResult | null>(null)
const aiProfile = ref<AiTalentProfile | null>(null)
const aiLoading = ref(false)
const profileGenerating = ref(false)
const scheduleOpen = ref(false)
const scheduleSuccessMsg = ref('')
const reparseLoading = ref(false)
const reparseMsg = ref('')
const jobOptions = ref<JobPost[]>([])
const selectedJobId = ref<number | null>(null)
const matchLoading = ref(false)
const matchMsg = ref('')
const applicationInterviews = ref<InterviewListItem[]>([])
let parsePollTimer: ReturnType<typeof setInterval> | null = null
let matchPollTimer: ReturnType<typeof setInterval> | null = null

const resumeId = computed(() => {
  const id = Number(route.query.id)
  return Number.isFinite(id) && id > 0 ? id : null
})

function formatDateTime(iso?: string | null) {
  if (!iso) return '—'
  return iso.slice(0, 10)
}

function goBack() {
  router.push('/hr/resumes')
}

const candidate = computed(() => ({
  name: detail.value?.candidateName ?? '—',
  title: detail.value?.currentTitle ?? detail.value?.resumeName ?? '—',
  exp: detail.value?.resumeType === 'attachment'
    ? `附件 · ${(detail.value?.fileType ?? 'file').toUpperCase()}`
    : '在线简历',
  edu: formatDegree(detail.value?.highestEdu) || '—',
  location: detail.value?.city ?? '—',
  phone: detail.value?.phone ?? '—',
  email: detail.value?.email ?? '—',
  match: aiMatch.value?.matchScore ?? detail.value?.matchScore ?? 0,
  status: screenStatusLabel(detail.value?.screenStatus),
  appliedJob: detail.value?.appliedJobTitle || '暂无投递记录',
  appliedDate: formatDateTime(detail.value?.appliedAt),
  fileName: detail.value?.fileName,
  attachmentId: detail.value?.attachmentId,
}))

const canScheduleInterview = computed(() => !!detail.value?.applicationId && detail.value.applicationId > 0)

const completedInterviewsWithEvaluation = computed(() =>
  applicationInterviews.value.filter(
    (iv) => iv.status === INTERVIEW_STATUS.COMPLETED && (iv.evaluation || iv.totalScore),
  ),
)

async function enrichInterviewEvaluations(list: InterviewListItem[]) {
  return Promise.all(
    list.map(async (iv) => {
      const needsDetail =
        iv.status === INTERVIEW_STATUS.COMPLETED
        && !iv.evaluation
        && (iv.totalScore != null || iv.evaluationConclusion != null)
      if (!needsDetail) return iv
      try {
        const detail = await fetchHrInterviewDetail(iv.interviewId)
        return {
          ...iv,
          evaluation: detail.evaluation ?? iv.evaluation,
          totalScore: detail.totalScore ?? iv.totalScore,
          evaluationConclusion: detail.evaluationConclusion ?? iv.evaluationConclusion,
          evaluationConclusionLabel: detail.evaluationConclusionLabel ?? iv.evaluationConclusionLabel,
        }
      } catch {
        return iv
      }
    }),
  )
}

async function loadApplicationInterviews(applicationId: number) {
  try {
    const list = await fetchInterviewsByApplication(applicationId)
    applicationInterviews.value = await enrichInterviewEvaluations(list)
  } catch {
    applicationInterviews.value = []
  }
}

function handleScheduleSuccess() {
  scheduleSuccessMsg.value = '面试已安排成功'
  void loadDetail()
}

const workExp = computed(() =>
  (detail.value?.workExperiences ?? []).map((w) => ({
    company: w.companyName,
    title: `${formatExperienceType(w.experienceType)} · ${w.jobTitle}`,
    period: formatResumePeriod(w.startDate, w.endDate) || '—',
    desc: w.jobDescription ?? '',
  })),
)

const projectList = computed(() => detail.value?.projects ?? [])

const certificateList = computed(() => detail.value?.certificates ?? [])

const skills = computed(() =>
  (detail.value?.skills ?? []).map((s) => ({
    name: s.skillName,
    level: skillProficiencyPercent(s.proficiencyLevel),
    label: skillProficiencyLabel(s.proficiencyLevel),
  })),
)

const hasAiPortrait = computed(() => {
  if (aiProfile.value?.profileSummary) return true
  if (aiMatch.value?.matchStatus === 2 && (aiMatch.value.matchScore ?? 0) > 0) return true
  return (detail.value?.matchScore ?? 0) > 0
})

const profileTags = computed(() => aiProfile.value?.profileTags ?? [])

const matchAdvantages = computed(() => parseJsonStringArray(aiMatch.value?.advantages))
const matchDisadvantages = computed(() => parseJsonStringArray(aiMatch.value?.disadvantages))
const matchQuestions = computed(() => parseJsonStringArray(aiMatch.value?.suggestedQuestions))

const matchLevelText = computed(() =>
  formatMatchLevel(
    aiMatch.value?.matchScore ?? (detail.value?.matchScore ?? 0),
    aiMatch.value?.matchLevel,
  ),
)

const matchBusy = computed(() => {
  if (matchLoading.value) return true
  const status = aiMatch.value?.matchStatus
  return status === 0 || status === 1
})

const radarData = computed(() => {
  const fromAi = parseDimensionScores(aiMatch.value?.dimensionScores)
  if (fromAi.length > 0) return fromAi
  const base = aiMatch.value?.matchScore ?? detail.value?.matchScore ?? 0
  if (base <= 0) return []
  return [
    { subject: '技术能力', value: Math.min(100, base + 5) },
    { subject: '项目经验', value: Math.min(100, base) },
    { subject: '学习能力', value: Math.min(100, base - 3) },
    { subject: '沟通协作', value: Math.min(100, base - 5) },
    { subject: '稳定性', value: Math.min(100, base - 8) },
    { subject: '薪资匹配', value: Math.min(100, base - 2) },
  ]
})

const aiSummaryText = computed(() => {
  if (aiProfile.value?.profileSummary) {
    return aiProfile.value.profileSummary
  }
  if (aiMatch.value?.matchStatus === 2 && aiMatch.value.matchReason) {
    return aiMatch.value.matchReason
  }
  if (detail.value?.summary) {
    return '基于候选人简历摘要与档案信息，建议结合岗位需求进一步评估。'
  }
  return '暂无 AI 评价，请先完善候选人简历或等待 AI 解析完成。'
})

const parseStatusText = computed(() => aiTaskStatusLabel(aiParse.value?.taskStatus))

const parseBusy = computed(() => {
  const status = aiParse.value?.taskStatus
  return status === 0 || status === 1
})

function stopParsePolling() {
  if (parsePollTimer != null) {
    clearInterval(parsePollTimer)
    parsePollTimer = null
  }
}

function startParsePolling() {
  stopParsePolling()
  parsePollTimer = setInterval(async () => {
    if (!resumeId.value) return
    try {
      const task = await fetchAiParseLatest(resumeId.value)
      aiParse.value = task
      if (task && (task.taskStatus === 2 || task.taskStatus === 3)) {
        stopParsePolling()
        reparseLoading.value = false
        if (task.taskStatus === 2) {
          reparseMsg.value = '解析完成，已更新简历内容'
          await loadDetail()
        } else {
          reparseMsg.value = task.errorMessage || '解析失败，请稍后重试'
        }
      }
    } catch {
      /* 轮询失败时继续等待 */
    }
  }, 3000)
}

async function handleReparse() {
  if (!resumeId.value || reparseLoading.value || parseBusy.value) return
  const data = detail.value
  reparseLoading.value = true
  reparseMsg.value = ''
  errorMsg.value = ''
  try {
    aiParse.value = await retryAiParse({
      resumeId: resumeId.value,
      applicationId: data?.applicationId,
      jobId: data?.jobId,
      candidateId: data?.candidateId,
    })
    reparseMsg.value = '已提交重新解析，请稍候…'
    startParsePolling()
  } catch (e) {
    reparseLoading.value = false
    errorMsg.value = getErrorMessage(e, '重新解析失败')
  }
}

async function loadJobOptions(defaultJobId?: number, defaultTitle?: string) {
  try {
    const data = await fetchHrJobList({ status: 1, size: 100 })
    jobOptions.value = data.records ?? []
  } catch {
    jobOptions.value = []
  }
  if (defaultJobId && !jobOptions.value.some((j) => j.id === defaultJobId)) {
    jobOptions.value.unshift({
      id: defaultJobId,
      title: defaultTitle || `岗位 #${defaultJobId}`,
    })
  }
  if (selectedJobId.value == null && defaultJobId) {
    selectedJobId.value = defaultJobId
  }
}

async function refreshMatchResult() {
  if (!resumeId.value || !selectedJobId.value) {
    aiMatch.value = null
    return
  }
  try {
    aiMatch.value = await fetchAiMatchLatest(resumeId.value, selectedJobId.value)
  } catch {
    aiMatch.value = null
  }
}

function stopMatchPolling() {
  if (matchPollTimer != null) {
    clearInterval(matchPollTimer)
    matchPollTimer = null
  }
}

function startMatchPolling() {
  stopMatchPolling()
  matchPollTimer = setInterval(async () => {
    if (!resumeId.value || !selectedJobId.value) return
    try {
      const match = await fetchAiMatchLatest(resumeId.value, selectedJobId.value)
      aiMatch.value = match
      if (match && (match.matchStatus === 2 || match.matchStatus === 3)) {
        stopMatchPolling()
        matchLoading.value = false
        if (match.matchStatus === 2) {
          matchMsg.value = '匹配分析完成'
        } else {
          matchMsg.value = formatMatchErrorMessage(match.errorMessage)
        }
      }
    } catch {
      /* 轮询失败时继续等待 */
    }
  }, 3000)
}

async function handleMatchAnalysis() {
  if (!resumeId.value || !selectedJobId.value || matchLoading.value || matchBusy.value) return
  if (parseBusy.value || aiParse.value?.taskStatus !== 2) {
    matchMsg.value = '请等待简历解析完成后再进行匹配分析'
    return
  }
  matchLoading.value = true
  matchMsg.value = ''
  errorMsg.value = ''
  try {
    aiMatch.value = await triggerAiMatch({
      resumeId: resumeId.value,
      jobId: selectedJobId.value,
      applicationId: detail.value?.applicationId,
      candidateId: detail.value?.candidateId,
    })
    matchMsg.value = '匹配分析已提交，请稍候…'
    startMatchPolling()
  } catch (e) {
    matchLoading.value = false
    const msg = getErrorMessage(e, '匹配分析失败')
    matchMsg.value = msg
    if (!msg.includes('解析完成')) {
      errorMsg.value = msg
    }
  }
}

async function loadAiInsights(data: HrResumeDetail) {
  if (!resumeId.value) return
  aiLoading.value = true
  aiParse.value = null
  aiProfile.value = null
  try {
    aiParse.value = await fetchAiParseLatest(resumeId.value)
    await refreshMatchResult()
    if (data.applicationId) {
      aiProfile.value = await fetchAiProfileByApplication(data.applicationId).catch(() => null)
    }
  } catch {
    /* AI 服务不可用时保留简历基础数据 */
  } finally {
    aiLoading.value = false
  }
}

async function handleGenerateProfile() {
  const data = detail.value
  if (!data?.applicationId || profileGenerating.value) return
  profileGenerating.value = true
  errorMsg.value = ''
  try {
    aiProfile.value = await generateAiProfile({
      applicationId: data.applicationId,
      candidateId: data.candidateId,
      candidateName: data.candidateName,
      resumeId: data.resumeId ?? resumeId.value ?? undefined,
      jobId: data.jobId,
      jobTitle: data.appliedJobTitle,
    })
  } catch (e) {
    errorMsg.value = getErrorMessage(e, 'AI 人才画像生成失败')
  } finally {
    profileGenerating.value = false
  }
}

async function loadDetail() {
  if (!resumeId.value) {
    errorMsg.value = '缺少简历 ID'
    loading.value = false
    return
  }
  loading.value = true
  errorMsg.value = ''
  pdfPreviewUrl.value = ''
  try {
    const data = await fetchHrResumeDetail(resumeId.value)
    detail.value = data
    if (data.candidateId) {
      try {
        const brief = await fetchHrCandidateBrief(data.candidateId)
        detail.value = {
          ...data,
          candidateName: brief.realName || data.candidateName,
          phone: brief.phone || data.phone,
          email: brief.email || data.email,
          city: brief.city || data.city,
          currentTitle: brief.currentTitle || data.currentTitle,
          highestEdu: brief.highestEdu ?? data.highestEdu,
          matchScore: brief.aiScore ?? data.matchScore,
        }
      } catch {
        /* 档案走 auth 直连，失败时仍展示简历服务返回的数据 */
      }
    }
    await loadJobOptions(data.jobId, data.appliedJobTitle)
    await loadAiInsights(detail.value)
    if (data.applicationId) {
      await loadApplicationInterviews(data.applicationId)
    } else {
      applicationInterviews.value = []
    }
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '简历详情加载失败')
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function handlePreviewInline() {
  if (!candidate.value.attachmentId) {
    errorMsg.value = '该简历暂无附件'
    return
  }
  previewLoading.value = true
  errorMsg.value = ''
  try {
    const preview = await fetchHrResumePreview(candidate.value.attachmentId)
    if (preview.fileType === 'pdf') {
      pdfPreviewUrl.value = preview.presignedUrl
    } else {
      openResumePreview(preview)
    }
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '预览失败，请确认 MinIO 已启动且 talent-resume 已重启')
  } finally {
    previewLoading.value = false
  }
}

onMounted(() => {
  loadDetail()
})

watch(selectedJobId, () => {
  matchMsg.value = ''
  void refreshMatchResult()
})

onUnmounted(() => {
  stopParsePolling()
  stopMatchPolling()
})

const radarOption = computed<EChartsOption>(() => ({
  radar: {
    indicator: radarData.value.map((d) => ({ name: d.subject, max: 100 })),
    splitLine: { lineStyle: { color: '#e2e8f0' } },
    axisName: { color: '#94a3b8', fontSize: 10 },
  },
  series: [
    {
      type: 'radar',
      data: [
        {
          value: radarData.value.map((d) => d.value),
          name: 'candidate',
          areaStyle: { color: 'rgba(124,58,237,0.2)' },
          lineStyle: { color: '#5a8a82', width: 2 },
          itemStyle: { color: '#5a8a82' },
        },
      ],
    },
  ],
}))
</script>

<template>
  <div data-cmp="ResumeDetail" class="flex h-full" style="height: calc(100vh - 64px)">
    <div class="w-72 flex-shrink-0 border-r border-border bg-card overflow-y-auto scrollbar-thin p-5">
      <button
        type="button"
        class="flex items-center gap-1 text-sm text-muted-foreground hover:text-brand-blue mb-4 -ml-1 px-1 py-1 rounded-lg hover:bg-muted transition-colors"
        @click="goBack"
      >
        <ChevronLeft :size="18" />
        <span>返回简历列表</span>
      </button>
      <div class="flex flex-col items-center text-center mb-5 pt-2">
        <div class="w-20 h-20 rounded-full gradient-blue-purple flex items-center justify-center mb-3 shadow-card">
          <User :size="32" class="text-white" />
        </div>
        <div class="text-lg font-bold text-foreground">{{ candidate.name }}</div>
        <div class="text-sm text-muted-foreground mt-0.5">{{ candidate.title }}</div>
        <div class="text-xs text-muted-foreground mt-1">{{ candidate.exp }}</div>
        <div class="mt-3">
          <span class="text-xs px-2 py-0.5 rounded-full border text-brand-blue bg-brand-tint border-brand-border">
            {{ candidate.status }}
          </span>
        </div>
      </div>
      <div class="bg-accent rounded-2xl p-4 mb-4 text-center border border-brand-border/50">
        <div class="text-3xl font-black text-brand-purple mb-1">{{ candidate.match }}%</div>
        <div class="text-xs text-muted-foreground">
          AI综合匹配度
          <span v-if="matchLevelText !== '—'" class="ml-1 text-brand-purple">· {{ matchLevelText }}</span>
        </div>
      </div>
      <div class="mb-4 rounded-xl border border-border bg-muted/40 px-3 py-2 space-y-2">
        <div class="flex items-center justify-between gap-2">
          <div class="text-xs text-muted-foreground">人岗匹配分析</div>
          <Sparkles :size="14" class="text-brand-purple flex-shrink-0" />
        </div>
        <label class="block">
          <span class="text-[11px] text-muted-foreground">目标岗位</span>
          <select
            v-model.number="selectedJobId"
            class="mt-1 w-full rounded-lg border border-border bg-card px-2 py-1.5 text-xs text-foreground"
            :disabled="matchLoading || matchBusy || jobOptions.length === 0"
          >
            <option v-if="jobOptions.length === 0" :value="null">暂无在招岗位</option>
            <option v-for="job in jobOptions" :key="job.id" :value="job.id">
              {{ job.title }}{{ job.workCity ? ` · ${job.workCity}` : '' }}
            </option>
          </select>
        </label>
        <button
          type="button"
          class="w-full inline-flex items-center justify-center gap-1.5 text-xs py-2 rounded-lg gradient-purple text-white disabled:opacity-50"
          :disabled="!selectedJobId || matchLoading || matchBusy || parseBusy"
          @click="handleMatchAnalysis"
        >
          <Sparkles :size="13" :class="matchLoading || matchBusy ? 'animate-pulse' : ''" />
          {{ matchLoading || matchBusy ? '分析中…' : '匹配分析' }}
        </button>
        <p v-if="parseBusy || (aiParse && aiParse.taskStatus !== 2)" class="text-[11px] text-amber-600">
          请等待简历解析完成后再进行匹配分析
        </p>
        <p v-else-if="matchMsg" class="text-[11px]" :class="matchMsg.includes('完成') ? 'text-brand-green' : 'text-muted-foreground'">
          {{ matchMsg }}
        </p>
        <p v-else-if="aiMatch?.matchStatus === 3" class="text-[11px] text-red-600">
          {{ formatMatchErrorMessage(aiMatch.errorMessage) }}
        </p>
      </div>
      <div class="mb-4 rounded-xl border border-border bg-muted/40 px-3 py-2">
        <div class="flex items-center justify-between gap-2">
          <div class="text-xs text-muted-foreground">简历解析</div>
          <button
            type="button"
            class="inline-flex items-center gap-1 text-[11px] px-2 py-1 rounded-lg border border-brand-blue/30 text-brand-blue hover:bg-brand-tint disabled:opacity-50"
            :disabled="reparseLoading || parseBusy"
            @click="handleReparse"
          >
            <RotateCw :size="12" :class="reparseLoading || parseBusy ? 'animate-spin' : ''" />
            {{ reparseLoading || parseBusy ? '解析中…' : '重新解析' }}
          </button>
        </div>
        <div class="text-xs font-medium text-foreground mt-1">{{ parseStatusText }}</div>
        <div v-if="aiParse?.taskStatus === 3 && aiParse.errorMessage" class="text-xs text-red-600 mt-1">
          {{ aiParse.errorMessage }}
        </div>
        <p v-else-if="reparseMsg" class="text-xs text-brand-green mt-1">{{ reparseMsg }}</p>
        <p v-else-if="!aiParse" class="text-xs text-muted-foreground mt-1">尚未解析，可点击重新解析触发</p>
      </div>
      <div class="space-y-3 mb-4">
        <div
          v-for="item in [
            { icon: Phone, label: '电话', value: candidate.phone },
            { icon: Mail, label: '邮箱', value: candidate.email },
            { icon: MapPin, label: '城市', value: candidate.location },
            { icon: GraduationCap, label: '学历', value: candidate.edu },
            { icon: Briefcase, label: '应聘岗位', value: candidate.appliedJob },
            { icon: Calendar, label: '投递时间', value: candidate.appliedDate },
          ]"
          :key="item.label"
          class="flex items-center gap-3"
        >
          <div class="w-7 h-7 rounded-lg bg-muted flex items-center justify-center flex-shrink-0">
            <component :is="item.icon" :size="12" class="text-muted-foreground" />
          </div>
          <div class="flex-1 min-w-0">
            <div class="text-xs text-muted-foreground">{{ item.label }}</div>
            <div class="text-xs text-foreground font-medium truncate">{{ item.value }}</div>
          </div>
        </div>
      </div>
      <div class="mb-4">
        <div class="text-xs font-semibold text-muted-foreground mb-2">筛选操作</div>
        <p v-if="scheduleSuccessMsg" class="text-xs text-brand-green mb-2">{{ scheduleSuccessMsg }}</p>
        <button
          type="button"
          class="w-full py-2.5 rounded-control gradient-blue text-white text-sm font-medium flex items-center justify-center gap-2 disabled:opacity-50"
          :disabled="!canScheduleInterview"
          :title="canScheduleInterview ? '' : '需先有投递记录'"
          @click="scheduleOpen = true"
        >
          <Calendar :size="14" />
          <span>安排面试</span>
        </button>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto scrollbar-thin p-6 min-w-0">
      <p v-if="errorMsg" class="text-xs text-red-600 mb-4">{{ errorMsg }}</p>
      <p v-if="loading" class="text-sm text-muted-foreground mb-4">加载中...</p>

      <div v-if="detail?.summary" class="mb-6 bg-card border border-border rounded-xl p-4 shadow-card">
        <h2 class="text-sm font-bold text-foreground mb-2">个人简介</h2>
        <p class="text-xs text-muted-foreground leading-relaxed whitespace-pre-wrap">{{ detail.summary }}</p>
      </div>

      <div v-if="candidate.attachmentId" class="mb-6 bg-card border border-border rounded-xl p-4 shadow-card">
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2">
            <FileText :size="16" class="text-brand-blue" />
            <h2 class="text-sm font-bold text-foreground">简历附件</h2>
          </div>
          <button
            type="button"
            class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg gradient-blue text-white text-xs disabled:opacity-50"
            :disabled="previewLoading"
            @click="handlePreviewInline"
          >
            <Eye :size="14" />
            {{ previewLoading ? '加载中...' : pdfPreviewUrl ? '刷新预览' : '在线预览' }}
          </button>
        </div>
        <p class="text-xs text-muted-foreground mb-3 truncate">{{ candidate.fileName }}</p>
        <iframe
          v-if="pdfPreviewUrl"
          :src="pdfPreviewUrl"
          class="w-full rounded-lg border border-border bg-white"
          style="height: min(70vh, 720px)"
          title="简历预览"
        />
        <p v-else class="text-xs text-muted-foreground">点击「在线预览」在页面内查看 PDF；Word 文档将在新窗口打开。</p>
      </div>

      <div v-if="detail?.applicationId" class="mb-6 bg-card border border-border rounded-xl p-4 shadow-card">
        <div class="flex items-center gap-2 mb-4">
          <ClipboardList :size="16" class="text-brand-blue" />
          <h2 class="text-sm font-bold text-foreground">面试官评价</h2>
        </div>
        <p v-if="completedInterviewsWithEvaluation.length === 0" class="text-xs text-muted-foreground">
          暂无已完成的面试评价，安排面试后由面试官在「我的面试」中提交。
        </p>
        <div v-else class="space-y-4">
          <div
            v-for="iv in completedInterviewsWithEvaluation"
            :key="iv.interviewId"
            class="rounded-xl border border-border bg-muted/20 p-4"
          >
            <HrInterviewEvaluationPanel
              :evaluation="iv.evaluation"
              :interviewer-name="iv.interviewerName"
              :round-type-label="iv.roundTypeLabel"
              :scheduled-start="iv.scheduledStart"
            />
          </div>
        </div>
      </div>

      <div class="mb-6">
        <div class="flex items-center gap-2 mb-4">
          <Briefcase :size="16" class="text-brand-blue" />
          <h2 class="text-sm font-bold text-foreground">工作经历</h2>
        </div>
        <p v-if="workExp.length === 0" class="text-xs text-muted-foreground">暂无工作经历</p>
        <div v-else class="space-y-4">
          <div v-for="(w, i) in workExp" :key="i" class="flex gap-4">
            <div class="flex flex-col items-center">
              <div class="w-2 h-2 rounded-full bg-brand-blue flex-shrink-0 mt-1.5" />
              <div v-if="i < workExp.length - 1" class="w-px flex-1 bg-border mt-1" />
            </div>
            <div class="flex-1 pb-4">
              <div class="flex items-center justify-between mb-1">
                <span class="text-sm font-semibold text-foreground">{{ w.company }}</span>
                <span class="text-xs text-muted-foreground">{{ w.period }}</span>
              </div>
              <div class="text-xs text-brand-blue mb-2">{{ w.title }}</div>
              <div v-if="w.desc" class="text-xs text-muted-foreground leading-relaxed">{{ w.desc }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="mb-6">
        <div class="flex items-center gap-2 mb-4">
          <FolderKanban :size="16" class="text-brand-orange" />
          <h2 class="text-sm font-bold text-foreground">项目经历</h2>
        </div>
        <p v-if="projectList.length === 0" class="text-xs text-muted-foreground">暂无项目经历</p>
        <div v-else class="space-y-3">
          <div v-for="(p, i) in projectList" :key="p.id ?? i" class="bg-card border border-border rounded-xl p-3">
            <div class="flex items-center justify-between gap-2">
              <span class="text-sm font-semibold text-foreground">{{ p.projectName }}</span>
              <span class="text-xs text-muted-foreground flex-shrink-0">{{ formatResumePeriod(p.startDate, p.endDate) || '—' }}</span>
            </div>
            <div class="text-xs text-muted-foreground mt-1">{{ [p.role, p.techStack].filter(Boolean).join(' · ') || '—' }}</div>
            <p v-if="p.description" class="text-xs text-muted-foreground mt-2 leading-relaxed">{{ p.description }}</p>
          </div>
        </div>
      </div>

      <div class="mb-6">
        <div class="flex items-center gap-2 mb-4">
          <TrendingUp :size="16" class="text-brand-green" />
          <h2 class="text-sm font-bold text-foreground">技能掌握</h2>
        </div>
        <p v-if="skills.length === 0" class="text-xs text-muted-foreground">暂无技能数据</p>
        <div v-else class="space-y-3">
          <div v-for="s in skills" :key="s.name" class="flex items-center gap-3">
            <div class="text-xs text-foreground w-20 flex-shrink-0">{{ s.name }}</div>
            <div class="flex-1 h-2 bg-muted rounded-full overflow-hidden">
              <div
                class="h-full rounded-full transition-all"
                :style="{
                  width: `${s.level}%`,
                  background: s.level >= 90 ? 'var(--brand-purple)' : 'var(--brand-blue)',
                }"
              />
            </div>
            <div class="text-xs text-muted-foreground w-10 text-right">{{ s.label }}</div>
          </div>
        </div>
      </div>

      <div class="mb-6">
        <div class="flex items-center gap-2 mb-4">
          <Award :size="16" class="text-brand-purple" />
          <h2 class="text-sm font-bold text-foreground">证书与荣誉</h2>
        </div>
        <p v-if="certificateList.length === 0" class="text-xs text-muted-foreground">暂无证书与荣誉</p>
        <div v-else class="space-y-3">
          <div
            v-for="(c, i) in certificateList"
            :key="c.id ?? i"
            class="bg-card border border-border rounded-xl p-3"
          >
            <div class="flex items-center justify-between gap-2">
              <span class="text-sm font-semibold text-foreground">{{ c.name }}</span>
              <span class="text-xs text-muted-foreground flex-shrink-0">{{ formatCertType(c.certType) }}</span>
            </div>
            <div class="text-xs text-muted-foreground mt-1">
              {{ [c.issuer, c.issueDate?.slice(0, 7)].filter(Boolean).join(' · ') || '—' }}
            </div>
            <p v-if="c.description" class="text-xs text-muted-foreground mt-2 leading-relaxed">{{ c.description }}</p>
          </div>
        </div>
      </div>

      <div v-if="detail?.educations?.length" class="mb-6">
        <div class="flex items-center gap-2 mb-4">
          <GraduationCap :size="16" class="text-brand-purple" />
          <h2 class="text-sm font-bold text-foreground">教育经历</h2>
        </div>
        <div class="space-y-3">
          <div
            v-for="(edu, i) in detail.educations"
            :key="edu.id ?? i"
            class="bg-card border border-border rounded-xl p-3"
          >
            <div class="flex items-center justify-between">
              <span class="text-sm font-semibold text-foreground">{{ edu.schoolName }}</span>
              <span class="text-xs text-muted-foreground">{{ formatResumePeriod(edu.startDate, edu.endDate) }}</span>
            </div>
            <div class="text-xs text-muted-foreground mt-1">
              {{ [edu.major, formatDegree(edu.degree)].filter(Boolean).join(' · ') || '—' }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="w-72 flex-shrink-0 border-l border-border bg-card overflow-y-auto scrollbar-thin p-5">
      <div class="flex items-center justify-between gap-2 mb-5">
        <div class="flex items-center gap-2">
          <div class="w-7 h-7 rounded-lg gradient-purple flex items-center justify-center">
            <Sparkles :size="14" class="text-white" />
          </div>
          <span class="text-sm font-bold text-foreground">AI人才画像</span>
        </div>
        <button
          v-if="detail?.applicationId"
          type="button"
          class="text-[10px] px-2 py-1 rounded-lg border border-brand-purple/30 text-brand-purple hover:bg-brand-tint-2 disabled:opacity-50"
          :disabled="profileGenerating || aiLoading"
          @click="handleGenerateProfile"
        >
          {{ profileGenerating ? '生成中...' : aiProfile ? '刷新画像' : '生成画像' }}
        </button>
      </div>
      <div v-if="profileTags.length" class="flex flex-wrap gap-1.5 mb-4">
        <span
          v-for="(tag, i) in profileTags"
          :key="i"
          class="text-[10px] px-2 py-0.5 rounded-full bg-brand-tint-2 text-brand-purple"
        >
          {{ tag }}
        </span>
      </div>
      <div v-if="hasAiPortrait && radarData.length" class="bg-muted rounded-2xl p-3 mb-4">
        <VChart :option="radarOption" autoresize style="height: 180px" />
      </div>
      <p v-else-if="aiLoading" class="text-xs text-muted-foreground mb-4">AI 分析加载中...</p>
      <p v-else class="text-xs text-muted-foreground mb-4">暂无 AI 画像数据，完成投递匹配后将展示</p>
      <div class="bg-accent rounded-xl p-4 mb-4 border border-brand-border/50">
        <div class="text-xs font-semibold text-brand-purple mb-2">AI综合评价</div>
        <p class="text-xs text-muted-foreground leading-relaxed">{{ aiSummaryText }}</p>
      </div>
      <div v-if="matchAdvantages.length" class="mb-4">
        <div class="text-xs font-semibold text-brand-green mb-2">匹配优势</div>
        <ul class="space-y-1.5">
          <li v-for="(item, i) in matchAdvantages" :key="i" class="text-xs text-muted-foreground leading-relaxed">
            · {{ item }}
          </li>
        </ul>
      </div>
      <div v-if="matchDisadvantages.length" class="mb-4">
        <div class="text-xs font-semibold text-red-600 mb-2">待提升点</div>
        <ul class="space-y-1.5">
          <li v-for="(item, i) in matchDisadvantages" :key="i" class="text-xs text-muted-foreground leading-relaxed">
            · {{ item }}
          </li>
        </ul>
      </div>
      <div v-if="matchQuestions.length" class="mb-4">
        <div class="text-xs font-semibold text-brand-blue mb-2">建议面试问题</div>
        <ul class="space-y-1.5">
          <li v-for="(item, i) in matchQuestions" :key="i" class="text-xs text-muted-foreground leading-relaxed">
            {{ i + 1 }}. {{ item }}
          </li>
        </ul>
      </div>
    </div>
  </div>

  <InterviewScheduleDialog
    :open="scheduleOpen"
    :application-id="detail?.applicationId ?? null"
    :candidate-name="detail?.candidateName"
    :job-title="detail?.appliedJobTitle"
    @close="scheduleOpen = false"
    @success="handleScheduleSuccess"
  />
</template>
