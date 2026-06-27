import request from '@/utils/request'

export interface OnlineEducation {
  id?: number
  schoolName: string
  major?: string
  degree?: number
  startDate?: string
  endDate?: string
  description?: string
  sortOrder?: number
}

export interface OnlineWorkExperience {
  id?: number
  companyName: string
  jobTitle: string
  /** 1-全职 2-实习 3-兼职 */
  experienceType?: number
  startDate?: string
  endDate?: string
  jobDescription?: string
  sortOrder?: number
}

export interface OnlineProject {
  id?: number
  projectName: string
  role?: string
  techStack?: string
  startDate?: string
  endDate?: string
  description?: string
  linkUrl?: string
  sortOrder?: number
}

export interface OnlineCertificate {
  id?: number
  /** 1-证书 2-荣誉 3-职称 */
  certType: number
  name: string
  issuer?: string
  issueDate?: string
  description?: string
  sortOrder?: number
}

export interface OnlineSkill {
  id?: number
  skillName: string
  proficiencyLevel?: number
  sortOrder?: number
}

export interface OnlineResumeListItem {
  id: number
  resumeName: string
  summary?: string
  isDefault?: number
  completeness?: number
  updatedAt?: string
}

export interface OnlineResumeDetail {
  id: number
  resumeName: string
  summary?: string
  isDefault?: number
  completeness?: number
  createdAt?: string
  updatedAt?: string
  educations: OnlineEducation[]
  workExperiences: OnlineWorkExperience[]
  skills: OnlineSkill[]
  projects?: OnlineProject[]
  certificates?: OnlineCertificate[]
  workYears?: number | null
}

export interface OnlineResumeSavePayload {
  resumeName?: string
  summary?: string
  isDefault?: number
  educations?: OnlineEducation[]
  workExperiences?: OnlineWorkExperience[]
  skills?: OnlineSkill[]
  projects?: OnlineProject[]
  certificates?: OnlineCertificate[]
}

export function fetchOnlineResumeList() {
  return request.get<OnlineResumeListItem[]>('/api/resume/online/my') as Promise<OnlineResumeListItem[]>
}

export function fetchOnlineResumeDetail(id: number) {
  return request.get<OnlineResumeDetail>(`/api/resume/online/${id}`) as Promise<OnlineResumeDetail>
}

export function createOnlineResume(payload?: OnlineResumeSavePayload) {
  return request.post<OnlineResumeDetail>('/api/resume/online', payload ?? {}) as Promise<OnlineResumeDetail>
}

export function updateOnlineResume(id: number, payload: OnlineResumeSavePayload) {
  return request.put<OnlineResumeDetail>(`/api/resume/online/${id}`, payload) as Promise<OnlineResumeDetail>
}

export function deleteOnlineResume(id: number) {
  return request.delete(`/api/resume/online/${id}`)
}
