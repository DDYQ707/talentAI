<script setup lang="ts">
import { computed } from 'vue'
import type { EChartsOption } from 'echarts'
import { Bot, Zap, BarChart2, Activity, Settings, Sparkles, CheckCircle, AlertCircle, Play, Pause } from 'lucide-vue-next'

const models = [
  { id: 1, name: 'GPT-4o (OpenAI)', type: '语言模型', purpose: '简历分析、AI助手、面试官', status: '运行中', calls: '12,480', latency: '1.2s', tokens: '2.4M', cost: '¥1,280', accent: 'text-brand-green' },
  { id: 2, name: 'Claude-3.5-Sonnet', type: '语言模型', purpose: '职位描述生成、Offer信', status: '运行中', calls: '3,240', latency: '0.9s', tokens: '680K', cost: '¥420', accent: 'text-brand-green' },
  { id: 3, name: '智谱GLM-4', type: '语言模型', purpose: '中文简历理解、标注', status: '运行中', calls: '8,920', latency: '0.7s', tokens: '1.8M', cost: '¥360', accent: 'text-brand-green' },
  { id: 4, name: '向量嵌入模型 v2', type: '嵌入模型', purpose: '人才库语义搜索', status: '运行中', calls: '42,000', latency: '0.1s', tokens: '—', cost: '¥180', accent: 'text-brand-green' },
  { id: 5, name: 'CV-Parser Pro', type: '文档解析', purpose: '简历结构化提取', status: '暂停', calls: '0', latency: '—', tokens: '—', cost: '¥0', accent: 'text-muted-foreground' },
]

const usageData = [
  { date: '6/8', calls: 1200 },
  { date: '6/9', calls: 1800 },
  { date: '6/10', calls: 1400 },
  { date: '6/11', calls: 2200 },
  { date: '6/12', calls: 1900 },
  { date: '6/13', calls: 2600 },
  { date: '6/14', calls: 3100 },
]

const usageOption = computed<EChartsOption>(() => ({
  grid: { left: 48, right: 24, top: 16, bottom: 28 },
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'var(--card)',
    borderColor: 'var(--border)',
    borderWidth: 1,
    textStyle: { fontSize: 12 },
  },
  xAxis: {
    type: 'category',
    data: usageData.map((d) => d.date),
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#64748b', fontSize: 11 },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#e2e8f0', type: 'dashed' } },
    axisLabel: { color: '#64748b', fontSize: 11 },
  },
  series: [
    {
      type: 'line',
      name: '调用量',
      data: usageData.map((d) => d.calls),
      smooth: true,
      showSymbol: false,
      lineStyle: { width: 2, color: '#3d8b7a' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(37,99,235,0.25)' },
            { offset: 1, color: 'rgba(37,99,235,0)' },
          ],
        },
      },
    },
  ],
}))
</script>

<template>
  <div data-cmp="AIModels" class="p-8 h-full overflow-y-auto scrollbar-thin">
    <div class="max-w-6xl mx-auto">
      <div class="flex items-center justify-between mb-8">
        <div>
          <h1 class="text-2xl font-black text-foreground">AI模型管理</h1>
          <p class="text-muted-foreground mt-1">管理和监控系统中所有AI模型的运行状态</p>
        </div>
        <div class="flex gap-3">
          <button
            type="button"
            class="flex items-center gap-2 px-4 py-2 border border-border rounded-xl text-sm text-muted-foreground hover:bg-muted"
          >
            <Settings :size="14" />
            <span>全局配置</span>
          </button>
          <button
            type="button"
            class="flex items-center gap-2 px-4 py-2 gradient-blue rounded-xl text-white text-sm font-medium shadow-custom"
          >
            <Bot :size="14" />
            <span>接入新模型</span>
          </button>
        </div>
      </div>

      <div class="flex gap-4 mb-8">
        <div
          v-for="s in [
            { label: '活跃模型', value: 4, icon: Bot, cls: 'text-brand-blue bg-brand-tint' },
            { label: '今日调用量', value: '3,100', icon: Zap, cls: 'text-brand-purple bg-brand-tint-2' },
            { label: '本月花费', value: '¥2,240', icon: BarChart2, cls: 'text-brand-orange bg-orange-50' },
            { label: '平均延迟', value: '0.73s', icon: Activity, cls: 'text-brand-green bg-green-50' },
          ]"
          :key="s.label"
          class="flex-1 bg-card p-5 shadow-card border border-border"
        >
          <div :class="['w-10 h-10 rounded-xl flex items-center justify-center mb-3', s.cls]">
            <component :is="s.icon" :size="18" />
          </div>
          <div class="text-2xl font-black text-foreground">{{ s.value }}</div>
          <div class="text-sm text-muted-foreground mt-1">{{ s.label }}</div>
        </div>
      </div>

      <div class="bg-card p-5 shadow-card border border-border mb-6">
        <div class="flex items-center justify-between mb-5">
          <div class="flex items-center gap-2">
            <BarChart2 :size="15" class="text-brand-blue" />
            <span class="text-sm font-bold text-foreground">近7日 AI调用趋势</span>
          </div>
          <div class="flex gap-2">
            <button
              v-for="(t, i) in ['全部模型', 'GPT-4o', 'GLM-4']"
              :key="t"
              type="button"
              class="text-xs px-3 py-1.5 rounded-lg border"
              :class="
                i === 0
                  ? 'gradient-blue text-white border-transparent'
                  : 'border-border text-muted-foreground hover:border-brand-blue/30'
              "
            >
              {{ t }}
            </button>
          </div>
        </div>
        <VChart :option="usageOption" autoresize style="height: 180px" />
      </div>

      <div class="bg-card shadow-card border border-border overflow-hidden">
        <div class="px-5 py-4 border-b border-border flex items-center gap-2">
          <Bot :size="15" class="text-brand-purple" />
          <span class="text-sm font-bold text-foreground">模型列表</span>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr class="border-b border-border">
                <th
                  v-for="h in ['模型名称', '类型', '使用场景', '状态', '调用次数', '平均延迟', '消耗Tokens', '本月费用', '操作']"
                  :key="h"
                  class="px-4 py-3 text-left text-xs text-muted-foreground font-medium"
                >
                  {{ h }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="m in models"
                :key="m.id"
                class="border-b border-border last:border-0 hover:bg-muted/50 transition-colors"
              >
                <td class="px-4 py-3">
                  <div class="flex items-center gap-2">
                    <div class="w-7 h-7 rounded-lg gradient-purple flex items-center justify-center">
                      <Sparkles :size="12" class="text-white" />
                    </div>
                    <span class="text-sm font-medium text-foreground">{{ m.name }}</span>
                  </div>
                </td>
                <td class="px-4 py-3 text-xs text-muted-foreground">{{ m.type }}</td>
                <td class="px-4 py-3 text-xs text-muted-foreground max-w-36 truncate">{{ m.purpose }}</td>
                <td class="px-4 py-3">
                  <span
                    :class="[
                      'flex items-center gap-1 text-xs font-medium',
                      m.status === '运行中' ? 'text-brand-green' : 'text-muted-foreground',
                    ]"
                  >
                    <CheckCircle v-if="m.status === '运行中'" :size="11" />
                    <AlertCircle v-else :size="11" />
                    {{ m.status }}
                  </span>
                </td>
                <td class="px-4 py-3 text-sm font-medium text-foreground">{{ m.calls }}</td>
                <td class="px-4 py-3 text-sm text-muted-foreground">{{ m.latency }}</td>
                <td class="px-4 py-3 text-sm text-muted-foreground">{{ m.tokens }}</td>
                <td class="px-4 py-3 text-sm font-medium text-foreground">{{ m.cost }}</td>
                <td class="px-4 py-3">
                  <div class="flex items-center gap-1">
                    <button
                      v-if="m.status === '运行中'"
                      type="button"
                      class="p-1.5 rounded-lg bg-orange-50 hover:bg-orange-100"
                    >
                      <Pause :size="13" class="text-brand-orange" />
                    </button>
                    <button v-else type="button" class="p-1.5 rounded-lg bg-green-50 hover:bg-green-100">
                      <Play :size="13" class="text-brand-green" />
                    </button>
                    <button type="button" class="p-1.5 rounded-lg hover:bg-muted">
                      <Settings :size="13" class="text-muted-foreground" />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>
