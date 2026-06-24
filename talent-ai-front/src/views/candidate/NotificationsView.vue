<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, ChevronLeft, CheckCheck } from 'lucide-vue-next'
import {
  fetchMyNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  type NotificationItem,
} from '@/api/notification'
import { formatDateTime } from '@/utils/jobFormat'
import { getErrorMessage } from '@/utils/validators'

const router = useRouter()
const loading = ref(false)
const markingAll = ref(false)
const errorMsg = ref('')
const items = ref<NotificationItem[]>([])
const total = ref(0)

const unreadCount = computed(() => items.value.filter((item) => !item.read).length)

async function loadNotifications() {
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await fetchMyNotifications({ current: 1, size: 50 })
    items.value = data.records ?? []
    total.value = data.total ?? items.value.length
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '消息加载失败')
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function handleItemClick(item: NotificationItem) {
  if (!item.read) {
    try {
      await markNotificationRead(item.id)
      item.read = true
    } catch {
      // 标记已读失败不阻断查看
    }
  }
  if (item.bizType === 'application') {
    router.push('/candidate/applications')
  } else if (item.bizType === 'interview' && item.bizId) {
    router.push({ path: '/candidate/interview', query: { id: String(item.bizId) } })
  } else if (item.bizType === 'interview') {
    router.push('/candidate/interviews')
  }
}

async function handleMarkAllRead() {
  if (unreadCount.value === 0) return
  markingAll.value = true
  errorMsg.value = ''
  try {
    await markAllNotificationsRead()
    items.value = items.value.map((item) => ({ ...item, read: true }))
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '全部已读失败')
  } finally {
    markingAll.value = false
  }
}

function goBack() {
  router.back()
}

onMounted(() => {
  loadNotifications()
})
</script>

<template>
  <div data-cmp="Notifications" class="flex h-full flex-col bg-[#EBF4F0]">
    <div class="px-4 py-4 bg-card border-b border-border flex-shrink-0 flex items-center gap-2">
      <button type="button" class="p-1 -ml-1 rounded-lg hover:bg-muted" @click="goBack">
        <ChevronLeft :size="20" class="text-foreground" />
      </button>
      <div class="flex-1 min-w-0">
        <h1 class="text-base font-bold text-foreground">消息通知</h1>
        <p class="text-xs text-muted-foreground mt-0.5">
          {{ loading ? '加载中...' : `共 ${total} 条，${unreadCount} 条未读` }}
        </p>
      </div>
      <button
        type="button"
        class="flex items-center gap-1 text-xs text-brand-blue disabled:opacity-40"
        :disabled="markingAll || unreadCount === 0"
        @click="handleMarkAllRead"
      >
        <CheckCheck :size="14" />
        全部已读
      </button>
    </div>

    <div class="flex-1 overflow-y-auto px-4 py-4 scrollbar-thin space-y-3">
      <p v-if="errorMsg" class="text-xs text-red-600 bg-red-50 border border-red-100 rounded-xl px-3 py-2">
        {{ errorMsg }}
      </p>

      <div
        v-if="!loading && items.length === 0"
        class="rounded-2xl border border-border bg-card p-8 text-center shadow-card"
      >
        <Bell :size="32" class="mx-auto text-muted-foreground/50 mb-2" />
        <p class="text-sm text-muted-foreground">暂无消息通知</p>
        <p class="text-xs text-muted-foreground mt-1">投递、面试等环节的状态变更会在这里提醒</p>
      </div>

      <button
        v-for="item in items"
        :key="item.id"
        type="button"
        class="w-full text-left rounded-2xl border bg-card p-4 shadow-card transition-colors hover:border-brand-blue/30"
        :class="item.read ? 'border-border' : 'border-brand-blue/40 bg-brand-tint/30'"
        @click="handleItemClick(item)"
      >
        <div class="flex items-start gap-3">
          <div
            class="w-8 h-8 rounded-lg flex items-center justify-center shrink-0"
            :class="item.read ? 'bg-muted' : 'bg-brand-blue/10'"
          >
            <Bell :size="14" :class="item.read ? 'text-muted-foreground' : 'text-brand-blue'" />
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <span class="text-sm font-semibold text-foreground truncate">{{ item.title }}</span>
              <span
                v-if="!item.read"
                class="w-2 h-2 rounded-full bg-brand-red shrink-0"
              />
            </div>
            <p v-if="item.content" class="text-xs text-muted-foreground mt-1 line-clamp-3">{{ item.content }}</p>
            <div class="flex items-center gap-2 mt-2 text-[11px] text-muted-foreground">
              <span v-if="item.notifyTypeLabel">{{ item.notifyTypeLabel }}</span>
              <span v-if="item.createdAt">{{ formatDateTime(item.createdAt) }}</span>
            </div>
          </div>
        </div>
      </button>
    </div>
  </div>
</template>
