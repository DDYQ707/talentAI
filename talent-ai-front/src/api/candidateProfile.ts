import request from '@/utils/request'

export interface CandidateProfile {
  userId: number
  username?: string
  nickname?: string
  phone?: string
  email?: string
  avatarUrl?: string
  realName?: string
  gender?: number
  birthDate?: string
  currentTitle?: string
  workYears?: number
  highestEdu?: number
  city?: string
  jobSeekingStatus?: number
  resumeCompleteness?: number
  aiScore?: number
  profileComplete?: boolean
}

export interface CandidateProfileSavePayload {
  realName: string
  phone: string
  email?: string
  gender?: number
  birthDate?: string
  currentTitle?: string
  workYears?: number
  highestEdu: number
  city: string
  jobSeekingStatus?: number
}

export interface ProfileCompleteness {
  complete: boolean
  completeness: number
  missingFields: string[]
}

export function fetchMyCandidateProfile() {
  return request.get<CandidateProfile>('/api/auth/candidate/profile/my') as Promise<CandidateProfile>
}

export function saveMyCandidateProfile(payload: CandidateProfileSavePayload) {
  return request.put<CandidateProfile>('/api/auth/candidate/profile/my', payload) as Promise<CandidateProfile>
}

export function fetchProfileCompleteness() {
  return request.get<ProfileCompleteness>('/api/auth/candidate/profile/complete') as Promise<ProfileCompleteness>
}
