/** 候选人首页岗位分类 Tab */
export const JOB_LIST_CATEGORIES = ['推荐', '技术', '产品', '设计', '运营', '管理'] as const

export type JobListCategory = (typeof JOB_LIST_CATEGORIES)[number]

/** 分类 Tab → 部门名称（与 HR 发布岗位时的 deptName 精确一致） */
export const CATEGORY_DEPT_NAMES: Record<Exclude<JobListCategory, '推荐'>, string[]> = {
  技术: ['技术部', '技术研发部'],
  产品: ['产品部'],
  设计: ['设计部'],
  运营: ['运营部'],
  管理: ['人力资源部'],
}

export function resolveCategoryDeptNames(category: JobListCategory): string[] | undefined {
  if (category === '推荐') return undefined
  return CATEGORY_DEPT_NAMES[category]
}
