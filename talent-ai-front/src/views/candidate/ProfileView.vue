<script setup lang="ts">
import { computed, onActivated, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import {
  User,
  Edit,
  ChevronRight,
  Bell,
  Star,
  Settings,
  LogOut,
  Sparkles,
  AlertCircle,
  Calendar,
} from 'lucide-vue-next'
import { fetchMyCandidateProfile, type CandidateProfile } from '@/api/candidateProfile'
import { fetchMyApplications } from '@/api/delivery'
import { fetchUnreadNotificationCount } from '@/api/notification'
import { computeDeliveryStats } from '@/constants/delivery'
import { profileDisplayName, profileSubtitle } from '@/constants/candidateProfile'
import { useAuthStore } from '@/stores/auth'
import { getErrorMessage } from '@/utils/validators'

type ProfileMenuKey = 'notifications' | 'interviews' | 'favorites' | 'account'

const menuItems: Array<{
  icon: typeof Bell
  label: string
  key: ProfileMenuKey
}> = [
  { icon: Bell, label: '消息通知', key: 'notifications' },
  { icon: Calendar, label: '我的面试', key: 'interviews' },
  { icon: Star, label: '我的收藏', key: 'favorites' },
  { icon: Settings, label: '账号设置', key: 'account' },
]

const router = useRouter()
const auth = useAuthStore()
const { userInfo } = storeToRefs(auth)

const profile = ref<CandidateProfile | null>(null)
const loading = ref(true)
const errorMsg = ref('')
const stats = ref({ apply: 0, interview: 0, offer: 0 })
const unreadCount = ref(0)

const displayName = computed(() => profileDisplayName(profile.value ?? { nickname: userInfo.value?.nickname }))
const subtitle = computed(() => profileSubtitle(profile.value))
const completeness = computed(() => profile.value?.resumeCompleteness ?? 0)
const profileComplete = computed(() => profile.value?.profileComplete === true)
const missingHint = computed(() => {
  if (profileComplete.value) return ''
  return '投递简历前请完善：真实姓名、手机号、所在城市、最高学历'
})

async function loadStats() {
  try {
    const page = await fetchMyApplications({ current: 1, size: 200 })
    const records = page?.records ?? []
    const statsResult = computeDeliveryStats(records, page?.total ?? records.length)
    stats.value.apply = statsResult.total
    stats.value.interview = statsResult.interviewing
    stats.value.offer = statsResult.offer
  } catch {
    stats.value = { apply: 0, interview: 0, offer: 0 }
  }
}

async function loadUnreadCount() {
  try {
    const count = await fetchUnreadNotificationCount()
    unreadCount.value = Math.max(0, Number(count) || 0)
  } catch {
    unreadCount.value = 0
  }
}

async function loadProfile() {
  loading.value = true
  errorMsg.value = ''
  try {
    profile.value = await fetchMyCandidateProfile()
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '加载个人信息失败')
  } finally {
    loading.value = false
  }
}

function goEdit() {
  router.push('/candidate/profile/edit')
}

function handleMenuClick(item: (typeof menuItems)[number]) {
  switch (item.key) {
    case 'notifications':
      router.push('/candidate/notifications')
      break
    case 'interviews':
      router.push('/candidate/interviews')
      break
    case 'favorites':
      router.push('/candidate/favorites')
      break
    case 'account':
      router.push('/candidate/profile/edit')
      break
    default:
      break
  }
}

function handleLogout() {
  auth.logout()
  router.push('/login')
}

onMounted(() => {
  loadProfile()
  loadStats()
  loadUnreadCount()
})

onActivated(() => {
  loadUnreadCount()
  loadStats()
})
</script>

<template>
  <div data-cmp="Profile" class="flex h-full flex-col bg-[#EBF4F0]">
    <div class="gradient-blue-purple relative shrink-0 px-4 pb-6 pt-8">
      <div class="text-center">
        <div class="w-20 h-20 rounded-full bg-white/20 border-4 border-white/40 flex items-center justify-center mx-auto mb-3 overflow-hidden">
          <img
            v-if="profile?.avatarUrl"
            :src="profile.avatarUrl"
            alt=""
            class="w-full h-full object-cover"
          />
          <User v-else :size="36" class="text-white" />
        </div>
        <div class="text-lg font-bold text-white">{{ loading ? '加载中...' : displayName }}</div>
        <div class="text-sm text-white/90 mt-0.5">{{ subtitle }}</div>
        <div v-if="!profileComplete && !loading" class="mt-2 inline-flex items-center gap-1 text-xs text-amber-100 bg-white/15 rounded-full px-2.5 py-1">
          <AlertCircle :size="12" />
          档案完整度 {{ completeness }}%，待完善
        </div>
        <div class="flex items-center justify-center gap-4 mt-3">
          <div v-for="s in [{ label: '投递', count: stats.apply }, { label: '面试', count: stats.interview }, { label: 'Offer', count: stats.offer }]" :key="s.label" class="text-center">
            <div class="text-lg font-black text-white">{{ s.count }}</div>
            <div class="text-xs text-white/90">{{ s.label }}</div>
          </div>
        </div>
      </div>
      <button type="button" class="absolute top-4 right-4 p-2 rounded-full bg-white/20" title="编辑个人信息" @click="goEdit">
        <Edit :size="16" class="text-white" />
      </button>
    </div>

    <div class="flex-1 space-y-4 overflow-y-auto px-4 pb-6 pt-4 scrollbar-thin">
      <p v-if="errorMsg" class="text-xs text-red-600 bg-red-50 border border-red-100 rounded-xl px-3 py-2">{{ errorMsg }}</p>

      <div
        v-if="missingHint && !loading"
        class="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 flex items-start gap-2"
      >
        <AlertCircle :size="16" class="text-amber-600 flex-shrink-0 mt-0.5" />
        <div class="flex-1 min-w-0">
          <p class="text-xs text-amber-800">{{ missingHint }}</p>
          <button type="button" class="text-xs text-brand-blue font-medium mt-1" @click="goEdit">去完善</button>
        </div>
      </div>

      <div
        class="rounded-2xl border border-border bg-card p-4 shadow-card cursor-pointer hover:border-brand-blue/30"
        @click="goEdit"
      >
        <div class="flex items-center justify-between mb-2">
          <span class="text-sm font-semibold text-foreground">个人信息</span>
          <ChevronRight :size="14" class="text-muted-foreground" />
        </div>
        <dl class="grid grid-cols-2 gap-x-3 gap-y-2 text-xs">
          <div>
            <dt class="text-muted-foreground">手机</dt>
            <dd class="text-foreground font-medium">{{ profile?.phone || '—' }}</dd>
          </div>
          <div>
            <dt class="text-muted-foreground">邮箱</dt>
            <dd class="text-foreground font-medium truncate">{{ profile?.email || '—' }}</dd>
          </div>
          <div>
            <dt class="text-muted-foreground">城市</dt>
            <dd class="text-foreground font-medium">{{ profile?.city || '—' }}</dd>
          </div>
          <div>
            <dt class="text-muted-foreground">职位</dt>
            <dd class="text-foreground font-medium truncate">{{ profile?.currentTitle || '—' }}</dd>
          </div>
        </dl>
      </div>

      <div class="relative z-10 rounded-2xl border border-border bg-card p-4 shadow-card">
        <div class="flex items-center gap-2 mb-3">
          <Sparkles :size="14" class="text-brand-purple" />
          <span class="text-sm font-semibold text-foreground">AI职业画像</span>
          <span class="ml-auto text-xs text-muted-foreground">敬请期待</span>
        </div>
        <p v-if="profile?.aiScore" class="text-xs text-muted-foreground mb-2">AI 评分 {{ profile.aiScore }}</p>
        <p v-else class="text-xs text-muted-foreground">完善简历与档案后，将生成职业标签与能力分析</p>
      </div>

      <div class="overflow-hidden rounded-2xl border border-border bg-card shadow-card">
        <div
          v-for="(item, i) in menuItems"
          :key="item.label"
          class="flex items-center gap-3 px-4 py-3.5 cursor-pointer hover:bg-muted transition-colors"
          :class="i < menuItems.length - 1 ? 'border-b border-border' : ''"
          @click="handleMenuClick(item)"
        >
          <div class="w-7 h-7 rounded-lg bg-muted flex items-center justify-center">
            <component :is="item.icon" :size="14" class="text-muted-foreground" />
          </div>
          <span class="text-sm text-foreground flex-1">{{ item.label }}</span>
          <div
            v-if="item.key === 'notifications' && unreadCount > 0"
            class="min-w-5 h-5 px-1 rounded-full bg-brand-red flex items-center justify-center"
          >
            <span class="text-xs text-white font-bold">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </div>
          <ChevronRight :size="14" class="text-muted-foreground" />
        </div>
      </div>

      <button
        type="button"
        class="w-full flex items-center justify-center gap-2 rounded-2xl border border-border bg-card py-3 text-sm text-muted-foreground shadow-card hover:border-brand-red/30 hover:text-brand-red"
        @click="handleLogout"
      >
        <LogOut :size="16" />
        <span>退出登录</span>
      </button>
    </div>
  </div>
</template>
