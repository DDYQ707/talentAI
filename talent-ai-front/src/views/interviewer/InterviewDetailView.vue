<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import type { EChartsOption } from 'echarts'
import { useRoute, useRouter } from 'vue-router'
import {
  Sparkles,
  User,
  Briefcase,
  Calendar,
  Video,
  Star,
  ChevronRight,
  ClipboardList,
  ExternalLink,
  Copy,
  MapPin,
} from 'lucide-vue-next'
import {
  fetchAiMatchByApplication,
  parseDimensionScores,
  parseJsonStringArray,
  type AiMatchResult,
} from '@/api/ai'
import {
  fetchMyInterviewDetail,
  submitInterviewEvaluation,
  type InterviewDetail,
} from '@/api/interview'
import {
  INTERVIEW_CONCLUSION,
  INTERVIEW_STATUS,
  EVALUATION_DIMENSION_KEYS,
  averageEvaluationScore,
  defaultEvaluationDimensions,
  formatInterviewDateTime,
  interviewStatusLabel,
  parseEvaluationDimensions,
} from '@/constants/interview'
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
const errorMsg = ref('')
const copyTip = ref('')
const detail = ref<InterviewDetail | null>(null)
const aiMatch = ref<AiMatchResult | null>(null)
const aiLoading = ref(false)

const comment = ref('')
const dimensionScores = ref(defaultEvaluationDimensions())
const submitting = ref(false)
const submitSuccess = ref('')

const computedOverallScore = computed(() => averageEvaluationScore(dimensionScores.value))
const submittedDimensions = computed(() =>
  parseEvaluationDimensions(detail.value?.evaluation?.dimensionScores),
)

const interviewId = computed(() => {
  const id = Number(route.query.id)
  return Number.isFinite(id) && id > 0 ? id : null
})

const radarData = computed(() => parseDimensionScores(aiMatch.value?.dimensionScores))
const advantages = computed(() => parseJsonStringArray(aiMatch.value?.advantages))
const disadvantages = computed(() => parseJsonStringArray(aiMatch.value?.disadvantages))
const matchScore = computed(() => aiMatch.value?.matchScore ?? 0)

const radarOption = computed<EChartsOption>(() => ({
  radar: {
    indicator: radarData.value.length
      ? radarData.value.map((d) => ({ name: d.subject, max: 100 }))
      : [{ name: '暂无', max: 100 }],
    splitLine: { lineStyle: { color: 'var(--border)' } },
    axisName: { color: 'var(--muted-foreground)', fontSize: 10 },
  },
  series: [
    {
      type: 'radar',
      data: [
        {
          value: radarData.value.length ? radarData.value.map((d) => d.value) : [0],
          name: 'score',
          areaStyle: { color: 'rgba(124,58,237,0.25)' },
          lineStyle: { color: '#5a8a82', width: 2 },
        },
      ],
    },
  ],
}))

const canEvaluate = computed(
  () => detail.value?.status === INTERVIEW_STATUS.PENDING && !detail.value?.evaluation,
)
const hasEvaluation = computed(() => !!detail.value?.evaluation)
const showMeetingBar = computed(() => detail.value && canJoinMeeting(detail.value))

async function loadDetail() {
  if (!interviewId.value) {
    loading.value = false
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    detail.value = await fetchMyInterviewDetail(interviewId.value)
    comment.value = detail.value.evaluation?.comment ?? ''
    await loadAiMatch(detail.value.applicationId)
    if (detail.value.evaluation?.dimensionScores) {
      const parsed = parseEvaluationDimensions(detail.value.evaluation.dimensionScores)
      dimensionScores.value = {
        ...defaultEvaluationDimensions(),
        ...parsed,
      }
    } else {
      dimensionScores.value = defaultEvaluationDimensions()
    }
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '面试详情加载失败')
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function loadAiMatch(applicationId: number) {
  aiLoading.value = true
  try {
    aiMatch.value = await fetchAiMatchByApplication(applicationId)
  } catch {
    aiMatch.value = null
  } finally {
    aiLoading.value = false
  }
}

function openPrep() {
  if (!interviewId.value) return
  router.push({ path: '/interviewer/prep', query: { id: String(interviewId.value) } })
}

function handleJoinMeeting() {
  if (!detail.value?.meetingUrl) return
  openMeeting(detail.value.meetingUrl)
}

async function handleCopyMeeting() {
  if (!detail.value) return
  const ok = await copyMeetingInfo(buildMeetingCopyText(detail.value))
  copyTip.value = ok ? '会议信息已复制' : '复制失败'
  setTimeout(() => {
    copyTip.value = ''
  }, 2000)
}

async function handleSubmit(conclusion: number) {
  if (!interviewId.value || submitting.value || !canEvaluate.value) return
  submitting.value = true
  submitSuccess.value = ''
  errorMsg.value = ''
  try {
    await submitInterviewEvaluation(interviewId.value, {
      conclusion,
      comment: comment.value.trim() || undefined,
      dimensionScores: { ...dimensionScores.value },
    })
    submitSuccess.value = '评价已提交'
    await loadDetail()
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '评价提交失败')
  } finally {
    submitting.value = false
  }
}

watch(interviewId, () => loadDetail())
onMounted(() => loadDetail())
</script>

<template>
  <div data-cmp="InterviewDetail" class="h-full overflow-y-auto scrollbar-thin p-8">
    <div class="max-w-6xl mx-auto">
      <div class="flex items-center justify-between mb-6">
        <div>
          <div class="flex items-center gap-2 text-sm text-muted-foreground mb-1">
            <button type="button" class="cursor-pointer hover:text-brand-blue" @click="router.push('/interviewer/interviews')">
              我的面试
            </button>
            <ChevronRight :size="13" />
            <span class="text-foreground">{{ detail?.candidateName ?? '面试详情' }}</span>
          </div>
          <h1 v-if="detail" class="text-2xl font-black text-foreground">
            {{ detail.candidateName }} — {{ detail.jobTitle }}
          </h1>
          <h1 v-else class="text-2xl font-black text-foreground">面试详情</h1>
        </div>
        <button
          v-if="canEvaluate"
          type="button"
          class="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium border border-brand-purple/30 text-brand-purple hover:bg-brand-tint-2"
          @click="openPrep"
        >
          <Sparkles :size="15" />
          <span>面试准备</span>
        </button>
      </div>

      <p v-if="errorMsg" class="text-xs text-red-600 mb-4">{{ errorMsg }}</p>
      <p v-if="copyTip" class="text-xs text-brand-green mb-4">{{ copyTip }}</p>
      <p v-if="loading" class="text-sm text-muted-foreground mb-4">加载中...</p>
      <p v-if="submitSuccess" class="text-xs text-brand-green mb-4">{{ submitSuccess }}</p>

      <div
        v-if="showMeetingBar"
        class="mb-6 flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-brand-blue/30 bg-brand-tint px-5 py-4"
      >
        <div class="min-w-0">
          <div class="text-sm font-semibold text-foreground">视频会议</div>
          <p class="text-xs text-muted-foreground mt-1 break-all">{{ detail?.meetingUrl }}</p>
        </div>
        <div class="flex gap-2 flex-shrink-0">
          <button
            type="button"
            class="flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-medium gradient-blue text-white shadow-custom"
            @click="handleJoinMeeting"
          >
            <ExternalLink :size="14" />
            进入会议
          </button>
          <button
            type="button"
            class="flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-medium border border-border bg-card hover:bg-muted"
            @click="handleCopyMeeting"
          >
            <Copy :size="14" />
            复制信息
          </button>
        </div>
      </div>

      <div
        v-if="!interviewId && !loading"
        class="bg-card border border-border rounded-2xl p-10 text-center shadow-card max-w-md mx-auto mt-16"
      >
        <p class="text-sm text-muted-foreground mb-4">请从面试列表中选择一场面试查看详情</p>
        <button
          type="button"
          class="px-5 py-2.5 rounded-xl gradient-purple text-white text-sm font-medium shadow-custom"
          @click="router.push('/interviewer/interviews')"
        >
          返回面试列表
        </button>
      </div>

      <div v-else-if="detail" class="flex gap-6">
        <div class="w-72 flex-shrink-0 space-y-4">
          <div class="bg-card p-5 shadow-card border border-border">
            <div class="text-center mb-4">
              <div class="w-16 h-16 rounded-control gradient-blue mx-auto flex items-center justify-center mb-3">
                <User :size="28" class="text-white" />
              </div>
              <div class="text-base font-bold text-foreground">{{ detail.candidateName }}</div>
              <div class="text-sm text-muted-foreground">{{ interviewStatusLabel(detail.status) }}</div>
            </div>
            <div class="space-y-2">
              <div class="flex items-start gap-2 text-xs">
                <Briefcase :size="12" class="text-muted-foreground flex-shrink-0 mt-0.5" />
                <div>
                  <span class="text-muted-foreground">应聘岗位：</span>
                  <span class="text-foreground font-medium">{{ detail.jobTitle }}</span>
                </div>
              </div>
              <div class="flex items-start gap-2 text-xs">
                <Calendar :size="12" class="text-muted-foreground flex-shrink-0 mt-0.5" />
                <div>
                  <span class="text-muted-foreground">面试时间：</span>
                  <span class="text-foreground font-medium">{{ formatInterviewDateTime(detail.scheduledStart) }}</span>
                </div>
              </div>
              <div class="flex items-start gap-2 text-xs">
                <Video :size="12" class="text-muted-foreground flex-shrink-0 mt-0.5" />
                <div>
                  <span class="text-muted-foreground">面试形式：</span>
                  <span class="text-foreground font-medium">{{ detail.interviewModeLabel }}</span>
                </div>
              </div>
              <div v-if="detail.location" class="flex items-start gap-2 text-xs">
                <MapPin :size="12" class="text-muted-foreground flex-shrink-0 mt-0.5" />
                <div>
                  <span class="text-muted-foreground">地点：</span>
                  <span class="text-foreground font-medium">{{ detail.location }}</span>
                </div>
              </div>
              <div class="flex items-start gap-2 text-xs">
                <ClipboardList :size="12" class="text-muted-foreground flex-shrink-0 mt-0.5" />
                <div>
                  <span class="text-muted-foreground">面试轮次：</span>
                  <span class="text-foreground font-medium">{{ detail.roundTypeLabel }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="bg-card p-5 shadow-card border border-border">
            <div class="flex items-center gap-2 mb-3">
              <Sparkles :size="14" class="text-brand-purple" />
              <span class="text-sm font-semibold text-foreground">AI 初筛参考</span>
            </div>
            <p v-if="aiLoading" class="text-xs text-muted-foreground mb-2">加载中...</p>
            <VChart v-if="radarData.length" :option="radarOption" autoresize style="height: 160px" />
            <div class="text-center mt-2">
              <div class="flex items-center justify-center gap-1">
                <Star :size="12" class="text-brand-orange" />
                <span class="text-lg font-black text-foreground">{{ matchScore || '—' }}</span>
                <span v-if="matchScore" class="text-sm text-muted-foreground">/ 100</span>
              </div>
              <div class="text-xs text-muted-foreground">综合匹配分（只读）</div>
            </div>
          </div>

          <div v-if="advantages.length" class="bg-card p-4 shadow-card border border-border">
            <div class="text-xs font-semibold text-brand-green mb-2">匹配优势</div>
            <ul class="space-y-1">
              <li v-for="(item, i) in advantages" :key="i" class="text-xs text-muted-foreground">· {{ item }}</li>
            </ul>
          </div>

          <div v-if="disadvantages.length" class="bg-card p-4 shadow-card border border-border">
            <div class="text-xs font-semibold text-brand-orange mb-2">待验证点</div>
            <ul class="space-y-1">
              <li v-for="(item, i) in disadvantages" :key="i" class="text-xs text-muted-foreground">· {{ item }}</li>
            </ul>
          </div>
        </div>

        <div class="flex-1">
          <div class="bg-card p-5 shadow-card border border-border">
            <div class="flex items-center gap-2 mb-4">
              <ClipboardList :size="15" class="text-brand-blue" />
              <span class="text-base font-bold text-foreground">面试评价</span>
            </div>

            <div v-if="hasEvaluation" class="space-y-3 text-sm">
              <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div
                  v-for="key in EVALUATION_DIMENSION_KEYS"
                  :key="key"
                  class="rounded-xl bg-muted/60 px-3 py-2"
                >
                  <div class="text-xs text-muted-foreground">{{ key }}</div>
                  <div class="text-base font-bold text-brand-purple mt-0.5">
                    {{ submittedDimensions[key] ?? '—' }}
                  </div>
                </div>
              </div>
              <p>
                <span class="text-muted-foreground">综合评分：</span>
                <span class="font-bold text-brand-purple">{{ detail.evaluation?.overallScore }}</span>
              </p>
              <p>
                <span class="text-muted-foreground">结论：</span>
                {{ detail.evaluation?.conclusionLabel }}
              </p>
              <p v-if="detail.evaluation?.comment" class="text-muted-foreground leading-relaxed whitespace-pre-wrap">
                {{ detail.evaluation.comment }}
              </p>
            </div>

            <template v-else-if="canEvaluate">
              <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-4">
                <div v-for="key in EVALUATION_DIMENSION_KEYS" :key="key">
                  <label class="text-xs text-muted-foreground mb-1.5 block">{{ key }}（0-100）</label>
                  <input
                    v-model.number="dimensionScores[key]"
                    type="number"
                    min="0"
                    max="100"
                    class="w-full bg-muted rounded-xl px-3 py-2 text-sm text-foreground outline-none border border-transparent focus:border-brand-blue/40"
                  />
                </div>
              </div>
              <p class="text-xs text-muted-foreground mb-4">
                综合评分（自动）：<span class="font-semibold text-brand-purple">{{ computedOverallScore }}</span> 分
              </p>
              <textarea
                v-model="comment"
                class="w-full bg-muted rounded-xl p-4 text-sm text-foreground outline-none resize-none border border-transparent focus:border-brand-blue/40"
                rows="5"
                placeholder="记录面试观察：技术深度、沟通表达、项目经验验证、团队协作等..."
              />
              <p class="text-[10px] text-muted-foreground mt-2">
                提示：可在「面试准备」页生成 AI 面试提纲，结束后在此提交评价
              </p>
              <div class="flex gap-3 mt-4">
                <button
                  type="button"
                  class="flex-1 py-2.5 rounded-xl bg-red-50 text-brand-red text-sm font-medium border border-red-100 hover:bg-red-100 disabled:opacity-50"
                  :disabled="submitting"
                  @click="handleSubmit(INTERVIEW_CONCLUSION.REJECT)"
                >
                  不推荐
                </button>
                <button
                  type="button"
                  class="flex-1 py-2.5 rounded-xl bg-orange-50 text-brand-orange text-sm font-medium border border-orange-100 hover:bg-orange-100 disabled:opacity-50"
                  :disabled="submitting"
                  @click="handleSubmit(INTERVIEW_CONCLUSION.HOLD)"
                >
                  待定
                </button>
                <button
                  type="button"
                  class="flex-1 py-2.5 rounded-xl bg-green-50 text-brand-green text-sm font-medium border border-green-100 hover:bg-green-100 disabled:opacity-50"
                  :disabled="submitting"
                  @click="handleSubmit(INTERVIEW_CONCLUSION.PASS)"
                >
                  {{ submitting ? '提交中...' : '推荐通过' }}
                </button>
              </div>
            </template>

            <p v-else class="text-sm text-muted-foreground">该面试当前不可评价</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
