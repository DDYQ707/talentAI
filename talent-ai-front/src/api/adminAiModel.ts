import request from '@/utils/request'

/* ---- 数据模型 ---- */

export interface AiModelItem {
  id: number
  modelCode: string
  modelName: string
  modelType: number          // 1-语言 2-嵌入 3-文档解析
  purpose?: string
  apiEndpoint?: string
  promptTemplate?: string
  status: number             // 1-运行中 2-暂停
  createdAt?: string
  updatedAt?: string
}

export interface AiModelPage {
  records: AiModelItem[]
  total: number
  page: number
  size: number
}

export interface ListAiModelsParams {
  page?: number
  size?: number
  keyword?: string
  status?: number
  modelType?: number
}

export interface AiModelSaveRequest {
  modelCode: string
  modelName: string
  modelType: number
  purpose?: string
  apiEndpoint?: string
  promptTemplate?: string
  status?: number
}

export interface ModelStats {
  activeModels: number
  todayCalls: number
  totalTokens: number
  estimatedCost: number
  avgLatency: number
}

export interface UsageTrendItem {
  date: string
  calls: number
}

export interface UsageDistItem {
  name: string
  count: number
}

/* ---- 常量 ---- */

export const MODEL_TYPE = {
  LANGUAGE: 1,
  EMBEDDING: 2,
  DOC_PARSE: 3,
} as const

export const MODEL_TYPE_LABEL: Record<number, string> = {
  1: '语言模型',
  2: '嵌入模型',
  3: '文档解析',
}

export const MODEL_TYPE_OPTIONS = [
  { label: '语言模型', value: MODEL_TYPE.LANGUAGE },
  { label: '嵌入模型', value: MODEL_TYPE.EMBEDDING },
  { label: '文档解析', value: MODEL_TYPE.DOC_PARSE },
]

export const MODEL_STATUS = {
  RUNNING: 1,
  PAUSED: 2,
} as const

export function modelTypeLabel(type: number): string {
  return MODEL_TYPE_LABEL[type] ?? '未知'
}

/* ---- API 接口 ---- */

export function listAiModels(params: ListAiModelsParams = {}) {
  return request.get<AiModelPage>('/api/admin/ai-models', { params }) as unknown as Promise<AiModelPage>
}

export function createAiModel(data: AiModelSaveRequest) {
  return request.post<number>('/api/admin/ai-models', data) as unknown as Promise<number>
}

export function updateAiModel(id: number, data: AiModelSaveRequest) {
  return request.put<null>(`/api/admin/ai-models/${id}`, data) as unknown as Promise<null>
}

export function deleteAiModel(id: number) {
  return request.delete<null>(`/api/admin/ai-models/${id}`) as unknown as Promise<null>
}

export function updateAiModelStatus(id: number, status: 1 | 2) {
  return request.put<null>(`/api/admin/ai-models/${id}/status`, { status }) as unknown as Promise<null>
}

export function fetchModelStats() {
  return request.get<ModelStats>('/api/admin/ai-models/stats') as unknown as Promise<ModelStats>
}

export function fetchUsageTrend() {
  return request.get<UsageTrendItem[]>('/api/admin/ai-models/usage-trend') as unknown as Promise<UsageTrendItem[]>
}

export function fetchUsageDistribution() {
  return request.get<UsageDistItem[]>('/api/admin/ai-models/usage-distribution') as unknown as Promise<UsageDistItem[]>
}