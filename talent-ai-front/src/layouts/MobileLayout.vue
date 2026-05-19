<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter, RouterView } from 'vue-router'
import { Home, FileText, Send, User } from 'lucide-vue-next'

const tabItems = [
  { icon: Home, label: '首页', path: '/candidate' },
  { icon: FileText, label: '我的简历', path: '/candidate/resume' },
  { icon: Send, label: '投递记录', path: '/candidate/applications' },
  { icon: User, label: '我的', path: '/candidate/profile' },
]

const router = useRouter()
const route = useRoute()

const showTabBar = computed(() => !['/candidate/job', '/candidate/apply'].includes(route.path))

function go(path: string) {
  router.push(path)
}

function isActive(path: string) {
  if (path === '/candidate') return route.path === '/candidate'
  return route.path.startsWith(path)
}
</script>

<template>
  <div
    data-cmp="MobileLayout"
    class="mx-auto flex h-[100dvh] w-full max-w-lg flex-col overflow-hidden bg-[#EBF4F0] text-foreground"
  >
    <main class="min-h-0 flex-1 overflow-hidden">
      <RouterView />
    </main>

    <nav
      v-if="showTabBar"
      class="shrink-0 border-t border-[#d4e8e0] bg-white/95 backdrop-blur-md"
      style="padding-bottom: env(safe-area-inset-bottom, 0px)"
    >
      <div class="flex h-14 items-stretch">
        <button
          v-for="item in tabItems"
          :key="item.path"
          type="button"
          class="flex flex-1 flex-col items-center justify-center gap-0.5 [&_svg]:stroke-[1.75]"
          @click="go(item.path)"
        >
          <component
            :is="item.icon"
            :size="22"
            :class="isActive(item.path) ? 'text-[#3d8b7a]' : 'text-slate-400'"
            stroke-width="1.75"
          />
          <span
            :class="[
              'text-[10px] leading-none',
              isActive(item.path) ? 'font-semibold text-[#3d8b7a]' : 'text-slate-400',
            ]"
          >
            {{ item.label }}
          </span>
        </button>
      </div>
    </nav>
  </div>
</template>
