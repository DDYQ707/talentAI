<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Clock, CheckCircle, XCircle, Calendar, Sparkles, ChevronRight, FileText } from 'lucide-vue-next'
import { fetchMyApplications, type DeliveryRecord } from '@/api/delivery'
import { fetchResumePreviewByResume, openResumePreview } from '@/api/resume'
import {
  APPLICATION_STAGES,
  stageLabel,
  statusToUi,
  type ApplicationUiStatus,
} from '@/constants/delivery'
import { formatDateTime } from '@/utils/jobFormat'
import { getErrorMessage } from '@/utils/validators'

interface AppItem {
  id: number
  applicationNo: string
  job: string
  date: string
  stage: string
  stageIndex: number
  status: ApplicationUiStatus
  next: string
  resumeId?: number
  resumeName?: string
  attachmentFileName?: string
  attachmentFileType?: string
}

const apps = ref<AppItem[]>([])
const loading = ref(false)
const previewingId = ref<number | null>(null)
const errorMsg = ref('')

const statusConf: Record<string, { cls: string; label: string; icon: typeof Clock }> = {
  进行中: { cls: 'text-brand-blue', label: '进行中', icon: Clock },
  offer: { cls: 'text-brand-green', label: '收到Offer!', icon: CheckCircle },
  已淘汰: { cls: 'text-muted-foreground', label: '未通过', icon: XCircle },
  已撤回: { cls: 'text-muted-foreground', label: '已撤回', icon: XCircle },
}

const stages = APPLICATION_STAGES.slice(0, 6)

function mapRecord(record: DeliveryRecord): AppItem {
  const uiStatus = statusToUi(record.status)
  const stageName = stageLabel(record.currentStage)
  const stageIndex = Math.max(0, APPLICATION_STAGES.indexOf(stageName as (typeof APPLICATION_STAGES)[number]))

  let next = ''
  if (uiStatus === 'offer') next = '请在规定时间内确认 Offer'
  else if (uiStatus === '进行中') next = `当前阶段：${stageName}`

  return {
    id: record.id,
    applicationNo: record.applicationNo,
    job: record.jobTitle,
    date: formatDateTime(record.appliedAt),
    stage: stageName,
    stageIndex,
    status: uiStatus,
    next,
    resumeId: record.resumeId,
    resumeName: record.resumeName,
    attachmentFileName: record.attachmentFileName,
    attachmentFileType: record.attachmentFileType,
  }
}

async function previewAttachment(app: AppItem) {
  if (!app.resumeId) return
  previewingId.value = app.id
  errorMsg.value = ''
  try {
    const preview = await fetchResumePreviewByResume(app.resumeId)
    openResumePreview(preview)
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '无法打开简历附件')
  } finally {
    previewingId.value = null
  }
}

const summary = computed(() => {
  const inProgress = apps.value.filter((a) => a.status === '进行中').length
  const interviewing = apps.value.filter(
    (a) => a.status === '进行中' && a.stageIndex >= 2 && a.stageIndex <= 4,
  ).length
  const offer = apps.value.filter((a) => a.status === 'offer').length
  return [
    { label: '投递中', count: inProgress, color: 'text-brand-blue bg-brand-tint' },
    { label: '面试中', count: interviewing, color: 'text-brand-purple bg-brand-tint-2' },
    { label: '收到Offer', count: offer, color: 'text-brand-green bg-green-50' },
  ]
})

async function loadApplications() {
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await fetchMyApplications({ current: 1, size: 50 })
    apps.value = (data.records ?? []).map(mapRecord)
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '投递记录加载失败')
    apps.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadApplications()
})
</script>

<template>
  <div data-cmp="Applications" class="flex h-full flex-col bg-[#EBF4F0]">
    <div class="px-4 py-4 bg-card border-b border-border flex-shrink-0">
      <h1 class="text-base font-bold text-foreground">投递状态</h1>
      <p class="text-xs text-muted-foreground mt-0.5">共 {{ apps.length }} 条投递记录</p>
    </div>

    <div class="flex gap-3 px-4 py-3 bg-card border-b border-border flex-shrink-0">
      <div
        v-for="s in summary"
        :key="s.label"
        :class="['flex-1 rounded-xl px-3 py-2 text-center', s.color]"
      >
        <div class="text-lg font-black">{{ s.count }}</div>
        <div class="text-xs opacity-80">{{ s.label }}</div>
      </div>
    </div>

    <div v-if="errorMsg" class="mx-4 mt-3 text-xs text-red-600 bg-red-50 border border-red-100 rounded-xl px-3 py-2">
      {{ errorMsg }}
    </div>

    <div class="flex-1 overflow-y-auto scrollbar-thin px-4 pt-4 pb-4 space-y-4">
      <div v-if="loading" class="text-center text-xs text-muted-foreground py-8">加载中...</div>
      <div v-else-if="!apps.length" class="text-center text-xs text-muted-foreground py-8">
        暂无投递记录，去岗位列表投递吧
      </div>

      <div
        v-for="app in apps"
        :key="app.id"
        :class="['bg-card shadow-card border p-4', app.status === 'offer' ? 'border-green-200' : 'border-border']"
      >
        <div class="flex items-start justify-between mb-3">
          <div>
            <div class="text-sm font-bold text-foreground">{{ app.job }}</div>
            <div class="text-xs text-muted-foreground mt-0.5">{{ app.applicationNo }}</div>
          </div>
          <div :class="['flex items-center gap-1 text-xs font-medium', (statusConf[app.status] || statusConf['进行中']).cls]">
            <component :is="(statusConf[app.status] || statusConf['进行中']).icon" :size="12" />
            <span>{{ (statusConf[app.status] || statusConf['进行中']).label }}</span>
          </div>
        </div>

        <div class="flex items-center mb-3">
          <div v-for="(s, i) in stages" :key="s" class="flex items-center flex-1">
            <div class="flex items-center flex-1">
              <div
                class="w-4 h-4 rounded-full flex items-center justify-center flex-shrink-0 text-white text-xs"
                :class="[
                  i < app.stageIndex ? 'bg-brand-green' : i === app.stageIndex ? 'bg-brand-blue' : 'bg-muted',
                ]"
              >
                <CheckCircle v-if="i < app.stageIndex" :size="10" />
                <span v-else>{{ i + 1 }}</span>
              </div>
              <div v-if="i < stages.length - 1" :class="['flex-1 h-0.5', i < app.stageIndex ? 'bg-brand-green' : 'bg-muted']" />
            </div>
          </div>
        </div>

        <div class="text-xs text-muted-foreground mb-1">当前：{{ app.stage }}</div>
        <div
          v-if="app.next"
          :class="['flex items-center gap-2 text-xs rounded-lg px-3 py-2 mt-2', app.status === 'offer' ? 'bg-green-50 text-brand-green' : 'bg-accent text-brand-purple']"
        >
          <CheckCircle v-if="app.status === 'offer'" :size="12" />
          <Calendar v-else :size="12" />
          <span>{{ app.next }}</span>
          <ChevronRight :size="12" class="ml-auto" />
        </div>
        <div
          v-if="app.resumeId && (app.attachmentFileName || app.resumeName)"
          class="flex items-center gap-2 mt-2 p-2 rounded-lg bg-muted/50 border border-border"
        >
          <FileText :size="14" class="text-brand-blue flex-shrink-0" />
          <span class="text-xs text-foreground truncate flex-1">
            {{ app.attachmentFileName || app.resumeName }}
            <span v-if="app.attachmentFileType" class="text-muted-foreground uppercase">
              · {{ app.attachmentFileType }}
            </span>
          </span>
          <button
            type="button"
            class="text-xs text-brand-blue whitespace-nowrap disabled:opacity-50"
            :disabled="previewingId === app.id"
            @click="previewAttachment(app)"
          >
            {{ previewingId === app.id ? '打开中...' : '查看附件' }}
          </button>
        </div>
        <div class="flex items-center justify-between mt-2">
          <span class="text-xs text-muted-foreground">投递时间：{{ app.date }}</span>
          <button type="button" class="flex items-center gap-1 text-xs text-brand-blue">
            <Sparkles :size="11" />
            <span>AI跟进建议</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
