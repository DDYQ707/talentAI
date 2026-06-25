import request from '@/utils/request'

/* ---- 数据模型 ---- */

export interface EnterpriseAudit {
  id: number
  companyName: string
  creditCode: string
  legalPerson: string
  registeredCapital: string
  businessScope: string
  licenseUrl: string
  status: number          // 0=待审核 1=已通过 2=已驳回
  statusLabel: string
  rejectReason?: string
  submittedAt?: string
  reviewedAt?: string
}

export interface EnterpriseAuditPage {
  records: EnterpriseAudit[]
  total: number
  page: number
  size: number
}

export interface ListEnterprisesParams {
  page?: number
  size?: number
  keyword?: string
  creditCode?: string
  status?: number
  startDate?: string
  endDate?: string
}

/* ---- 状态常量 ---- */

export const ENTERPRISE_STATUS = {
  PENDING: 0,
  APPROVED: 1,
  REJECTED: 2,
} as const

export const ENTERPRISE_STATUS_OPTIONS = [
  { label: '全部状态', value: undefined },
  { label: '待审核', value: ENTERPRISE_STATUS.PENDING },
  { label: '已通过', value: ENTERPRISE_STATUS.APPROVED },
  { label: '已驳回', value: ENTERPRISE_STATUS.REJECTED },
]

/* ---- API 接口 ---- */

export function listEnterprises(params: ListEnterprisesParams = {}) {
  return request.get<EnterpriseAuditPage>('/api/admin/enterprises', { params }) as Promise<EnterpriseAuditPage>
}

export function approveEnterprise(id: number) {
  return request.put<null>(`/api/admin/enterprises/${id}/approve`) as Promise<null>
}

export function rejectEnterprise(id: number, reason: string) {
  return request.put<null>(`/api/admin/enterprises/${id}/reject`, { rejectReason: reason }) as Promise<null>
}
