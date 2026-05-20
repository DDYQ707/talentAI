<script setup lang="ts">
import { onActivated, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Eye,
  Download,
  Sparkles,
  CheckCircle,
  AlertTriangle,
  GraduationCap,
  Briefcase,
  Code,
  Edit,
} from 'lucide-vue-next'
import AttachmentResumePanel from '@/components/candidate/AttachmentResumePanel.vue'
import {
  createOnlineResume,
  fetchOnlineResumeDetail,
  fetchOnlineResumeList,
  type OnlineResumeDetail,
} from '@/api/onlineResume'
import {
  formatEducationSub,
  formatResumePeriod,
  formatWorkSub,
} from '@/utils/resumeFormat'
import { getErrorMessage } from '@/utils/validators'

const router = useRouter()

const detail = ref<OnlineResumeDetail | null>(null)
const loading = ref(false)
const errorMsg = ref('')

const completeness = ref(0)

const aiScoreTags = [
  { label: '技术栈完整', ok: true },
  { label: '经历详实', ok: true },
  { label: '量化不足', ok: false },
]

const aiSuggestions = [
  '建议补充量化成果，如「性能提升30%」',
  '可添加作品集或 GitHub 链接',
  '建议增加自我评价模块',
]

async function loadOnlineResume() {
  loading.value = true
  errorMsg.value = ''
  try {
    const list = await fetchOnlineResumeList()
    if (!list?.length) {
      detail.value = null
      completeness.value = 0
      return
    }
    detail.value = await fetchOnlineResumeDetail(list[0].id)
    completeness.value = detail.value.completeness ?? 0
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '加载在线简历失败')
    detail.value = null
  } finally {
    loading.value = false
  }
}

function goEdit(section?: 'education' | 'work' | 'skills') {
  const id = detail.value?.id
  router.push({
    path: '/candidate/resume/edit',
    query: {
      ...(id != null ? { id: String(id) } : {}),
      ...(section ? { section } : {}),
    },
  })
}

async function ensureAndEdit() {
  if (detail.value?.id) {
    goEdit()
    return
  }
  try {
    const created = await createOnlineResume({ resumeName: '我的在线简历' })
    router.push({ path: '/candidate/resume/edit', query: { id: String(created.id) } })
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '创建简历失败')
  }
}

onMounted(() => {
  loadOnlineResume()
})

onActivated(() => {
  loadOnlineResume()
})
</script>

<template>
  <div data-cmp="Resume" class="flex h-full flex-col bg-[#EBF4F0]">
    <div class="px-4 pt-4 pb-3 bg-card border-b border-border flex-shrink-0">
      <div class="flex items-center justify-between mb-1">
        <h1 class="text-base font-bold text-foreground">我的简历</h1>
        <div class="flex items-center gap-2">
          <button type="button" class="p-1.5 rounded-lg hover:bg-muted" title="预览">
            <Eye :size="16" class="text-muted-foreground" />
          </button>
          <button type="button" class="p-1.5 rounded-lg hover:bg-muted" title="下载">
            <Download :size="16" class="text-muted-foreground" />
          </button>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <div class="flex-1 h-1.5 bg-muted rounded-full overflow-hidden">
          <div
            class="h-full rounded-full gradient-blue transition-all"
            :style="{ width: `${completeness}%` }"
          />
        </div>
        <span class="text-xs font-semibold text-brand-blue">{{ completeness }}%</span>
        <span class="text-xs text-muted-foreground">完整度</span>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto scrollbar-thin px-4 py-4 pb-4 space-y-4">
      <div class="gradient-blue-purple rounded-2xl p-4 text-white">
        <div class="flex items-center gap-2 mb-2">
          <Sparkles :size="14" />
          <span class="text-sm font-semibold">AI简历评分</span>
        </div>
        <div class="flex items-end gap-2 mb-3">
          <span class="text-4xl font-black">88</span>
          <span class="text-white/70 mb-1">/ 100</span>
        </div>
        <div class="flex flex-wrap gap-1.5">
          <span
            v-for="t in aiScoreTags"
            :key="t.label"
            class="text-[11px] px-2 py-0.5 rounded-full border"
            :class="t.ok ? 'bg-white/15 border-white/30' : 'bg-white/10 border-white/20 line-through opacity-80'"
          >
            {{ t.ok ? '✓' : '×' }} {{ t.label }}
          </span>
        </div>
      </div>

      <div class="bg-accent rounded-2xl p-4 border border-brand-border/50">
        <div class="flex items-center gap-2 mb-3">
          <Sparkles :size="14" class="text-brand-purple" />
          <span class="text-xs font-semibold text-brand-purple">AI优化建议</span>
        </div>
        <ul class="space-y-2">
          <li
            v-for="(tip, i) in aiSuggestions"
            :key="i"
            class="flex items-start gap-2 text-xs text-muted-foreground"
          >
            <AlertTriangle v-if="i === 2" :size="12" class="text-brand-orange flex-shrink-0 mt-0.5" />
            <CheckCircle v-else :size="12" class="text-brand-green flex-shrink-0 mt-0.5" />
            <span>{{ i + 1 }}. {{ tip }}</span>
          </li>
        </ul>
        <button type="button" class="mt-3 w-full py-2 rounded-xl gradient-purple text-white text-xs font-medium">
          AI一键优化
        </button>
      </div>

      <p v-if="errorMsg" class="text-xs text-red-600">{{ errorMsg }}</p>
      <p v-if="loading" class="text-xs text-muted-foreground text-center py-4">加载中...</p>

      <!-- 教育经历 -->
      <div class="bg-card shadow-card p-4 border border-border">
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2">
            <GraduationCap :size="15" class="text-brand-blue" />
            <span class="text-sm font-semibold text-foreground">教育经历</span>
          </div>
          <button
            type="button"
            class="p-1.5 rounded-lg hover:bg-muted"
            title="编辑"
            @click="detail ? goEdit('education') : ensureAndEdit()"
          >
            <Edit :size="14" class="text-muted-foreground" />
          </button>
        </div>
        <template v-if="detail?.educations?.length">
          <div
            v-for="(edu, i) in detail.educations"
            :key="edu.id ?? i"
            class="py-2 border-b border-border last:border-0"
          >
            <div class="flex items-start justify-between gap-2">
              <div class="text-xs font-semibold text-foreground">{{ edu.schoolName }}</div>
              <span class="text-xs text-muted-foreground flex-shrink-0">
                {{ formatResumePeriod(edu.startDate, edu.endDate) }}
              </span>
            </div>
            <div class="text-xs text-muted-foreground mt-0.5">
              {{ formatEducationSub(edu.major, edu.degree) || '—' }}
            </div>
          </div>
        </template>
        <p v-else class="text-xs text-muted-foreground py-2">暂无教育经历，点击右上角编辑</p>
      </div>

      <!-- 工作经历 -->
      <div class="bg-card shadow-card p-4 border border-border">
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2">
            <Briefcase :size="15" class="text-brand-purple" />
            <span class="text-sm font-semibold text-foreground">工作经历</span>
          </div>
          <button
            type="button"
            class="p-1.5 rounded-lg hover:bg-muted"
            title="编辑"
            @click="detail ? goEdit('work') : ensureAndEdit()"
          >
            <Edit :size="14" class="text-muted-foreground" />
          </button>
        </div>
        <template v-if="detail?.workExperiences?.length">
          <div
            v-for="(w, i) in detail.workExperiences"
            :key="w.id ?? i"
            class="py-2 border-b border-border last:border-0"
          >
            <div class="text-xs font-semibold text-foreground">{{ w.companyName }}</div>
            <div class="text-xs text-muted-foreground mt-0.5">
              {{ formatWorkSub(w.jobTitle, w.startDate, w.endDate) }}
            </div>
          </div>
        </template>
        <p v-else class="text-xs text-muted-foreground py-2">暂无工作经历，点击右上角编辑</p>
      </div>

      <!-- 技能标签 -->
      <div class="bg-card shadow-card p-4 border border-border">
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2">
            <Code :size="15" class="text-brand-green" />
            <span class="text-sm font-semibold text-foreground">技能标签</span>
          </div>
          <button
            type="button"
            class="p-1.5 rounded-lg hover:bg-muted"
            title="编辑"
            @click="detail ? goEdit('skills') : ensureAndEdit()"
          >
            <Edit :size="14" class="text-muted-foreground" />
          </button>
        </div>
        <div v-if="detail?.skills?.length" class="flex flex-wrap gap-1.5">
          <span
            v-for="(s, i) in detail.skills"
            :key="s.id ?? i"
            class="text-xs px-2 py-1 rounded-full bg-muted text-muted-foreground"
          >
            {{ s.skillName }}
          </span>
        </div>
        <p v-else class="text-xs text-muted-foreground py-2">暂无技能标签，点击右上角编辑</p>
      </div>

      <div class="pt-2 border-t border-border">
        <h2 class="text-sm font-semibold text-foreground mb-3 flex items-center gap-2">
          附件简历
          <span class="text-[11px] font-normal text-muted-foreground">（pdf/doc/docx，用于岗位投递）</span>
        </h2>
        <AttachmentResumePanel embedded />
      </div>
    </div>
  </div>
</template>
