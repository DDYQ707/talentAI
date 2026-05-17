<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Calendar, Clock, User, Video, Sparkles, CheckCircle, ChevronRight, Briefcase, Star } from 'lucide-vue-next'

const interviews = [
  { id: 1, name: '张三', job: '高级前端工程师', time: '今日 14:00', type: '视频面试', score: 92, status: '待面试', tags: ['名企背景', 'React专家', '高潜力'] },
  { id: 2, name: '李四', job: 'AI产品经理', time: '今日 16:30', type: '现场面试', score: 85, status: '待面试', tags: ['产品思维强', 'BD经验'] },
  { id: 3, name: '王五', job: '后端架构师', time: '明日 10:00', type: '视频面试', score: 88, status: '已安排', tags: ['系统设计', 'Java高级'] },
]

const statusConf: Record<string, string> = {
  待面试: 'bg-brand-tint text-brand-blue border-brand-border',
  已安排: 'bg-brand-tint-2 text-brand-purple border-brand-border',
  已完成: 'bg-green-50 text-brand-green border-green-200',
}

const router = useRouter()
</script>

<template>
  <div data-cmp="InterviewList" class="p-8 h-full overflow-y-auto scrollbar-thin">
    <div class="max-w-5xl mx-auto">
      <div class="flex items-center justify-between mb-8">
        <div>
          <h1 class="text-2xl font-black text-foreground">我的面试列表</h1>
          <p class="text-muted-foreground mt-1">您好，今天有 2 场面试待进行</p>
        </div>
        <button type="button" class="flex items-center gap-2 px-4 py-2 gradient-purple rounded-xl text-white text-sm font-medium shadow-custom" @click="router.push('/interviewer/ai-mode')">
          <Sparkles :size="15" />
          <span>AI面试官模式</span>
        </button>
      </div>
      <div class="flex gap-4 mb-8">
        <div v-for="s in [{ label: '今日面试', value: 2, icon: Calendar, color: 'text-brand-blue bg-brand-tint' }, { label: '本周合计', value: 8, icon: Clock, color: 'text-brand-purple bg-brand-tint-2' }, { label: '已完成', value: 5, icon: CheckCircle, color: 'text-brand-green bg-green-50' }, { label: '平均评分', value: '88分', icon: Star, color: 'text-brand-orange bg-orange-50' }]" :key="s.label" class="flex-1 bg-card p-5 shadow-card border border-border">
          <div :class="['w-10 h-10 rounded-xl flex items-center justify-center mb-3', s.color]">
            <component :is="s.icon" :size="18" />
          </div>
          <div class="text-2xl font-black text-foreground">{{ s.value }}</div>
          <div class="text-sm text-muted-foreground mt-1">{{ s.label }}</div>
        </div>
      </div>
      <div class="flex gap-2 mb-6">
        <button v-for="(tab, i) in ['全部', '今日', '本周', '待面试', '已完成']" :key="tab" type="button" :class="['px-4 py-2 rounded-xl text-sm font-medium border transition-colors', i === 0 ? 'gradient-blue text-white border-transparent shadow-custom' : 'bg-card text-muted-foreground border-border hover:border-brand-blue/30']">
          {{ tab }}
        </button>
      </div>
      <div class="space-y-4">
        <div
          v-for="iv in interviews"
          :key="iv.id"
          role="button"
          class="bg-card p-5 shadow-card border border-border hover:shadow-lg hover:border-brand-blue/20 transition-all cursor-pointer text-left w-full"
          @click="router.push('/interviewer/detail')"
        >
          <div class="flex items-start gap-4">
            <div class="w-12 h-12 rounded-control gradient-blue flex items-center justify-center flex-shrink-0">
              <User :size="20" class="text-white" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between mb-1">
                <div class="flex items-center gap-2">
                  <span class="text-base font-bold text-foreground">{{ iv.name }}</span>
                  <span :class="['text-xs px-2 py-0.5 rounded-full border', statusConf[iv.status]]">{{ iv.status }}</span>
                </div>
                <div class="flex items-center gap-1 text-xs">
                  <Sparkles :size="11" class="text-brand-purple" />
                  <span class="font-bold text-brand-purple">AI {{ iv.score }}分</span>
                </div>
              </div>
              <div class="flex items-center gap-4 text-sm text-muted-foreground mb-2">
                <span class="flex items-center gap-1"><Briefcase :size="13" />{{ iv.job }}</span>
                <span class="flex items-center gap-1"><Clock :size="13" />{{ iv.time }}</span>
                <span class="flex items-center gap-1"><Video :size="13" />{{ iv.type }}</span>
              </div>
              <div class="flex items-center justify-between">
                <div class="flex flex-wrap gap-1">
                  <span v-for="tag in iv.tags" :key="tag" class="text-xs px-2 py-0.5 rounded-full bg-accent text-accent-foreground">{{ tag }}</span>
                </div>
                <ChevronRight :size="16" class="text-muted-foreground flex-shrink-0" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
