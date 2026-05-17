<script setup lang="ts">
import { ref, computed } from 'vue'
import type { EChartsOption } from 'echarts'
import {
  Users,
  Briefcase,
  Calendar,
  TrendingUp,
  Bot,
  Send,
  ArrowUp,
  Clock,
  CheckCircle,
  AlertCircle,
  Sparkles,
  Zap,
} from 'lucide-vue-next'

const statsCards = [
  { label: '在招岗位', value: '42', change: '+5', icon: Briefcase, color: 'text-brand-blue', bg: 'bg-brand-tint' },
  { label: '本月投递', value: '1,284', change: '+18%', icon: Send, color: 'text-brand-purple', bg: 'bg-brand-tint-2' },
  { label: '面试进行中', value: '86', change: '+12', icon: Calendar, color: 'text-brand-green', bg: 'bg-brand-tint' },
  { label: '本月录用', value: '23', change: '+8%', icon: CheckCircle, color: 'text-brand-orange', bg: 'bg-brand-tint-2' },
]

const funnelData = [
  { name: '简历投递', value: 1284, fill: '#3d8b7a' },
  { name: 'AI筛选通过', value: 856, fill: '#5a8a82' },
  { name: 'HR初筛', value: 412, fill: '#6ba898' },
  { name: '面试安排', value: 186, fill: '#85a185' },
  { name: '终面', value: 56, fill: '#7aab9c' },
  { name: '录用', value: 23, fill: '#ac9c8a' },
]

const trendData = [
  { month: '1月', 投递: 820, 面试: 140, 录用: 18 },
  { month: '2月', 投递: 650, 面试: 110, 录用: 12 },
  { month: '3月', 投递: 960, 面试: 180, 录用: 25 },
  { month: '4月', 投递: 1100, 面试: 210, 录用: 30 },
  { month: '5月', 投递: 980, 面试: 190, 录用: 22 },
  { month: '6月', 投递: 1284, 面试: 220, 录用: 28 },
]

const todos = [
  { label: '待初筛简历', count: 47, color: 'text-brand-blue', urgent: false },
  { label: '待安排面试', count: 12, color: 'text-brand-orange', urgent: true },
  { label: 'Offer待审批', count: 5, color: 'text-brand-purple', urgent: true },
  { label: '今日面试提醒', count: 8, color: 'text-brand-green', urgent: false },
  { label: '背调待完成', count: 3, color: 'text-brand-red', urgent: true },
]

const aiSuggestions = [
  '📊 今日有12份高匹配度简历等待处理，建议优先查阅',
  '🎯 前端工程师岗位竞争激烈，建议提前1周安排初面',
  '⚠️ 产品经理岗位已30天无进展，建议重新评估JD',
  '🤖 AI识别到3位候选人可能在多家公司面试，建议加急',
]

const aiInput = ref('')

const trendOption = computed<EChartsOption>(() => ({
  grid: { left: 40, right: 16, top: 16, bottom: 28 },
  tooltip: { trigger: 'axis', textStyle: { fontSize: 12 } },
  legend: { data: ['投递', '面试', '录用'], textStyle: { fontSize: 11 } },
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
    axisLine: { show: false },
    axisTick: { show: false },
  },
  series: [
    { name: '投递', type: 'line', data: trendData.map((d) => d['投递']), smooth: true, showSymbol: false, lineStyle: { color: '#3d8b7a', width: 2 } },
    { name: '面试', type: 'line', data: trendData.map((d) => d['面试']), smooth: true, showSymbol: false, lineStyle: { color: '#5a8a82', width: 2 } },
    { name: '录用', type: 'line', data: trendData.map((d) => d['录用']), smooth: true, showSymbol: false, lineStyle: { color: '#85a185', width: 2 } },
  ],
}))

const deptBarData = [
  { dept: '技术部', 目标: 10, 完成: 7 },
  { dept: '产品部', 目标: 5, 完成: 3 },
  { dept: '运营部', 目标: 8, 完成: 5 },
  { dept: '市场部', 目标: 6, 完成: 4 },
  { dept: '设计部', 目标: 4, 完成: 4 },
]

const barDeptOption = computed<EChartsOption>(() => ({
  grid: { left: 48, right: 16, top: 16, bottom: 28 },
  tooltip: { trigger: 'axis', textStyle: { fontSize: 12 } },
  legend: { data: ['目标', '完成'], textStyle: { fontSize: 11 } },
  xAxis: {
    type: 'category',
    data: deptBarData.map((d) => d.dept),
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
    { name: '目标', type: 'bar', data: deptBarData.map((d) => d['目标']), itemStyle: { color: '#E2E8F0', borderRadius: [4, 4, 0, 0] }, barWidth: 16 },
    { name: '完成', type: 'bar', data: deptBarData.map((d) => d['完成']), itemStyle: { color: '#3d8b7a', borderRadius: [4, 4, 0, 0] }, barWidth: 16 },
  ],
}))
</script>

<template>
  <div data-cmp="HRWorkbench" class="p-6 space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-xl font-bold text-foreground">我的工作台</h1>
        <p class="text-sm text-muted-foreground mt-1">2024年6月14日 星期五 · 欢迎回来，张招聘</p>
      </div>
      <div class="flex items-center gap-1.5 rounded-full border border-brand-border bg-brand-tint px-3 py-1.5 text-xs text-brand-green">
        <div class="w-1.5 h-1.5 rounded-full bg-brand-green" />
        <span>系统运行正常</span>
      </div>
    </div>

    <div class="flex gap-4">
      <div v-for="card in statsCards" :key="card.label" class="flex-1 bg-card p-5 shadow-card">
        <div class="flex items-center justify-between mb-4">
          <div :class="['w-10 h-10 rounded-xl flex items-center justify-center', card.bg]">
            <component :is="card.icon" :size="20" :class="card.color" />
          </div>
          <div class="flex items-center gap-1 text-xs text-brand-green">
            <ArrowUp :size="12" />
            <span>{{ card.change }}</span>
          </div>
        </div>
        <div class="text-2xl font-bold text-foreground mb-1">{{ card.value }}</div>
        <div class="text-sm text-muted-foreground">{{ card.label }}</div>
      </div>
    </div>

    <div class="flex gap-6">
      <div class="flex-1 bg-card p-5 shadow-card">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-sm font-semibold text-foreground">招聘漏斗</h3>
          <span class="text-xs text-muted-foreground">本月数据</span>
        </div>
        <div class="space-y-2">
          <div v-for="(item, i) in funnelData" :key="item.name" class="flex items-center gap-3">
            <div class="text-xs text-muted-foreground w-20 flex-shrink-0">{{ item.name }}</div>
            <div class="flex-1 h-7 bg-muted rounded-lg overflow-hidden">
              <div
                class="h-full rounded-lg flex items-center px-3 transition-all"
                :style="{
                  width: `${(item.value / 1284) * 100}%`,
                  background: item.fill,
                  minWidth: '60px',
                }"
              >
                <span class="text-white text-xs font-medium">{{ item.value }}</span>
              </div>
            </div>
            <div class="text-xs text-muted-foreground w-12 text-right flex-shrink-0">
              {{ i === 0 ? '100%' : `${Math.round((item.value / 1284) * 100)}%` }}
            </div>
          </div>
        </div>
      </div>

      <div class="flex-1 bg-card p-5 shadow-card">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-sm font-semibold text-foreground">招聘趋势</h3>
          <div class="flex items-center gap-4 text-xs">
            <div class="flex items-center gap-1">
              <div class="w-2 h-2 rounded-full bg-brand-blue" />
              <span class="text-muted-foreground">投递</span>
            </div>
            <div class="flex items-center gap-1">
              <div class="w-2 h-2 rounded-full bg-brand-purple" />
              <span class="text-muted-foreground">面试</span>
            </div>
            <div class="flex items-center gap-1">
              <div class="w-2 h-2 rounded-full bg-brand-green" />
              <span class="text-muted-foreground">录用</span>
            </div>
          </div>
        </div>
        <VChart :option="trendOption" autoresize style="height: 180px" />
      </div>

      <div class="w-72 flex-shrink-0 bg-card p-5 shadow-card flex flex-col">
        <div class="flex items-center gap-2 mb-4">
          <div class="w-7 h-7 rounded-lg gradient-purple flex items-center justify-center">
            <Bot :size="14" class="text-white" />
          </div>
          <h3 class="text-sm font-semibold text-foreground">AI招聘助手</h3>
          <div class="ml-auto w-1.5 h-1.5 rounded-full bg-brand-green" />
        </div>
        <div class="flex-1 space-y-2 mb-3">
          <div v-for="(s, i) in aiSuggestions" :key="i" class="text-xs text-muted-foreground bg-accent rounded-lg p-2.5 leading-relaxed">
            {{ s }}
          </div>
        </div>
        <div class="flex gap-2">
          <input
            v-model="aiInput"
            class="flex-1 px-3 py-2 rounded-lg bg-muted text-xs outline-none border border-border focus:border-brand-purple"
            placeholder="问AI助手..."
          />
          <button type="button" class="px-3 py-2 rounded-lg gradient-purple text-white text-xs">
            <Send :size="12" />
          </button>
        </div>
      </div>
    </div>

    <div class="flex gap-6">
      <div class="w-80 flex-shrink-0 bg-card p-5 shadow-card">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-sm font-semibold text-foreground">待办事项</h3>
          <span class="text-xs text-brand-blue cursor-pointer">查看全部</span>
        </div>
        <div class="space-y-3">
          <div
            v-for="t in todos"
            :key="t.label"
            class="flex items-center justify-between py-2 border-b border-border last:border-0"
          >
            <div class="flex items-center gap-2">
              <AlertCircle v-if="t.urgent" :size="14" class="text-brand-orange" />
              <Clock v-else :size="14" class="text-muted-foreground" />
              <span class="text-sm text-foreground">{{ t.label }}</span>
            </div>
            <div :class="['text-sm font-bold', t.color]">{{ t.count }}</div>
          </div>
        </div>
      </div>

      <div class="flex-1 bg-card p-5 shadow-card">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-sm font-semibold text-foreground">部门招聘进度</h3>
          <span class="text-xs text-muted-foreground">本月</span>
        </div>
        <VChart :option="barDeptOption" autoresize style="height: 160px" />
      </div>

      <div class="w-72 flex-shrink-0 bg-card p-5 shadow-card">
        <div class="flex items-center gap-2 mb-4">
          <Sparkles :size="16" class="text-brand-purple" />
          <h3 class="text-sm font-semibold text-foreground">AI今日洞察</h3>
        </div>
        <div class="space-y-3">
          <div
            v-for="item in [
              { label: 'AI处理简历', value: '486份', icon: Zap, color: 'text-brand-blue' },
              { label: '高意向候选人', value: '23人', icon: Users, color: 'text-brand-green' },
              { label: '风险预警', value: '5条', icon: AlertCircle, color: 'text-brand-orange' },
              { label: '智能匹配成功', value: '142对', icon: TrendingUp, color: 'text-brand-purple' },
            ]"
            :key="item.label"
            class="flex items-center justify-between"
          >
            <div class="flex items-center gap-2">
              <component :is="item.icon" :size="14" :class="item.color" />
              <span class="text-xs text-muted-foreground">{{ item.label }}</span>
            </div>
            <span :class="['text-sm font-bold', item.color]">{{ item.value }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
