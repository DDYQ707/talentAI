<script setup lang="ts">
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

function go(path: string) {
  router.push(path)
}

function isActive(path: string) {
  return route.path === path
}
</script>

<template>
  <div
    data-cmp="MobileLayout"
    class="flex min-h-screen w-full min-w-0 flex-col bg-background text-foreground"
  >
    <div class="flex flex-1 flex-col items-stretch justify-start gap-6 px-4 py-6 sm:flex-row sm:items-start sm:justify-center sm:gap-8 sm:px-6 sm:py-8 lg:px-8">
      <div class="w-full max-w-xs shrink-0 pt-2 sm:w-72 sm:pt-12">
        <div class="mb-3 text-xs font-medium uppercase tracking-wider text-muted-foreground">候选人端</div>
        <h2 class="mb-2 text-2xl font-bold tracking-tight text-foreground">求职平台</h2>
        <p class="mb-6 text-sm leading-relaxed text-muted-foreground">
          AI 赋能的现代求职体验，智能匹配、简历优化、全程跟踪
        </p>
        <div class="space-y-2">
          <button
            v-for="item in tabItems"
            :key="item.path"
            type="button"
            class="flex w-full items-center gap-3 rounded-control px-4 py-3 text-sm transition-all [&_svg]:stroke-[1.75]"
            :class="
              isActive(item.path)
                ? 'bg-[#3d8b7a] font-medium text-white shadow-card'
                : 'border border-border/80 bg-card text-foreground shadow-card hover:bg-brand-tint/80'
            "
            @click="go(item.path)"
          >
            <component :is="item.icon" :size="16" stroke-width="1.75" />
            <span>{{ item.label }}</span>
          </button>
        </div>
      </div>

      <div class="mx-auto flex w-full max-w-[390px] shrink-0 justify-center sm:mx-0">
        <div
          class="relative overflow-hidden rounded-[28px] border-2 border-border bg-card shadow-panel"
          style="width: min(100%, 390px); min-height: min(844px, 85vh); box-shadow: 0 8px 24px rgba(61, 139, 125, 0.12), 0 0 0 6px rgba(61, 139, 125, 0.06)"
        >
          <div class="flex h-10 items-center justify-between bg-card px-5">
            <span class="text-xs font-semibold text-foreground">9:41</span>
            <div class="h-5 w-24 rounded-full bg-muted" />
            <div class="flex items-center gap-1">
              <div class="h-2 w-3 rounded-sm border border-border">
                <div class="h-full w-2 rounded-sm bg-[#5a8a82]" />
              </div>
            </div>
          </div>
          <div class="scrollbar-thin overflow-y-auto" style="height: calc(min(844px, 85vh) - 40px - 60px)" data-px-slot>
            <RouterView />
          </div>
          <div
            class="absolute bottom-0 left-0 right-0 flex h-16 items-center border-t border-border bg-card/95 backdrop-blur-sm"
          >
            <button
              v-for="item in tabItems"
              :key="item.path + '-tab'"
              type="button"
              class="flex flex-1 flex-col items-center gap-1 py-2 [&_svg]:stroke-[1.75]"
              @click="go(item.path)"
            >
              <component
                :is="item.icon"
                :size="20"
                :class="isActive(item.path) ? 'text-[#3d8b7a]' : 'text-muted-foreground'"
                stroke-width="1.75"
              />
              <span
                :class="[
                  'text-xs',
                  isActive(item.path) ? 'font-medium text-[#3d8b7a]' : 'text-muted-foreground',
                ]"
              >
                {{ item.label }}
              </span>
            </button>
          </div>
        </div>
      </div>

      <div class="w-full max-w-xs shrink-0 pt-2 sm:w-72 sm:pt-12">
        <div class="mb-3 text-xs font-medium uppercase tracking-wider text-muted-foreground">功能亮点</div>
        <div class="space-y-3">
          <div
            v-for="f in [
              { title: 'AI智能匹配', desc: '基于画像分析，精准推荐岗位' },
              { title: '简历优化建议', desc: 'AI解读简历，提升投递成功率' },
              { title: '全程状态追踪', desc: '从投递到入职，实时通知' },
              { title: '人才画像图谱', desc: '雷达图展示多维度能力' },
            ]"
            :key="f.title"
            class="border border-border/60 bg-card p-4 shadow-card"
          >
            <div class="mb-1 text-sm font-semibold text-foreground">{{ f.title }}</div>
            <div class="text-xs leading-relaxed text-muted-foreground">{{ f.desc }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
