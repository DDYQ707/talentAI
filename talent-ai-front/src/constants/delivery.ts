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

/** 阶段在 APPLICATION_STAGES 中的 0-based 索引 */
export function resolveStageIndex(currentStage?: number | null): number {
  const stageName = stageLabel(currentStage)
  const idx = APPLICATION_STAGES.indexOf(stageName as (typeof APPLICATION_STAGES)[number])
  return idx >= 0 ? idx : 0
}

/** 是否处于面试环节（HR 初面～终面，不含 Offer/已入职） */
export function isInterviewingStage(currentStage?: number | null, status?: number | null): boolean {
  if (status !== APPLICATION_STATUS.IN_PROGRESS) return false
  const stage = currentStage ?? 0
  return stage >= 3 && stage <= 5
}

/** 是否 Offer / 已入职 */
export function isOfferStatus(currentStage?: number | null, status?: number | null): boolean {
  return status === APPLICATION_STATUS.HIRED || (currentStage ?? 0) >= 6
}

export interface DeliveryStats {
  total: number
  inProgress: number
  interviewing: number
  offer: number
}

/** 候选人投递统计（个人中心与投递记录页统一口径） */
export function computeDeliveryStats(
  records: Array<{ currentStage: number; status: number }>,
  total?: number,
): DeliveryStats {
  let inProgress = 0
  let interviewing = 0
  let offer = 0
  for (const r of records) {
    if (statusToUi(r.status) === '进行中') inProgress++
    if (isInterviewingStage(r.currentStage, r.status)) interviewing++
    if (statusToUi(r.status) === 'offer') offer++
  }
  return {
    total: total ?? records.length,
    inProgress,
    interviewing,
    offer,
  }
}

export type StepProgressState = 'completed' | 'active' | 'pending'

/** 投递进度条单步状态（含 stage 7 / 已入职） */
export function stepProgressState(stepIndex: number, stageIndex: number): StepProgressState {
  const lastIndex = APPLICATION_STAGES.length - 1
  if (stageIndex >= lastIndex) {
    return stepIndex <= lastIndex ? 'completed' : 'pending'
  }
  if (stepIndex < stageIndex) return 'completed'
  if (stepIndex === stageIndex) return 'active'
  return 'pending'
}

export function isProgressConnectorComplete(afterStepIndex: number, stageIndex: number): boolean {
  const lastIndex = APPLICATION_STAGES.length - 1
  if (stageIndex >= lastIndex) return true
  return afterStepIndex < stageIndex
}

export function formatMatchScore(score?: number | null): string {
  if (score == null || Number(score) <= 0) return '—'
  return String(Math.round(Number(score)))
}
