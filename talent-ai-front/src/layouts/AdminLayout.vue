<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter, RouterView } from 'vue-router'
import { Shield, Bot, Bell, User, LogOut, Users, Building2, BookOpen, ShieldAlert, LayoutDashboard, Megaphone, Library } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const displayName = computed(() => auth.userInfo?.nickname || '系统管理员')
const roleLabel = computed(() => (auth.userInfo?.userType === 4 ? 'Super Admin' : '管理员'))

const navItems = [
  { icon: LayoutDashboard, label: '数据大盘', path: '/admin/dashboard' },
  { icon: Megaphone, label: '广播中心', path: '/admin/broadcast' },
  { icon: Users, label: '账号管理', path: '/admin/accounts' },
  { icon: Shield, label: '权限管理', path: '/admin/permissions' },
  { icon: Bot, label: 'AI模型管理', path: '/admin/ai-models' },
  { icon: Library, label: 'AI知识库', path: '/admin/knowledge' },
  { icon: Building2, label: '企业资质审核', path: '/admin/enterprise-audit' },
  { icon: BookOpen, label: '数据字典', path: '/admin/data-dict' },
  { icon: ShieldAlert, label: '职位风控', path: '/admin/job-risk' },
]

const router = useRouter()
const route = useRoute()

function go(path: string) {
  router.push(path)
}

function isActive(path: string) {
  return route.path === path
}

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div data-cmp="AdminLayout" class="flex h-screen w-full min-w-0 overflow-hidden bg-background text-foreground">
    <aside
      class="sidebar-brand-gradient flex h-full w-52 shrink-0 flex-col overflow-hidden [&_svg]:stroke-[1.75]"
    >
      <div class="flex h-16 shrink-0 items-center gap-2.5 border-b border-sidebar-border px-3">
        <div class="flex h-9 w-9 items-center justify-center rounded-control shadow-card gradient-blue-purple">
          <Bot :size="17" class="text-primary-foreground" stroke-width="1.75" />
        </div>
        <div class="min-w-0">
          <div class="truncate text-xs font-semibold text-sidebar-foreground">TalentAI</div>
          <div class="truncate text-xs text-muted-foreground">系统管理</div>
        </div>
      </div>
      <div class="flex shrink-0 items-center gap-3 border-b border-sidebar-border px-3 py-4">
        <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-white/90 shadow-card gradient-purple">
          <Shield :size="15" class="text-primary-foreground" stroke-width="1.75" />
        </div>
        <div class="min-w-0">
          <div class="truncate text-xs font-semibold text-sidebar-foreground">{{ displayName }}</div>
          <div class="truncate text-xs text-muted-foreground">{{ roleLabel }}</div>
        </div>
      </div>
      <nav class="flex min-h-0 flex-1 flex-col overflow-hidden px-2 py-3">
        <button
          v-for="item in navItems"
          :key="item.path"
          type="button"
          class="flex w-full min-w-0 items-center gap-2.5 py-2.5 text-left text-sm transition-all duration-200"
          :class="
            isActive(item.path)
              ? 'rounded-control bg-white/95 px-2 font-medium text-[#3d8b7a] shadow-card'
              : 'px-2 text-muted-foreground hover:bg-white/55 hover:text-sidebar-foreground'
          "
          @click="go(item.path)"
        >
          <component :is="item.icon" :size="18" stroke-width="1.75" />
          <span class="truncate">{{ item.label }}</span>
        </button>
      </nav>
      <div class="shrink-0 border-t border-sidebar-border px-2 pb-4 pt-2">
        <button
          type="button"
          class="flex w-full min-w-0 items-center gap-2.5 px-2 py-2.5 text-left text-sm text-muted-foreground transition-colors hover:bg-white/55 hover:text-sidebar-foreground"
          @click="logout"
        >
          <LogOut :size="18" stroke-width="1.75" />
          <span>退出登录</span>
        </button>
      </div>
    </aside>
    <div class="flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden">
      <header
        class="flex h-16 shrink-0 items-center border-b border-border bg-card/90 px-4 shadow-card backdrop-blur-md sm:px-6"
      >
        <div class="text-sm font-semibold tracking-tight text-foreground">系统管理后台</div>
        <div class="ml-auto flex items-center gap-2 sm:gap-3">
          <button
            type="button"
            class="flex h-10 w-10 items-center justify-center rounded-control text-muted-foreground transition-colors hover:bg-muted"
          >
            <Bell :size="18" stroke-width="1.75" />
          </button>
          <div class="flex h-9 w-9 items-center justify-center rounded-full bg-white shadow-card gradient-purple">
            <User :size="15" class="text-primary-foreground" stroke-width="1.75" />
          </div>
        </div>
      </header>
      <div class="scrollbar-thin min-h-0 flex-1 overflow-auto" data-px-slot>
        <RouterView />
      </div>
    </div>
  </div>
</template>
