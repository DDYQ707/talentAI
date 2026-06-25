import request from '@/utils/request'

/* ---- 字典类型 ---- */

export interface DictType {
  id: number
  code: string
  name: string
  status: number       // 1=启用 0=禁用
  remark?: string
  createdAt?: string
}

export interface DictTypeSavePayload {
  code: string
  name: string
  status?: number
  remark?: string
}

/* ---- 字典项 ---- */

export interface DictItem {
  id: number
  dictTypeId: number
  label: string
  value: string
  sort: number
  status: number       // 1=启用 0=禁用
  createdAt?: string
}

export interface DictItemSavePayload {
  dictTypeId: number
  label: string
  value: string
  sort?: number
  status?: number
}

/* ---- 字典类型 API ---- */

export function listDictTypes() {
  return request.get<DictType[]>('/api/admin/dict/types') as Promise<DictType[]>
}

export function createDictType(data: DictTypeSavePayload) {
  return request.post<DictType>('/api/admin/dict/types', data) as Promise<DictType>
}

export function updateDictType(id: number, data: Partial<DictTypeSavePayload>) {
  return request.put<DictType>(`/api/admin/dict/types/${id}`, data) as Promise<DictType>
}

export function deleteDictType(id: number) {
  return request.delete<null>(`/api/admin/dict/types/${id}`) as Promise<null>
}

/* ---- 字典项 API ---- */

export function listDictItems(typeId: number) {
  return request.get<DictItem[]>('/api/admin/dict/items', { params: { typeId } }) as Promise<DictItem[]>
}

export function createDictItem(data: DictItemSavePayload) {
  return request.post<DictItem>('/api/admin/dict/items', data) as Promise<DictItem>
}

export function updateDictItem(id: number, data: Partial<DictItemSavePayload>) {
  return request.put<DictItem>(`/api/admin/dict/items/${id}`, data) as Promise<DictItem>
}

export function deleteDictItem(id: number) {
  return request.delete<null>(`/api/admin/dict/items/${id}`) as Promise<null>
}
