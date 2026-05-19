<script setup lang="ts">
import { useRouter } from 'vue-router'
import { User, Edit, ChevronRight, Bell, Star, Shield, Settings, HelpCircle, LogOut, Sparkles } from 'lucide-vue-next'

const menuItems = [
  { icon: Bell, label: '消息通知', badge: 3 },
  { icon: Star, label: '我的收藏' },
  { icon: Shield, label: '隐私设置' },
  { icon: Settings, label: '账号设置' },
  { icon: HelpCircle, label: '帮助与反馈' },
]

const router = useRouter()
</script>

<template>
  <div data-cmp="Profile" class="flex h-full flex-col bg-[#EBF4F0]">
    <div class="gradient-blue-purple relative shrink-0 px-4 pb-6 pt-8">
      <div class="text-center">
        <div class="w-20 h-20 rounded-full bg-white/20 border-4 border-white/40 flex items-center justify-center mx-auto mb-3">
          <User :size="36" class="text-white" />
        </div>
        <div class="text-lg font-bold text-white">张三</div>
        <div class="text-sm text-white/90 mt-0.5">高级前端工程师 · 北京</div>
        <div class="flex items-center justify-center gap-4 mt-3">
          <div v-for="s in [{ label: '投递', count: 8 }, { label: '面试', count: 3 }, { label: 'Offer', count: 1 }]" :key="s.label" class="text-center">
            <div class="text-lg font-black text-white">{{ s.count }}</div>
            <div class="text-xs text-white/90">{{ s.label }}</div>
          </div>
        </div>
      </div>
      <button type="button" class="absolute top-4 right-4 p-2 rounded-full bg-white/20">
        <Edit :size="16" class="text-white" />
      </button>
    </div>
    <div class="flex-1 space-y-4 overflow-y-auto px-4 pb-6 pt-4 scrollbar-thin">
      <div class="relative z-10 rounded-2xl border border-border bg-card p-4 shadow-card">
        <div class="flex items-center gap-2 mb-3">
          <Sparkles :size="14" class="text-brand-purple" />
          <span class="text-sm font-semibold text-foreground">AI职业画像</span>
          <span class="ml-auto text-xs text-brand-purple">查看详情 ></span>
        </div>
        <div class="flex gap-2 flex-wrap">
          <span v-for="tag in ['前端专家', '高成长性', '稳定性佳', '名企经历', '开源活跃']" :key="tag" class="text-xs px-2 py-1 rounded-full bg-accent text-accent-foreground border border-brand-border/50">{{ tag }}</span>
        </div>
      </div>
      <div class="overflow-hidden rounded-2xl border border-border bg-card shadow-card">
        <div
          v-for="(item, i) in menuItems"
          :key="item.label"
          class="flex items-center gap-3 px-4 py-3.5 cursor-pointer hover:bg-muted transition-colors"
          :class="i < menuItems.length - 1 ? 'border-b border-border' : ''"
        >
          <div class="w-7 h-7 rounded-lg bg-muted flex items-center justify-center">
            <component :is="item.icon" :size="14" class="text-muted-foreground" />
          </div>
          <span class="text-sm text-foreground flex-1">{{ item.label }}</span>
          <div v-if="item.badge" class="w-5 h-5 rounded-full bg-brand-red flex items-center justify-center">
            <span class="text-xs text-white font-bold">{{ item.badge }}</span>
          </div>
          <ChevronRight :size="14" class="text-muted-foreground" />
        </div>
      </div>
      <button
        type="button"
        class="w-full flex items-center justify-center gap-2 rounded-2xl border border-border bg-card py-3 text-sm text-muted-foreground shadow-card hover:border-brand-red/30 hover:text-brand-red"
        @click="router.push('/login')"
      >
        <LogOut :size="16" />
        <span>退出登录</span>
      </button>
    </div>
  </div>
</template>
