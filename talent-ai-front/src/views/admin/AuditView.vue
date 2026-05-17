<script setup lang="ts">
import { computed } from 'vue'
import type { EChartsOption } from 'echarts'
import {
  Search,
  Filter,
  Download,
  Sparkles,
  Shield,
  AlertCircle,
  CheckCircle,
  Clock,
  User,
  FileText,
  BarChart2,
  Eye,
} from 'lucide-vue-next'

const auditLogs = [
  { id: 1, time: '2024-06-15 14:32:11', user: '张HR', role: 'HR管理员', action: 'AI简历分析', target: '候选人#2847 简历', model: 'GPT-4o', tokens: 2840, result: '成功', risk: '低' },
  { id: 2, time: '2024-06-15 14:28:05', user: '李HR', role: 'HR管理员', action: 'AI生成JD', target: '高级前端工程师', model: 'Claude-3.5', tokens: 1200, result: '成功', risk: '低' },
  { id: 3, time: '2024-06-15 14:15:48', user: '王面试官', role: '面试官', action: 'AI面试官模式', target: '候选人张三', model: 'GPT-4o', tokens: 8960, result: '成功', risk: '低' },
  { id: 4, time: '2024-06-15 13:58:23', user: 'admin', role: '超级管理员', action: '批量导出简历数据', target: '全部简历 (234条)', model: '—', tokens: 0, result: '成功', risk: '高' },
  { id: 5, time: '2024-06-15 13:45:02', user: '陈HR', role: 'HR管理员', action: 'AI人才匹配', target: '人才库 Top100搜索', model: '向量嵌入v2', tokens: 450, result: '成功', risk: '低' },
  { id: 6, time: '2024-06-15 13:22:17', user: '未知IP', role: '—', action: '登录尝试失败', target: 'admin账号', model: '—', tokens: 0, result: '失败', risk: '极高' },
  { id: 7, time: '2024-06-15 12:10:44', user: '赵HR', role: 'HR管理员', action: 'AI Offer草拟', target: '候选人李四', model: 'GLM-4', tokens: 3100, result: '成功', risk: '低' },
]

const barData = [
  { name: 'AI分析', count: 284 },
  { name: 'JD生成', count: 128 },
  { name: '面试问题', count: 96 },
  { name: '匹配搜索', count: 420 },
  { name: 'Offer生成', count: 64 },
  { name: '数据导出', count: 32 },
]

const riskConf: Record<string, string> = {
  低: 'text-brand-green bg-green-50 border-green-200',
  高: 'text-brand-orange bg-orange-50 border-orange-200',
  极高: 'text-brand-red bg-red-50 border-red-200',
}

const barOption = computed<EChartsOption>(() => ({
  grid: { left: 48, right: 16, top: 16, bottom: 48 },
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'var(--card)',
    borderColor: 'var(--border)',
    borderWidth: 1,
    textStyle: { fontSize: 12 },
  },
  xAxis: {
    type: 'category',
    data: barData.map((d) => d.name),
    axisLabel: { color: '#64748b', fontSize: 11, interval: 0, rotate: 20 },
    axisLine: { show: false },
    axisTick: { show: false },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#e2e8f0', type: 'dashed' } },
    axisLabel: { color: '#64748b', fontSize: 11 },
  },
  series: [
    {
      type: 'bar',
      data: barData.map((d) => d.count),
      itemStyle: { color: '#5a8a82', borderRadius: [6, 6, 0, 0] },
      barWidth: 28,
    },
  ],
}))
</script>

<template>
  <div data-cmp="Audit" class="p-8 h-full overflow-y-auto scrollbar-thin">
    <div class="max-w-6xl mx-auto">
      <div class="flex items-center justify-between mb-8">
        <div>
          <h1 class="text-2xl font-black text-foreground">AI审计中心</h1>
          <p class="text-muted-foreground mt-1">全量记录并审计所有AI使用行为与系统操作</p>
        </div>
        <button
          type="button"
          class="flex items-center gap-2 px-4 py-2 border border-border rounded-xl text-sm text-muted-foreground hover:bg-muted"
        >
          <Download :size="14" />
          <span>导出审计日志</span>
        </button>
      </div>

      <div class="flex items-center gap-3 px-4 py-3 bg-red-50 border border-red-200 rounded-2xl mb-6">
        <AlertCircle :size="16" class="text-brand-red flex-shrink-0" />
        <span class="text-sm text-brand-red font-medium">
          发现 1 条极高风险操作：admin账号登录尝试失败，IP 来自异常地区
        </span>
        <button type="button" class="ml-auto text-xs text-brand-red border border-red-200 px-3 py-1 rounded-lg">
          查看详情
        </button>
      </div>

      <div class="flex gap-4 mb-8">
        <div
          v-for="s in [
            { label: '今日操作总数', value: '1,284', icon: Clock, cls: 'text-brand-blue bg-brand-tint' },
            { label: 'AI调用次数', value: '992', icon: Sparkles, cls: 'text-brand-purple bg-brand-tint-2' },
            { label: '高风险操作', value: 2, icon: AlertCircle, cls: 'text-brand-orange bg-orange-50' },
            { label: '合规通过率', value: '99.8%', icon: Shield, cls: 'text-brand-green bg-green-50' },
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

      <div class="flex gap-6 mb-6">
        <div class="flex-1 bg-card p-5 shadow-card border border-border">
          <div class="flex items-center gap-2 mb-5">
            <BarChart2 :size="15" class="text-brand-blue" />
            <span class="text-sm font-bold text-foreground">AI功能使用分布</span>
          </div>
          <VChart :option="barOption" autoresize style="height: 180px" />
        </div>
        <div class="w-64 flex-shrink-0 bg-card p-5 shadow-card border border-border">
          <div class="flex items-center gap-2 mb-4">
            <Shield :size="15" class="text-brand-green" />
            <span class="text-sm font-bold text-foreground">风险等级分布</span>
          </div>
          <div class="space-y-3">
            <div v-for="r in [{ label: '低风险', count: 1278, pct: 99.5, color: 'bg-brand-green' }, { label: '高风险', count: 5, pct: 0.4, color: 'bg-brand-orange' }, { label: '极高风险', count: 1, pct: 0.1, color: 'bg-brand-red' }]" :key="r.label">
              <div class="flex justify-between text-xs mb-1">
                <span class="text-muted-foreground">{{ r.label }}</span>
                <span class="font-semibold text-foreground">{{ r.count }}</span>
              </div>
              <div class="h-2 bg-muted rounded-full overflow-hidden">
                <div :class="['h-full rounded-full', r.color]" :style="{ width: `${r.pct}%` }" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="bg-card shadow-card border border-border overflow-hidden">
        <div class="px-5 py-4 border-b border-border flex items-center gap-3">
          <FileText :size="15" class="text-brand-purple" />
          <span class="text-sm font-bold text-foreground">操作日志</span>
          <div class="ml-auto flex items-center gap-2">
            <div class="flex items-center gap-2 px-3 py-2 bg-muted rounded-xl">
              <Search :size="13" class="text-muted-foreground" />
              <input
                class="bg-transparent outline-none text-sm text-foreground placeholder:text-muted-foreground w-40"
                placeholder="搜索日志..."
              />
            </div>
            <button
              type="button"
              class="flex items-center gap-2 px-3 py-2 border border-border rounded-xl text-sm text-muted-foreground hover:bg-muted"
            >
              <Filter :size="13" />
              <span>筛选</span>
            </button>
          </div>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr class="border-b border-border">
                <th
                  v-for="h in ['时间', '操作人', '操作行为', '操作对象', 'AI模型', 'Token消耗', '结果', '风险', '详情']"
                  :key="h"
                  class="px-4 py-3 text-left text-xs text-muted-foreground font-medium whitespace-nowrap"
                >
                  {{ h }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="log in auditLogs"
                :key="log.id"
                class="border-b border-border last:border-0 hover:bg-muted/50 transition-colors"
                :class="[log.risk === '极高' ? 'bg-red-50/40' : '', log.risk === '高' ? 'bg-orange-50/30' : '']"
              >
                <td class="px-4 py-3 text-xs text-muted-foreground whitespace-nowrap">{{ log.time }}</td>
                <td class="px-4 py-3">
                  <div class="flex items-center gap-1.5">
                    <User :size="11" class="text-muted-foreground" />
                    <span class="text-xs text-foreground">{{ log.user }}</span>
                  </div>
                  <div class="text-xs text-muted-foreground">{{ log.role }}</div>
                </td>
                <td class="px-4 py-3">
                  <div class="flex items-center gap-1">
                    <Sparkles :size="11" class="text-brand-purple" />
                    <span class="text-xs text-foreground">{{ log.action }}</span>
                  </div>
                </td>
                <td class="px-4 py-3 text-xs text-muted-foreground max-w-36 truncate">{{ log.target }}</td>
                <td class="px-4 py-3 text-xs text-muted-foreground">{{ log.model }}</td>
                <td class="px-4 py-3 text-xs text-foreground">{{ log.tokens > 0 ? log.tokens.toLocaleString() : '—' }}</td>
                <td class="px-4 py-3">
                  <span
                    :class="[
                      'flex items-center gap-1 text-xs font-medium',
                      log.result === '成功' ? 'text-brand-green' : 'text-brand-red',
                    ]"
                  >
                    <CheckCircle v-if="log.result === '成功'" :size="11" />
                    <AlertCircle v-else :size="11" />
                    {{ log.result }}
                  </span>
                </td>
                <td class="px-4 py-3">
                  <span :class="['text-xs px-2 py-0.5 rounded-full border', riskConf[log.risk]]">{{ log.risk }}</span>
                </td>
                <td class="px-4 py-3">
                  <button type="button" class="p-1.5 rounded-lg hover:bg-muted">
                    <Eye :size="13" class="text-muted-foreground" />
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>
