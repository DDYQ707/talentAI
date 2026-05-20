<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Sparkles, MapPin, Briefcase, Bookmark } from 'lucide-vue-next'
import { fetchJobList, type JobPost } from '@/api/job'
import { formatEmploymentType, formatJobSalary, parseSkillTags } from '@/utils/jobFormat'
import { getErrorMessage } from '@/utils/validators'

const categories = ['推荐', '技术', '产品', '设计', '运营', '管理']

const router = useRouter()
const jobs = ref<JobPost[]>([])
const loading = ref(false)
const errorMsg = ref('')

async function loadJobs() {
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await fetchJobList({ current: 1, size: 50, status: 1 })
    jobs.value = data.records ?? []
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '岗位列表加载失败')
    jobs.value = []
  } finally {
    loading.value = false
  }
}

function goDetail(job: JobPost) {
  router.push({ path: '/candidate/job', query: { id: String(job.id) } })
}

function jobTags(job: JobPost): string[] {
  const tags = parseSkillTags(job.skillTags)
  if (job.isRemote && !tags.includes('远程可')) tags.push('远程可')
  return tags.slice(0, 4)
}

const displayJobs = computed(() => jobs.value)

onMounted(() => {
  loadJobs()
})
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
        <span class="text-sm font-semibold">AI为你推荐了 {{ displayJobs.length }} 个高匹配岗位</span>
      </div>
      <p class="text-xs text-white/90">基于你的简历和求职意向，为你精准匹配</p>
    </div>

    <div v-if="errorMsg" class="mx-4 mt-3 text-xs text-red-600 bg-red-50 border border-red-100 rounded-xl px-3 py-2">
      {{ errorMsg }}
    </div>

    <div class="flex-1 overflow-y-auto scrollbar-thin px-4 pt-4 pb-2 space-y-3">
      <div v-if="loading" class="text-center text-xs text-muted-foreground py-8">加载中...</div>
      <div v-else-if="!displayJobs.length" class="text-center text-xs text-muted-foreground py-8">
        暂无招聘中的岗位
      </div>

      <div
        v-for="job in displayJobs"
        :key="job.id"
        role="button"
        class="bg-card shadow-card p-4 border border-border text-left w-full cursor-pointer"
        @click="goDetail(job)"
      >
        <div class="flex items-start justify-between mb-3">
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-1">
              <span
                v-if="(job.appliedCount ?? 0) > 10"
                class="text-xs px-1.5 py-0.5 rounded-full bg-red-50 text-brand-red border border-red-200 flex-shrink-0"
              >热招</span>
              <span class="text-sm font-bold text-foreground truncate">{{ job.title }}</span>
            </div>
            <div class="text-xs text-muted-foreground">{{ job.deptName || '—' }}</div>
          </div>
          <button type="button" class="p-1.5 ml-2 flex-shrink-0" @click.stop>
            <Bookmark :size="16" class="text-muted-foreground" />
          </button>
        </div>
        <div class="flex items-center gap-4 mb-3 flex-wrap">
          <span class="text-sm font-bold text-brand-blue">{{ formatJobSalary(job) }}</span>
          <span v-if="job.workCity" class="flex items-center gap-1 text-xs text-muted-foreground">
            <MapPin :size="10" />{{ job.workCity }}
          </span>
          <span class="flex items-center gap-1 text-xs text-muted-foreground">
            <Briefcase :size="10" />{{ formatEmploymentType(job.employmentType) }}
          </span>
        </div>
        <div v-if="jobTags(job).length" class="flex flex-wrap gap-1 mb-3">
          <span v-for="tag in jobTags(job)" :key="tag" class="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground">{{ tag }}</span>
        </div>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-1.5">
            <div class="w-5 h-5 rounded-full gradient-purple flex items-center justify-center">
              <Sparkles :size="10" class="text-white" />
            </div>
            <span class="text-xs text-brand-purple font-semibold">匹配度 —</span>
          </div>
          <button
            type="button"
            class="px-4 py-1.5 rounded-full gradient-blue text-white text-xs font-medium"
            @click.stop="goDetail(job)"
          >
            立即投递
          </button>
        </div>
      </div>
    </div>
  </div>
</template>