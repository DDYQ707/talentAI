import request from '@/utils/request'
import type { JobPost } from '@/api/job'

export interface JobFavoriteItem {
  favoriteId: number
  jobId: number
  favoritedAt?: string
  job?: JobPost | null
}

export interface JobFavoritePageData {
  records: JobFavoriteItem[]
  total: number
  current: number
  pages: number
}

export interface FavoriteJobIdsData {
  jobIds: number[]
}

export interface ToggleFavoriteResult {
  favorited: boolean
}

/** 我的收藏列表 */
export function fetchMyFavorites(params?: { current?: number; size?: number }) {
  return request.get<JobFavoritePageData>('/api/delivery/favorites/my', {
    params: {
      current: params?.current ?? 1,
      size: params?.size ?? 20,
    },
  }) as Promise<JobFavoritePageData>
}

/** 已收藏岗位 ID 列表 */
export function fetchFavoriteJobIds() {
  return request.get<FavoriteJobIdsData>('/api/delivery/favorites/job-ids') as Promise<FavoriteJobIdsData>
}

/** 切换收藏状态 */
export function toggleJobFavorite(jobId: number) {
  return request.put<ToggleFavoriteResult>(`/api/delivery/favorites/${jobId}/toggle`) as Promise<ToggleFavoriteResult>
}
