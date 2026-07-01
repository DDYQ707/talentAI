/** 简历筛选状态（与 resume.screen_status 一致） */
export const RESUME_SCREEN_STATUS = {
  PENDING: 1,
  INTERVIEWING: 2,
  HIRED: 3,
  REJECTED: 4,
  OFFER_PENDING: 5,
} as const

export const RESUME_SCREEN_LABEL: Record<number, string> = {
  1: '待筛选',
  2: '面试中',
  3: '已录用',
  4: '已淘汰',
  5: '待录用',
}

/** 终态：不可再通过 HR 手动变更筛选状态 */
export const TERMINAL_SCREEN_STATUSES = [
  RESUME_SCREEN_STATUS.HIRED,
  RESUME_SCREEN_STATUS.REJECTED,
] as const

export function isTerminalScreenStatus(status?: number | null): boolean {
  return status != null && (TERMINAL_SCREEN_STATUSES as readonly number[]).includes(status)
}

export function screenStatusLabel(status?: number | null): string {
  if (status == null) return '待筛选'
  return RESUME_SCREEN_LABEL[status] ?? '待筛选'
}
