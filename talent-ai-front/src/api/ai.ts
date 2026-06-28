import request from '@/utils/request'

/** 0-待处理 1-处理中 2-成功 3-失败 */
export type AiTaskStatus = 0 | 1 | 2 | 3

export interface AiParseTaskResult {
  taskId: number
  resumeId: number
  attachmentId?: number
  applicationId?: number | null
  candidateId?: number | null
  taskStatus: AiTaskStatus
  errorMessage?: string | null
  rawTextLength?: number
  fileName?: string
  fileType?: string
  startedAt?: string
  finishedAt?: string
}

export interface AiMatchResult {
  matchId: number
  applicationId: number
  jobId: number
  resumeId: number
  matchScore: number
  matchStatus: AiTaskStatus
  matchLevel?: string | null
  matchReason?: string | null
  advantages?: string | null
  disadvantages?: string | null
  suggestedQuestions?: string | null
  dimensionScores?: string | null
  errorMessage?: string | null
  startedAt?: string
  finishedAt?: string
  createdAt?: string
}

export function fetchAiParseLatest(resumeId: number) {
  return request.get<AiParseTaskResult | null>('/api/ai/parse/latest', {
    params: { resumeId },
  }) as Promise<AiParseTaskResult | null>
}

export interface AiParseRetryPayload {
  resumeId: number
  applicationId?: number | null
  jobId?: number | null
  candidateId?: number | null
}

/** HR 手动重新解析简历 */
export function retryAiParse(payload: AiParseRetryPayload) {
  return request.post<AiParseTaskResult>('/api/ai/parse/retry', payload) as Promise<AiParseTaskResult>
}

export function fetchAiMatchByApplication(applicationId: number) {
  return request.get<AiMatchResult | null>('/api/ai/match/by-application', {
    params: { applicationId },
  }) as Promise<AiMatchResult | null>
}

export function fetchAiMatchLatest(resumeId: number, jobId: number) {
  return request.get<AiMatchResult | null>('/api/ai/match/latest', {
    params: { resumeId, jobId },
  }) as Promise<AiMatchResult | null>
}

export interface AiMatchTriggerPayload {
  resumeId: number
  jobId: number
  applicationId?: number | null
  candidateId?: number | null
}

/** HR 手动触发人岗匹配分析 */
export function triggerAiMatch(payload: AiMatchTriggerPayload) {
  return request.post<AiMatchResult>('/api/ai/match/trigger', payload, {
    timeout: 30000,
  }) as Promise<AiMatchResult>
}

/** 按分数映射匹配等级（与后端 MatchLevelUtil 一致） */
export function formatMatchLevel(score?: number | null, fallback?: string | null): string {
  if (score != null && score > 0) {
    if (score >= 80) return '强烈推荐'
    if (score >= 60) return '可考虑'
    return '暂不匹配'
  }
  return fallback?.trim() || '—'
}

/** 匹配失败时的友好提示 */
export function formatMatchErrorMessage(errorMessage?: string | null): string {
  if (!errorMessage) return 'AI服务繁忙，请稍后重试'
  const lower = errorMessage.toLowerCase()
  if (
    lower.includes('timeout')
    || lower.includes('timed out')
    || lower.includes('超时')
    || lower.includes('繁忙')
    || lower.includes('rate limit')
  ) {
    return 'AI服务繁忙，请稍后重试'
  }
  return errorMessage
}

/** 查询岗位预览匹配（不触发新任务） */
export function fetchPreviewMatch(jobId: number) {
  return request.get<AiMatchResult | null>('/api/ai/match/preview', {
    params: { jobId },
  }) as Promise<AiMatchResult | null>
}

/** 批量查询预览匹配缓存 */
export function fetchPreviewMatchBatch(jobIds: number[]) {
  if (!jobIds.length) {
    return Promise.resolve({} as Record<number, AiMatchResult>)
  }
  return request.get<Record<number, AiMatchResult>>('/api/ai/match/preview/batch', {
    params: { jobIds: jobIds.join(',') },
  }) as Promise<Record<number, AiMatchResult>>
}

/** 触发岗位预览匹配（无需投递） */
export function triggerPreviewMatch(jobId: number) {
  return request.post<AiMatchResult>('/api/ai/match/preview', null, {
    params: { jobId },
  }) as Promise<AiMatchResult>
}

/** HR 工作台 — AI 今日洞察 GET /api/ai/hr/insights */
export interface HrAiInsights {
  parsedToday: number
  parsedThisMonth: number
  highMatchCount: number
  matchSuccessThisMonth: number
  pendingAiTasks: number
  failedToday: number
  healthScore: number
  healthLabel: string
  summary: string
}

export function fetchHrAiInsights() {
  return request.get<HrAiInsights>('/api/ai/hr/insights') as Promise<HrAiInsights>
}

export interface AiInterviewQuestion {
  id: number
  interviewId: number
  questionText: string
  category?: string | null
  focusPoint?: string | null
  sortOrder?: number
  createdAt?: string
}

export interface AiInterviewQuestionGenerateResult {
  interviewId: number
  applicationId: number
  jobId: number
  resumeId: number
  candidateName?: string | null
  jobTitle?: string | null
  questions: AiInterviewQuestion[]
}

export function fetchAiInterviewQuestions(interviewId: number) {
  return request.get<AiInterviewQuestion[]>('/api/ai/interview-questions', {
    params: { interviewId },
  }) as Promise<AiInterviewQuestion[]>
}

export function generateAiInterviewQuestions(payload: {
  interviewId?: number
  applicationId?: number
}) {
  return request.post<AiInterviewQuestionGenerateResult>(
    '/api/ai/interview-questions/generate',
    payload,
  ) as Promise<AiInterviewQuestionGenerateResult>
}

export interface AiInterviewNote {
  id: number
  interviewId: number
  interviewerId: number
  noteContent?: string | null
  aiSummary?: string | null
  aiSuggestedScore?: number | null
  aiSuggestedConclusion?: number | null
  aiSuggestedConclusionLabel?: string | null
  aiDimensionScores?: Record<string, number>
  aiHighlights?: string[]
  updatedAt?: string
}

export async function fetchAiInterviewNote(interviewId: number) {
  const raw = await request.get<AiInterviewNote | { data?: AiInterviewNote | null } | null>(
    '/api/ai/interview-notes',
    { params: { interviewId } },
  )
  if (raw && typeof raw === 'object' && 'interviewId' in raw) {
    return raw as AiInterviewNote
  }
  if (raw && typeof raw === 'object' && 'data' in raw) {
    const nested = (raw as { data?: AiInterviewNote | null }).data
    return nested ?? null
  }
  return null
}

export function saveAiInterviewNote(payload: { interviewId: number; noteContent?: string }) {
  return request.put<AiInterviewNote>('/api/ai/interview-notes', payload) as Promise<AiInterviewNote>
}

export function synthesizeAiInterviewNote(payload: { interviewId: number; noteContent?: string }) {
  return request.post<AiInterviewNote>(
    '/api/ai/interview-notes/synthesize',
    payload,
    { timeout: 60000 },
  ) as Promise<AiInterviewNote>
}

export interface AiTalentProfile {
  profileId: number
  candidateId: number
  applicationId: number
  profileSummary: string
  profileTags?: string[]
  version?: number
  createdAt?: string
  updatedAt?: string
}

export function fetchAiProfileByApplication(applicationId: number) {
  return request.get<AiTalentProfile | null>('/api/ai/profile/by-application', {
    params: { applicationId },
  }) as Promise<AiTalentProfile | null>
}

export function generateAiProfile(payload: {
  applicationId: number
  candidateId?: number
  candidateName?: string
  resumeId?: number
  jobId?: number
  jobTitle?: string
  status?: number
}) {
  return request.post<AiTalentProfile>('/api/ai/profile/generate', payload) as Promise<AiTalentProfile>
}

/** 解析后端 JSON 字符串数组字段 */
export function parseJsonStringArray(raw?: string | null): string[] {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed.map((item) => String(item)).filter(Boolean)
  } catch {
    return []
  }
}

/** 解析维度得分 → 雷达图数据 */
export function parseDimensionScores(raw?: string | null): { subject: string; value: number }[] {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object') return []
    const labelMap: Record<string, string> = {
      skill: '技能匹配',
      experience: '经验匹配',
      education: '学历匹配',
      industry: '行业匹配',
      overall: '综合',
    }
    return Object.entries(parsed as Record<string, unknown>)
      .map(([key, val]) => ({
        subject: labelMap[key] ?? key,
        value: Math.min(100, Math.max(0, Number(val) || 0)),
      }))
      .filter((item) => item.value > 0)
  } catch {
    return []
  }
}

export function aiTaskStatusLabel(status?: AiTaskStatus | null): string {
  switch (status) {
    case 0:
      return '待处理'
    case 1:
      return '处理中'
    case 2:
      return '已完成'
    case 3:
      return '失败'
    default:
      return '未知'
  }
}

export interface AiResumeQualityScore {
  scoreId: number
  resumeId: number
  candidateId: number
  parseTaskId?: number | null
  qualityScore: number
  summary?: string | null
  strengths?: string[]
  weaknesses?: string[]
  suggestions?: string[]
  dimensionScores?: Record<string, number>
  evaluatedAt?: string
}

export function fetchAiResumeQualityLatest(resumeId: number) {
  return request.get<AiResumeQualityScore | null>('/api/ai/resume-score/latest', {
    params: { resumeId },
  }) as Promise<AiResumeQualityScore | null>
}

export function evaluateAiResumeQuality(payload: { resumeId: number; forceRefresh?: boolean }) {
  return request.post<AiResumeQualityScore>('/api/ai/resume-score/evaluate', payload) as Promise<AiResumeQualityScore>
}
