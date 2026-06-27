<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Sparkles,
  ChevronRight,
  User,
  Briefcase,
  Calendar,
  Video,
  Loader2,
  Lightbulb,
  Copy,
  ExternalLink,
  Star,
} from 'lucide-vue-next'
import {
  fetchAiInterviewQuestions,
  fetchAiMatchByApplication,
  generateAiInterviewQuestions,
  parseDimensionScores,
  parseJsonStringArray,
  type AiInterviewQuestion,
  type AiMatchResult,
} from '@/api/ai'
import {
  fetchMyInterviewPage,
  fetchMyInterviewDetail,
  type InterviewDetail,
  type InterviewListItem,
} from '@/api/interview'
import { INTERVIEW_STATUS, formatInterviewDateTime } from '@/constants/interview'
import {
  buildMeetingCopyText,
  canJoinMeeting,
  copyMeetingInfo,
  openMeeting,
} from '@/utils/interviewMeeting'
import { getErrorMessage } from '@/utils/validators'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const generating = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const copyTip = ref('')

const pendingInterviews = ref<InterviewListItem[]>([])
const selectedInterviewId = ref<number | null>(null)
const interviewDetail = ref<InterviewDetail | null>(null)
const aiMatch = ref<AiMatchResult | null>(null)
const aiQuestions = ref<AiInterviewQuestion[]>([])
const askedQuestionIds = ref<Set<number>>(new Set())

const queryInterviewId = computed(() => {
  const id = Number(route.query.id)
  return Number.isFinite(id) && id > 0 ? id : null
})

const matchScore = computed(() => aiMatch.value?.matchScore ?? 0)
const advantages = computed(() => parseJsonStringArray(aiMatch.value?.advantages))
const disadvantages = computed(() => parseJsonStringArray(aiMatch.value?.disadvantages))

const fallbackQuestions = computed(() => {
  if (aiQuestions.value.length) return aiQuestions.value
  return parseJsonStringArray(aiMatch.value?.suggestedQuestions).map((text, index) => ({
    id: -(index + 1),
    questionText: text,
    category: null as string | null,
    focusPoint: null as string | null,
  }))
})

async function loadPendingInterviews() {
  const pageData = await fetchMyInterviewPage({
    page: 1,
    size: 50,
    status: INTERVIEW_STATUS.PENDING,
  })
  pendingInterviews.value = pageData.records ?? []
}

async function loadInterviewContext(interviewId: number) {
  loading.value = true
  errorMsg.value = ''
  try {
    const detail = await fetchMyInterviewDetail(interviewId)
    interviewDetail.value = detail
    const [questions, match] = await Promise.all([
      fetchAiInterviewQuestions(interviewId).catch(() => [] as AiInterviewQuestion[]),
      fetchAiMatchByApplication(detail.applicationId).catch(() => null),
    ])
    aiQuestions.value = questions
    aiMatch.value = match
    askedQuestionIds.value = new Set()
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '面试信息加载失败')
    interviewDetail.value = null
    aiQuestions.value = []
    aiMatch.value = null
  } finally {
    loading.value = false
  }
}

function selectInterview(interviewId: number) {
  selectedInterviewId.value = interviewId
  router.replace({ path: '/interviewer/prep', query: { id: String(interviewId) } })
}

function toggleAsked(id: number) {
  const next = new Set(askedQuestionIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  askedQuestionIds.value = next
}

async function handleGenerateQuestions() {
  if (!selectedInterviewId.value || generating.value) return
  generating.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const result = await generateAiInterviewQuestions({ interviewId: selectedInterviewId.value })
    aiQuestions.value = result.questions ?? []
    askedQuestionIds.value = new Set()
    successMsg.value = `已生成 ${aiQuestions.value.length} 道面试题`
  } catch (e) {
    errorMsg.value = getErrorMessage(e, 'AI 面试题生成失败')
  } finally {
    generating.value = false
  }
}

function handleJoinMeeting() {
  if (!interviewDetail.value?.meetingUrl) return
  openMeeting(interviewDetail.value.meetingUrl)
}

async function handleCopyMeeting() {
  if (!interviewDetail.value) return
  const ok = await copyMeetingInfo(buildMeetingCopyText(interviewDetail.value))
  copyTip.value = ok ? '会议信息已复制' : '复制失败'
  setTimeout(() => {
    copyTip.value = ''
  }, 2000)
}

function goToDetail() {
  if (!selectedInterviewId.value) return
  router.push({ path: '/interviewer/detail', query: { id: String(selectedInterviewId.value) } })
}

watch(
  () => route.query.id,
  async (id) => {
    const interviewId = Number(id)
    if (Number.isFinite(interviewId) && interviewId > 0) {
      selectedInterviewId.value = interviewId
      await loadInterviewContext(interviewId)
    }
  },
)

onMounted(async () => {
  loading.value = true
  try {
    await loadPendingInterviews()
    const initialId = queryInterviewId.value ?? pendingInterviews.value[0]?.interviewId ?? null
    if (initialId) {
      selectedInterviewId.value = initialId
      await loadInterviewContext(initialId)
    } else {
      loading.value = false
    }
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '数据加载失败')
    loading.value = false
  }
})
</script>

<template>
  <div data-cmp="InterviewPrep" class="h-full overflow-y-auto scrollbar-thin p-8">
    <div class="max-w-6xl mx-auto">
      <div class="mb-8">
        <div class="flex items-center gap-2 text-sm text-muted-foreground mb-1">
          <button type="button" class="cursor-pointer hover:text-brand-blue" @click="router.push('/interviewer/workbench')">
            工作台
          </button>
          <ChevronRight :size="13" />
          <span class="text-foreground">面试准备</span>
        </div>
        <h1 class="text-2xl font-black text-foreground">面试准备</h1>
        <p class="text-muted-foreground mt-1">会前生成 AI 面试提纲，到点进入腾讯/钉钉会议进行线上面试</p>
      </div>

      <p v-if="errorMsg" class="text-xs text-red-600 mb-4">{{ errorMsg }}</p>
      <p v-if="successMsg" class="text-xs text-brand-green mb-4">{{ successMsg }}</p>
      <p v-if="copyTip" class="text-xs text-brand-green mb-4">{{ copyTip }}</p>

      <div class="flex gap-6">
        <div class="w-72 flex-shrink-0 space-y-4">
          <div class="bg-card p-4 shadow-card border border-border">
            <div class="text-sm font-semibold text-foreground mb-3">待面试</div>
            <div v-if="pendingInterviews.length === 0" class="text-xs text-muted-foreground">
              暂无待面试
            </div>
            <div v-else class="space-y-2 max-h-64 overflow-y-auto scrollbar-thin">
              <button
                v-for="iv in pendingInterviews"
                :key="iv.interviewId"
                type="button"
                :class="[
                  'w-full text-left p-3 rounded-xl border transition-colors',
                  selectedInterviewId === iv.interviewId
                    ? 'border-brand-purple/40 bg-brand-tint-2'
                    : 'border-border hover:border-brand-blue/30',
                ]"
                @click="selectInterview(iv.interviewId)"
              >
                <div class="text-sm font-medium text-foreground truncate">{{ iv.candidateName }}</div>
                <div class="text-xs text-muted-foreground truncate mt-0.5">{{ iv.jobTitle }}</div>
                <div class="text-[10px] text-muted-foreground mt-1">
                  {{ formatInterviewDateTime(iv.scheduledStart) }}
                </div>
              </button>
            </div>
          </div>

          <div v-if="interviewDetail" class="bg-card p-4 shadow-card border border-border">
            <div class="flex items-center gap-2 mb-3">
              <User :size="14" class="text-brand-blue" />
              <span class="text-sm font-semibold text-foreground">会议信息</span>
            </div>
            <div class="space-y-2 text-xs">
              <div class="flex items-center gap-2">
                <Briefcase :size="11" class="text-muted-foreground" />
                <span class="text-foreground">{{ interviewDetail.jobTitle }}</span>
              </div>
              <div class="flex items-center gap-2">
                <Calendar :size="11" class="text-muted-foreground" />
                <span class="text-muted-foreground">{{ formatInterviewDateTime(interviewDetail.scheduledStart) }}</span>
              </div>
              <div class="flex items-center gap-2">
                <Video :size="11" class="text-muted-foreground" />
                <span class="text-muted-foreground">{{ interviewDetail.interviewModeLabel }}</span>
              </div>
              <p v-if="interviewDetail.meetingUrl" class="text-foreground break-all mt-2 leading-relaxed">
                {{ interviewDetail.meetingUrl }}
              </p>
              <p v-else class="text-muted-foreground mt-2">暂未填写会议链接</p>
            </div>
            <div class="flex gap-2 mt-3">
              <button
                v-if="canJoinMeeting(interviewDetail)"
                type="button"
                class="flex-1 flex items-center justify-center gap-1 py-1.5 rounded-lg text-xs font-medium gradient-blue text-white"
                @click="handleJoinMeeting"
              >
                <ExternalLink :size="12" />
                进入会议
              </button>
              <button
                v-if="interviewDetail.meetingUrl"
                type="button"
                class="flex items-center justify-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium border border-border hover:bg-muted"
                @click="handleCopyMeeting"
              >
                <Copy :size="12" />
              </button>
            </div>
          </div>
        </div>

        <div class="flex-1 space-y-4">
          <div class="bg-card p-5 shadow-card border border-border">
            <div class="flex items-center gap-2 mb-3">
              <Sparkles :size="15" class="text-brand-purple" />
              <span class="text-base font-bold text-foreground">AI 初筛参考</span>
            </div>
            <div v-if="!selectedInterviewId && !loading" class="text-sm text-muted-foreground py-8 text-center">
              请从左侧选择一场待面试
            </div>
            <div v-else-if="loading" class="text-sm text-muted-foreground py-8 text-center">加载中...</div>
            <template v-else>
              <div class="flex items-center gap-4 mb-4">
                <div class="flex items-center gap-1">
                  <Star :size="14" class="text-brand-orange" />
                  <span class="text-lg font-black text-foreground">{{ matchScore || '—' }}</span>
                  <span v-if="matchScore" class="text-sm text-muted-foreground">/ 100</span>
                </div>
                <span class="text-xs text-muted-foreground">综合匹配分（只读参考）</span>
              </div>
              <div v-if="advantages.length" class="mb-3">
                <div class="text-xs font-semibold text-brand-green mb-1">匹配优势</div>
                <ul class="space-y-0.5">
                  <li v-for="(item, i) in advantages" :key="i" class="text-xs text-muted-foreground">· {{ item }}</li>
                </ul>
              </div>
              <div v-if="disadvantages.length">
                <div class="text-xs font-semibold text-brand-orange mb-1">待验证点</div>
                <ul class="space-y-0.5">
                  <li v-for="(item, i) in disadvantages" :key="i" class="text-xs text-muted-foreground">· {{ item }}</li>
                </ul>
              </div>
              <p v-if="!matchScore && !advantages.length && !disadvantages.length" class="text-xs text-muted-foreground">
                暂无 AI 初筛数据，请结合岗位 JD 自行准备问题
              </p>
            </template>
          </div>

          <div class="bg-card p-5 shadow-card border border-border">
            <div class="flex items-center justify-between gap-3 mb-4">
              <div class="flex items-center gap-2">
                <Sparkles :size="15" class="text-brand-purple" />
                <span class="text-base font-bold text-foreground">AI 面试提纲</span>
              </div>
              <button
                type="button"
                class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium gradient-purple text-white disabled:opacity-50"
                :disabled="!selectedInterviewId || generating || loading"
                @click="handleGenerateQuestions"
              >
                <Loader2 v-if="generating" :size="13" class="animate-spin" />
                <Sparkles v-else :size="13" />
                <span>{{ generating ? '生成中...' : 'AI 生成面试题' }}</span>
              </button>
            </div>

            <div v-if="!selectedInterviewId && !loading" class="text-sm text-muted-foreground py-8 text-center">
              选择面试后可生成针对性面试题
            </div>
            <div v-else-if="loading" class="text-sm text-muted-foreground py-8 text-center">加载中...</div>
            <div v-else-if="fallbackQuestions.length === 0" class="text-sm text-muted-foreground py-6 text-center border border-dashed border-border rounded-xl">
              暂无面试题，点击上方按钮生成；或在详情页提交评价
            </div>
            <div v-else class="space-y-2">
              <label
                v-for="q in fallbackQuestions"
                :key="q.id"
                class="flex items-start gap-3 p-3 rounded-xl border border-border hover:border-brand-purple/30 cursor-pointer transition-colors"
                :class="askedQuestionIds.has(q.id) ? 'bg-green-50/50 border-green-200' : ''"
              >
                <input
                  type="checkbox"
                  class="mt-0.5 accent-brand-purple"
                  :checked="askedQuestionIds.has(q.id)"
                  @change="toggleAsked(q.id)"
                />
                <div class="flex-1 min-w-0">
                  <div class="text-sm text-foreground font-medium">{{ q.questionText }}</div>
                  <div v-if="q.category" class="text-[10px] text-muted-foreground mt-1">{{ q.category }}</div>
                  <div v-if="q.focusPoint" class="flex items-start gap-1 text-xs text-muted-foreground mt-1">
                    <Lightbulb :size="11" class="text-brand-orange mt-0.5 flex-shrink-0" />
                    <span>考察重点：{{ q.focusPoint }}</span>
                  </div>
                </div>
              </label>
            </div>

            <div v-if="selectedInterviewId && !loading" class="mt-4 pt-4 border-t border-border flex justify-end">
              <button
                type="button"
                class="text-sm text-brand-blue hover:underline"
                @click="goToDetail"
              >
                面试结束后前往提交评价 →
              </button>
            </div>
          </div>
        </div>

        <div class="w-56 flex-shrink-0 hidden xl:block">
          <div class="bg-accent rounded-2xl p-4 border border-brand-border/50 sticky top-0">
            <div class="flex items-center gap-2 mb-2">
              <Lightbulb :size="13" class="text-brand-orange" />
              <span class="text-xs font-semibold text-brand-orange">使用流程</span>
            </div>
            <ol class="text-xs text-foreground leading-relaxed space-y-2 list-decimal list-inside">
              <li>选择待面试</li>
              <li>查看 AI 初筛参考</li>
              <li>生成并勾选面试题</li>
              <li>到点进入腾讯/钉钉会议</li>
              <li>结束后提交评价</li>
            </ol>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
