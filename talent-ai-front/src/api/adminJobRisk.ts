import request from '@/utils/request'

/* ---- 数据模型 ---- */

export interface JobRiskVO {
  id: number
  jobTitle: string
  companyName: string
  publisherId: number
  salaryMin: number
  salaryMax: number
  description: string
  status: number          // 0=正常 1=风险预警 2=已下架
  statusLabel: string
  riskKeywords: string[]
  createdAt?: string
  takenDownAt?: string
  takenDownBy?: string
}

export interface JobRiskPage {
  records: JobRiskVO[]
  total: number
  page: number
  size: number
}

export interface ListJobRiskParams {
  page?: number
  size?: number
  keyword?: string
  status?: number
}

/* ---- 状态常量 ---- */

export const JOB_RISK_STATUS = {
  NORMAL: 0,
  WARNING: 1,
  TAKEN_DOWN: 2,
} as const

export const JOB_RISK_STATUS_OPTIONS = [
  { label: '全部状态', value: undefined },
  { label: '正常', value: JOB_RISK_STATUS.NORMAL },
  { label: '风险预警', value: JOB_RISK_STATUS.WARNING },
  { label: '已下架', value: JOB_RISK_STATUS.TAKEN_DOWN },
]

/* ---- API 接口 ---- */

export function listRiskJobs(params: ListJobRiskParams = {}) {
  return request.get<JobRiskPage>('/api/admin/jobs/risk', { params }) as Promise<JobRiskPage>
}

export function takedownJob(id: number, reason: string) {
  return request.put<null>(`/api/admin/jobs/risk/${id}/takedown`, { reason }) as Promise<null>
}
