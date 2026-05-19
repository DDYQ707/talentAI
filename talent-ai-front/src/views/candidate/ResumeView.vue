<script setup lang="ts">
import { Plus, Sparkles, Edit, GraduationCap, Briefcase, Code, Download, Eye } from 'lucide-vue-next'

const sections = [
  {
    title: '教育经历',
    icon: GraduationCap,
    color: 'text-brand-blue',
    items: [{ title: '清华大学', sub: '计算机科学与技术 · 本科', period: '2014-2018' }],
  },
  {
    title: '工作经历',
    icon: Briefcase,
    color: 'text-brand-purple',
    items: [
      { title: '字节跳动', sub: '高级前端工程师 · 2021-至今', period: '' },
      { title: '腾讯', sub: '前端工程师 · 2019-2021', period: '' },
    ],
  },
  {
    title: '技能标签',
    icon: Code,
    color: 'text-brand-green',
    skills: ['React', 'TypeScript', 'Vue3', 'Node.js', 'Webpack', '性能优化'],
  },
]
</script>

<template>
  <div data-cmp="Resume" class="flex h-full flex-col bg-[#EBF4F0]">
    <div class="px-4 pt-4 pb-3 bg-card border-b border-border flex-shrink-0">
      <div class="flex items-center justify-between mb-1">
        <h1 class="text-base font-bold text-foreground">我的简历</h1>
        <div class="flex items-center gap-2">
          <button type="button" class="p-1.5 rounded-lg hover:bg-muted">
            <Eye :size="16" class="text-muted-foreground" />
          </button>
          <button type="button" class="p-1.5 rounded-lg hover:bg-muted">
            <Download :size="16" class="text-muted-foreground" />
          </button>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <div class="flex-1 h-1.5 bg-muted rounded-full overflow-hidden">
          <div class="h-full rounded-full gradient-blue" style="width: 82%" />
        </div>
        <span class="text-xs font-semibold text-brand-blue">82%</span>
        <span class="text-xs text-muted-foreground">完整度</span>
      </div>
    </div>
    <div class="flex-1 overflow-y-auto scrollbar-thin px-4 py-4 pb-4 space-y-4">
      <div class="gradient-blue-purple rounded-2xl p-4 text-white">
        <div class="flex items-center gap-2 mb-2">
          <Sparkles :size="14" />
          <span class="text-sm font-semibold">AI简历评分</span>
        </div>
        <div class="flex items-end gap-2 mb-2">
          <span class="text-4xl font-black">88</span>
          <span class="text-white/70 mb-1">/ 100</span>
        </div>
      </div>
      <div class="bg-accent rounded-2xl p-4 border border-brand-border/50">
        <div class="flex items-center gap-2 mb-3">
          <Sparkles :size="14" class="text-brand-purple" />
          <span class="text-xs font-semibold text-brand-purple">AI优化建议</span>
        </div>
        <button type="button" class="mt-3 w-full py-2 rounded-xl gradient-purple text-white text-xs font-medium">AI一键优化</button>
      </div>
      <div v-for="sec in sections" :key="sec.title" class="bg-card shadow-card p-4 border border-border">
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2">
            <component :is="sec.icon" :size="15" :class="sec.color" />
            <span class="text-sm font-semibold text-foreground">{{ sec.title }}</span>
          </div>
          <button type="button" class="p-1.5 rounded-lg hover:bg-muted">
            <Edit :size="14" class="text-muted-foreground" />
          </button>
        </div>
        <template v-if="'items' in sec && sec.items">
          <div v-for="(item, i) in sec.items" :key="i" class="flex items-start justify-between py-2 border-b border-border last:border-0">
            <div>
              <div class="text-xs font-semibold text-foreground">{{ item.title }}</div>
              <div class="text-xs text-muted-foreground mt-0.5">{{ item.sub }}</div>
            </div>
            <span v-if="item.period" class="text-xs text-muted-foreground">{{ item.period }}</span>
          </div>
        </template>
        <div v-if="'skills' in sec && sec.skills" class="flex flex-wrap gap-1">
          <span v-for="skill in sec.skills" :key="skill" class="text-xs px-2 py-1 rounded-full bg-muted text-muted-foreground">{{ skill }}</span>
          <button type="button" class="text-xs px-2 py-1 rounded-full border border-dashed border-brand-blue/30 text-brand-blue">+ 添加</button>
        </div>
      </div>
      <button type="button" class="w-full flex items-center justify-center gap-2 py-3 rounded-2xl border border-dashed border-brand-blue/30 text-brand-blue text-xs">
        <Plus :size="14" />
        <span>添加更多经历</span>
      </button>
    </div>
  </div>
</template>
