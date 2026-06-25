<script setup lang="ts">
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/tabs/style/css'
import 'element-plus/es/components/tab-pane/style/css'
import 'element-plus/es/components/switch/style/css'
import { ref, onMounted } from 'vue'
import {
  Megaphone,
  Image as ImageIcon,
  Bell,
  ExternalLink,
  Calendar,
  Trash2,
  Send,
  AlertTriangle,
  Info,
  AlertCircle,
  Loader2,
  Users,
  Globe,
  UserCheck,
} from 'lucide-vue-next'
import { ElTabs, ElTabPane, ElSwitch, ElMessage, ElMessageBox } from 'element-plus'
import {
  listBanners,
  updateBannerStatus,
  deleteBanner,
  listAnnouncements,
  broadcastAnnouncement,
  deleteAnnouncement,
  type BannerItem,
  type AnnouncementItem,
  type AnnouncementLevel,
  type AnnouncementTarget,
} from '@/api/adminDashboard'
import { getErrorMessage } from '@/utils/validators'

/* =================================================================
 *  Mock 数据
 * ================================================================= */

const mockBanners: BannerItem[] = [
  {
    id: 1,
    title: '2026 春季校园招聘季火热进行中',
    imageUrl: 'https://picsum.photos/seed/banner1/640/200',
    linkUrl: 'https://talent-ai.example.com/campus-2026',
    startTime: '2026-06-01T00:00:00',
    endTime: '2026-07-31T23:59:59',
    status: 1,
    sortOrder: 1,
    createdAt: '2026-05-28T10:00:00',
  },
  {
    id: 2,
    title: 'AI 简历解析引擎 v3.0 全面升级',
    imageUrl: 'https://picsum.photos/seed/banner2/640/200',
    linkUrl: 'https://talent-ai.example.com/ai-v3',
    startTime: '2026-06-10T00:00:00',
    endTime: '2026-08-10T23:59:59',
    status: 1,
    sortOrder: 2,
    createdAt: '2026-06-08T14:30:00',
  },
  {
    id: 3,
    title: '高端人才猎头专区限时开放',
    imageUrl: 'https://picsum.photos/seed/banner3/640/200',
    linkUrl: 'https://talent-ai.example.com/headhunter',
    startTime: '2026-05-15T00:00:00',
    endTime: '2026-06-15T23:59:59',
    status: 0,
    sortOrder: 3,
    createdAt: '2026-05-10T09:00:00',
  },
  {
    id: 4,
    title: '新能源行业专场 — 万人在线招聘会',
    imageUrl: 'https://picsum.photos/seed/banner4/640/200',
    linkUrl: 'https://talent-ai.example.com/energy-fair',
    startTime: '2026-07-01T00:00:00',
    endTime: '2026-07-15T23:59:59',
    status: 1,
    sortOrder: 4,
    createdAt: '2026-06-20T16:00:00',
  },
]

const mockAnnouncements: AnnouncementItem[] = [
  {
    id: 1,
    title: '系统定期维护通知（6月28日 02:00-04:00）',
    content: '为提升服务稳定性，平台将于6月28日凌晨进行例行维护，届时部分功能可能暂时不可用，敬请谅解。',
    level: 'info',
    target: 'all',
    broadcasted: true,
    broadcastedAt: '2026-06-24T10:00:00',
    createdAt: '2026-06-23T15:00:00',
  },
  {
    id: 2,
    title: '紧急：检测到异常简历批量提交行为',
    content: '风控系统已自动拦截来自可疑IP段的批量简历投递请求，相关账号已临时冻结，请HR端注意核验近期收到的简历。',
    level: 'critical',
    target: 'hr',
    broadcasted: true,
    broadcastedAt: '2026-06-25T08:30:00',
    createdAt: '2026-06-25T08:00:00',
  },
  {
    id: 3,
    title: '新功能上线：AI 智能面试评分系统',
    content: 'AI智能面试评分系统现已全面上线，HR端可在面试管理中开启AI辅助评分，提升面试效率。',
    level: 'info',
    target: 'hr',
    broadcasted: false,
    createdAt: '2026-06-24T14:00:00',
  },
  {
    id: 4,
    title: '求职者端简历模板库更新',
    content: '我们新增了15套高质量简历模板，涵盖技术、金融、设计等热门行业，快来更新您的简历吧！',
    level: 'info',
    target: 'candidate',
    broadcasted: false,
    createdAt: '2026-06-22T11:00:00',
  },
  {
    id: 5,
    title: '安全警告：请勿点击不明链接',
    content: '近期发现有不法分子冒充平台发送钓鱼邮件，请广大用户提高警惕，本平台不会通过邮件索要密码等敏感信息。',
    level: 'warning',
    target: 'all',
    broadcasted: true,
    broadcastedAt: '2026-06-20T09:00:00',
    createdAt: '2026-06-19T17:00:00',
  },
]

/* =================================================================
 *  响应式状态
 * ================================================================= */

const activeTab = ref('banner')
const bannerLoading = ref(false)
const announcementLoading = ref(false)
const banners = ref<BannerItem[]>([...mockBanners])
const announcements = ref<AnnouncementItem[]>([...mockAnnouncements])

/* =================================================================
 *  数据加载
 * ================================================================= */

async function loadBanners() {
  bannerLoading.value = true
  try {
    const data = await listBanners()
    banners.value = data?.records ?? [...mockBanners]
  } catch {
    // API 不可用 → 保持 mock
  } finally {
    bannerLoading.value = false
  }
}

async function loadAnnouncements() {
  announcementLoading.value = true
  try {
    const data = await listAnnouncements()
    announcements.value = data?.records ?? [...mockAnnouncements]
  } catch {
    // API 不可用 → 保持 mock
  } finally {
    announcementLoading.value = false
  }
}

/* =================================================================
 *  轮播图操作
 * ================================================================= */

async function handleBannerStatusChange(banner: BannerItem) {
  const newStatus = banner.status === 1 ? 0 : 1
  try {
    await updateBannerStatus(banner.id, newStatus as 0 | 1)
    banner.status = newStatus as 0 | 1
    ElMessage.success(newStatus === 1 ? '已上线' : '已下线')
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '状态更新失败'))
  }
}

async function handleDeleteBanner(banner: BannerItem) {
  try {
    await ElMessageBox.confirm(
      `确定要删除轮播图「${banner.title}」吗？此操作不可撤销。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteBanner(banner.id)
    banners.value = banners.value.filter(b => b.id !== banner.id)
    ElMessage.success('已删除')
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '删除失败'))
  }
}

/* =================================================================
 *  公告操作
 * ================================================================= */

async function handleBroadcast(item: AnnouncementItem) {
  try {
    await ElMessageBox.confirm(
      `确定要立即广播公告「${item.title}」吗？推送目标：${targetLabel(item.target)}。广播后将即时推送给所有目标用户。`,
      '广播确认',
      { confirmButtonText: '确认广播', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await broadcastAnnouncement(item.id)
    item.broadcasted = true
    item.broadcastedAt = new Date().toISOString()
    ElMessage.success('广播已发送')
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '广播失败'))
  }
}

async function handleDeleteAnnouncement(item: AnnouncementItem) {
  try {
    await ElMessageBox.confirm(
      `确定要删除公告「${item.title}」吗？`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteAnnouncement(item.id)
    announcements.value = announcements.value.filter(a => a.id !== item.id)
    ElMessage.success('已删除')
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '删除失败'))
  }
}

/* =================================================================
 *  工具函数
 * ================================================================= */

function formatTime(value?: string): string {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

function formatDateRange(start: string, end: string): string {
  return `${formatTime(start)} ~ ${formatTime(end)}`
}

const levelConfig: Record<AnnouncementLevel, { label: string; cls: string; icon: typeof Info }> = {
  info:     { label: 'Info',     cls: 'bg-blue-50 text-blue-600 border-blue-200',     icon: Info },
  warning:  { label: 'Warning',  cls: 'bg-orange-50 text-brand-orange border-orange-200', icon: AlertTriangle },
  critical: { label: 'Critical', cls: 'bg-red-50 text-brand-red border-red-200',      icon: AlertCircle },
}

const targetConfig: Record<AnnouncementTarget, { label: string; icon: typeof Globe }> = {
  candidate: { label: '求职者端', icon: UserCheck },
  hr:        { label: 'HR端',    icon: Users },
  all:       { label: '全站',    icon: Globe },
}

function targetLabel(target: AnnouncementTarget): string {
  return targetConfig[target]?.label ?? target
}

function isActive(banner: BannerItem): boolean {
  const now = Date.now()
  return banner.status === 1 && new Date(banner.startTime).getTime() <= now && new Date(banner.endTime).getTime() >= now
}

onMounted(() => {
  loadBanners()
  loadAnnouncements()
})
</script>

<template>
  <div data-cmp="BroadcastConsole" class="scrollbar-thin h-full overflow-y-auto p-6 sm:p-8">
    <div class="mx-auto max-w-6xl">

      <!-- 页面头部 -->
      <div class="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div class="flex items-center gap-3">
          <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-orange text-white">
            <Megaphone :size="20" stroke-width="1.75" />
          </div>
          <div>
            <h1 class="text-2xl font-black text-foreground">全站物料与系统广播中心</h1>
            <p class="mt-0.5 text-sm text-muted-foreground">Global Content & Broadcast Console</p>
          </div>
        </div>
      </div>

      <!-- Tabs -->
      <div class="rounded-2xl border border-border bg-card p-5 shadow-card">
        <ElTabs v-model="activeTab" class="broadcast-tabs">

          <!-- =================== Tab 1: 轮播图管理 =================== -->
          <ElTabPane label="轮播图管理" name="banner">
            <template #label>
              <div class="flex items-center gap-1.5">
                <ImageIcon :size="15" />
                <span>轮播图管理</span>
              </div>
            </template>

            <!-- Loading -->
            <div v-if="bannerLoading" class="flex items-center justify-center py-16 text-muted-foreground">
              <Loader2 :size="20" class="animate-spin text-brand-blue" />
              <span class="ml-2 text-sm">加载中…</span>
            </div>

            <!-- Banner List -->
            <div v-else class="space-y-4 pt-2">
              <div
                v-for="banner in banners"
                :key="banner.id"
                class="flex flex-col gap-4 rounded-xl border border-border bg-background p-4 transition-all duration-200 hover:shadow-md sm:flex-row sm:items-center"
              >
                <!-- 缩略图 -->
                <div class="h-20 w-36 shrink-0 overflow-hidden rounded-lg bg-muted">
                  <img
                    :src="banner.imageUrl"
                    :alt="banner.title"
                    class="h-full w-full object-cover"
                    loading="lazy"
                  />
                </div>

                <!-- 信息 -->
                <div class="min-w-0 flex-1 space-y-2">
                  <div class="flex items-center gap-2">
                    <span class="truncate text-sm font-bold text-foreground">{{ banner.title }}</span>
                    <span
                      v-if="isActive(banner)"
                      class="shrink-0 rounded-full bg-green-50 px-2 py-0.5 text-xs font-medium text-green-600 border border-green-200"
                    >投放中</span>
                    <span
                      v-else-if="banner.status === 0"
                      class="shrink-0 rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground"
                    >已下线</span>
                    <span
                      v-else
                      class="shrink-0 rounded-full bg-orange-50 px-2 py-0.5 text-xs font-medium text-brand-orange border border-orange-200"
                    >待投放</span>
                  </div>
                  <div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground">
                    <div class="flex items-center gap-1">
                      <ExternalLink :size="11" />
                      <span class="max-w-[200px] truncate">{{ banner.linkUrl }}</span>
                    </div>
                    <div class="flex items-center gap-1">
                      <Calendar :size="11" />
                      <span>{{ formatDateRange(banner.startTime, banner.endTime) }}</span>
                    </div>
                  </div>
                </div>

                <!-- 操作 -->
                <div class="flex shrink-0 items-center gap-3">
                  <ElSwitch
                    :model-value="banner.status === 1"
                    active-text="上线"
                    inactive-text="下线"
                    inline-prompt
                    style="--el-switch-on-color: #3d8b7a; --el-switch-off-color: #d1d5db"
                    @change="handleBannerStatusChange(banner)"
                  />
                  <button
                    type="button"
                    class="rounded-lg p-2 text-muted-foreground transition-colors hover:bg-red-50 hover:text-brand-red"
                    title="删除"
                    @click="handleDeleteBanner(banner)"
                  >
                    <Trash2 :size="15" />
                  </button>
                </div>
              </div>
            </div>
          </ElTabPane>

          <!-- =================== Tab 2: 系统公告管理 =================== -->
          <ElTabPane label="系统公告管理" name="announcement">
            <template #label>
              <div class="flex items-center gap-1.5">
                <Bell :size="15" />
                <span>系统公告管理</span>
              </div>
            </template>

            <!-- Loading -->
            <div v-if="announcementLoading" class="flex items-center justify-center py-16 text-muted-foreground">
              <Loader2 :size="20" class="animate-spin text-brand-blue" />
              <span class="ml-2 text-sm">加载中…</span>
            </div>

            <!-- Announcement List -->
            <div v-else class="space-y-4 pt-2">
              <div
                v-for="item in announcements"
                :key="item.id"
                class="rounded-xl border border-border bg-background p-4 transition-all duration-200 hover:shadow-md"
              >
                <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div class="min-w-0 flex-1 space-y-2.5">
                    <!-- 标题行 -->
                    <div class="flex flex-wrap items-center gap-2">
                      <span class="text-sm font-bold text-foreground">{{ item.title }}</span>
                      <!-- 重要级别标签 -->
                      <span
                        :class="[
                          'inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs font-medium',
                          levelConfig[item.level].cls,
                        ]"
                      >
                        <component :is="levelConfig[item.level].icon" :size="11" />
                        {{ levelConfig[item.level].label }}
                      </span>
                      <!-- 推送目标 -->
                      <span class="inline-flex items-center gap-1 rounded-full border border-border bg-muted px-2 py-0.5 text-xs text-muted-foreground">
                        <component :is="targetConfig[item.target].icon" :size="11" />
                        {{ targetConfig[item.target].label }}
                      </span>
                    </div>
                    <!-- 内容摘要 -->
                    <p class="text-xs leading-relaxed text-muted-foreground">{{ item.content }}</p>
                    <!-- 元信息 -->
                    <div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground">
                      <span>创建于 {{ formatTime(item.createdAt) }}</span>
                      <span v-if="item.broadcasted" class="flex items-center gap-1 text-green-600">
                        <Send :size="11" />
                        已广播 {{ formatTime(item.broadcastedAt) }}
                      </span>
                    </div>
                  </div>

                  <!-- 操作 -->
                  <div class="flex shrink-0 items-center gap-2 sm:flex-col sm:items-end">
                    <button
                      v-if="!item.broadcasted"
                      type="button"
                      class="flex items-center gap-1.5 rounded-xl border border-brand-border bg-accent px-3 py-2 text-xs font-semibold text-brand-blue transition-all duration-200 hover:bg-brand-tint-2 hover:shadow-sm"
                      @click="handleBroadcast(item)"
                    >
                      <Send :size="13" />
                      立即广播
                    </button>
                    <span
                      v-else
                      class="flex items-center gap-1 rounded-xl bg-green-50 px-3 py-2 text-xs font-medium text-green-600"
                    >
                      <Send :size="13" />
                      已广播
                    </span>
                    <button
                      type="button"
                      class="rounded-lg p-2 text-muted-foreground transition-colors hover:bg-red-50 hover:text-brand-red"
                      title="删除"
                      @click="handleDeleteAnnouncement(item)"
                    >
                      <Trash2 :size="15" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </ElTabPane>
        </ElTabs>
      </div>

    </div>
  </div>
</template>

<style scoped>
/* 覆盖 ElTabs 默认样式以匹配项目设计系统 */
:deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--border);
}
:deep(.el-tabs__active-bar) {
  background-color: var(--brand-teal);
}
:deep(.el-tabs__item) {
  color: var(--muted-foreground);
  font-size: 14px;
  font-weight: 500;
}
:deep(.el-tabs__item.is-active) {
  color: var(--brand-teal);
}
:deep(.el-tabs__item:hover) {
  color: var(--brand-teal-mid);
}
</style>
