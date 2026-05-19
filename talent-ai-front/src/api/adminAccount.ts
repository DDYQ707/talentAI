import request from '@/utils/request'

export interface AdminAccount {
  id: number
  account: string
  phone?: string
  email?: string
  nickname: string
  userType: number
  userTypeLabel: string
  status: number
  statusLabel: string
  createdAt?: string
  updatedAt?: string
}

export interface AdminAccountPage {
  records: AdminAccount[]
  total: number
  page: number
  size: number
}

export interface AdminAccountSavePayload {
  account: string
  password?: string
  userType: number
  nickname?: string
  status?: number
}

export interface ListAccountsParams {
  page?: number
  size?: number
  keyword?: string
  userType?: number
}

export function listAdminAccounts(params: ListAccountsParams = {}) {
  return request.get<AdminAccountPage>('/api/auth/admin/accounts', { params }) as Promise<AdminAccountPage>
}

export function createAdminAccount(data: AdminAccountSavePayload) {
  return request.post<AdminAccount>('/api/auth/admin/accounts', data) as Promise<AdminAccount>
}

export function updateAdminAccount(id: number, data: AdminAccountSavePayload) {
  return request.put<AdminAccount>(`/api/auth/admin/accounts/${id}`, data) as Promise<AdminAccount>
}

export function deleteAdminAccount(id: number) {
  return request.delete<null>(`/api/auth/admin/accounts/${id}`) as Promise<null>
}
