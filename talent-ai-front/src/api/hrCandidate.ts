import request from '@/utils/request'

export interface HrCandidateBrief {
  realName?: string
  phone?: string
  email?: string
  city?: string
  currentTitle?: string
  highestEdu?: number
  aiScore?: number
}

/** HR 查看候选人档案摘要（走网关 → talent-auth） */
export function fetchHrCandidateBrief(userId: number) {
  return request.get<HrCandidateBrief>('/api/auth/hr/candidate-brief', {
    params: { userId },
  }) as Promise<HrCandidateBrief>
}
