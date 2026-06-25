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

/** HR 数据驾驶舱聚合指标 GET /api/analytics/hr/dashboard */
export function fetchHrDashboard() {
  return request.get<DashboardMetrics>('/api/analytics/hr/dashboard') as Promise<DashboardMetrics>
}

/** HR 工作台聚合指标 GET /api/analytics/hr/workbench */
export function fetchHrWorkbench() {
  return request.get<DashboardMetrics>('/api/analytics/hr/workbench') as Promise<DashboardMetrics>
}
