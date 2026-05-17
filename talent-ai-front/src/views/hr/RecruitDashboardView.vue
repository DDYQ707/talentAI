<script setup lang="ts">
import { computed } from 'vue'
import type { EChartsOption } from 'echarts'
import { TrendingUp, Users, Briefcase, Calendar, Sparkles, Zap, AlertTriangle, CheckCircle } from 'lucide-vue-next'

const trendData = [
  { month: '1月', 投递: 120, 面试: 45, offer: 12 },
  { month: '2月', 投递: 98, 面试: 38, offer: 9 },
  { month: '3月', 投递: 156, 面试: 62, offer: 18 },
  { month: '4月', 投递: 142, 面试: 55, offer: 14 },
  { month: '5月', 投递: 189, 面试: 71, offer: 22 },
  { month: '6月', 投递: 234, 面试: 88, offer: 28 },
]

const deptData = [
  { dept: '技术部', 缺口: 8, 在招: 5 },
  { dept: '产品部', 缺口: 3, 在招: 3 },
  { dept: '运营部', 缺口: 5, 在招: 4 },
  { dept: '设计部', 缺口: 2, 在招: 1 },
  { dept: '市场部', 缺口: 4, 在招: 3 },
]

const channelData = [
  { name: 'BOSS直聘', value: 38, color: '#3d8b7a' },
  { name: '猎头', value: 22, color: '#5a8a82' },
  { name: '内推', value: 20, color: '#10B981' },
  { name: '智联招聘', value: 12, color: '#F59E0B' },
  { name: '其他', value: 8, color: '#94A3B8' },
]

const funnelData = [
  { name: '简历投递', value: 234, fill: '#3d8b7a' },
  { name: 'AI初筛通过', value: 156, fill: '#3B82F6' },
  { name: 'HR面试', value: 88, fill: '#5a8a82' },
  { name: '技术面试', value: 52, fill: '#8B5CF6' },
  { name: 'Offer发放', value: 28, fill: '#10B981' },
]

const kpis = [
  { label: '本月投递', value: 234, trend: '+24%', positive: true, icon: Users, color: 'text-brand-blue', bg: 'gradient-blue' },
  { label: '面试完成', value: 88, trend: '+12%', positive: true, icon: Calendar, color: 'text-brand-purple', bg: 'gradient-purple' },
  { label: 'Offer发放', value: 28, trend: '+8%', positive: true, icon: Briefcase, color: 'text-brand-green', bg: 'bg-brand-green' },
  { label: '平均招聘天数', value: '23天', trend: '-2天', positive: true, icon: TrendingUp, color: 'text-brand-orange', bg: 'bg-brand-orange' },
]

const trendOption = computed<EChartsOption>(() => ({
  grid: { left: 48, right: 24, top: 32, bottom: 28 },
  tooltip: { trigger: 'axis' },
  legend: { data: ['投递', '面试', 'offer'], textStyle: { fontSize: 11 } },
  xAxis: {
    type: 'category',
    data: trendData.map((d) => d.month),
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
      data: trendData.map((d) => d['投递']),
      lineStyle: { color: '#3d8b7a', width: 2 },
      areaStyle: { color: 'rgba(37,99,235,0.12)' },
    },
    {
      name: '面试',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: trendData.map((d) => d['面试']),
      lineStyle: { color: '#5a8a82', width: 2 },
      areaStyle: { color: 'rgba(124,58,237,0.12)' },
    },
    {
      name: 'offer',
      type: 'line',
      data: trendData.map((d) => d.offer),
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
    data: deptData.map((d) => d.dept),
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#94a3b8', fontSize: 11 },
  },
  series: [
    { name: '缺口', type: 'bar', data: deptData.map((d) => d['缺口']), itemStyle: { color: '#E2E8F0', borderRadius: [0, 4, 4, 0] } },
    { name: '在招', type: 'bar', data: deptData.map((d) => d['在招']), itemStyle: { color: '#3d8b7a', borderRadius: [0, 4, 4, 0] } },
  ],
}))

const pieOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item' },
  series: [
    {
      type: 'pie',
      radius: ['32%', '58%'],
      center: ['50%', '50%'],
      data: channelData.map((c) => ({ name: c.name, value: c.value, itemStyle: { color: c.color } })),
      label: { show: false },
    },
  ],
}))
</script>

<template>
  <div data-cmp="RecruitDashboard" class="p-6 space-y-5" style="height: calc(100vh - 64px); overflow-y: auto">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-xl font-bold text-foreground">招聘数据驾驶舱</h1>
        <p class="text-sm text-muted-foreground mt-0.5">2024年6月 · 实时数据更新</p>
      </div>
      <div class="flex items-center gap-2 px-4 py-2 rounded-xl bg-accent border border-brand-border/50">
        <Sparkles :size="14" class="text-brand-purple" />
        <span class="text-xs text-brand-purple font-medium">AI智能分析已开启</span>
      </div>
    </div>

    <div class="flex gap-4">
      <div v-for="k in kpis" :key="k.label" class="flex-1 bg-card shadow-card p-4 flex items-center gap-4">
        <div :class="['w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 text-white', k.bg]">
          <component :is="k.icon" :size="18" />
        </div>
        <div class="flex-1 min-w-0">
          <div class="text-2xl font-black text-foreground">{{ k.value }}</div>
          <div class="text-xs text-muted-foreground">{{ k.label }}</div>
        </div>
        <div :class="['text-xs font-medium', k.positive ? 'text-brand-green' : 'text-brand-red']">{{ k.trend }}</div>
      </div>
    </div>

    <div class="flex gap-5">
      <div class="flex-[2] bg-card shadow-card p-5">
        <h3 class="text-sm font-semibold text-foreground mb-4">招聘趋势 (近6个月)</h3>
        <VChart :option="trendOption" autoresize style="height: 200px" />
      </div>
      <div class="flex-1 bg-card shadow-card p-5">
        <h3 class="text-sm font-semibold text-foreground mb-4">本月招聘漏斗</h3>
        <div class="space-y-2">
          <div v-for="f in funnelData" :key="f.name" class="space-y-1">
            <div class="flex items-center justify-between text-xs">
              <span class="text-muted-foreground">{{ f.name }}</span>
              <span class="font-bold text-foreground">{{ f.value }}</span>
            </div>
            <div class="h-6 bg-muted rounded-lg overflow-hidden">
              <div
                class="h-full rounded-lg flex items-center px-2 transition-all"
                :style="{ width: `${Math.round((f.value / funnelData[0].value) * 100)}%`, background: f.fill }"
              >
                <span class="text-white text-xs font-bold">{{ Math.round((f.value / funnelData[0].value) * 100) }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="flex gap-5">
      <div class="flex-[2] bg-card shadow-card p-5">
        <h3 class="text-sm font-semibold text-foreground mb-4">各部门招聘进度</h3>
        <VChart :option="deptBarOption" autoresize style="height: 180px" />
      </div>
      <div class="flex-1 bg-card shadow-card p-5">
        <h3 class="text-sm font-semibold text-foreground mb-4">招聘渠道分布</h3>
        <div class="flex items-center gap-4">
          <div class="w-[120px] h-[120px] flex-shrink-0">
            <VChart :option="pieOption" autoresize style="width: 120px; height: 120px" />
          </div>
          <div class="space-y-2">
            <div v-for="c in channelData" :key="c.name" class="flex items-center gap-2">
              <div class="w-2 h-2 rounded-full flex-shrink-0" :style="{ background: c.color }" />
              <span class="text-xs text-muted-foreground">{{ c.name }}</span>
              <span class="text-xs font-bold text-foreground ml-auto">{{ c.value }}%</span>
            </div>
          </div>
        </div>
      </div>
      <div class="flex-1 bg-accent rounded-2xl p-5 border border-brand-border/50">
        <div class="flex items-center gap-2 mb-3">
          <Zap :size="14" class="text-brand-purple" />
          <span class="text-xs font-semibold text-brand-purple">AI洞察</span>
        </div>
        <div class="space-y-3">
          <div v-for="(insight, i) in [
            { type: 'positive', text: '本月技术岗投递量同比增长28%，建议加快面试流程' },
            { type: 'warning', text: '前端岗位平均招聘周期超出目标3天，建议优化' },
            { type: 'positive', text: '内推渠道质量最高，接受率达82%，建议扩大内推激励' },
          ]" :key="i" class="flex items-start gap-2">
            <CheckCircle v-if="insight.type === 'positive'" :size="12" class="text-brand-green mt-0.5 flex-shrink-0" />
            <AlertTriangle v-else :size="12" class="text-brand-orange mt-0.5 flex-shrink-0" />
            <p class="text-xs text-muted-foreground leading-relaxed">{{ insight.text }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
