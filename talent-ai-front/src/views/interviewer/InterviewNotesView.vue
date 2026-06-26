<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Sparkles,
  ClipboardList,
  ChevronRight,
  User,
  Briefcase,
  Calendar,
  Save,
  Loader2,
  Lightbulb,
  Star,
  ArrowRight,
  CheckCircle2,
} from 'lucide-vue-next'
import {
  fetchAiInterviewNote,
  fetchAiInterviewQuestions,
  saveAiInterviewNote,
  synthesizeAiInterviewNote,
  type AiInterviewNote,
  type AiInterviewQuestion,
} from '@/api/ai'
import {
  fetchMyInterviewPage,
  fetchMyInterviewDetail,
  type InterviewDetail,
  type InterviewListItem,
} from '@/api/interview'
import { INTERVIEW_STATUS, formatInterviewDateTime } from '@/constants/interview'
import { getErrorMessage } from '@/utils/validators'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const saving = ref(false)
const synthesizing = ref(false)
const errorMsg = ref('')
const successMsg = ref('')

const pendingInterviews = ref<InterviewListItem[]>([])
const selectedInterviewId = ref<number | null>(null)
const interviewDetail = ref<InterviewDetail | null>(null)
const aiQuestions = ref<AiInterviewQuestion[]>([])
const savedNote = ref<AiInterviewNote | null>(null)

const noteContent = ref('')
const checkedQuestions = ref<Set<number>>(new Set())

const queryInterviewId = computed(() => {
  const id = Number(route.query.id)
  return Number.isFinite(id) && id > 0 ? id : null
})

const selectedInterview = computed(() =>
  pendingInterviews.value.find((iv) => iv.interviewId === selectedInterviewId.value),
)

const hasDraft = computed(() => !!savedNote.value?.aiSummary)

const draftScore = computed(() => savedNote.value?.aiSuggestedScore ?? null)

const draftConclusion = computed(() => savedNote.value?.aiSuggestedConclusionLabel ?? '')

const draftHighlights = computed(() => savedNote.value?.aiHighlights ?? [])

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
    const [detail, questions, note] = await Promise.all([
      fetchMyInterviewDetail(interviewId),
      fetchAiInterviewQuestions(interviewId).catch(() => [] as AiInterviewQuestion[]),
      fetchAiInterviewNote(interviewId).catch(() => null),
    ])
    interviewDetail.value = detail
    aiQuestions.value = questions
    savedNote.value = note
    noteContent.value = note?.noteContent ?? ''
    checkedQuestions.value = new Set(questions.map((q) => q.id))
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '面试信息加载失败')
    interviewDetail.value = null
  } finally {
    loading.value = false
  }
}

function selectInterview(interviewId: number) {
  selectedInterviewId.value = interviewId
  router.replace({ path: '/interviewer/notes', query: { id: String(interviewId) } })
}

function toggleQuestion(id: number) {
  const next = new Set(checkedQuestions.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  checkedQuestions.value = next
}

async function handleSave() {
  if (!selectedInterviewId.value || saving.value) return
  saving.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    savedNote.value = await saveAiInterviewNote({
      interviewId: selectedInterviewId.value,
      noteContent: noteContent.value,
    })
    successMsg.value = '笔记已保存'
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '笔记保存失败')
  } finally {
    saving.value = false
  }
}

async function handleSynthesize() {
  if (!selectedInterviewId.value || synthesizing.value) return
  if (noteContent.value.trim().length < 10) {
    errorMsg.value = '请至少记录 10 字以上的面试观察后再生成评估草稿'
    return
  }
  synthesizing.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    savedNote.value = await synthesizeAiInterviewNote({
      interviewId: selectedInterviewId.value,
      noteContent: noteContent.value,
    })
    successMsg.value = 'AI 评估草稿已生成'
  } catch (e) {
    errorMsg.value = getErrorMessage(e, 'AI 评估草稿生成失败')
  } finally {
    synthesizing.value = false
  }
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
    }
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '数据加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div data-cmp="InterviewNotes" class="h-full overflow-y-auto scrollbar-thin p-8">
    <div class="max-w-6xl mx-auto">
      <div class="flex items-center justify-between mb-8">
        <div>
          <div class="flex items-center gap-2 text-sm text-muted-foreground mb-1">
            <button type="button" class="cursor-pointer hover:text-brand-blue" @click="router.push('/interviewer')">
              面试列表
            </button>
            <ChevronRight :size="13" />
            <span class="text-foreground">AI 面试笔记</span>
          </div>
          <h1 class="text-2xl font-black text-foreground">AI 面试笔记助手</h1>
          <p class="text-muted-foreground mt-1">记录现场观察，AI 帮你生成评估草稿，一键同步到评价页</p>
        </div>
      </div>

      <p v-if="errorMsg" class="text-xs text-red-600 mb-4">{{ errorMsg }}</p>
      <p v-if="successMsg" class="text-xs text-brand-green mb-4">{{ successMsg }}</p>

      <div class="flex gap-6">
        <!-- 左侧：待面试选择 + 面试题 -->
        <div class="w-72 flex-shrink-0 space-y-4">
          <div class="bg-card p-4 shadow-card border border-border">
            <div class="text-sm font-semibold text-foreground mb-3">选择待面试</div>
            <div v-if="pendingInterviews.length === 0" class="text-xs text-muted-foreground">
              暂无待进行面试
            </div>
            <div v-else class="space-y-2 max-h-48 overflow-y-auto scrollbar-thin">
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
              <span class="text-sm font-semibold text-foreground">当前面试</span>
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
            </div>
          </div>

          <div class="bg-card p-4 shadow-card border border-border">
            <div class="flex items-center gap-2 mb-3">
              <Sparkles :size="14" class="text-brand-purple" />
              <span class="text-sm font-semibold text-foreground">参考面试题</span>
            </div>
            <p v-if="aiQuestions.length === 0" class="text-xs text-muted-foreground">
              暂无 AI 面试题，可在详情页生成后回来使用
            </p>
            <div v-else class="space-y-2 max-h-64 overflow-y-auto scrollbar-thin">
              <label
                v-for="q in aiQuestions"
                :key="q.id"
                class="flex items-start gap-2 p-2 rounded-lg hover:bg-muted cursor-pointer"
              >
                <input
                  type="checkbox"
                  class="mt-0.5 accent-brand-purple"
                  :checked="checkedQuestions.has(q.id)"
                  @change="toggleQuestion(q.id)"
                />
                <span class="text-xs text-foreground leading-relaxed">{{ q.questionText }}</span>
              </label>
            </div>
          </div>
        </div>

        <!-- 中间：笔记编辑 -->
        <div class="flex-1 space-y-4">
          <div class="bg-card p-5 shadow-card border border-border">
            <div class="flex items-center justify-between mb-4">
              <div class="flex items-center gap-2">
                <ClipboardList :size="15" class="text-brand-blue" />
                <span class="text-base font-bold text-foreground">面试笔记</span>
              </div>
              <div class="flex gap-2">
                <button
                  type="button"
                  class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium border border-border hover:bg-muted disabled:opacity-50"
                  :disabled="!selectedInterviewId || saving"
                  @click="handleSave"
                >
                  <Loader2 v-if="saving" :size="13" class="animate-spin" />
                  <Save v-else :size="13" />
                  <span>{{ saving ? '保存中...' : '保存笔记' }}</span>
                </button>
                <button
                  type="button"
                  class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium gradient-purple text-white disabled:opacity-50"
                  :disabled="!selectedInterviewId || synthesizing"
                  @click="handleSynthesize"
                >
                  <Loader2 v-if="synthesizing" :size="13" class="animate-spin" />
                  <Sparkles v-else :size="13" />
                  <span>{{ synthesizing ? '生成中...' : 'AI 生成评估草稿' }}</span>
                </button>
              </div>
            </div>

            <div v-if="!selectedInterviewId && !loading" class="text-sm text-muted-foreground py-12 text-center">
              请从左侧选择一场待进行面试
            </div>
            <div v-else-if="loading" class="text-sm text-muted-foreground py-12 text-center">加载中...</div>
            <textarea
              v-else
              v-model="noteContent"
              class="w-full bg-muted rounded-xl p-4 text-sm text-foreground outline-none resize-none border border-transparent focus:border-brand-blue/40 min-h-[320px]"
              placeholder="记录面试过程中的关键观察，例如：&#10;- 候选人对 XX 项目的描述是否清晰&#10;- 技术深度验证结果&#10;- 沟通表达与团队协作表现&#10;- 需要 HR 进一步确认的点"
            />
            <p class="text-[10px] text-muted-foreground mt-2">
              提示：笔记越具体，AI 生成的评估草稿越准确
            </p>
          </div>
        </div>

        <!-- 右侧：AI 草稿 -->
        <div class="w-72 flex-shrink-0 space-y-4">
          <div class="bg-card p-5 shadow-card border border-border">
            <div class="flex items-center gap-2 mb-3">
              <Sparkles :size="14" class="text-brand-purple" />
              <span class="text-sm font-semibold text-foreground">AI 评估草稿</span>
            </div>

            <div v-if="!hasDraft" class="text-xs text-muted-foreground leading-relaxed">
              填写笔记后点击「AI 生成评估草稿」，系统将结合岗位匹配信息与面试题，自动生成评价摘要与建议结论。
            </div>

            <template v-else>
              <div class="text-center mb-4">
                <div class="text-3xl font-black gradient-purple-text">{{ draftScore ?? '—' }}</div>
                <div class="text-xs text-muted-foreground">建议综合分</div>
                <div
                  v-if="draftConclusion"
                  class="inline-block mt-2 text-xs px-2 py-0.5 rounded-full bg-brand-tint-2 text-brand-purple font-medium"
                >
                  {{ draftConclusion }}
                </div>
              </div>

              <p class="text-xs text-foreground leading-relaxed mb-4">{{ savedNote?.aiSummary }}</p>

              <div v-if="draftHighlights.length" class="space-y-2 mb-4">
                <div class="text-xs font-semibold text-foreground">关键信号</div>
                <div v-for="(h, i) in draftHighlights" :key="i" class="flex items-center gap-2">
                  <CheckCircle2 :size="11" class="text-brand-green flex-shrink-0" />
                  <span class="text-xs text-muted-foreground">{{ h }}</span>
                </div>
              </div>

              <button
                type="button"
                class="w-full flex items-center justify-center gap-2 py-2.5 rounded-xl gradient-blue text-white text-sm font-medium shadow-custom"
                @click="goToDetail"
              >
                <span>前往提交评价</span>
                <ArrowRight :size="14" />
              </button>
            </template>
          </div>

          <div v-if="selectedInterview" class="bg-accent rounded-2xl p-4 border border-brand-border/50">
            <div class="flex items-center gap-2 mb-2">
              <Lightbulb :size="13" class="text-brand-orange" />
              <span class="text-xs font-semibold text-brand-orange">使用建议</span>
            </div>
            <p class="text-xs text-foreground leading-relaxed">
              先在详情页生成 AI 面试题 → 此处记录现场笔记 → AI 生成草稿 → 跳转详情页确认并提交评价
            </p>
          </div>

          <div v-if="interviewDetail?.meetingUrl" class="bg-card p-4 shadow-card border border-border">
            <div class="flex items-center gap-2 mb-2">
              <Star :size="13" class="text-brand-blue" />
              <span class="text-xs font-semibold text-foreground">视频会议</span>
            </div>
            <a
              :href="interviewDetail.meetingUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="text-xs text-brand-blue hover:underline break-all"
            >
              {{ interviewDetail.meetingUrl }}
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
