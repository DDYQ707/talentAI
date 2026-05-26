<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  Search,
  LayoutGrid,
  List,
  Sparkles,
  User,
  Star,
  Eye,
  FileText,
  Briefcase,
} from 'lucide-vue-next'
import { consolidateHrResumes, fetchHrResumePage, fetchHrResumePreview, type HrResumeListItem } from '@/api/hrResume'
import { fetchHrCandidateBrief } from '@/api/hrCandidate'
import { openResumePreview } from '@/api/resume'
import { RESUME_SCREEN_STATUS, screenStatusLabel } from '@/constants/resume'
import { formatDegree } from '@/utils/resumeFormat'
import { getErrorMessage } from '@/utils/validators'

const router = useRouter()

const filterTags = [
  { label: '全部', value: undefined as number | undefined },
  { label: '待初筛', value: RESUME_SCREEN_STATUS.PENDING },
  { label: '面试中', value: RESUME_SCREEN_STATUS.INTERVIEWING },
  { label: '已录用', value: RESUME_SCREEN_STATUS.HIRED },
  { label: '已淘汰', value: RESUME_SCREEN_STATUS.REJECTED },
]

const statusStyles: Record<string, string> = {
  待初筛: 'bg-muted text-muted-foreground border-border',
  面试中: 'bg-orange-50 text-brand-orange border-orange-200',
  已录用: 'bg-green-50 text-brand-green border-green-200',
  已淘汰: 'bg-red-50 text-brand-red border-red-200',
}

const candidates = ref<HrResumeListItem[]>([])
const loading = ref(false)
const errorMsg = ref('')
const keyword = ref('')
const viewMode = ref<'grid' | 'list'>('grid')
const activeFilter = ref('全部')
const previewingId = ref<number | null>(null)

const filtered = computed(() => candidates.value)

function screenStatusForItem(item: HrResumeListItem) {
  return screenStatusLabel(item.screenStatus)
}

function appliedJobLabel(item: HrResumeListItem) {
  return item.appliedJobTitle || '暂无投递记录'
}

async function enrichListProfile(items: HrResumeListItem[]) {
  const out = [...items]
  await Promise.all(
    out.map(async (item) => {
      if (item.phone && item.highestEdu != null) return
      try {
        const brief = await fetchHrCandidateBrief(item.candidateId)
        if (brief.realName) item.candidateName = brief.realName
        if (brief.phone) item.phone = brief.phone
        if (brief.city) item.city = brief.city
        if (brief.currentTitle) item.currentTitle = brief.currentTitle
        if (brief.highestEdu != null) item.highestEdu = brief.highestEdu
      } catch {
        /* 档案接口不可用时保留列表基础字段 */
      }
    }),
  )
  return out
}

async function loadList() {
  loading.value = true
  errorMsg.value = ''
  const tag = filterTags.find((t) => t.label === activeFilter.value)
  try {
    try {
      await consolidateHrResumes()
    } catch {
      /* 合并接口失败时仍尝试加载列表 */
    }
    const data = await fetchHrResumePage({
      current: 1,
      size: 50,
      keyword: keyword.value.trim() || undefined,
      screenStatus: tag?.value,
    })
    candidates.value = await enrichListProfile(data.records ?? [])
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '简历列表加载失败')
    candidates.value = []
  } finally {
    loading.value = false
  }
}

function goDetail(item: HrResumeListItem) {
  router.push({ path: '/hr/resumes/detail', query: { id: String(item.id) } })
}

async function quickPreview(item: HrResumeListItem, ev: Event) {
  ev.stopPropagation()
  if (!item.attachmentId) {
    errorMsg.value = '该简历暂无附件'
    return
  }
  previewingId.value = item.id
  try {
    const preview = await fetchHrResumePreview(item.attachmentId)
    openResumePreview(preview)
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '预览失败')
  } finally {
    previewingId.value = null
  }
}

watch(activeFilter, () => loadList())

onMounted(() => {
  loadList()
})
</script>

<template>
  <div data-cmp="ResumeManagement" class="flex h-full" style="height: calc(100vh - 64px)">
    <div class="w-56 flex-shrink-0 border-r border-border bg-card p-4 overflow-y-auto scrollbar-thin">
      <div class="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">筛选条件</div>
      <div class="mb-4">
        <div class="text-xs font-medium text-foreground mb-2">候选状态</div>
        <button
          v-for="tag in filterTags"
          :key="tag.label"
          type="button"
          class="w-full text-left text-xs px-3 py-2 rounded-lg mb-1 transition-colors"
          :class="activeFilter === tag.label ? 'bg-brand-blue text-white' : 'hover:bg-muted text-muted-foreground'"
          @click="activeFilter = tag.label"
        >
          {{ tag.label }}
        </button>
      </div>
    </div>

    <div class="flex-1 flex flex-col min-w-0 overflow-hidden">
      <div class="px-6 py-4 border-b border-border flex items-center gap-3">
        <div class="flex items-center gap-2 bg-muted rounded-lg px-3 py-2 flex-1 max-w-md">
          <Search :size="14" class="text-muted-foreground" />
          <input
            v-model="keyword"
            class="bg-transparent text-sm outline-none flex-1 placeholder:text-muted-foreground"
            placeholder="搜索简历名称..."
            @keyup.enter="loadList"
          />
        </div>
        <button type="button" class="px-3 py-2 rounded-lg border border-border text-sm" @click="loadList">
          搜索
        </button>
        <div class="ml-auto flex items-center gap-2">
          <div class="text-xs text-muted-foreground">共 {{ filtered.length }} 份</div>
          <button
            type="button"
            class="p-2 rounded-lg"
            :class="viewMode === 'grid' ? 'bg-brand-blue text-white' : 'hover:bg-muted text-muted-foreground'"
            @click="viewMode = 'grid'"
          >
            <LayoutGrid :size="16" />
          </button>
          <button
            type="button"
            class="p-2 rounded-lg"
            :class="viewMode === 'list' ? 'bg-brand-blue text-white' : 'hover:bg-muted text-muted-foreground'"
            @click="viewMode = 'list'"
          >
            <List :size="16" />
          </button>
        </div>
      </div>

      <p v-if="errorMsg" class="mx-6 mt-2 text-xs text-red-600">{{ errorMsg }}</p>

      <div class="flex-1 overflow-auto scrollbar-thin p-6">
        <div v-if="loading" class="text-sm text-muted-foreground text-center py-12">加载中...</div>
        <div v-else-if="!filtered.length" class="text-sm text-muted-foreground text-center py-12">暂无简历数据</div>
        <div v-else :class="viewMode === 'grid' ? 'flex flex-wrap gap-4' : 'flex flex-col gap-3'">
          <div
            v-for="c in filtered"
            :key="c.id"
            class="bg-card shadow-card hover:shadow-panel transition-shadow cursor-pointer border border-border hover:border-brand-blue/30"
            :class="viewMode === 'grid' ? 'w-64 flex-shrink-0' : 'w-full'"
            @click="goDetail(c)"
          >
            <div class="p-4">
              <div class="flex items-start justify-between mb-3">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-full gradient-blue-purple flex items-center justify-center flex-shrink-0">
                    <User :size="16" class="text-white" />
                  </div>
                  <div>
                    <div class="text-sm font-semibold text-foreground">{{ c.candidateName }}</div>
                    <div class="text-xs text-muted-foreground truncate max-w-[140px]">
                      {{ c.currentTitle || c.resumeName }}
                    </div>
                    <div v-if="c.phone || c.highestEdu" class="text-[11px] text-muted-foreground mt-0.5 flex gap-2">
                      <span v-if="c.phone">{{ c.phone }}</span>
                      <span v-if="c.highestEdu">{{ formatDegree(c.highestEdu) }}</span>
                    </div>
                  </div>
                </div>
                <Star :size="16" class="text-muted-foreground/40" />
              </div>
              <div v-if="c.fileName" class="flex items-center gap-2 text-xs text-muted-foreground mb-2">
                <FileText :size="12" class="text-brand-blue" />
                <span class="truncate flex-1">{{ c.fileName }}</span>
                <span v-if="c.fileType" class="uppercase">{{ c.fileType }}</span>
              </div>
              <div class="flex items-center gap-2 text-xs text-muted-foreground mb-3">
                <Briefcase :size="12" class="text-brand-purple flex-shrink-0" />
                <span class="truncate" :class="c.appliedJobTitle ? 'text-foreground' : 'text-muted-foreground'">
                  应聘：{{ appliedJobLabel(c) }}
                </span>
              </div>
              <div class="flex items-center justify-between">
                <span :class="['text-xs px-2 py-0.5 rounded-full border', statusStyles[screenStatusForItem(c)] || statusStyles['待初筛']]">
                  {{ screenStatusForItem(c) }}
                </span>
                <div class="flex items-center gap-1">
                  <button
                    type="button"
                    class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground disabled:opacity-50"
                    title="预览附件"
                    :disabled="!c.attachmentId || previewingId === c.id"
                    @click="quickPreview(c, $event)"
                  >
                    <Eye :size="12" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
