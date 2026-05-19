<script setup lang="ts">
import { ref, computed } from 'vue'
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
  TrendingUp,
  ArrowUp,
  ArrowDown,
  RefreshCw
} from 'lucide-vue-next'

// --- 顶部统计卡片数据 ---
const statsCards = [
  { label: '在招岗位', value: '42', subLabel: '较上周', subValue: '+5', icon: Briefcase, color: 'text-teal-600', bg: 'bg-teal-50' },
  { label: '本月投递', value: '1,284', subLabel: '较上月', subValue: '+18%', icon: Send, color: 'text-teal-600', bg: 'bg-teal-50' },
  { label: '面试进行中', value: '86', subLabel: '今日安排', subValue: '12 场', icon: Calendar, color: 'text-teal-600', bg: 'bg-teal-50' },
  { label: '本月录用', value: '23', isProgress: true, progressRate: 82, subLabel: 'Offer接受率', subValue: '82%', icon: CheckCircle, color: 'text-orange-500', bg: 'bg-orange-50' },
]

// --- 招聘漏斗数据 ---
const funnelData = [
  { name: '简历投递', value: 1284, fill: '#3d8b7a' },
  { name: 'AI筛选通过', value: 856, fill: '#5a8a82' },
  { name: 'HR初筛', value: 412, fill: '#6ba898' },
  { name: '面试安排', value: 186, fill: '#85a185' },
  { name: '终面', value: 56, fill: '#7aab9c' },
  { name: '录用', value: 23, fill: '#ac9c8a' },
]

// --- 招聘趋势数据 ---
const trendData = [
  { month: '1月', 投递: 820, 面试: 140, 录用: 18 },
  { month: '2月', 投递: 650, 面试: 110, 录用: 12 },
  { month: '3月', 投递: 960, 面试: 180, 录用: 25 },
  { month: '4月', 投递: 1100, 面试: 210, 录用: 30 },
  { month: '5月', 投递: 980, 面试: 190, 录用: 22 },
  { month: '6月', 投递: 1284, 面试: 220, 录用: 28 },
]

const trendOption = computed<EChartsOption>(() => ({
  grid: { left: 40, right: 16, top: 20, bottom: 24 },
  tooltip: { trigger: 'axis', textStyle: { fontSize: 12 } },
  legend: { data: ['投递', '面试', '录用'], textStyle: { fontSize: 11 }, right: 0, top: 0, itemWidth: 8, itemHeight: 8, icon: 'circle' },
  xAxis: {
    type: 'category',
    data: trendData.map((d) => d.month),
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
    { name: '投递', type: 'line', data: trendData.map((d) => d['投递']), smooth: true, showSymbol: false, lineStyle: { color: '#3d8b7a', width: 2 } },
    { name: '面试', type: 'line', data: trendData.map((d) => d['面试']), smooth: true, showSymbol: false, lineStyle: { color: '#85a185', width: 2 } },
    { name: '录用', type: 'line', data: trendData.map((d) => d['录用']), smooth: true, showSymbol: false, lineStyle: { color: '#475569', width: 2 } },
  ],
}))

// --- 部门进度数据 ---
const deptBarData = [
  { dept: '技术部', 目标: 11, 完成: 9 },
  { dept: '产品部', 目标: 7, 完成: 4 },
  { dept: '运营部', 目标: 10, 完成: 6 },
  { dept: '市场部', 目标: 8, 完成: 5 },
  { dept: '设计部', 目标: 6, 完成: 6 },
]

const barDeptOption = computed<EChartsOption>(() => ({
  grid: { left: 30, right: 16, top: 20, bottom: 24 },
  tooltip: { trigger: 'axis', textStyle: { fontSize: 12 } },
  legend: { data: ['目标', '完成'], textStyle: { fontSize: 11 }, right: 0, top: 0, itemWidth: 12, itemHeight: 4, icon: 'rect' },
  xAxis: {
    type: 'category',
    data: deptBarData.map((d) => d.dept),
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
    { name: '目标', type: 'bar', data: deptBarData.map((d) => d['目标']), itemStyle: { color: '#e2e8f0', borderRadius: [2, 2, 0, 0] }, barWidth: 12 },
    { name: '完成', type: 'bar', data: deptBarData.map((d) => d['完成']), itemStyle: { color: '#3d8b7a', borderRadius: [2, 2, 0, 0] }, barWidth: 12 },
  ],
}))

// --- 待办事项 ---
const todos = [
  { label: '待初筛简历', count: 47, dot: 'bg-slate-300', numColor: 'text-teal-600' },
  { label: '待安排面试', count: 12, dot: 'bg-orange-300', numColor: 'text-orange-500' },
  { label: 'Offer待审批', count: 5, dot: 'bg-teal-300', numColor: 'text-teal-600' },
  { label: '今日面试提醒', count: 8, dot: 'bg-teal-300', numColor: 'text-teal-600' },
  { label: '背调待完成', count: 3, dot: 'bg-red-300', numColor: 'text-red-500' },
]

// --- AI 助手数据 ---
const aiSuggestions = [
  { title: '高匹配候选人', desc: '发现 12 位高匹配候选人', icon: Star, iconColor: 'text-teal-600', iconBg: 'bg-teal-50', btn: '查看' },
  { title: '岗位风险预警', desc: '前端工程师岗位竞争激烈', icon: AlertTriangle, iconColor: 'text-orange-500', iconBg: 'bg-orange-50', btn: '分析' },
  { title: 'Offer 待确认', desc: '有 5 份 Offer 待确认处理', icon: Mail, iconColor: 'text-purple-500', iconBg: 'bg-purple-50', btn: '处理' },
]
const shortcuts = [
  { label: '智能筛选', icon: Filter },
  { label: '生成 JD', icon: FileText },
  { label: '安排面试', icon: Calendar },
  { label: '数据分析', icon: BarChart2 },
]
const aiInput = ref('')

// --- AI 洞察数据 ---
const aiInsights = [
  { label: 'AI处理简历', value: '486 份', trend: 'up', trendVal: '18%' },
  { label: '高意向候选人', value: '23 人', trend: 'up', trendVal: '15%' },
  { label: '风险预警', value: '5 条', trend: 'down', trendVal: '10%' },
  { label: '智能匹配成功', value: '142 对', trend: 'up', trendVal: '22%' },
]
</script>

<template>
  <div data-cmp="HRWorkbench" class="min-h-screen bg-[#EBF4F0] p-6 text-slate-800">
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">我的工作台</h1>
        <p class="mt-1 text-sm text-slate-500">2024年6月14日 星期五 · 欢迎回来，张招聘</p>
      </div>
      <div class="flex items-center gap-2 rounded-full border border-slate-200 bg-white px-4 py-2 text-sm text-slate-600 shadow-sm cursor-pointer hover:bg-slate-50 transition-colors">
        <div class="h-2 w-2 rounded-full bg-teal-500" />
        <span>系统运行正常</span>
        <RefreshCw :size="14" class="ml-1 text-slate-400" />
      </div>
    </div>

    <div class="flex gap-6">
      
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
              <h3 class="font-bold text-slate-800">招聘漏斗</h3>
              <select class="rounded-md border border-slate-200 px-2 py-1 text-xs text-slate-500 outline-none">
                <option>本月数据</option>
              </select>
            </div>
            <div class="space-y-3.5">
              <div v-for="(item, i) in funnelData" :key="item.name" class="flex items-center gap-4">
                <div class="w-20 shrink-0 text-sm text-slate-500">{{ item.name }}</div>
                <div class="flex-1 h-7 rounded-full bg-slate-100">
                  <div
                    class="flex h-full items-center rounded-full px-3 transition-all duration-500"
                    :style="{ width: `${(item.value / 1284) * 100}%`, backgroundColor: item.fill, minWidth: '50px' }"
                  >
                    <span class="text-xs font-medium text-white">{{ item.value }}</span>
                  </div>
                </div>
                <div class="w-10 shrink-0 text-right text-sm text-slate-400">
                  {{ i === 0 ? '100%' : `${Math.round((item.value / 1284) * 100)}%` }}
                </div>
              </div>
            </div>
          </div>

          <div class="rounded-xl bg-white p-5 shadow-sm border border-slate-100 flex flex-col">
            <div class="mb-2 flex items-center justify-between">
              <h3 class="font-bold text-slate-800">招聘趋势</h3>
            </div>
            <div class="flex-1 min-h-[220px]">
               <VChart :option="trendOption" autoresize class="w-full h-full" />
            </div>
          </div>
        </div>

        <div class="grid grid-cols-5 gap-6">
          <div class="col-span-2 rounded-xl bg-white p-5 shadow-sm border border-slate-100">
            <div class="mb-5 flex items-center justify-between">
              <h3 class="font-bold text-slate-800">待办事项</h3>
              <span class="cursor-pointer text-xs text-teal-600 hover:underline">查看全部</span>
            </div>
            <div class="space-y-1">
              <div v-for="t in todos" :key="t.label" class="flex items-center justify-between border-b border-slate-50 py-3 last:border-0 hover:bg-slate-50 rounded-lg px-2 -mx-2 transition-colors">
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
              <h3 class="font-bold text-slate-800">部门招聘进度</h3>
              <select class="rounded-md border border-slate-200 px-2 py-1 text-xs text-slate-500 outline-none">
                <option>本月</option>
              </select>
            </div>
            <div class="flex-1 min-h-[180px]">
              <VChart :option="barDeptOption" autoresize class="w-full h-full" />
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
            <div class="ml-auto flex items-center gap-1.5 text-xs text-slate-400">
              <div class="h-1.5 w-1.5 rounded-full bg-teal-500 shadow-[0_0_4px_rgba(20,184,166,0.8)]" />
              AI 助手已在线
            </div>
          </div>

          <div class="mb-2 text-xs text-slate-400 pl-1 border-l-2 border-slate-200">今日重点建议</div>
          <div class="mb-6 space-y-3">
            <div v-for="item in aiSuggestions" :key="item.title" class="flex items-center gap-3 rounded-xl border border-slate-100 bg-slate-50/50 p-3 hover:bg-slate-50 transition-colors">
              <div :class="['flex h-8 w-8 shrink-0 items-center justify-center rounded-lg', item.iconBg]">
                <component :is="item.icon" :size="16" :class="item.iconColor" />
              </div>
              <div class="flex-1 overflow-hidden">
                <div class="truncate text-sm font-bold text-slate-800">{{ item.title }}</div>
                <div class="truncate text-xs text-slate-500 mt-0.5">{{ item.desc }}</div>
              </div>
              <button class="shrink-0 rounded-full border border-teal-200 px-3 py-1 text-xs text-teal-600 hover:bg-teal-50 transition-colors">
                {{ item.btn }}
              </button>
            </div>
          </div>

          <div class="mb-3 text-xs text-slate-400 pl-1 border-l-2 border-slate-200">快捷操作</div>
          <div class="grid grid-cols-2 gap-2 mb-auto">
            <button v-for="btn in shortcuts" :key="btn.label" class="flex items-center justify-center gap-1.5 rounded-full border border-slate-200 bg-white py-1.5 text-xs text-slate-600 hover:border-teal-500 hover:text-teal-600 transition-colors">
              <component :is="btn.icon" :size="12" />
              {{ btn.label }}
            </button>
          </div>

          <div class="relative mt-6">
            <input
              v-model="aiInput"
              class="w-full rounded-full border border-slate-200 bg-slate-50 py-3 pl-4 pr-12 text-sm outline-none focus:border-teal-500 focus:bg-white transition-all"
              placeholder="向AI助手提问..."
            />
            <button class="absolute right-2 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full bg-teal-600 text-white hover:bg-teal-700 transition-colors">
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
                  <circle cx="44" cy="44" r="38" stroke="currentColor" stroke-width="6" fill="transparent" stroke-dasharray="238.7" stroke-dashoffset="19" class="text-teal-600" />
                </svg>
                <span class="absolute text-3xl font-bold text-slate-800">92</span>
              </div>
              <div class="mt-2 text-center">
                <div class="text-xs text-slate-500">招聘健康度</div>
                <div class="text-sm font-bold text-teal-600 mt-0.5">优秀</div>
              </div>
            </div>
            
            <div class="flex-1 space-y-3">
              <div v-for="item in aiInsights" :key="item.label" class="flex items-center justify-between">
                <span class="text-xs text-slate-500">{{ item.label }}</span>
                <div class="flex items-center gap-2">
                  <span class="text-sm font-bold text-slate-800">{{ item.value }}</span>
                  <div :class="['flex items-center text-xs', item.trend === 'up' ? 'text-teal-500' : 'text-orange-500']">
                    <ArrowUp v-if="item.trend === 'up'" :size="12" />
                    <ArrowDown v-else :size="12" />
                    {{ item.trendVal }}
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <div class="mt-5 rounded-lg bg-slate-50 py-2.5 text-center text-xs text-slate-500">
            较昨日综合提升 <span class="font-bold text-teal-600">12% ↗</span>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped>
/* 可按需引入外部样式，使用 Tailwind 时一般留空即可 */
</style>