<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChevronLeft, MapPin, Briefcase, Users, Building2, Sparkles, CheckCircle, Share, Bookmark } from 'lucide-vue-next'
import { fetchActiveAppliedJobIds, fetchMyApplications } from '@/api/delivery'
import { fetchFavoriteJobIds, toggleJobFavorite } from '@/api/favorite'
import { fetchJobDetail, type JobPost } from '@/api/job'
import { formatMatchScore } from '@/constants/delivery'
import { resolveApplicationMatchScore, resolveJobPreviewMatchScore } from '@/utils/candidateMatch'
import { formatEmploymentType, formatJobSalary, parseSkillTags } from '@/utils/jobFormat'
import { useCandidateHint } from '@/composables/useCandidateHint'
import { getErrorMessage } from '@/utils/validators'

const router = useRouter()
const route = useRoute()
const { showComingSoon } = useCandidateHint()

const job = ref<JobPost | null>(null)
const loading = ref(false)
const errorMsg = ref('')
const applied = ref(false)
const favorited = ref(false)
const togglingFavorite = ref(false)
const matchScore = ref<number | null>(null)
const matchLoading = ref(false)

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

async function loadAppliedStatus() {
  if (!jobId.value) {
    applied.value = false
    favorited.value = false
    matchScore.value = null
    return
  }
  matchLoading.value = true
  try {
    const [appliedData, appPage, favoriteData] = await Promise.all([
      fetchActiveAppliedJobIds(),
      fetchMyApplications({ current: 1, size: 200 }),
      fetchFavoriteJobIds(),
    ])
    applied.value = (appliedData.jobIds ?? []).includes(jobId.value)
    favorited.value = (favoriteData.jobIds ?? []).includes(jobId.value)
    matchScore.value = null
    if (applied.value) {
      const record = (appPage.records ?? []).find((r) => r.jobId === jobId.value)
      if (record) {
        matchScore.value = await resolveApplicationMatchScore(record)
      }
    } else {
      matchScore.value = await resolveJobPreviewMatchScore(jobId.value)
    }
  } catch {
    applied.value = false
    favorited.value = false
    matchScore.value = null
  } finally {
    matchLoading.value = false
  }
}

const matchScoreDisplay = computed(() => formatMatchScore(matchScore.value))

async function loadJob() {
  if (!jobId.value) {
    errorMsg.value = '缺少岗位信息'
    job.value = null
    applied.value = false
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const [detail] = await Promise.all([fetchJobDetail(jobId.value), loadAppliedStatus()])
    job.value = detail
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '岗位详情加载失败')
    job.value = null
    applied.value = false
  } finally {
    loading.value = false
  }
}

function goApply() {
  if (!jobId.value || applied.value) return
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

function goApplications() {
  router.push('/candidate/applications')
}

function onPrimaryAction() {
  if (applied.value) {
    goApplications()
  } else {
    goApply()
  }
}

async function handleToggleFavorite() {
  if (!jobId.value || togglingFavorite.value) return
  togglingFavorite.value = true
  try {
    const result = await toggleJobFavorite(jobId.value)
    favorited.value = result.favorited
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '收藏操作失败')
  } finally {
    togglingFavorite.value = false
  }
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
      <button type="button" class="p-1.5 rounded-lg hover:bg-muted" title="分享" @click="showComingSoon('分享')">
        <Share :size="16" class="text-muted-foreground" />
      </button>
      <button
        type="button"
        class="p-1.5 rounded-lg hover:bg-muted disabled:opacity-50"
        :title="favorited ? '取消收藏' : '收藏'"
        :disabled="togglingFavorite"
        @click="handleToggleFavorite"
      >
        <Bookmark
          :size="16"
          :class="favorited ? 'text-brand-orange fill-brand-orange' : 'text-muted-foreground'"
        />
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
              <div class="flex items-center gap-2 mb-1">
                <span
                  v-if="applied"
                  class="text-[10px] px-1.5 py-0.5 rounded-full bg-brand-tint text-brand-blue border border-brand-border flex-shrink-0"
                >已投递</span>
                <h1 class="text-base font-bold text-foreground mb-0">{{ job.title }}</h1>
              </div>
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
          <template v-if="matchScoreDisplay !== '—'">
            <div class="flex items-end gap-2 mb-2">
              <span class="text-3xl font-black text-brand-purple">{{ matchScoreDisplay }}</span>
              <span class="text-sm text-muted-foreground mb-1">/ 100</span>
            </div>
            <p class="text-xs text-muted-foreground leading-relaxed">
              {{
                applied
                  ? '基于您投递的简历与岗位要求的 AI 匹配评估，分数越高表示契合度越好。'
                  : '基于您当前简历与岗位要求的 AI 预估匹配，投递后将同步为正式匹配记录。'
              }}
            </p>
          </template>
          <template v-else-if="matchLoading">
            <p class="text-xs text-muted-foreground leading-relaxed">AI 匹配分析生成中，请稍后刷新…</p>
          </template>
          <template v-else-if="applied">
            <p class="text-xs text-muted-foreground leading-relaxed">
              已投递，AI 正在分析匹配度，完成后将在此展示评分。
            </p>
          </template>
          <p v-else class="text-xs text-muted-foreground leading-relaxed">
            基于您当前简历对该岗位的 AI 预估匹配度；完善简历后可获得更准结果。若显示为空，请稍等片刻后刷新。
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
          class="w-full py-3.5 rounded-control text-white text-sm font-bold shadow-custom disabled:opacity-60"
          :class="applied ? 'bg-brand-blue/90' : 'gradient-blue'"
          :disabled="!applied && job.status !== 1"
          @click="onPrimaryAction"
        >
          {{
            applied
              ? '已投递，查看进度'
              : job.status === 1
                ? '立即投递简历'
                : '岗位已关闭'
          }}
        </button>
      </div>
    </template>

    <div v-else class="flex-1 flex flex-col items-center justify-center px-4 text-center">
      <p class="text-sm text-muted-foreground mb-3">{{ errorMsg || '岗位不存在' }}</p>
      <button type="button" class="text-xs text-brand-blue" @click="router.push('/candidate')">返回岗位列表</button>
    </div>
  </div>
</template>
