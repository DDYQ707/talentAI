<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import type { EChartsOption } from 'echarts'
import * as echarts from 'echarts/core'
import {
  LayoutDashboard,
  TrendingUp,
  TrendingDown,
  Users,
  Building2,
  Zap,
  ShieldAlert,
  Activity,
  Clock,
  RefreshCw,
  Server,
  Wifi,
  WifiOff,
} from 'lucide-vue-next'
import {
  fetchDashboardData,
  type DashboardData,
  type DashboardOverview,
  type SupplyDemandTrendItem,
  type IndustryTalentItem,
  type ServiceHealthItem,
} from '@/api/adminDashboard'

/* =================================================================
 *  Mock 数据 — 保证视图无后端也能完美渲染
 * ================================================================= */

const mockOverview: DashboardOverview = {
  totalUsers: 128_456,
  totalUsersTrend: 12.6,
  totalEnterprises: 3_842,
  totalEnterprisesTrend: 5.3,
  todayDeliveryPeak: 4_721,
  todayDeliveryPeakTrend: -3.1,
  aiRiskBlocked: 286,
  aiRiskBlockedTrend: 18.7,
}

function generateTrendMock(): SupplyDemandTrendItem[] {
  const items: SupplyDemandTrendItem[] = []
  const now = new Date()
  for (let i = 29; i >= 0; i--) {
    const d = new Date(now)
    d.setDate(d.getDate() - i)
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    items.push({
      date: `${mm}-${dd}`,
      resumeDeliveries: Math.floor(800 + Math.random() * 600 + Math.sin(i / 3) * 200),
      jobPublications: Math.floor(300 + Math.random() * 250 + Math.cos(i / 4) * 120),
    })
  }
  return items
}

const mockIndustry: IndustryTalentItem[] = [
  { industry: '互联网/IT', value: 3820 },
  { industry: '金融', value: 2150 },
  { industry: '制造业', value: 1780 },
  { industry: '教育', value: 1340 },
  { industry: '医疗健康', value: 980 },
  { industry: '新能源', value: 760 },
]

const mockServices: ServiceHealthItem[] = [
  { name: 'Gateway',          status: 'UP',   latency: 12, uptime: '99.99%', lastChecked: new Date().toISOString() },
  { name: 'Auth-Service',     status: 'UP',   latency: 32, uptime: '99.97%', lastChecked: new Date().toISOString() },
  { name: 'User-Service',     status: 'UP',   latency: 28, uptime: '99.95%', lastChecked: new Date().toISOString() },
  { name: 'Job-Service',      status: 'UP',   latency: 45, uptime: '99.92%', lastChecked: new Date().toISOString() },
  { name: 'AI-Risk-Service',  status: 'DOWN', latency: 0,  uptime: '98.41%', lastChecked: new Date().toISOString() },
]

/* =================================================================
 *  响应式状态
 * ================================================================= */

const overview = reactive<DashboardOverview>({ ...mockOverview })
const trendData = ref<SupplyDemandTrendItem[]>(generateTrendMock())
const industryData = ref<IndustryTalentItem[]>([...mockIndustry])
const serviceHealth = ref<ServiceHealthItem[]>([...mockServices])
const loading = ref(false)
const lastRefresh = ref(formatNow())

/* ---- 指标卡片配置 ---- */
const kpiCards = computed(() => [
  {
    label: '总用户量',
    value: formatNumber(overview.totalUsers),
    trend: overview.totalUsersTrend,
    icon: Users,
    iconBg: 'gradient-blue',
  },
  {
    label: '入驻企业数',
    value: formatNumber(overview.totalEnterprises),
    trend: overview.totalEnterprisesTrend,
    icon: Building2,
    iconBg: 'gradient-purple',
  },
  {
    label: '今日投递峰值',
    value: formatNumber(overview.todayDeliveryPeak),
    trend: overview.todayDeliveryPeakTrend,
    icon: Zap,
    iconBg: 'bg-brand-orange',
  },
  {
    label: 'AI风控拦截数',
    value: formatNumber(overview.aiRiskBlocked),
    trend: overview.aiRiskBlockedTrend,
    icon: ShieldAlert,
    iconBg: 'bg-brand-red',
  },
])

/* =================================================================
 *  ECharts 实例管理 (ResizeObserver + 防抖销毁)
 * ================================================================= */

const trendChartRef = ref<HTMLDivElement | null>(null)
const doughnutChartRef = ref<HTMLDivElement | null>(null)
let trendChart: echarts.ECharts | null = null
let doughnutChart: echarts.ECharts | null = null
let resizeObserver: ResizeObserver | null = null
let resizeTimer: ReturnType<typeof setTimeout> | null = null

const BRAND_TEAL = '#3d8b7a'
const BRAND_TEAL_MID = '#5a8a82'
const BRAND_SAGE = '#85a185'
const BRAND_ORANGE = '#b8956a'
const BRAND_WARM = '#ac9c8a'
const BRAND_RED = '#c45c5c'

const trendOption = computed<EChartsOption>(() => ({
  grid: { left: 52, right: 28, top: 40, bottom: 32 },
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(255,255,255,0.96)',
    borderColor: '#e5e7eb',
    textStyle: { color: '#1a1a1a', fontSize: 12 },
  },
  legend: {
    data: ['简历投递', '职位发布'],
    top: 4,
    right: 16,
    textStyle: { fontSize: 11, color: '#6f7f7a' },
  },
  xAxis: {
    type: 'category',
    data: trendData.value.map(d => d.date),
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#94a3b8', fontSize: 10, interval: 4 },
    boundaryGap: false,
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
    axisLabel: { color: '#94a3b8', fontSize: 10 },
  },
  series: [
    {
      name: '简历投递',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: trendData.value.map(d => d.resumeDeliveries),
      lineStyle: { color: BRAND_TEAL, width: 2.5 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(61,139,122,0.25)' },
          { offset: 1, color: 'rgba(61,139,122,0.02)' },
        ]),
      },
    },
    {
      name: '职位发布',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: trendData.value.map(d => d.jobPublications),
      lineStyle: { color: BRAND_ORANGE, width: 2.5 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(184,149,106,0.2)' },
          { offset: 1, color: 'rgba(184,149,106,0.02)' },
        ]),
      },
    },
  ],
}))

const doughnutColors = [BRAND_TEAL, BRAND_TEAL_MID, BRAND_SAGE, BRAND_ORANGE, BRAND_WARM, BRAND_RED]

const doughnutOption = computed<EChartsOption>(() => ({
  tooltip: {
    trigger: 'item',
    backgroundColor: 'rgba(255,255,255,0.96)',
    borderColor: '#e5e7eb',
    textStyle: { color: '#1a1a1a', fontSize: 12 },
    formatter: '{b}: {c} ({d}%)',
  },
  legend: {
    type: 'scroll',
    orient: 'vertical',
    right: 8,
    top: 'center',
    textStyle: { fontSize: 11, color: '#6f7f7a' },
    itemWidth: 10,
    itemHeight: 10,
    icon: 'circle',
  },
  series: [
    {
      type: 'pie',
      radius: ['42%', '70%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 13, fontWeight: 'bold' },
        itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.15)' },
      },
      data: industryData.value.map((item, i) => ({
        name: item.industry,
        value: item.value,
        itemStyle: { color: doughnutColors[i % doughnutColors.length] },
      })),
    },
  ],
}))

function initCharts() {
  if (trendChartRef.value) {
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption(trendOption.value)
  }
  if (doughnutChartRef.value) {
    doughnutChart = echarts.init(doughnutChartRef.value)
    doughnutChart.setOption(doughnutOption.value)
  }
}

function handleResize() {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    trendChart?.resize()
    doughnutChart?.resize()
  }, 200)
}

function disposeCharts() {
  if (resizeTimer) { clearTimeout(resizeTimer); resizeTimer = null }
  if (resizeObserver) { resizeObserver.disconnect(); resizeObserver = null }
  trendChart?.dispose(); trendChart = null
  doughnutChart?.dispose(); doughnutChart = null
}

/* =================================================================
 *  数据加载（带 API 预留）
 * ================================================================= */

async function loadDashboard() {
  loading.value = true
  try {
    const data: DashboardData = await fetchDashboardData()
    // API 成功时覆盖 mock
    Object.assign(overview, data.overview)
    trendData.value = data.supplyDemandTrend
    industryData.value = data.industryTalent
    serviceHealth.value = data.serviceHealth
  } catch {
    // API 不可用 → 保持 mock 数据，静默
  } finally {
    loading.value = false
    lastRefresh.value = formatNow()
    await nextTick()
    trendChart?.setOption(trendOption.value)
    doughnutChart?.setOption(doughnutOption.value)
  }
}

/* =================================================================
 *  生命周期
 * ================================================================= */

onMounted(async () => {
  await nextTick()
  initCharts()
  resizeObserver = new ResizeObserver(handleResize)
  if (trendChartRef.value) resizeObserver.observe(trendChartRef.value)
  if (doughnutChartRef.value) resizeObserver.observe(doughnutChartRef.value)
  loadDashboard()
})

onUnmounted(() => {
  disposeCharts()
})

/* =================================================================
 *  工具函数
 * ================================================================= */

function formatNumber(n: number): string {
  if (n >= 10_000) return (n / 10_000).toFixed(1).replace(/\.0$/, '') + '万'
  return n.toLocaleString()
}

function formatNow(): string {
  const d = new Date()
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}:${d.getSeconds().toString().padStart(2, '0')}`
}
</script>

<template>
  <div data-cmp="AdminDashboard" class="scrollbar-thin h-full overflow-y-auto p-6 sm:p-8">
    <div class="mx-auto max-w-7xl">

      <!-- ============ 页面头部 ============ -->
      <div class="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div class="flex items-center gap-3">
            <div class="flex h-10 w-10 items-center justify-center rounded-xl gradient-blue-purple text-white">
              <LayoutDashboard :size="20" stroke-width="1.75" />
            </div>
            <div>
              <h1 class="text-2xl font-black text-foreground">全链路数据态势感知大盘</h1>
              <p class="mt-0.5 text-sm text-muted-foreground">Global Situation Awareness Dashboard</p>
            </div>
          </div>
        </div>
        <div class="flex items-center gap-3">
          <div class="flex items-center gap-1.5 rounded-xl bg-muted px-3 py-2 text-xs text-muted-foreground">
            <Clock :size="13" />
            <span>最近更新 {{ lastRefresh }}</span>
          </div>
          <button
            type="button"
            class="flex items-center gap-1.5 rounded-xl border border-border bg-card px-3 py-2 text-xs font-medium text-foreground shadow-card transition-colors hover:bg-muted"
            :disabled="loading"
            @click="loadDashboard"
          >
            <RefreshCw :size="13" :class="loading ? 'animate-spin' : ''" />
            刷新
          </button>
        </div>
      </div>

      <!-- ============ Tier 1：核心指标卡片 ============ -->
      <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div
          v-for="card in kpiCards"
          :key="card.label"
          class="flex items-center gap-4 rounded-2xl border border-border bg-card p-5 shadow-card transition-transform duration-200 hover:scale-[1.02]"
        >
          <div :class="['flex h-12 w-12 shrink-0 items-center justify-center rounded-xl text-white', card.iconBg]">
            <component :is="card.icon" :size="22" stroke-width="1.75" />
          </div>
          <div class="min-w-0 flex-1">
            <div class="text-2xl font-black tracking-tight text-foreground">{{ card.value }}</div>
            <div class="mt-0.5 text-xs text-muted-foreground">{{ card.label }}</div>
          </div>
          <!-- 环比趋势指示器 -->
          <div
            :class="[
              'flex items-center gap-0.5 rounded-lg px-2 py-1 text-xs font-semibold',
              card.trend >= 0
                ? 'bg-green-50 text-green-600'
                : 'bg-red-50 text-red-500',
            ]"
          >
            <TrendingUp v-if="card.trend >= 0" :size="13" />
            <TrendingDown v-else :size="13" />
            {{ card.trend >= 0 ? '+' : '' }}{{ card.trend }}%
          </div>
        </div>
      </div>

      <!-- ============ Tier 2：中层洞察 ============ -->
      <div class="mb-6 grid grid-cols-1 gap-5 lg:grid-cols-5">
        <!-- 供需趋势图（60% → 3/5） -->
        <div class="rounded-2xl border border-border bg-card p-5 shadow-card lg:col-span-3">
          <div class="mb-4 flex items-center justify-between">
            <h3 class="text-sm font-semibold text-foreground">近30日供需趋势（简历投递 vs 职位发布）</h3>
            <span class="rounded-lg bg-accent px-2.5 py-1 text-xs text-brand-blue">Smooth Line</span>
          </div>
          <div ref="trendChartRef" style="width: 100%; height: 280px" />
        </div>

        <!-- 行业人才聚集度环形图（40% → 2/5） -->
        <div class="rounded-2xl border border-border bg-card p-5 shadow-card lg:col-span-2">
          <div class="mb-4 flex items-center justify-between">
            <h3 class="text-sm font-semibold text-foreground">行业人才聚集度</h3>
            <span class="rounded-lg bg-accent px-2.5 py-1 text-xs text-brand-blue">Doughnut</span>
          </div>
          <div ref="doughnutChartRef" style="width: 100%; height: 280px" />
        </div>
      </div>

      <!-- ============ Tier 3：微服务节点健康监控 ============ -->
      <div class="rounded-2xl border border-border bg-card p-5 shadow-card">
        <div class="mb-5 flex items-center justify-between">
          <div class="flex items-center gap-2">
            <Server :size="16" class="text-brand-blue" />
            <h3 class="text-sm font-semibold text-foreground">微服务节点健康监控 · Service Health Matrix</h3>
          </div>
          <span class="rounded-lg bg-muted px-2.5 py-1 text-xs text-muted-foreground">
            {{ serviceHealth.filter(s => s.status === 'UP').length }}/{{ serviceHealth.length }} 在线
          </span>
        </div>

        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">
          <div
            v-for="svc in serviceHealth"
            :key="svc.name"
            :class="[
              'relative flex flex-col gap-3 rounded-xl border p-4 transition-all duration-200 hover:shadow-md',
              svc.status === 'UP'
                ? 'border-green-200 bg-green-50/50'
                : 'border-red-200 bg-red-50/50',
            ]"
          >
            <!-- 呼吸灯 + 状态文字 -->
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <span
                  :class="[
                    'relative flex h-3 w-3',
                  ]"
                >
                  <span
                    :class="[
                      'absolute inline-flex h-full w-full animate-ping rounded-full opacity-75',
                      svc.status === 'UP' ? 'bg-green-400' : 'bg-red-400',
                    ]"
                  />
                  <span
                    :class="[
                      'relative inline-flex h-3 w-3 rounded-full',
                      svc.status === 'UP' ? 'bg-green-500' : 'bg-red-500',
                    ]"
                  />
                </span>
                <span
                  :class="[
                    'text-xs font-bold uppercase',
                    svc.status === 'UP' ? 'text-green-600' : 'text-red-500',
                  ]"
                >{{ svc.status }}</span>
              </div>
              <Wifi v-if="svc.status === 'UP'" :size="14" class="text-green-500" />
              <WifiOff v-else :size="14" class="text-red-400" />
            </div>

            <!-- 服务名 -->
            <div class="text-sm font-bold text-foreground">{{ svc.name }}</div>

            <!-- 指标 -->
            <div class="flex items-center gap-3 text-xs text-muted-foreground">
              <div class="flex items-center gap-1">
                <Activity :size="11" />
                <span v-if="svc.status === 'UP'">{{ svc.latency }}ms</span>
                <span v-else class="text-red-400">N/A</span>
              </div>
              <div class="flex items-center gap-1">
                <TrendingUp :size="11" />
                <span>{{ svc.uptime }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>
