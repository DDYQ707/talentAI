<script setup lang="ts">
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/image-viewer/style/css'
import { ref, computed, onMounted } from 'vue'
import {
  Search,
  RefreshCw,
  Building2,
  Eye,
  CheckCircle,
  XCircle,
  Clock,
  X,
  FileText,
  AlertCircle,
  Loader2,
  ChevronLeft,
  ChevronRight,
  RotateCcw,
} from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ElImageViewer } from 'element-plus'
import {
  listEnterprises,
  approveEnterprise,
  rejectEnterprise,
  ENTERPRISE_STATUS,
  ENTERPRISE_STATUS_OPTIONS,
  type EnterpriseAudit,
} from '@/api/adminEnterprise'
import { getErrorMessage } from '@/utils/validators'

/* ---- 列表状态 ---- */
const loading = ref(false)
const records = ref<EnterpriseAudit[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

/* ---- 查询条件 ---- */
const searchKeyword = ref('')
const searchCreditCode = ref('')
const filterStatus = ref<number | undefined>(undefined)
const startDate = ref('')
const endDate = ref('')

/* ---- 抽屉状态 ---- */
const drawerOpen = ref(false)
const currentRecord = ref<EnterpriseAudit | null>(null)
const rejectReason = ref('')
const rejectError = ref('')
const operating = ref(false)

/* ---- 图片预览 ---- */
const showImageViewer = ref(false)
const previewImages = ref<string[]>([])

/* ---- 状态样式 ---- */
const statusStyleMap: Record<number, { cls: string; icon: typeof Clock }> = {
  [ENTERPRISE_STATUS.PENDING]:  { cls: 'bg-orange-50 text-brand-orange border-orange-200', icon: Clock },
  [ENTERPRISE_STATUS.APPROVED]: { cls: 'bg-green-50 text-brand-green border-green-200', icon: CheckCircle },
  [ENTERPRISE_STATUS.REJECTED]: { cls: 'bg-red-50 text-brand-red border-red-200', icon: XCircle },
}

function getStatusStyle(status: number) {
  return statusStyleMap[status] ?? { cls: 'bg-muted text-muted-foreground border-border', icon: FileText }
}

/* ---- 数据加载 ---- */
async function fetchList() {
  loading.value = true
  try {
    const data = await listEnterprises({
      page: page.value,
      size: pageSize.value,
      keyword: searchKeyword.value.trim() || undefined,
      creditCode: searchCreditCode.value.trim() || undefined,
      status: filterStatus.value,
      startDate: startDate.value || undefined,
      endDate: endDate.value || undefined,
    })
    records.value = data?.records ?? []
    total.value = Number(data?.total ?? 0)
  } catch (e) {
    records.value = []
    total.value = 0
    ElMessage.error(getErrorMessage(e, '加载企业列表失败'))
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchList()
}

function handleReset() {
  searchKeyword.value = ''
  searchCreditCode.value = ''
  filterStatus.value = undefined
  startDate.value = ''
  endDate.value = ''
  page.value = 1
  fetchList()
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

/* ---- 抽屉操作 ---- */
function openDrawer(row: EnterpriseAudit) {
  currentRecord.value = row
  rejectReason.value = ''
  rejectError.value = ''
  drawerOpen.value = true
}

function closeDrawer() {
  drawerOpen.value = false
  currentRecord.value = null
  rejectReason.value = ''
  rejectError.value = ''
}

/* ---- 图片预览 ---- */
function openImagePreview(url: string) {
  previewImages.value = [url]
  showImageViewer.value = true
}

function closeImageViewer() {
  showImageViewer.value = false
  previewImages.value = []
}

/* ---- 审批操作 ---- */
async function handleApprove() {
  if (!currentRecord.value) return
  try {
    await ElMessageBox.confirm(
      `确定核准通过「${currentRecord.value.companyName}」的入驻资质？`,
      '核准确认',
      { confirmButtonText: '确定通过', cancelButtonText: '取消', type: 'success' },
    )
  } catch { return }

  operating.value = true
  try {
    await approveEnterprise(currentRecord.value.id)
    ElMessage.success('已核准通过')
    closeDrawer()
    await fetchList()
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '审批操作失败'))
  } finally {
    operating.value = false
  }
}

async function handleReject() {
  if (!currentRecord.value) return
  rejectError.value = ''
  if (!rejectReason.value.trim()) {
    rejectError.value = '请填写驳回理由'
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定驳回「${currentRecord.value.companyName}」的入驻资质？`,
      '驳回确认',
      { confirmButtonText: '确定驳回', cancelButtonText: '取消', type: 'warning' },
    )
  } catch { return }

  operating.value = true
  try {
    await rejectEnterprise(currentRecord.value.id, rejectReason.value.trim())
    ElMessage.success('已驳回')
    closeDrawer()
    await fetchList()
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '驳回操作失败'))
  } finally {
    operating.value = false
  }
}

onMounted(fetchList)
</script>

<template>
  <div data-cmp="EnterpriseAudit" class="scrollbar-thin h-full overflow-y-auto p-6 sm:p-8">
    <div class="mx-auto max-w-6xl">
      <!-- 页面头部 -->
      <div class="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 class="text-2xl font-black text-foreground">企业资质审核</h1>
          <p class="mt-1 text-muted-foreground">处理 B 端企业入驻的资质流转与合规审查</p>
        </div>
      </div>

      <!-- 复合查询面板 -->
      <div class="mb-6 flex flex-col gap-3 rounded-2xl border border-border bg-card p-4 shadow-card">
        <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
          <div class="flex flex-1 items-center gap-2 rounded-xl bg-muted px-3 py-2">
            <Search :size="16" class="shrink-0 text-muted-foreground" />
            <input
              v-model="searchKeyword"
              type="search"
              class="min-w-0 flex-1 bg-transparent text-sm text-foreground outline-none placeholder:text-muted-foreground"
              placeholder="搜索企业名称…"
              @keyup.enter="handleSearch"
            />
          </div>
          <div class="flex flex-1 items-center gap-2 rounded-xl bg-muted px-3 py-2">
            <Building2 :size="16" class="shrink-0 text-muted-foreground" />
            <input
              v-model="searchCreditCode"
              type="search"
              class="min-w-0 flex-1 bg-transparent text-sm text-foreground outline-none placeholder:text-muted-foreground"
              placeholder="统一社会信用代码…"
              @keyup.enter="handleSearch"
            />
          </div>
          <select
            v-model="filterStatus"
            class="rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:border-[#85A185]"
            @change="handleSearch"
          >
            <option
              v-for="opt in ENTERPRISE_STATUS_OPTIONS"
              :key="String(opt.value)"
              :value="opt.value"
            >
              {{ opt.label }}
            </option>
          </select>
        </div>
        <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
          <div class="flex items-center gap-2 text-sm text-muted-foreground">
            <span class="shrink-0">提交时间</span>
            <input
              v-model="startDate"
              type="date"
              class="rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:border-[#85A185]"
            />
            <span>至</span>
            <input
              v-model="endDate"
              type="date"
              class="rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:border-[#85A185]"
            />
          </div>
          <div class="flex gap-2 sm:ml-auto">
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
              class="flex items-center justify-center gap-2 rounded-xl border border-border px-4 py-2 text-sm text-muted-foreground hover:bg-muted"
              :disabled="loading"
              @click="handleReset"
            >
              <RotateCcw :size="15" />
              重置
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
        </div>
      </div>

      <!-- 数据表格 -->
      <div class="overflow-hidden rounded-2xl border border-border bg-card shadow-card">
        <div class="overflow-x-auto">
          <table class="w-full min-w-[860px] text-left text-sm">
            <thead class="border-b border-border bg-muted/50 text-xs uppercase tracking-wide text-muted-foreground">
              <tr>
                <th class="px-4 py-3 font-semibold">企业名称</th>
                <th class="px-4 py-3 font-semibold">统一社会信用代码</th>
                <th class="px-4 py-3 font-semibold">法定代表人</th>
                <th class="px-4 py-3 font-semibold">注册资本</th>
                <th class="px-4 py-3 font-semibold">提交时间</th>
                <th class="px-4 py-3 font-semibold">审核状态</th>
                <th class="px-4 py-3 text-right font-semibold">操作</th>
              </tr>
            </thead>
            <tbody>
              <!-- Loading -->
              <tr v-if="loading">
                <td colspan="7" class="px-4 py-16 text-center">
                  <div class="flex items-center justify-center gap-2 text-muted-foreground">
                    <Loader2 :size="20" class="animate-spin text-brand-blue" />
                    <span>加载中…</span>
                  </div>
                </td>
              </tr>
              <!-- Empty -->
              <tr v-else-if="records.length === 0">
                <td colspan="7" class="px-4 py-16 text-center">
                  <div class="flex flex-col items-center gap-2 text-muted-foreground">
                    <AlertCircle :size="32" class="opacity-40" />
                    <span class="text-sm">暂无企业资质数据</span>
                  </div>
                </td>
              </tr>
              <!-- Data rows -->
              <tr
                v-for="row in records"
                v-else
                :key="row.id"
                class="border-b border-border last:border-0 transition-colors hover:bg-muted/30"
              >
                <td class="px-4 py-3 font-medium text-foreground">{{ row.companyName }}</td>
                <td class="px-4 py-3 font-mono text-xs text-muted-foreground">{{ row.creditCode }}</td>
                <td class="px-4 py-3 text-foreground">{{ row.legalPerson }}</td>
                <td class="px-4 py-3 text-muted-foreground">{{ row.registeredCapital || '—' }}</td>
                <td class="px-4 py-3 text-muted-foreground">{{ formatTime(row.submittedAt) }}</td>
                <td class="px-4 py-3">
                  <span
                    :class="['inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-xs font-medium', getStatusStyle(row.status).cls]"
                  >
                    <component :is="getStatusStyle(row.status).icon" :size="12" />
                    {{ row.statusLabel }}
                  </span>
                </td>
                <td class="px-4 py-3">
                  <div class="flex justify-end">
                    <button
                      type="button"
                      class="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium text-brand-blue transition-colors hover:bg-brand-tint"
                      @click="openDrawer(row)"
                    >
                      <Eye :size="14" />
                      审查
                    </button>
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

    <!-- 审查抽屉 -->
    <Teleport to="body">
      <div
        v-if="drawerOpen"
        class="fixed inset-0 z-50 flex justify-end bg-black/40 backdrop-blur-sm"
        @click.self="closeDrawer"
      >
        <div
          class="flex h-full w-full max-w-lg flex-col border-l border-border bg-card shadow-2xl"
          role="dialog"
          aria-modal="true"
          aria-labelledby="drawer-title"
        >
          <!-- Header -->
          <div class="flex shrink-0 items-center justify-between border-b border-border px-6 py-4">
            <div class="flex items-center gap-3">
              <div class="flex h-10 w-10 items-center justify-center rounded-xl gradient-blue">
                <Building2 :size="20" class="text-white" />
              </div>
              <div>
                <h2 id="drawer-title" class="text-lg font-bold text-foreground">企业资质审查</h2>
                <p class="text-xs text-muted-foreground">审核企业入驻资质材料</p>
              </div>
            </div>
            <button
              type="button"
              class="rounded-lg p-1.5 text-muted-foreground hover:bg-muted"
              @click="closeDrawer"
            >
              <X :size="18" />
            </button>
          </div>

          <!-- Body -->
          <div class="scrollbar-thin min-h-0 flex-1 overflow-y-auto px-6 py-5">
            <template v-if="currentRecord">
              <!-- 基本信息 -->
              <div class="mb-6 space-y-3">
                <h3 class="text-sm font-bold text-foreground">基本信息</h3>
                <div class="grid grid-cols-2 gap-3">
                  <div
                    v-for="info in [
                      { label: '企业名称', value: currentRecord.companyName },
                      { label: '信用代码', value: currentRecord.creditCode },
                      { label: '法定代表人', value: currentRecord.legalPerson },
                      { label: '注册资本', value: currentRecord.registeredCapital || '—' },
                      { label: '提交时间', value: formatTime(currentRecord.submittedAt) },
                      { label: '审核状态', value: currentRecord.statusLabel },
                    ]"
                    :key="info.label"
                    class="rounded-xl border border-border bg-muted/30 p-3"
                  >
                    <div class="text-xs text-muted-foreground">{{ info.label }}</div>
                    <div class="mt-0.5 text-sm font-medium text-foreground">{{ info.value }}</div>
                  </div>
                </div>
                <div class="rounded-xl border border-border bg-muted/30 p-3">
                  <div class="text-xs text-muted-foreground">经营范围</div>
                  <div class="mt-0.5 text-sm text-foreground leading-relaxed">
                    {{ currentRecord.businessScope || '—' }}
                  </div>
                </div>
              </div>

              <!-- 营业执照预览 -->
              <div class="mb-6">
                <h3 class="mb-3 text-sm font-bold text-foreground">营业执照</h3>
                <div
                  v-if="currentRecord.licenseUrl"
                  class="group relative cursor-pointer overflow-hidden rounded-xl border border-border"
                  @click="openImagePreview(currentRecord.licenseUrl)"
                >
                  <img
                    :src="currentRecord.licenseUrl"
                    alt="营业执照"
                    class="w-full object-contain transition-transform duration-200 group-hover:scale-105"
                    style="max-height: 300px"
                  />
                  <div class="absolute inset-0 flex items-center justify-center bg-black/0 transition-colors group-hover:bg-black/20">
                    <Eye :size="24" class="text-white opacity-0 transition-opacity group-hover:opacity-100" />
                  </div>
                </div>
                <div v-else class="flex items-center gap-2 rounded-xl border border-border bg-muted/30 px-4 py-8 text-muted-foreground">
                  <FileText :size="16" />
                  <span class="text-sm">暂未上传营业执照</span>
                </div>
              </div>

              <!-- 已驳回理由展示 -->
              <div v-if="currentRecord.status === 2 && currentRecord.rejectReason" class="mb-6">
                <h3 class="mb-2 text-sm font-bold text-foreground">驳回理由</h3>
                <div class="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-brand-red">
                  {{ currentRecord.rejectReason }}
                </div>
              </div>

              <!-- 待审核：驳回理由输入 -->
              <div v-if="currentRecord.status === 0" class="mb-4">
                <label class="mb-1.5 block text-xs font-medium text-foreground">驳回理由（驳回时必填）</label>
                <textarea
                  v-model="rejectReason"
                  rows="3"
                  class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185] focus:ring-2 focus:ring-[#85A185]/20"
                  placeholder="请输入驳回理由…"
                />
                <p v-if="rejectError" class="mt-1 text-xs font-medium text-red-600">{{ rejectError }}</p>
              </div>
            </template>
          </div>

          <!-- Footer -->
          <div
            v-if="currentRecord?.status === 0"
            class="flex shrink-0 gap-3 border-t border-border px-6 py-4"
          >
            <button
              type="button"
              class="flex flex-1 items-center justify-center gap-2 rounded-xl border border-red-200 bg-red-50 py-2.5 text-sm font-medium text-brand-red transition-colors hover:bg-red-100 disabled:opacity-60"
              :disabled="operating"
              @click="handleReject"
            >
              <XCircle :size="16" />
              驳回
            </button>
            <button
              type="button"
              class="flex flex-1 items-center justify-center gap-2 rounded-xl py-2.5 text-sm font-medium text-white gradient-blue shadow-custom disabled:opacity-60"
              :disabled="operating"
              @click="handleApprove"
            >
              <CheckCircle :size="16" />
              {{ operating ? '处理中…' : '核准通过' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Element Plus 图片预览 -->
    <Teleport to="body">
      <ElImageViewer
        v-if="showImageViewer"
        :url-list="previewImages"
        :initial-index="0"
        @close="closeImageViewer"
      />
    </Teleport>
  </div>
</template>
