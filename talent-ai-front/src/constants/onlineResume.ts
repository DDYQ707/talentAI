/** 教育经历学历（与 resume_education.degree 一致） */
export const EDU_DEGREE_OPTIONS = [
  { value: 1, label: '大专' },
  { value: 2, label: '本科' },
  { value: 3, label: '硕士' },
  { value: 4, label: '博士' },
] as const

/** 工作经历类型 */
export const EXPERIENCE_TYPE_OPTIONS = [
  { value: 1, label: '全职' },
  { value: 2, label: '实习' },
  { value: 3, label: '兼职' },
] as const

/** 证书/荣誉/职称类型 */
export const CERT_TYPE_OPTIONS = [
  { value: 1, label: '证书' },
  { value: 2, label: '荣誉' },
  { value: 3, label: '职称' },
] as const

/** 技能熟练度（存库 proficiency_level：1-4 语义档位） */
export const SKILL_PROFICIENCY_OPTIONS = [
  { value: 1, label: '了解', percent: 25 },
  { value: 2, label: '熟悉', percent: 50 },
  { value: 3, label: '熟练', percent: 75 },
  { value: 4, label: '精通', percent: 100 },
] as const

export const ONLINE_RESUME_MIN_SUBMIT_SCORE = 80

export const SUMMARY_PLACEHOLDER =
  '【求职意向】如：前端工程师｜【经验】3年｜【优势】熟悉 React/TypeScript，有大型项目交付经验…'

export const WORK_DESC_PLACEHOLDER =
  '描述主要职责与成果，建议量化：负责…；完成…；提升效率/指标 …%'

export const PROJECT_DESC_PLACEHOLDER =
  '项目背景、你的职责、技术方案与成果（建议量化）'

export const PROJECT_ROLE_PLACEHOLDER = '如：核心开发 / 项目负责人'

export function formatExperienceType(type?: number | null): string {
  return EXPERIENCE_TYPE_OPTIONS.find((o) => o.value === type)?.label ?? '全职'
}

export function formatCertType(type?: number | null): string {
  return CERT_TYPE_OPTIONS.find((o) => o.value === type)?.label ?? '证书'
}
