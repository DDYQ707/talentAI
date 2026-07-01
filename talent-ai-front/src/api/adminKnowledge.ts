import request from '@/utils/request'

export interface KnowledgeDoc {
  id: number
  title: string
  category: string
  sourcePath?: string | null
  status?: number
  chunkCount?: number
  createdAt?: string
  updatedAt?: string
}

export interface KnowledgeImportPayload {
  title: string
  category: string
  content: string
  sourcePath?: string
}

export const KNOWLEDGE_CATEGORY_OPTIONS = [
  { value: 'process', label: '招聘流程 (process)' },
  { value: 'faq', label: '常见问题 (faq)' },
  { value: 'policy', label: '流程制度 (policy)' },
  { value: 'jd_template', label: '岗位模板 (jd_template)' },
] as const

export function knowledgeCategoryLabel(category?: string | null): string {
  if (!category) return '—'
  const hit = KNOWLEDGE_CATEGORY_OPTIONS.find((item) => item.value === category.toLowerCase())
  return hit?.label ?? category
}

export function listKnowledgeDocs() {
  return request.get<KnowledgeDoc[]>('/api/ai/knowledge/docs') as Promise<KnowledgeDoc[]>
}

export function importKnowledgeDoc(data: KnowledgeImportPayload) {
  return request.post<KnowledgeDoc>('/api/ai/knowledge/import', data, {
    timeout: 120000,
  }) as Promise<KnowledgeDoc>
}

export function reindexKnowledgeDoc(docId: number) {
  return request.post<KnowledgeDoc>(`/api/ai/knowledge/${docId}/reindex`, null, {
    timeout: 120000,
  }) as Promise<KnowledgeDoc>
}
