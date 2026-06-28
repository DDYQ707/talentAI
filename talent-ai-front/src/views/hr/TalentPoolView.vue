<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  Search, User, Briefcase, Tag, Plus, X, Pencil, Trash2,
  ChevronLeft, ChevronRight, Loader2, AlertCircle, Users,
} from 'lucide-vue-next'
import {
  bindTags,
  createTag,
  deleteTalentRecord,
  fetchTalentPoolPage,
  fetchAllTags,
  unbindTag,
  updateTalentRecord,
  JOB_SEEKING_STATUS,
  JOB_SEEKING_STATUS_LABELS,
  TAG_TYPE,
  TAG_TYPE_LABELS,
  type TalentPoolRecordVO,
  type TalentTagVO,
} from '@/api/talentPool'
import { getErrorMessage } from '@/utils/validators'

const router = useRouter()
const talents = ref<TalentPoolRecordVO[]>([])
const allTags = ref<TalentTagVO[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const errorMsg = ref('')
const successMsg = ref('')

const jobSeekingStatusFilter = ref<number | undefined>(undefined)
const selectedTagId = ref<number | undefined>(undefined)

const createTagOpen = ref(false)
const createTagSaving = ref(false)
const newTagName = ref('')
const newTagType = ref<number>(TAG_TYPE.CUSTOM)

const tagManageOpen = ref(false)
const managingTalent = ref<TalentPoolRecordVO | null>(null)
const tagActionLoading = ref(false)

const editOpen = ref(false)
const editSaving = ref(false)
const editingTalent = ref<TalentPoolRecordVO | null>(null)
const editForm = ref({
  currentTitle: '',
  currentCompany: '',
  jobSeekingStatus: JOB_SEEKING_STATUS.WATCHING as number,
  matchScore: 0,
  archiveReason: '',
  isSaved: false,
})
const deleteLoadingId = ref<number | null>(null)

const jobSeekingOptions = [
  { value: JOB_SEEKING_STATUS.ACTIVE, label: JOB_SEEKING_STATUS_LABELS[JOB_SEEKING_STATUS.ACTIVE] },
  { value: JOB_SEEKING_STATUS.PASSIVE, label: JOB_SEEKING_STATUS_LABELS[JOB_SEEKING_STATUS.PASSIVE] },
  { value: JOB_SEEKING_STATUS.WATCHING, label: JOB_SEEKING_STATUS_LABELS[JOB_SEEKING_STATUS.WATCHING] },
]

const tagTypeOptions = [
  { value: TAG_TYPE.SKILL, label: TAG_TYPE_LABELS[TAG_TYPE.SKILL] },
  { value: TAG_TYPE.DOMAIN, label: TAG_TYPE_LABELS[TAG_TYPE.DOMAIN] },
  { value: TAG_TYPE.CUSTOM, label: TAG_TYPE_LABELS[TAG_TYPE.CUSTOM] },
]

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

function flashSuccess(message: string) {
  successMsg.value = message
  setTimeout(() => {
    successMsg.value = ''
  }, 2500)
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

const availableTagsForManage = computed(() => {
  if (!managingTalent.value) return allTags.value
  const bound = new Set((managingTalent.value.tags ?? []).map((t) => t.id))
  return allTags.value.filter((t) => !bound.has(t.id))
})

async function loadTalents() {
  loading.value = true
  errorMsg.value = ''
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
    errorMsg.value = getErrorMessage(err, '加载人才库失败')
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

function openCreateTagDialog() {
  newTagName.value = ''
  newTagType.value = TAG_TYPE.CUSTOM
  createTagOpen.value = true
}

async function handleCreateTag() {
  const name = newTagName.value.trim()
  if (!name) {
    errorMsg.value = '请输入标签名称'
    return
  }
  createTagSaving.value = true
  errorMsg.value = ''
  try {
    await createTag({ tagName: name, tagType: newTagType.value })
    createTagOpen.value = false
    await loadTags()
    flashSuccess('标签已创建')
  } catch (err) {
    errorMsg.value = getErrorMessage(err, '创建标签失败')
  } finally {
    createTagSaving.value = false
  }
}

function openTagManage(t: TalentPoolRecordVO, event?: Event) {
  event?.stopPropagation()
  managingTalent.value = t
  tagManageOpen.value = true
}

function closeTagManage() {
  tagManageOpen.value = false
  managingTalent.value = null
}

async function refreshManagingTalent() {
  if (!managingTalent.value) return
  const id = managingTalent.value.id
  await loadTalents()
  managingTalent.value = talents.value.find((t) => t.id === id) ?? managingTalent.value
}

async function handleBindTag(tag: TalentTagVO) {
  if (!managingTalent.value || tagActionLoading.value) return
  tagActionLoading.value = true
  errorMsg.value = ''
  try {
    await bindTags(managingTalent.value.id, [tag.id])
    await refreshManagingTalent()
    flashSuccess(`已添加标签「${tag.tagName}」`)
  } catch (err) {
    errorMsg.value = getErrorMessage(err, '绑定标签失败')
  } finally {
    tagActionLoading.value = false
  }
}

async function handleUnbindTag(
  t: TalentPoolRecordVO,
  tag: TalentTagVO,
  event?: Event,
) {
  event?.stopPropagation()
  if (tagActionLoading.value) return
  tagActionLoading.value = true
  errorMsg.value = ''
  try {
    await unbindTag(t.id, tag.id)
    if (managingTalent.value?.id === t.id) {
      await refreshManagingTalent()
    } else {
      await loadTalents()
    }
    flashSuccess(`已移除标签「${tag.tagName}」`)
  } catch (err) {
    errorMsg.value = getErrorMessage(err, '移除标签失败')
  } finally {
    tagActionLoading.value = false
  }
}

function openEditDialog(t: TalentPoolRecordVO, event?: Event) {
  event?.stopPropagation()
  editingTalent.value = t
  editForm.value = {
    currentTitle: t.currentTitle ?? '',
    currentCompany: t.currentCompany ?? '',
    jobSeekingStatus: t.jobSeekingStatus ?? JOB_SEEKING_STATUS.WATCHING,
    matchScore: t.matchScore ?? 0,
    archiveReason: t.archiveReason ?? '',
    isSaved: !!t.isSaved,
  }
  editOpen.value = true
}

function closeEditDialog() {
  editOpen.value = false
  editingTalent.value = null
}

async function handleSaveEdit() {
  if (!editingTalent.value || editSaving.value) return
  editSaving.value = true
  errorMsg.value = ''
  try {
    await updateTalentRecord(editingTalent.value.id, {
      currentTitle: editForm.value.currentTitle.trim() || undefined,
      currentCompany: editForm.value.currentCompany.trim() || undefined,
      jobSeekingStatus: editForm.value.jobSeekingStatus,
      matchScore: Math.min(100, Math.max(0, Math.round(editForm.value.matchScore))),
      archiveReason: editForm.value.archiveReason.trim() || undefined,
      isSaved: editForm.value.isSaved,
    })
    closeEditDialog()
    await loadTalents()
    flashSuccess('记录已更新')
  } catch (err) {
    errorMsg.value = getErrorMessage(err, '更新失败')
  } finally {
    editSaving.value = false
  }
}

async function handleDeleteRecord(t: TalentPoolRecordVO, event?: Event) {
  event?.stopPropagation()
  if (deleteLoadingId.value != null) return
  if (!window.confirm(`确认将「${t.candidateName}」移出人才库？此操作不可恢复展示。`)) return
  deleteLoadingId.value = t.id
  errorMsg.value = ''
  try {
    await deleteTalentRecord(t.id)
    if (managingTalent.value?.id === t.id) closeTagManage()
    if (editingTalent.value?.id === t.id) closeEditDialog()
    await loadTalents()
    flashSuccess('已从人才库移除')
  } catch (err) {
    errorMsg.value = getErrorMessage(err, '删除失败')
  } finally {
    deleteLoadingId.value = null
  }
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
        <div class="flex items-center justify-between mb-3">
          <div class="text-xs font-semibold text-muted-foreground uppercase tracking-wider">标签筛选</div>
          <button
            type="button"
            class="p-1 rounded-lg hover:bg-muted text-brand-purple"
            title="新建标签"
            @click="openCreateTagDialog"
          >
            <Plus :size="14" />
          </button>
        </div>
        <div v-if="allTags.length === 0" class="text-xs text-muted-foreground px-3 py-1.5">暂无标签，点击 + 创建</div>
        <div
          v-for="tag in allTags"
          :key="tag.id"
          class="flex items-center gap-2 px-3 py-1.5 rounded-lg cursor-pointer transition-colors"
          :class="selectedTagId === tag.id ? 'bg-brand-tint text-brand-purple' : 'hover:bg-muted'"
          @click="onTagSelect(tag.id)"
        >
          <Tag :size="11" class="text-brand-purple" />
          <span
            class="text-xs truncate"
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
        <p v-if="errorMsg" class="text-xs text-red-600 mb-2">{{ errorMsg }}</p>
        <p v-if="successMsg" class="text-xs text-brand-green mb-2">{{ successMsg }}</p>
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
                <div class="flex flex-wrap items-center gap-1.5">
                  <span
                    v-for="tag in t.tags ?? []"
                    :key="tag.id"
                    class="inline-flex items-center gap-0.5 text-xs pl-2 pr-1 py-0.5 rounded-full bg-accent text-accent-foreground"
                  >
                    {{ tag.tagName }}
                    <button
                      type="button"
                      class="p-0.5 rounded-full hover:bg-muted-foreground/20"
                      title="移除标签"
                      @click="handleUnbindTag(t, tag, $event)"
                    >
                      <X :size="10" />
                    </button>
                  </span>
                  <button
                    type="button"
                    class="inline-flex items-center gap-0.5 text-[10px] px-2 py-0.5 rounded-full border border-dashed border-brand-purple/40 text-brand-purple hover:bg-brand-tint-2"
                    @click="openTagManage(t, $event)"
                  >
                    <Plus :size="10" />
                    标签
                  </button>
                </div>
              </div>
              <div class="flex flex-col items-end gap-2 flex-shrink-0">
                <div class="flex items-center gap-1">
                  <button
                    type="button"
                    class="p-1.5 rounded-lg border border-border text-muted-foreground hover:text-brand-blue hover:bg-muted"
                    title="编辑记录"
                    @click="openEditDialog(t, $event)"
                  >
                    <Pencil :size="14" />
                  </button>
                  <button
                    type="button"
                    class="p-1.5 rounded-lg border border-border text-muted-foreground hover:text-brand-red hover:bg-red-50 disabled:opacity-50"
                    title="移出人才库"
                    :disabled="deleteLoadingId === t.id"
                    @click="handleDeleteRecord(t, $event)"
                  >
                    <Loader2 v-if="deleteLoadingId === t.id" :size="14" class="animate-spin" />
                    <Trash2 v-else :size="14" />
                  </button>
                </div>
                <div v-if="t.matchScore != null && t.matchScore > 0" class="text-center px-2">
                  <div class="text-xl font-black text-brand-purple">{{ t.matchScore }}%</div>
                  <div class="text-xs text-muted-foreground">匹配分</div>
                </div>
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

    <!-- 新建标签 -->
    <div
      v-if="createTagOpen"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      @click.self="createTagOpen = false"
    >
      <div class="w-full max-w-md bg-card rounded-2xl border border-border shadow-panel p-5">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-base font-bold text-foreground">新建标签</h2>
          <button type="button" class="p-1 rounded-lg hover:bg-muted" @click="createTagOpen = false">
            <X :size="16" />
          </button>
        </div>
        <label class="text-xs text-muted-foreground block mb-1.5">标签名称</label>
        <input
          v-model="newTagName"
          type="text"
          maxlength="32"
          class="w-full mb-3 px-3 py-2 rounded-xl bg-muted text-sm outline-none border border-transparent focus:border-brand-blue/40"
          placeholder="例如：Java 强、可二面、高潜"
        />
        <label class="text-xs text-muted-foreground block mb-1.5">标签类型</label>
        <select
          v-model.number="newTagType"
          class="w-full mb-4 px-3 py-2 rounded-xl bg-muted text-sm outline-none border border-transparent focus:border-brand-blue/40"
        >
          <option v-for="opt in tagTypeOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </option>
        </select>
        <div class="flex justify-end gap-2">
          <button
            type="button"
            class="px-4 py-2 rounded-xl border border-border text-sm"
            :disabled="createTagSaving"
            @click="createTagOpen = false"
          >
            取消
          </button>
          <button
            type="button"
            class="px-4 py-2 rounded-xl gradient-purple text-white text-sm font-medium disabled:opacity-50"
            :disabled="createTagSaving"
            @click="handleCreateTag"
          >
            {{ createTagSaving ? '创建中...' : '创建' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 管理候选人标签 -->
    <div
      v-if="tagManageOpen && managingTalent"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      @click.self="closeTagManage"
    >
      <div class="w-full max-w-md bg-card rounded-2xl border border-border shadow-panel p-5">
        <div class="flex items-center justify-between mb-1">
          <h2 class="text-base font-bold text-foreground">管理标签</h2>
          <button type="button" class="p-1 rounded-lg hover:bg-muted" @click="closeTagManage">
            <X :size="16" />
          </button>
        </div>
        <p class="text-xs text-muted-foreground mb-4">{{ managingTalent.candidateName }}</p>

        <div class="mb-4">
          <div class="text-xs font-semibold text-muted-foreground mb-2">已绑定</div>
          <div v-if="!(managingTalent.tags?.length)" class="text-xs text-muted-foreground">暂无标签</div>
          <div v-else class="flex flex-wrap gap-1.5">
            <span
              v-for="tag in managingTalent.tags"
              :key="tag.id"
              class="inline-flex items-center gap-0.5 text-xs pl-2 pr-1 py-0.5 rounded-full bg-accent text-accent-foreground"
            >
              {{ tag.tagName }}
              <button
                type="button"
                class="p-0.5 rounded-full hover:bg-muted-foreground/20"
                :disabled="tagActionLoading"
                @click="handleUnbindTag(managingTalent, tag)"
              >
                <X :size="10" />
              </button>
            </span>
          </div>
        </div>

        <div>
          <div class="text-xs font-semibold text-muted-foreground mb-2">添加标签</div>
          <div v-if="allTags.length === 0" class="text-xs text-muted-foreground">
            请先创建标签
          </div>
          <div v-else-if="availableTagsForManage.length === 0" class="text-xs text-muted-foreground">
            所有标签已绑定
          </div>
          <div v-else class="flex flex-wrap gap-1.5 max-h-40 overflow-y-auto scrollbar-thin">
            <button
              v-for="tag in availableTagsForManage"
              :key="tag.id"
              type="button"
              class="text-xs px-2.5 py-1 rounded-full border border-brand-purple/30 text-brand-purple hover:bg-brand-tint-2 disabled:opacity-50"
              :disabled="tagActionLoading"
              @click="handleBindTag(tag)"
            >
              + {{ tag.tagName }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑人才库记录 -->
    <div
      v-if="editOpen && editingTalent"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      @click.self="closeEditDialog"
    >
      <div class="w-full max-w-lg bg-card rounded-2xl border border-border shadow-panel p-5 max-h-[90vh] overflow-y-auto scrollbar-thin">
        <div class="flex items-center justify-between mb-1">
          <h2 class="text-base font-bold text-foreground">编辑人才库记录</h2>
          <button type="button" class="p-1 rounded-lg hover:bg-muted" @click="closeEditDialog">
            <X :size="16" />
          </button>
        </div>
        <p class="text-xs text-muted-foreground mb-4">
          {{ editingTalent.candidateName }}
          <span v-if="editingTalent.sourceJobTitle"> · 来源：{{ editingTalent.sourceJobTitle }}</span>
        </p>

        <div class="space-y-3">
          <div>
            <label class="text-xs text-muted-foreground block mb-1.5">当前职位</label>
            <input
              v-model="editForm.currentTitle"
              type="text"
              class="w-full px-3 py-2 rounded-xl bg-muted text-sm outline-none border border-transparent focus:border-brand-blue/40"
            />
          </div>
          <div>
            <label class="text-xs text-muted-foreground block mb-1.5">当前公司</label>
            <input
              v-model="editForm.currentCompany"
              type="text"
              class="w-full px-3 py-2 rounded-xl bg-muted text-sm outline-none border border-transparent focus:border-brand-blue/40"
            />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="text-xs text-muted-foreground block mb-1.5">求职状态</label>
              <select
                v-model.number="editForm.jobSeekingStatus"
                class="w-full px-3 py-2 rounded-xl bg-muted text-sm outline-none border border-transparent focus:border-brand-blue/40"
              >
                <option v-for="opt in jobSeekingOptions" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </option>
              </select>
            </div>
            <div>
              <label class="text-xs text-muted-foreground block mb-1.5">匹配分 (0-100)</label>
              <input
                v-model.number="editForm.matchScore"
                type="number"
                min="0"
                max="100"
                class="w-full px-3 py-2 rounded-xl bg-muted text-sm outline-none border border-transparent focus:border-brand-blue/40"
              />
            </div>
          </div>
          <div>
            <label class="text-xs text-muted-foreground block mb-1.5">归档原因</label>
            <textarea
              v-model="editForm.archiveReason"
              rows="3"
              class="w-full px-3 py-2 rounded-xl bg-muted text-sm outline-none resize-none border border-transparent focus:border-brand-blue/40"
            />
          </div>
          <label class="inline-flex items-center gap-2 text-sm text-foreground cursor-pointer">
            <input v-model="editForm.isSaved" type="checkbox" class="rounded border-border" />
            标记为收藏
          </label>
        </div>

        <div class="flex justify-end gap-2 mt-5">
          <button
            type="button"
            class="px-4 py-2 rounded-xl border border-border text-sm"
            :disabled="editSaving"
            @click="closeEditDialog"
          >
            取消
          </button>
          <button
            type="button"
            class="px-4 py-2 rounded-xl gradient-purple text-white text-sm font-medium disabled:opacity-50"
            :disabled="editSaving"
            @click="handleSaveEdit"
          >
            {{ editSaving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
