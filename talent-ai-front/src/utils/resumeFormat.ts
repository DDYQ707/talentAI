/** 年份，如 2014-06-01 → 2014 */
export function formatResumeYear(date?: string | null): string {
  if (!date) return ''
  return date.slice(0, 4)
}

/** 时间段：2014-2018 或 2021-至今 */
export function formatResumePeriod(start?: string | null, end?: string | null): string {
  const s = formatResumeYear(start)
  if (!s) return ''
  const e = end ? formatResumeYear(end) : '至今'
  return `${s}-${e}`
}

const DEGREE_LABEL: Record<number, string> = {
  1: '大专',
  2: '本科',
  3: '硕士',
  4: '博士',
}

export function formatDegree(degree?: number | null): string {
  if (degree == null) return ''
  return DEGREE_LABEL[degree] ?? ''
}

export function formatEducationSub(major?: string | null, degree?: number | null): string {
  const parts: string[] = []
  if (major) parts.push(major)
  const d = formatDegree(degree)
  if (d) parts.push(d)
  return parts.join(' · ')
}

export function formatWorkSub(jobTitle: string, start?: string | null, end?: string | null): string {
  const period = formatResumePeriod(start, end)
  return period ? `${jobTitle} · ${period}` : jobTitle
}

export {
  skillProficiencyLabel,
  skillProficiencyPercent,
} from '@/utils/onlineResumeCompleteness'
