import type { OnlineResumeDetail, OnlineResumeSavePayload } from '@/api/onlineResume'
import { SKILL_PROFICIENCY_OPTIONS } from '@/constants/onlineResume'

type ResumeLike = Pick<
  OnlineResumeSavePayload,
  'summary' | 'educations' | 'workExperiences' | 'skills' | 'projects' | 'certificates'
>

export function isFreshGraduate(workYears?: number | null): boolean {
  return workYears == null || Number.isNaN(workYears) || workYears < 1
}

function hasValidEducation(educations?: ResumeLike['educations']): boolean {
  return (educations ?? []).some((e) => e.schoolName?.trim() && e.degree != null)
}

function hasPartialEducation(educations?: ResumeLike['educations']): boolean {
  return (educations ?? []).some((e) => e.schoolName?.trim())
}

function hasValidWork(workExperiences?: ResumeLike['workExperiences']): boolean {
  return (workExperiences ?? []).some(
    (w) => w.companyName?.trim() && w.jobTitle?.trim() && w.startDate?.trim(),
  )
}

function hasPartialWork(workExperiences?: ResumeLike['workExperiences']): boolean {
  return (workExperiences ?? []).some((w) => w.companyName?.trim() && w.jobTitle?.trim())
}

function hasValidProject(projects?: ResumeLike['projects']): boolean {
  return (projects ?? []).some(
    (p) => p.projectName?.trim() && p.role?.trim() && p.startDate?.trim(),
  )
}

function hasPartialProject(projects?: ResumeLike['projects']): boolean {
  return (projects ?? []).some((p) => p.projectName?.trim())
}

function hasValidSkill(skills?: ResumeLike['skills']): boolean {
  return (skills ?? []).some((s) => s.skillName?.trim() && s.proficiencyLevel != null)
}

function hasPartialSkill(skills?: ResumeLike['skills']): boolean {
  return (skills ?? []).some((s) => s.skillName?.trim())
}

function hasValidCertificate(certificates?: ResumeLike['certificates']): boolean {
  return (certificates ?? []).some((c) => c.name?.trim() && c.certType != null)
}

function hasMeaningfulSummary(summary?: string): boolean {
  return (summary?.trim().length ?? 0) >= 10
}

function hasWorkDescription(workExperiences?: ResumeLike['workExperiences']): boolean {
  return (workExperiences ?? []).some((w) => (w.jobDescription?.trim().length ?? 0) >= 10)
}

function hasProjectDescription(projects?: ResumeLike['projects']): boolean {
  return (projects ?? []).some((p) => (p.description?.trim().length ?? 0) >= 10)
}

/** 与后端 OnlineResumeService.calcCompleteness 规则对齐 */
export function calcOnlineResumeScore(payload: ResumeLike, workYears?: number | null): number {
  let score = 0
  const summary = payload.summary?.trim() ?? ''
  if (summary.length >= 10) score += 15
  else if (summary.length > 0) score += 5

  if (hasValidEducation(payload.educations)) score += 20
  else if (hasPartialEducation(payload.educations)) score += 10

  const fresh = isFreshGraduate(workYears)

  if (fresh) {
    if (hasValidProject(payload.projects)) score += 25
    else if (hasPartialProject(payload.projects)) score += 12
    if (hasValidWork(payload.workExperiences)) score += 5
    if (hasProjectDescription(payload.projects) || hasWorkDescription(payload.workExperiences)) score += 10
  } else {
    if (hasValidWork(payload.workExperiences)) score += 25
    else if (hasPartialWork(payload.workExperiences)) score += 12
    if (hasWorkDescription(payload.workExperiences)) score += 10
    if (hasValidProject(payload.projects)) score += 5
  }

  if (hasValidSkill(payload.skills)) score += 15
  else if (hasPartialSkill(payload.skills)) score += 6

  if (hasValidCertificate(payload.certificates)) score += 5

  return Math.min(100, score)
}

export function getOnlineResumeMissingHints(payload: ResumeLike, workYears?: number | null): string[] {
  const missing: string[] = []
  const fresh = isFreshGraduate(workYears)

  if (!hasMeaningfulSummary(payload.summary)) missing.push('个人总结（至少 10 字）')
  if (!hasValidEducation(payload.educations)) missing.push('教育经历（学校 + 学历）')
  if (!hasValidSkill(payload.skills)) missing.push('技能标签（名称 + 熟练度）')

  if (fresh) {
    if (!hasValidProject(payload.projects) && !hasValidWork(payload.workExperiences)) {
      missing.push('项目经历（项目名 + 角色 + 时间）或实习/工作经历')
    }
    if (!hasProjectDescription(payload.projects) && !hasWorkDescription(payload.workExperiences)) {
      missing.push('项目或工作描述（建议填写成果）')
    }
  } else {
    if (!hasValidWork(payload.workExperiences)) missing.push('工作经历（公司 + 职位 + 开始时间）')
    if (!hasWorkDescription(payload.workExperiences)) missing.push('工作描述（建议填写成果）')
  }

  return missing
}

export function analyzeOnlineResume(payload: ResumeLike, workYears?: number | null) {
  const score = calcOnlineResumeScore(payload, workYears)
  const missing = getOnlineResumeMissingHints(payload, workYears)
  return { score, missing, freshGraduate: isFreshGraduate(workYears) }
}

export function analyzeOnlineResumeDetail(detail: OnlineResumeDetail | null | undefined) {
  if (!detail) {
    return { score: 0, missing: ['请先创建在线简历'], freshGraduate: true }
  }
  const workYears = detail.workYears ?? null
  return analyzeOnlineResume(
    {
      summary: detail.summary,
      educations: detail.educations,
      workExperiences: detail.workExperiences,
      skills: detail.skills,
      projects: detail.projects,
      certificates: detail.certificates,
    },
    workYears,
  )
}

/** 将历史 0-100 分档或 1-4 档位统一为编辑器用的 1-4 */
export function normalizeSkillProficiencyLevel(level?: number | null): number {
  if (level == null || Number.isNaN(level)) return 3
  if (level >= 1 && level <= 4) return level
  if (level <= 25) return 1
  if (level <= 50) return 2
  if (level <= 75) return 3
  return 4
}

export function skillProficiencyLabel(level?: number | null): string {
  const normalized = normalizeSkillProficiencyLevel(level)
  return SKILL_PROFICIENCY_OPTIONS.find((o) => o.value === normalized)?.label ?? '熟练'
}

export function skillProficiencyPercent(level?: number | null): number {
  const normalized = normalizeSkillProficiencyLevel(level)
  return SKILL_PROFICIENCY_OPTIONS.find((o) => o.value === normalized)?.percent ?? 75
}
