/** 岗位状态（与后端 job_post.status 一致） */
export const JOB_STATUS = {
  OPEN: 1,
  PAUSED: 2,
  CLOSED: 3,
} as const

/** 岗位优先级（与后端 job_post.priority 一致） */
export const JOB_PRIORITY = {
  HIGH: 1,
  MEDIUM: 2,
  LOW: 3,
} as const

export type JobStatusKey = '招聘中' | '暂停' | '已完成'

export function jobStatusLabel(status?: number | null): JobStatusKey {
  switch (status) {
    case JOB_STATUS.PAUSED:
      return '暂停'
    case JOB_STATUS.CLOSED:
      return '已完成'
    default:
      return '招聘中'
  }
}

export function jobPriorityLabel(priority?: number | null): '高' | '中' | '低' {
  switch (priority) {
    case JOB_PRIORITY.HIGH:
      return '高'
    case JOB_PRIORITY.LOW:
      return '低'
    default:
      return '中'
  }
}
