import request from '@/utils/request'
import { fetchAiMatchByApplication } from '@/api/ai'
import { fetchHrResumeDetail, fetchHrResumePage, type HrResumeListItem } from '@/api/hrResume'

export interface InterviewerOption {
  id: number
  nickname: string
  account: string
}

export interface InterviewListItem {
  interviewId: number
  applicationId: number
  jobId: number
  candidateId: number
  candidateName: string
  jobTitle: string
  interviewerId: number
  interviewerName: string
  roundNo: number
  roundType: number
  roundTypeLabel: string
  interviewMode: number
  interviewModeLabel: string
  scheduledStart?: string
  scheduledEnd?: string | null
  meetingUrl?: string | null
  location?: string | null
  status: number
  statusLabel: string
  totalScore?: number | null
  /** 1-通过 2-待定 3-不通过 */
  evaluationConclusion?: number | null
  evaluationConclusionLabel?: string | null
  evaluation?: InterviewEvaluation | null
  resumeId?: number | null
}

export interface InterviewEvaluation {
  evaluationId: number
  evaluatorId: number
  evaluatorName: string
  dimensionScores?: string | null
  overallScore?: number | null
  conclusion?: number | null
  conclusionLabel?: string | null
  comment?: string | null
  createdAt?: string
}

export interface InterviewDetail extends InterviewListItem {
  createdBy?: number
  createdByName?: string
  createdAt?: string
  updatedAt?: string
  evaluation?: InterviewEvaluation | null
}

export interface InterviewStats {
  todayPending: number
  weekTotal: number
  completed: number
  toSchedule: number
  cancelled: number
}

export interface InterviewPageData {
  records: InterviewListItem[]
  total: number
  page: number
  size: number
}

export interface InterviewSchedulePayload {
  applicationId: number
  interviewerId: number
  roundType: number
  roundNo?: number
  interviewMode: number
  scheduledStart: string
  scheduledEnd?: string
  meetingUrl?: string
  location?: string
}

export interface InterviewScheduleResult {
  interviewId: number
  status: number
  statusLabel: string
  candidateName: string
  jobTitle: string
  interviewerName: string
  scheduledStart: string
}

export interface EvaluationPayload {
  overallScore?: number
  conclusion: number
  comment?: string
  dimensionScores: Record<string, number>
}

export function reassignInterview(interviewId: number, interviewerId: number) {
  return request.put<null>(`/api/interview/hr/${interviewId}/reassign`, {
    interviewerId,
  }) as Promise<null>
}

export interface HrInterviewQuery {
  page?: number
  size?: number
  keyword?: string
  status?: number
  dateFrom?: string
  dateTo?: string
}

export interface MyInterviewQuery {
  page?: number
  size?: number
  keyword?: string
  status?: number
}

export function fetchInterviewers() {
  return request.get<InterviewerOption[]>('/api/auth/hr/interviewers') as Promise<InterviewerOption[]>
}

export function scheduleInterview(data: InterviewSchedulePayload) {
  return request.post<InterviewScheduleResult>('/api/interview/hr/schedule', data) as Promise<InterviewScheduleResult>
}

export function fetchHrInterviewPage(params: HrInterviewQuery = {}) {
  return request.get<InterviewPageData>('/api/interview/hr/page', { params }) as Promise<InterviewPageData>
}

export function fetchHrInterviewStats() {
  return request.get<InterviewStats>('/api/interview/hr/stats') as Promise<InterviewStats>
}

export function fetchHrInterviewDetail(interviewId: number) {
  return request.get<InterviewDetail>(`/api/interview/hr/${interviewId}`) as Promise<InterviewDetail>
}

export function cancelInterview(interviewId: number) {
  return request.put<null>(`/api/interview/hr/${interviewId}/cancel`) as Promise<null>
}

export function fetchInterviewsByApplication(applicationId: number) {
  return request.get<InterviewListItem[]>('/api/interview/hr/by-application', {
    params: { applicationId },
  }) as Promise<InterviewListItem[]>
}

export function fetchMyInterviewPage(params: MyInterviewQuery = {}) {
  return request.get<InterviewPageData>('/api/interview/my/page', { params }) as Promise<InterviewPageData>
}

export function fetchMyInterviewStats() {
  return request.get<InterviewStats>('/api/interview/my/stats') as Promise<InterviewStats>
}

export function fetchMyInterviewDetail(interviewId: number) {
  return request.get<InterviewDetail>(`/api/interview/my/${interviewId}`) as Promise<InterviewDetail>
}

export function fetchCandidateInterviewPage(params: MyInterviewQuery = {}) {
  return request.get<InterviewPageData>('/api/interview/candidate/page', { params }) as Promise<InterviewPageData>
}

export function fetchCandidateInterviewDetail(interviewId: number) {
  return request.get<InterviewDetail>(`/api/interview/candidate/${interviewId}`) as Promise<InterviewDetail>
}

export function submitInterviewEvaluation(interviewId: number, data: EvaluationPayload) {
  return request.post<InterviewEvaluation>(`/api/interview/my/${interviewId}/evaluation`, data) as Promise<InterviewEvaluation>
}

/** 安排面试时可选的投递记录（来自 HR 简历列表 + 详情补全 applicationId） */
export interface ScheduleableApplicationOption {
  resumeId: number
  applicationId: number
  candidateName: string
  jobTitle: string
  matchScore?: number | null
}

/** 拉取可安排面试的投递单（待筛选 / 面试中，且已有投递岗位） */
export async function fetchScheduleableApplications(): Promise<ScheduleableApplicationOption[]> {
  const [pending, interviewing] = await Promise.all([
    fetchHrResumePage({ current: 1, size: 100, screenStatus: 1 }),
    fetchHrResumePage({ current: 1, size: 100, screenStatus: 2 }),
  ])
  const seenResumeIds = new Set<number>()
  const candidates: HrResumeListItem[] = []
  for (const item of [...pending.records, ...interviewing.records]) {
    if (!item.appliedJobTitle || seenResumeIds.has(item.id)) continue
    seenResumeIds.add(item.id)
    candidates.push(item)
  }

  const resolved = await Promise.all(
    candidates.map(async (item) => {
      try {
        const detail = await fetchHrResumeDetail(item.id)
        if (!detail.applicationId || detail.applicationId <= 0) return null
        return {
          resumeId: item.id,
          applicationId: detail.applicationId,
          candidateName: detail.candidateName || item.candidateName,
          jobTitle: detail.appliedJobTitle || item.appliedJobTitle || '',
          matchScore: detail.matchScore ?? item.matchScore ?? null,
        } satisfies ScheduleableApplicationOption
      } catch {
        return null
      }
    }),
  )
  return resolved.filter((item): item is ScheduleableApplicationOption => item != null)
}

/** 批量查询投递单的 AI 匹配分（用于面试官列表等） */
export async function fetchAiMatchScoresByApplications(
  applicationIds: number[],
): Promise<Record<number, number>> {
  const uniqueIds = [...new Set(applicationIds.filter((id) => id > 0))]
  if (uniqueIds.length === 0) return {}

  const entries = await Promise.all(
    uniqueIds.map(async (applicationId) => {
      try {
        const match = await fetchAiMatchByApplication(applicationId)
        return [applicationId, match?.matchScore ?? null] as const
      } catch {
        return [applicationId, null] as const
      }
    }),
  )

  const scores: Record<number, number> = {}
  for (const [applicationId, score] of entries) {
    if (score != null && score > 0) scores[applicationId] = score
  }
  return scores
}
