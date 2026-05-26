<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
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
  Bookmark,
  Send,
  TrendingUp,
  FileText,
  Eye,
} from 'lucide-vue-next'
import { fetchHrCandidateBrief } from '@/api/hrCandidate'
import { fetchHrResumeDetail, fetchHrResumePreview, updateHrScreenStatus, type HrResumeDetail } from '@/api/hrResume'
import { openResumePreview } from '@/api/resume'
import { RESUME_SCREEN_STATUS, screenStatusLabel } from '@/constants/resume'
import { formatDegree, formatResumePeriod } from '@/utils/resumeFormat'
import { getErrorMessage } from '@/utils/validators'

const route = useRoute()
const detail = ref<HrResumeDetail | null>(null)
const loading = ref(true)
const errorMsg = ref('')
const pdfPreviewUrl = ref('')
const previewLoading = ref(false)
const statusUpdating = ref(false)
const statusSuccessMsg = ref('')

const resumeId = computed(() => {
  const id = Number(route.query.id)
  return Number.isFinite(id) && id > 0 ? id : null
})

function formatDateTime(iso?: string | null) {
  if (!iso) return '—'
  return iso.slice(0, 10)
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
  match: detail.value?.matchScore ?? 0,
  status: screenStatusLabel(detail.value?.screenStatus),
  appliedJob: detail.value?.appliedJobTitle || '暂无投递记录',
  appliedDate: formatDateTime(detail.value?.appliedAt),
  fileName: detail.value?.fileName,
  attachmentId: detail.value?.attachmentId,
}))

const currentScreenStatus = computed(() => detail.value?.screenStatus ?? RESUME_SCREEN_STATUS.PENDING)

interface ScreenAction {
  label: string
  status: number
  variant: 'primary' | 'success' | 'danger' | 'muted'
}

const screenActions = computed<ScreenAction[]>(() => {
  const current = currentScreenStatus.value
  const actions: ScreenAction[] = []
  if (current === RESUME_SCREEN_STATUS.PENDING) {
    actions.push({ label: '进入面试', status: RESUME_SCREEN_STATUS.INTERVIEWING, variant: 'primary' })
    actions.push({ label: '直接录用', status: RESUME_SCREEN_STATUS.HIRED, variant: 'success' })
    actions.push({ label: '淘汰', status: RESUME_SCREEN_STATUS.REJECTED, variant: 'danger' })
  } else if (current === RESUME_SCREEN_STATUS.INTERVIEWING) {
    actions.push({ label: '录用', status: RESUME_SCREEN_STATUS.HIRED, variant: 'success' })
    actions.push({ label: '淘汰', status: RESUME_SCREEN_STATUS.REJECTED, variant: 'danger' })
    actions.push({ label: '退回待初筛', status: RESUME_SCREEN_STATUS.PENDING, variant: 'muted' })
  } else if (current === RESUME_SCREEN_STATUS.HIRED || current === RESUME_SCREEN_STATUS.REJECTED) {
    actions.push({ label: '恢复待初筛', status: RESUME_SCREEN_STATUS.PENDING, variant: 'muted' })
  }
  return actions
})

function actionButtonClass(variant: ScreenAction['variant']) {
  switch (variant) {
    case 'primary':
      return 'gradient-blue text-white border-transparent'
    case 'success':
      return 'bg-brand-green/10 text-brand-green border-brand-green/30 hover:bg-brand-green/20'
    case 'danger':
      return 'bg-red-50 text-red-600 border-red-200 hover:bg-red-100'
    default:
      return 'border-border text-muted-foreground hover:bg-muted'
  }
}

async function handleUpdateScreenStatus(targetStatus: number) {
  if (!resumeId.value || statusUpdating.value) return
  if (targetStatus === currentScreenStatus.value) return
  statusUpdating.value = true
  errorMsg.value = ''
  statusSuccessMsg.value = ''
  try {
    await updateHrScreenStatus(resumeId.value, targetStatus)
    await loadDetail()
    statusSuccessMsg.value = `已更新为「${screenStatusLabel(targetStatus)}」`
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '筛选状态更新失败')
  } finally {
    statusUpdating.value = false
  }
}

const workExp = computed(() =>
  (detail.value?.workExperiences ?? []).map((w) => ({
    company: w.companyName,
    title: w.jobTitle,
    period: formatResumePeriod(w.startDate, w.endDate) || '—',
    desc: w.jobDescription ?? '',
  })),
)

const skills = computed(() =>
  (detail.value?.skills ?? []).map((s) => ({
    name: s.skillName,
    level: s.proficiencyLevel ?? 60,
  })),
)

const hasAiPortrait = computed(() => (detail.value?.matchScore ?? 0) > 0)

const radarData = computed(() => {
  const base = detail.value?.matchScore ?? 0
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
        <div class="text-xs text-muted-foreground">AI综合匹配度</div>
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
        <p v-if="statusSuccessMsg" class="text-xs text-brand-green mb-2">{{ statusSuccessMsg }}</p>
        <div class="flex flex-col gap-2">
          <button
            v-for="action in screenActions"
            :key="action.status"
            type="button"
            class="w-full py-2 rounded-xl border text-sm font-medium disabled:opacity-50"
            :class="actionButtonClass(action.variant)"
            :disabled="statusUpdating"
            @click="handleUpdateScreenStatus(action.status)"
          >
            {{ statusUpdating ? '更新中...' : action.label }}
          </button>
        </div>
      </div>
      <div class="flex flex-col gap-2">
        <button type="button" class="w-full py-2.5 rounded-control gradient-blue text-white text-sm font-medium flex items-center justify-center gap-2">
          <Calendar :size="14" />
          <span>安排面试</span>
        </button>
        <button type="button" class="w-full py-2.5 rounded-xl border border-border text-sm text-foreground hover:bg-muted flex items-center justify-center gap-2">
          <Send :size="14" />
          <span>发送Offer</span>
        </button>
        <button type="button" class="w-full py-2.5 rounded-xl border border-border text-sm text-muted-foreground hover:bg-muted flex items-center justify-center gap-2">
          <Bookmark :size="14" />
          <span>存入人才库</span>
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
            <div class="text-xs text-muted-foreground w-8 text-right">{{ s.level }}</div>
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
      <div class="flex items-center gap-2 mb-5">
        <div class="w-7 h-7 rounded-lg gradient-purple flex items-center justify-center">
          <Sparkles :size="14" class="text-white" />
        </div>
        <span class="text-sm font-bold text-foreground">AI人才画像</span>
      </div>
      <div v-if="hasAiPortrait && radarData.length" class="bg-muted rounded-2xl p-3 mb-4">
        <VChart :option="radarOption" autoresize style="height: 180px" />
      </div>
      <p v-else class="text-xs text-muted-foreground mb-4">暂无 AI 画像数据，完成投递匹配后将展示</p>
      <div class="bg-accent rounded-xl p-4 mb-4 border border-brand-border/50">
        <div class="text-xs font-semibold text-brand-purple mb-2">AI综合评价</div>
        <p class="text-xs text-muted-foreground">
          {{ detail?.summary ? '基于候选人简历摘要与档案信息，建议结合岗位需求进一步评估。' : '暂无 AI 评价，请先完善候选人简历或等待 AI 解析完成。' }}
        </p>
      </div>
    </div>
  </div>
</template>
