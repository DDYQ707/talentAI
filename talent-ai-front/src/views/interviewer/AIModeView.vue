<script setup lang="ts">
import { ref, computed } from 'vue'
import type { EChartsOption } from 'echarts'
import {
  Sparkles,
  Mic,
  MicOff,
  Video,
  VideoOff,
  PhoneOff,
  MessageSquare,
  ClipboardList,
  Bot,
  User,
  Lightbulb,
  BarChart2,
  Clock,
} from 'lucide-vue-next'

const chatMessages = [
  { role: 'ai' as const, text: '面试开始，请候选人做一个简单的自我介绍，重点介绍你的技术背景和核心项目经验。' },
  { role: 'candidate' as const, text: '您好，我是张三，有5年前端开发经验，主要专注于React生态。在字节跳动期间主导了多个大规模Web应用的性能优化...' },
]

const radarData = [
  { subject: '专业技能', A: 90 },
  { subject: '沟通表达', A: 82 },
  { subject: '逻辑思维', A: 88 },
  { subject: '团队合作', A: 80 },
  { subject: '学习能力', A: 92 },
  { subject: '压力应对', A: 75 },
]

const micOn = ref(true)
const camOn = ref(true)

const radarOption = computed<EChartsOption>(() => ({
  radar: {
    indicator: radarData.map((d) => ({ name: d.subject, max: 100 })),
    splitLine: { lineStyle: { color: 'var(--border)' } },
    axisName: { color: 'var(--muted-foreground)', fontSize: 9 },
  },
  series: [
    {
      type: 'radar',
      data: [{ value: radarData.map((d) => d.A), name: 'score', areaStyle: { color: 'rgba(124,58,237,0.2)' }, lineStyle: { color: '#5a8a82' } }],
    },
  ],
}))
</script>

<template>
  <div data-cmp="AIMode" class="h-full overflow-y-auto scrollbar-thin p-6">
    <div class="max-w-7xl mx-auto">
      <div class="flex items-center justify-between mb-6">
        <div class="flex items-center gap-3">
          <div class="flex items-center gap-2 px-3 py-1.5 rounded-full bg-red-50 border border-red-200">
            <div class="w-2 h-2 rounded-full bg-brand-red animate-pulse" />
            <span class="text-xs text-brand-red font-semibold">REC</span>
          </div>
          <div class="flex items-center gap-1.5 text-sm text-muted-foreground">
            <Clock :size="14" />
            <span>00:23:45</span>
          </div>
          <div class="text-sm text-muted-foreground">张三 · 高级前端工程师</div>
        </div>
        <div class="flex items-center gap-2">
          <button type="button" class="flex items-center gap-2 px-3 py-2 rounded-xl border border-border text-sm text-muted-foreground hover:bg-muted">
            <ClipboardList :size="14" />
            <span>生成报告</span>
          </button>
          <button type="button" class="flex items-center gap-2 px-3 py-2 rounded-xl bg-red-50 text-brand-red text-sm font-medium border border-red-200 hover:bg-red-100">
            <PhoneOff :size="14" />
            <span>结束面试</span>
          </button>
        </div>
      </div>
      <div class="flex gap-6">
        <div class="flex-1 space-y-4">
          <div class="bg-slate-900 rounded-2xl overflow-hidden" style="aspect-ratio: 16/7">
            <div class="h-full flex items-center justify-center relative min-h-[200px]">
              <div class="flex flex-col items-center gap-3 text-slate-400">
                <div class="w-20 h-20 rounded-full bg-slate-700 flex items-center justify-center">
                  <User :size="36" class="text-slate-400" />
                </div>
                <span class="text-sm">候选人视频画面</span>
              </div>
              <div class="absolute bottom-4 right-4 w-32 h-20 bg-slate-700 rounded-xl flex items-center justify-center border-2 border-slate-600">
                <div class="flex flex-col items-center gap-1 text-slate-500">
                  <Video :size="16" />
                  <span class="text-xs">面试官</span>
                </div>
              </div>
              <div class="absolute top-4 left-4 flex items-center gap-2 px-3 py-1.5 bg-brand-purple/80 rounded-full">
                <Bot :size="13" class="text-white" />
                <span class="text-xs text-white font-medium">AI面试官已接管</span>
              </div>
            </div>
          </div>
          <div class="flex justify-center gap-4">
            <button type="button" :class="['w-12 h-12 rounded-2xl flex items-center justify-center transition-colors', micOn ? 'bg-card border border-border hover:bg-muted' : 'bg-red-50 border border-red-200 text-brand-red']" @click="micOn = !micOn">
              <Mic v-if="micOn" :size="18" class="text-foreground" />
              <MicOff v-else :size="18" />
            </button>
            <button type="button" :class="['w-12 h-12 rounded-2xl flex items-center justify-center transition-colors', camOn ? 'bg-card border border-border hover:bg-muted' : 'bg-red-50 border border-red-200 text-brand-red']" @click="camOn = !camOn">
              <Video v-if="camOn" :size="18" class="text-foreground" />
              <VideoOff v-else :size="18" />
            </button>
            <button type="button" class="w-12 h-12 rounded-2xl bg-card border border-border flex items-center justify-center hover:bg-muted">
              <MessageSquare :size="18" class="text-foreground" />
            </button>
          </div>
          <div class="bg-card shadow-card border border-border">
            <div class="flex items-center gap-2 px-5 py-4 border-b border-border">
              <MessageSquare :size="15" class="text-brand-blue" />
              <span class="text-sm font-semibold text-foreground">AI对话记录</span>
            </div>
            <div class="p-4 space-y-3 max-h-64 overflow-y-auto scrollbar-thin">
              <div v-for="(msg, i) in chatMessages" :key="i" class="flex gap-2" :class="msg.role === 'candidate' ? 'flex-row-reverse' : ''">
                <div :class="['w-7 h-7 rounded-xl flex items-center justify-center flex-shrink-0', msg.role === 'ai' ? 'gradient-purple' : 'gradient-blue']">
                  <Bot v-if="msg.role === 'ai'" :size="13" class="text-white" />
                  <User v-else :size="13" class="text-white" />
                </div>
                <div :class="['max-w-lg px-3 py-2 rounded-xl text-xs leading-relaxed', msg.role === 'ai' ? 'bg-accent text-foreground' : 'bg-brand-blue/10 text-foreground']">
                  {{ msg.text }}
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="w-72 flex-shrink-0 space-y-4">
          <div class="bg-card p-5 shadow-card border border-border">
            <div class="flex items-center gap-2 mb-3">
              <Sparkles :size="14" class="text-brand-purple" />
              <span class="text-sm font-semibold text-foreground">实时AI评分</span>
            </div>
            <div class="text-center mb-3">
              <div class="text-4xl font-black gradient-purple-text">87</div>
              <div class="text-xs text-muted-foreground">综合评分 (实时更新)</div>
            </div>
            <VChart :option="radarOption" autoresize style="height: 150px" />
          </div>
          <div class="bg-accent rounded-2xl p-4 border border-brand-border/50">
            <div class="flex items-center gap-2 mb-2">
              <Lightbulb :size="13" class="text-brand-purple" />
              <span class="text-xs font-semibold text-brand-purple">AI下一问题推荐</span>
            </div>
            <p class="text-xs text-foreground leading-relaxed">在大型团队协作中，你如何保证代码规范和架构一致性？有没有推行成功的案例？</p>
            <button type="button" class="mt-3 w-full py-1.5 rounded-xl gradient-purple text-white text-xs font-medium">使用此问题</button>
          </div>
          <div class="bg-card p-4 shadow-card border border-border">
            <div class="flex items-center gap-2 mb-3">
              <BarChart2 :size="14" class="text-brand-blue" />
              <span class="text-sm font-semibold text-foreground">关键信号捕捉</span>
            </div>
            <div class="space-y-2">
              <div v-for="s in [{ signal: '量化成果意识强', type: 'positive' }, { signal: '熟悉前端工程化全链路', type: 'positive' }]" :key="s.signal" class="flex items-center gap-2">
                <div :class="['w-1.5 h-1.5 rounded-full flex-shrink-0', s.type === 'positive' ? 'bg-brand-green' : 'bg-brand-orange']" />
                <span class="text-xs text-muted-foreground">{{ s.signal }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
