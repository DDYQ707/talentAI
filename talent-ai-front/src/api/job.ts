import request from '@/utils/request'

export interface JobPost {
  id: number
  title: string
  deptName?: string
  publisherName?: string
  status?: number
  employmentType?: number
  workCity?: string
  isRemote?: boolean
  salaryMin?: number
  salaryMax?: number
  salaryNegotiable?: boolean
  headcount?: number
  jobDescription?: string
  jobRequirements?: string
  skillTags?: string
  appliedCount?: number
  matchedCount?: number
  priority?: number
  publishedAt?: string
  createdAt?: string
}

export interface JobPageData {
  total: number
  current: number
  pages: number
  records: JobPost[]
}

/** 招聘中岗位分页列表 */
export function fetchJobList(params?: { current?: number; size?: number; title?: string; status?: number }) {
  return request.get<JobPageData>('/api/job/list', {
    params: {
      current: params?.current ?? 1,
      size: params?.size ?? 20,
      status: params?.status ?? 1,
      title: params?.title,
    },
  }) as Promise<JobPageData>
}

/** 岗位详情 */
export function fetchJobDetail(id: number) {
  return request.get<JobPost>('/api/job/detail', { params: { id } }) as Promise<JobPost>
}
