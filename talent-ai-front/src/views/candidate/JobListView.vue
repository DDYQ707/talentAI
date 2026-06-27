<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Sparkles, MapPin, Briefcase, Bookmark } from 'lucide-vue-next'
import { fetchProfileCompleteness } from '@/api/candidateProfile'
import { fetchActiveAppliedJobIds, fetchMyApplications } from '@/api/delivery'
import { fetchFavoriteJobIds, toggleJobFavorite } from '@/api/favorite'
import { fetchJobList, type JobPost } from '@/api/job'
import { fetchOnlineResumeList } from '@/api/onlineResume'
import { formatMatchScore } from '@/constants/delivery'
import { JOB_LIST_CATEGORIES, resolveCategoryDeptNames, type JobListCategory } from '@/constants/jobCategory'
import { ONLINE_RESUME_MIN_SUBMIT_SCORE } from '@/constants/onlineResume'
import { useMatchScorePoll } from '@/composables/useMatchScorePoll'
import { loadJobListPreviewMatchStates, sortJobsByMatchScore } from '@/utils/candidateMatch'
import { formatEmploymentType, formatJobSalary, parseSkillTags } from '@/utils/jobFormat'
import { getErrorMessage } from '@/utils/validators'

const categories = JOB_LIST_CATEGORIES

const router = useRouter()
const jobs = ref<JobPost[]>([])
const total = ref(0)
const loading = ref(false)
const errorMsg = ref('')
const searchKeyword = ref('')
const activeCategory = ref(0)
const appliedJobIds = ref<Set<number>>(new Set())
const favoriteJobIds = ref<Set<number>>(new Set())
const togglingFavoriteId = ref<number | null>(null)
const jobMatchScores = ref<Map<number, number>>(new Map())
const jobMatchPending = ref<Set<number>>(new Set())
const jobMatchFailed = ref<Set<number>>(new Set())
const resumeReady = ref(false)
const checkingResume = ref(false)

let searchTimer: ReturnType<typeof setTimeout> | undefined

const matchPoll = useMatchScorePoll(
  async () => {
    if (!resumeReady.value || jobMatchPending.value.size === 0) return true
    const result = await loadJobListPreviewMatchStates(
      jobs.value.map((job) => job.id),
      { triggerMissing: false },
    )
    jobMatchScores.value = result.scores
    jobMatchPending.value = result.pendingIds
    jobMatchFailed.value = result.failedIds
    return result.pendingIds.size === 0
  },
  4000,
  30,
)

const activeCategoryLabel = computed(() => categories[activeCategory.value] as JobListCategory)

const searchText = computed(() => searchKeyword.value.trim())

const listBannerText = computed(() => {
  if (searchText.value) {
    return total.value > 0
      ? `找到 ${total.value} 个与「${searchText.value}」相关的岗位`
      : `未找到与「${searchText.value}」相关的岗位`
  }
  if (activeCategoryLabel.value !== '推荐') {
    return total.value > 0
      ? `${activeCategoryLabel.value}部门共 ${total.value} 个岗位`
      : `${activeCategoryLabel.value}部门暂无在招岗位`
  }
  return `共 ${total.value} 个招聘中岗位`
})

const listBannerHint = computed(() => {
  if (!resumeReady.value && !checkingResume.value) {
    return '完善在线简历（完整度≥80%）后，将自动估算各岗位匹配度并按从高到低排序'
  }
  if (jobMatchPending.value.size > 0) {
    return 'AI 正在估算各岗位匹配度，列表将随评分完成自动重排'
  }
  if (resumeReady.value && jobMatchScores.value.size > 0) {
    return '已按 AI 预估匹配度从高到低排序，分数越高表示与您的简历越契合'
  }
  if (searchText.value) {
    return '支持岗位名称、部门、城市、技能标签等模糊搜索'
  }
  if (activeCategoryLabel.value !== '推荐') {
    return `「${activeCategoryLabel.value}」部门岗位，按预估匹配度排序`
  }
  return '浏览全部开放岗位，或使用上方搜索快速筛选'
})

async function loadAppliedJobs() {
  try {
    const [appliedData, appPage, favoriteData] = await Promise.all([
      fetchActiveAppliedJobIds(),
      fetchMyApplications({ current: 1, size: 200 }),
      fetchFavoriteJobIds(),
    ])
    appliedJobIds.value = new Set((appliedData.jobIds ?? []).filter((id) => id > 0))
    favoriteJobIds.value = new Set((favoriteData.jobIds ?? []).filter((id) => id > 0))
    return appPage.records ?? []
  } catch {
    appliedJobIds.value = new Set()
    favoriteJobIds.value = new Set()
    return []
  }
}

async function checkResumeReady(): Promise<boolean> {
  checkingResume.value = true
  try {
    const [profile, resumes] = await Promise.all([fetchProfileCompleteness(), fetchOnlineResumeList()])
    if (!profile.complete) return false
    if (!resumes.length) return false
    const bestCompleteness = resumes.reduce((max, item) => Math.max(max, item.completeness ?? 0), 0)
    return bestCompleteness >= ONLINE_RESUME_MIN_SUBMIT_SCORE
  } catch {
    return false
  } finally {
    checkingResume.value = false
  }
}

async function refreshMatchScores() {
  if (!resumeReady.value) {
    jobMatchScores.value = new Map()
    jobMatchPending.value = new Set()
    jobMatchFailed.value = new Set()
    matchPoll.stop()
    return
  }
  const jobIds = jobs.value.map((job) => job.id)
  const result = await loadJobListPreviewMatchStates(jobIds, { triggerMissing: true })
  jobMatchScores.value = result.scores
  jobMatchPending.value = result.pendingIds
  jobMatchFailed.value = result.failedIds
  if (result.pendingIds.size > 0) {
    matchPoll.start()
  } else {
    matchPoll.stop()
  }
}

function matchScoreText(jobId: number): string {
  if (jobMatchScores.value.has(jobId)) {
    return formatMatchScore(jobMatchScores.value.get(jobId))
  }
  if (jobMatchPending.value.has(jobId)) return '计算中…'
  if (jobMatchFailed.value.has(jobId)) return '暂不可用'
  return '—'
}

function matchScoreHasPoints(jobId: number): boolean {
  return jobMatchScores.value.has(jobId)
}

function matchScoreHint(_jobId: number): string {
  return '预估匹配度'
}

function goEditResume() {
  router.push('/candidate/resume/edit')
}

async function loadJobs() {
  matchPoll.stop()
  loading.value = true
  errorMsg.value = ''
  try {
    resumeReady.value = await checkResumeReady()
    const keyword = searchText.value || undefined
    const deptNames = !keyword ? resolveCategoryDeptNames(activeCategoryLabel.value) : undefined
    const [data] = await Promise.all([
      fetchJobList({ current: 1, size: 50, status: 1, keyword, deptNames }),
      loadAppliedJobs(),
    ])
    jobs.value = data.records ?? []
    total.value = data.total ?? jobs.value.length
    await refreshMatchScores()
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '岗位列表加载失败')
    jobs.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function scheduleSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    void loadJobs()
  }, 300)
}

function onSearchInput() {
  activeCategory.value = 0
  scheduleSearch()
}

function selectCategory(index: number) {
  activeCategory.value = index
  if (index !== 0) {
    searchKeyword.value = ''
  }
  void loadJobs()
}

function goDetail(job: JobPost) {
  router.push({ path: '/candidate/job', query: { id: String(job.id) } })
}

function isApplied(jobId: number) {
  return appliedJobIds.value.has(jobId)
}

function isFavorited(jobId: number) {
  return favoriteJobIds.value.has(jobId)
}

async function handleToggleFavorite(jobId: number) {
  if (togglingFavoriteId.value != null) return
  togglingFavoriteId.value = jobId
  try {
    const result = await toggleJobFavorite(jobId)
    const next = new Set(favoriteJobIds.value)
    if (result.favorited) {
      next.add(jobId)
    } else {
      next.delete(jobId)
    }
    favoriteJobIds.value = next
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '收藏操作失败')
  } finally {
    togglingFavoriteId.value = null
  }
}

function goApplications() {
  router.push('/candidate/applications')
}

function onJobAction(job: JobPost) {
  if (isApplied(job.id)) {
    goApplications()
  } else {
    goDetail(job)
  }
}

function jobTags(job: JobPost): string[] {
  const tags = parseSkillTags(job.skillTags)
  if (job.isRemote && !tags.includes('远程可')) tags.push('远程可')
  return tags.slice(0, 4)
}

const displayJobs = computed(() => {
  if (!resumeReady.value) return jobs.value
  return sortJobsByMatchScore(jobs.value, jobMatchScores.value, jobMatchPending.value)
})

const showResumeTip = computed(() => !checkingResume.value && !loading.value && !resumeReady.value && jobs.value.length > 0)

watch(searchKeyword, () => {
  if (searchKeyword.value.trim()) {
    activeCategory.value = 0
  }
})

onMounted(() => {
  loadJobs()
})
</script>

<template>
  <div data-cmp="JobList" class="flex h-full flex-col overflow-hidden bg-[#EBF4F0]">
    <div class="p-4 bg-card border-b border-border">
      <div class="flex items-center gap-2 bg-muted rounded-2xl px-3 py-2.5">
        <Search :size="15" class="text-muted-foreground flex-shrink-0" />
        <input
          v-model="searchKeyword"
          class="bg-transparent text-sm outline-none flex-1 placeholder:text-muted-foreground"
          placeholder="搜索岗位、部门、城市、技能..."
          @input="onSearchInput"
        />
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
            i === activeCategory ? 'gradient-blue text-white' : 'bg-muted text-muted-foreground',
          ]"
          @click="selectCategory(i)"
        >
          {{ c }}
        </button>
      </div>
    </div>

    <div class="mx-4 mt-4 gradient-blue-purple rounded-2xl p-4 text-white">
      <div class="flex items-center gap-2 mb-1">
        <Sparkles :size="14" />
        <span class="text-sm font-semibold">{{ listBannerText }}</span>
      </div>
      <p class="text-xs text-white/90">
        {{ listBannerHint }}
      </p>
    </div>

    <div v-if="showResumeTip" class="mx-4 mt-3 rounded-xl border border-amber-200 bg-amber-50 px-3 py-2.5">
      <p class="text-xs text-amber-800 leading-relaxed">
        完善在线简历（完整度≥80%）后，系统将基于您的简历自动估算各岗位匹配度，并按匹配度从高到低排序推荐。
      </p>
      <button
        type="button"
        class="mt-2 text-xs font-medium text-brand-blue"
        @click="goEditResume"
      >
        去完善简历 →
      </button>
    </div>

    <div v-if="errorMsg" class="mx-4 mt-3 text-xs text-red-600 bg-red-50 border border-red-100 rounded-xl px-3 py-2">
      {{ errorMsg }}
    </div>

    <div class="flex-1 overflow-y-auto scrollbar-thin px-4 pt-4 pb-2 space-y-3">
      <div v-if="loading" class="text-center text-xs text-muted-foreground py-8">加载中...</div>
      <div v-else-if="!displayJobs.length" class="text-center text-xs text-muted-foreground py-8">
        {{
          searchText
            ? '没有匹配的岗位，试试其他关键词'
            : activeCategoryLabel !== '推荐'
              ? `${activeCategoryLabel}部门暂无在招岗位`
              : '暂无招聘中的岗位'
        }}
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
                v-if="isApplied(job.id)"
                class="text-xs px-1.5 py-0.5 rounded-full bg-brand-tint text-brand-blue border border-brand-border flex-shrink-0"
              >已投递</span>
              <span
                v-else-if="(job.appliedCount ?? 0) > 10"
                class="text-xs px-1.5 py-0.5 rounded-full bg-red-50 text-brand-red border border-red-200 flex-shrink-0"
              >热招</span>
              <span class="text-sm font-bold text-foreground truncate">{{ job.title }}</span>
            </div>
            <div class="text-xs text-muted-foreground">{{ job.deptName || '—' }}</div>
          </div>
          <button
            type="button"
            class="p-1.5 ml-2 flex-shrink-0 rounded-lg hover:bg-muted disabled:opacity-50"
            :title="isFavorited(job.id) ? '取消收藏' : '收藏岗位'"
            :disabled="togglingFavoriteId === job.id"
            @click.stop="handleToggleFavorite(job.id)"
          >
            <Bookmark
              :size="16"
              :class="isFavorited(job.id) ? 'text-brand-orange fill-brand-orange' : 'text-muted-foreground'"
            />
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
            <span
              class="text-xs font-semibold"
              :class="jobMatchPending.has(job.id) ? 'text-muted-foreground' : 'text-brand-purple'"
            >
              {{ matchScoreHint(job.id) }} {{ matchScoreText(job.id) }}<span v-if="matchScoreHasPoints(job.id)"> 分</span>
            </span>
          </div>
          <button
            type="button"
            class="px-4 py-1.5 rounded-full text-xs font-medium"
            :class="isApplied(job.id) ? 'border border-brand-blue text-brand-blue bg-brand-tint' : 'gradient-blue text-white'"
            @click.stop="onJobAction(job)"
          >
            {{ isApplied(job.id) ? '查看进度' : '立即投递' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
