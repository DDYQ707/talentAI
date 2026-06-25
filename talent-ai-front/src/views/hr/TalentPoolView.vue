<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  Search, Filter, User, Bookmark, Sparkles, Brain,
  MapPin, Briefcase, Tag, Plus, ChevronDown, Zap,
  ChevronLeft, ChevronRight, Loader2, AlertCircle,
} from 'lucide-vue-next'
import {
  fetchTalentPoolPage, fetchAllTags,
  JOB_SEEKING_STATUS, JOB_SEEKING_STATUS_LABELS,
  type TalentPoolRecordVO, type TalentTagVO,
} from '@/api/talentPool'

/* ---- 响应式状态 ---- */
const talents = ref<TalentPoolRecordVO[]>([])
const allTags = ref<TalentTagVO[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const searchVal = ref('')

/** 求职状态筛选（undefined 代表全部） */
const jobSeekingStatusFilter = ref<number | undefined>(undefined)

/** 标签筛选（后端 tagId 精确匹配） */
const selectedTagId = ref<number | undefined>(undefined)

/* ---- 求职状态 → 样式 ---- */
const statusColors: Record<number, string> = {
  [JOB_SEEKING_STATUS.ACTIVE]:   'bg-green-50 text-brand-green border-green-200',
  [JOB_SEEKING_STATUS.PASSIVE]:  'bg-brand-tint text-brand-blue border-brand-border',
  [JOB_SEEKING_STATUS.WATCHING]: 'bg-orange-50 text-brand-orange border-orange-200',
}

function getJobStatusLabel(status: number): string {
  return JOB_SEEKING_STATUS_LABELS[status] ?? '未知'
}

function getJobStatusCls(status: number): string {
  return statusColors[status] ?? 'bg-muted text-muted-foreground border-border'
}

/* ---- 侧边栏：求职状态分类 ---- */
const statusCategories = [
  { label: '全部人才', value: undefined },
  { label: '主动求职', value: JOB_SEEKING_STATUS.ACTIVE },
  { label: '被动求职', value: JOB_SEEKING_STATUS.PASSIVE },
  { label: '在职-观望', value: JOB_SEEKING_STATUS.WATCHING },
]
const activeCategory = ref<number | undefined>(undefined)

/* ---- 分页 ---- */
const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

/* ---- 数据加载 ---- */
async function loadTalents() {
  loading.value = true
  try {
    const data = await fetchTalentPoolPage({
      current: currentPage.value,
      size: pageSize.value,
      jobSeekingStatus: jobSeekingStatusFilter.value,
      tagId: selectedTagId.value,
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

/* ---- 筛选操作 ---- */
function onCategorySelect(value: number | undefined) {
  activeCategory.value = value
  jobSeekingStatusFilter.value = value
  currentPage.value = 1
  loadTalents()
}

function onTagSelect(tagId: number) {
  if (selectedTagId.value === tagId) {
    selectedTagId.value = undefined
  } else {
    selectedTagId.value = tagId
  }
  currentPage.value = 1
  loadTalents()
}

/** 搜索防抖（预留，当前后端暂无 keyword 参数，仅做 UI 交互） */
let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    loadTalents()
  }, 400)
}

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

/* ---- 初始化 ---- */
onMounted(() => {
  loadTalents()
  loadTags()
})
</script>

<template>
  <div data-cmp="TalentPool" class="flex h-full" style="height: calc(100vh - 64px)">
    <!-- 左侧边栏 -->
    <div class="w-56 flex-shrink-0 border-r border-border bg-card flex flex-col p-4">
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

      <!-- 标签筛选 -->
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
          <span class="text-xs" :class="selectedTagId === tag.id ? 'text-brand-purple font-medium' : 'text-muted-foreground'">{{ tag.tagName }}</span>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="flex-1 flex flex-col min-w-0">
      <!-- 搜索栏 -->
      <div class="px-6 py-4 border-b border-border flex items-center gap-3">
        <div class="flex items-center gap-2 bg-muted rounded-lg px-3 py-2 flex-1 max-w-lg">
          <Search :size="14" class="text-muted-foreground" />
          <input
            v-model="searchVal"
            class="bg-transparent text-sm outline-none flex-1 placeholder:text-muted-foreground"
            placeholder="AI语义搜索：会Python且了解金融行业的5年以上工程师..."
            @input="onSearchInput"
          />
        </div>
        <button type="button" class="flex items-center gap-1.5 px-4 py-2 rounded-lg gradient-purple text-white text-sm">
          <Sparkles :size="14" />
          <span>智能搜索</span>
        </button>
        <button type="button" class="flex items-center gap-1.5 px-3 py-2 rounded-lg border border-border text-xs text-muted-foreground">
          <Filter :size="14" />
          <span>高级筛选</span>
        </button>
        <div class="ml-auto text-xs text-muted-foreground">共 {{ total }} 位人才</div>
      </div>

      <!-- 工具栏 -->
      <div class="px-6 py-2 border-b border-border bg-muted/30 flex items-center gap-3">
        <span class="text-xs text-muted-foreground">排序：</span>
        <button v-for="s in ['匹配度', '最近添加', '活跃度']" :key="s" type="button" class="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground px-2 py-1 rounded">
          <span>{{ s }}</span>
          <ChevronDown :size="10" />
        </button>
        <div class="ml-auto flex items-center gap-2">
          <button type="button" class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-accent text-brand-purple text-xs border border-brand-border/50">
            <Zap :size="12" />
            <span>AI批量分析</span>
          </button>
          <button type="button" class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-border text-xs text-muted-foreground">
            <Plus :size="12" />
            <span>添加人才</span>
          </button>
        </div>
      </div>

      <!-- 内容列表 -->
      <div class="flex-1 overflow-y-auto scrollbar-thin p-6">
        <!-- Loading -->
        <div v-if="loading" class="flex items-center justify-center py-16">
          <Loader2 :size="24" class="text-brand-blue animate-spin" />
          <span class="ml-2 text-sm text-muted-foreground">加载中...</span>
        </div>

        <!-- Empty -->
        <div v-else-if="talents.length === 0" class="flex flex-col items-center justify-center py-16 text-muted-foreground">
          <AlertCircle :size="32" class="mb-2 opacity-40" />
          <span class="text-sm">暂无人才数据</span>
        </div>

        <!-- 人才卡片列表 -->
        <div v-else class="flex flex-col gap-3">
          <div
            v-for="t in talents"
            :key="t.id"
            class="bg-card shadow-card hover:shadow-panel transition-shadow border border-border hover:border-brand-blue/20 cursor-pointer"
          >
            <div class="flex items-center gap-4 p-4">
              <div class="w-12 h-12 rounded-full gradient-blue-purple flex items-center justify-center flex-shrink-0">
                <User :size="20" class="text-white" />
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-3 mb-1">
                  <span class="text-sm font-bold text-foreground">{{ t.candidateName }}</span>
                  <span class="text-xs text-muted-foreground">{{ t.currentTitle || '-' }}</span>
                  <span
                    :class="[
                      'text-xs px-2 py-0.5 rounded-full border',
                      getJobStatusCls(t.jobSeekingStatus),
                    ]"
                  >
                    {{ getJobStatusLabel(t.jobSeekingStatus) }}
                  </span>
                </div>
                <div class="flex items-center gap-4 text-xs text-muted-foreground mb-2">
                  <span class="flex items-center gap-1"><Briefcase :size="10" />{{ t.currentCompany || '-' }}</span>
                  <span v-if="t.talentCategory" class="flex items-center gap-1"><MapPin :size="10" />分类{{ t.talentCategory }}</span>
                </div>
                <div class="flex flex-wrap gap-1">
                  <span
                    v-for="tag in t.tags"
                    :key="tag.id"
                    class="text-xs px-2 py-0.5 rounded-full bg-accent text-accent-foreground"
                  >{{ tag.tagName }}</span>
                </div>
              </div>
              <div class="flex items-center gap-4 flex-shrink-0">
                <div class="text-center">
                  <div class="text-xl font-black text-brand-purple">{{ t.matchScore ?? '-' }}%</div>
                  <div class="text-xs text-muted-foreground">匹配度</div>
                </div>
                <div class="flex flex-col gap-1">
                  <button type="button" :class="['p-2 rounded-lg hover:bg-muted', t.isSaved ? 'text-brand-orange' : 'text-muted-foreground']">
                    <Bookmark :size="16" :class="t.isSaved ? 'fill-brand-orange' : ''" />
                  </button>
                  <button type="button" class="p-2 rounded-lg hover:bg-muted text-muted-foreground hover:text-brand-purple">
                    <Brain :size="16" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页控件 -->
        <div v-if="!loading && talents.length > 0" class="flex items-center justify-between mt-4 pt-4 border-t border-border">
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
