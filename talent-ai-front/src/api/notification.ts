import request from '@/utils/request'

export interface NotificationItem {
  id: number
  title: string
  content?: string
  notifyType?: number
  notifyTypeLabel?: string
  bizType?: string
  bizId?: number
  read?: boolean
  createdAt?: string
}

export interface NotificationPageData {
  records: NotificationItem[]
  total: number
  current: number
  pages: number
}

export function fetchMyNotifications(params?: { current?: number; size?: number }) {
  return request.get<NotificationPageData>('/api/auth/notification/my', { params }) as Promise<NotificationPageData>
}

export function fetchUnreadNotificationCount() {
  return request.get<number>('/api/auth/notification/unread-count') as Promise<number>
}

export function markNotificationRead(id: number) {
  return request.put<void>(`/api/auth/notification/${id}/read`) as Promise<void>
}

export function markAllNotificationsRead() {
  return request.put<void>('/api/auth/notification/read-all') as Promise<void>
}
