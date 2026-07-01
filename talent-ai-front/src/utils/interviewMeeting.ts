import { INTERVIEW_MODE, formatInterviewDateTime } from '@/constants/interview'

export function isMeetingUrl(value: string): boolean {
  return /^https?:\/\//i.test(value.trim())
}

export function canJoinMeeting(item: {
  meetingUrl?: string | null
  interviewMode?: number
}): boolean {
  return (
    item.interviewMode === INTERVIEW_MODE.VIDEO &&
    Boolean(item.meetingUrl?.trim())
  )
}

export function openMeeting(meetingUrl: string) {
  const trimmed = meetingUrl.trim()
  if (isMeetingUrl(trimmed)) {
    window.open(trimmed, '_blank', 'noopener,noreferrer')
    return
  }
  void copyMeetingInfo(trimmed)
}

export async function copyMeetingInfo(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text.trim())
    return true
  } catch {
    return false
  }
}

export function buildMeetingCopyText(item: {
  candidateName: string
  jobTitle: string
  scheduledStart?: string | null
  meetingUrl?: string | null
  interviewModeLabel?: string
}): string {
  const lines = [
    `${item.candidateName} · ${item.jobTitle}`,
    `时间：${formatInterviewDateTime(item.scheduledStart)}`,
  ]
  if (item.interviewModeLabel) {
    lines.push(`形式：${item.interviewModeLabel}`)
  }
  if (item.meetingUrl?.trim()) {
    lines.push(`会议：${item.meetingUrl.trim()}`)
  }
  return lines.join('\n')
}

export function isScheduledToday(iso?: string | null): boolean {
  if (!iso) return false
  const normalized = iso.includes('T') ? iso : iso.replace(' ', 'T')
  const scheduled = new Date(normalized)
  if (Number.isNaN(scheduled.getTime())) return false
  const now = new Date()
  return (
    scheduled.getFullYear() === now.getFullYear() &&
    scheduled.getMonth() === now.getMonth() &&
    scheduled.getDate() === now.getDate()
  )
}
