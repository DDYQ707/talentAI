<script setup lang="ts">
import 'element-plus/es/components/message/style/css';
import 'element-plus/es/components/message-box/style/css';
import { ref, reactive, computed, onMounted, watch } from 'vue'
import {
  Plus,
  Search,
  User,
  CheckCircle,
  Clock,
  XCircle,
  Send,
  FileText,
  Sparkles,
  TrendingUp,
  Edit,
  Eye,
  ChevronLeft,
  ChevronRight,
  Loader2,
  AlertCircle,
  Ban,
  MailCheck,
  MailX,
  Undo2,
  ThumbsUp,
  ThumbsDown,
} from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchOfferList,
  OFFER_STATUS,
  type OfferListVO,
  revokeOffer,
  acceptOffer,
  rejectOffer,
  approveOfferApproval,
  rejectOfferApproval,
} from '@/api/offer'

/* ---- 响应式状态 ---- */
const offers = ref<OfferListVO[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const statusFilter = ref<number | undefined>(undefined)

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
  { label: '待审批', value: OFFER_STATUS.PENDING },
  { label: '审批中', value: OFFER_STATUS.APPROVING },
  { label: '已通过', value: OFFER_STATUS.APPROVED },
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
    label: '审批中',
    value: offers.value.filter(o => o.status === OFFER_STATUS.APPROVING || o.status === OFFER_STATUS.PENDING).length,
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

/* ---- 数据加载 ---- */
async function loadOffers() {
  loading.value = true
  try {
    const data = await fetchOfferList({
      current: currentPage.value,
      size: pageSize.value,
      status: statusFilter.value,
      candidateName: searchKeyword.value || undefined,
    })
    offers.value = data.records ?? []
    total.value = data.total ?? 0
  } catch (err) {
    console.error('加载 Offer 列表失败:', err)
    offers.value = []
    total.value = 0
  } finally {
    loading.value = false
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
    return // 用户点了取消
  }
  try {
    await revokeOffer(offer.id)
    ElMessage.success('Offer 已成功撤回')
    await loadOffers()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || '撤回失败，请稍后重试')
  }
}

/* ---- 操作：候选人接受 ---- */
async function handleAccept(offer: OfferListVO) {
  try {
    await ElMessageBox.confirm(
      `确认标记候选人「${offer.candidateName}」已接受 Offer？`,
      '接受确认',
      { confirmButtonText: '确认接受', cancelButtonText: '取消', type: 'success' },
    )
  } catch {
    return
  }
  try {
    await acceptOffer(offer.id)
    ElMessage.success('已标记为候选人接受')
    await loadOffers()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || '操作失败，请稍后重试')
  }
}

/* ---- 操作：候选人拒绝 ---- */
async function handleReject(offer: OfferListVO) {
  try {
    await ElMessageBox.confirm(
      `确认标记候选人「${offer.candidateName}」已拒绝 Offer？`,
      '拒绝确认',
      { confirmButtonText: '确认拒绝', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await rejectOffer(offer.id)
    ElMessage.success('已标记为候选人拒绝')
    await loadOffers()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || '操作失败，请稍后重试')
  }
}

/* ---- 操作：审批通过 ---- */
async function handleApprove(offer: OfferListVO) {
  try {
    await ElMessageBox.confirm(
      `确定审批通过候选人「${offer.candidateName}」的 Offer？`,
      '审批通过确认',
      { confirmButtonText: '通过', cancelButtonText: '取消', type: 'success' },
    )
  } catch {
    return
  }
  try {
    await approveOfferApproval(offer.id)
    ElMessage.success('审批已通过')
    await loadOffers()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || '审批操作失败，请稍后重试')
  }
}

/* ---- 操作：审批驳回 ---- */
async function handleRejectApproval(offer: OfferListVO) {
  try {
    await ElMessageBox.confirm(
      `确定驳回候选人「${offer.candidateName}」的 Offer 审批？`,
      '审批驳回确认',
      { confirmButtonText: '确定驳回', cancelButtonText: '取消', type: 'error' },
    )
  } catch {
    return
  }
  try {
    await rejectOfferApproval(offer.id)
    ElMessage.success('审批已驳回')
    await loadOffers()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || '驳回操作失败，请稍后重试')
  }
}

/* ---- 初始化 ---- */
onMounted(() => {
  loadOffers()
})
</script>

<template>
  <div data-cmp="OfferManagement" class="p-6 space-y-5" style="height: calc(100vh - 64px); overflow-y: auto">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-xl font-bold text-foreground">Offer管理</h1>
        <p class="text-sm text-muted-foreground mt-0.5">管理所有候选人的Offer发放与审批</p>
      </div>
      <button type="button" class="flex items-center gap-2 px-4 py-2.5 rounded-control gradient-blue text-white text-sm font-medium">
        <Plus :size="16" />
        <span>创建Offer</span>
      </button>
    </div>

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
      <div class="ml-auto flex items-center gap-2">
        <button type="button" class="flex items-center gap-1.5 px-3 py-2 rounded-lg gradient-purple text-white text-xs">
          <Sparkles :size="12" />
          <span>AI生成Offer</span>
        </button>
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
          <tr v-for="offer in offers" :key="offer.id" class="border-b border-border hover:bg-muted/30 transition-colors">
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
              <div class="flex items-center gap-1">
                <!-- 查看（始终可用） -->
                <button type="button" title="查看" class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground hover:text-foreground">
                  <Eye :size="14" />
                </button>
                <!-- 审批通过：待审批 / 审批中 -->
                <button
                  v-if="offer.status === OFFER_STATUS.PENDING || offer.status === OFFER_STATUS.APPROVING"
                  type="button"
                  title="审批通过"
                  class="p-1.5 rounded-lg hover:bg-green-50 text-brand-green hover:text-green-700"
                  @click="handleApprove(offer)"
                >
                  <ThumbsUp :size="14" />
                </button>
                <!-- 审批驳回：待审批 / 审批中 -->
                <button
                  v-if="offer.status === OFFER_STATUS.PENDING || offer.status === OFFER_STATUS.APPROVING"
                  type="button"
                  title="审批驳回"
                  class="p-1.5 rounded-lg hover:bg-red-50 text-brand-red hover:text-red-700"
                  @click="handleRejectApproval(offer)"
                >
                  <ThumbsDown :size="14" />
                </button>
                <!-- 候选人接受：已发放 -->
                <button
                  v-if="offer.status === OFFER_STATUS.ISSUED"
                  type="button"
                  title="候选人接受"
                  class="p-1.5 rounded-lg hover:bg-green-50 text-brand-green hover:text-green-700"
                  @click="handleAccept(offer)"
                >
                  <MailCheck :size="14" />
                </button>
                <!-- 候选人拒绝：已发放 -->
                <button
                  v-if="offer.status === OFFER_STATUS.ISSUED"
                  type="button"
                  title="候选人拒绝"
                  class="p-1.5 rounded-lg hover:bg-red-50 text-brand-red hover:text-red-700"
                  @click="handleReject(offer)"
                >
                  <MailX :size="14" />
                </button>
                <!-- 撤回：待审批 / 审批中 / 已通过 / 已发放 -->
                <button
                  v-if="[OFFER_STATUS.PENDING, OFFER_STATUS.APPROVING, OFFER_STATUS.APPROVED, OFFER_STATUS.ISSUED].includes(offer.status)"
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

    <div class="bg-card shadow-card p-5">
      <div class="flex items-center gap-2 mb-4">
        <div class="w-7 h-7 rounded-lg gradient-purple flex items-center justify-center">
          <Sparkles :size="14" class="text-white" />
        </div>
        <h3 class="text-sm font-semibold text-foreground">AI Offer助手</h3>
      </div>
      <div class="flex gap-4">
        <div
          v-for="item in [
            { title: '薪资建议', desc: '基于市场数据，自动生成最具竞争力的薪资方案', action: '生成建议' },
            { title: 'Offer信件生成', desc: 'AI一键生成个性化Offer信件，提升候选人体验', action: '立即生成' },
            { title: '风险预测', desc: '分析候选人接受/拒绝概率，提前做好备选方案', action: '查看分析' },
          ]"
          :key="item.title"
          class="flex-1 bg-accent rounded-xl p-4 border border-brand-border/50"
        >
          <div class="flex items-center gap-2 mb-2">
            <TrendingUp :size="14" class="text-brand-purple" />
            <span class="text-xs font-semibold text-foreground">{{ item.title }}</span>
          </div>
          <p class="text-xs text-muted-foreground leading-relaxed mb-3">{{ item.desc }}</p>
          <button type="button" class="text-xs text-brand-purple font-medium hover:underline">{{ item.action }} →</button>
        </div>
      </div>
    </div>
  </div>
</template>
