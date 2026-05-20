import type { JobPost } from '@/api/job'

/** 月薪展示，如 25-35K */
export function formatJobSalary(job: Pick<JobPost, 'salaryMin' | 'salaryMax' | 'salaryNegotiable'>): string {
  if (job.salaryNegotiable) return '面议'
  const min = job.salaryMin
  const max = job.salaryMax
  if (min != null && max != null) {
    return `${Math.round(min / 1000)}-${Math.round(max / 1000)}K`
  }
  if (min != null) return `${Math.round(min / 1000)}K+`
  if (max != null) return `≤${Math.round(max / 1000)}K`
  return '薪资面议'
}

export function formatEmploymentType(type?: number): string {
  switch (type) {
    case 2:
      return '兼职'
    case 3:
      return '实习'
    default:
      return '全职'
  }
}

export function parseSkillTags(tags?: string | null): string[] {
  if (!tags?.trim()) return []
  return tags.split(/[,，]/).map((t) => t.trim()).filter(Boolean)
}

export function formatDateTime(value?: string | null): string {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
