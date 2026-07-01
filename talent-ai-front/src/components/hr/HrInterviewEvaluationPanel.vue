<script setup lang="ts">
import { computed } from 'vue'
import { ClipboardList } from 'lucide-vue-next'
import type { InterviewEvaluation } from '@/api/interview'
import {
  EVALUATION_DIMENSION_KEYS,
  formatInterviewDateTime,
  parseEvaluationDimensions,
} from '@/constants/interview'

const props = defineProps<{
  evaluation?: InterviewEvaluation | null
  interviewerName?: string | null
  roundTypeLabel?: string | null
  scheduledStart?: string | null
  compact?: boolean
}>()

const dimensions = computed(() => parseEvaluationDimensions(props.evaluation?.dimensionScores))
const hasEvaluation = computed(() => !!props.evaluation)
</script>

<template>
  <div v-if="hasEvaluation" :class="compact ? 'space-y-2' : 'space-y-3'">
    <div v-if="interviewerName || roundTypeLabel || scheduledStart" class="text-xs text-muted-foreground">
      <span v-if="interviewerName">面试官：{{ interviewerName }}</span>
      <span v-if="roundTypeLabel"> · {{ roundTypeLabel }}</span>
      <span v-if="scheduledStart"> · {{ formatInterviewDateTime(scheduledStart) }}</span>
    </div>
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-2">
      <div
        v-for="key in EVALUATION_DIMENSION_KEYS"
        :key="key"
        class="rounded-xl bg-muted/60 px-3 py-2"
      >
        <div class="text-xs text-muted-foreground">{{ key }}</div>
        <div class="text-base font-bold text-brand-purple mt-0.5">
          {{ dimensions[key] ?? '—' }}
        </div>
      </div>
    </div>
    <div class="flex flex-wrap items-center gap-3 text-sm">
      <span>
        <span class="text-muted-foreground">综合评分：</span>
        <span class="font-bold text-brand-purple">{{ evaluation?.overallScore ?? '—' }}</span>
      </span>
      <span v-if="evaluation?.conclusionLabel">
        <span class="text-muted-foreground">结论：</span>
        {{ evaluation.conclusionLabel }}
      </span>
      <span v-if="evaluation?.evaluatorName" class="text-xs text-muted-foreground">
        评价人：{{ evaluation.evaluatorName }}
      </span>
    </div>
    <p
      v-if="evaluation?.comment"
      class="text-sm text-muted-foreground leading-relaxed whitespace-pre-wrap rounded-xl bg-muted/40 px-3 py-2"
    >
      {{ evaluation.comment }}
    </p>
  </div>
  <p v-else class="text-xs text-muted-foreground">暂无面试官评价</p>
</template>
