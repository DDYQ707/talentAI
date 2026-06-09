<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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
  Mic,
  Lightbulb,
  ClipboardList,
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
  formatInterviewDateTime,
  interviewStatusLabel,
} from '@/constants/interview'
import { getErrorMessage } from '@/utils/validators'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const errorMsg = ref('')
const detail = ref<InterviewDetail | null>(null)
const aiMatch = ref<AiMatchResult | null>(null)
const aiLoading = ref(false)

const comment = ref('')
const submitting = ref(false)
const submitSuccess = ref('')

const interviewId = computed(() => {
  const id = Number(route.query.id)
  return Number.isFinite(id) && id > 0 ? id : null
})

const radarData = computed(() => parseDimensionScores(aiMatch.value?.dimensionScores))

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

const questions = computed(() => parseJsonStringArray(aiMatch.value?.suggestedQuestions))
const advantages = computed(() => parseJsonStringArray(aiMatch.value?.advantages))
const disadvantages = computed(() => parseJsonStringArray(aiMatch.value?.disadvantages))

const matchScore = computed(() => aiMatch.value?.matchScore ?? 0)

const canEvaluate = computed(
  () => detail.value?.status === INTERVIEW_STATUS.PENDING && !detail.value?.evaluation,
)

const hasEvaluation = computed(() => !!detail.value?.evaluation)

async function loadDetail() {
  if (!interviewId.value) {
    errorMsg.value = '缺少面试 ID'
    loading.value = false
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    detail.value = await fetchMyInterviewDetail(interviewId.value)
    comment.value = detail.value.evaluation?.comment ?? ''
    await loadAiMatch(detail.value.applicationId)
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

async function handleSubmit(conclusion: number) {
  if (!interviewId.value || submitting.value || !canEvaluate.value) return
  submitting.value = true
  submitSuccess.value = ''
  errorMsg.value = ''
  try {
    await submitInterviewEvaluation(interviewId.value, {
      overallScore: matchScore.value > 0 ? matchScore.value : 80,
      conclusion,
      comment: comment.value.trim() || undefined,
      dimensionScores: radarData.value.length
        ? Object.fromEntries(radarData.value.map((d) => [d.subject, d.value]))
        : undefined,
    })
    submitSuccess.value = '评价已提交'
    await loadDetail()
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '评价提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => loadDetail())
</script>

<template>
  <div data-cmp="InterviewDetail" class="h-full overflow-y-auto scrollbar-thin p-8">
    <div class="max-w-6xl mx-auto">
      <div class="flex items-center justify-between mb-8">
        <div>
          <div class="flex items-center gap-2 text-sm text-muted-foreground mb-1">
            <button type="button" class="cursor-pointer hover:text-brand-blue" @click="router.push('/interviewer')">
              面试列表
            </button>
            <ChevronRight :size="13" />
            <span class="text-foreground">{{ detail?.candidateName ?? '面试详情' }}</span>
          </div>
          <h1 v-if="detail" class="text-2xl font-black text-foreground">
            {{ detail.candidateName }} — {{ detail.jobTitle }}
          </h1>
          <h1 v-else class="text-2xl font-black text-foreground">面试详情</h1>
        </div>
        <div class="flex gap-3">
          <button
            type="button"
            class="flex items-center gap-2 px-4 py-2 gradient-purple rounded-xl text-white text-sm font-medium shadow-custom"
            @click="router.push('/interviewer/ai-mode')"
          >
            <Mic :size="15" />
            <span>进入AI面试</span>
          </button>
        </div>
      </div>

      <p v-if="errorMsg" class="text-xs text-red-600 mb-4">{{ errorMsg }}</p>
      <p v-if="loading" class="text-sm text-muted-foreground mb-4">加载中...</p>
      <p v-if="submitSuccess" class="text-xs text-brand-green mb-4">{{ submitSuccess }}</p>

      <div v-if="detail" class="flex gap-6">
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
              <div class="flex items-center gap-2 text-xs">
                <Briefcase :size="12" class="text-muted-foreground flex-shrink-0" />
                <span class="text-muted-foreground">应聘岗位：</span>
                <span class="text-foreground font-medium">{{ detail.jobTitle }}</span>
              </div>
              <div class="flex items-center gap-2 text-xs">
                <Calendar :size="12" class="text-muted-foreground flex-shrink-0" />
                <span class="text-muted-foreground">面试时间：</span>
                <span class="text-foreground font-medium">{{ formatInterviewDateTime(detail.scheduledStart) }}</span>
              </div>
              <div class="flex items-center gap-2 text-xs">
                <Video :size="12" class="text-muted-foreground flex-shrink-0" />
                <span class="text-muted-foreground">面试形式：</span>
                <span class="text-foreground font-medium">{{ detail.interviewModeLabel }}</span>
              </div>
            </div>
          </div>

          <div class="bg-card p-5 shadow-card border border-border">
            <div class="flex items-center gap-2 mb-3">
              <Sparkles :size="14" class="text-brand-purple" />
              <span class="text-sm font-semibold text-foreground">AI能力雷达</span>
            </div>
            <p v-if="aiLoading" class="text-xs text-muted-foreground mb-2">AI 数据加载中...</p>
            <VChart v-if="radarData.length" :option="radarOption" autoresize style="height: 180px" />
            <p v-else-if="!aiLoading" class="text-xs text-muted-foreground mb-2">暂无 AI 维度数据</p>
            <div class="text-center">
              <div class="flex items-center justify-center gap-1">
                <Star :size="12" class="text-brand-orange" />
                <span class="text-lg font-black text-foreground">{{ matchScore || '—' }}</span>
                <span v-if="matchScore" class="text-sm text-muted-foreground">/ 100</span>
              </div>
              <div class="text-xs text-muted-foreground">AI综合评估分</div>
            </div>
          </div>

          <div v-if="advantages.length" class="bg-card p-4 shadow-card border border-border">
            <div class="text-xs font-semibold text-brand-green mb-2">匹配优势</div>
            <ul class="space-y-1">
              <li v-for="(item, i) in advantages" :key="i" class="text-xs text-muted-foreground">· {{ item }}</li>
            </ul>
          </div>
        </div>

        <div class="flex-1 space-y-4">
          <div class="bg-card p-5 shadow-card border border-border">
            <div class="flex items-center gap-2 mb-4">
              <Sparkles :size="15" class="text-brand-purple" />
              <span class="text-base font-bold text-foreground">AI推荐面试题</span>
            </div>
            <div v-if="questions.length === 0" class="text-sm text-muted-foreground">暂无推荐面试题</div>
            <div v-else class="space-y-3">
              <div
                v-for="(q, i) in questions"
                :key="i"
                class="border border-border rounded-xl p-4 hover:border-brand-purple/30 transition-colors"
              >
                <div class="flex items-start gap-3">
                  <div class="w-6 h-6 rounded-lg bg-brand-tint-2 flex items-center justify-center flex-shrink-0">
                    <span class="text-xs text-brand-purple font-bold">{{ i + 1 }}</span>
                  </div>
                  <div class="flex-1">
                    <div class="text-sm text-foreground font-medium mb-1">{{ q }}</div>
                    <div v-if="disadvantages[i]" class="flex items-start gap-1.5 text-xs text-muted-foreground mt-2">
                      <Lightbulb :size="11" class="text-brand-orange mt-0.5 flex-shrink-0" />
                      <span>关联待提升：{{ disadvantages[i] }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="bg-card p-5 shadow-card border border-border">
            <div class="flex items-center gap-2 mb-4">
              <ClipboardList :size="15" class="text-brand-blue" />
              <span class="text-base font-bold text-foreground">面试评价</span>
            </div>

            <div v-if="hasEvaluation" class="space-y-2 text-sm">
              <p>
                <span class="text-muted-foreground">综合评分：</span>
                <span class="font-bold text-brand-purple">{{ detail.evaluation?.overallScore }}</span>
              </p>
              <p>
                <span class="text-muted-foreground">结论：</span>
                {{ detail.evaluation?.conclusionLabel }}
              </p>
              <p v-if="detail.evaluation?.comment" class="text-muted-foreground leading-relaxed">
                {{ detail.evaluation.comment }}
              </p>
            </div>

            <template v-else-if="canEvaluate">
              <textarea
                v-model="comment"
                class="w-full bg-muted rounded-xl p-4 text-sm text-foreground outline-none resize-none border border-transparent focus:border-brand-blue/40"
                rows="4"
                placeholder="记录面试过程中的观察与感受..."
              />
              <div class="flex gap-3 mt-3">
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
