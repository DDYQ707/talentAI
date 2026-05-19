<script setup lang="ts">
import { Clock, CheckCircle, XCircle, Calendar, Sparkles, ChevronRight } from 'lucide-vue-next'

const apps = [
  { id: 1, job: '高级前端工程师', company: '智聘科技', salary: '25-35K', date: '2024-06-12', stage: '技术面试', status: '进行中', next: '2024-06-15 14:00 技术面试' },
  { id: 2, job: 'AI产品经理', company: '未来AI', salary: '20-30K', date: '2024-06-10', stage: 'HR初面', status: '进行中', next: '等待HR回复' },
  { id: 3, job: '前端工程师', company: '云起集团', salary: '18-25K', date: '2024-06-05', stage: '已发Offer', status: 'offer', next: '请在3天内确认' },
  { id: 4, job: '全栈开发工程师', company: '像素工坊', salary: '20-28K', date: '2024-05-28', stage: '简历筛选', status: '已淘汰', next: '' },
]

const statusConf: Record<string, { cls: string; label: string; icon: typeof Clock }> = {
  进行中: { cls: 'text-brand-blue', label: '进行中', icon: Clock },
  offer: { cls: 'text-brand-green', label: '收到Offer!', icon: CheckCircle },
  已淘汰: { cls: 'text-muted-foreground', label: '未通过', icon: XCircle },
}

const stages = ['简历投递', 'AI初筛', 'HR初面', '技术面试', '终面', 'Offer']
</script>

<template>
  <div data-cmp="Applications" class="flex h-full flex-col bg-[#EBF4F0]">
    <div class="px-4 py-4 bg-card border-b border-border flex-shrink-0">
      <h1 class="text-base font-bold text-foreground">投递状态</h1>
      <p class="text-xs text-muted-foreground mt-0.5">共 {{ apps.length }} 条投递记录</p>
    </div>
    <div class="flex gap-3 px-4 py-3 bg-card border-b border-border flex-shrink-0">
      <div v-for="s in [{ label: '投递中', count: 2, color: 'text-brand-blue bg-brand-tint' }, { label: '面试中', count: 1, color: 'text-brand-purple bg-brand-tint-2' }, { label: '收到Offer', count: 1, color: 'text-brand-green bg-green-50' }]" :key="s.label" :class="['flex-1 rounded-xl px-3 py-2 text-center', s.color]">
        <div class="text-lg font-black">{{ s.count }}</div>
        <div class="text-xs opacity-80">{{ s.label }}</div>
      </div>
    </div>
    <div class="flex-1 overflow-y-auto scrollbar-thin px-4 pt-4 pb-4 space-y-4">
      <div v-for="app in apps" :key="app.id" :class="['bg-card shadow-card border p-4', app.status === 'offer' ? 'border-green-200' : 'border-border']">
        <div class="flex items-start justify-between mb-3">
          <div>
            <div class="text-sm font-bold text-foreground">{{ app.job }}</div>
            <div class="text-xs text-muted-foreground mt-0.5">{{ app.company }} · {{ app.salary }}</div>
          </div>
          <div :class="['flex items-center gap-1 text-xs font-medium', (statusConf[app.status] || statusConf['进行中']).cls]">
            <component :is="(statusConf[app.status] || statusConf['进行中']).icon" :size="12" />
            <span>{{ (statusConf[app.status] || statusConf['进行中']).label }}</span>
          </div>
        </div>
        <div class="flex items-center mb-3">
          <div v-for="(s, i) in stages" :key="s" class="flex items-center flex-1">
            <div class="flex items-center flex-1">
              <div
                class="w-4 h-4 rounded-full flex items-center justify-center flex-shrink-0 text-white text-xs"
                :class="[
                  i < stages.indexOf(app.stage) ? 'bg-brand-green' : i === stages.indexOf(app.stage) ? 'bg-brand-blue' : 'bg-muted',
                ]"
              >
                <CheckCircle v-if="i < stages.indexOf(app.stage)" :size="10" />
                <span v-else>{{ i + 1 }}</span>
              </div>
              <div v-if="i < stages.length - 1" :class="['flex-1 h-0.5', i < stages.indexOf(app.stage) ? 'bg-brand-green' : 'bg-muted']" />
            </div>
          </div>
        </div>
        <div class="text-xs text-muted-foreground mb-1">当前：{{ app.stage }}</div>
        <div v-if="app.next" :class="['flex items-center gap-2 text-xs rounded-lg px-3 py-2 mt-2', app.status === 'offer' ? 'bg-green-50 text-brand-green' : 'bg-accent text-brand-purple']">
          <CheckCircle v-if="app.status === 'offer'" :size="12" />
          <Calendar v-else :size="12" />
          <span>{{ app.next }}</span>
          <ChevronRight :size="12" class="ml-auto" />
        </div>
        <div class="flex items-center justify-between mt-2">
          <span class="text-xs text-muted-foreground">投递时间：{{ app.date }}</span>
          <button type="button" class="flex items-center gap-1 text-xs text-brand-blue">
            <Sparkles :size="11" />
            <span>AI跟进建议</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
