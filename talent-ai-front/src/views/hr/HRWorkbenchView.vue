<script setup lang="ts">
import { ref, computed, onMounted, type Component } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { EChartsOption } from 'echarts'
import {
  Briefcase,
  Send,
  Calendar,
  CheckCircle,
  Bot,
  Star,
  AlertTriangle,
  Mail,
  Filter,
  FileText,
  BarChart2,
  Sparkles,
  RefreshCw,
  Loader2,
} from 'lucide-vue-next'
import {
  fetchHrWorkbench,
  fetchRecruitmentTrend,
  fetchDepartmentProgress,
  type DashboardMetrics,
  type FunnelStage,
  type TrendPoint,
  type DepartmentProgress,
} from '@/api/analytics'
import { fetchHrInterviewStats } from '@/api/interview'
import { fetchHrResumePage } from '@/api/hrResume'
import { fetchOfferList, OFFER_STATUS } from '@/api/offer'
import { fetchHrAiInsights, type HrAiInsights } from '@/api/ai'
import { RESUME_SCREEN_STATUS } from '@/constants/resume'

const router = useRouter()
const authStore = useAuthStore()
const FUNNEL_COLORS = ['#3d8b7a', '#5a8a82', '#6ba898', '#85a185', '#ac9c8a']

const loading = ref(true)
const error = ref('')
const metrics = ref<DashboardMetrics | null>(null)
const trendData = ref<TrendPoint[]>([])
const deptData = ref<DepartmentProgress[]>([])
const aiInsightsData = ref<HrAiInsights | null>(null)

const welcomeLabel = computed(() => {
  const now = new Date()
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  const dateText = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 ${weekdays[now.getDay()]}`
  const name = authStore.userInfo?.nickname?.trim() || 'HR'
  return `${dateText} · 欢迎回来，${name}`
})

const statsCards = computed(() => {
  const m = metrics.value
  const offerRate = m?.offerAcceptRate ? Math.round(m.offerAcceptRate * 100) : null
  return [
    {
      label: '在招岗位',
      value: m != null ? String(m.activeJobs) : '—',
      subLabel: '简历总量',
      subValue: m != null ? String(m.totalResumes) : '—',
      icon: Briefcase,
      color: 'text-teal-600',
      bg: 'bg-teal-50',
    },
    {
      label: '本月投递',
      value: m != null ? String(m.monthlyApplications) : '—',
      subLabel: 'AI初筛通过',
      subValue: m != null ? String(m.initialScreenPass) : '—',
      icon: Send,
      color: 'text-teal-600',
      bg: 'bg-teal-50',
    },
    {
      label: '面试进行中',
      value: m != null ? String(m.ongoingInterviews) : '—',
      subLabel: '本月已完成',
      subValue: m != null ? `${m.completedInterviewsThisMonth} 场` : '—',
      icon: Calendar,
      color: 'text-teal-600',
      bg: 'bg-teal-50',
    },
    {
      label: '本月录用',
      value: m != null ? String(m.monthlyHired) : '—',
      isProgress: true,
      progressRate: offerRate ?? (m?.monthlyHired ? 100 : 0),
      subLabel: 'Offer接受率',
      subValue: offerRate != null ? `${offerRate}%` : '—',
      icon: CheckCircle,
      color: 'text-orange-500',
      bg: 'bg-orange-50',
    },
  ]
})

const funnelData = computed(() => {
  const funnel = metrics.value?.funnel ?? []
  return funnel.map((item: FunnelStage, i: number) => ({
    name: item.stageName,
    value: item.count ?? 0,
    fill: FUNNEL_COLORS[i % FUNNEL_COLORS.length],
  }))
})

const funnelBase = computed(() => Math.max(funnelData.value[0]?.value ?? 1, 1))

interface WorkbenchTodo {
  key: string
  label: string
  count: number
  dot: string
  numColor: string
  path: string
}

const todoCounts = ref({
  pendingScreen: 0,
  toSchedule: 0,
  offerApproval: 0,
  todayInterview: 0,
  toHire: 0,
})

const todos = computed<WorkbenchTodo[]>(() => [
  {
    key: 'pendingScreen',
    label: '待初筛简历',
    count: todoCounts.value.pendingScreen,
    dot: 'bg-slate-300',
    numColor: 'text-teal-600',
    path: '/hr/resumes',
  },
  {
    key: 'toSchedule',
    label: '待安排面试',
    count: todoCounts.value.toSchedule,
    dot: 'bg-orange-300',
    numColor: 'text-orange-500',
    path: '/hr/interviews',
  },
  {
    key: 'offerApproval',
    label: 'Offer 待审批',
    count: todoCounts.value.offerApproval,
    dot: 'bg-teal-300',
    numColor: 'text-teal-600',
    path: '/hr/offers',
  },
  {
    key: 'todayInterview',
    label: '今日面试提醒',
    count: todoCounts.value.todayInterview,
    dot: 'bg-teal-300',
    numColor: 'text-teal-600',
    path: '/hr/interviews',
  },
  {
    key: 'toHire',
    label: '待录用确认',
    count: todoCounts.value.toHire,
    dot: 'bg-orange-300',
    numColor: 'text-orange-500',
    path: '/hr/resumes',
  },
])

const todoTotal = computed(() => todos.value.reduce((sum, item) => sum + item.count, 0))

async function loadTodoCounts() {
  const [
    pendingScreen,
    offerPending,
    offerApproving,
    interviewStats,
    toHire,
  ] = await Promise.all([
    fetchHrResumePage({ current: 1, size: 1, screenStatus: RESUME_SCREEN_STATUS.PENDING }).catch(() => ({ total: 0 })),
    fetchOfferList({ current: 1, size: 1, status: OFFER_STATUS.PENDING }).catch(() => ({ total: 0 })),
    fetchOfferList({ current: 1, size: 1, status: OFFER_STATUS.APPROVING }).catch(() => ({ total: 0 })),
    fetchHrInterviewStats().catch(() => ({ todayPending: 0, toSchedule: 0 })),
    fetchHrResumePage({ current: 1, size: 1, screenStatus: RESUME_SCREEN_STATUS.OFFER_PENDING }).catch(() => ({ total: 0 })),
  ])

  todoCounts.value = {
    pendingScreen: pendingScreen.total ?? 0,
    toSchedule: interviewStats.toSchedule ?? 0,
    offerApproval: (offerPending.total ?? 0) + (offerApproving.total ?? 0),
    todayInterview: interviewStats.todayPending ?? 0,
    toHire: toHire.total ?? 0,
  }
}

async function loadAiInsights() {
  try {
    aiInsightsData.value = await fetchHrAiInsights()
  } catch {
    aiInsightsData.value = null
  }
}

async function loadWorkbench() {
  loading.value = true
  error.value = ''
  const todoPromise = loadTodoCounts().catch(() => {})
  const aiInsightsPromise = loadAiInsights()
  try {
    const [workbench, trend, dept] = await Promise.all([
      fetchHrWorkbench(),
      fetchRecruitmentTrend(),
      fetchDepartmentProgress(),
    ])
    metrics.value = workbench
    trendData.value = trend || []
    deptData.value = dept || []
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    await Promise.all([todoPromise, aiInsightsPromise])
    loading.value = false
  }
}

onMounted(loadWorkbench)

function goAiAssistant(message?: string) {
  const q = (message ?? aiInput.value).trim()
  router.push({
    path: '/hr/ai-assistant',
    ...(q ? { query: { q } } : {}),
  })
  if (!message) aiInput.value = ''
}

function goTodo(item: WorkbenchTodo) {
  router.push(item.path)
}

const trendOption = computed<EChartsOption>(() => ({
  grid: { left: 40, right: 16, top: 20, bottom: 24 },
  tooltip: { trigger: 'axis', textStyle: { fontSize: 12 } },
  legend: { data: ['投递', '面试', 'Offer'], textStyle: { fontSize: 11 }, right: 0, top: 0, itemWidth: 8, itemHeight: 8, icon: 'circle' },
  xAxis: {
    type: 'category',
    data: trendData.value.map((d) => d.month),
    axisLine: { show: true, lineStyle: { color: '#e2e8f0' } },
    axisTick: { show: false },
    axisLabel: { color: '#94a3b8', fontSize: 11 },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#f1f5f9', type: 'solid' } },
    axisLabel: { color: '#94a3b8', fontSize: 11 },
  },
  series: [
    { name: '投递', type: 'line', data: trendData.value.map((d) => d.applications), smooth: true, showSymbol: false, lineStyle: { color: '#3d8b7a', width: 2 } },
    { name: '面试', type: 'line', data: trendData.value.map((d) => d.completedInterviews), smooth: true, showSymbol: false, lineStyle: { color: '#85a185', width: 2 } },
    { name: 'Offer', type: 'line', data: trendData.value.map((d) => d.offers), smooth: true, showSymbol: false, lineStyle: { color: '#475569', width: 2 } },
  ],
}))

const barDeptOption = computed<EChartsOption>(() => ({
  grid: { left: 30, right: 16, top: 20, bottom: 24 },
  tooltip: { trigger: 'axis', textStyle: { fontSize: 12 } },
  legend: { data: ['缺口', '在招'], textStyle: { fontSize: 11 }, right: 0, top: 0, itemWidth: 12, itemHeight: 4, icon: 'rect' },
  xAxis: {
    type: 'category',
    data: deptData.value.map((d) => d.dept),
    axisLine: { show: true, lineStyle: { color: '#e2e8f0' } },
    axisTick: { show: false },
    axisLabel: { color: '#64748b', fontSize: 11 },
  },
  yAxis: {
    type: 'value',
    splitLine: { show: false },
    axisLabel: { color: '#94a3b8', fontSize: 11 },
  },
  series: [
    { name: '缺口', type: 'bar', data: deptData.value.map((d) => d.gap), itemStyle: { color: '#e2e8f0', borderRadius: [2, 2, 0, 0] }, barWidth: 12 },
    { name: '在招', type: 'bar', data: deptData.value.map((d) => d.active), itemStyle: { color: '#3d8b7a', borderRadius: [2, 2, 0, 0] }, barWidth: 12 },
  ],
}))

interface WorkbenchAiSuggestion {
  key: string
  title: string
  desc: string
  icon: Component
  iconColor: string
  iconBg: string
  btn: string
  onAction: () => void
}

const aiSuggestions = computed<WorkbenchAiSuggestion[]>(() => {
  const c = todoCounts.value
  const items: WorkbenchAiSuggestion[] = []

  if (c.pendingScreen > 0) {
    items.push({
      key: 'pendingScreen',
      title: '待初筛简历',
      desc: `有 ${c.pendingScreen} 份简历等待初筛`,
      icon: Star,
      iconColor: 'text-teal-600',
      iconBg: 'bg-teal-50',
      btn: '查看',
      onAction: () => router.push('/hr/resumes'),
    })
  }
  if (c.toSchedule > 0) {
    items.push({
      key: 'toSchedule',
      title: '待安排面试',
      desc: `有 ${c.toSchedule} 位候选人待安排面试`,
      icon: Calendar,
      iconColor: 'text-orange-500',
      iconBg: 'bg-orange-50',
      btn: '安排',
      onAction: () => router.push('/hr/interviews'),
    })
  }
  if (c.offerApproval > 0) {
    items.push({
      key: 'offerApproval',
      title: 'Offer 待审批',
      desc: `有 ${c.offerApproval} 份 Offer 等待审批`,
      icon: Mail,
      iconColor: 'text-purple-500',
      iconBg: 'bg-purple-50',
      btn: '处理',
      onAction: () => router.push('/hr/offers'),
    })
  }
  if (c.todayInterview > 0) {
    items.push({
      key: 'todayInterview',
      title: '今日面试提醒',
      desc: `今天共有 ${c.todayInterview} 场面试待进行`,
      icon: AlertTriangle,
      iconColor: 'text-teal-600',
      iconBg: 'bg-teal-50',
      btn: '查看',
      onAction: () => router.push('/hr/interviews'),
    })
  }
  if (c.toHire > 0) {
    items.push({
      key: 'toHire',
      title: '待录用确认',
      desc: `有 ${c.toHire} 位候选人处于待录用状态`,
      icon: CheckCircle,
      iconColor: 'text-orange-500',
      iconBg: 'bg-orange-50',
      btn: '确认',
      onAction: () => router.push('/hr/resumes'),
    })
  }

  if (items.length > 0) {
    return items.slice(0, 3)
  }

  return [
    {
      key: 'search',
      title: '智能搜索候选人',
      desc: '用自然语言查找简历与匹配信息',
      icon: Sparkles,
      iconColor: 'text-teal-600',
      iconBg: 'bg-teal-50',
      btn: '开始',
      onAction: () => goAiAssistant('搜一下待初筛的简历'),
    },
    {
      key: 'jd',
      title: '生成岗位 JD',
      desc: '快速撰写标准化职位描述',
      icon: FileText,
      iconColor: 'text-orange-500',
      iconBg: 'bg-orange-50',
      btn: '生成',
      onAction: () => goAiAssistant('帮我写一份高级前端工程师的职位描述'),
    },
    {
      key: 'strategy',
      title: '招聘策略建议',
      desc: '获取流程优化与面试通过率建议',
      icon: Bot,
      iconColor: 'text-purple-500',
      iconBg: 'bg-purple-50',
      btn: '咨询',
      onAction: () => goAiAssistant('我们的面试通过率偏低，如何提升？'),
    },
  ]
})

const shortcuts = [
  { label: '智能筛选', icon: Filter, action: () => goAiAssistant('搜一下待初筛的简历') },
  { label: '生成 JD', icon: FileText, action: () => goAiAssistant('帮我写一份高级前端工程师的职位描述') },
  { label: '安排面试', icon: Calendar, action: () => router.push('/hr/interviews') },
  { label: '数据分析', icon: BarChart2, action: () => router.push('/hr/dashboard') },
]
const aiInput = ref('')

const HEALTH_CIRCLE = 238.7

const recruitHealth = computed(() => {
  const ai = aiInsightsData.value
  if (ai) {
    return {
      score: ai.healthScore,
      label: ai.healthLabel,
      strokeOffset: HEALTH_CIRCLE * (1 - ai.healthScore / 100),
    }
  }

  const m = metrics.value
  if (!m) return { score: 0, label: '—', strokeOffset: HEALTH_CIRCLE }

  const applications = m.monthlyApplications ?? 0
  const pass = m.initialScreenPass ?? 0
  const hired = m.monthlyHired ?? 0

  if (applications === 0) {
    return { score: 60, label: '待启动', strokeOffset: HEALTH_CIRCLE * 0.4 }
  }

  const passRate = pass / applications
  const hireRate = hired / applications
  const score = Math.min(100, Math.max(0, Math.round(50 + passRate * 25 + hireRate * 25)))
  const label = score >= 85 ? '优秀' : score >= 70 ? '良好' : score >= 50 ? '一般' : '待改善'

  return {
    score,
    label,
    strokeOffset: HEALTH_CIRCLE * (1 - score / 100),
  }
})

const aiInsights = computed(() => {
  const ai = aiInsightsData.value
  if (ai) {
    return [
      { label: '今日 AI 解析', value: `${ai.parsedToday} 份` },
      { label: '高匹配候选人', value: `${ai.highMatchCount} 人` },
      { label: '待处理 AI 任务', value: `${ai.pendingAiTasks} 项` },
      { label: '本月匹配完成', value: `${ai.matchSuccessThisMonth} 次` },
    ]
  }

  return [
    { label: '今日 AI 解析', value: '—' },
    { label: '高匹配候选人', value: '—' },
    { label: '待处理 AI 任务', value: '—' },
    { label: '本月匹配完成', value: '—' },
  ]
})

const aiSummary = computed(() => {
  if (aiInsightsData.value?.summary) {
    return aiInsightsData.value.summary
  }

  const m = metrics.value
  if (!m) return 'AI 洞察加载中…'

  const parts: string[] = []
  if (m.monthlyApplications > 0) parts.push(`本月投递 ${m.monthlyApplications} 份`)
  if (m.initialScreenPass > 0) parts.push(`AI 初筛通过 ${m.initialScreenPass} 份`)
  if (todoTotal.value > 0) parts.push(`待办 ${todoTotal.value} 项`)

  return parts.length > 0
    ? parts.join(' · ')
    : '当前暂无待处理事项，可继续使用 AI 助手'
})
</script>

<template>
  <div data-cmp="HRWorkbench" class="min-h-screen bg-[#EBF4F0] p-6 text-slate-800">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">我的工作台</h1>
        <p class="mt-1 text-sm text-slate-500">{{ welcomeLabel }}</p>
      </div>
      <div class="flex items-center gap-2 rounded-full border border-slate-200 bg-white px-4 py-2 text-sm text-slate-600 shadow-sm cursor-pointer hover:bg-slate-50 transition-colors" @click="loadWorkbench">
        <div :class="['h-2 w-2 rounded-full', loading ? 'bg-amber-400' : error ? 'bg-red-400' : 'bg-teal-500']" />
        <span>{{ loading ? '加载中…' : error ? '加载异常' : '系统运行正常' }}</span>
        <RefreshCw :size="14" :class="['ml-1 text-slate-400', loading && 'animate-spin']" />
      </div>
    </div>

    <div v-if="error" class="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
      {{ error }}
      <button class="ml-2 underline" @click="loadWorkbench">重试</button>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-24 text-slate-500">
      <Loader2 :size="24" class="animate-spin mr-2" />
      正在加载工作台数据…
    </div>

    <div v-else class="flex gap-6">
      
      <div class="flex-1 space-y-6">
        
        <div class="grid grid-cols-4 gap-4">
          <div v-for="card in statsCards" :key="card.label" class="flex flex-col rounded-xl bg-white p-5 shadow-sm border border-slate-100 relative overflow-hidden">
            <div class="mb-3 flex items-start justify-between">
              <div :class="['flex h-10 w-10 items-center justify-center rounded-xl', card.bg]">
                <component :is="card.icon" :size="20" :class="card.color" />
              </div>
              <div v-if="card.isProgress" class="relative flex h-12 w-12 items-center justify-center">
                <svg class="h-12 w-12 -rotate-90 transform">
                  <circle cx="24" cy="24" r="20" stroke="currentColor" stroke-width="4" fill="transparent" class="text-slate-100" />
                  <circle cx="24" cy="24" r="20" stroke="currentColor" stroke-width="4" fill="transparent" :stroke-dasharray="125.6" :stroke-dashoffset="125.6 - (125.6 * card.progressRate) / 100" class="text-teal-600 transition-all duration-1000" />
                </svg>
                <span class="absolute text-xs font-bold text-slate-700">{{ card.progressRate }}%</span>
              </div>
              <div v-else class="text-right">
                <div class="text-xs text-slate-400">{{ card.subLabel }}</div>
                <div class="text-sm font-semibold text-teal-600">{{ card.subValue }}</div>
              </div>
            </div>
            <div>
              <div class="text-2xl font-bold text-slate-800">{{ card.value }}</div>
              <div class="text-sm text-slate-500 mt-1">{{ card.label }}</div>
            </div>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-6">
          <div class="rounded-xl bg-white p-5 shadow-sm border border-slate-100">
            <div class="mb-5 flex items-center justify-between">
              <div>
                <h3 class="font-bold text-slate-800">招聘漏斗</h3>
                <p class="text-xs text-slate-400 mt-1">本月数据</p>
              </div>
            </div>
            <div class="space-y-3.5">
              <div v-if="!funnelData.length" class="py-8 text-center text-sm text-slate-400">暂无漏斗数据</div>
              <div v-for="(item, i) in funnelData" :key="item.name" class="flex items-center gap-4">
                <div class="w-20 shrink-0 text-sm text-slate-500">{{ item.name }}</div>
                <div class="flex-1 h-7 rounded-full bg-slate-100">
                  <div
                    class="flex h-full items-center rounded-full px-3 transition-all duration-500"
                    :style="{ width: `${Math.max((item.value / funnelBase) * 100, item.value > 0 ? 8 : 0)}%`, backgroundColor: item.fill, minWidth: item.value > 0 ? '50px' : '0' }"
                  >
                    <span v-if="item.value > 0" class="text-xs font-medium text-white">{{ item.value }}</span>
                  </div>
                </div>
                <div class="w-10 shrink-0 text-right text-sm text-slate-400">
                  {{ i === 0 ? '100%' : `${Math.round((item.value / funnelBase) * 100)}%` }}
                </div>
              </div>
            </div>
          </div>

          <div class="rounded-xl bg-white p-5 shadow-sm border border-slate-100 flex flex-col">
            <div class="mb-2 flex items-center justify-between">
              <h3 class="font-bold text-slate-800">招聘趋势</h3>
            </div>
            <div class="flex-1 min-h-[220px]">
              <div v-if="!trendData.length" class="flex h-full items-center justify-center text-sm text-slate-400">暂无趋势数据</div>
              <VChart v-else :option="trendOption" autoresize class="w-full h-full" />
            </div>
          </div>
        </div>

        <div class="grid grid-cols-5 gap-6">
          <div class="col-span-2 rounded-xl bg-white p-5 shadow-sm border border-slate-100">
            <div class="mb-5 flex items-center justify-between">
              <h3 class="font-bold text-slate-800">待办事项</h3>
              <span
                class="cursor-pointer text-xs text-teal-600 hover:underline"
                @click="router.push('/hr/resumes')"
              >
                查看全部{{ todoTotal > 0 ? ` (${todoTotal})` : '' }}
              </span>
            </div>
            <div class="space-y-1">
              <div
                v-for="t in todos"
                :key="t.key"
                class="flex items-center justify-between border-b border-slate-50 py-3 last:border-0 hover:bg-slate-50 rounded-lg px-2 -mx-2 transition-colors cursor-pointer"
                @click="goTodo(t)"
              >
                <div class="flex items-center gap-3">
                  <div :class="['h-2 w-2 rounded-full', t.dot]" />
                  <span class="text-sm text-slate-600">{{ t.label }}</span>
                </div>
                <div :class="['text-sm font-bold', t.numColor]">{{ t.count }}</div>
              </div>
            </div>
          </div>

          <div class="col-span-3 rounded-xl bg-white p-5 shadow-sm border border-slate-100 flex flex-col">
            <div class="mb-2 flex items-center justify-between">
              <div>
                <h3 class="font-bold text-slate-800">部门招聘进度</h3>
                <p class="text-xs text-slate-400 mt-1">当前在招岗位</p>
              </div>
            </div>
            <div class="flex-1 min-h-[180px]">
              <div v-if="!deptData.length" class="flex h-full items-center justify-center text-sm text-slate-400">暂无部门进度数据</div>
              <VChart v-else :option="barDeptOption" autoresize class="w-full h-full" />
            </div>
          </div>
        </div>

      </div>

      <div class="w-[320px] shrink-0 space-y-6">
        
        <div class="flex h-[560px] flex-col rounded-xl bg-white p-5 shadow-sm border border-slate-100 relative">
          <div class="mb-5 flex items-center gap-2">
            <div class="flex h-8 w-8 items-center justify-center rounded-full bg-teal-50">
              <Bot :size="18" class="text-teal-600" />
            </div>
            <h3 class="font-bold text-slate-800">AI 招聘助手</h3>
            <button
              type="button"
              class="ml-auto text-xs text-teal-600 hover:underline"
              @click="goAiAssistant()"
            >
              进入助手
            </button>
          </div>

          <div class="mb-2 text-xs text-slate-400 pl-1 border-l-2 border-slate-200">今日重点建议</div>
          <div class="mb-6 space-y-3">
            <div
              v-for="item in aiSuggestions"
              :key="item.key"
              class="flex items-center gap-3 rounded-xl border border-slate-100 bg-slate-50/50 p-3 hover:bg-slate-50 transition-colors"
            >
              <div :class="['flex h-8 w-8 shrink-0 items-center justify-center rounded-lg', item.iconBg]">
                <component :is="item.icon" :size="16" :class="item.iconColor" />
              </div>
              <div class="flex-1 overflow-hidden">
                <div class="truncate text-sm font-bold text-slate-800">{{ item.title }}</div>
                <div class="truncate text-xs text-slate-500 mt-0.5">{{ item.desc }}</div>
              </div>
              <button
                type="button"
                class="shrink-0 rounded-full border border-teal-200 px-3 py-1 text-xs text-teal-600 hover:bg-teal-50 transition-colors"
                @click="item.onAction()"
              >
                {{ item.btn }}
              </button>
            </div>
          </div>

          <div class="mb-3 text-xs text-slate-400 pl-1 border-l-2 border-slate-200">快捷操作</div>
          <div class="grid grid-cols-2 gap-2 mb-auto">
            <button
              v-for="btn in shortcuts"
              :key="btn.label"
              type="button"
              class="flex items-center justify-center gap-1.5 rounded-full border border-slate-200 bg-white py-1.5 text-xs text-slate-600 hover:border-teal-500 hover:text-teal-600 transition-colors"
              @click="btn.action()"
            >
              <component :is="btn.icon" :size="12" />
              {{ btn.label }}
            </button>
          </div>

          <div class="relative mt-6">
            <input
              v-model="aiInput"
              class="w-full rounded-full border border-slate-200 bg-slate-50 py-3 pl-4 pr-12 text-sm outline-none focus:border-teal-500 focus:bg-white transition-all"
              placeholder="向AI助手提问..."
              @keydown.enter="goAiAssistant()"
            />
            <button
              type="button"
              class="absolute right-2 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full bg-teal-600 text-white hover:bg-teal-700 transition-colors"
              @click="goAiAssistant()"
            >
              <Send :size="14" class="ml-0.5" />
            </button>
          </div>
        </div>

        <div class="rounded-xl bg-white p-5 shadow-sm border border-slate-100">
          <div class="mb-5 flex items-center gap-2">
            <Bot :size="18" class="text-teal-600" />
            <h3 class="font-bold text-slate-800">AI 今日洞察</h3>
          </div>
          
          <div class="flex items-center gap-6">
            <div class="flex flex-col items-center justify-center">
               <div class="relative flex h-[88px] w-[88px] items-center justify-center">
                <svg class="h-full w-full -rotate-90 transform">
                  <circle cx="44" cy="44" r="38" stroke="currentColor" stroke-width="6" fill="transparent" class="text-slate-100" />
                  <circle cx="44" cy="44" r="38" stroke="currentColor" stroke-width="6" fill="transparent" :stroke-dasharray="HEALTH_CIRCLE" :stroke-dashoffset="recruitHealth.strokeOffset" class="text-teal-600 transition-all duration-1000" />
                </svg>
                <span class="absolute text-3xl font-bold text-slate-800">{{ recruitHealth.score }}</span>
              </div>
              <div class="mt-2 text-center">
                <div class="text-xs text-slate-500">招聘健康度</div>
                <div class="text-sm font-bold text-teal-600 mt-0.5">{{ recruitHealth.label }}</div>
              </div>
            </div>
            
            <div class="flex-1 space-y-3">
              <div v-for="item in aiInsights" :key="item.label" class="flex items-center justify-between">
                <span class="text-xs text-slate-500">{{ item.label }}</span>
                <span class="text-sm font-bold text-slate-800">{{ item.value }}</span>
              </div>
            </div>
          </div>
          
          <div class="mt-5 rounded-lg bg-slate-50 py-2.5 text-center text-xs text-slate-500">
            {{ aiSummary }}
          </div>
        </div>

      </div>
    </div>
  </div>
</template>