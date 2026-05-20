/** 招聘阶段（与后端 current_stage 一致） */
export const APPLICATION_STAGES = [
  '简历投递',
  'AI初筛',
  'HR初面',
  '技术面试',
  '终面',
  'Offer',
  '已入职',
] as const

/** 投递状态 */
export const APPLICATION_STATUS = {
  IN_PROGRESS: 1,
  HIRED: 2,
  REJECTED: 3,
  WITHDRAWN: 4,
} as const

export type ApplicationUiStatus = '进行中' | 'offer' | '已淘汰' | '已撤回'

export function stageLabel(stage?: number | null): string {
  if (!stage || stage < 1) return APPLICATION_STAGES[0]
  const idx = Math.min(stage, APPLICATION_STAGES.length) - 1
  return APPLICATION_STAGES[idx]
}

export function statusToUi(status?: number | null): ApplicationUiStatus {
  switch (status) {
    case APPLICATION_STATUS.HIRED:
      return 'offer'
    case APPLICATION_STATUS.REJECTED:
      return '已淘汰'
    case APPLICATION_STATUS.WITHDRAWN:
      return '已撤回'
    default:
      return '进行中'
  }
}
