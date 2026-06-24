import request from '@/utils/request'

export interface DeliverySubmitPayload {
  jobId: number
  resumeId: number
  channel?: number
}

export interface DeliverySubmitResult {
  id: number
  applicationNo: string
  jobId: number
  jobTitle: string
  resumeId: number
  currentStage: number
  status: number
  appliedAt: string
}

export interface DeliveryRecord {
  id: number
  applicationNo: string
  jobId: number
  jobTitle: string
  resumeId?: number
  resumeName?: string
  attachmentId?: number
  attachmentFileName?: string
  attachmentFileType?: string
  currentStage: number
  status: number
  matchScore?: number | null
  appliedAt: string
}

export interface DeliveryPageData {
  total: number
  current: number
  pages: number
  records: DeliveryRecord[]
}

export interface AppliedJobsData {
  jobIds: number[]
}

/** 投递简历 */
export function submitApplication(payload: DeliverySubmitPayload) {
  return request.post<DeliverySubmitResult>('/api/delivery/submit', payload) as Promise<DeliverySubmitResult>
}

/** 我的投递记录 */
export function fetchMyApplications(params?: { current?: number; size?: number }) {
  return request.get<DeliveryPageData>('/api/delivery/my', {
    params: {
      current: params?.current ?? 1,
      size: params?.size ?? 20,
    },
  }) as Promise<DeliveryPageData>
}

/** 进行中投递的岗位 ID（用于展示已投递状态） */
export function fetchActiveAppliedJobIds() {
  return request.get<AppliedJobsData>('/api/delivery/applied-jobs') as Promise<AppliedJobsData>
}
