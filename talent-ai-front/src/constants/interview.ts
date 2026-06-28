/** 1-待面试 2-已完成 3-待安排 4-已取消 */
export const INTERVIEW_STATUS = {
  PENDING: 1,
  COMPLETED: 2,
  TO_SCHEDULE: 3,
  CANCELLED: 4,
} as const

/** 面试评价必填维度（与后端 InterviewConstants 一致） */
export const EVALUATION_DIMENSION_KEYS = ['沟通能力', '专业技能', '岗位匹配度'] as const

export type EvaluationDimensionKey = (typeof EVALUATION_DIMENSION_KEYS)[number]

export const EVALUATION_DIMENSION_LABELS: Record<EvaluationDimensionKey, string> = {
  沟通能力: '沟通能力',
  专业技能: '专业技能',
  岗位匹配度: '岗位匹配度',
}

/** 1-视频 2-现场 3-线上评审 */
export const INTERVIEW_MODE = {
  VIDEO: 1,
  ONSITE: 2,
  ONLINE_REVIEW: 3,
} as const

/** 1-AI初筛 2-业务初试 3-业务复试 4-HR面 5-终面 6-交叉面 7-作品评审 */
export const INTERVIEW_ROUND_TYPE = {
  AI_SCREEN: 1,
  TECH_FIRST: 2,
  TECH_SECOND: 3,
  HR: 4,
  FINAL: 5,
  CROSS: 6,
  PORTFOLIO: 7,
} as const

/** 1-通过 2-待定 3-不通过 */
export const INTERVIEW_CONCLUSION = {
  PASS: 1,
  HOLD: 2,
  REJECT: 3,
} as const

export const ROUND_TYPE_OPTIONS = [
  { value: INTERVIEW_ROUND_TYPE.TECH_FIRST, label: '业务初试' },
  { value: INTERVIEW_ROUND_TYPE.TECH_SECOND, label: '业务复试' },
  { value: INTERVIEW_ROUND_TYPE.HR, label: 'HR初面' },
  { value: INTERVIEW_ROUND_TYPE.FINAL, label: '终面' },
  { value: INTERVIEW_ROUND_TYPE.CROSS, label: '交叉面' },
  { value: INTERVIEW_ROUND_TYPE.PORTFOLIO, label: '作品评审' },
]

export const INTERVIEW_MODE_OPTIONS = [
  { value: INTERVIEW_MODE.VIDEO, label: '视频面试' },
  { value: INTERVIEW_MODE.ONSITE, label: '现场面试' },
  { value: INTERVIEW_MODE.ONLINE_REVIEW, label: '线上评审' },
]

export function interviewStatusLabel(status?: number | null): string {
  switch (status) {
    case INTERVIEW_STATUS.PENDING:
      return '待面试'
    case INTERVIEW_STATUS.COMPLETED:
      return '已完成'
    case INTERVIEW_STATUS.TO_SCHEDULE:
      return '待安排'
    case INTERVIEW_STATUS.CANCELLED:
      return '已取消'
    default:
      return '未知'
  }
}

export function parseEvaluationDimensions(
  raw?: string | Record<string, number> | null,
): Partial<Record<EvaluationDimensionKey, number>> {
  if (!raw) return {}
  let parsed: Record<string, unknown> | null = null
  if (typeof raw === 'string') {
    try {
      parsed = JSON.parse(raw) as Record<string, unknown>
    } catch {
      return {}
    }
  } else {
    parsed = raw as Record<string, unknown>
  }
  const result: Partial<Record<EvaluationDimensionKey, number>> = {}
  for (const key of EVALUATION_DIMENSION_KEYS) {
    const value = parsed[key]
    if (typeof value === 'number' && Number.isFinite(value)) {
      result[key] = value
    } else if (value != null && value !== '') {
      const num = Number(value)
      if (Number.isFinite(num)) result[key] = num
    }
  }
  return result
}

export function defaultEvaluationDimensions(): Record<EvaluationDimensionKey, number> {
  return {
    沟通能力: 80,
    专业技能: 80,
    岗位匹配度: 80,
  }
}

export function averageEvaluationScore(scores: Partial<Record<EvaluationDimensionKey, number>>): number {
  const values = EVALUATION_DIMENSION_KEYS.map((key) => scores[key]).filter(
    (v): v is number => v != null && Number.isFinite(v),
  )
  if (values.length === 0) return 0
  return Math.round(values.reduce((sum, v) => sum + v, 0) / values.length)
}

/** 将 AI 面试笔记的五维建议分映射为正式评价的三维分 */
export function mapAiNoteDimensionsToEvaluation(
  aiDimensions?: Record<string, number> | null,
): Partial<Record<EvaluationDimensionKey, number>> {
  if (!aiDimensions) return {}
  const result: Partial<Record<EvaluationDimensionKey, number>> = {}
  const skill = aiDimensions['专业技能']
  if (skill != null && Number.isFinite(skill)) {
    result['专业技能'] = Math.min(100, Math.max(0, Math.round(skill)))
  }
  const comm = aiDimensions['沟通表达'] ?? aiDimensions['沟通能力']
  if (comm != null && Number.isFinite(comm)) {
    result['沟通能力'] = Math.min(100, Math.max(0, Math.round(comm)))
  }
  const matchCandidates = ['岗位匹配度', '逻辑思维', '团队合作', '学习能力']
    .map((key) => aiDimensions[key])
    .filter((v): v is number => v != null && Number.isFinite(v))
  if (matchCandidates.length) {
    const avg = matchCandidates.reduce((sum, v) => sum + v, 0) / matchCandidates.length
    result['岗位匹配度'] = Math.min(100, Math.max(0, Math.round(avg)))
  }
  return result
}

/** 组装 AI 草稿填入正式评语的文本 */
export function buildEvaluationCommentFromAiNote(note: {
  aiSummary?: string | null
  aiHighlights?: string[] | null
}): string {
  const parts: string[] = []
  if (note.aiSummary?.trim()) {
    parts.push(note.aiSummary.trim())
  }
  if (note.aiHighlights?.length) {
    parts.push('', '关键信号：', ...note.aiHighlights.map((item) => `· ${item}`))
  }
  return parts.join('\n').trim()
}

export function interviewStatusClass(status?: number | null): string {
  switch (status) {
    case INTERVIEW_STATUS.PENDING:
      return 'bg-brand-tint text-brand-blue border-brand-border'
    case INTERVIEW_STATUS.COMPLETED:
      return 'bg-green-50 text-brand-green border-green-200'
    case INTERVIEW_STATUS.TO_SCHEDULE:
      return 'bg-orange-50 text-brand-orange border-orange-200'
    case INTERVIEW_STATUS.CANCELLED:
      return 'bg-muted text-muted-foreground border-border'
    default:
      return 'bg-muted text-muted-foreground border-border'
  }
}

export function conclusionLabel(conclusion?: number | null): string {
  switch (conclusion) {
    case INTERVIEW_CONCLUSION.PASS:
      return '通过'
    case INTERVIEW_CONCLUSION.HOLD:
      return '待定'
    case INTERVIEW_CONCLUSION.REJECT:
      return '不通过'
    default:
      return '—'
  }
}

/** datetime-local → ISO 本地时间字符串（无 Z） */
export function toLocalDateTimeString(value: string): string {
  if (!value) return ''
  if (value.length === 16) return `${value}:00`
  return value
}

export function formatInterviewDateTime(iso?: string | null): string {
  if (!iso) return '—'
  const normalized = iso.replace('T', ' ')
  return normalized.length >= 16 ? normalized.slice(0, 16) : normalized
}
