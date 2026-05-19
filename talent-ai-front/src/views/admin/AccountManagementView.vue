<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Plus, Search, Edit, Trash2, RefreshCw, X, UserCog, Briefcase, Mic } from 'lucide-vue-next'
import {
  listAdminAccounts,
  createAdminAccount,
  updateAdminAccount,
  deleteAdminAccount,
  type AdminAccount,
} from '@/api/adminAccount'
import { getErrorMessage, validateAccount, validatePassword } from '@/utils/validators'

const USER_TYPE_HR = 2
const USER_TYPE_INTERVIEWER = 3

const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const filterType = ref<'all' | 'hr' | 'interviewer'>('all')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const records = ref<AdminAccount[]>([])

const dialogOpen = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const formError = ref('')
const listError = ref('')

const formAccount = ref('')
const formPassword = ref('')
const formUserType = ref<number>(USER_TYPE_HR)
const formNickname = ref('')
const formStatus = ref(1)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新建企业账号' : '编辑企业账号'))

function userTypeParam() {
  if (filterType.value === 'hr') return USER_TYPE_HR
  if (filterType.value === 'interviewer') return USER_TYPE_INTERVIEWER
  return undefined
}

async function fetchList() {
  loading.value = true
  listError.value = ''
  try {
    const pageData = await listAdminAccounts({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value.trim() || undefined,
      userType: userTypeParam(),
    })
    records.value = pageData?.records ?? []
    total.value = Number(pageData?.total ?? 0)
  } catch (e) {
    records.value = []
    total.value = 0
    listError.value = getErrorMessage(e, '加载账号列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchList()
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = null
  formAccount.value = ''
  formPassword.value = ''
  formUserType.value = USER_TYPE_HR
  formNickname.value = ''
  formStatus.value = 1
  formError.value = ''
  dialogOpen.value = true
}

function openEdit(row: AdminAccount) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  formAccount.value = row.account
  formPassword.value = ''
  formUserType.value = row.userType
  formNickname.value = row.nickname ?? ''
  formStatus.value = row.status
  formError.value = ''
  dialogOpen.value = true
}

function closeDialog() {
  dialogOpen.value = false
  formError.value = ''
}

function validateForm(): string | null {
  const accountErr = validateAccount(formAccount.value)
  if (accountErr) return accountErr
  if (dialogMode.value === 'create') {
    const pwdErr = validatePassword(formPassword.value)
    if (pwdErr) return pwdErr
  } else if (formPassword.value && validatePassword(formPassword.value)) {
    return validatePassword(formPassword.value)
  }
  if (formUserType.value !== USER_TYPE_HR && formUserType.value !== USER_TYPE_INTERVIEWER) {
    return '请选择 HR 或面试官'
  }
  return null
}

async function submitForm() {
  formError.value = ''
  const err = validateForm()
  if (err) {
    formError.value = err
    return
  }

  const payload = {
    account: formAccount.value.trim(),
    userType: formUserType.value,
    nickname: formNickname.value.trim() || undefined,
    status: formStatus.value,
    ...(formPassword.value ? { password: formPassword.value } : {}),
  }

  saving.value = true
  try {
    if (dialogMode.value === 'create') {
      await createAdminAccount({ ...payload, password: formPassword.value })
      page.value = 1
    } else if (editingId.value != null) {
      await updateAdminAccount(editingId.value, payload)
    }
    closeDialog()
    await fetchList()
  } catch (e) {
    formError.value = getErrorMessage(e, '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: AdminAccount) {
  const ok = window.confirm(`确定删除「${row.account}」（${row.userTypeLabel}）？此操作不可恢复。`)
  if (!ok) return
  try {
    await deleteAdminAccount(row.id)
    if (records.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await fetchList()
  } catch (e) {
    window.alert(getErrorMessage(e, '删除失败'))
  }
}

function formatTime(value?: string) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

function prevPage() {
  if (page.value > 1) {
    page.value -= 1
    fetchList()
  }
}

function nextPage() {
  if (page.value < totalPages.value) {
    page.value += 1
    fetchList()
  }
}

onMounted(fetchList)
</script>

<template>
  <div data-cmp="AccountManagement" class="scrollbar-thin h-full overflow-y-auto p-6 sm:p-8">
    <div class="mx-auto max-w-6xl">
      <div class="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 class="text-2xl font-black text-foreground">账号管理</h1>
          <p class="mt-1 text-muted-foreground">创建与管理 HR、面试官登录账号（手机号或邮箱 + 初始密码）</p>
        </div>
        <button
          type="button"
          class="gradient-blue flex items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium text-white shadow-custom"
          @click="openCreate"
        >
          <Plus :size="16" />
          <span>新建账号</span>
        </button>
      </div>

      <div class="mb-6 flex flex-col gap-3 rounded-2xl border border-border bg-card p-4 shadow-card sm:flex-row sm:items-center">
        <div class="flex flex-1 items-center gap-2 rounded-xl bg-muted px-3 py-2">
          <Search :size="16" class="shrink-0 text-muted-foreground" />
          <input
            v-model="keyword"
            type="search"
            class="min-w-0 flex-1 bg-transparent text-sm text-foreground outline-none placeholder:text-muted-foreground"
            placeholder="搜索手机号、邮箱或昵称…"
            @keyup.enter="handleSearch"
          />
        </div>
        <select
          v-model="filterType"
          class="rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:border-[#85A185]"
          @change="handleSearch"
        >
          <option value="all">全部类型</option>
          <option value="hr">仅 HR</option>
          <option value="interviewer">仅面试官</option>
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

      <div class="overflow-hidden rounded-2xl border border-border bg-card shadow-card">
        <div class="overflow-x-auto">
          <table class="w-full min-w-[720px] text-left text-sm">
            <thead class="border-b border-border bg-muted/50 text-xs uppercase tracking-wide text-muted-foreground">
              <tr>
                <th class="px-4 py-3 font-semibold">登录账号</th>
                <th class="px-4 py-3 font-semibold">类型</th>
                <th class="px-4 py-3 font-semibold">昵称</th>
                <th class="px-4 py-3 font-semibold">状态</th>
                <th class="px-4 py-3 font-semibold">创建时间</th>
                <th class="px-4 py-3 text-right font-semibold">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="6" class="px-4 py-12 text-center text-muted-foreground">加载中…</td>
              </tr>
              <tr v-else-if="records.length === 0">
                <td colspan="6" class="px-4 py-12 text-center text-muted-foreground">暂无账号，点击「新建账号」添加</td>
              </tr>
              <tr
                v-for="row in records"
                v-else
                :key="row.id"
                class="border-b border-border last:border-0 transition-colors hover:bg-muted/30"
              >
                <td class="px-4 py-3 font-medium text-foreground">{{ row.account }}</td>
                <td class="px-4 py-3">
                  <span
                    class="inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-xs font-medium"
                    :class="
                      row.userType === USER_TYPE_HR
                        ? 'border-brand-border bg-brand-tint text-brand-blue'
                        : 'border-brand-border bg-brand-tint-2 text-brand-purple'
                    "
                  >
                    <Briefcase v-if="row.userType === USER_TYPE_HR" :size="12" />
                    <Mic v-else :size="12" />
                    {{ row.userTypeLabel }}
                  </span>
                </td>
                <td class="px-4 py-3 text-foreground">{{ row.nickname || '—' }}</td>
                <td class="px-4 py-3">
                  <span
                    class="rounded-full px-2 py-0.5 text-xs font-medium"
                    :class="row.status === 1 ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-600'"
                  >
                    {{ row.statusLabel }}
                  </span>
                </td>
                <td class="px-4 py-3 text-muted-foreground">{{ formatTime(row.createdAt) }}</td>
                <td class="px-4 py-3">
                  <div class="flex justify-end gap-1">
                    <button
                      type="button"
                      class="rounded-lg p-2 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                      title="编辑"
                      @click="openEdit(row)"
                    >
                      <Edit :size="16" />
                    </button>
                    <button
                      type="button"
                      class="rounded-lg p-2 text-muted-foreground transition-colors hover:bg-red-50 hover:text-red-600"
                      title="删除"
                      @click="handleDelete(row)"
                    >
                      <Trash2 :size="16" />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div
          v-if="total > 0"
          class="flex flex-col items-center justify-between gap-3 border-t border-border px-4 py-3 text-sm text-muted-foreground sm:flex-row"
        >
          <span>共 {{ total }} 条，第 {{ page }} / {{ totalPages }} 页</span>
          <div class="flex gap-2">
            <button
              type="button"
              class="rounded-lg border border-border px-3 py-1.5 disabled:opacity-40"
              :disabled="page <= 1 || loading"
              @click="prevPage"
            >
              上一页
            </button>
            <button
              type="button"
              class="rounded-lg border border-border px-3 py-1.5 disabled:opacity-40"
              :disabled="page >= totalPages || loading"
              @click="nextPage"
            >
              下一页
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 新建/编辑弹窗 -->
    <Teleport to="body">
      <div
        v-if="dialogOpen"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm"
        @click.self="closeDialog"
      >
        <div
          class="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-2xl"
          role="dialog"
          aria-modal="true"
          :aria-labelledby="'account-dialog-title'"
        >
          <div class="mb-5 flex items-start justify-between gap-3">
            <div class="flex items-center gap-2">
              <div class="gradient-blue flex h-10 w-10 items-center justify-center rounded-xl">
                <UserCog :size="20" class="text-white" />
              </div>
              <div>
                <h2 id="account-dialog-title" class="text-lg font-bold text-foreground">{{ dialogTitle }}</h2>
                <p class="text-xs text-muted-foreground">账号为手机号或邮箱，用于对应门户登录</p>
              </div>
            </div>
            <button type="button" class="rounded-lg p-1.5 text-muted-foreground hover:bg-muted" @click="closeDialog">
              <X :size="18" />
            </button>
          </div>

          <form class="space-y-4" @submit.prevent="submitForm">
            <div>
              <label class="mb-1.5 block text-xs font-medium text-foreground">账号类型</label>
              <div class="grid grid-cols-2 gap-2">
                <label
                  class="flex cursor-pointer items-center justify-center gap-2 rounded-xl border py-2.5 text-sm transition-colors"
                  :class="
                    formUserType === USER_TYPE_HR
                      ? 'border-brand-blue bg-brand-tint font-medium text-brand-blue'
                      : 'border-border text-muted-foreground hover:bg-muted'
                  "
                >
                  <input v-model="formUserType" type="radio" class="sr-only" :value="USER_TYPE_HR" />
                  <Briefcase :size="16" />
                  HR
                </label>
                <label
                  class="flex cursor-pointer items-center justify-center gap-2 rounded-xl border py-2.5 text-sm transition-colors"
                  :class="
                    formUserType === USER_TYPE_INTERVIEWER
                      ? 'border-brand-purple bg-brand-tint-2 font-medium text-brand-purple'
                      : 'border-border text-muted-foreground hover:bg-muted'
                  "
                >
                  <input v-model="formUserType" type="radio" class="sr-only" :value="USER_TYPE_INTERVIEWER" />
                  <Mic :size="16" />
                  面试官
                </label>
              </div>
            </div>

            <div>
              <label class="mb-1.5 block text-xs font-medium text-foreground">手机号 / 邮箱</label>
              <input
                v-model="formAccount"
                type="text"
                class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185] focus:ring-2 focus:ring-[#85A185]/20"
                placeholder="例如 13800138000 或 hr@company.com"
                autocomplete="off"
              />
            </div>

            <div>
              <label class="mb-1.5 block text-xs font-medium text-foreground">
                {{ dialogMode === 'create' ? '初始密码' : '新密码（留空不修改）' }}
              </label>
              <input
                v-model="formPassword"
                type="password"
                class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185] focus:ring-2 focus:ring-[#85A185]/20"
                :placeholder="dialogMode === 'create' ? '至少 6 位' : '不修改请留空'"
                autocomplete="new-password"
              />
            </div>

            <div>
              <label class="mb-1.5 block text-xs font-medium text-foreground">昵称（可选）</label>
              <input
                v-model="formNickname"
                type="text"
                class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185] focus:ring-2 focus:ring-[#85A185]/20"
                placeholder="默认同账号"
              />
            </div>

            <div v-if="dialogMode === 'edit'">
              <label class="mb-1.5 block text-xs font-medium text-foreground">账号状态</label>
              <select
                v-model.number="formStatus"
                class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185]"
              >
                <option :value="1">正常</option>
                <option :value="0">禁用</option>
              </select>
            </div>

            <p v-if="formError" class="text-sm font-medium text-red-600" role="alert">{{ formError }}</p>

            <div class="flex gap-3 pt-2">
              <button
                type="button"
                class="flex-1 rounded-xl border border-border py-2.5 text-sm font-medium text-foreground hover:bg-muted"
                @click="closeDialog"
              >
                取消
              </button>
              <button
                type="submit"
                class="gradient-blue flex-1 rounded-xl py-2.5 text-sm font-medium text-white disabled:opacity-60"
                :disabled="saving"
              >
                {{ saving ? '保存中…' : '保存' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>
  </div>
</template>
