<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import { TrendingUp, Users, Briefcase, Calendar, Sparkles, Zap, AlertTriangle, CheckCircle, Loader2 } from 'lucide-vue-next'
import { fetchHrDashboard, fetchRecruitmentTrend, fetchDepartmentProgress, type DashboardMetrics, type FunnelStage, type TrendPoint, type DepartmentProgress } from '@/api/analytics'

const FUNNEL_COLORS = ['#3d8b7a', '#3B82F6', '#5a8a82', '#8B5CF6', '#10B981']

const loading = ref(true)
const error = ref('')
const metrics = ref<DashboardMetrics | null>(null)
const trendData = ref<TrendPoint[]>([])
const deptData = ref<DepartmentProgress[]>([])

const funnelData = computed(() => {
  const funnel = metrics.value?.funnel ?? []
  return funnel.map((f: FunnelStage, i: number) => ({
    name: f.stageName,
    value: f.count ?? 0,
    fill: FUNNEL_COLORS[i % FUNNEL_COLORS.length],
  }))
})

const funnelMax = computed(() => Math.max(funnelData.value[0]?.value ?? 1, 1))

const kpis = computed(() => {
  const m = metrics.value
  const offerRate = m?.offerAcceptRate ? `${Math.round(m.offerAcceptRate * 100)}%` : '—'
  return [
    {
      label: '本月投递',
      value: m?.monthlyApplications ?? '—',
      trend: m ? `简历库 ${m.totalResumes}` : '',
      positive: true,
      icon: Users,
      color: 'text-brand-blue',
      bg: 'gradient-blue',
    },
    {
      label: '面试完成',
      value: m?.completedInterviewsThisMonth ?? '—',
      trend: m ? `进行中 ${m.ongoingInterviews}` : '',
      positive: true,
      icon: Calendar,
      color: 'text-brand-purple',
      bg: 'gradient-purple',
    },
    {
      label: 'Offer发放',
      value: m?.monthlyOfferSent ?? '—',
      trend: m?.placeholderFields?.includes('monthlyOfferSent') ? '待 Offer 模块' : '',
      positive: true,
      icon: Briefcase,
      color: 'text-brand-green',
      bg: 'bg-brand-green',
    },
    {
      label: '本月录用',
      value: m?.monthlyHired ?? '—',
      trend: offerRate !== '—' ? `接受率 ${offerRate}` : '',
      positive: true,
      icon: TrendingUp,
      color: 'text-brand-orange',
      bg: 'bg-brand-orange',
    },
  ]
})

async function loadDashboard() {
  loading.value = true
  error.value = ''
  try {
    const [dashboard, trend, dept] = await Promise.all([
      fetchHrDashboard(),
      fetchRecruitmentTrend(),
      fetchDepartmentProgress(),
    ])
    metrics.value = dashboard
    trendData.value = trend || []
    deptData.value = dept || []
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)

const trendOption = computed<EChartsOption>(() => ({
  grid: { left: 48, right: 24, top: 32, bottom: 28 },
  tooltip: { trigger: 'axis' },
  legend: { data: ['投递', '面试', 'offer'], textStyle: { fontSize: 11 } },
  xAxis: {
    type: 'category',
    data: trendData.value.map((d) => d.month),
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#94a3b8', fontSize: 11 },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
    axisLabel: { color: '#94a3b8', fontSize: 11 },
  },
  series: [
    {
      name: '投递',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: trendData.value.map((d) => d.applications),
      lineStyle: { color: '#3d8b7a', width: 2 },
      areaStyle: { color: 'rgba(37,99,235,0.12)' },
    },
    {
      name: '面试',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: trendData.value.map((d) => d.completedInterviews),
      lineStyle: { color: '#5a8a82', width: 2 },
      areaStyle: { color: 'rgba(124,58,237,0.12)' },
    },
    {
      name: 'offer',
      type: 'line',
      data: trendData.value.map((d) => d.offers),
      lineStyle: { color: '#10B981', width: 2 },
      symbolSize: 6,
      itemStyle: { color: '#10B981' },
    },
  ],
}))

const deptBarOption = computed<EChartsOption>(() => ({
  grid: { left: 72, right: 24, top: 16, bottom: 28 },
  tooltip: { trigger: 'axis' },
  legend: { data: ['缺口', '在招'], textStyle: { fontSize: 11 } },
  xAxis: { type: 'value', splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } }, axisLabel: { color: '#94a3b8', fontSize: 11 } },
  yAxis: {
    type: 'category',
    data: deptData.value.map((d) => d.dept),
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#94a3b8', fontSize: 11 },
  },
  series: [
    { name: '缺口', type: 'bar', data: deptData.value.map((d) => d.gap), itemStyle: { color: '#E2E8F0', borderRadius: [0, 4, 4, 0] } },
    { name: '在招', type: 'bar', data: deptData.value.map((d) => d.active), itemStyle: { color: '#3d8b7a', borderRadius: [0, 4, 4, 0] } },
  ],
}))


</script>

<template>
  <div data-cmp="RecruitDashboard" class="p-6 space-y-5" style="height: calc(100vh - 64px); overflow-y: auto">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-xl font-bold text-foreground">招聘数据驾驶舱</h1>
        <p class="text-sm text-muted-foreground mt-0.5">{{ nowLabel }} · {{ loading ? '加载中…' : '实时数据' }}</p>
      </div>
      <div class="flex items-center gap-2 px-4 py-2 rounded-xl bg-accent border border-brand-border/50">
        <Sparkles :size="14" class="text-brand-purple" />
        <span class="text-xs text-brand-purple font-medium">AI智能分析已开启</span>
      </div>
    </div>

    <div v-if="error" class="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
      {{ error }}
      <button class="ml-2 underline" @click="loadDashboard">重试</button>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-16 text-muted-foreground">
      <Loader2 :size="24" class="animate-spin mr-2" />
      正在加载驾驶舱数据…
    </div>

    <template v-else>
      <div class="flex gap-4">
        <div v-for="k in kpis" :key="k.label" class="flex-1 bg-card shadow-card p-4 flex items-center gap-4">
          <div :class="['w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 text-white', k.bg]">
            <component :is="k.icon" :size="18" />
          </div>
          <div class="flex-1 min-w-0">
            <div class="text-2xl font-black text-foreground">{{ k.value }}</div>
            <div class="text-xs text-muted-foreground">{{ k.label }}</div>
          </div>
          <div v-if="k.trend" :class="['text-xs font-medium', k.positive ? 'text-brand-green' : 'text-brand-red']">{{ k.trend }}</div>
        </div>
      </div>

      <div class="flex gap-5">
        <div class="flex-[2] bg-card shadow-card p-5">
          <h3 class="text-sm font-semibold text-foreground mb-1">招聘趋势 (近6个月)</h3>
          <p class="text-xs text-muted-foreground mb-3">按月度统计投递 / 面试完成 / Offer发放量</p>
          <VChart :option="trendOption" autoresize style="height: 200px" />
        </div>
        <div class="flex-1 bg-card shadow-card p-5">
          <h3 class="text-sm font-semibold text-foreground mb-4">本月招聘漏斗</h3>
          <div v-if="!funnelData.length" class="text-sm text-muted-foreground py-8 text-center">暂无漏斗数据</div>
          <div v-else class="space-y-2">
            <div v-for="f in funnelData" :key="f.name" class="space-y-1">
              <div class="flex items-center justify-between text-xs">
                <span class="text-muted-foreground">{{ f.name }}</span>
                <span class="font-bold text-foreground">{{ f.value }}</span>
              </div>
              <div class="h-6 bg-muted rounded-lg overflow-hidden">
                <div
                  class="h-full rounded-lg flex items-center px-2 transition-all"
                  :style="{ width: `${Math.max(Math.round((f.value / funnelMax) * 100), f.value > 0 ? 8 : 0)}%`, background: f.fill }"
                >
                  <span v-if="f.value > 0" class="text-white text-xs font-bold">{{ Math.round((f.value / funnelMax) * 100) }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="flex gap-5">
        <div class="flex-[2] bg-card shadow-card p-5">
          <h3 class="text-sm font-semibold text-foreground mb-1">各部门招聘进度</h3>
          <p class="text-xs text-muted-foreground mb-3">缺口 = 部门总编制 | 在招 = 当前活跃岗位数</p>
          <VChart :option="deptBarOption" autoresize style="height: 180px" />
        </div>
        <div class="flex-[2] bg-accent rounded-2xl p-5 border border-brand-border/50">
          <div class="flex items-center gap-2 mb-3">
            <Zap :size="14" class="text-brand-purple" />
            <span class="text-xs font-semibold text-brand-purple">AI洞察</span>
          </div>
          <div class="space-y-3">
            <div v-for="(insight, i) in [
              { type: 'positive', text: `在招岗位 ${metrics?.activeJobs ?? 0} 个，AI 初筛通过 ${metrics?.initialScreenPass ?? 0} 份` },
              { type: 'warning', text: metrics?.placeholderFields?.includes('monthlyOfferSent') ? 'Offer 统计待 Offer 模块对接后自动更新' : 'Offer 数据已同步' },
              { type: 'positive', text: `本月已完成面试 ${metrics?.completedInterviewsThisMonth ?? 0} 场` },
            ]" :key="i" class="flex items-start gap-2">
              <CheckCircle v-if="insight.type === 'positive'" :size="12" class="text-brand-green mt-0.5 flex-shrink-0" />
              <AlertTriangle v-else :size="12" class="text-brand-orange mt-0.5 flex-shrink-0" />
              <p class="text-xs text-muted-foreground leading-relaxed">{{ insight.text }}</p>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
