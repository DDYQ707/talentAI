import request from '@/utils/request'
import type { JobPageData, JobPost } from '@/api/job'

export type { JobPost, JobPageData }

/** HR 岗位列表查询参数 */
export interface HrJobListParams {
  current?: number
  size?: number
  /** 岗位名称模糊搜索 */
  title?: string
  /** 1-招聘中 2-暂停 3-已完成；不传表示全部 */
  status?: number
}

/** HR 发布/更新岗位请求体 */
export interface PublishJobPayload {
  title: string
  deptName?: string
  workCity?: string
  employmentType?: number
  salaryMin?: number
  salaryMax?: number
  salaryNegotiable?: boolean
  headcount?: number
  priority?: number
  status?: number
  jobDescription?: string
  jobRequirements?: string
  skillTags?: string
}

/**
 * HR 岗位分页列表（不过滤状态，支持按标题/状态筛选）
 * GET /api/job/list
 */
export function fetchHrJobList(params: HrJobListParams = {}) {
  const query: Record<string, string | number> = {
    current: params.current ?? 1,
    size: params.size ?? 50,
  }
  if (params.title?.trim()) query.title = params.title.trim()
  if (params.status != null) query.status = params.status

  return request.get<JobPageData>('/api/job/list', { params: query }) as Promise<JobPageData>
}

/**
 * HR 岗位详情
 * GET /api/job/detail
 */
export function fetchHrJobDetail(id: number) {
  return request.get<JobPost>('/api/job/detail', { params: { id } }) as Promise<JobPost>
}

/**
 * HR 发布岗位
 * POST /api/job/publish
 */
export function publishHrJob(data: PublishJobPayload) {
  return request.post<JobPost>('/api/job/publish', data) as Promise<JobPost>
}

/**
 * HR 更新岗位
 * PUT /api/job/{id}
 */
export function updateHrJob(id: number, data: PublishJobPayload) {
  return request.put<JobPost>(`/api/job/${id}`, data) as Promise<JobPost>
}

/**
 * HR 删除岗位（逻辑删除）
 * DELETE /api/job/{id}
 */
export function deleteHrJob(id: number) {
  return request.delete<null>(`/api/job/${id}`) as Promise<null>
}
