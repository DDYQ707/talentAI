import type { JobPost } from '@/api/job'
import { jobPriorityLabel, jobStatusLabel } from '@/constants/job'
import { formatDateTime, formatEmploymentType, formatJobSalary } from '@/utils/jobFormat'

/** HR 岗位管理列表行（页面展示用） */
export interface HrJobRow {
  id: number
  title: string
  dept: string
  status: ReturnType<typeof jobStatusLabel>
  type: string
  location: string
  salary: string
  headcount: number
  applied: number
  matched: number
  created: string
  priority: ReturnType<typeof jobPriorityLabel>
  raw: JobPost
}

export function mapJobPostToHrRow(job: JobPost): HrJobRow {
  return {
    id: job.id,
    title: job.title,
    dept: job.deptName || '—',
    status: jobStatusLabel(job.status),
    type: formatEmploymentType(job.employmentType),
    location: job.workCity || (job.isRemote ? '远程' : '—'),
    salary: formatJobSalary(job),
    headcount: job.headcount ?? 1,
    applied: job.appliedCount ?? 0,
    matched: job.matchedCount ?? 0,
    created: formatDateTime(job.publishedAt || job.createdAt),
    priority: jobPriorityLabel(job.priority),
    raw: job,
  }
}
