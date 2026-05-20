<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/message/style/css'
import {
  Plus,
  Search,
  Filter,
  ChevronDown,
  Users,
  Eye,
  Edit,
  Trash2,
  Sparkles,
  MapPin,
  Briefcase,
  CheckCircle,
  AlertCircle,
  XCircle,
} from 'lucide-vue-next'
import { deleteHrJob, fetchHrJobList } from '@/api/hrJob'
import JobCreateDialog from '@/components/hr/JobCreateDialog.vue'
import { JOB_STATUS } from '@/constants/job'
import { mapJobPostToHrRow, type HrJobRow } from '@/utils/hrJobMapper'
import { getErrorMessage } from '@/utils/validators'

const statusMap: Record<string, { label: string; color: string; bg: string }> = {
  招聘中: { label: '招聘中', color: 'text-brand-green', bg: 'bg-green-50 border-green-200' },
  暂停: { label: '已暂停', color: 'text-brand-orange', bg: 'bg-orange-50 border-orange-200' },
  已完成: { label: '已完成', color: 'text-muted-foreground', bg: 'bg-muted border-border' },
}

const priorityMap: Record<string, string> = {
  高: 'text-brand-red bg-red-50 border-red-200',
  中: 'text-brand-orange bg-orange-50 border-orange-200',
  低: 'text-muted-foreground bg-muted border-border',
}

const statusFilterOptions = [
  { label: '全部状态', value: '' },
  { label: '招聘中', value: String(JOB_STATUS.OPEN) },
  { label: '已暂停', value: String(JOB_STATUS.PAUSED) },
  { label: '已完成', value: String(JOB_STATUS.CLOSED) },
]

const jobs = ref<HrJobRow[]>([])
const total = ref(0)
const loading = ref(false)
const errorMsg = ref('')
const searchTitle = ref('')
const statusFilter = ref('')
const drawerOpen = ref(false)
const selectedJob = ref<HrJobRow | null>(null)
const jobFormOpen = ref(false)
const jobFormMode = ref<'create' | 'edit'>('create')
const editJobId = ref<number | null>(null)
const publishSuccessMsg = ref('')
const deletingId = ref<number | null>(null)

const summaryCards = computed(() => {
  const openCount = jobs.value.filter((j) => j.status === '招聘中').length
  const pausedCount = jobs.value.filter((j) => j.status === '暂停').length
  const totalApplied = jobs.value.reduce((sum, j) => sum + j.applied, 0)
  const totalMatched = jobs.value.reduce((sum, j) => sum + j.matched, 0)
  return [
    { label: '招聘中', value: openCount, icon: CheckCircle, color: 'text-brand-green', bg: 'bg-green-50' },
    { label: '总投递', value: totalApplied, icon: Users, color: 'text-brand-blue', bg: 'bg-brand-tint' },
    { label: 'AI匹配', value: totalMatched, icon: Sparkles, color: 'text-brand-purple', bg: 'bg-brand-tint-2' },
    { label: '已暂停', value: pausedCount, icon: AlertCircle, color: 'text-brand-orange', bg: 'bg-orange-50' },
  ]
})

const matchRatePercent = computed(() => {
  const job = selectedJob.value
  if (!job || job.applied <= 0) return 0
  return Math.round((job.matched / job.applied) * 100)
})

const competitionRatio = computed(() => {
  const job = selectedJob.value
  if (!job || job.headcount <= 0) return '—'
  return `1:${Math.round(job.applied / job.headcount)}`
})

async function loadJobs() {
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await fetchHrJobList({
      current: 1,
      size: 100,
      title: searchTitle.value.trim() || undefined,
      status: statusFilter.value ? Number(statusFilter.value) : undefined,
    })
    jobs.value = (data.records ?? []).map(mapJobPostToHrRow)
    total.value = data.total ?? jobs.value.length
    if (selectedJob.value) {
      const updated = jobs.value.find((j) => j.id === selectedJob.value?.id)
      if (updated) selectedJob.value = updated
      else if (!jobs.value.length) drawerOpen.value = false
    }
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '岗位列表加载失败')
    jobs.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function openDrawer(job: HrJobRow) {
  selectedJob.value = job
  drawerOpen.value = true
}

function openCreateDialog() {
  publishSuccessMsg.value = ''
  jobFormMode.value = 'create'
  editJobId.value = null
  jobFormOpen.value = true
}

function openEditDialog(job: HrJobRow) {
  publishSuccessMsg.value = ''
  jobFormMode.value = 'edit'
  editJobId.value = job.id
  jobFormOpen.value = true
}

function onJobFormSuccess() {
  publishSuccessMsg.value = jobFormMode.value === 'edit' ? '岗位更新成功' : '岗位发布成功'
  loadJobs()
}

async function handleDelete(job: HrJobRow) {
  try {
    await ElMessageBox.confirm(
      `确定删除岗位「${job.title}」？删除后不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  } catch {
    return
  }

  deletingId.value = job.id
  try {
    await deleteHrJob(job.id)
    ElMessage.success('岗位已删除')
    if (selectedJob.value?.id === job.id) {
      selectedJob.value = null
      drawerOpen.value = false
    }
    await loadJobs()
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '删除失败'))
  } finally {
    deletingId.value = null
  }
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(searchTitle, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => loadJobs(), 300)
})

watch(statusFilter, () => {
  loadJobs()
})

onMounted(() => {
  loadJobs()
})
</script>

<template>
  <div data-cmp="JobsManagement" class="flex h-full" style="height: calc(100vh - 64px)">
    <div class="flex-1 p-6 flex flex-col gap-4 min-w-0 overflow-auto scrollbar-thin">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-xl font-bold text-foreground">岗位管理</h1>
          <p class="text-sm text-muted-foreground mt-0.5">共 {{ total }} 个岗位 · AI智能管理</p>
        </div>
        <button
          type="button"
          class="flex items-center gap-2 px-4 py-2.5 rounded-control gradient-blue text-white text-sm font-medium"
          @click="openCreateDialog"
        >
          <Plus :size="16" />
          <span>新建岗位</span>
        </button>
      </div>

      <div class="flex gap-4">
        <div
          v-for="s in summaryCards"
          :key="s.label"
          class="flex-1 bg-card rounded-xl p-4 shadow-card flex items-center gap-3"
        >
          <div :class="['w-9 h-9 rounded-xl flex items-center justify-center', s.bg]">
            <component :is="s.icon" :size="18" :class="s.color" />
          </div>
          <div>
            <div class="text-lg font-bold text-foreground">{{ s.value }}</div>
            <div class="text-xs text-muted-foreground">{{ s.label }}</div>
          </div>
        </div>
      </div>

      <div v-if="publishSuccessMsg" class="text-xs text-brand-green bg-green-50 border border-green-200 rounded-xl px-3 py-2">
        {{ publishSuccessMsg }}
      </div>

      <div v-if="errorMsg" class="text-xs text-red-600 bg-red-50 border border-red-100 rounded-xl px-3 py-2">
        {{ errorMsg }}
      </div>

      <div class="flex items-center gap-3 bg-card rounded-xl p-3 shadow-card">
        <div class="flex items-center gap-2 bg-muted rounded-lg px-3 py-2 flex-1 max-w-xs">
          <Search :size="14" class="text-muted-foreground" />
          <input
            v-model="searchTitle"
            class="bg-transparent text-sm outline-none flex-1 placeholder:text-muted-foreground"
            placeholder="搜索岗位名称"
          />
        </div>
        <div class="relative">
          <select
            v-model="statusFilter"
            class="appearance-none flex items-center gap-1.5 pl-3 pr-8 py-2 rounded-lg bg-muted text-sm text-muted-foreground border border-border cursor-pointer outline-none"
          >
            <option v-for="opt in statusFilterOptions" :key="opt.label" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
          <ChevronDown :size="14" class="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
        </div>
        <button type="button" class="flex items-center gap-1.5 px-3 py-2 rounded-lg border border-border text-sm text-muted-foreground hover:text-foreground ml-auto">
          <Filter :size="14" />
          <span>高级筛选</span>
        </button>
      </div>

      <div class="bg-card rounded-xl shadow-card overflow-hidden">
        <table class="w-full">
          <thead>
            <tr class="border-b border-border bg-muted/50">
              <th
                v-for="h in ['岗位名称', '部门', '状态', '优先级', '薪资范围', '投递/匹配', '发布时间', '操作']"
                :key="h"
                class="text-left text-xs font-medium text-muted-foreground px-4 py-3"
              >
                {{ h }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="8" class="px-4 py-8 text-center text-sm text-muted-foreground">加载中...</td>
            </tr>
            <tr v-else-if="!jobs.length">
              <td colspan="8" class="px-4 py-8 text-center text-sm text-muted-foreground">暂无岗位数据</td>
            </tr>
            <template v-else>
              <tr
                v-for="job in jobs"
                :key="job.id"
                class="border-b border-border hover:bg-muted/30 transition-colors"
              >
                <td class="px-4 py-3">
                  <div class="flex items-center gap-3">
                    <div class="w-8 h-8 rounded-lg gradient-blue flex items-center justify-center">
                      <Briefcase :size="14" class="text-white" />
                    </div>
                    <div>
                      <div class="text-sm font-medium text-foreground">{{ job.title }}</div>
                      <div class="flex items-center gap-1 text-xs text-muted-foreground mt-0.5">
                        <MapPin :size="10" />
                        <span>{{ job.location }}</span>
                      </div>
                    </div>
                  </div>
                </td>
                <td class="px-4 py-3 text-sm text-muted-foreground">{{ job.dept }}</td>
                <td class="px-4 py-3">
                  <span
                    :class="[
                      'text-xs px-2 py-1 rounded-full border',
                      statusMap[job.status]?.bg,
                      statusMap[job.status]?.color,
                    ]"
                  >
                    {{ statusMap[job.status]?.label }}
                  </span>
                </td>
                <td class="px-4 py-3">
                  <span :class="['text-xs px-2 py-1 rounded-full border', priorityMap[job.priority]]">
                    {{ job.priority }}优先
                  </span>
                </td>
                <td class="px-4 py-3 text-sm font-medium text-foreground">{{ job.salary }}</td>
                <td class="px-4 py-3">
                  <div class="flex items-center gap-2">
                    <span class="text-sm text-foreground">{{ job.applied }}</span>
                    <span class="text-muted-foreground">/</span>
                    <span class="text-sm text-brand-purple font-medium">{{ job.matched }}</span>
                  </div>
                  <div class="mt-1 h-1 bg-muted rounded-full overflow-hidden w-16">
                    <div
                      class="h-full rounded-full bg-brand-purple"
                      :style="{ width: `${job.applied > 0 ? Math.min((job.matched / job.applied) * 100, 100) : 0}%` }"
                    />
                  </div>
                </td>
                <td class="px-4 py-3 text-xs text-muted-foreground">{{ job.created || '—' }}</td>
                <td class="px-4 py-3">
                  <div class="flex items-center gap-1">
                    <button
                      type="button"
                      class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground hover:text-foreground"
                      @click="openDrawer(job)"
                    >
                      <Eye :size="14" />
                    </button>
                    <button
                      type="button"
                      class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground hover:text-foreground"
                      title="编辑"
                      @click="openEditDialog(job)"
                    >
                      <Edit :size="14" />
                    </button>
                    <button
                      type="button"
                      class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground hover:text-brand-red disabled:opacity-50"
                      title="删除"
                      :disabled="deletingId === job.id"
                      @click="handleDelete(job)"
                    >
                      <Trash2 :size="14" />
                    </button>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>
    </div>

    <div
      class="flex-shrink-0 border-l border-border bg-card transition-all duration-300 overflow-auto scrollbar-thin"
      :class="drawerOpen && selectedJob ? 'w-80' : 'w-0 overflow-hidden'"
    >
      <div v-if="selectedJob" class="p-5" style="width: 320px">
        <div class="flex items-center justify-between mb-5">
          <span class="text-sm font-semibold text-foreground">岗位画像分析</span>
          <button type="button" class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground" @click="drawerOpen = false">
            <XCircle :size="16" />
          </button>
        </div>
        <div class="bg-muted rounded-xl p-4 mb-4">
          <div class="text-base font-bold text-foreground mb-1">{{ selectedJob.title }}</div>
          <div class="text-sm text-muted-foreground mb-2">{{ selectedJob.dept }} · {{ selectedJob.location }}</div>
          <div class="text-lg font-bold text-brand-blue">{{ selectedJob.salary }}</div>
        </div>
        <div class="flex gap-3 mb-4">
          <div
            v-for="s in [
              { label: '总投递', value: selectedJob.applied, color: 'text-brand-blue' },
              { label: 'AI匹配', value: selectedJob.matched, color: 'text-brand-purple' },
              { label: '招聘人数', value: selectedJob.headcount, color: 'text-brand-green' },
            ]"
            :key="s.label"
            class="flex-1 bg-muted rounded-xl p-3 text-center"
          >
            <div :class="['text-lg font-bold', s.color]">{{ s.value }}</div>
            <div class="text-xs text-muted-foreground">{{ s.label }}</div>
          </div>
        </div>
        <div class="bg-accent rounded-xl p-4 border border-brand-border/50">
          <div class="flex items-center gap-2 mb-3">
            <Sparkles :size="14" class="text-brand-purple" />
            <span class="text-xs font-semibold text-brand-purple">AI岗位画像</span>
          </div>
          <div class="space-y-2 text-xs text-muted-foreground">
            <div class="flex justify-between">
              <span>岗位状态</span>
              <span class="text-foreground font-medium">{{ statusMap[selectedJob.status]?.label }}</span>
            </div>
            <div class="flex justify-between">
              <span>竞争系数</span>
              <span class="text-brand-red font-medium">{{ competitionRatio }}</span>
            </div>
            <div class="flex justify-between">
              <span>匹配率</span>
              <span class="text-brand-green font-medium">{{ matchRatePercent }}%</span>
            </div>
            <div class="flex justify-between">
              <span>发布人</span>
              <span class="text-foreground font-medium">{{ selectedJob.raw.publisherName || '—' }}</span>
            </div>
          </div>
          <div class="mt-3 pt-3 border-t border-brand-border/50 text-xs text-muted-foreground leading-relaxed">
            该岗位投递量{{ selectedJob.applied > 50 ? '充足' : '一般' }}，建议结合 AI 匹配分优化筛选阈值
          </div>
        </div>
        <div class="mt-4 flex flex-col gap-2">
          <button type="button" class="w-full py-2.5 rounded-control gradient-blue text-white text-sm font-medium">
            查看所有候选人
          </button>
          <button
            type="button"
            class="w-full py-2.5 rounded-xl border border-border text-sm text-foreground hover:bg-muted"
            @click="openEditDialog(selectedJob)"
          >
            编辑岗位信息
          </button>
        </div>
      </div>
    </div>

    <JobCreateDialog
      :open="jobFormOpen"
      :mode="jobFormMode"
      :job-id="editJobId"
      @close="jobFormOpen = false"
      @success="onJobFormSuccess"
    />
  </div>
</template>
