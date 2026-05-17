<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ChevronLeft, MapPin, Briefcase, Users, Building2, Sparkles, CheckCircle, Share, Bookmark } from 'lucide-vue-next'

const router = useRouter()
</script>

<template>
  <div data-cmp="JobDetail" class="h-full flex flex-col bg-background">
    <div class="flex items-center gap-3 px-4 py-3 border-b border-border bg-card flex-shrink-0">
      <button type="button" class="p-1.5 rounded-lg hover:bg-muted" @click="router.push('/candidate')">
        <ChevronLeft :size="20" class="text-foreground" />
      </button>
      <span class="text-sm font-semibold text-foreground flex-1">岗位详情</span>
      <button type="button" class="p-1.5 rounded-lg hover:bg-muted">
        <Share :size="16" class="text-muted-foreground" />
      </button>
      <button type="button" class="p-1.5 rounded-lg hover:bg-muted">
        <Bookmark :size="16" class="text-muted-foreground" />
      </button>
    </div>
    <div class="flex-1 overflow-y-auto scrollbar-thin px-4 py-4 pb-24 space-y-4">
      <div class="bg-card p-4 shadow-card border border-border">
        <div class="flex items-start gap-3 mb-3">
          <div class="w-12 h-12 rounded-control gradient-blue flex items-center justify-center flex-shrink-0">
            <Building2 :size="20" class="text-white" />
          </div>
          <div class="flex-1 min-w-0">
            <h1 class="text-base font-bold text-foreground mb-1">高级前端工程师</h1>
            <div class="text-xs text-muted-foreground">智聘科技 · 技术部</div>
          </div>
          <span class="text-base font-black text-brand-blue flex-shrink-0">25-35K</span>
        </div>
        <div class="flex flex-wrap gap-2 text-xs text-muted-foreground mb-3">
          <span class="flex items-center gap-1"><MapPin :size="10" />北京·朝阳</span>
          <span class="flex items-center gap-1"><Briefcase :size="10" />3-5年</span>
          <span class="flex items-center gap-1"><Users :size="10" />本科及以上</span>
        </div>
        <div class="flex flex-wrap gap-1">
          <span v-for="t in ['React', 'TypeScript', 'Vue', '远程可']" :key="t" class="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground">{{ t }}</span>
        </div>
      </div>
      <div class="bg-accent rounded-2xl p-4 border border-brand-border/50">
        <div class="flex items-center gap-2 mb-3">
          <Sparkles :size="14" class="text-brand-purple" />
          <span class="text-xs font-semibold text-brand-purple">AI匹配分析</span>
          <span class="ml-auto text-lg font-black text-brand-purple">96%</span>
        </div>
        <div class="space-y-2">
          <div v-for="m in [{ label: '技术匹配', pct: 98, color: '#3d8b7a' }, { label: '经验匹配', pct: 95, color: '#5a8a82' }, { label: '薪资匹配', pct: 88, color: '#10B981' }]" :key="m.label" class="flex items-center gap-2">
            <span class="text-xs text-muted-foreground w-14">{{ m.label }}</span>
            <div class="flex-1 h-1.5 bg-brand-border/35 rounded-full overflow-hidden">
              <div class="h-full rounded-full" :style="{ width: `${m.pct}%`, background: m.color }" />
            </div>
            <span class="text-xs font-bold text-foreground w-8 text-right">{{ m.pct }}%</span>
          </div>
        </div>
        <div class="mt-3 p-3 bg-white/60 rounded-xl">
          <p class="text-xs text-muted-foreground leading-relaxed">
            AI建议：你的React和TypeScript经验与岗位高度匹配，建议在投递信中重点突出项目优化经历
          </p>
        </div>
      </div>
      <div class="bg-card p-4 shadow-card border border-border">
        <h3 class="text-sm font-semibold text-foreground mb-3">职位描述</h3>
        <div class="space-y-2 text-xs text-muted-foreground leading-relaxed">
          <p>负责公司核心产品前端架构设计与研发，推动前端技术体系演进</p>
          <p>主导前端性能优化，保障页面核心指标（LCP/FID/CLS）达到业界领先水平</p>
          <p>参与技术选型和团队建设，培养初中级工程师成长</p>
        </div>
      </div>
      <div class="bg-card p-4 shadow-card border border-border">
        <h3 class="text-sm font-semibold text-foreground mb-3">任职要求</h3>
        <div class="space-y-2">
          <div v-for="req in ['本科及以上学历，3年以上前端开发经验', '精通React/Vue，有大型项目架构经验', '熟悉TypeScript，有工程化配置经验']" :key="req" class="flex items-start gap-2">
            <CheckCircle :size="12" class="text-brand-green mt-0.5 flex-shrink-0" />
            <span class="text-xs text-muted-foreground">{{ req }}</span>
          </div>
        </div>
      </div>
    </div>
    <div class="flex-shrink-0 px-4 py-4 bg-card border-t border-border">
      <button type="button" class="w-full py-3.5 rounded-control gradient-blue text-white text-sm font-bold shadow-custom" @click="router.push('/candidate/apply')">
        立即投递简历
      </button>
    </div>
  </div>
</template>
