<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { Plus, Trash2, Save, GraduationCap, Briefcase, Code } from 'lucide-vue-next'
import {
  createOnlineResume,
  fetchOnlineResumeDetail,
  fetchOnlineResumeList,
  updateOnlineResume,
  deleteOnlineResume,
  type OnlineResumeDetail,
  type OnlineResumeListItem,
  type OnlineResumeSavePayload,
} from '@/api/onlineResume'
import { getErrorMessage } from '@/utils/validators'

const emit = defineEmits<{
  saved: []
}>()

const props = withDefaults(
  defineProps<{
    embedded?: boolean
    active?: boolean
    initialResumeId?: number | null
    /** 进入编辑页时滚动到对应区块 */
    focusSection?: 'education' | 'work' | 'skills' | ''
  }>(),
  { embedded: false, active: true, initialResumeId: null, focusSection: '' },
)

const list = ref<OnlineResumeListItem[]>([])
const currentId = ref<number | null>(null)
const form = ref<OnlineResumeSavePayload>({
  resumeName: '我的在线简历',
  summary: '',
  educations: [],
  workExperiences: [],
  skills: [],
})
const completeness = ref(0)
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const errorMsg = ref('')

const hasResume = computed(() => currentId.value != null)

function emptyEducation() {
  return { schoolName: '', major: '', startDate: '', endDate: '' }
}

function emptyWork() {
  return { companyName: '', jobTitle: '', startDate: '', endDate: '', jobDescription: '' }
}

function emptySkill() {
  return { skillName: '', proficiencyLevel: 80 }
}

function applyDetail(detail: OnlineResumeDetail) {
  currentId.value = detail.id
  completeness.value = detail.completeness ?? 0
  form.value = {
    resumeName: detail.resumeName,
    summary: detail.summary ?? '',
    educations: detail.educations?.length ? [...detail.educations] : [],
    workExperiences: detail.workExperiences?.length ? [...detail.workExperiences] : [],
    skills: detail.skills?.length ? [...detail.skills] : [],
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
  })
  applyDetail(detail)
  await loadList()
  return detail.id
}

function normalizePayload(): OnlineResumeSavePayload {
  const normMonth = (d?: string) => {
    if (!d) return undefined
    if (/^\d{4}-\d{2}$/.test(d)) return `${d}-01`
    return d
  }
  return {
    ...form.value,
    educations: form.value.educations?.map((e) => ({
      ...e,
      startDate: normMonth(e.startDate),
      endDate: normMonth(e.endDate),
    })),
    workExperiences: form.value.workExperiences?.map((w) => ({
      ...w,
      startDate: normMonth(w.startDate),
      endDate: normMonth(w.endDate),
    })),
  }
}

async function handleSave() {
  saving.value = true
  message.value = ''
  errorMsg.value = ''
  try {
    const id = await ensureResume()
    const detail = await updateOnlineResume(id, normalizePayload())
    applyDetail(detail)
    message.value = '已保存'
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
    form.value = { resumeName: '我的在线简历', summary: '', educations: [], workExperiences: [], skills: [] }
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

function scrollToFocusSection() {
  const section = props.focusSection
  if (!section) return
  nextTick(() => {
    document.getElementById(`section-${section}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

async function init() {
  await loadList()
  const initId = props.initialResumeId ?? (list.value.length > 0 ? list.value[0].id : null)
  if (initId) {
    await loadDetail(initId)
  } else {
    form.value.educations = [emptyEducation()]
    form.value.workExperiences = [emptyWork()]
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

defineExpose({ completeness, handleSave })
</script>

<template>
  <div class="space-y-4">
    <div v-if="!embedded" class="flex items-center gap-2">
      <div class="flex-1 h-1.5 bg-muted rounded-full overflow-hidden">
        <div class="h-full rounded-full gradient-blue transition-all" :style="{ width: `${completeness}%` }" />
      </div>
      <span class="text-xs font-semibold text-brand-blue">{{ completeness }}%</span>
      <span class="text-xs text-muted-foreground">完整度</span>
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
        placeholder="简要介绍你的优势与求职意向..."
      />
    </div>

    <div v-if="!embedded" class="bg-card border border-border rounded-xl p-3 space-y-2">
      <label class="text-xs text-muted-foreground">简历名称</label>
      <input
        v-model="form.resumeName"
        class="w-full text-sm px-3 py-2 rounded-lg border border-border bg-background outline-none focus:border-brand-blue"
        placeholder="例如：在线简历 v1"
      />
      <label class="text-xs text-muted-foreground">个人总结</label>
      <textarea
        v-model="form.summary"
        rows="3"
        class="w-full text-sm px-3 py-2 rounded-lg border border-border bg-background outline-none focus:border-brand-blue resize-none"
        placeholder="简要介绍你的优势与求职意向..."
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
        <input v-model="edu.major" placeholder="专业" class="w-full text-xs px-2 py-1.5 rounded border border-border" />
        <div class="flex gap-2">
          <input v-model="edu.startDate" type="month" class="flex-1 text-xs px-2 py-1.5 rounded border border-border" />
          <input v-model="edu.endDate" type="month" class="flex-1 text-xs px-2 py-1.5 rounded border border-border" />
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
          工作经历
        </div>
        <button type="button" class="text-xs text-brand-blue flex items-center gap-1" @click="addWork">
          <Plus :size="12" /> 添加
        </button>
      </div>
      <div v-for="(w, i) in form.workExperiences" :key="i" class="border border-border rounded-lg p-2 mb-2 space-y-2">
        <input v-model="w.companyName" placeholder="公司 *" class="w-full text-xs px-2 py-1.5 rounded border border-border" />
        <input v-model="w.jobTitle" placeholder="职位 *" class="w-full text-xs px-2 py-1.5 rounded border border-border" />
        <div class="flex gap-2">
          <input v-model="w.startDate" type="month" class="flex-1 text-xs px-2 py-1.5 rounded border border-border" />
          <input v-model="w.endDate" type="month" class="flex-1 text-xs px-2 py-1.5 rounded border border-border" />
        </div>
        <textarea
          v-model="w.jobDescription"
          rows="2"
          placeholder="工作描述"
          class="w-full text-xs px-2 py-1.5 rounded border border-border resize-none"
        />
        <button type="button" class="text-xs text-red-500 flex items-center gap-1" @click="form.workExperiences?.splice(i, 1)">
          <Trash2 :size="12" /> 删除
        </button>
      </div>
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
        <input v-model="s.skillName" placeholder="技能名" class="flex-1 text-xs px-2 py-1.5 rounded border border-border" />
        <input
          v-model.number="s.proficiencyLevel"
          type="number"
          min="0"
          max="100"
          class="w-14 text-xs px-2 py-1.5 rounded border border-border"
        />
        <button type="button" class="p-1 text-red-500" @click="form.skills?.splice(i, 1)">
          <Trash2 :size="14" />
        </button>
      </div>
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
