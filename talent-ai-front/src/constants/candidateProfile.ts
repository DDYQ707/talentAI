import { formatDegree } from '@/utils/resumeFormat'

export const HIGHEST_EDU_OPTIONS = [
  { value: 1, label: '大专' },
  { value: 2, label: '本科' },
  { value: 3, label: '硕士' },
  { value: 4, label: '博士' },
] as const

export const GENDER_OPTIONS = [
  { value: 0, label: '未知' },
  { value: 1, label: '男' },
  { value: 2, label: '女' },
] as const

export const JOB_SEEKING_OPTIONS = [
  { value: 1, label: '在职观望' },
  { value: 2, label: '主动求职' },
  { value: 3, label: '被动求职' },
] as const

export function profileDisplayName(profile: { realName?: string; nickname?: string } | null | undefined): string {
  if (!profile) return '未设置姓名'
  const name = profile.realName?.trim() || profile.nickname?.trim()
  return name || '未设置姓名'
}

export function profileSubtitle(profile: {
  currentTitle?: string
  city?: string
  highestEdu?: number
} | null | undefined): string {
  if (!profile) return '请完善个人信息'
  const parts: string[] = []
  if (profile.currentTitle?.trim()) parts.push(profile.currentTitle.trim())
  if (profile.city?.trim()) parts.push(profile.city.trim())
  const edu = formatDegree(profile.highestEdu)
  if (edu) parts.push(edu)
  return parts.length ? parts.join(' · ') : '请完善个人信息'
}
