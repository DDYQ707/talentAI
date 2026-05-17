<script setup lang="ts">
import { computed } from 'vue'
import type { EChartsOption } from 'echarts'
import { useRouter } from 'vue-router'
import {
  Sparkles,
  User,
  Briefcase,
  Calendar,
  Video,
  Star,
  ChevronRight,
  FileText,
  Mic,
  Lightbulb,
  ClipboardList,
} from 'lucide-vue-next'

const radarData = [
  { subject: '专业技能', A: 92 },
  { subject: '沟通表达', A: 78 },
  { subject: '逻辑思维', A: 88 },
  { subject: '团队合作', A: 85 },
  { subject: '学习能力', A: 90 },
  { subject: '稳定性', A: 80 },
]

const questions = [
  { q: '请介绍一下你在React性能优化方面的实践经验？', category: '技术深度', ai: '重点考察虚拟DOM diff、memo、useMemo等' },
  { q: '描述一次你主导的大型前端项目重构经历？', category: '项目经验', ai: '关注架构决策、风险管理能力' },
]

const radarOption = computed<EChartsOption>(() => ({
  radar: {
    indicator: radarData.map((d) => ({ name: d.subject, max: 100 })),
    splitLine: { lineStyle: { color: 'var(--border)' } },
    axisName: { color: 'var(--muted-foreground)', fontSize: 10 },
  },
  series: [
    {
      type: 'radar',
      data: [{ value: radarData.map((d) => d.A), name: 'score', areaStyle: { color: 'rgba(124,58,237,0.25)' }, lineStyle: { color: '#5a8a82', width: 2 } }],
    },
  ],
}))

const router = useRouter()
</script>

<template>
  <div data-cmp="InterviewDetail" class="h-full overflow-y-auto scrollbar-thin p-8">
    <div class="max-w-6xl mx-auto">
      <div class="flex items-center justify-between mb-8">
        <div>
          <div class="flex items-center gap-2 text-sm text-muted-foreground mb-1">
            <button type="button" class="cursor-pointer hover:text-brand-blue" @click="router.push('/interviewer')">面试列表</button>
            <ChevronRight :size="13" />
            <span class="text-foreground">张三 · 面试详情</span>
          </div>
          <h1 class="text-2xl font-black text-foreground">张三 — 高级前端工程师</h1>
        </div>
        <div class="flex gap-3">
          <button type="button" class="flex items-center gap-2 px-4 py-2 border border-border rounded-xl text-sm text-muted-foreground hover:border-brand-blue/30">
            <FileText :size="15" />
            <span>评价报告</span>
          </button>
          <button type="button" class="flex items-center gap-2 px-4 py-2 gradient-purple rounded-xl text-white text-sm font-medium shadow-custom" @click="router.push('/interviewer/ai-mode')">
            <Mic :size="15" />
            <span>进入AI面试</span>
          </button>
        </div>
      </div>
      <div class="flex gap-6">
        <div class="w-72 flex-shrink-0 space-y-4">
          <div class="bg-card p-5 shadow-card border border-border">
            <div class="text-center mb-4">
              <div class="w-16 h-16 rounded-control gradient-blue mx-auto flex items-center justify-center mb-3">
                <User :size="28" class="text-white" />
              </div>
              <div class="text-base font-bold text-foreground">张三</div>
              <div class="text-sm text-muted-foreground">5年经验 · 清华大学</div>
            </div>
            <div class="space-y-2">
              <div v-for="item in [{ icon: Briefcase, label: '应聘岗位', val: '高级前端工程师' }, { icon: Calendar, label: '面试时间', val: '2024-06-15 14:00' }, { icon: Video, label: '面试形式', val: '视频面试' }]" :key="item.label" class="flex items-center gap-2 text-xs">
                <component :is="item.icon" :size="12" class="text-muted-foreground flex-shrink-0" />
                <span class="text-muted-foreground">{{ item.label }}：</span>
                <span class="text-foreground font-medium">{{ item.val }}</span>
              </div>
            </div>
          </div>
          <div class="bg-card p-5 shadow-card border border-border">
            <div class="flex items-center gap-2 mb-3">
              <Sparkles :size="14" class="text-brand-purple" />
              <span class="text-sm font-semibold text-foreground">AI能力雷达</span>
            </div>
            <VChart :option="radarOption" autoresize style="height: 180px" />
            <div class="text-center">
              <div class="flex items-center justify-center gap-1">
                <Star :size="12" class="text-brand-orange" />
                <span class="text-lg font-black text-foreground">92</span>
                <span class="text-sm text-muted-foreground">/ 100</span>
              </div>
              <div class="text-xs text-muted-foreground">AI综合评估分</div>
            </div>
          </div>
        </div>
        <div class="flex-1 space-y-4">
          <div class="bg-card p-5 shadow-card border border-border">
            <div class="flex items-center gap-2 mb-4">
              <Sparkles :size="15" class="text-brand-purple" />
              <span class="text-base font-bold text-foreground">AI推荐面试题</span>
            </div>
            <div class="space-y-3">
              <div v-for="(q, i) in questions" :key="i" class="border border-border rounded-xl p-4 hover:border-brand-purple/30 transition-colors">
                <div class="flex items-start gap-3">
                  <div class="w-6 h-6 rounded-lg bg-brand-tint-2 flex items-center justify-center flex-shrink-0">
                    <span class="text-xs text-brand-purple font-bold">{{ i + 1 }}</span>
                  </div>
                  <div class="flex-1">
                    <div class="text-sm text-foreground font-medium mb-1">{{ q.q }}</div>
                    <span class="text-xs px-2 py-0.5 rounded-full bg-accent text-accent-foreground">{{ q.category }}</span>
                    <div class="flex items-start gap-1.5 text-xs text-muted-foreground mt-2">
                      <Lightbulb :size="11" class="text-brand-orange mt-0.5 flex-shrink-0" />
                      <span>考察重点：{{ q.ai }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="bg-card p-5 shadow-card border border-border">
            <div class="flex items-center gap-2 mb-4">
              <ClipboardList :size="15" class="text-brand-blue" />
              <span class="text-base font-bold text-foreground">面试评价</span>
            </div>
            <textarea class="w-full bg-muted rounded-xl p-4 text-sm text-muted-foreground outline-none resize-none border border-transparent focus:border-brand-blue/40" rows="4" placeholder="记录面试过程中的观察与感受..." />
            <div class="flex gap-3 mt-3">
              <button type="button" class="flex-1 py-2.5 rounded-xl bg-red-50 text-brand-red text-sm font-medium border border-red-100 hover:bg-red-100">❌ 不推荐</button>
              <button type="button" class="flex-1 py-2.5 rounded-xl bg-green-50 text-brand-green text-sm font-medium border border-green-100 hover:bg-green-100">✅ 推荐通过</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
