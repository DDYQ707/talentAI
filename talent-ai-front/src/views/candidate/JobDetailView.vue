<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChevronLeft, MapPin, Briefcase, Users, Building2, Sparkles, CheckCircle, Share, Bookmark } from 'lucide-vue-next'
import { fetchJobDetail, type JobPost } from '@/api/job'
import { formatEmploymentType, formatJobSalary, parseSkillTags } from '@/utils/jobFormat'
import { getErrorMessage } from '@/utils/validators'

const router = useRouter()
const route = useRoute()

const job = ref<JobPost | null>(null)
const loading = ref(false)
const errorMsg = ref('')

const jobId = computed(() => {
  const id = Number(route.query.id)
  return Number.isFinite(id) && id > 0 ? id : null
})

const tags = computed(() => (job.value ? parseSkillTags(job.value.skillTags) : []))
const requirements = computed(() => {
  const text = job.value?.jobRequirements?.trim()
  if (!text) return []
  return text.split(/\n+/).map((s) => s.trim()).filter(Boolean)
})

async function loadJob() {
  if (!jobId.value) {
    errorMsg.value = '缺少岗位信息'
    job.value = null
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    job.value = await fetchJobDetail(jobId.value)
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '岗位详情加载失败')
    job.value = null
  } finally {
    loading.value = false
  }
}

function goApply() {
  if (!jobId.value) return
  router.push({
    path: '/candidate/apply',
    query: {
      jobId: String(jobId.value),
      title: job.value?.title ?? '',
      salary: job.value ? formatJobSalary(job.value) : '',
      dept: job.value?.deptName ?? '',
    },
  })
}

onMounted(loadJob)
watch(jobId, loadJob)
</script>

<template>
  <div data-cmp="JobDetail" class="flex h-full flex-col bg-[#EBF4F0]">
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

    <div v-if="loading" class="flex-1 flex items-center justify-center text-xs text-muted-foreground">加载中...</div>

    <template v-else-if="job">
      <div class="flex-1 overflow-y-auto scrollbar-thin px-4 py-4 pb-24 space-y-4">
        <div class="bg-card p-4 shadow-card border border-border">
          <div class="flex items-start gap-3 mb-3">
            <div class="w-12 h-12 rounded-control gradient-blue flex items-center justify-center flex-shrink-0">
              <Building2 :size="20" class="text-white" />
            </div>
            <div class="flex-1 min-w-0">
              <h1 class="text-base font-bold text-foreground mb-1">{{ job.title }}</h1>
              <span class="text-xs text-muted-foreground">{{ job.deptName || '—' }}</span>
            </div>
            <span class="text-base font-black text-brand-blue flex-shrink-0">{{ formatJobSalary(job) }}</span>
          </div>
          <div class="flex flex-wrap gap-2 text-xs text-muted-foreground mb-3">
            <span v-if="job.workCity" class="flex items-center gap-1"><MapPin :size="10" />{{ job.workCity }}</span>
            <span class="flex items-center gap-1"><Briefcase :size="10" />{{ formatEmploymentType(job.employmentType) }}</span>
            <span v-if="job.headcount" class="flex items-center gap-1"><Users :size="10" />招 {{ job.headcount }} 人</span>
          </div>
          <div v-if="tags.length" class="flex flex-wrap gap-1">
            <span v-for="t in tags" :key="t" class="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground">{{ t }}</span>
          </div>
        </div>

        <div class="bg-accent rounded-2xl p-4 border border-brand-border/50">
          <div class="flex items-center gap-2 mb-3">
            <Sparkles :size="14" class="text-brand-purple" />
            <span class="text-xs font-semibold text-brand-purple">AI匹配分析</span>
          </div>
          <p class="text-xs text-muted-foreground leading-relaxed">
            完善简历后可获得 AI 匹配度分析；当前岗位已开放投递。
          </p>
        </div>

        <div v-if="job.jobDescription" class="bg-card p-4 shadow-card border border-border">
          <h3 class="text-sm font-semibold text-foreground mb-3">职位描述</h3>
          <p class="text-xs text-muted-foreground leading-relaxed whitespace-pre-line">{{ job.jobDescription }}</p>
        </div>

        <div v-if="requirements.length" class="bg-card p-4 shadow-card border border-border">
          <h3 class="text-sm font-semibold text-foreground mb-3">任职要求</h3>
          <div class="space-y-2">
            <div
              v-for="req in requirements"
              :key="req"
              class="flex items-start gap-2"
            >
              <CheckCircle :size="12" class="text-brand-green mt-0.5 flex-shrink-0" />
              <span class="text-xs text-muted-foreground">{{ req }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="flex-shrink-0 px-4 py-4 bg-card border-t border-border">
        <button
          type="button"
          class="w-full py-3.5 rounded-control gradient-blue text-white text-sm font-bold shadow-custom disabled:opacity-60"
          :disabled="job.status !== 1"
          @click="goApply"
        >
          {{ job.status === 1 ? '立即投递简历' : '岗位已关闭' }}
        </button>
      </div>
    </template>

    <div v-else class="flex-1 flex flex-col items-center justify-center px-4 text-center">
      <p class="text-sm text-muted-foreground mb-3">{{ errorMsg || '岗位不存在' }}</p>
      <button type="button" class="text-xs text-brand-blue" @click="router.push('/candidate')">返回岗位列表</button>
    </div>
  </div>
</template>
