import request from '@/utils/request'

/* ======================================================================
 *  Module 7 — 全链路数据态势感知大盘 (Global Situation Awareness Dashboard)
 * ====================================================================== */

/** Tier-1 核心指标快照 */
export interface DashboardOverview {
  totalUsers: number
  totalUsersTrend: number          // 环比百分比，正数上涨负数下跌
  totalEnterprises: number
  totalEnterprisesTrend: number
  todayDeliveryPeak: number
  todayDeliveryPeakTrend: number
  aiRiskBlocked: number
  aiRiskBlockedTrend: number
}

/** Tier-2 供需趋势（按日） */
export interface SupplyDemandTrendItem {
  date: string             // e.g. '06-01'
  resumeDeliveries: number // 简历投递数
  jobPublications: number  // 职位发布数
}

/** Tier-2 行业人才聚集度 */
export interface IndustryTalentItem {
  industry: string
  value: number
}

/** Tier-3 微服务健康 */
export interface ServiceHealthItem {
  name: string
  status: 'UP' | 'DOWN'
  latency: number          // ms
  uptime: string           // e.g. '99.97%'
  lastChecked: string      // ISO datetime
}

/** 大盘聚合响应 */
export interface DashboardData {
  overview: DashboardOverview
  supplyDemandTrend: SupplyDemandTrendItem[]
  industryTalent: IndustryTalentItem[]
  serviceHealth: ServiceHealthItem[]
}

export function fetchDashboardData() {
  return request.get<DashboardData>('/api/admin/dashboard') as Promise<DashboardData>
}

/* ======================================================================
 *  Module 8 — 全站物料与系统广播中心 (Global Content & Broadcast Console)
 * ====================================================================== */

/* ---- 轮播图管理 ---- */

export interface BannerItem {
  id: number
  title: string
  imageUrl: string
  linkUrl: string
  startTime: string       // ISO datetime
  endTime: string
  status: 0 | 1           // 0=下线 1=上线
  sortOrder: number
  createdAt?: string
}

export interface BannerListResult {
  records: BannerItem[]
  total: number
}

export function listBanners(params: { page?: number; size?: number } = {}) {
  return request.get<BannerListResult>('/api/admin/banners', { params }) as Promise<BannerListResult>
}

export function updateBannerStatus(id: number, status: 0 | 1) {
  return request.put<null>(`/api/admin/banners/${id}/status`, { status }) as Promise<null>
}

export function deleteBanner(id: number) {
  return request.delete<null>(`/api/admin/banners/${id}`) as Promise<null>
}

/* ---- 系统公告管理 ---- */

export type AnnouncementLevel = 'info' | 'warning' | 'critical'
export type AnnouncementTarget = 'candidate' | 'hr' | 'all'

export interface AnnouncementItem {
  id: number
  title: string
  content: string
  level: AnnouncementLevel
  target: AnnouncementTarget
  broadcasted: boolean
  broadcastedAt?: string
  createdAt?: string
}

export interface AnnouncementListResult {
  records: AnnouncementItem[]
  total: number
}

export function listAnnouncements(params: { page?: number; size?: number } = {}) {
  return request.get<AnnouncementListResult>('/api/admin/announcements', { params }) as Promise<AnnouncementListResult>
}

export function broadcastAnnouncement(id: number) {
  return request.post<null>(`/api/admin/announcements/${id}/broadcast`) as Promise<null>
}

export function deleteAnnouncement(id: number) {
  return request.delete<null>(`/api/admin/announcements/${id}`) as Promise<null>
}
