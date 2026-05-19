<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Search, Sparkles, MapPin, Briefcase, Clock, Bookmark } from 'lucide-vue-next'

const jobs = [
  { id: 1, title: '高级前端工程师', company: '智聘科技', dept: '技术部', salary: '25-35K', loc: '北京', type: '全职', time: '3天前', match: 96, hot: true, tags: ['React', 'TypeScript', '远程可'] },
  { id: 2, title: 'AI产品经理', company: '未来AI', dept: '产品部', salary: '20-30K', loc: '上海', type: '全职', time: '1天前', match: 88, hot: false, tags: ['AI产品', '增长', 'B端'] },
  { id: 3, title: '运营总监', company: '云起集团', dept: '运营部', salary: '35-50K', loc: '深圳', type: '全职', time: '5天前', match: 79, hot: true, tags: ['团队管理', 'GMV增长'] },
  { id: 4, title: 'UI/UX设计师', company: '像素工坊', dept: '设计部', salary: '15-22K', loc: '杭州', type: '全职', time: '2天前', match: 92, hot: false, tags: ['Figma', '设计系统'] },
  { id: 5, title: '后端架构师', company: '链上科技', dept: '技术部', salary: '40-60K', loc: '北京', type: '全职', time: '1天前', match: 83, hot: false, tags: ['Go', '微服务', 'K8s'] },
]

const categories = ['推荐', '技术', '产品', '设计', '运营', '管理']

const router = useRouter()
</script>

<template>
  <div data-cmp="JobList" class="flex h-full flex-col overflow-hidden bg-[#EBF4F0]">
    <div class="p-4 bg-card border-b border-border">
      <div class="flex items-center gap-2 bg-muted rounded-2xl px-3 py-2.5">
        <Search :size="15" class="text-muted-foreground flex-shrink-0" />
        <input class="bg-transparent text-sm outline-none flex-1 placeholder:text-muted-foreground" placeholder="搜索岗位、公司..." />
        <div class="flex items-center gap-1 px-2 py-0.5 rounded-full bg-brand-purple/10 border border-brand-border">
          <Sparkles :size="10" class="text-brand-purple" />
          <span class="text-xs text-brand-purple">AI</span>
        </div>
      </div>
      <div class="flex gap-2 mt-3 overflow-x-auto pb-0.5">
        <button
          v-for="(c, i) in categories"
          :key="c"
          type="button"
          :class="[
            'flex-shrink-0 px-3 py-1 rounded-full text-xs font-medium transition-colors',
            i === 0 ? 'gradient-blue text-white' : 'bg-muted text-muted-foreground',
          ]"
        >
          {{ c }}
        </button>
      </div>
    </div>
    <div class="mx-4 mt-4 gradient-blue-purple rounded-2xl p-4 text-white">
      <div class="flex items-center gap-2 mb-1">
        <Sparkles :size="14" />
        <span class="text-sm font-semibold">AI为你推荐了 {{ jobs.length }} 个高匹配岗位</span>
      </div>
      <p class="text-xs text-white/90">基于你的简历和求职意向，为你精准匹配</p>
    </div>
    <div class="flex-1 overflow-y-auto scrollbar-thin px-4 pt-4 pb-2 space-y-3">
      <div
        v-for="job in jobs"
        :key="job.id"
        role="button"
        class="bg-card shadow-card p-4 border border-border text-left w-full cursor-pointer"
        @click="router.push('/candidate/job')"
      >
        <div class="flex items-start justify-between mb-3">
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-1">
              <span v-if="job.hot" class="text-xs px-1.5 py-0.5 rounded-full bg-red-50 text-brand-red border border-red-200 flex-shrink-0">热招</span>
              <span class="text-sm font-bold text-foreground truncate">{{ job.title }}</span>
            </div>
            <div class="text-xs text-muted-foreground">{{ job.company }} · {{ job.dept }}</div>
          </div>
          <button type="button" class="p-1.5 ml-2 flex-shrink-0" @click.stop>
            <Bookmark :size="16" class="text-muted-foreground" />
          </button>
        </div>
        <div class="flex items-center gap-4 mb-3">
          <span class="text-sm font-bold text-brand-blue">{{ job.salary }}</span>
          <span class="flex items-center gap-1 text-xs text-muted-foreground">
            <MapPin :size="10" />{{ job.loc }}
          </span>
          <span class="flex items-center gap-1 text-xs text-muted-foreground">
            <Briefcase :size="10" />{{ job.type }}
          </span>
          <span class="flex items-center gap-1 text-xs text-muted-foreground ml-auto">
            <Clock :size="10" />{{ job.time }}
          </span>
        </div>
        <div class="flex flex-wrap gap-1 mb-3">
          <span v-for="tag in job.tags" :key="tag" class="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground">{{ tag }}</span>
        </div>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-1.5">
            <div class="w-5 h-5 rounded-full gradient-purple flex items-center justify-center">
              <Sparkles :size="10" class="text-white" />
            </div>
            <span class="text-xs text-brand-purple font-semibold">匹配度 {{ job.match }}%</span>
          </div>
          <button type="button" class="px-4 py-1.5 rounded-full gradient-blue text-white text-xs font-medium" @click.stop="router.push('/candidate/job')">
            立即投递
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
