<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bookmark, Briefcase, ChevronLeft, MapPin, Star } from 'lucide-vue-next'
import { fetchActiveAppliedJobIds } from '@/api/delivery'
import { fetchMyFavorites, toggleJobFavorite, type JobFavoriteItem } from '@/api/favorite'
import type { JobPost } from '@/api/job'
import { formatEmploymentType, formatJobSalary, parseSkillTags } from '@/utils/jobFormat'
import { getErrorMessage } from '@/utils/validators'

const router = useRouter()
const loading = ref(false)
const togglingId = ref<number | null>(null)
const errorMsg = ref('')
const items = ref<JobFavoriteItem[]>([])
const total = ref(0)
const appliedJobIds = ref<Set<number>>(new Set())

async function loadAppliedJobs() {
  try {
    const data = await fetchActiveAppliedJobIds()
    appliedJobIds.value = new Set((data.jobIds ?? []).filter((id) => id > 0))
  } catch {
    appliedJobIds.value = new Set()
  }
}

async function loadFavorites() {
  loading.value = true
  errorMsg.value = ''
  try {
    const [data] = await Promise.all([
      fetchMyFavorites({ current: 1, size: 50 }),
      loadAppliedJobs(),
    ])
    items.value = data.records ?? []
    total.value = data.total ?? items.value.length
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '收藏列表加载失败')
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function jobOf(item: JobFavoriteItem): JobPost | null {
  return item.job ?? null
}

function isApplied(jobId: number) {
  return appliedJobIds.value.has(jobId)
}

function jobTags(job: JobPost): string[] {
  const tags = parseSkillTags(job.skillTags)
  if (job.isRemote && !tags.includes('远程可')) tags.push('远程可')
  return tags.slice(0, 4)
}

function goDetail(jobId: number) {
  router.push({ path: '/candidate/job', query: { id: String(jobId) } })
}

function goBack() {
  router.push('/candidate/profile')
}

async function handleUnfavorite(item: JobFavoriteItem, event: Event) {
  event.stopPropagation()
  if (togglingId.value != null) return
  togglingId.value = item.jobId
  errorMsg.value = ''
  try {
    const result = await toggleJobFavorite(item.jobId)
    if (!result.favorited) {
      items.value = items.value.filter((row) => row.jobId !== item.jobId)
      total.value = Math.max(0, total.value - 1)
    }
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '取消收藏失败')
  } finally {
    togglingId.value = null
  }
}

onMounted(loadFavorites)
</script>

<template>
  <div data-cmp="Favorites" class="flex h-full flex-col bg-[#EBF4F0]">
    <div class="px-4 py-4 bg-card border-b border-border flex-shrink-0 flex items-center gap-2">
      <button type="button" class="p-1 -ml-1 rounded-lg hover:bg-muted" @click="goBack">
        <ChevronLeft :size="20" class="text-foreground" />
      </button>
      <div class="flex-1 min-w-0">
        <h1 class="text-base font-bold text-foreground">我的收藏</h1>
        <p class="text-xs text-muted-foreground mt-0.5">
          {{ loading ? '加载中...' : `共 ${total} 个收藏岗位` }}
        </p>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto px-4 py-4 scrollbar-thin space-y-3">
      <p v-if="errorMsg" class="text-xs text-red-600 bg-red-50 border border-red-100 rounded-xl px-3 py-2">
        {{ errorMsg }}
      </p>

      <div
        v-if="!loading && items.length === 0"
        class="rounded-2xl border border-border bg-card p-8 text-center shadow-card"
      >
        <Star :size="32" class="mx-auto text-muted-foreground/50 mb-2" />
        <p class="text-sm text-muted-foreground">暂无收藏岗位</p>
        <p class="text-xs text-muted-foreground mt-1">在岗位列表或详情页点击收藏图标即可保存心仪岗位</p>
        <button
          type="button"
          class="mt-4 text-xs text-brand-blue"
          @click="router.push('/candidate')"
        >
          去浏览岗位
        </button>
      </div>

      <button
        v-for="item in items"
        :key="item.favoriteId"
        type="button"
        class="w-full text-left bg-card shadow-card p-4 border border-border rounded-2xl hover:border-brand-blue/30 transition-colors"
        @click="goDetail(item.jobId)"
      >
        <template v-if="jobOf(item)">
          <div class="flex items-start justify-between mb-3">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <span
                  v-if="isApplied(item.jobId)"
                  class="text-xs px-1.5 py-0.5 rounded-full bg-brand-tint text-brand-blue border border-brand-border flex-shrink-0"
                >已投递</span>
                <span
                  v-else-if="jobOf(item)!.status !== 1"
                  class="text-xs px-1.5 py-0.5 rounded-full bg-muted text-muted-foreground flex-shrink-0"
                >已关闭</span>
                <span class="text-sm font-bold text-foreground truncate">{{ jobOf(item)!.title }}</span>
              </div>
              <div class="text-xs text-muted-foreground">{{ jobOf(item)!.deptName || '—' }}</div>
            </div>
            <button
              type="button"
              class="p-1.5 ml-2 flex-shrink-0 rounded-lg hover:bg-muted disabled:opacity-50"
              title="取消收藏"
              :disabled="togglingId === item.jobId"
              @click="handleUnfavorite(item, $event)"
            >
              <Bookmark :size="16" class="text-brand-orange fill-brand-orange" />
            </button>
          </div>
          <div class="flex items-center gap-4 mb-3 flex-wrap">
            <span class="text-sm font-bold text-brand-blue">{{ formatJobSalary(jobOf(item)!) }}</span>
            <span v-if="jobOf(item)!.workCity" class="flex items-center gap-1 text-xs text-muted-foreground">
              <MapPin :size="10" />{{ jobOf(item)!.workCity }}
            </span>
            <span class="flex items-center gap-1 text-xs text-muted-foreground">
              <Briefcase :size="10" />{{ formatEmploymentType(jobOf(item)!.employmentType) }}
            </span>
          </div>
          <div v-if="jobTags(jobOf(item)!).length" class="flex flex-wrap gap-1">
            <span
              v-for="tag in jobTags(jobOf(item)!)"
              :key="tag"
              class="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground"
            >{{ tag }}</span>
          </div>
        </template>
        <template v-else>
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-semibold text-foreground">岗位已下架</p>
              <p class="text-xs text-muted-foreground mt-1">岗位 ID：{{ item.jobId }}</p>
            </div>
            <button
              type="button"
              class="p-1.5 rounded-lg hover:bg-muted disabled:opacity-50"
              title="取消收藏"
              :disabled="togglingId === item.jobId"
              @click="handleUnfavorite(item, $event)"
            >
              <Bookmark :size="16" class="text-brand-orange fill-brand-orange" />
            </button>
          </div>
        </template>
      </button>
    </div>
  </div>
</template>
