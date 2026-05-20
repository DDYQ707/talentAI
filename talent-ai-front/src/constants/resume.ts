/** 简历初筛状态（与 resume.screen_status 一致） */
export const RESUME_SCREEN_STATUS = {
  PENDING: 1,
  INTERVIEWING: 2,
  HIRED: 3,
  REJECTED: 4,
} as const

export const RESUME_SCREEN_LABEL: Record<number, string> = {
  1: '待初筛',
  2: '面试中',
  3: '已录用',
  4: '已淘汰',
}

export function screenStatusLabel(status?: number | null): string {
  if (status == null) return '待初筛'
  return RESUME_SCREEN_LABEL[status] ?? '待初筛'
}
