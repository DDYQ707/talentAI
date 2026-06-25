<script setup lang="ts">
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import { ref, computed, onMounted, watch } from 'vue'
import {
  Search,
  Plus,
  Edit,
  Trash2,
  BookOpen,
  ChevronUp,
  ChevronDown,
  X,
  Tag,
  Loader2,
  AlertCircle,
  Settings,
  CheckCircle,
  XCircle,
} from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listDictTypes,
  createDictType,
  updateDictType,
  deleteDictType,
  listDictItems,
  createDictItem,
  updateDictItem,
  deleteDictItem,
  type DictType,
  type DictItem,
} from '@/api/adminDict'
import { getErrorMessage } from '@/utils/validators'

/* ---- 树状态管理 ---- */
const dictTypes = ref<DictType[]>([])
const typesLoading = ref(false)
const typeSearch = ref('')
const currentKey = ref<number | null>(null)      // 选中的字典类型 ID
const expandedKeys = ref<Set<number>>(new Set())  // 展开的类型 ID 集合（预留层级化）

const filteredTypes = computed(() => {
  const kw = typeSearch.value.trim().toLowerCase()
  if (!kw) return dictTypes.value
  return dictTypes.value.filter(
    t => t.name.toLowerCase().includes(kw) || t.code.toLowerCase().includes(kw),
  )
})

const currentType = computed(() =>
  dictTypes.value.find(t => t.id === currentKey.value) ?? null,
)

/* ---- 字典项状态 ---- */
const dictItems = ref<DictItem[]>([])
const itemsLoading = ref(false)

/* ---- 弹窗状态 ---- */
const typeDialogOpen = ref(false)
const typeDialogMode = ref<'create' | 'edit'>('create')
const typeForm = ref({ code: '', name: '', remark: '', status: 1 })
const typeEditingId = ref<number | null>(null)
const typeFormError = ref('')
const typeSaving = ref(false)

const itemDialogOpen = ref(false)
const itemDialogMode = ref<'create' | 'edit'>('create')
const itemForm = ref({ label: '', value: '', sort: 0, status: 1 })
const itemEditingId = ref<number | null>(null)
const itemFormError = ref('')
const itemSaving = ref(false)

/* ---- 加载字典类型（保留选中和展开状态） ---- */
async function fetchTypes() {
  typesLoading.value = true
  const savedKey = currentKey.value
  const savedExpanded = new Set(expandedKeys.value)
  try {
    const data = await listDictTypes()
    dictTypes.value = Array.isArray(data) ? data : []
    // 恢复选中状态
    if (savedKey !== null && dictTypes.value.some(t => t.id === savedKey)) {
      currentKey.value = savedKey
    } else if (dictTypes.value.length > 0) {
      currentKey.value = dictTypes.value[0].id
    } else {
      currentKey.value = null
    }
    // 恢复展开状态
    expandedKeys.value = savedExpanded
  } catch (e) {
    dictTypes.value = []
    ElMessage.error(getErrorMessage(e, '加载字典类型失败'))
  } finally {
    typesLoading.value = false
  }
}

/* ---- 加载字典项 ---- */
async function fetchItems() {
  if (currentKey.value == null) {
    dictItems.value = []
    return
  }
  itemsLoading.value = true
  try {
    const data = await listDictItems(currentKey.value)
    dictItems.value = Array.isArray(data) ? data : []
  } catch (e) {
    dictItems.value = []
    ElMessage.error(getErrorMessage(e, '加载字典项失败'))
  } finally {
    itemsLoading.value = false
  }
}

/* ---- 切换选中类型时自动加载右侧 ---- */
watch(currentKey, () => {
  fetchItems()
})

function selectType(id: number) {
  currentKey.value = id
}

/* ---- 字典类型 CRUD ---- */
function openTypeCreate() {
  typeDialogMode.value = 'create'
  typeEditingId.value = null
  typeForm.value = { code: '', name: '', remark: '', status: 1 }
  typeFormError.value = ''
  typeDialogOpen.value = true
}

function openTypeEdit(t: DictType) {
  typeDialogMode.value = 'edit'
  typeEditingId.value = t.id
  typeForm.value = { code: t.code, name: t.name, remark: t.remark ?? '', status: t.status }
  typeFormError.value = ''
  typeDialogOpen.value = true
}

function closeTypeDialog() {
  typeDialogOpen.value = false
  typeFormError.value = ''
}

async function submitTypeForm() {
  typeFormError.value = ''
  if (!typeForm.value.code.trim()) { typeFormError.value = '请输入字典编码'; return }
  if (!typeForm.value.name.trim()) { typeFormError.value = '请输入字典名称'; return }

  typeSaving.value = true
  try {
    if (typeDialogMode.value === 'create') {
      await createDictType(typeForm.value)
      ElMessage.success('字典类型创建成功')
    } else if (typeEditingId.value != null) {
      await updateDictType(typeEditingId.value, typeForm.value)
      ElMessage.success('字典类型更新成功')
    }
    closeTypeDialog()
    await fetchTypes()
  } catch (e) {
    typeFormError.value = getErrorMessage(e, '保存失败')
  } finally {
    typeSaving.value = false
  }
}

async function handleDeleteType(t: DictType) {
  try {
    await ElMessageBox.confirm(
      `确定删除字典类型「${t.name}」及其下所有字典项？此操作不可恢复。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch { return }

  try {
    await deleteDictType(t.id)
    ElMessage.success('已删除')
    if (currentKey.value === t.id) {
      currentKey.value = null
    }
    await fetchTypes()
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '删除失败'))
  }
}

/* ---- 字典项 CRUD ---- */
function openItemCreate() {
  if (currentKey.value == null) return
  itemDialogMode.value = 'create'
  itemEditingId.value = null
  itemForm.value = { label: '', value: '', sort: dictItems.value.length, status: 1 }
  itemFormError.value = ''
  itemDialogOpen.value = true
}

function openItemEdit(item: DictItem) {
  itemDialogMode.value = 'edit'
  itemEditingId.value = item.id
  itemForm.value = { label: item.label, value: item.value, sort: item.sort, status: item.status }
  itemFormError.value = ''
  itemDialogOpen.value = true
}

function closeItemDialog() {
  itemDialogOpen.value = false
  itemFormError.value = ''
}

async function submitItemForm() {
  itemFormError.value = ''
  if (!itemForm.value.label.trim()) { itemFormError.value = '请输入键名'; return }
  if (!itemForm.value.value.trim()) { itemFormError.value = '请输入键值'; return }
  if (currentKey.value == null) return

  itemSaving.value = true
  try {
    if (itemDialogMode.value === 'create') {
      await createDictItem({ ...itemForm.value, dictTypeId: currentKey.value })
      ElMessage.success('字典项创建成功')
    } else if (itemEditingId.value != null) {
      await updateDictItem(itemEditingId.value, { ...itemForm.value, dictTypeId: currentKey.value })
      ElMessage.success('字典项更新成功')
    }
    closeItemDialog()
    await fetchItems()
  } catch (e) {
    itemFormError.value = getErrorMessage(e, '保存失败')
  } finally {
    itemSaving.value = false
  }
}

async function handleDeleteItem(item: DictItem) {
  try {
    await ElMessageBox.confirm(
      `确定删除字典项「${item.label}」？`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch { return }

  try {
    await deleteDictItem(item.id)
    ElMessage.success('已删除')
    await fetchItems()
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '删除失败'))
  }
}

/* ---- 排序操作 ---- */
async function moveItem(item: DictItem, direction: 'up' | 'down') {
  const idx = dictItems.value.findIndex(i => i.id === item.id)
  if (idx < 0) return
  const swapIdx = direction === 'up' ? idx - 1 : idx + 1
  if (swapIdx < 0 || swapIdx >= dictItems.value.length) return

  const target = dictItems.value[swapIdx]
  try {
    // 交换 sort 值
    await updateDictItem(item.id, { sort: target.sort, dictTypeId: item.dictTypeId })
    await updateDictItem(target.id, { sort: item.sort, dictTypeId: target.dictTypeId })
    await fetchItems()
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '排序更新失败'))
  }
}

onMounted(async () => {
  await fetchTypes()
})
</script>

<template>
  <div data-cmp="DataDict" class="scrollbar-thin h-full overflow-y-auto p-6 sm:p-8">
    <div class="mx-auto max-w-6xl">
      <!-- 页面头部 -->
      <div class="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 class="text-2xl font-black text-foreground">数据字典</h1>
          <p class="mt-1 text-muted-foreground">动态管理系统运转所需的基础元数据配置</p>
        </div>
      </div>

      <!-- 主布局：左侧目录树 + 右侧明细 -->
      <div class="flex gap-6">
        <!-- 左侧目录树 -->
        <div class="w-72 shrink-0">
          <div class="overflow-hidden rounded-2xl border border-border bg-card shadow-card">
            <!-- 搜索 -->
            <div class="border-b border-border px-4 py-3">
              <div class="flex items-center gap-2 rounded-xl bg-muted px-3 py-2">
                <Search :size="14" class="text-muted-foreground" />
                <input
                  v-model="typeSearch"
                  class="min-w-0 flex-1 bg-transparent text-sm text-foreground outline-none placeholder:text-muted-foreground"
                  placeholder="搜索字典类型…"
                />
              </div>
            </div>
            <!-- 类型列表 -->
            <div class="scrollbar-thin max-h-[520px] overflow-y-auto">
              <!-- Loading -->
              <div v-if="typesLoading" class="flex items-center justify-center py-8 text-muted-foreground">
                <Loader2 :size="18" class="animate-spin" />
              </div>
              <!-- Empty -->
              <div v-else-if="filteredTypes.length === 0" class="px-4 py-8 text-center text-sm text-muted-foreground">
                暂无字典类型
              </div>
              <!-- List -->
              <div
                v-for="t in filteredTypes"
                v-else
                :key="t.id"
                class="cursor-pointer border-b border-border px-4 py-3 transition-colors last:border-0 hover:bg-muted"
                :class="currentKey === t.id ? 'bg-brand-tint/50' : ''"
                @click="selectType(t.id)"
              >
                <div class="flex items-center justify-between mb-0.5">
                  <span class="text-sm font-semibold text-foreground">{{ t.name }}</span>
                  <div class="flex items-center gap-1">
                    <button
                      type="button"
                      class="rounded p-1 hover:bg-muted"
                      title="编辑"
                      @click.stop="openTypeEdit(t)"
                    >
                      <Edit :size="12" class="text-muted-foreground" />
                    </button>
                    <button
                      type="button"
                      class="rounded p-1 hover:bg-red-50"
                      title="删除"
                      @click.stop="handleDeleteType(t)"
                    >
                      <Trash2 :size="12" class="text-muted-foreground" />
                    </button>
                  </div>
                </div>
                <div class="flex items-center gap-2">
                  <span class="font-mono text-xs text-muted-foreground">{{ t.code }}</span>
                  <span
                    class="rounded-full px-1.5 py-0.5 text-xs"
                    :class="t.status === 1 ? 'bg-green-50 text-brand-green' : 'bg-muted text-muted-foreground'"
                  >
                    {{ t.status === 1 ? '启用' : '禁用' }}
                  </span>
                </div>
              </div>
            </div>
            <!-- 新增按钮 -->
            <div class="border-t border-border px-4 py-3">
              <button
                type="button"
                class="flex w-full items-center justify-center gap-2 rounded-xl border border-dashed border-border py-2.5 text-sm text-muted-foreground transition-colors hover:border-brand-blue hover:text-brand-blue"
                @click="openTypeCreate"
              >
                <Plus :size="14" />
                新增字典类型
              </button>
            </div>
          </div>
        </div>

        <!-- 右侧明细表 -->
        <div class="min-w-0 flex-1">
          <div class="overflow-hidden rounded-2xl border border-border bg-card shadow-card">
            <!-- 头部信息 -->
            <div class="flex items-center justify-between border-b border-border px-5 py-4">
              <div class="flex items-center gap-2">
                <BookOpen :size="15" class="text-brand-blue" />
                <span v-if="currentType" class="text-sm font-bold text-foreground">
                  {{ currentType.name }}
                  <span class="ml-1 font-mono text-xs text-muted-foreground">({{ currentType.code }})</span>
                </span>
                <span v-else class="text-sm text-muted-foreground">请选择左侧字典类型</span>
              </div>
              <button
                v-if="currentType"
                type="button"
                class="flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-medium text-white gradient-blue shadow-custom"
                @click="openItemCreate"
              >
                <Plus :size="14" />
                新增字典项
              </button>
            </div>

            <!-- 表格 -->
            <div class="overflow-x-auto">
              <table class="w-full text-left text-sm">
                <thead class="border-b border-border bg-muted/50 text-xs uppercase tracking-wide text-muted-foreground">
                  <tr>
                    <th class="px-4 py-3 font-semibold">键名 (Label)</th>
                    <th class="px-4 py-3 font-semibold">键值 (Value)</th>
                    <th class="px-4 py-3 font-semibold">排序</th>
                    <th class="px-4 py-3 font-semibold">状态</th>
                    <th class="px-4 py-3 text-right font-semibold">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <!-- No type selected -->
                  <tr v-if="!currentType">
                    <td colspan="5" class="px-4 py-16 text-center text-muted-foreground">
                      <div class="flex flex-col items-center gap-2">
                        <Tag :size="32" class="opacity-40" />
                        <span class="text-sm">选择左侧字典类型查看字典项</span>
                      </div>
                    </td>
                  </tr>
                  <!-- Loading -->
                  <tr v-else-if="itemsLoading">
                    <td colspan="5" class="px-4 py-16 text-center">
                      <div class="flex items-center justify-center gap-2 text-muted-foreground">
                        <Loader2 :size="20" class="animate-spin text-brand-blue" />
                        <span>加载中…</span>
                      </div>
                    </td>
                  </tr>
                  <!-- Empty -->
                  <tr v-else-if="dictItems.length === 0">
                    <td colspan="5" class="px-4 py-16 text-center">
                      <div class="flex flex-col items-center gap-2 text-muted-foreground">
                        <AlertCircle :size="32" class="opacity-40" />
                        <span class="text-sm">暂无字典项，点击「新增字典项」添加</span>
                      </div>
                    </td>
                  </tr>
                  <!-- Data rows -->
                  <tr
                    v-for="(item, idx) in dictItems"
                    v-else
                    :key="item.id"
                    class="border-b border-border last:border-0 transition-colors hover:bg-muted/30"
                  >
                    <td class="px-4 py-3 font-medium text-foreground">{{ item.label }}</td>
                    <td class="px-4 py-3 font-mono text-xs text-muted-foreground">{{ item.value }}</td>
                    <td class="px-4 py-3">
                      <div class="flex items-center gap-1">
                        <span class="mr-1 text-sm text-muted-foreground">{{ item.sort }}</span>
                        <button
                          type="button"
                          class="rounded p-1 hover:bg-muted disabled:opacity-30"
                          :disabled="idx === 0"
                          title="上移"
                          @click="moveItem(item, 'up')"
                        >
                          <ChevronUp :size="14" class="text-muted-foreground" />
                        </button>
                        <button
                          type="button"
                          class="rounded p-1 hover:bg-muted disabled:opacity-30"
                          :disabled="idx === dictItems.length - 1"
                          title="下移"
                          @click="moveItem(item, 'down')"
                        >
                          <ChevronDown :size="14" class="text-muted-foreground" />
                        </button>
                      </div>
                    </td>
                    <td class="px-4 py-3">
                      <span
                        class="rounded-full px-2 py-0.5 text-xs font-medium"
                        :class="item.status === 1 ? 'bg-green-50 text-brand-green' : 'bg-muted text-muted-foreground'"
                      >
                        {{ item.status === 1 ? '启用' : '禁用' }}
                      </span>
                    </td>
                    <td class="px-4 py-3">
                      <div class="flex justify-end gap-1">
                        <button
                          type="button"
                          class="rounded-lg p-2 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                          title="编辑"
                          @click="openItemEdit(item)"
                        >
                          <Edit :size="16" />
                        </button>
                        <button
                          type="button"
                          class="rounded-lg p-2 text-muted-foreground transition-colors hover:bg-red-50 hover:text-red-600"
                          title="删除"
                          @click="handleDeleteItem(item)"
                        >
                          <Trash2 :size="16" />
                        </button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 字典类型编辑弹窗 -->
    <Teleport to="body">
      <div
        v-if="typeDialogOpen"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm"
        @click.self="closeTypeDialog"
      >
        <div
          class="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-2xl"
          role="dialog"
          aria-modal="true"
        >
          <div class="mb-5 flex items-start justify-between gap-3">
            <div class="flex items-center gap-2">
              <div class="flex h-10 w-10 items-center justify-center rounded-xl gradient-blue">
                <Settings :size="20" class="text-white" />
              </div>
              <div>
                <h2 class="text-lg font-bold text-foreground">
                  {{ typeDialogMode === 'create' ? '新建字典类型' : '编辑字典类型' }}
                </h2>
                <p class="text-xs text-muted-foreground">配置系统基础元数据类别</p>
              </div>
            </div>
            <button type="button" class="rounded-lg p-1.5 text-muted-foreground hover:bg-muted" @click="closeTypeDialog">
              <X :size="18" />
            </button>
          </div>

          <form class="space-y-4" @submit.prevent="submitTypeForm">
            <div>
              <label class="mb-1.5 block text-xs font-medium text-foreground">字典编码</label>
              <input
                v-model="typeForm.code"
                type="text"
                class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185] focus:ring-2 focus:ring-[#85A185]/20"
                placeholder="例如 job_category、industry"
                :disabled="typeDialogMode === 'edit'"
              />
            </div>
            <div>
              <label class="mb-1.5 block text-xs font-medium text-foreground">字典名称</label>
              <input
                v-model="typeForm.name"
                type="text"
                class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185] focus:ring-2 focus:ring-[#85A185]/20"
                placeholder="例如 职位分类、行业领域"
              />
            </div>
            <div>
              <label class="mb-1.5 block text-xs font-medium text-foreground">备注（可选）</label>
              <textarea
                v-model="typeForm.remark"
                rows="2"
                class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185] focus:ring-2 focus:ring-[#85A185]/20"
                placeholder="对该字典类型的说明…"
              />
            </div>
            <div>
              <label class="mb-1.5 block text-xs font-medium text-foreground">状态</label>
              <select
                v-model.number="typeForm.status"
                class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185]"
              >
                <option :value="1">启用</option>
                <option :value="0">禁用</option>
              </select>
            </div>

            <p v-if="typeFormError" class="text-sm font-medium text-red-600" role="alert">{{ typeFormError }}</p>

            <div class="flex gap-3 pt-2">
              <button
                type="button"
                class="flex-1 rounded-xl border border-border py-2.5 text-sm font-medium text-foreground hover:bg-muted"
                @click="closeTypeDialog"
              >
                取消
              </button>
              <button
                type="submit"
                class="flex-1 rounded-xl py-2.5 text-sm font-medium text-white gradient-blue disabled:opacity-60"
                :disabled="typeSaving"
              >
                {{ typeSaving ? '保存中…' : '保存' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- 字典项编辑弹窗 -->
    <Teleport to="body">
      <div
        v-if="itemDialogOpen"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm"
        @click.self="closeItemDialog"
      >
        <div
          class="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-2xl"
          role="dialog"
          aria-modal="true"
        >
          <div class="mb-5 flex items-start justify-between gap-3">
            <div class="flex items-center gap-2">
              <div class="flex h-10 w-10 items-center justify-center rounded-xl gradient-purple">
                <Tag :size="20" class="text-white" />
              </div>
              <div>
                <h2 class="text-lg font-bold text-foreground">
                  {{ itemDialogMode === 'create' ? '新建字典项' : '编辑字典项' }}
                </h2>
                <p class="text-xs text-muted-foreground">配置键值对数据项</p>
              </div>
            </div>
            <button type="button" class="rounded-lg p-1.5 text-muted-foreground hover:bg-muted" @click="closeItemDialog">
              <X :size="18" />
            </button>
          </div>

          <form class="space-y-4" @submit.prevent="submitItemForm">
            <div>
              <label class="mb-1.5 block text-xs font-medium text-foreground">键名 (Label)</label>
              <input
                v-model="itemForm.label"
                type="text"
                class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185] focus:ring-2 focus:ring-[#85A185]/20"
                placeholder="例如 前端工程师"
              />
            </div>
            <div>
              <label class="mb-1.5 block text-xs font-medium text-foreground">键值 (Value)</label>
              <input
                v-model="itemForm.value"
                type="text"
                class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185] focus:ring-2 focus:ring-[#85A185]/20"
                placeholder="例如 frontend_engineer"
              />
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="mb-1.5 block text-xs font-medium text-foreground">排序序号</label>
                <input
                  v-model.number="itemForm.sort"
                  type="number"
                  min="0"
                  class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185] focus:ring-2 focus:ring-[#85A185]/20"
                />
              </div>
              <div>
                <label class="mb-1.5 block text-xs font-medium text-foreground">状态</label>
                <select
                  v-model.number="itemForm.status"
                  class="w-full rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none focus:border-[#85A185]"
                >
                  <option :value="1">启用</option>
                  <option :value="0">禁用</option>
                </select>
              </div>
            </div>

            <p v-if="itemFormError" class="text-sm font-medium text-red-600" role="alert">{{ itemFormError }}</p>

            <div class="flex gap-3 pt-2">
              <button
                type="button"
                class="flex-1 rounded-xl border border-border py-2.5 text-sm font-medium text-foreground hover:bg-muted"
                @click="closeItemDialog"
              >
                取消
              </button>
              <button
                type="submit"
                class="flex-1 rounded-xl py-2.5 text-sm font-medium text-white gradient-blue disabled:opacity-60"
                :disabled="itemSaving"
              >
                {{ itemSaving ? '保存中…' : '保存' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>
  </div>
</template>
