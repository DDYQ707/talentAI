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
