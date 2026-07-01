import request from '@/utils/request'

// ==================== Offer 状态常量 ====================

/** Offer 审批节点状态 */
export const OFFER_APPROVAL_STATUS = {
  PENDING: 1,
  APPROVED: 2,
  REJECTED: 3,
} as const

/** Offer 状态码（与后端 OfferConstants 一致） */
export const OFFER_STATUS = {
  PENDING: 1,      // 待审批
  APPROVING: 2,    // 审批中
  APPROVED: 3,     // 已通过
  REJECTED: 4,     // 已拒绝（审批拒绝）
  ISSUED: 5,       // 已发放
  ACCEPTED: 6,     // 候选人已接受
  DECLINED: 7,     // 候选人已拒绝
  REVOKED: 8,      // 已撤回
} as const

// ==================== TypeScript 接口 ====================

/** Offer 审批节点 */
export interface OfferApproval {
  id: number
  offerId: number
  seq: number
  approverId: number
  approverName: string
  status: number
  comment?: string | null
  approvedAt?: string | null
  createdAt?: string
  updatedAt?: string
}

/** Offer 列表视图对象（精简展示） */
export interface OfferListVO {
  id: number
  offerNo: string
  applicationId?: number | null
  jobTitle: string
  candidateName: string
  deptName: string
  baseSalary: number
  annualSalary: number
  positionLevel: string
  /** 数字状态码 (1-8) */
  status: number
  /** 中文状态描述（仅供展示） */
  statusText: string
  hrName: string
  createdAt: string
  jobId?: number | null
  candidateId?: number | null
}

/** Offer 详情视图对象（含审批链） */
export interface OfferDetailVO {
  id: number
  offerNo: string
  applicationId?: number | null
  jobId: number
  jobTitle: string
  candidateId: number
  candidateName: string
  deptId?: number | null
  deptName: string
  baseSalary: number
  annualSalary: number
  bonus: number
  salaryRemark?: string | null
  positionLevel: string
  expectedOnboardDate?: string | null
  probationMonths?: number | null
  /** 数字状态码 (1-8) */
  status: number
  /** 中文状态描述（仅供展示） */
  statusText: string
  hrId: number
  hrName: string
  remark?: string | null
  createdAt: string
  updatedAt?: string
  approvals: OfferApproval[]
}

/** Offer 列表分页查询参数 */
export interface OfferQueryParams {
  current?: number
  size?: number
  /** 状态过滤 (1-8) */
  status?: number
  /** 候选人姓名模糊搜索 */
  candidateName?: string
  /** 岗位名称模糊搜索 */
  jobTitle?: string
  /** 岗位 ID 精确过滤 */
  jobId?: number
  /** 投递 ID 精确过滤 */
  applicationId?: number
}

/** Offer 列表分页响应 */
export interface OfferPageData {
  records: OfferListVO[]
  total: number
  current: number
  pages: number
}

/** 创建 Offer 请求体 */
export interface OfferCreatePayload {
  jobId: number
  candidateId: number
  candidateName: string
  applicationId?: number
  baseSalary?: number
  annualSalary?: number
  bonus?: number
  salaryRemark?: string
  positionLevel?: string
  expectedOnboardDate?: string
  probationMonths?: number
  remark?: string
  approverIds?: number[]
}

/** 更新 Offer 请求体 */
export interface OfferUpdatePayload {
  baseSalary?: number
  annualSalary?: number
  bonus?: number
  salaryRemark?: string
  positionLevel?: string
  expectedOnboardDate?: string
  probationMonths?: number
  remark?: string
}

// ==================== API 方法 ====================

/**
 * 分页查询 Offer 列表
 * GET /api/offer/list
 */
export function fetchOfferList(params: OfferQueryParams = {}) {
  const query: Record<string, string | number> = {
    current: params.current ?? 1,
    size: params.size ?? 10,
  }
  if (params.status != null) query.status = params.status
  if (params.candidateName?.trim()) query.candidateName = params.candidateName.trim()
  if (params.jobTitle?.trim()) query.jobTitle = params.jobTitle.trim()
  if (params.jobId != null) query.jobId = params.jobId
  if (params.applicationId != null) query.applicationId = params.applicationId
  return request.get<OfferPageData>('/api/offer/list', { params: query }) as Promise<OfferPageData>
}

/**
 * 按投递 ID 查询 Offer
 * GET /api/offer/by-application/{applicationId}
 */
export function fetchOfferByApplication(applicationId: number) {
  return request.get<OfferDetailVO | null>(`/api/offer/by-application/${applicationId}`) as Promise<OfferDetailVO | null>
}

/**
 * 查询 Offer 详情（含审批链）
 * GET /api/offer/{id}
 */
export function fetchOfferDetail(id: number) {
  return request.get<OfferDetailVO>(`/api/offer/${id}`) as Promise<OfferDetailVO>
}

/**
 * HR 创建 Offer
 * POST /api/offer
 */
export function createOffer(data: OfferCreatePayload) {
  return request.post<OfferDetailVO>('/api/offer', data) as Promise<OfferDetailVO>
}

/**
 * HR 更新 Offer
 * PUT /api/offer/{id}
 */
export function updateOffer(id: number, data: OfferUpdatePayload) {
  return request.put<OfferDetailVO>(`/api/offer/${id}`, data) as Promise<OfferDetailVO>
}

/**
 * HR 发放 Offer
 * PUT /api/offer/{id}/issue
 */
export function issueOffer(id: number) {
  return request.put<null>(`/api/offer/${id}/issue`) as Promise<null>
}

/**
 * HR 撤回 Offer
 * PUT /api/offer/{id}/revoke
 */
export function revokeOffer(id: number) {
  return request.put<null>(`/api/offer/${id}/revoke`) as Promise<null>
}

/**
 * 候选人接受 Offer
 * PUT /api/offer/{id}/accept
 */
export function acceptOffer(id: number) {
  return request.put<null>(`/api/offer/${id}/accept`) as Promise<null>
}

/**
 * 候选人拒绝 Offer
 * PUT /api/offer/{id}/reject
 */
export function rejectOffer(id: number) {
  return request.put<null>(`/api/offer/${id}/reject`) as Promise<null>
}

/**
 * 审批通过
 * PUT /api/offer/approval/{approvalId}/approve
 */
export function approveOfferApproval(approvalId: number, comment?: string) {
  return request.put<null>(`/api/offer/approval/${approvalId}/approve`, comment ? { comment } : undefined) as Promise<null>
}

/**
 * 审批拒绝
 * PUT /api/offer/approval/{approvalId}/reject
 */
export function rejectOfferApproval(approvalId: number, comment?: string) {
  return request.put<null>(`/api/offer/approval/${approvalId}/reject`, comment ? { comment } : undefined) as Promise<null>
}
