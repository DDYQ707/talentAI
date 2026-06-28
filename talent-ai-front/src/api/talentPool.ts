import request from '@/utils/request'

// ==================== 求职状态常量 ====================

/** 求职状态码 */
export const JOB_SEEKING_STATUS = {
  ACTIVE: 1,    // 主动求职
  PASSIVE: 2,   // 被动求职
  WATCHING: 3,  // 在职-观望
} as const

/** 求职状态码 → 中文标签 */
export const JOB_SEEKING_STATUS_LABELS: Record<number, string> = {
  [JOB_SEEKING_STATUS.ACTIVE]: '主动求职',
  [JOB_SEEKING_STATUS.PASSIVE]: '被动求职',
  [JOB_SEEKING_STATUS.WATCHING]: '在职-观望',
}

/** 标签类型码 */
export const TAG_TYPE = {
  SKILL: 1,     // 技能
  DOMAIN: 2,    // 领域
  CUSTOM: 3,    // 自定义
} as const

export const TAG_TYPE_LABELS: Record<number, string> = {
  [TAG_TYPE.SKILL]: '技能',
  [TAG_TYPE.DOMAIN]: '领域',
  [TAG_TYPE.CUSTOM]: '自定义',
}

// ==================== TypeScript 接口 ====================

/** 标签视图 */
export interface TalentTagVO {
  id: number
  tagName: string
  /** 1-技能 2-领域 3-自定义 */
  tagType: number
}

/** 人才库记录视图 */
export interface TalentPoolRecordVO {
  id: number
  candidateId: number
  candidateName: string
  currentTitle: string
  resumeId?: number | null
  sourceApplicationId?: number | null
  /** 来源岗位名称快照 */
  sourceJobTitle?: string | null
  /** 面试评价摘要 */
  interviewSummary?: string | null
  /** 人才分类 */
  talentCategory?: number | null
  /** 求职状态 (1-主动求职 2-被动求职 3-在职-观望) */
  jobSeekingStatus?: number | null
  /** 匹配分 (0-100) */
  matchScore: number
  currentCompany: string
  isSaved: boolean
  archiveReason?: string | null
  archivedBy?: number | null
  archivedAt?: string | null
  createdAt?: string
  /** 关联标签列表 */
  tags: TalentTagVO[]
}

/** 人才库分页查询参数 */
export interface TalentPoolQueryParams {
  current?: number
  size?: number
  /** 求职状态过滤 (1/2/3) */
  jobSeekingStatus?: number
  /** 匹配分数下限 */
  minScore?: number
  /** 匹配分数上限 */
  maxScore?: number
  /** 标签ID筛选 */
  tagId?: number
  /** 姓名、岗位、归档原因等关键词 */
  keyword?: string
}

/** 人才库分页响应 */
export interface TalentPoolPageData {
  records: TalentPoolRecordVO[]
  total: number
  current: number
  pages: number
}

/** 人才归档入库请求体 */
export interface TalentPoolArchivePayload {
  candidateId: number
  candidateName: string
  currentTitle?: string
  resumeId?: number
  sourceApplicationId?: number
  sourceJobTitle?: string
  talentCategory?: number
  jobSeekingStatus?: number
  matchScore?: number
  currentCompany?: string
  archiveReason?: string
  interviewSummary?: string
}

/** 人才库记录更新请求体 */
export interface TalentPoolUpdatePayload {
  currentTitle?: string
  talentCategory?: number
  jobSeekingStatus?: number
  matchScore?: number
  currentCompany?: string
  isSaved?: boolean
  archiveReason?: string
}

/** 标签绑定请求体 */
export interface TagBindPayload {
  tagIds: number[]
}

/** 创建标签请求体 */
export interface TalentTagCreatePayload {
  tagName: string
  /** 1-技能 2-领域 3-自定义 */
  tagType?: number
}

// ==================== API 方法 ====================

/**
 * 人才大厅分页查询（支持求职状态、匹配分数、标签筛选）
 * GET /api/talent-pool/list
 */
export function fetchTalentPoolPage(params: TalentPoolQueryParams = {}) {
  const query: Record<string, string | number> = {
    current: params.current ?? 1,
    size: params.size ?? 20,
  }
  if (params.jobSeekingStatus != null) query.jobSeekingStatus = params.jobSeekingStatus
  if (params.minScore != null) query.minScore = params.minScore
  if (params.maxScore != null) query.maxScore = params.maxScore
  if (params.tagId != null) query.tagId = params.tagId
  if (params.keyword?.trim()) query.keyword = params.keyword.trim()
  return request.get<TalentPoolPageData>('/api/talent-pool/list', { params: query }) as Promise<TalentPoolPageData>
}

/**
 * 人才归档入库
 * POST /api/talent-pool/archive
 */
export function archiveTalent(data: TalentPoolArchivePayload) {
  return request.post<TalentPoolRecordVO>('/api/talent-pool/archive', data) as Promise<TalentPoolRecordVO>
}

/** 检查候选人是否已在人才库 */
export function fetchTalentPoolExists(candidateId: number) {
  return request.get<boolean>('/api/talent-pool/exists', {
    params: { candidateId },
  }) as Promise<boolean>
}

/** 批量检查候选人是否已在人才库 */
export function fetchTalentPoolExistsBatch(candidateIds: number[]) {
  return request.post<Record<string, boolean>>('/api/talent-pool/exists-batch', {
    candidateIds,
  }) as Promise<Record<string, boolean>>
}

/**
 * 删除归档记录（逻辑删除）
 * DELETE /api/talent-pool/{id}
 */
export function deleteTalentRecord(id: number) {
  return request.delete<null>(`/api/talent-pool/${id}`) as Promise<null>
}

/**
 * 更新归档记录
 * PUT /api/talent-pool/{id}
 */
export function updateTalentRecord(id: number, data: TalentPoolUpdatePayload) {
  return request.put<null>(`/api/talent-pool/${id}`, data) as Promise<null>
}

/**
 * 为人才库记录批量绑定标签
 * POST /api/talent-pool/{poolRecordId}/tags
 */
export function bindTags(poolRecordId: number, tagIds: number[]) {
  return request.post<null>(`/api/talent-pool/${poolRecordId}/tags`, { tagIds }) as Promise<null>
}

/**
 * 移除人才库记录的某个标签
 * DELETE /api/talent-pool/{poolRecordId}/tags/{tagId}
 */
export function unbindTag(poolRecordId: number, tagId: number) {
  return request.delete<null>(`/api/talent-pool/${poolRecordId}/tags/${tagId}`) as Promise<null>
}

/**
 * 创建标签
 * POST /api/talent-pool/tags
 */
export function createTag(data: TalentTagCreatePayload) {
  return request.post<TalentTagVO>('/api/talent-pool/tags', data) as Promise<TalentTagVO>
}

/**
 * 查询所有标签列表
 * GET /api/talent-pool/tags
 */
export function fetchAllTags() {
  return request.get<TalentTagVO[]>('/api/talent-pool/tags') as Promise<TalentTagVO[]>
}
