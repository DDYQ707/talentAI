<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  Search, User, Briefcase, Tag,
  ChevronLeft, ChevronRight, Loader2, AlertCircle, Users,
} from 'lucide-vue-next'
import {
  fetchTalentPoolPage, fetchAllTags,
  JOB_SEEKING_STATUS, JOB_SEEKING_STATUS_LABELS,
  type TalentPoolRecordVO, type TalentTagVO,
} from '@/api/talentPool'

const router = useRouter()
const talents = ref<TalentPoolRecordVO[]>([])
const allTags = ref<TalentTagVO[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const keyword = ref('')

const jobSeekingStatusFilter = ref<number | undefined>(undefined)
const selectedTagId = ref<number | undefined>(undefined)

const statusColors: Record<number, string> = {
  [JOB_SEEKING_STATUS.ACTIVE]: 'bg-green-50 text-brand-green border-green-200',
  [JOB_SEEKING_STATUS.PASSIVE]: 'bg-brand-tint text-brand-blue border-brand-border',
  [JOB_SEEKING_STATUS.WATCHING]: 'bg-orange-50 text-brand-orange border-orange-200',
}

function getJobStatusLabel(status?: number | null): string {
  if (status == null) return '在职-观望'
  return JOB_SEEKING_STATUS_LABELS[status] ?? '未知'
}

function getJobStatusCls(status?: number | null): string {
  const key = status ?? JOB_SEEKING_STATUS.WATCHING
  return statusColors[key] ?? 'bg-muted text-muted-foreground border-border'
}

function openTalentDetail(t: TalentPoolRecordVO) {
  if (t.resumeId) {
    router.push({ path: '/hr/resumes/detail', query: { id: String(t.resumeId) } })
    return
  }
  router.push({ path: '/hr/resumes', query: { keyword: t.candidateName } })
}

function formatArchivedAt(iso?: string | null) {
  if (!iso) return '—'
  return iso.replace('T', ' ').slice(0, 16)
}

const statusCategories = [
  { label: '全部人才', value: undefined },
  { label: '主动求职', value: JOB_SEEKING_STATUS.ACTIVE },
  { label: '被动求职', value: JOB_SEEKING_STATUS.PASSIVE },
  { label: '在职-观望', value: JOB_SEEKING_STATUS.WATCHING },
]
const activeCategory = ref<number | undefined>(undefined)

const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

const headerSubtitle = computed(() => {
  if (total.value === 0) return '暂无归档人才，可在面试/Offer/简历详情中存入人才库'
  return `共 ${total.value} 位归档人才 · 按入库时间倒序`
})

async function loadTalents() {
  loading.value = true
  try {
    const data = await fetchTalentPoolPage({
      current: currentPage.value,
      size: pageSize.value,
      jobSeekingStatus: jobSeekingStatusFilter.value,
      tagId: selectedTagId.value,
      keyword: keyword.value.trim() || undefined,
    })
    talents.value = data.records ?? []
    total.value = data.total ?? 0
  } catch (err) {
    console.error('加载人才库失败:', err)
    talents.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadTags() {
  try {
    allTags.value = await fetchAllTags()
  } catch (err) {
    console.error('加载标签列表失败:', err)
    allTags.value = []
  }
}

function onCategorySelect(value: number | undefined) {
  activeCategory.value = value
  jobSeekingStatusFilter.value = value
  currentPage.value = 1
  loadTalents()
}

function onTagSelect(tagId: number) {
  selectedTagId.value = selectedTagId.value === tagId ? undefined : tagId
  currentPage.value = 1
  loadTalents()
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(keyword, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    loadTalents()
  }, 300)
})

function prevPage() {
  if (currentPage.value > 1) {
    currentPage.value--
    loadTalents()
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    loadTalents()
  }
}

onMounted(() => {
  loadTalents()
  loadTags()
})
</script>

<template>
  <div data-cmp="TalentPool" class="flex h-full" style="height: calc(100vh - 64px)">
    <!-- 左侧筛选 -->
    <div class="w-56 flex-shrink-0 border-r border-border bg-card flex flex-col p-4 overflow-y-auto scrollbar-thin">
      <div class="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">求职状态</div>
      <button
        v-for="c in statusCategories"
        :key="String(c.value)"
        type="button"
        class="w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-xs mb-1 transition-colors"
        :class="activeCategory === c.value ? 'bg-brand-blue text-white' : 'hover:bg-muted text-muted-foreground'"
        @click="onCategorySelect(c.value)"
      >
        <span>{{ c.label }}</span>
      </button>

      <div class="mt-4 pt-4 border-t border-border">
        <div class="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">标签筛选</div>
        <div v-if="allTags.length === 0" class="text-xs text-muted-foreground px-3 py-1.5">暂无标签</div>
        <div
          v-for="tag in allTags"
          :key="tag.id"
          class="flex items-center gap-2 px-3 py-1.5 rounded-lg cursor-pointer transition-colors"
          :class="selectedTagId === tag.id ? 'bg-brand-tint text-brand-purple' : 'hover:bg-muted'"
          @click="onTagSelect(tag.id)"
        >
          <Tag :size="11" class="text-brand-purple" />
          <span
            class="text-xs"
            :class="selectedTagId === tag.id ? 'text-brand-purple font-medium' : 'text-muted-foreground'"
          >
            {{ tag.tagName }}
          </span>
        </div>
      </div>
    </div>

    <!-- 主内容 -->
    <div class="flex-1 flex flex-col min-w-0">
      <div class="px-6 py-4 border-b border-border">
        <div class="flex items-start justify-between gap-4 mb-4">
          <div>
            <h1 class="text-xl font-bold text-foreground flex items-center gap-2">
              <Users :size="20" class="text-brand-purple" />
              人才库
            </h1>
            <p class="text-sm text-muted-foreground mt-1">{{ headerSubtitle }}</p>
          </div>
        </div>
        <div class="flex items-center gap-3 flex-wrap">
          <div class="flex items-center gap-2 bg-card rounded-lg px-3 py-2 border border-border flex-1 max-w-md min-w-[220px]">
            <Search :size="14" class="text-muted-foreground flex-shrink-0" />
            <input
              v-model="keyword"
              class="bg-transparent text-sm outline-none flex-1 placeholder:text-muted-foreground"
              placeholder="搜索姓名、公司、岗位或归档原因"
            />
          </div>
          <p class="text-xs text-muted-foreground">
            人才由 HR 在淘汰、Offer 结束或面试后手动归档入库
          </p>
        </div>
      </div>

      <div class="flex-1 overflow-y-auto scrollbar-thin p-6">
        <div v-if="loading" class="flex items-center justify-center py-16">
          <Loader2 :size="24" class="text-brand-blue animate-spin" />
          <span class="ml-2 text-sm text-muted-foreground">加载中...</span>
        </div>

        <div
          v-else-if="talents.length === 0"
          class="flex flex-col items-center justify-center py-16 text-muted-foreground"
        >
          <AlertCircle :size="32" class="mb-2 opacity-40" />
          <span class="text-sm">{{ keyword.trim() ? '未找到匹配人才' : '暂无人才数据' }}</span>
          <span v-if="!keyword.trim()" class="text-xs mt-1">在简历详情、面试管理或 Offer 管理中可「存入人才库」</span>
        </div>

        <div v-else class="flex flex-col gap-3">
          <div
            v-for="t in talents"
            :key="t.id"
            class="bg-card shadow-card hover:shadow-panel transition-shadow border border-border hover:border-brand-blue/20 rounded-xl cursor-pointer"
            @click="openTalentDetail(t)"
          >
            <div class="flex items-center gap-4 p-4">
              <div class="w-12 h-12 rounded-full gradient-blue-purple flex items-center justify-center flex-shrink-0">
                <User :size="20" class="text-white" />
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-3 mb-1 flex-wrap">
                  <span class="text-sm font-bold text-foreground">{{ t.candidateName }}</span>
                  <span class="text-xs text-muted-foreground">{{ t.currentTitle || '职位未填' }}</span>
                  <span
                    :class="['text-xs px-2 py-0.5 rounded-full border', getJobStatusCls(t.jobSeekingStatus)]"
                  >
                    {{ getJobStatusLabel(t.jobSeekingStatus) }}
                  </span>
                </div>
                <div class="flex items-center gap-4 text-xs text-muted-foreground mb-2 flex-wrap">
                  <span v-if="t.currentCompany" class="flex items-center gap-1">
                    <Briefcase :size="10" />
                    {{ t.currentCompany }}
                  </span>
                  <span v-if="t.sourceJobTitle" class="truncate max-w-[240px]">
                    来源岗位：{{ t.sourceJobTitle }}
                  </span>
                  <span>入库：{{ formatArchivedAt(t.archivedAt) }}</span>
                </div>
                <p v-if="t.archiveReason" class="text-[11px] text-muted-foreground mb-1 line-clamp-2">
                  归档原因：{{ t.archiveReason }}
                </p>
                <p v-if="t.interviewSummary" class="text-[11px] text-muted-foreground mb-2 line-clamp-2">
                  面试摘要：{{ t.interviewSummary }}
                </p>
                <div v-if="t.tags?.length" class="flex flex-wrap gap-1">
                  <span
                    v-for="tag in t.tags"
                    :key="tag.id"
                    class="text-xs px-2 py-0.5 rounded-full bg-accent text-accent-foreground"
                  >
                    {{ tag.tagName }}
                  </span>
                </div>
              </div>
              <div v-if="t.matchScore != null && t.matchScore > 0" class="text-center flex-shrink-0 px-2">
                <div class="text-xl font-black text-brand-purple">{{ t.matchScore }}%</div>
                <div class="text-xs text-muted-foreground">匹配分</div>
              </div>
            </div>
          </div>
        </div>

        <div
          v-if="!loading && talents.length > 0"
          class="flex items-center justify-between mt-4 pt-4 border-t border-border"
        >
          <span class="text-xs text-muted-foreground">共 {{ total }} 条记录</span>
          <div class="flex items-center gap-2">
            <button
              type="button"
              class="p-1.5 rounded-lg border border-border text-muted-foreground hover:text-foreground disabled:opacity-30 disabled:cursor-not-allowed"
              :disabled="currentPage <= 1"
              @click="prevPage"
            >
              <ChevronLeft :size="14" />
            </button>
            <span class="text-xs text-muted-foreground">{{ currentPage }} / {{ totalPages }}</span>
            <button
              type="button"
              class="p-1.5 rounded-lg border border-border text-muted-foreground hover:text-foreground disabled:opacity-30 disabled:cursor-not-allowed"
              :disabled="currentPage >= totalPages"
              @click="nextPage"
            >
              <ChevronRight :size="14" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
