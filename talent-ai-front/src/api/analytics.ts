import request from '@/utils/request'

export interface FunnelStage {
  stageName: string
  count: number
}

export interface DashboardMetrics {
  activeJobs: number
  totalResumes: number
  monthlyApplications: number
  initialScreenPass: number
  ongoingInterviews: number
  completedInterviewsThisMonth: number
  monthlyHired: number
  monthlyOfferSent: number
  offerAcceptRate: number
  approxMonthlyHiredFromInterview: number
  funnel?: FunnelStage[]
  placeholderFields?: string[]
}

export interface TrendPoint {
  month: string
  applications: number
  completedInterviews: number
  offers: number
}

export interface DepartmentProgress {
  dept: string
  gap: number
  active: number
}

/** HR 数据驾驶舱聚合指标 GET /api/analytics/hr/dashboard */
export function fetchHrDashboard() {
  return request.get<DashboardMetrics>('/api/analytics/hr/dashboard') as Promise<DashboardMetrics>
}

/** HR 工作台聚合指标 GET /api/analytics/hr/workbench */
export function fetchHrWorkbench() {
  return request.get<DashboardMetrics>('/api/analytics/hr/workbench') as Promise<DashboardMetrics>
}

/** 招聘趋势（近6个月） GET /api/analytics/hr/trend */
export function fetchRecruitmentTrend() {
  return request.get<TrendPoint[]>('/api/analytics/hr/trend') as Promise<TrendPoint[]>
}

/** 各部门招聘进度 GET /api/analytics/hr/department-progress */
export function fetchDepartmentProgress() {
  return request.get<DepartmentProgress[]>('/api/analytics/hr/department-progress') as Promise<DepartmentProgress[]>
}
