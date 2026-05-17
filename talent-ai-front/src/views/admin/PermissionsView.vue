<script setup lang="ts">
import { Plus, Search, Edit, Trash2, Shield, User, CheckCircle, XCircle, Users } from 'lucide-vue-next'

const roles = [
  { name: '超级管理员', users: 2, desc: '系统全部权限，含AI模型管理与审计', color: 'text-brand-red bg-red-50 border-red-200', perms: 48 },
  { name: 'HR管理员', users: 12, desc: '招聘全流程管理，简历、面试、Offer', color: 'text-brand-blue bg-brand-tint border-brand-border', perms: 32 },
  { name: '面试官', users: 28, desc: '查看分配的面试任务，提交评价', color: 'text-brand-purple bg-brand-tint-2 border-brand-border', perms: 12 },
  { name: '部门负责人', users: 8, desc: '查看部门招聘进展，审批Offer', color: 'text-brand-orange bg-orange-50 border-orange-200', perms: 18 },
  { name: '只读用户', users: 5, desc: '仅可查看数据驾驶舱报表', color: 'text-muted-foreground bg-muted border-border', perms: 4 },
]

const permModules = [
  { module: '招聘管理', perms: ['查看岗位', '发布岗位', '编辑岗位', '关闭岗位'] },
  { module: '简历管理', perms: ['查看简历', '下载简历', '标注简历', '删除简历'] },
  { module: 'AI功能', perms: ['AI初筛', 'AI助手', 'AI面试官', 'AI报告导出'] },
  { module: '数据报表', perms: ['查看报表', '导出报表', '自定义报表'] },
  { module: '系统设置', perms: ['用户管理', '角色管理', 'AI模型配置', '系统审计'] },
]

const hrEnabled = new Set([
  '招聘管理-查看岗位',
  '招聘管理-发布岗位',
  '招聘管理-编辑岗位',
  '招聘管理-关闭岗位',
  '简历管理-查看简历',
  '简历管理-下载简历',
  '简历管理-标注简历',
  'AI功能-AI初筛',
  'AI功能-AI助手',
  'AI功能-AI面试官',
  '数据报表-查看报表',
  '数据报表-导出报表',
])

function isEnabled(mod: string, perm: string) {
  return hrEnabled.has(`${mod}-${perm}`)
}
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
        >
          <Plus :size="15" />
          <span>新建角色</span>
        </button>
      </div>

      <div class="flex gap-4 mb-8">
        <div
          v-for="s in [
            { label: '角色总数', value: roles.length, icon: Shield, cls: 'text-brand-blue bg-brand-tint' },
            {
              label: '系统用户',
              value: roles.reduce((a, r) => a + r.users, 0),
              icon: Users,
              cls: 'text-brand-purple bg-brand-tint-2',
            },
            { label: '权限项目', value: 48, icon: CheckCircle, cls: 'text-brand-green bg-green-50' },
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

      <div class="flex gap-6">
        <div class="w-72 flex-shrink-0">
          <div class="bg-card shadow-card border border-border overflow-hidden">
            <div class="px-4 py-3 border-b border-border">
              <div class="flex items-center gap-2 px-3 py-2 bg-muted rounded-xl">
                <Search :size="14" class="text-muted-foreground" />
                <input
                  class="bg-transparent outline-none text-sm text-foreground placeholder:text-muted-foreground flex-1"
                  placeholder="搜索角色..."
                />
              </div>
            </div>
            <div>
              <div
                v-for="(role, i) in roles"
                :key="role.name"
                class="px-4 py-3 border-b border-border last:border-0 cursor-pointer transition-colors hover:bg-muted"
                :class="i === 1 ? 'bg-brand-tint/50' : ''"
              >
                <div class="flex items-center justify-between mb-1">
                  <span class="text-sm font-semibold text-foreground">{{ role.name }}</span>
                  <div class="flex items-center gap-1.5">
                    <button type="button" class="p-1 rounded hover:bg-muted">
                      <Edit :size="12" class="text-muted-foreground" />
                    </button>
                    <button type="button" class="p-1 rounded hover:bg-muted">
                      <Trash2 :size="12" class="text-muted-foreground" />
                    </button>
                  </div>
                </div>
                <div class="flex items-center gap-2">
                  <div class="flex items-center gap-1 text-xs text-muted-foreground">
                    <User :size="10" />
                    <span>{{ role.users }} 人</span>
                  </div>
                  <span :class="['text-xs px-2 py-0.5 rounded-full border', role.color]">{{ role.perms }}项权限</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="flex-1">
          <div class="bg-card shadow-card border border-border p-5">
            <div class="flex items-center gap-2 mb-5">
              <Shield :size="16" class="text-brand-blue" />
              <span class="text-base font-bold text-foreground">HR管理员 — 权限配置</span>
            </div>
            <div class="space-y-5">
              <div v-for="mod in permModules" :key="mod.module">
                <div class="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-2">
                  {{ mod.module }}
                </div>
                <div class="flex flex-wrap gap-2">
                  <div
                    v-for="perm in mod.perms"
                    :key="perm"
                    class="flex items-center gap-1.5 px-3 py-2 rounded-xl border text-xs font-medium cursor-pointer transition-colors"
                    :class="
                      isEnabled(mod.module, perm)
                        ? 'bg-brand-tint text-brand-blue border-brand-border'
                        : 'bg-muted text-muted-foreground border-border hover:border-brand-blue/30'
                    "
                  >
                    <CheckCircle v-if="isEnabled(mod.module, perm)" :size="12" />
                    <XCircle v-else :size="12" />
                    <span>{{ perm }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div class="flex gap-3 mt-6 pt-5 border-t border-border">
              <button
                type="button"
                class="flex-1 py-2.5 rounded-xl border border-border text-sm text-muted-foreground hover:bg-muted"
              >
                重置为默认
              </button>
              <button
                type="button"
                class="flex-1 py-2.5 rounded-control gradient-blue text-white text-sm font-medium shadow-custom"
              >
                保存权限配置
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
