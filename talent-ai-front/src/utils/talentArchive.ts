import { JOB_SEEKING_STATUS, type TalentPoolArchivePayload } from '@/api/talentPool'

export interface TalentArchiveContext {
  candidateId: number
  candidateName: string
  resumeId?: number | null
  applicationId?: number | null
  archiveReason?: string
  matchScore?: number | null
  currentTitle?: string | null
  currentCompany?: string | null
  appliedJobTitle?: string | null
  jobSeekingStatus?: number | null
  /** 面试结论摘要（单独字段存储） */
  interviewSummary?: string | null
  /** 兼容旧调用：会写入 interviewSummary */
  evaluationNote?: string | null
}

/** 构建人才库归档请求（字段由后端 Feign 进一步补全） */
export function buildTalentArchivePayload(ctx: TalentArchiveContext): TalentPoolArchivePayload {
  const archiveReason = ctx.archiveReason?.trim() || 'HR 手动归档'
  let interviewSummary = ctx.interviewSummary?.trim()
  if (!interviewSummary && ctx.evaluationNote?.trim()) {
    interviewSummary = ctx.evaluationNote.trim()
  }

  const payload: TalentPoolArchivePayload = {
    candidateId: ctx.candidateId,
    candidateName: ctx.candidateName,
    resumeId: ctx.resumeId ?? undefined,
    sourceApplicationId: ctx.applicationId ?? undefined,
    archiveReason,
    interviewSummary: interviewSummary || undefined,
    sourceJobTitle: ctx.appliedJobTitle ?? undefined,
    currentTitle: ctx.currentTitle ?? undefined,
    currentCompany: ctx.currentCompany ?? undefined,
    jobSeekingStatus: ctx.jobSeekingStatus ?? JOB_SEEKING_STATUS.WATCHING,
  }

  if (ctx.matchScore != null && Number.isFinite(ctx.matchScore)) {
    payload.matchScore = Math.max(0, Math.min(100, Math.round(ctx.matchScore)))
  }

  return payload
}

export function firstWorkCompany(
  workExperiences?: Array<{ companyName?: string | null }> | null,
): string | undefined {
  const company = workExperiences?.[0]?.companyName?.trim()
  return company || undefined
}
