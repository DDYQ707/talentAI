<script setup lang="ts">
import 'element-plus/es/components/message/style/css';
import 'element-plus/es/components/message-box/style/css';
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  Plus,
  Search,
  User,
  CheckCircle,
  Clock,
  XCircle,
  Send,
  FileText,
  ChevronLeft,
  ChevronRight,
  Loader2,
  AlertCircle,
  MailCheck,
  MailX,
  Undo2,
  Edit,
  X,
  Users,
} from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchOfferList,
  fetchOfferByApplication,
  createOffer,
  updateOffer,
  issueOffer,
  OFFER_STATUS,
  type OfferListVO,
  type OfferCreatePayload,
  type OfferUpdatePayload,
  revokeOffer,
} from '@/api/offer'
import { archiveTalent, fetchTalentPoolExistsBatch } from '@/api/talentPool'
import { buildTalentArchivePayload } from '@/utils/talentArchive'
import { getErrorMessage } from '@/utils/validators'

const route = useRoute()
const fromApplicationId = computed(() => {
  const id = Number(route.query.applicationId)
  return Number.isFinite(id) && id > 0 ? id : null
})
const fromCandidateId = computed(() => {
  const id = Number(route.query.candidateId)
  return Number.isFinite(id) && id > 0 ? id : null
})
const fromJobId = computed(() => {
  const id = Number(route.query.jobId)
  return Number.isFinite(id) && id > 0 ? id : null
})
const fromJobTitle = computed(() =>
  typeof route.query.jobTitle === 'string' ? route.query.jobTitle.trim() : '',
)

/* ---- 响应式状态 ---- */
const offers = ref<OfferListVO[]>([])
const loading = ref(false)
const errorMsg = ref('')
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const statusFilter = ref<number | undefined>(undefined)
const talentPoolMap = ref<Record<string, boolean>>({})

const formOpen = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const formSubmitting = ref(false)
const editingOfferId = ref<number | null>(null)
const form = ref({
  baseSalary: '',
  annualSalary: '',
  positionLevel: '',
  expectedOnboardDate: '',
  probationMonths: '3',
  salaryRemark: '',
  remark: '',
})

/* ---- 状态样式映射（数字 Key） ---- */
const statusStyles: Record<number, { cls: string; icon: typeof Send }> = {
  [OFFER_STATUS.PENDING]:   { cls: 'bg-orange-50 text-brand-orange border-orange-200', icon: Clock },
  [OFFER_STATUS.APPROVING]: { cls: 'bg-orange-50 text-brand-orange border-orange-200', icon: Clock },
  [OFFER_STATUS.APPROVED]:  { cls: 'bg-green-50 text-brand-green border-green-200', icon: CheckCircle },
  [OFFER_STATUS.REJECTED]:  { cls: 'bg-red-50 text-brand-red border-red-200', icon: XCircle },
  [OFFER_STATUS.ISSUED]:    { cls: 'bg-brand-tint text-brand-blue border-brand-border', icon: Send },
  [OFFER_STATUS.ACCEPTED]:  { cls: 'bg-green-50 text-brand-green border-green-200', icon: MailCheck },
  [OFFER_STATUS.DECLINED]:  { cls: 'bg-red-50 text-brand-red border-red-200', icon: MailX },
  [OFFER_STATUS.REVOKED]:   { cls: 'bg-muted text-muted-foreground border-border', icon: Undo2 },
}

/** 兜底样式 */
function getStatusStyle(status: number) {
  return statusStyles[status] ?? { cls: 'bg-muted text-muted-foreground border-border', icon: FileText }
}

/* ---- 状态筛选选项 ---- */
const statusOptions = [
  { label: '全部状态', value: undefined },
  { label: '待发放', value: OFFER_STATUS.APPROVED },
  { label: '已拒绝', value: OFFER_STATUS.REJECTED },
  { label: '已发放', value: OFFER_STATUS.ISSUED },
  { label: '候选人已接受', value: OFFER_STATUS.ACCEPTED },
  { label: '候选人已拒绝', value: OFFER_STATUS.DECLINED },
  { label: '已撤回', value: OFFER_STATUS.REVOKED },
]
const showStatusDropdown = ref(false)

/* ---- 统计卡片（从列表数据动态计算） ---- */
const stats = computed(() => [
  { label: '本月发放', value: total.value, change: '', color: 'text-brand-blue' },
  {
    label: '已接受',
    value: offers.value.filter(o => o.status === OFFER_STATUS.ACCEPTED).length,
    change: total.value > 0
      ? Math.round(offers.value.filter(o => o.status === OFFER_STATUS.ACCEPTED).length / total.value * 100) + '%'
      : '0%',
    color: 'text-brand-green',
  },
  {
    label: '待发放',
    value: offers.value.filter(o => o.status === OFFER_STATUS.APPROVED).length,
    change: '',
    color: 'text-brand-orange',
  },
  {
    label: '已拒绝',
    value: offers.value.filter(o => o.status === OFFER_STATUS.REJECTED || o.status === OFFER_STATUS.DECLINED).length,
    change: total.value > 0
      ? Math.round(offers.value.filter(o => o.status === OFFER_STATUS.REJECTED || o.status === OFFER_STATUS.DECLINED).length / total.value * 100) + '%'
      : '0%',
    color: 'text-brand-red',
  },
])

/* ---- 分页 ---- */
const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

/* ---- 薪资格式化 ---- */
function formatSalary(amount: number | null | undefined): string {
  if (amount == null || amount <= 0) return '-'
  if (amount >= 1000) return (amount / 1000).toFixed(0) + 'K'
  return String(amount)
}

function formatDate(dateStr: string | null | undefined): string {
  if (!dateStr) return '-'
  return dateStr.slice(0, 10)
}

function canArchiveOffer(offer: OfferListVO) {
  if (!offer.candidateId) return false
  if (talentPoolMap.value[String(offer.candidateId)]) return false
  return [OFFER_STATUS.DECLINED, OFFER_STATUS.REJECTED, OFFER_STATUS.REVOKED].includes(offer.status)
}

async function handleArchiveToTalentPool(offer: OfferListVO) {
  if (!offer.candidateId) {
    ElMessage.warning('缺少候选人信息')
    return
  }
  const reason = window.prompt('请输入归档原因（可选）', 'Offer 结束后归档')
  if (reason === null) return
  try {
    await archiveTalent(
      buildTalentArchivePayload({
        candidateId: offer.candidateId,
        candidateName: offer.candidateName,
        applicationId: offer.applicationId,
        appliedJobTitle: offer.jobTitle,
        archiveReason: reason.trim() || 'Offer 结束后归档',
      }),
    )
    talentPoolMap.value[String(offer.candidateId)] = true
    ElMessage.success('已存入人才库')
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '存入人才库失败'))
  }
}

/* ---- 数据加载 ---- */
async function loadOffers() {
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await fetchOfferList({
      current: currentPage.value,
      size: pageSize.value,
      status: statusFilter.value,
      candidateName: searchKeyword.value || undefined,
      applicationId: fromApplicationId.value ?? undefined,
    })
    offers.value = data.records ?? []
    total.value = data.total ?? 0
    const ids = offers.value
      .map((o) => o.candidateId)
      .filter((id): id is number => id != null && id > 0)
    if (ids.length) {
      try {
        talentPoolMap.value = await fetchTalentPoolExistsBatch(ids)
      } catch {
        talentPoolMap.value = {}
      }
    } else {
      talentPoolMap.value = {}
    }
  } catch (err) {
    errorMsg.value = getErrorMessage(err, 'Offer 列表加载失败')
    offers.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.value = {
    baseSalary: '',
    annualSalary: '',
    positionLevel: '',
    expectedOnboardDate: '',
    probationMonths: '3',
    salaryRemark: '',
    remark: '',
  }
}

function openCreateDialog() {
  formMode.value = 'create'
  editingOfferId.value = null
  resetForm()
  formOpen.value = true
}

function openEditDialog(offer: OfferListVO) {
  formMode.value = 'edit'
  editingOfferId.value = offer.id
  form.value = {
    baseSalary: offer.baseSalary ? String(offer.baseSalary) : '',
    annualSalary: offer.annualSalary ? String(offer.annualSalary) : '',
    positionLevel: offer.positionLevel || '',
    expectedOnboardDate: '',
    probationMonths: '3',
    salaryRemark: '',
    remark: '',
  }
  formOpen.value = true
}

function closeForm() {
  formOpen.value = false
}

function buildSalaryPayload(): OfferUpdatePayload {
  const payload: OfferUpdatePayload = {}
  const base = Number(form.value.baseSalary)
  const annual = Number(form.value.annualSalary)
  if (Number.isFinite(base) && base > 0) payload.baseSalary = base
  if (Number.isFinite(annual) && annual > 0) payload.annualSalary = annual
  if (form.value.positionLevel.trim()) payload.positionLevel = form.value.positionLevel.trim()
  if (form.value.expectedOnboardDate) payload.expectedOnboardDate = form.value.expectedOnboardDate
  const probation = Number(form.value.probationMonths)
  if (Number.isFinite(probation) && probation > 0) payload.probationMonths = probation
  if (form.value.salaryRemark.trim()) payload.salaryRemark = form.value.salaryRemark.trim()
  if (form.value.remark.trim()) payload.remark = form.value.remark.trim()
  return payload
}

async function submitForm() {
  if (formSubmitting.value) return
  formSubmitting.value = true
  try {
    const salaryPayload = buildSalaryPayload()
    if (formMode.value === 'create') {
      if (!fromJobId.value || !fromCandidateId.value) {
        ElMessage.warning('缺少岗位或候选人信息，请从面试管理或简历详情进入')
        return
      }
      const name =
        (typeof route.query.candidateName === 'string' && route.query.candidateName.trim()) ||
        searchKeyword.value.trim() ||
        '候选人'
      const payload: OfferCreatePayload = {
        jobId: fromJobId.value,
        candidateId: fromCandidateId.value,
        candidateName: name,
        applicationId: fromApplicationId.value ?? undefined,
        ...salaryPayload,
      }
      await createOffer(payload)
      ElMessage.success('Offer 已创建，请确认信息后发放')
    } else if (editingOfferId.value) {
      await updateOffer(editingOfferId.value, salaryPayload)
      ElMessage.success('Offer 已更新')
    }
    formOpen.value = false
    await loadOffers()
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '保存失败'))
  } finally {
    formSubmitting.value = false
  }
}

async function handleIssue(offer: OfferListVO) {
  try {
    await ElMessageBox.confirm(
      `确定向「${offer.candidateName}」发放 Offer 吗？发放后候选人可在端内查看并响应。`,
      '发放确认',
      { confirmButtonText: '确认发放', cancelButtonText: '取消', type: 'success' },
    )
  } catch {
    return
  }
  try {
    await issueOffer(offer.id)
    ElMessage.success('Offer 已发放')
    await loadOffers()
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '发放失败'))
  }
}

async function tryOpenCreateFromInterview() {
  if (!fromApplicationId.value) return
  try {
    const existing = await fetchOfferByApplication(fromApplicationId.value)
    if (existing?.id) return
  } catch {
    /* 查询失败时仍允许手动创建 */
  }
  if (offers.value.length === 0 && fromJobId.value && fromCandidateId.value) {
    openCreateDialog()
  }
}

/* ---- 搜索防抖 ---- */
let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    loadOffers()
  }, 400)
}

function onStatusSelect(value: number | undefined) {
  statusFilter.value = value
  showStatusDropdown.value = false
  currentPage.value = 1
  loadOffers()
}

function prevPage() {
  if (currentPage.value > 1) {
    currentPage.value--
    loadOffers()
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    loadOffers()
  }
}

/* ---- 操作：撤回 Offer ---- */
async function handleRevoke(offer: OfferListVO) {
  try {
    await ElMessageBox.confirm(
      `确定要撤回候选人「${offer.candidateName}」的 Offer 吗？`,
      '撤回确认',
      { confirmButtonText: '确定撤回', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await revokeOffer(offer.id)
    ElMessage.success('Offer 已成功撤回')
    await loadOffers()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || '撤回失败，请稍后重试')
  }
}

/* ---- 初始化 ---- */
onMounted(async () => {
  const name = typeof route.query.candidateName === 'string' ? route.query.candidateName.trim() : ''
  if (name) {
    searchKeyword.value = name
  }
  await loadOffers()
  await tryOpenCreateFromInterview()
})

function isHighlighted(offer: OfferListVO) {
  return fromApplicationId.value != null && offer.applicationId === fromApplicationId.value
}
</script>

<template>
  <div data-cmp="OfferManagement" class="p-6 space-y-5" style="height: calc(100vh - 64px); overflow-y: auto">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-xl font-bold text-foreground">Offer管理</h1>
        <p class="text-sm text-muted-foreground mt-0.5">
          创建 Offer 并发放给候选人确认
          <span v-if="fromApplicationId" class="text-brand-green"> · 来自候选人详情（投递 #{{ fromApplicationId }}）</span>
        </p>
      </div>
      <button
        type="button"
        class="flex items-center gap-2 px-4 py-2.5 rounded-control gradient-blue text-white text-sm font-medium"
        @click="openCreateDialog"
      >
        <Plus :size="16" />
        <span>创建Offer</span>
      </button>
    </div>

    <p v-if="errorMsg" class="text-xs text-red-600">{{ errorMsg }}</p>

    <div class="flex gap-4">
      <div v-for="s in stats" :key="s.label" class="flex-1 bg-card rounded-xl p-4 shadow-card">
        <div :class="['text-2xl font-bold', s.color]">{{ s.value }}</div>
        <div class="text-xs text-muted-foreground mt-1">{{ s.label }}</div>
        <div v-if="s.change" class="text-xs text-muted-foreground mt-1">{{ s.change }} 接受率</div>
      </div>
    </div>

    <div class="flex items-center gap-3">
      <div class="flex items-center gap-2 bg-card rounded-lg px-3 py-2 border border-border flex-1 max-w-sm">
        <Search :size="14" class="text-muted-foreground" />
        <input
          v-model="searchKeyword"
          class="bg-transparent text-sm outline-none flex-1 placeholder:text-muted-foreground"
          placeholder="搜索候选人或岗位"
          @input="onSearchInput"
        />
      </div>
      <div class="relative">
        <button
          type="button"
          class="flex items-center gap-1.5 px-3 py-2 rounded-lg border border-border text-xs text-muted-foreground hover:text-foreground"
          @click="showStatusDropdown = !showStatusDropdown"
        >
          {{ statusOptions.find(o => o.value === statusFilter)?.label || '全部状态' }}
        </button>
        <div
          v-if="showStatusDropdown"
          class="absolute top-full left-0 mt-1 bg-card border border-border rounded-lg shadow-panel py-1 z-50 min-w-[140px]"
        >
          <button
            v-for="opt in statusOptions"
            :key="String(opt.value)"
            type="button"
            class="w-full text-left px-3 py-2 text-xs hover:bg-muted transition-colors"
            :class="statusFilter === opt.value ? 'text-brand-blue font-semibold' : 'text-muted-foreground'"
            @click="onStatusSelect(opt.value)"
          >
            {{ opt.label }}
          </button>
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <div class="bg-card shadow-card overflow-hidden">
      <!-- Loading -->
      <div v-if="loading" class="flex items-center justify-center py-16">
        <Loader2 :size="24" class="text-brand-blue animate-spin" />
        <span class="ml-2 text-sm text-muted-foreground">加载中...</span>
      </div>

      <!-- Empty -->
      <div v-else-if="offers.length === 0" class="flex flex-col items-center justify-center py-16 text-muted-foreground">
        <AlertCircle :size="32" class="mb-2 opacity-40" />
        <span class="text-sm">暂无 Offer 数据</span>
      </div>

      <!-- Table -->
      <table v-else class="w-full">
        <thead>
          <tr class="border-b border-border bg-muted/50">
            <th v-for="h in ['候选人', '应聘岗位', '部门', '薪资', '年薪', '职级', 'HR', '状态', '操作']" :key="h" class="text-left text-xs font-medium text-muted-foreground px-4 py-3">
              {{ h }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="offer in offers"
            :key="offer.id"
            class="border-b border-border hover:bg-muted/30 transition-colors"
            :class="isHighlighted(offer) ? 'bg-brand-green/5' : ''"
          >
            <td class="px-4 py-3">
              <div class="flex items-center gap-2">
                <div class="w-7 h-7 rounded-full gradient-blue flex items-center justify-center">
                  <User :size="12" class="text-white" />
                </div>
                <div>
                  <div class="text-sm font-medium text-foreground">{{ offer.candidateName }}</div>
                  <div class="text-xs text-muted-foreground">{{ formatDate(offer.createdAt) }}</div>
                </div>
              </div>
            </td>
            <td class="px-4 py-3 text-sm text-muted-foreground">{{ offer.jobTitle }}</td>
            <td class="px-4 py-3 text-sm text-muted-foreground">{{ offer.deptName || '-' }}</td>
            <td class="px-4 py-3">
              <span class="text-sm font-bold text-brand-blue">{{ formatSalary(offer.baseSalary) }}</span>
            </td>
            <td class="px-4 py-3 text-sm text-muted-foreground">{{ formatSalary(offer.annualSalary) }}</td>
            <td class="px-4 py-3 text-sm text-muted-foreground">{{ offer.positionLevel || '-' }}</td>
            <td class="px-4 py-3 text-sm text-muted-foreground">{{ offer.hrName || '-' }}</td>
            <td class="px-4 py-3">
              <div class="flex items-center gap-1.5">
                <component :is="getStatusStyle(offer.status).icon" :size="12" :class="getStatusStyle(offer.status).cls.split(' ')[1]" />
                <span :class="['text-xs px-2 py-0.5 rounded-full border', getStatusStyle(offer.status).cls]">{{ offer.statusText }}</span>
              </div>
            </td>
            <td class="px-4 py-3">
              <div class="flex items-center gap-1 flex-wrap">
                <!-- 编辑：待发放 -->
                <button
                  v-if="offer.status === OFFER_STATUS.APPROVED"
                  type="button"
                  title="编辑 Offer"
                  class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground hover:text-foreground"
                  @click="openEditDialog(offer)"
                >
                  <Edit :size="14" />
                </button>
                <!-- 发放：待发放 -->
                <button
                  v-if="offer.status === OFFER_STATUS.APPROVED"
                  type="button"
                  title="发放 Offer"
                  class="p-1.5 rounded-lg hover:bg-brand-tint text-brand-blue"
                  @click="handleIssue(offer)"
                >
                  <Send :size="14" />
                </button>
                <!-- 已发放：等待候选人确认 -->
                <span
                  v-if="offer.status === OFFER_STATUS.ISSUED"
                  class="text-[10px] text-brand-blue px-2 py-1 rounded-full bg-brand-tint border border-brand-border"
                  title="候选人需在端内确认"
                >
                  等待候选人确认
                </span>
                <!-- 存入人才库：候选人拒绝 / 已撤回 -->
                <button
                  v-if="canArchiveOffer(offer)"
                  type="button"
                  title="存入人才库"
                  class="p-1.5 rounded-lg hover:bg-purple-50 text-brand-purple hover:text-purple-700"
                  @click="handleArchiveToTalentPool(offer)"
                >
                  <Users :size="14" />
                </button>
                <!-- 撤回 -->
                <button
                  v-if="[OFFER_STATUS.APPROVED, OFFER_STATUS.ISSUED].includes(offer.status)"
                  type="button"
                  title="撤回 Offer"
                  class="p-1.5 rounded-lg hover:bg-orange-50 text-brand-orange hover:text-orange-700"
                  @click="handleRevoke(offer)"
                >
                  <Undo2 :size="14" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 分页控件 -->
      <div v-if="!loading && offers.length > 0" class="flex items-center justify-between px-4 py-3 border-t border-border">
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

    <!-- 创建 / 编辑 Offer -->
    <div
      v-if="formOpen"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      @click.self="closeForm"
    >
      <div class="bg-card rounded-2xl shadow-panel w-full max-w-md p-5 border border-border">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-sm font-bold text-foreground">
            {{ formMode === 'create' ? '创建 Offer' : '编辑 Offer' }}
          </h3>
          <button type="button" class="p-1 rounded-lg hover:bg-muted text-muted-foreground" @click="closeForm">
            <X :size="16" />
          </button>
        </div>
        <div v-if="fromApplicationId" class="text-xs text-muted-foreground mb-3">
          投递 #{{ fromApplicationId }}
          <span v-if="fromJobTitle"> · {{ fromJobTitle }}</span>
        </div>
        <div class="space-y-3">
          <label class="block text-xs">
            <span class="text-muted-foreground">月薪（元）</span>
            <input v-model="form.baseSalary" type="number" class="mt-1 w-full px-3 py-2 rounded-lg border border-border text-sm outline-none" placeholder="例如 25000" />
          </label>
          <label class="block text-xs">
            <span class="text-muted-foreground">年薪（元）</span>
            <input v-model="form.annualSalary" type="number" class="mt-1 w-full px-3 py-2 rounded-lg border border-border text-sm outline-none" placeholder="例如 300000" />
          </label>
          <label class="block text-xs">
            <span class="text-muted-foreground">职级</span>
            <input v-model="form.positionLevel" class="mt-1 w-full px-3 py-2 rounded-lg border border-border text-sm outline-none" placeholder="例如 P6" />
          </label>
          <label class="block text-xs">
            <span class="text-muted-foreground">预计入职日期</span>
            <input v-model="form.expectedOnboardDate" type="date" class="mt-1 w-full px-3 py-2 rounded-lg border border-border text-sm outline-none" />
          </label>
          <label class="block text-xs">
            <span class="text-muted-foreground">备注</span>
            <textarea v-model="form.remark" rows="2" class="mt-1 w-full px-3 py-2 rounded-lg border border-border text-sm outline-none resize-none" placeholder="可选" />
          </label>
        </div>
        <div class="flex gap-2 mt-5">
          <button type="button" class="flex-1 py-2 rounded-xl border border-border text-sm" @click="closeForm">取消</button>
          <button
            type="button"
            class="flex-1 py-2 rounded-xl gradient-blue text-white text-sm font-medium disabled:opacity-50"
            :disabled="formSubmitting"
            @click="submitForm"
          >
            {{ formSubmitting ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
