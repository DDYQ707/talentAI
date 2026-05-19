<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ChevronLeft, FileText, Sparkles, CheckCircle, Upload } from 'lucide-vue-next'

const router = useRouter()
</script>

<template>
  <div data-cmp="Apply" class="flex h-full flex-col bg-[#EBF4F0]">
    <div class="flex items-center gap-3 px-4 py-3 border-b border-border bg-card flex-shrink-0">
      <button type="button" class="p-1.5 rounded-lg hover:bg-muted" @click="router.push('/candidate/job')">
        <ChevronLeft :size="20" />
      </button>
      <span class="text-sm font-semibold text-foreground flex-1">投递简历</span>
    </div>
    <div class="flex-1 overflow-y-auto scrollbar-thin px-4 py-4 pb-24 space-y-4">
      <div class="bg-card p-4 shadow-card border border-border">
        <div class="text-xs text-muted-foreground mb-1">投递岗位</div>
        <div class="flex items-center justify-between">
          <div>
            <div class="text-sm font-bold text-foreground">高级前端工程师</div>
            <div class="text-xs text-muted-foreground">智聘科技 · 25-35K</div>
          </div>
          <span class="text-xs px-2 py-1 rounded-full bg-green-50 text-brand-green border border-green-200">96% 匹配</span>
        </div>
      </div>
      <div class="bg-card p-4 shadow-card border border-border">
        <h3 class="text-sm font-semibold text-foreground mb-3">选择简历</h3>
        <div
          v-for="r in [
            { name: '在线简历 v2.1', date: '更新于2024-06-10', active: true },
            { name: '附件简历.pdf', date: '上传于2024-05-28', active: false },
          ]"
          :key="r.name"
          class="flex items-center gap-3 p-3 rounded-xl mb-2 border transition-colors cursor-pointer"
          :class="r.active ? 'border-brand-blue bg-brand-tint' : 'border-border hover:border-brand-blue/30'"
        >
          <FileText :size="16" :class="r.active ? 'text-brand-blue' : 'text-muted-foreground'" />
          <div class="flex-1 min-w-0">
            <div :class="['text-xs font-medium', r.active ? 'text-brand-blue' : 'text-foreground']">{{ r.name }}</div>
            <div class="text-xs text-muted-foreground">{{ r.date }}</div>
          </div>
          <CheckCircle v-if="r.active" :size="16" class="text-brand-blue flex-shrink-0" />
        </div>
        <button type="button" class="w-full flex items-center justify-center gap-2 py-2.5 border border-dashed border-border rounded-xl text-xs text-muted-foreground hover:border-brand-blue/40 mt-1">
          <Upload :size="14" />
          <span>上传新简历</span>
        </button>
      </div>
      <div class="bg-accent rounded-2xl p-4 border border-brand-border/50">
        <div class="flex items-center gap-2 mb-3">
          <Sparkles :size="14" class="text-brand-purple" />
          <span class="text-xs font-semibold text-brand-purple">AI简历优化</span>
        </div>
        <p class="text-xs text-muted-foreground leading-relaxed mb-3">AI分析发现，针对此岗位可优化以下内容，提升匹配度</p>
        <button type="button" class="mt-3 w-full py-2 rounded-xl gradient-purple text-white text-xs font-medium">AI一键优化简历</button>
      </div>
    </div>
    <div class="flex-shrink-0 px-4 py-4 bg-card border-t border-border">
      <button type="button" class="w-full py-3.5 rounded-control gradient-blue text-white text-sm font-bold shadow-custom" @click="router.push('/candidate/applications')">
        确认投递
      </button>
    </div>
  </div>
</template>
