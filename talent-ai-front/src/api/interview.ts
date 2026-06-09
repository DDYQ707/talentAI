import request from '@/utils/request'

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
  overallScore: number
  conclusion: number
  comment?: string
  dimensionScores?: Record<string, number>
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

export function submitInterviewEvaluation(interviewId: number, data: EvaluationPayload) {
  return request.post<InterviewEvaluation>(`/api/interview/my/${interviewId}/evaluation`, data) as Promise<InterviewEvaluation>
}
