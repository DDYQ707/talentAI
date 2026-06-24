import {
  fetchAiMatchByApplication,
  fetchPreviewMatch,
  fetchPreviewMatchBatch,
  triggerPreviewMatch,
  type AiMatchResult,
} from '@/api/ai'
import type { DeliveryRecord } from '@/api/delivery'

/** 解析单条投递的 AI 匹配分（优先投递记录，否则查 AI 服务） */
export async function resolveApplicationMatchScore(record: DeliveryRecord): Promise<number | null> {
  if (record.matchScore != null && record.matchScore > 0) {
    return record.matchScore
  }
  try {
    const match = await fetchAiMatchByApplication(record.id)
    if (match?.matchScore != null && match.matchScore > 0) {
      return match.matchScore
    }
  } catch {
    // ignore
  }
  return null
}

function extractMatchScore(match: AiMatchResult | null | undefined): number | null {
  if (match?.matchScore != null && match.matchScore > 0 && match.matchStatus === 2) {
    return match.matchScore
  }
  return null
}

/** jobId → 匹配分（已投递岗位，来自投递记录） */
export async function buildJobMatchScoreMap(records: DeliveryRecord[]): Promise<Map<number, number>> {
  const map = new Map<number, number>()
  const tasks = records.map(async (record) => {
    if (!record.jobId) return
    const score = await resolveApplicationMatchScore(record)
    if (score != null && score > 0) {
      map.set(record.jobId, score)
    }
  })
  await Promise.all(tasks)
  return map
}

/** applicationId → 匹配分 */
export async function buildApplicationMatchScoreMap(
  records: DeliveryRecord[],
): Promise<Map<number, number>> {
  const map = new Map<number, number>()
  const tasks = records.map(async (record) => {
    const score = await resolveApplicationMatchScore(record)
    if (score != null && score > 0) {
      map.set(record.id, score)
    }
  })
  await Promise.all(tasks)
  return map
}

/** 未投递岗位：批量读取预览匹配缓存并后台触发缺失项 */
export async function buildJobPreviewMatchScoreMap(jobIds: number[]): Promise<Map<number, number>> {
  const map = new Map<number, number>()
  const ids = [...new Set(jobIds.filter((id) => id > 0))]
  if (!ids.length) return map

  try {
    const batch = await fetchPreviewMatchBatch(ids)
    for (const [key, match] of Object.entries(batch ?? {})) {
      const score = extractMatchScore(match)
      if (score != null) {
        map.set(Number(key), score)
      }
    }
  } catch {
    // ignore
  }

  void triggerMissingPreviewMatches(ids, map)
  return map
}

/** 未投递岗位：确保预览匹配（查询缓存，必要时触发） */
export async function resolveJobPreviewMatchScore(jobId: number): Promise<number | null> {
  if (!jobId) return null
  try {
    let match = await fetchPreviewMatch(jobId)
    const cached = extractMatchScore(match)
    if (cached != null) return cached
    if (match?.matchStatus === 0 || match?.matchStatus === 1) {
      return null
    }
    match = await triggerPreviewMatch(jobId)
    return extractMatchScore(match)
  } catch {
    return null
  }
}

async function triggerMissingPreviewMatches(
  jobIds: number[],
  existing: Map<number, number>,
) {
  const pending = jobIds.filter((id) => !existing.has(id))
  const concurrency = 2
  let index = 0

  async function worker() {
    while (index < pending.length) {
      const jobId = pending[index++]
      try {
        await triggerPreviewMatch(jobId)
      } catch {
        // ignore single job failure
      }
    }
  }

  if (pending.length === 0) return
  await Promise.all(Array.from({ length: Math.min(concurrency, pending.length) }, () => worker()))
}

/** 合并已投递与预览匹配分（已投递优先） */
export async function buildCombinedJobMatchScoreMap(
  records: DeliveryRecord[],
  jobIds: number[],
  appliedJobIdSet: Set<number>,
): Promise<Map<number, number>> {
  const appliedMap = await buildJobMatchScoreMap(records)
  const previewJobIds = jobIds.filter((id) => !appliedJobIdSet.has(id))
  const previewMap = await buildJobPreviewMatchScoreMap(previewJobIds)
  return new Map<number, number>([...previewMap, ...appliedMap])
}
