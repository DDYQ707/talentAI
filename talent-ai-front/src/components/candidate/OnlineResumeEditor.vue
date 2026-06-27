<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Trash2, Save, GraduationCap, Briefcase, Code, User, ChevronRight, AlertCircle, FolderKanban, Award } from 'lucide-vue-next'
import {
  createOnlineResume,
  fetchOnlineResumeDetail,
  fetchOnlineResumeList,
  updateOnlineResume,
  deleteOnlineResume,
  type OnlineResumeDetail,
  type OnlineResumeListItem,
  type OnlineResumeSavePayload,
  type OnlineWorkExperience,
  type OnlineProject,
  type OnlineCertificate,
} from '@/api/onlineResume'
import { fetchMyCandidateProfile, type CandidateProfile } from '@/api/candidateProfile'
import {
  CERT_TYPE_OPTIONS,
  EDU_DEGREE_OPTIONS,
  EXPERIENCE_TYPE_OPTIONS,
  PROJECT_DESC_PLACEHOLDER,
  PROJECT_ROLE_PLACEHOLDER,
  SKILL_PROFICIENCY_OPTIONS,
  SUMMARY_PLACEHOLDER,
  WORK_DESC_PLACEHOLDER,
} from '@/constants/onlineResume'
import { profileDisplayName, profileSubtitle } from '@/constants/candidateProfile'
import {
  analyzeOnlineResume,
  calcOnlineResumeScore,
  isFreshGraduate,
  normalizeSkillProficiencyLevel,
} from '@/utils/onlineResumeCompleteness'
import { getErrorMessage } from '@/utils/validators'

type WorkFormItem = OnlineWorkExperience & { isCurrent?: boolean }
type ProjectFormItem = OnlineProject & { isCurrent?: boolean }

const emit = defineEmits<{
  saved: []
}>()

const props = withDefaults(
  defineProps<{
    embedded?: boolean
    active?: boolean
    initialResumeId?: number | null
    focusSection?: 'education' | 'work' | 'projects' | 'skills' | 'certificates' | ''
  }>(),
  { embedded: false, active: true, initialResumeId: null, focusSection: '' },
)

const router = useRouter()

const list = ref<OnlineResumeListItem[]>([])
const currentId = ref<number | null>(null)
const profile = ref<CandidateProfile | null>(null)
const form = ref<OnlineResumeSavePayload & { workExperiences?: WorkFormItem[]; projects?: ProjectFormItem[] }>({
  resumeName: '我的在线简历',
  summary: '',
  educations: [],
  workExperiences: [],
  skills: [],
  projects: [],
  certificates: [],
})
const detailWorkYears = ref<number | null>(null)
const completeness = ref(0)
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const errorMsg = ref('')

const hasResume = computed(() => currentId.value != null)
const workYears = computed(() => {
  if (profile.value?.workYears != null) return Number(profile.value.workYears)
  return detailWorkYears.value
})
const freshGraduate = computed(() => isFreshGraduate(workYears.value))
const localScore = computed(() => calcOnlineResumeScore(form.value, workYears.value))
const localMissing = computed(() => analyzeOnlineResume(form.value, workYears.value).missing)

function defaultExperienceType() {
  return freshGraduate.value ? 2 : 1
}

function emptyEducation() {
  return { schoolName: '', major: '', degree: undefined as number | undefined, startDate: '', endDate: '' }
}

function emptyWork(): WorkFormItem {
  return {
    companyName: '',
    jobTitle: '',
    experienceType: defaultExperienceType(),
    startDate: '',
    endDate: '',
    jobDescription: '',
    isCurrent: false,
  }
}

function emptyProject(): ProjectFormItem {
  return {
    projectName: '',
    role: '',
    techStack: '',
    startDate: '',
    endDate: '',
    description: '',
    linkUrl: '',
    isCurrent: false,
  }
}

function emptyCertificate(): OnlineCertificate {
  return { certType: 1, name: '', issuer: '', issueDate: '', description: '' }
}

function emptySkill() {
  return { skillName: '', proficiencyLevel: 3 }
}

function applyDetail(detail: OnlineResumeDetail) {
  currentId.value = detail.id
  detailWorkYears.value = detail.workYears != null ? Number(detail.workYears) : null
  completeness.value = detail.completeness ?? calcOnlineResumeScore(detail, workYears.value)
  form.value = {
    resumeName: detail.resumeName,
    summary: detail.summary ?? '',
    educations: detail.educations?.length ? detail.educations.map((e) => ({ ...e })) : [],
    workExperiences: detail.workExperiences?.length
      ? detail.workExperiences.map((w) => ({
          ...w,
          experienceType: w.experienceType ?? defaultExperienceType(),
          isCurrent: !w.endDate,
        }))
      : [],
    skills: detail.skills?.length
      ? detail.skills.map((s) => ({
          ...s,
          proficiencyLevel: normalizeSkillProficiencyLevel(s.proficiencyLevel),
        }))
      : [],
    projects: detail.projects?.length
      ? detail.projects.map((p) => ({
          ...p,
          isCurrent: !p.endDate,
        }))
      : [],
    certificates: detail.certificates?.length ? detail.certificates.map((c) => ({ ...c })) : [],
  }
}

async function loadProfile() {
  if (props.embedded) return
  try {
    profile.value = await fetchMyCandidateProfile()
  } catch {
    profile.value = null
  }
}

async function loadList() {
  try {
    list.value = (await fetchOnlineResumeList()) ?? []
  } catch {
    list.value = []
  }
}

async function loadDetail(id: number) {
  loading.value = true
  errorMsg.value = ''
  try {
    const detail = await fetchOnlineResumeDetail(id)
    applyDetail(detail)
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '加载失败')
  } finally {
    loading.value = false
  }
}

async function ensureResume() {
  if (currentId.value) return currentId.value
  const detail = await createOnlineResume({
    resumeName: form.value.resumeName || '我的在线简历',
    summary: form.value.summary,
    educations: [],
    workExperiences: [],
    skills: [],
    projects: [],
    certificates: [],
  })
  applyDetail(detail)
  await loadList()
  return detail.id
}

function normMonth(d?: string) {
  if (!d) return undefined
  if (/^\d{4}-\d{2}$/.test(d)) return `${d}-01`
  return d
}

function normalizePayload(): OnlineResumeSavePayload {
  return {
    resumeName: form.value.resumeName,
    summary: form.value.summary?.trim(),
    educations: form.value.educations
      ?.filter((e) => e.schoolName?.trim())
      .map((e) => ({
        ...e,
        schoolName: e.schoolName.trim(),
        major: e.major?.trim() || undefined,
        startDate: normMonth(e.startDate),
        endDate: normMonth(e.endDate),
      })),
    workExperiences: form.value.workExperiences
      ?.filter((w) => w.companyName?.trim() && w.jobTitle?.trim() && w.startDate?.trim())
      .map((w) => ({
        companyName: w.companyName.trim(),
        jobTitle: w.jobTitle.trim(),
        experienceType: w.experienceType ?? defaultExperienceType(),
        startDate: normMonth(w.startDate),
        endDate: w.isCurrent ? undefined : normMonth(w.endDate),
        jobDescription: w.jobDescription?.trim() || undefined,
      })),
    projects: form.value.projects
      ?.filter((p) => p.projectName?.trim() && p.startDate?.trim())
      .map((p) => ({
        projectName: p.projectName.trim(),
        role: p.role?.trim() || undefined,
        techStack: p.techStack?.trim() || undefined,
        startDate: normMonth(p.startDate),
        endDate: p.isCurrent ? undefined : normMonth(p.endDate),
        description: p.description?.trim() || undefined,
        linkUrl: p.linkUrl?.trim() || undefined,
      })),
    certificates: form.value.certificates
      ?.filter((c) => c.name?.trim() && c.certType != null)
      .map((c) => ({
        certType: c.certType,
        name: c.name.trim(),
        issuer: c.issuer?.trim() || undefined,
        issueDate: normMonth(c.issueDate),
        description: c.description?.trim() || undefined,
      })),
    skills: form.value.skills
      ?.filter((s) => s.skillName?.trim())
      .map((s) => ({
        skillName: s.skillName.trim(),
        proficiencyLevel: normalizeSkillProficiencyLevel(s.proficiencyLevel),
      })),
  }
}

function validateForm(): string | null {
  const payload = normalizePayload()
  const { missing } = analyzeOnlineResume(payload, workYears.value)
  if (!payload.summary?.trim()) return '请填写个人总结'
  if (missing.includes('教育经历（学校 + 学历）')) return '请至少填写一段教育经历（学校 + 学历）'
  if (missing.some((m) => m.includes('项目经历') || m.includes('工作经历'))) {
    return freshGraduate.value
      ? '请至少填写一段项目经历，或一段实习/工作经历'
      : '请至少填写一段工作经历（公司 + 职位 + 开始时间）'
  }
  for (const w of form.value.workExperiences ?? []) {
    if (w.companyName?.trim() || w.jobTitle?.trim()) {
      if (!w.companyName?.trim() || !w.jobTitle?.trim()) return '工作经历请填写公司与职位'
      if (!w.startDate?.trim()) return '工作经历请填写开始时间'
    }
  }
  for (const p of form.value.projects ?? []) {
    if (p.projectName?.trim() && !p.startDate?.trim()) return '项目经历请填写开始时间'
  }
  return null
}

async function handleSave() {
  const validationError = validateForm()
  if (validationError) {
    errorMsg.value = validationError
    return
  }

  saving.value = true
  message.value = ''
  errorMsg.value = ''
  try {
    const id = await ensureResume()
    const detail = await updateOnlineResume(id, normalizePayload())
    applyDetail(detail)
    message.value = `已保存 · 完整度 ${detail.completeness ?? localScore.value}%`
    await loadList()
    emit('saved')
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleNew() {
  currentId.value = null
  form.value = {
    resumeName: '我的在线简历',
    summary: '',
    educations: [emptyEducation()],
    workExperiences: [emptyWork()],
    skills: [],
    projects: freshGraduate.value ? [emptyProject()] : [],
    certificates: [],
  }
  completeness.value = 0
  try {
    const detail = await createOnlineResume(normalizePayload())
    applyDetail(detail)
    message.value = '已创建新简历'
    await loadList()
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '创建失败')
  }
}

async function handleDelete() {
  if (!currentId.value || !confirm('确定删除该在线简历？')) return
  try {
    await deleteOnlineResume(currentId.value)
    currentId.value = null
    form.value = {
      resumeName: '我的在线简历',
      summary: '',
      educations: [],
      workExperiences: [],
      skills: [],
      projects: [],
      certificates: [],
    }
    completeness.value = 0
    message.value = '已删除'
    await loadList()
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '删除失败')
  }
}

function addEducation() {
  form.value.educations = [...(form.value.educations ?? []), emptyEducation()]
}

function addWork() {
  form.value.workExperiences = [...(form.value.workExperiences ?? []), emptyWork()]
}

function addSkill() {
  form.value.skills = [...(form.value.skills ?? []), emptySkill()]
}

function addProject() {
  form.value.projects = [...(form.value.projects ?? []), emptyProject()]
}

function addCertificate() {
  form.value.certificates = [...(form.value.certificates ?? []), emptyCertificate()]
}

function toggleWorkCurrent(w: WorkFormItem, checked: boolean) {
  w.isCurrent = checked
  if (checked) w.endDate = ''
}

function toggleProjectCurrent(p: ProjectFormItem, checked: boolean) {
  p.isCurrent = checked
  if (checked) p.endDate = ''
}

function scrollToFocusSection() {
  const section = props.focusSection
  if (!section) return
  nextTick(() => {
    document.getElementById(`section-${section}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

function goProfileEdit() {
  router.push('/candidate/profile/edit')
}

async function init() {
  await Promise.all([loadList(), loadProfile()])
  const initId = props.initialResumeId ?? (list.value.length > 0 ? list.value[0].id : null)
  if (initId) {
    await loadDetail(initId)
  } else {
    form.value.educations = [emptyEducation()]
    form.value.workExperiences = [emptyWork()]
    form.value.projects = freshGraduate.value ? [emptyProject()] : []
  }
  scrollToFocusSection()
}

watch(
  () => props.active,
  (v) => {
    if (v && !props.embedded) init()
  },
)

onMounted(() => {
  if (props.embedded || props.active !== false) init()
})

defineExpose({ completeness, handleSave, localScore })
</script>

<template>
  <div class="space-y-4">
    <div v-if="!embedded && profile" class="rounded-xl border border-border bg-card p-3">
      <div class="flex items-center gap-3">
        <div class="w-9 h-9 rounded-full bg-brand-tint flex items-center justify-center shrink-0">
          <User :size="16" class="text-brand-blue" />
        </div>
        <div class="flex-1 min-w-0">
          <div class="text-sm font-semibold text-foreground truncate">{{ profileDisplayName(profile) }}</div>
          <div class="text-xs text-muted-foreground truncate">{{ profileSubtitle(profile) }}</div>
        </div>
        <button
          type="button"
          class="text-xs text-brand-blue flex items-center gap-0.5 shrink-0"
          @click="goProfileEdit"
        >
          完善档案
          <ChevronRight :size="12" />
        </button>
      </div>
      <p class="text-[11px] text-muted-foreground mt-2">
        姓名、手机、城市、工作年限等在个人档案维护。
        <span v-if="freshGraduate" class="text-brand-blue">当前按「应届/在校生」规则评估完整度，请重点填写项目经历。</span>
        <span v-else class="text-brand-blue">当前按「在职」规则评估，请重点填写工作经历。</span>
      </p>
    </div>

    <div class="flex items-center gap-2">
      <div class="flex-1 h-1.5 bg-muted rounded-full overflow-hidden">
        <div
          class="h-full rounded-full gradient-blue transition-all"
          :style="{ width: `${currentId ? completeness : localScore}%` }"
        />
      </div>
      <span class="text-xs font-semibold text-brand-blue">{{ currentId ? completeness : localScore }}%</span>
      <span class="text-xs text-muted-foreground">完整度</span>
    </div>

    <div
      v-if="localMissing.length && localScore < 80"
      class="flex items-start gap-2 rounded-xl border border-amber-200 bg-amber-50 px-3 py-2"
    >
      <AlertCircle :size="14" class="text-amber-600 shrink-0 mt-0.5" />
      <p class="text-[11px] text-amber-800 leading-relaxed">
        建议完善：{{ localMissing.join('、') }}（完整度 ≥80% 更适合投递）
      </p>
    </div>

    <div v-if="list.length > 1 && !embedded" class="flex flex-wrap gap-2">
      <button
        v-for="item in list"
        :key="item.id"
        type="button"
        class="text-xs px-2.5 py-1 rounded-full border"
        :class="currentId === item.id ? 'border-brand-blue bg-brand-tint text-brand-blue' : 'border-border text-muted-foreground'"
        @click="loadDetail(item.id)"
      >
        {{ item.resumeName }}
      </button>
    </div>

    <div v-if="embedded" class="bg-card shadow-card border border-border rounded-xl p-4 space-y-2">
      <label class="text-xs text-muted-foreground">个人总结</label>
      <textarea
        v-model="form.summary"
        rows="2"
        class="w-full text-sm px-3 py-2 rounded-lg border border-border bg-background outline-none focus:border-brand-blue resize-none"
        :placeholder="SUMMARY_PLACEHOLDER"
      />
    </div>

    <div v-if="!embedded" class="bg-card border border-border rounded-xl p-3 space-y-2">
      <label class="text-xs text-muted-foreground">简历名称</label>
      <input
        v-model="form.resumeName"
        class="w-full text-sm px-3 py-2 rounded-lg border border-border bg-background outline-none focus:border-brand-blue"
        placeholder="例如：在线简历 v1"
      />
      <label class="text-xs text-muted-foreground">个人总结 <span class="text-red-500">*</span></label>
      <textarea
        v-model="form.summary"
        rows="3"
        class="w-full text-sm px-3 py-2 rounded-lg border border-border bg-background outline-none focus:border-brand-blue resize-none"
        :placeholder="SUMMARY_PLACEHOLDER"
      />
    </div>

    <div id="section-education" class="bg-card shadow-card border border-border rounded-xl p-4 scroll-mt-4">
      <div class="flex items-center justify-between mb-2">
        <div class="flex items-center gap-2 text-sm font-semibold">
          <GraduationCap :size="15" class="text-brand-blue" />
          教育经历
        </div>
        <button type="button" class="text-xs text-brand-blue flex items-center gap-1" @click="addEducation">
          <Plus :size="12" /> 添加
        </button>
      </div>
      <div
        v-for="(edu, i) in form.educations"
        :key="i"
        class="border border-border rounded-lg p-2 mb-2 space-y-2"
      >
        <input v-model="edu.schoolName" placeholder="学校 *" class="w-full text-xs px-2 py-1.5 rounded border border-border" />
        <div class="flex gap-2">
          <input v-model="edu.major" placeholder="专业" class="flex-1 text-xs px-2 py-1.5 rounded border border-border" />
          <select
            v-model.number="edu.degree"
            class="w-24 text-xs px-2 py-1.5 rounded border border-border bg-background"
          >
            <option :value="undefined" disabled>学历 *</option>
            <option v-for="o in EDU_DEGREE_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
        </div>
        <div class="flex gap-2">
          <input v-model="edu.startDate" type="month" title="开始时间" class="flex-1 text-xs px-2 py-1.5 rounded border border-border" />
          <input v-model="edu.endDate" type="month" title="结束时间" class="flex-1 text-xs px-2 py-1.5 rounded border border-border" />
        </div>
        <button
          type="button"
          class="text-xs text-red-500 flex items-center gap-1"
          @click="form.educations?.splice(i, 1)"
        >
          <Trash2 :size="12" /> 删除
        </button>
      </div>
      <p v-if="!form.educations?.length" class="text-xs text-muted-foreground">暂无，点击添加</p>
    </div>

    <div id="section-work" class="bg-card shadow-card border border-border rounded-xl p-4 scroll-mt-4">
      <div class="flex items-center justify-between mb-2">
        <div class="flex items-center gap-2 text-sm font-semibold">
          <Briefcase :size="15" class="text-brand-purple" />
          {{ freshGraduate ? '实习 / 工作经历' : '工作经历' }}
        </div>
        <button type="button" class="text-xs text-brand-blue flex items-center gap-1" @click="addWork">
          <Plus :size="12" /> 添加
        </button>
      </div>
      <p v-if="freshGraduate" class="text-[11px] text-muted-foreground mb-2">无正式工作经历时可填实习；也可只填下方项目经历。</p>
      <div v-for="(w, i) in form.workExperiences" :key="i" class="border border-border rounded-lg p-2 mb-2 space-y-2">
        <div class="flex gap-2">
          <input v-model="w.companyName" placeholder="公司 *" class="flex-1 text-xs px-2 py-1.5 rounded border border-border" />
          <select v-model.number="w.experienceType" class="w-20 text-xs px-2 py-1.5 rounded border border-border bg-background">
            <option v-for="o in EXPERIENCE_TYPE_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
        </div>
        <input v-model="w.jobTitle" placeholder="职位 *" class="w-full text-xs px-2 py-1.5 rounded border border-border" />
        <div class="flex gap-2 items-center">
          <input
            v-model="w.startDate"
            type="month"
            title="开始时间 *"
            class="flex-1 text-xs px-2 py-1.5 rounded border border-border"
          />
          <input
            v-model="w.endDate"
            type="month"
            title="结束时间"
            class="flex-1 text-xs px-2 py-1.5 rounded border border-border"
            :disabled="w.isCurrent"
          />
        </div>
        <label class="flex items-center gap-1.5 text-xs text-muted-foreground">
          <input
            type="checkbox"
            :checked="!!w.isCurrent"
            class="rounded border-border"
            @change="toggleWorkCurrent(w, ($event.target as HTMLInputElement).checked)"
          />
          至今（在职）
        </label>
        <textarea
          v-model="w.jobDescription"
          rows="3"
          :placeholder="WORK_DESC_PLACEHOLDER"
          class="w-full text-xs px-2 py-1.5 rounded border border-border resize-none"
        />
        <button type="button" class="text-xs text-red-500 flex items-center gap-1" @click="form.workExperiences?.splice(i, 1)">
          <Trash2 :size="12" /> 删除
        </button>
      </div>
    </div>

    <div id="section-projects" class="bg-card shadow-card border border-border rounded-xl p-4 scroll-mt-4">
      <div class="flex items-center justify-between mb-2">
        <div class="flex items-center gap-2 text-sm font-semibold">
          <FolderKanban :size="15" class="text-brand-orange" />
          项目经历
          <span v-if="freshGraduate" class="text-[10px] font-normal text-red-500">* 应届建议必填</span>
        </div>
        <button type="button" class="text-xs text-brand-blue flex items-center gap-1" @click="addProject">
          <Plus :size="12" /> 添加
        </button>
      </div>
      <div v-for="(p, i) in form.projects" :key="i" class="border border-border rounded-lg p-2 mb-2 space-y-2">
        <input v-model="p.projectName" placeholder="项目名称 *" class="w-full text-xs px-2 py-1.5 rounded border border-border" />
        <input v-model="p.role" :placeholder="PROJECT_ROLE_PLACEHOLDER + ' *'" class="w-full text-xs px-2 py-1.5 rounded border border-border" />
        <input v-model="p.techStack" placeholder="技术栈，如 Vue3 / Spring Boot" class="w-full text-xs px-2 py-1.5 rounded border border-border" />
        <div class="flex gap-2 items-center">
          <input v-model="p.startDate" type="month" title="开始时间 *" class="flex-1 text-xs px-2 py-1.5 rounded border border-border" />
          <input v-model="p.endDate" type="month" title="结束时间" class="flex-1 text-xs px-2 py-1.5 rounded border border-border" :disabled="p.isCurrent" />
        </div>
        <label class="flex items-center gap-1.5 text-xs text-muted-foreground">
          <input type="checkbox" :checked="!!p.isCurrent" class="rounded border-border" @change="toggleProjectCurrent(p, ($event.target as HTMLInputElement).checked)" />
          进行中
        </label>
        <textarea v-model="p.description" rows="3" :placeholder="PROJECT_DESC_PLACEHOLDER" class="w-full text-xs px-2 py-1.5 rounded border border-border resize-none" />
        <input v-model="p.linkUrl" placeholder="项目链接 / GitHub（选填）" class="w-full text-xs px-2 py-1.5 rounded border border-border" />
        <button type="button" class="text-xs text-red-500 flex items-center gap-1" @click="form.projects?.splice(i, 1)">
          <Trash2 :size="12" /> 删除
        </button>
      </div>
      <p v-if="!form.projects?.length" class="text-xs text-muted-foreground">暂无，点击添加（课设、竞赛、开源、毕设均可）</p>
    </div>

    <div id="section-skills" class="bg-card shadow-card border border-border rounded-xl p-4 scroll-mt-4">
      <div class="flex items-center justify-between mb-2">
        <div class="flex items-center gap-2 text-sm font-semibold">
          <Code :size="15" class="text-brand-green" />
          技能标签
        </div>
        <button type="button" class="text-xs text-brand-blue flex items-center gap-1" @click="addSkill">
          <Plus :size="12" /> 添加
        </button>
      </div>
      <div v-for="(s, i) in form.skills" :key="i" class="flex items-center gap-2 mb-2">
        <input v-model="s.skillName" placeholder="如 Java、React" class="flex-1 text-xs px-2 py-1.5 rounded border border-border" />
        <select
          v-model.number="s.proficiencyLevel"
          class="w-20 text-xs px-2 py-1.5 rounded border border-border bg-background"
        >
          <option v-for="o in SKILL_PROFICIENCY_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
        <button type="button" class="p-1 text-red-500" @click="form.skills?.splice(i, 1)">
          <Trash2 :size="14" />
        </button>
      </div>
      <p v-if="!form.skills?.length" class="text-xs text-muted-foreground">暂无，点击添加</p>
    </div>

    <div id="section-certificates" class="bg-card shadow-card border border-border rounded-xl p-4 scroll-mt-4">
      <div class="flex items-center justify-between mb-2">
        <div class="flex items-center gap-2 text-sm font-semibold">
          <Award :size="15" class="text-brand-purple" />
          证书与荣誉
        </div>
        <button type="button" class="text-xs text-brand-blue flex items-center gap-1" @click="addCertificate">
          <Plus :size="12" /> 添加
        </button>
      </div>
      <p class="text-[11px] text-muted-foreground mb-2">含职业证书、竞赛荣誉、专业技术职称（如中级工程师）等，选填可加分。</p>
      <div v-for="(c, i) in form.certificates" :key="i" class="border border-border rounded-lg p-2 mb-2 space-y-2">
        <div class="flex gap-2">
          <select v-model.number="c.certType" class="w-20 text-xs px-2 py-1.5 rounded border border-border bg-background">
            <option v-for="o in CERT_TYPE_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
          <input v-model="c.name" placeholder="名称 *" class="flex-1 text-xs px-2 py-1.5 rounded border border-border" />
        </div>
        <input v-model="c.issuer" placeholder="颁发/发证单位" class="w-full text-xs px-2 py-1.5 rounded border border-border" />
        <input v-model="c.issueDate" type="month" title="获得时间" class="w-full text-xs px-2 py-1.5 rounded border border-border" />
        <input v-model="c.description" placeholder="补充说明（选填，如成绩、等级）" class="w-full text-xs px-2 py-1.5 rounded border border-border" />
        <button type="button" class="text-xs text-red-500 flex items-center gap-1" @click="form.certificates?.splice(i, 1)">
          <Trash2 :size="12" /> 删除
        </button>
      </div>
      <p v-if="!form.certificates?.length" class="text-xs text-muted-foreground">暂无，如四六级、软考、奖学金等</p>
    </div>

    <div class="flex gap-2">
      <button
        type="button"
        class="flex-1 py-2.5 rounded-xl border border-border text-xs"
        @click="handleNew"
      >
        新建
      </button>
      <button
        v-if="hasResume"
        type="button"
        class="py-2.5 px-3 rounded-xl border border-red-200 text-xs text-red-600"
        @click="handleDelete"
      >
        删除
      </button>
      <button
        type="button"
        class="flex-1 py-2.5 rounded-xl gradient-blue text-white text-xs font-medium flex items-center justify-center gap-1 disabled:opacity-50"
        :disabled="saving || loading"
        @click="handleSave"
      >
        <Save :size="14" />
        {{ saving ? '保存中...' : '保存' }}
      </button>
    </div>

    <p v-if="message" class="text-xs text-brand-green">{{ message }}</p>
    <p v-if="errorMsg" class="text-xs text-red-600">{{ errorMsg }}</p>
  </div>
</template>
