<script setup lang="ts">
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import { ref, computed, onMounted } from 'vue'
import {
  Search,
  RefreshCw,
  ShieldAlert,
  AlertTriangle,
  CheckCircle,
  XCircle,
  Ban,
  Loader2,
  AlertCircle,
  ChevronLeft,
  ChevronRight,
  Activity,
  Eye,
} from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listRiskJobs,
  takedownJob,
  JOB_RISK_STATUS,
  JOB_RISK_STATUS_OPTIONS,
  type JobRiskVO,
} from '@/api/adminJobRisk'
import { getErrorMessage } from '@/utils/validators'

/* ---- 高危词汇正则 ---- */
const RISK_KEYWORDS_PATTERN = /刷单|传销|日结|押金|高薪日结|赌博|色情|诈骗|代刷|兼职打字|挂机|零投入|躺赚/g

/**
 * 安全关键词高亮：将文本 split 为普通段和匹配段，返回 { text, highlight } 数组。
 * 渲染时用 v-for + <span> 包裹，不使用 v-html，避免 XSS。
 */
function splitByRiskKeywords(text: string): { text: string; highlight: boolean }[] {
  if (!text) return [{ text: '—', highlight: false }]
  const segments: { text: string; highlight: boolean }[] = []
  let lastIndex = 0
  // 重置 lastIndex 以确保全局正则从头开始匹配
  RISK_KEYWORDS_PATTERN.lastIndex = 0
  let match: RegExpExecArray | null
  while ((match = RISK_KEYWORDS_PATTERN.exec(text)) !== null) {
    if (match.index > lastIndex) {
      segments.push({ text: text.slice(lastIndex, match.index), highlight: false })
    }
    segments.push({ text: match[0], highlight: true })
    lastIndex = match.index + match[0].length
  }
  if (lastIndex < text.length) {
    segments.push({ text: text.slice(lastIndex), highlight: false })
  }
  if (segments.length === 0) {
    segments.push({ text, highlight: false })
  }
  return segments
}

/* ---- 列表状态 ---- */
const loading = ref(false)
const records = ref<JobRiskVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(15)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

/* ---- 查询条件 ---- */
const searchKeyword = ref('')
const filterStatus = ref<number | undefined>(undefined)

/* ---- 统计卡片 ---- */
const stats = computed(() => [
  {
    label: '风险预警',
    value: records.value.filter(r => r.status === JOB_RISK_STATUS.WARNING).length,
    icon: AlertTriangle,
    cls: 'text-brand-orange bg-orange-50',
  },
  {
    label: '正常职位',
    value: records.value.filter(r => r.status === JOB_RISK_STATUS.NORMAL).length,
    icon: CheckCircle,
    cls: 'text-brand-green bg-green-50',
  },
  {
    label: '已下架',
    value: records.value.filter(r => r.status === JOB_RISK_STATUS.TAKEN_DOWN).length,
    icon: Ban,
    cls: 'text-brand-red bg-red-50',
  },
  {
    label: '本页总数',
    value: total.value,
    icon: Activity,
    cls: 'text-brand-blue bg-brand-tint',
  },
])

/* ---- 状态样式 ---- */
const statusStyleMap: Record<number, string> = {
  [JOB_RISK_STATUS.NORMAL]:     'bg-green-50 text-brand-green border-green-200',
  [JOB_RISK_STATUS.WARNING]:    'bg-orange-50 text-brand-orange border-orange-200',
  [JOB_RISK_STATUS.TAKEN_DOWN]: 'bg-red-50 text-brand-red border-red-200',
}

function getStatusCls(status: number) {
  return statusStyleMap[status] ?? 'bg-muted text-muted-foreground border-border'
}

/* ---- 数据加载 ---- */
async function fetchList() {
  loading.value = true
  try {
    const data = await listRiskJobs({
      page: page.value,
      size: pageSize.value,
      keyword: searchKeyword.value.trim() || undefined,
      status: filterStatus.value,
    })
    records.value = data?.records ?? []
    total.value = Number(data?.total ?? 0)
  } catch (e) {
    records.value = []
    total.value = 0
    ElMessage.error(getErrorMessage(e, '加载职位风控数据失败'))
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchList()
}

/* ---- 搜索防抖 ---- */
let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    page.value = 1
    fetchList()
  }, 400)
}

function prevPage() {
  if (page.value > 1) { page.value--; fetchList() }
}

function nextPage() {
  if (page.value < totalPages.value) { page.value++; fetchList() }
}

function formatTime(value?: string) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

function formatSalary(min: number, max: number) {
  if (!min && !max) return '—'
  const fmt = (v: number) => v >= 1000 ? (v / 1000).toFixed(0) + 'K' : String(v)
  return `${fmt(min)} - ${fmt(max)}`
}

/** 截断描述到指定长度 */
function truncateDesc(text: string, maxLen = 80) {
  if (!text) return '—'
  return text.length > maxLen ? text.slice(0, maxLen) + '…' : text
}

/* ---- 强制下架（二次确认） ---- */
async function handleTakedown(row: JobRiskVO) {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt(
      `确定要强制下架职位「${row.jobTitle}」吗？此操作将立即生效且会记入系统日志。`,
      '强制下架确认',
      {
        confirmButtonText: '确定下架',
        cancelButtonText: '取消',
        type: 'warning',
        inputPlaceholder: '请输入下架理由（必填）',
        inputValidator: (val: string) => {
          if (!val || !val.trim()) return '请输入下架理由'
          return true
        },
        customClass: '',
      },
    )
    reason = (result as any).value || ''
  } catch {
    return // 用户取消
  }

  try {
    await takedownJob(row.id, reason)
    ElMessage.success('已强制下架，操作已记入系统日志')
    await fetchList()
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '下架操作失败'))
  }
}

onMounted(fetchList)
</script>

<template>
  <div data-cmp="JobRisk" class="scrollbar-thin h-full overflow-y-auto p-6 sm:p-8">
    <div class="mx-auto max-w-6xl">
      <!-- 页面头部 -->
      <div class="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 class="text-2xl font-black text-foreground">职位风控中心</h1>
          <p class="mt-1 text-muted-foreground">针对平台流通的职位数据进行事后审查与违规阻断</p>
        </div>
      </div>

      <!-- 统计卡片 -->
      <div class="mb-6 flex gap-4">
        <div
          v-for="s in stats"
          :key="s.label"
          class="flex-1 rounded-2xl border border-border bg-card p-5 shadow-card"
        >
          <div :class="['mb-3 flex h-10 w-10 items-center justify-center rounded-xl', s.cls]">
            <component :is="s.icon" :size="18" />
          </div>
          <div class="text-2xl font-black text-foreground">{{ s.value }}</div>
          <div class="mt-1 text-sm text-muted-foreground">{{ s.label }}</div>
        </div>
      </div>

      <!-- 搜索栏 -->
      <div class="mb-6 flex flex-col gap-3 rounded-2xl border border-border bg-card p-4 shadow-card sm:flex-row sm:items-center">
        <div class="flex flex-1 items-center gap-2 rounded-xl bg-muted px-3 py-2">
          <Search :size="16" class="shrink-0 text-muted-foreground" />
          <input
            v-model="searchKeyword"
            type="search"
            class="min-w-0 flex-1 bg-transparent text-sm text-foreground outline-none placeholder:text-muted-foreground"
            placeholder="搜索职位名称、企业名称或描述关键词…"
            @input="onSearchInput"
            @keyup.enter="handleSearch"
          />
        </div>
        <select
          v-model="filterStatus"
          class="rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:border-[#85A185]"
          @change="handleSearch"
        >
          <option
            v-for="opt in JOB_RISK_STATUS_OPTIONS"
            :key="String(opt.value)"
            :value="opt.value"
          >
            {{ opt.label }}
          </option>
        </select>
        <button
          type="button"
          class="flex items-center justify-center gap-2 rounded-xl border border-border px-4 py-2 text-sm font-medium text-foreground transition-colors hover:bg-muted"
          :disabled="loading"
          @click="handleSearch"
        >
          <Search :size="15" />
          查询
        </button>
        <button
          type="button"
          class="flex items-center justify-center gap-2 rounded-xl border border-border px-3 py-2 text-sm text-muted-foreground hover:bg-muted"
          :disabled="loading"
          @click="fetchList"
        >
          <RefreshCw :size="15" :class="loading ? 'animate-spin' : ''" />
        </button>
      </div>

      <!-- 高密度数据表格 -->
      <div class="overflow-hidden rounded-2xl border border-border bg-card shadow-card">
        <div class="overflow-x-auto">
          <table class="w-full min-w-[960px] text-left text-sm">
            <thead class="border-b border-border bg-muted/50 text-xs uppercase tracking-wide text-muted-foreground">
              <tr>
                <th class="px-4 py-3 font-semibold">发布者 ID</th>
                <th class="px-4 py-3 font-semibold">职位名称</th>
                <th class="px-4 py-3 font-semibold">企业名称</th>
                <th class="px-4 py-3 font-semibold">薪酬范围</th>
                <th class="px-4 py-3 font-semibold" style="min-width: 240px">职位描述片段</th>
                <th class="px-4 py-3 font-semibold">风险状态</th>
                <th class="px-4 py-3 font-semibold">发布时间</th>
                <th class="px-4 py-3 text-right font-semibold">操作</th>
              </tr>
            </thead>
            <tbody>
              <!-- Loading -->
              <tr v-if="loading">
                <td colspan="8" class="px-4 py-16 text-center">
                  <div class="flex items-center justify-center gap-2 text-muted-foreground">
                    <Loader2 :size="20" class="animate-spin text-brand-blue" />
                    <span>加载中…</span>
                  </div>
                </td>
              </tr>
              <!-- Empty -->
              <tr v-else-if="records.length === 0">
                <td colspan="8" class="px-4 py-16 text-center">
                  <div class="flex flex-col items-center gap-2 text-muted-foreground">
                    <AlertCircle :size="32" class="opacity-40" />
                    <span class="text-sm">暂无职位风控数据</span>
                  </div>
                </td>
              </tr>
              <!-- Data rows -->
              <tr
                v-for="row in records"
                v-else
                :key="row.id"
                class="border-b border-border last:border-0 transition-colors hover:bg-muted/30"
                :class="row.status === JOB_RISK_STATUS.WARNING ? 'bg-orange-50/20' : ''"
              >
                <td class="px-4 py-3 font-mono text-xs text-muted-foreground">#{{ row.publisherId }}</td>
                <td class="px-4 py-3 font-medium text-foreground">{{ row.jobTitle }}</td>
                <td class="px-4 py-3 text-muted-foreground">{{ row.companyName }}</td>
                <td class="px-4 py-3">
                  <span class="text-sm font-bold text-brand-blue">{{ formatSalary(row.salaryMin, row.salaryMax) }}</span>
                </td>
                <!-- 安全高亮渲染：v-for + span，不使用 v-html -->
                <td class="px-4 py-3 text-xs leading-relaxed text-muted-foreground" style="max-width: 300px">
                  <template v-for="(seg, i) in splitByRiskKeywords(truncateDesc(row.description))" :key="i">
                    <span
                      v-if="seg.highlight"
                      class="rounded bg-red-100 px-0.5 font-medium text-red-700"
                    >{{ seg.text }}</span>
                    <span v-else>{{ seg.text }}</span>
                  </template>
                </td>
                <td class="px-4 py-3">
                  <span
                    :class="['inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-xs font-medium', getStatusCls(row.status)]"
                  >
                    <AlertTriangle v-if="row.status === JOB_RISK_STATUS.WARNING" :size="12" />
                    <CheckCircle v-else-if="row.status === JOB_RISK_STATUS.NORMAL" :size="12" />
                    <Ban v-else :size="12" />
                    {{ row.statusLabel }}
                  </span>
                </td>
                <td class="px-4 py-3 text-xs text-muted-foreground">{{ formatTime(row.createdAt) }}</td>
                <td class="px-4 py-3">
                  <div class="flex justify-end gap-1">
                    <button
                      type="button"
                      class="rounded-lg p-2 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                      title="查看详情"
                    >
                      <Eye :size="14" />
                    </button>
                    <button
                      v-if="row.status !== JOB_RISK_STATUS.TAKEN_DOWN"
                      type="button"
                      class="flex items-center gap-1 rounded-lg border border-red-200 bg-red-50 px-2.5 py-1.5 text-xs font-medium text-brand-red transition-colors hover:bg-red-100"
                      @click="handleTakedown(row)"
                    >
                      <Ban :size="12" />
                      强制下架
                    </button>
                    <span
                      v-else
                      class="flex items-center gap-1 rounded-lg bg-muted px-2.5 py-1.5 text-xs text-muted-foreground"
                    >
                      <XCircle :size="12" />
                      已下架
                    </span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 分页 -->
        <div
          v-if="total > 0"
          class="flex flex-col items-center justify-between gap-3 border-t border-border px-4 py-3 text-sm text-muted-foreground sm:flex-row"
        >
          <span>共 {{ total }} 条，第 {{ page }} / {{ totalPages }} 页</span>
          <div class="flex items-center gap-2">
            <button
              type="button"
              class="rounded-lg border border-border p-1.5 text-muted-foreground hover:text-foreground disabled:opacity-30"
              :disabled="page <= 1 || loading"
              @click="prevPage"
            >
              <ChevronLeft :size="14" />
            </button>
            <span class="text-xs">{{ page }} / {{ totalPages }}</span>
            <button
              type="button"
              class="rounded-lg border border-border p-1.5 text-muted-foreground hover:text-foreground disabled:opacity-30"
              :disabled="page >= totalPages || loading"
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
