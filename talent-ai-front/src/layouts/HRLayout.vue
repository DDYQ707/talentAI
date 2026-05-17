<script setup lang="ts">
import { ref } from 'vue'
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
  Settings,
  LogOut,
  User,
  Building2,
} from 'lucide-vue-next'

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

const bottomNavItems = [
  { icon: Settings, label: '系统管理', path: '/admin/permissions' },
  { icon: LogOut, label: '退出登录', path: '/login' },
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
</script>

<template>
  <div
    data-cmp="HRLayout"
    class="flex min-h-screen w-full min-w-0 bg-background text-foreground"
  >
    <aside
      class="sidebar-brand-gradient flex shrink-0 flex-col transition-[width] duration-300 ease-out [&_svg]:stroke-[1.75]"
      :class="collapsed ? 'w-16' : 'w-60'"
    >
      <div class="flex h-16 shrink-0 items-center border-b border-sidebar-border px-3 sm:px-4">
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
        class="flex shrink-0 items-center gap-3 border-b border-sidebar-border px-3 py-4 sm:px-4"
        :class="collapsed ? 'justify-center' : ''"
      >
        <div
          class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-white/90 shadow-card gradient-blue"
        >
          <User :size="15" class="text-primary-foreground" stroke-width="1.75" />
        </div>
        <div v-if="!collapsed" class="min-w-0">
          <div class="truncate text-xs font-semibold text-sidebar-foreground">张招聘</div>
          <div class="truncate text-xs text-muted-foreground">HR总监</div>
        </div>
      </div>

      <nav class="scrollbar-thin flex-1 overflow-y-auto py-3 [&_svg]:stroke-[1.75]">
        <button
          v-for="item in navItems"
          :key="item.path"
          type="button"
          class="flex w-full items-center gap-3 px-3 py-2.5 text-left text-sm transition-all duration-200 sm:px-4"
          :class="[
            isActive(item.path)
              ? 'mx-2 rounded-control bg-white/95 font-medium text-[#3d8b7a] shadow-card'
              : 'text-muted-foreground hover:bg-white/55 hover:text-sidebar-foreground',
            collapsed ? 'justify-center px-0' : '',
          ]"
          @click="go(item.path)"
        >
          <component :is="item.icon" :size="18" class="shrink-0" stroke-width="1.75" />
          <span v-if="!collapsed" class="truncate">{{ item.label }}</span>
        </button>
      </nav>

      <div class="border-t border-sidebar-border pb-4 pt-2">
        <button
          v-for="item in bottomNavItems"
          :key="item.path"
          type="button"
          class="flex w-full items-center gap-3 px-3 py-2.5 text-left text-sm text-muted-foreground transition-colors hover:bg-white/55 hover:text-sidebar-foreground sm:px-4"
          :class="collapsed ? 'justify-center' : ''"
          @click="go(item.path)"
        >
          <component :is="item.icon" :size="18" class="shrink-0" stroke-width="1.75" />
          <span v-if="!collapsed" class="truncate">{{ item.label }}</span>
        </button>
      </div>
    </aside>

    <div class="flex min-w-0 flex-1 flex-col">
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
            <span class="max-w-[140px] truncate">未来科技集团</span>
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

      <div class="scrollbar-thin flex-1 overflow-auto" data-px-slot>
        <RouterView />
      </div>
    </div>
  </div>
</template>
