<script setup lang="ts">
import { computed, onActivated, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Eye,
  Download,
  Sparkles,
  CheckCircle,
  AlertTriangle,
  GraduationCap,
  Briefcase,
  Code,
  Edit,
} from 'lucide-vue-next'
import AttachmentResumePanel from '@/components/candidate/AttachmentResumePanel.vue'
import {
  createOnlineResume,
  fetchOnlineResumeDetail,
  fetchOnlineResumeList,
  type OnlineResumeDetail,
} from '@/api/onlineResume'
import { fetchAttachmentResumes } from '@/api/resume'
import {
  evaluateAiResumeQuality,
  fetchAiParseLatest,
  fetchAiResumeQualityLatest,
  type AiResumeQualityScore,
} from '@/api/ai'
import {
  formatEducationSub,
  formatResumePeriod,
  formatWorkSub,
} from '@/utils/resumeFormat'
import { getErrorMessage } from '@/utils/validators'
import { useCandidateHint } from '@/composables/useCandidateHint'

const router = useRouter()
const { showComingSoon } = useCandidateHint()

const detail = ref<OnlineResumeDetail | null>(null)
const loading = ref(false)
const errorMsg = ref('')
const resumeId = ref<number | null>(null)

const completeness = ref(0)

const qualityLoading = ref(false)
const qualityEvaluating = ref(false)
const qualityScore = ref<AiResumeQualityScore | null>(null)
const parsePending = ref(false)

const displayScore = computed(() => qualityScore.value?.qualityScore ?? null)

const qualityTags = computed(() => {
  const tags: { label: string; ok: boolean }[] = []
  for (const label of qualityScore.value?.strengths ?? []) {
    tags.push({ label, ok: true })
  }
  for (const label of qualityScore.value?.weaknesses ?? []) {
    tags.push({ label, ok: false })
  }
  return tags
})

const aiSuggestions = computed(() => qualityScore.value?.suggestions ?? [])

const qualityHint = computed(() => {
  if (parsePending.value) return '简历解析中，完成后将自动评分…'
  if (qualityEvaluating.value) return 'AI 正在评估简历质量…'
  if (!resumeId.value) return '请先创建或上传简历'
  if (!qualityScore.value) return '投递后解析完成可获 AI 评分，或点击下方重新评分'
  return qualityScore.value.summary ?? ''
})

async function resolveResumeId() {
  const onlineList = await fetchOnlineResumeList()
  if (onlineList?.length) {
    resumeId.value = onlineList[0].id
    return
  }
  const attachments = await fetchAttachmentResumes()
  if (attachments?.length) {
    resumeId.value = attachments[0].id
  } else {
    resumeId.value = null
  }
}

async function loadQualityScore(options?: { forceEvaluate?: boolean }) {
  if (!resumeId.value) {
    qualityScore.value = null
    parsePending.value = false
    return
  }
  qualityLoading.value = true
  try {
    const parseTask = await fetchAiParseLatest(resumeId.value)
    parsePending.value = parseTask != null && parseTask.taskStatus !== 2

    if (options?.forceEvaluate) {
      qualityEvaluating.value = true
      qualityScore.value = await evaluateAiResumeQuality({
        resumeId: resumeId.value,
        forceRefresh: true,
      })
      return
    }

    qualityScore.value = await fetchAiResumeQualityLatest(resumeId.value)
    if (!qualityScore.value && parseTask?.taskStatus === 2) {
      qualityEvaluating.value = true
      qualityScore.value = await evaluateAiResumeQuality({ resumeId: resumeId.value })
    }
  } catch (e) {
    if (!qualityScore.value) {
      errorMsg.value = getErrorMessage(e, 'AI 简历评分加载失败')
    }
  } finally {
    qualityLoading.value = false
    qualityEvaluating.value = false
  }
}

async function handleRefreshQuality() {
  if (!resumeId.value || qualityEvaluating.value) return
  errorMsg.value = ''
  await loadQualityScore({ forceEvaluate: true })
}

async function loadOnlineResume() {
  loading.value = true
  errorMsg.value = ''
  try {
    await resolveResumeId()
    const list = await fetchOnlineResumeList()
    if (!list?.length) {
      detail.value = null
      completeness.value = 0
    } else {
      detail.value = await fetchOnlineResumeDetail(list[0].id)
      completeness.value = detail.value.completeness ?? 0
    }
    await loadQualityScore()
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '加载在线简历失败')
    detail.value = null
  } finally {
    loading.value = false
  }
}

function goEdit(section?: 'education' | 'work' | 'skills') {
  const id = detail.value?.id
  router.push({
    path: '/candidate/resume/edit',
    query: {
      ...(id != null ? { id: String(id) } : {}),
      ...(section ? { section } : {}),
    },
  })
}

async function ensureAndEdit() {
  if (detail.value?.id) {
    goEdit()
    return
  }
  try {
    const created = await createOnlineResume({ resumeName: '我的在线简历' })
    router.push({ path: '/candidate/resume/edit', query: { id: String(created.id) } })
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '创建简历失败')
  }
}

onMounted(() => {
  loadOnlineResume()
})

onActivated(() => {
  loadOnlineResume()
})
</script>

<template>
  <div data-cmp="Resume" class="flex h-full flex-col bg-[#EBF4F0]">
    <div class="px-4 pt-4 pb-3 bg-card border-b border-border flex-shrink-0">
      <div class="flex items-center justify-between mb-1">
        <h1 class="text-base font-bold text-foreground">我的简历</h1>
        <div class="flex items-center gap-2">
          <button
            type="button"
            class="p-1.5 rounded-lg hover:bg-muted"
            title="预览"
            @click="showComingSoon('在线简历预览')"
          >
            <Eye :size="16" class="text-muted-foreground" />
          </button>
          <button
            type="button"
            class="p-1.5 rounded-lg hover:bg-muted"
            title="下载"
            @click="showComingSoon('简历下载')"
          >
            <Download :size="16" class="text-muted-foreground" />
          </button>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <div class="flex-1 h-1.5 bg-muted rounded-full overflow-hidden">
          <div
            class="h-full rounded-full gradient-blue transition-all"
            :style="{ width: `${completeness}%` }"
          />
        </div>
        <span class="text-xs font-semibold text-brand-blue">{{ completeness }}%</span>
        <span class="text-xs text-muted-foreground">完整度</span>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto scrollbar-thin px-4 py-4 pb-4 space-y-4">
      <div class="gradient-blue-purple rounded-2xl p-4 text-white">
        <div class="flex items-center justify-between gap-2 mb-2">
          <div class="flex items-center gap-2">
            <Sparkles :size="14" />
            <span class="text-sm font-semibold">AI简历评分</span>
          </div>
          <button
            type="button"
            class="text-[11px] px-2 py-0.5 rounded-lg bg-white/15 border border-white/30 disabled:opacity-50"
            :disabled="!resumeId || qualityLoading || qualityEvaluating || parsePending"
            @click="handleRefreshQuality"
          >
            {{ qualityEvaluating ? '评分中…' : '重新评分' }}
          </button>
        </div>
        <div class="flex items-end gap-2 mb-3">
          <span v-if="displayScore != null" class="text-4xl font-black">{{ displayScore }}</span>
          <span v-else class="text-2xl font-bold text-white/80">—</span>
          <span class="text-white/70 mb-1">/ 100</span>
        </div>
        <p v-if="qualityHint" class="text-[11px] text-white/80 mb-3 leading-relaxed">{{ qualityHint }}</p>
        <div v-if="qualityTags.length" class="flex flex-wrap gap-1.5">
          <span
            v-for="t in qualityTags"
            :key="t.label"
            class="text-[11px] px-2 py-0.5 rounded-full border"
            :class="t.ok ? 'bg-white/15 border-white/30' : 'bg-white/10 border-white/20 line-through opacity-80'"
          >
            {{ t.ok ? '✓' : '×' }} {{ t.label }}
          </span>
        </div>
      </div>

      <div class="bg-accent rounded-2xl p-4 border border-brand-border/50">
        <div class="flex items-center gap-2 mb-3">
          <Sparkles :size="14" class="text-brand-purple" />
          <span class="text-xs font-semibold text-brand-purple">AI优化建议</span>
        </div>
        <ul v-if="aiSuggestions.length" class="space-y-2">
          <li
            v-for="(tip, i) in aiSuggestions"
            :key="i"
            class="flex items-start gap-2 text-xs text-muted-foreground"
          >
            <AlertTriangle v-if="i === aiSuggestions.length - 1" :size="12" class="text-brand-orange flex-shrink-0 mt-0.5" />
            <CheckCircle v-else :size="12" class="text-brand-green flex-shrink-0 mt-0.5" />
            <span>{{ i + 1 }}. {{ tip }}</span>
          </li>
        </ul>
        <p v-else class="text-xs text-muted-foreground">暂无优化建议，完成 AI 评分后将展示</p>
        <button
          type="button"
          class="mt-3 w-full py-2 rounded-xl gradient-purple text-white text-xs font-medium disabled:opacity-50"
          :disabled="!resumeId || qualityEvaluating"
          @click="handleRefreshQuality"
        >
          {{ qualityEvaluating ? 'AI 评分中…' : '刷新 AI 评分' }}
        </button>
      </div>

      <p v-if="errorMsg" class="text-xs text-red-600">{{ errorMsg }}</p>
      <p v-if="loading" class="text-xs text-muted-foreground text-center py-4">加载中...</p>

      <!-- 教育经历 -->
      <div class="bg-card shadow-card p-4 border border-border">
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2">
            <GraduationCap :size="15" class="text-brand-blue" />
            <span class="text-sm font-semibold text-foreground">教育经历</span>
          </div>
          <button
            type="button"
            class="p-1.5 rounded-lg hover:bg-muted"
            title="编辑"
            @click="detail ? goEdit('education') : ensureAndEdit()"
          >
            <Edit :size="14" class="text-muted-foreground" />
          </button>
        </div>
        <template v-if="detail?.educations?.length">
          <div
            v-for="(edu, i) in detail.educations"
            :key="edu.id ?? i"
            class="py-2 border-b border-border last:border-0"
          >
            <div class="flex items-start justify-between gap-2">
              <div class="text-xs font-semibold text-foreground">{{ edu.schoolName }}</div>
              <span class="text-xs text-muted-foreground flex-shrink-0">
                {{ formatResumePeriod(edu.startDate, edu.endDate) }}
              </span>
            </div>
            <div class="text-xs text-muted-foreground mt-0.5">
              {{ formatEducationSub(edu.major, edu.degree) || '—' }}
            </div>
          </div>
        </template>
        <p v-else class="text-xs text-muted-foreground py-2">暂无教育经历，点击右上角编辑</p>
      </div>

      <!-- 工作经历 -->
      <div class="bg-card shadow-card p-4 border border-border">
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2">
            <Briefcase :size="15" class="text-brand-purple" />
            <span class="text-sm font-semibold text-foreground">工作经历</span>
          </div>
          <button
            type="button"
            class="p-1.5 rounded-lg hover:bg-muted"
            title="编辑"
            @click="detail ? goEdit('work') : ensureAndEdit()"
          >
            <Edit :size="14" class="text-muted-foreground" />
          </button>
        </div>
        <template v-if="detail?.workExperiences?.length">
          <div
            v-for="(w, i) in detail.workExperiences"
            :key="w.id ?? i"
            class="py-2 border-b border-border last:border-0"
          >
            <div class="text-xs font-semibold text-foreground">{{ w.companyName }}</div>
            <div class="text-xs text-muted-foreground mt-0.5">
              {{ formatWorkSub(w.jobTitle, w.startDate, w.endDate) }}
            </div>
          </div>
        </template>
        <p v-else class="text-xs text-muted-foreground py-2">暂无工作经历，点击右上角编辑</p>
      </div>

      <!-- 技能标签 -->
      <div class="bg-card shadow-card p-4 border border-border">
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2">
            <Code :size="15" class="text-brand-green" />
            <span class="text-sm font-semibold text-foreground">技能标签</span>
          </div>
          <button
            type="button"
            class="p-1.5 rounded-lg hover:bg-muted"
            title="编辑"
            @click="detail ? goEdit('skills') : ensureAndEdit()"
          >
            <Edit :size="14" class="text-muted-foreground" />
          </button>
        </div>
        <div v-if="detail?.skills?.length" class="flex flex-wrap gap-1.5">
          <span
            v-for="(s, i) in detail.skills"
            :key="s.id ?? i"
            class="text-xs px-2 py-1 rounded-full bg-muted text-muted-foreground"
          >
            {{ s.skillName }}
          </span>
        </div>
        <p v-else class="text-xs text-muted-foreground py-2">暂无技能标签，点击右上角编辑</p>
      </div>

      <div class="pt-2 border-t border-border">
        <h2 class="text-sm font-semibold text-foreground mb-3 flex items-center gap-2">
          附件简历
          <span class="text-[11px] font-normal text-muted-foreground">（pdf/doc/docx，用于岗位投递）</span>
        </h2>
        <AttachmentResumePanel embedded />
      </div>
    </div>
  </div>
</template>
