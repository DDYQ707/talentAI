import request from '@/utils/request'
import type { ResumePreviewResult } from '@/api/resume'
import type {
  OnlineEducation,
  OnlineSkill,
  OnlineWorkExperience,
} from '@/api/onlineResume'

export interface HrResumeListItem {
  id: number
  candidateId: number
  candidateName: string
  resumeName: string
  resumeType?: 'attachment' | 'online'
  phone?: string
  city?: string
  currentTitle?: string
  highestEdu?: number
  screenStatus: number
  parseStatus?: number
  attachmentId?: number
  fileName?: string
  fileType?: string
  fileSize?: number
  updatedAt?: string
  appliedJobTitle?: string
  appliedAt?: string
  matchScore?: number | null
}

export interface HrResumePageData {
  total: number
  current: number
  pages: number
  records: HrResumeListItem[]
}

export interface HrResumeDetail {
  id: number
  candidateId: number
  candidateName: string
  resumeName: string
  summary?: string
  resumeType?: 'attachment' | 'online'
  isDefault?: number
  parseStatus?: number
  screenStatus: number
  attachmentId?: number
  fileName?: string
  fileType?: string
  fileSize?: number
  createdAt?: string
  updatedAt?: string
  phone?: string
  email?: string
  city?: string
  currentTitle?: string
  highestEdu?: number
  matchScore?: number
  appliedJobTitle?: string
  appliedAt?: string
  applicationId?: number
  jobId?: number
  educations?: OnlineEducation[]
  workExperiences?: OnlineWorkExperience[]
  skills?: OnlineSkill[]
}

export interface HrResumeListParams {
  current?: number
  size?: number
  keyword?: string
  /** 1-待初筛 2-面试中 3-已录用 4-已淘汰 */
  screenStatus?: number
}

/** 合并库中重复简历（同一候选人只保留一条） */
export function consolidateHrResumes() {
  return request.post<{ mergedDuplicates: number }>('/api/resume/hr/consolidate') as Promise<{
    mergedDuplicates: number
  }>
}

/** HR 简历分页列表 */
export function fetchHrResumePage(params: HrResumeListParams = {}) {
  const query: Record<string, string | number> = {
    current: params.current ?? 1,
    size: params.size ?? 20,
  }
  if (params.keyword) query.keyword = params.keyword
  if (params.screenStatus != null) query.screenStatus = params.screenStatus
  return request.get<HrResumePageData>('/api/resume/hr/page', { params: query }) as Promise<HrResumePageData>
}

/** HR 简历详情 */
export function fetchHrResumeDetail(resumeId: number) {
  return request.get<HrResumeDetail>(`/api/resume/hr/${resumeId}`) as Promise<HrResumeDetail>
}

/** HR 预览附件（预签名） */
export function fetchHrResumePreview(attachmentId: number) {
  return request.get<ResumePreviewResult>(`/api/resume/file/preview/${attachmentId}`) as Promise<ResumePreviewResult>
}

/** HR 更新简历筛选状态（1-待初筛 2-面试中 3-已录用 4-已淘汰） */
export function updateHrScreenStatus(resumeId: number, screenStatus: number, remark?: string) {
  return request.patch<HrResumeDetail>(`/api/resume/hr/${resumeId}/screen-status`, {
    screenStatus,
    remark,
  }) as Promise<HrResumeDetail>
}
