<script setup lang="ts">
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/dialog/style/css'
import 'element-plus/es/components/input/style/css'
import { ref, computed, onMounted } from 'vue'
import { Plus, Search, Edit, Trash2, Shield, User, CheckCircle, XCircle, Users, Loader2 } from 'lucide-vue-next'
import { ElDialog, ElInput, ElMessage, ElMessageBox } from 'element-plus'
import {
  listRoles,
  listPermTree,
  getRolePermissions,
  saveRolePermissions,
  createRole,
  updateRole,
  deleteRole,
  type RoleItem,
  type PermModule,
} from '@/api/adminPermission'
import { getErrorMessage } from '@/utils/validators'

/* =================================================================
 *  响应式状态
 * ================================================================= */

const roles = ref<RoleItem[]>([])
const permModules = ref<PermModule[]>([])
const loading = ref(false)
const searchKeyword = ref('')

/** 当前选中角色 */
const selectedRoleId = ref<number | null>(null)
/** 当前选中角色已勾选的权限ID集合 */
const checkedPerms = ref<Set<number>>(new Set())
const permLoading = ref(false)
const saving = ref(false)

/* =================================================================
 *  计算属性
 * ================================================================= */

const filteredRoles = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return roles.value
  return roles.value.filter(
    r =>
      r.roleName.toLowerCase().includes(kw) ||
      r.roleCode.toLowerCase().includes(kw),
  )
})

const selectedRole = computed(() => roles.value.find(r => r.id === selectedRoleId.value) ?? null)

/** 权限项目总数 */
const totalPermCount = computed(() =>
  permModules.value.reduce((sum, m) => sum + m.perms.length, 0),
)

/** 系统用户总数 */
const totalUserCount = computed(() =>
  roles.value.reduce((sum, r) => sum + (r.userCount ?? 0), 0),
)

const roleColors = [
  'text-brand-red bg-red-50 border-red-200',
  'text-brand-blue bg-brand-tint border-brand-border',
  'text-brand-purple bg-brand-tint-2 border-brand-border',
  'text-brand-orange bg-orange-50 border-orange-200',
  'text-muted-foreground bg-muted border-border',
]

function roleColor(index: number): string {
  return roleColors[index % roleColors.length]
}

/* =================================================================
 *  数据加载
 * ================================================================= */

async function loadData() {
  loading.value = true
  try {
    const [roleList, tree] = await Promise.all([listRoles(), listPermTree()])
    roles.value = roleList ?? []
    permModules.value = tree ?? []
    // 默认选中第一个角色
    if (roles.value.length > 0) {
      await selectRole(roles.value[0].id)
    }
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '权限数据加载失败'))
  } finally {
    loading.value = false
  }
}

async function selectRole(roleId: number) {
  selectedRoleId.value = roleId
  permLoading.value = true
  try {
    const ids = await getRolePermissions(roleId)
    checkedPerms.value = new Set(ids ?? [])
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '角色权限加载失败'))
    checkedPerms.value = new Set()
  } finally {
    permLoading.value = false
  }
}

function isChecked(permId: number): boolean {
  return checkedPerms.value.has(permId)
}

function togglePerm(permId: number) {
  const next = new Set(checkedPerms.value)
  if (next.has(permId)) {
    next.delete(permId)
  } else {
    next.add(permId)
  }
  checkedPerms.value = next
}

async function handleSavePermissions() {
  if (selectedRoleId.value == null) return
  saving.value = true
  try {
    await saveRolePermissions(selectedRoleId.value, Array.from(checkedPerms.value))
    ElMessage.success('权限配置已保存')
    // 刷新角色列表权限数
    await refreshRoles()
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '权限保存失败'))
  } finally {
    saving.value = false
  }
}

async function refreshRoles() {
  try {
    roles.value = (await listRoles()) ?? []
  } catch {
    // 静默，保留当前列表
  }
}

/* =================================================================
 *  角色新建 / 编辑
 * ================================================================= */

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const form = ref<{ id: number | null; roleCode: string; roleName: string; description: string }>({
  id: null,
  roleCode: '',
  roleName: '',
  description: '',
})

function openCreateDialog() {
  dialogMode.value = 'create'
  form.value = { id: null, roleCode: '', roleName: '', description: '' }
  dialogVisible.value = true
}

function openEditDialog(role: RoleItem) {
  dialogMode.value = 'edit'
  form.value = {
    id: role.id,
    roleCode: role.roleCode,
    roleName: role.roleName,
    description: role.description ?? '',
  }
  dialogVisible.value = true
}

async function handleSubmitRole() {
  if (!form.value.roleCode.trim()) {
    ElMessage.warning('请填写角色编码')
    return
  }
  if (!form.value.roleName.trim()) {
    ElMessage.warning('请填写角色名称')
    return
  }
  submitting.value = true
  try {
    const payload = {
      roleCode: form.value.roleCode.trim(),
      roleName: form.value.roleName.trim(),
      description: form.value.description.trim(),
    }
    if (dialogMode.value === 'create') {
      await createRole(payload)
      ElMessage.success('角色已创建')
    } else if (form.value.id != null) {
      await updateRole(form.value.id, payload)
      ElMessage.success('角色已更新')
    }
    dialogVisible.value = false
    await refreshRoles()
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '保存失败'))
  } finally {
    submitting.value = false
  }
}

async function handleDeleteRole(role: RoleItem) {
  try {
    await ElMessageBox.confirm(
      `确定要删除角色「${role.roleName}」吗？该角色的权限关联将一并清除，此操作不可撤销。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteRole(role.id)
    ElMessage.success('角色已删除')
    if (selectedRoleId.value === role.id) {
      selectedRoleId.value = null
      checkedPerms.value = new Set()
    }
    await refreshRoles()
    if (selectedRoleId.value == null && roles.value.length > 0) {
      await selectRole(roles.value[0].id)
    }
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '删除失败'))
  }
}

onMounted(loadData)
</script>

<template>
  <div data-cmp="Permissions" class="p-8 h-full overflow-y-auto scrollbar-thin">
    <div class="max-w-6xl mx-auto">
      <div class="flex items-center justify-between mb-8">
        <div>
          <h1 class="text-2xl font-black text-foreground">权限管理</h1>
          <p class="text-muted-foreground mt-1">管理系统角色与功能权限配置</p>
        </div>
        <button
          type="button"
          class="flex items-center gap-2 px-4 py-2 gradient-blue rounded-xl text-white text-sm font-medium shadow-custom"
          @click="openCreateDialog"
        >
          <Plus :size="15" />
          <span>新建角色</span>
        </button>
      </div>

      <div class="flex gap-4 mb-8">
        <div
          v-for="s in [
            { label: '角色总数', value: roles.length, icon: Shield, cls: 'text-brand-blue bg-brand-tint' },
            { label: '系统用户', value: totalUserCount, icon: Users, cls: 'text-brand-purple bg-brand-tint-2' },
            { label: '权限项目', value: totalPermCount, icon: CheckCircle, cls: 'text-brand-green bg-green-50' },
          ]"
          :key="s.label"
          class="flex-1 bg-card p-5 shadow-card border border-border"
        >
          <div :class="['w-10 h-10 rounded-xl flex items-center justify-center mb-3', s.cls]">
            <component :is="s.icon" :size="18" />
          </div>
          <div class="text-2xl font-black text-foreground">{{ s.value }}</div>
          <div class="text-sm text-muted-foreground mt-1">{{ s.label }}</div>
        </div>
      </div>

      <div v-if="loading" class="flex items-center justify-center py-20 text-muted-foreground">
        <Loader2 :size="20" class="animate-spin text-brand-blue" />
        <span class="ml-2 text-sm">加载中…</span>
      </div>

      <div v-else class="flex gap-6">
        <div class="w-72 flex-shrink-0">
          <div class="bg-card shadow-card border border-border overflow-hidden">
            <div class="px-4 py-3 border-b border-border">
              <div class="flex items-center gap-2 px-3 py-2 bg-muted rounded-xl">
                <Search :size="14" class="text-muted-foreground" />
                <input
                  v-model="searchKeyword"
                  class="bg-transparent outline-none text-sm text-foreground placeholder:text-muted-foreground flex-1"
                  placeholder="搜索角色..."
                />
              </div>
            </div>
            <div>
              <div
                v-for="(role, i) in filteredRoles"
                :key="role.id"
                class="px-4 py-3 border-b border-border last:border-0 cursor-pointer transition-colors hover:bg-muted"
                :class="role.id === selectedRoleId ? 'bg-brand-tint/50' : ''"
                @click="selectRole(role.id)"
              >
                <div class="flex items-center justify-between mb-1">
                  <span class="text-sm font-semibold text-foreground">{{ role.roleName }}</span>
                  <div class="flex items-center gap-1.5">
                    <button type="button" class="p-1 rounded hover:bg-muted" title="编辑" @click.stop="openEditDialog(role)">
                      <Edit :size="12" class="text-muted-foreground" />
                    </button>
                    <button type="button" class="p-1 rounded hover:bg-muted" title="删除" @click.stop="handleDeleteRole(role)">
                      <Trash2 :size="12" class="text-muted-foreground" />
                    </button>
                  </div>
                </div>
                <div class="flex items-center gap-2">
                  <div class="flex items-center gap-1 text-xs text-muted-foreground">
                    <User :size="10" />
                    <span>{{ role.userCount }} 人</span>
                  </div>
                  <span :class="['text-xs px-2 py-0.5 rounded-full border', roleColor(i)]">{{ role.permCount }}项权限</span>
                </div>
              </div>
              <div v-if="filteredRoles.length === 0" class="px-4 py-8 text-center text-sm text-muted-foreground">
                暂无角色
              </div>
            </div>
          </div>
        </div>

        <div class="flex-1">
          <div class="bg-card shadow-card border border-border p-5">
            <div class="flex items-center gap-2 mb-5">
              <Shield :size="16" class="text-brand-blue" />
              <span class="text-base font-bold text-foreground">
                {{ selectedRole ? selectedRole.roleName : '请选择角色' }} — 权限配置
              </span>
              <Loader2 v-if="permLoading" :size="14" class="animate-spin text-brand-blue" />
            </div>
            <div v-if="!selectedRole" class="py-12 text-center text-sm text-muted-foreground">
              请从左侧选择一个角色以配置权限
            </div>
            <template v-else>
              <div class="space-y-5">
                <div v-for="mod in permModules" :key="mod.module">
                  <div class="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-2">
                    {{ mod.module }}
                  </div>
                  <div class="flex flex-wrap gap-2">
                    <div
                      v-for="perm in mod.perms"
                      :key="perm.id"
                                            class="flex items-center gap-1.5 px-3 py-2 rounded-xl border text-xs cursor-pointer transition-colors"
                      :class="
                        isChecked(perm.id)
                          ? 'bg-green-50 text-brand-green border-green-300 font-semibold'
                          : 'bg-gray-100 text-gray-400 border-gray-200 opacity-60 font-medium hover:opacity-100'
                      "
                      @click="togglePerm(perm.id)"
                    >
                      <CheckCircle v-if="isChecked(perm.id)" :size="12" />
                      <XCircle v-else :size="12" />
                      <span>{{ perm.permName }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div class="flex gap-3 mt-6 pt-5 border-t border-border">
                <button
                  type="button"
                  :disabled="saving"
                  class="flex-1 py-2.5 rounded-xl border border-border text-sm text-muted-foreground hover:bg-muted disabled:opacity-60"
                  @click="selectRole(selectedRole.id)"
                >
                  撤销修改
                </button>
                <button
                  type="button"
                  :disabled="saving"
                  class="flex-1 py-2.5 rounded-control gradient-blue text-white text-sm font-medium shadow-custom disabled:opacity-60"
                  @click="handleSavePermissions"
                >
                  {{ saving ? '保存中…' : '保存权限配置' }}
                </button>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>

    <ElDialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建角色' : '编辑角色'"
      width="460px"
    >
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-foreground mb-1.5">角色编码</label>
          <ElInput v-model="form.roleCode" placeholder="如 HR_ADMIN" :disabled="dialogMode === 'edit'" />
        </div>
        <div>
          <label class="block text-sm font-medium text-foreground mb-1.5">角色名称</label>
          <ElInput v-model="form.roleName" placeholder="如 HR管理员" />
        </div>
        <div>
          <label class="block text-sm font-medium text-foreground mb-1.5">角色描述</label>
          <ElInput v-model="form.description" type="textarea" :rows="3" placeholder="角色职责描述" />
        </div>
      </div>
      <template #footer>
        <button
          type="button"
          class="px-4 py-2 rounded-xl border border-border text-sm text-muted-foreground hover:bg-muted mr-2"
          @click="dialogVisible = false"
        >
          取消
        </button>
        <button
          type="button"
          :disabled="submitting"
          class="px-4 py-2 rounded-xl gradient-blue text-white text-sm font-medium shadow-custom disabled:opacity-60"
          @click="handleSubmitRole"
        >
          {{ submitting ? '保存中…' : '确定' }}
        </button>
      </template>
    </ElDialog>
  </div>
</template>
