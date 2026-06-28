<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter, RouterView } from 'vue-router'
import {
  LayoutDashboard,
  Bot,
  Briefcase,
  FileText,
  Calendar,
  Gift,
  Users,
  BarChart3,
  ChevronLeft,
  ChevronRight,
  Bell,
  Search,
  LogOut,
  User,
  Building2,
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import request from '@/utils/request'

const auth = useAuthStore()

const displayName = computed(() => auth.userInfo?.nickname || 'HR用户')
const roleLabel = computed(() => {
  const t = auth.userInfo?.userType
  if (t === 2) return 'HR'
  if (t === 3) return '面试官'
  if (t === 4) return '管理员'
  return '企业用户'
})

const navItems = [
  { icon: LayoutDashboard, label: '工作台', path: '/hr' },
  { icon: Bot, label: 'AI招聘助手', path: '/hr/ai-assistant' },
  { icon: Briefcase, label: '岗位管理', path: '/hr/jobs' },
  { icon: FileText, label: '简历管理', path: '/hr/resumes' },
  { icon: Calendar, label: '面试管理', path: '/hr/interviews' },
  { icon: Gift, label: 'Offer管理', path: '/hr/offers' },
  { icon: Users, label: '人才库', path: '/hr/talent-pool' },
  { icon: BarChart3, label: '数据驾驶舱', path: '/hr/dashboard' },
]

const collapsed = ref(false)
const router = useRouter()
const route = useRoute()

function go(path: string) {
  router.push(path)
}

function isActive(path: string) {
  return route.path === path
}

/**
 * HR 端退出登录 —— 无论后端接口成功/失败/宕机，
 * 都必须在 finally 中强制清理本地状态并跳转到登录页。
 */
async function handleLogout() {
  try {
    await request.post('/api/auth/logout')
  } catch {
    // 后端异常不阻塞退出流程
  } finally {
    // 强制清理：清除 Pinia 状态 + localStorage
    auth.logout()
    // 强制跳转登录页
    router.push('/login')
  }
}
</script>

<template>
  <div
    data-cmp="HRLayout"
    class="flex h-screen w-full min-w-0 overflow-hidden bg-background text-foreground"
  >
    <aside
      class="sidebar-brand-gradient flex h-full shrink-0 flex-col overflow-hidden transition-[width] duration-300 ease-out [&_svg]:stroke-[1.75]"
      :class="collapsed ? 'w-16' : 'w-52'"
    >
      <div class="flex h-16 shrink-0 items-center border-b border-sidebar-border px-3">
        <div class="flex min-w-0 items-center gap-2.5">
          <div
            class="flex h-9 w-9 shrink-0 items-center justify-center rounded-control shadow-card gradient-blue-purple"
          >
            <Bot :size="17" class="text-primary-foreground" stroke-width="1.75" />
          </div>
          <span
            class="truncate text-sm font-semibold tracking-tight text-sidebar-foreground transition-opacity duration-300"
            :class="collapsed ? 'sr-only opacity-0' : 'opacity-100'"
          >
            TalentAI
          </span>
        </div>
        <button
          type="button"
          class="ml-auto flex h-8 w-8 shrink-0 items-center justify-center rounded-control text-muted-foreground transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
          :aria-label="collapsed ? '展开侧栏' : '收起侧栏'"
          @click="collapsed = !collapsed"
        >
          <ChevronRight v-if="collapsed" :size="18" stroke-width="1.75" />
          <ChevronLeft v-else :size="18" stroke-width="1.75" />
        </button>
      </div>

      <div
        class="flex shrink-0 items-center gap-3 border-b border-sidebar-border px-3 py-4"
        :class="collapsed ? 'justify-center' : ''"
      >
        <div
          class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-white/90 shadow-card gradient-blue"
        >
          <User :size="15" class="text-primary-foreground" stroke-width="1.75" />
        </div>
        <div v-if="!collapsed" class="min-w-0">
          <div class="truncate text-xs font-semibold text-sidebar-foreground">{{ displayName }}</div>
          <div class="truncate text-xs text-muted-foreground">{{ roleLabel }}</div>
        </div>
      </div>

      <nav class="flex min-h-0 flex-1 flex-col overflow-hidden px-2 py-3 [&_svg]:stroke-[1.75]">
        <button
          v-for="item in navItems"
          :key="item.path"
          type="button"
          class="flex w-full min-w-0 items-center gap-2.5 py-2.5 text-left text-sm transition-all duration-200"
          :class="[
            isActive(item.path)
              ? 'rounded-control bg-white/95 px-2 font-medium text-[#3d8b7a] shadow-card'
              : 'px-2 text-muted-foreground hover:bg-white/55 hover:text-sidebar-foreground',
            collapsed ? 'justify-center px-0' : '',
          ]"
          @click="go(item.path)"
        >
          <component :is="item.icon" :size="18" class="shrink-0" stroke-width="1.75" />
          <span v-if="!collapsed" class="truncate">{{ item.label }}</span>
        </button>
      </nav>

      <div class="shrink-0 border-t border-sidebar-border px-2 pb-4 pt-2">
        <button
          type="button"
          class="flex w-full min-w-0 items-center gap-2.5 px-2 py-2.5 text-left text-sm text-muted-foreground transition-colors hover:bg-white/55 hover:text-sidebar-foreground"
          :class="collapsed ? 'justify-center px-0' : ''"
          @click="handleLogout"
        >
          <LogOut :size="18" class="shrink-0" stroke-width="1.75" />
          <span v-if="!collapsed" class="truncate">退出登录</span>
        </button>
      </div>
    </aside>

    <div class="flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden">
      <header
        class="flex h-16 shrink-0 items-center gap-3 border-b border-border bg-card/90 px-4 shadow-card backdrop-blur-md sm:gap-4 sm:px-6"
      >
        <div class="flex max-w-md flex-1 items-center gap-2">
          <div
            class="flex flex-1 items-center gap-2 rounded-control border border-border/80 bg-muted/50 px-3 py-2"
          >
            <Search :size="17" class="shrink-0 text-muted-foreground" stroke-width="1.75" />
            <input
              class="min-w-0 flex-1 bg-transparent text-sm text-foreground outline-none placeholder:text-muted-foreground"
              placeholder="搜索岗位、候选人、简历..."
            />
          </div>
        </div>
        <div class="ml-auto flex items-center gap-2 sm:gap-3">
          <div
            class="hidden items-center gap-1.5 rounded-full border border-border bg-brand-tint/80 px-3 py-1 text-xs font-medium text-[#3d8b7a] sm:inline-flex"
          >
            <Building2 :size="13" stroke-width="1.75" />
            <span class="max-w-[140px] truncate">TalentAI 企业端</span>
          </div>
          <button
            type="button"
            class="relative flex h-10 w-10 items-center justify-center rounded-control text-muted-foreground transition-colors hover:bg-muted"
          >
            <Bell :size="18" stroke-width="1.75" />
            <span class="absolute right-2 top-2 h-2 w-2 rounded-full bg-brand-red ring-2 ring-card" />
          </button>
          <div
            class="flex h-9 w-9 items-center justify-center rounded-full bg-white shadow-card gradient-blue"
          >
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
