import {
  fetchAiMatchByApplication,
  fetchPreviewMatch,
  fetchPreviewMatchBatch,
  triggerPreviewMatch,
  type AiMatchResult,
} from '@/api/ai'
import type { DeliveryRecord } from '@/api/delivery'

export interface MatchScoreState {
  score: number | null
  pending: boolean
  failed: boolean
}

export interface MatchScoreBatch {
  scores: Map<number, number>
  pendingIds: Set<number>
  failedIds: Set<number>
}

function extractMatchScore(match: AiMatchResult | null | undefined): number | null {
  if (match?.matchScore != null && match.matchScore > 0 && match.matchStatus === 2) {
    return match.matchScore
  }
  return null
}

function stateFromMatch(match: AiMatchResult | null | undefined): MatchScoreState {
  const score = extractMatchScore(match)
  if (score != null) {
    return { score, pending: false, failed: false }
  }
  if (match?.matchStatus === 0 || match?.matchStatus === 1) {
    return { score: null, pending: true, failed: false }
  }
  if (match?.matchStatus === 3) {
    return { score: null, pending: false, failed: true }
  }
  return { score: null, pending: false, failed: false }
}

/** 已投递但尚无匹配记录时，视为仍在计算中 */
export function isAppliedMatchAwaiting(state: MatchScoreState, applied: boolean): boolean {
  if (state.score != null || state.failed) return false
  if (state.pending) return true
  return applied
}

/** 解析单条投递的 AI 匹配分（优先投递记录，否则查 AI 服务） */
export async function resolveApplicationMatchScore(record: DeliveryRecord): Promise<number | null> {
  const state = await resolveApplicationMatchState(record)
  return state.score
}

export async function resolveApplicationMatchState(record: DeliveryRecord): Promise<MatchScoreState> {
  if (record.matchScore != null && record.matchScore > 0) {
    return { score: record.matchScore, pending: false, failed: false }
  }
  try {
    const match = await fetchAiMatchByApplication(record.id)
    let state = stateFromMatch(match)
    if (state.score != null || state.failed) {
      return state
    }
    if (state.pending) {
      return state
    }
    // 预览匹配 application_id 为空，已投递岗位需回退到 resume+job 查询
    if (record.jobId) {
      const previewState = await fetchPreviewMatchState(record.jobId)
      if (previewState.score != null || previewState.failed) {
        return previewState
      }
      if (previewState.pending) {
        return previewState
      }
    }
    return { score: null, pending: false, failed: false }
  } catch {
    return { score: null, pending: false, failed: false }
  }
}

/** jobId → 匹配分（已投递岗位，来自投递记录） */
export async function buildJobMatchScoreMap(records: DeliveryRecord[]): Promise<Map<number, number>> {
  const batch = await loadApplicationMatchStates(records)
  const map = new Map<number, number>()
  for (const record of records) {
    if (!record.jobId) continue
    const score = batch.scores.get(record.id)
    if (score != null) {
      map.set(record.jobId, score)
    }
  }
  return map
}

/** applicationId → 匹配分 */
export async function buildApplicationMatchScoreMap(
  records: DeliveryRecord[],
): Promise<Map<number, number>> {
  const batch = await loadApplicationMatchStates(records)
  return batch.scores
}

export async function loadApplicationMatchStates(records: DeliveryRecord[]): Promise<MatchScoreBatch> {
  const scores = new Map<number, number>()
  const pendingIds = new Set<number>()
  const failedIds = new Set<number>()

  await Promise.all(
    records.map(async (record) => {
      const state = await resolveApplicationMatchState(record)
      if (state.score != null) {
        scores.set(record.id, state.score)
      } else if (state.pending || isAppliedMatchAwaiting(state, true)) {
        pendingIds.add(record.id)
      } else if (state.failed) {
        failedIds.add(record.id)
      }
    }),
  )

  return { scores, pendingIds, failedIds }
}

/** 未投递岗位：批量读取预览匹配缓存并后台触发缺失项 */
export async function buildJobPreviewMatchScoreMap(jobIds: number[]): Promise<Map<number, number>> {
  const batch = await loadJobPreviewMatchStates(jobIds, { triggerMissing: true })
  return batch.scores
}

export async function loadJobPreviewMatchStates(
  jobIds: number[],
  options?: { triggerMissing?: boolean },
): Promise<MatchScoreBatch> {
  const scores = new Map<number, number>()
  const pendingIds = new Set<number>()
  const failedIds = new Set<number>()
  const ids = [...new Set(jobIds.filter((id) => id > 0))]
  if (!ids.length) {
    return { scores, pendingIds, failedIds }
  }

  let batch: Record<number, AiMatchResult> = {}
  try {
    batch = (await fetchPreviewMatchBatch(ids)) ?? {}
  } catch {
    // ignore
  }

  for (const jobId of ids) {
    const match = batch[jobId] ?? batch[String(jobId) as unknown as number]
    const state = stateFromMatch(match)
    if (state.score != null) {
      scores.set(jobId, state.score)
    } else if (state.pending) {
      pendingIds.add(jobId)
    } else if (state.failed) {
      failedIds.add(jobId)
    }
  }

  if (options?.triggerMissing !== false) {
    const needTrigger = ids.filter((id) => !scores.has(id) && !pendingIds.has(id) && !failedIds.has(id))
    if (needTrigger.length) {
      needTrigger.forEach((id) => pendingIds.add(id))
      void triggerMissingPreviewMatches(needTrigger, scores)
    }
  }

  return { scores, pendingIds, failedIds }
}

/** 仅查询预览匹配（不触发新任务） */
export async function fetchPreviewMatchState(jobId: number): Promise<MatchScoreState> {
  if (!jobId) {
    return { score: null, pending: false, failed: false }
  }
  try {
    const match = await fetchPreviewMatch(jobId)
    const state = stateFromMatch(match)
    if (state.score != null || state.pending || state.failed) {
      return state
    }
    return { score: null, pending: false, failed: false }
  } catch {
    return { score: null, pending: false, failed: false }
  }
}

/** 未投递岗位：确保预览匹配（查询缓存，必要时触发） */
export async function resolveJobPreviewMatchScore(jobId: number): Promise<number | null> {
  const state = await resolveJobPreviewMatchState(jobId)
  return state.score
}

export async function resolveJobPreviewMatchState(jobId: number): Promise<MatchScoreState> {
  if (!jobId) {
    return { score: null, pending: false, failed: false }
  }
  const cached = await fetchPreviewMatchState(jobId)
  if (cached.score != null || cached.pending || cached.failed) {
    return cached
  }
  try {
    const match = await triggerPreviewMatch(jobId)
    const state = stateFromMatch(match)
    if (state.score != null || state.pending || state.failed) {
      return state
    }
    return { score: null, pending: true, failed: false }
  } catch {
    return { score: null, pending: false, failed: true }
  }
}

async function triggerMissingPreviewMatches(jobIds: number[], existing: Map<number, number>) {
  const pending = jobIds.filter((id) => !existing.has(id))
  const concurrency = 4
  let index = 0

  async function worker() {
    while (index < pending.length) {
      const jobId = pending[index++]
      try {
        await triggerPreviewMatch(jobId)
      } catch {
        // 触发失败时轮询阶段会清除 pending；常见原因为 DB 补丁未执行
      }
    }
  }

  if (pending.length === 0) return
  await Promise.all(Array.from({ length: Math.min(concurrency, pending.length) }, () => worker()))
}

/** 岗位列表按预估匹配度降序（有分 > 计算中 > 无分） */
export function sortJobsByMatchScore<T extends { id: number }>(
  jobs: T[],
  scores: Map<number, number>,
  pendingIds: Set<number>,
): T[] {
  return [...jobs].sort((a, b) => {
    const scoreA = scores.get(a.id)
    const scoreB = scores.get(b.id)
    const aScored = scoreA != null && scoreA > 0
    const bScored = scoreB != null && scoreB > 0
    if (aScored && bScored) return scoreB! - scoreA!
    if (aScored) return -1
    if (bScored) return 1
    const aPending = pendingIds.has(a.id)
    const bPending = pendingIds.has(b.id)
    if (aPending && !bPending) return -1
    if (!aPending && bPending) return 1
    return 0
  })
}

/** 岗位列表：基于当前简历对所有在招岗位做预览匹配（无需投递） */
export async function loadJobListPreviewMatchStates(
  jobIds: number[],
  options?: { triggerMissing?: boolean },
): Promise<MatchScoreBatch> {
  return loadJobPreviewMatchStates(jobIds, options)
}

/** 合并已投递与预览匹配分（已投递优先） */
export async function buildCombinedJobMatchScoreMap(
  records: DeliveryRecord[],
  jobIds: number[],
  appliedJobIdSet: Set<number>,
): Promise<Map<number, number>> {
  const batch = await loadCombinedJobMatchStates(records, jobIds, appliedJobIdSet)
  return batch.scores
}

export async function loadCombinedJobMatchStates(
  records: DeliveryRecord[],
  jobIds: number[],
  appliedJobIdSet: Set<number>,
  options?: { triggerMissingPreview?: boolean },
): Promise<MatchScoreBatch & { scores: Map<number, number> }> {
  const scores = new Map<number, number>()
  const pendingIds = new Set<number>()
  const failedIds = new Set<number>()

  const appliedRecords = records.filter((record) => record.jobId && appliedJobIdSet.has(record.jobId))
  const appliedBatch = await loadApplicationMatchStates(appliedRecords)
  for (const record of appliedRecords) {
    if (!record.jobId) continue
    const score = appliedBatch.scores.get(record.id)
    if (score != null) {
      scores.set(record.jobId, score)
    } else if (appliedBatch.pendingIds.has(record.id)) {
      pendingIds.add(record.jobId)
    } else if (appliedBatch.failedIds.has(record.id)) {
      failedIds.add(record.jobId)
    }
  }

  const previewJobIds = jobIds.filter((id) => !appliedJobIdSet.has(id))
  const previewBatch = await loadJobPreviewMatchStates(previewJobIds, {
    triggerMissing: options?.triggerMissingPreview !== false,
  })
  for (const [jobId, score] of previewBatch.scores) {
    scores.set(jobId, score)
  }
  for (const jobId of previewBatch.pendingIds) {
    pendingIds.add(jobId)
  }
  for (const jobId of previewBatch.failedIds) {
    failedIds.add(jobId)
  }

  return { scores, pendingIds, failedIds }
}
