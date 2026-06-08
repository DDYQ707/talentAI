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
