<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { EChartsOption } from 'echarts'
import {
  User,
  Phone,
  Mail,
  MapPin,
  GraduationCap,
  Briefcase,
  Sparkles,
  CheckCircle,
  AlertTriangle,
  Send,
  Calendar,
  Bookmark,
  TrendingUp,
  Zap,
  FileText,
  Eye,
} from 'lucide-vue-next'
import { fetchHrResumeDetail, fetchHrResumePreview, type HrResumeDetail } from '@/api/hrResume'
import { openResumePreview } from '@/api/resume'
import { screenStatusLabel } from '@/constants/resume'
import { getErrorMessage } from '@/utils/validators'

const route = useRoute()
const detail = ref<HrResumeDetail | null>(null)
const loading = ref(true)
const errorMsg = ref('')
const pdfPreviewUrl = ref('')
const previewLoading = ref(false)

const resumeId = computed(() => {
  const id = Number(route.query.id)
  return Number.isFinite(id) && id > 0 ? id : null
})

const candidate = computed(() => ({
  name: detail.value?.candidateName ?? '—',
  title: detail.value?.resumeName ?? '—',
  exp: detail.value?.fileType ? `附件 · ${detail.value.fileType.toUpperCase()}` : '暂无附件',
  edu: '—',
  location: '—',
  phone: '—',
  email: '—',
  match: 0,
  status: screenStatusLabel(detail.value?.screenStatus),
  appliedJob: detail.value?.resumeName ?? '—',
  appliedDate: detail.value?.updatedAt?.slice(0, 10) ?? '—',
  fileName: detail.value?.fileName,
  attachmentId: detail.value?.attachmentId,
}))

const radarData = [
  { subject: '技术能力', value: 95 },
  { subject: '项目经验', value: 88 },
  { subject: '学习能力', value: 90 },
  { subject: '沟通协作', value: 82 },
  { subject: '稳定性', value: 78 },
  { subject: '薪资匹配', value: 86 },
]

const workExp = [
  { company: '字节跳动', title: '高级前端工程师', period: '2021.07 - 至今', desc: '负责抖音创作者平台核心功能研发，主导重构后性能提升30%，带领5人前端团队' },
  { company: '腾讯', title: '前端工程师', period: '2019.07 - 2021.06', desc: '微信小程序团队，开发C端界面组件库，用户覆盖1亿+' },
  { company: '阿里巴巴', title: '前端实习', period: '2018.06 - 2018.12', desc: '天猫事业部，参与双十一活动前端研发' },
]

const skills = [
  { name: 'React', level: 95 },
  { name: 'Vue3', level: 90 },
  { name: 'TypeScript', level: 92 },
  { name: 'Node.js', level: 78 },
  { name: 'Webpack/Vite', level: 85 },
  { name: '性能优化', level: 88 },
]

const aiTags = [
  { label: '🔥 强烈推荐', color: 'text-brand-green bg-green-50 border-green-200' },
  { label: '⚡ 快速响应', color: 'text-brand-blue bg-brand-tint border-brand-border' },
  { label: '📚 学习能力强', color: 'text-brand-purple bg-brand-tint-2 border-brand-border' },
  { label: '⚠️ 薪资偏高', color: 'text-brand-orange bg-orange-50 border-orange-200' },
]

const interviews = [
  { round: 'AI初筛', score: 92, status: '通过', time: '2024-06-12 10:30' },
  { round: 'HR初面', score: 88, status: '待安排', time: '—' },
  { round: '技术面试', score: 0, status: '未开始', time: '—' },
  { round: '总监面试', score: 0, status: '未开始', time: '—' },
]

async function loadDetail() {
  if (!resumeId.value) {
    errorMsg.value = '缺少简历 ID'
    loading.value = false
    return
  }
  loading.value = true
  errorMsg.value = ''
  pdfPreviewUrl.value = ''
  try {
    detail.value = await fetchHrResumeDetail(resumeId.value)
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '简历详情加载失败')
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function handlePreviewInline() {
  if (!candidate.value.attachmentId) {
    errorMsg.value = '该简历暂无附件'
    return
  }
  previewLoading.value = true
  errorMsg.value = ''
  try {
    const preview = await fetchHrResumePreview(candidate.value.attachmentId)
    if (preview.fileType === 'pdf') {
      pdfPreviewUrl.value = preview.presignedUrl
    } else {
      openResumePreview(preview)
    }
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '预览失败')
  } finally {
    previewLoading.value = false
  }
}

onMounted(() => {
  loadDetail()
})

const radarOption = computed<EChartsOption>(() => ({
  radar: {
    indicator: radarData.map((d) => ({ name: d.subject, max: 100 })),
    splitLine: { lineStyle: { color: '#e2e8f0' } },
    axisName: { color: '#94a3b8', fontSize: 10 },
  },
  series: [
    {
      type: 'radar',
      data: [
        {
          value: radarData.map((d) => d.value),
          name: 'candidate',
          areaStyle: { color: 'rgba(124,58,237,0.2)' },
          lineStyle: { color: '#5a8a82', width: 2 },
          itemStyle: { color: '#5a8a82' },
        },
      ],
    },
  ],
}))
</script>

<template>
  <div data-cmp="ResumeDetail" class="flex h-full" style="height: calc(100vh - 64px)">
    <div class="w-72 flex-shrink-0 border-r border-border bg-card overflow-y-auto scrollbar-thin p-5">
      <div class="flex flex-col items-center text-center mb-5 pt-2">
        <div class="w-20 h-20 rounded-full gradient-blue-purple flex items-center justify-center mb-3 shadow-card">
          <User :size="32" class="text-white" />
        </div>
        <div class="text-lg font-bold text-foreground">{{ candidate.name }}</div>
        <div class="text-sm text-muted-foreground mt-0.5">{{ candidate.title }}</div>
        <div class="text-xs text-muted-foreground mt-1">{{ candidate.exp }}</div>
        <div class="flex flex-wrap justify-center gap-1 mt-3">
          <span v-for="t in aiTags" :key="t.label" :class="['text-xs px-2 py-0.5 rounded-full border', t.color]">{{ t.label }}</span>
        </div>
      </div>
      <div class="bg-accent rounded-2xl p-4 mb-4 text-center border border-brand-border/50">
        <div class="text-3xl font-black text-brand-purple mb-1">{{ candidate.match }}%</div>
        <div class="text-xs text-muted-foreground">AI综合匹配度</div>
      </div>
      <div class="space-y-3 mb-4">
        <div v-for="item in [
          { icon: Phone, label: '电话', value: candidate.phone },
          { icon: Mail, label: '邮箱', value: candidate.email },
          { icon: MapPin, label: '城市', value: candidate.location },
          { icon: GraduationCap, label: '学历', value: candidate.edu },
          { icon: Briefcase, label: '应聘岗位', value: candidate.appliedJob },
          { icon: Calendar, label: '投递时间', value: candidate.appliedDate },
        ]" :key="item.label" class="flex items-center gap-3">
          <div class="w-7 h-7 rounded-lg bg-muted flex items-center justify-center flex-shrink-0">
            <component :is="item.icon" :size="12" class="text-muted-foreground" />
          </div>
          <div class="flex-1 min-w-0">
            <div class="text-xs text-muted-foreground">{{ item.label }}</div>
            <div class="text-xs text-foreground font-medium truncate">{{ item.value }}</div>
          </div>
        </div>
      </div>
      <div class="flex flex-col gap-2">
        <button type="button" class="w-full py-2.5 rounded-control gradient-blue text-white text-sm font-medium flex items-center justify-center gap-2">
          <Calendar :size="14" />
          <span>安排面试</span>
        </button>
        <button type="button" class="w-full py-2.5 rounded-xl border border-border text-sm text-foreground hover:bg-muted flex items-center justify-center gap-2">
          <Send :size="14" />
          <span>发送Offer</span>
        </button>
        <button type="button" class="w-full py-2.5 rounded-xl border border-border text-sm text-muted-foreground hover:bg-muted flex items-center justify-center gap-2">
          <Bookmark :size="14" />
          <span>存入人才库</span>
        </button>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto scrollbar-thin p-6 min-w-0">
      <p v-if="errorMsg" class="text-xs text-red-600 mb-4">{{ errorMsg }}</p>
      <p v-if="loading" class="text-sm text-muted-foreground mb-4">加载中...</p>

      <div v-if="candidate.attachmentId" class="mb-6 bg-card border border-border rounded-xl p-4 shadow-card">
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2">
            <FileText :size="16" class="text-brand-blue" />
            <h2 class="text-sm font-bold text-foreground">简历附件</h2>
          </div>
          <button
            type="button"
            class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg gradient-blue text-white text-xs disabled:opacity-50"
            :disabled="previewLoading"
            @click="handlePreviewInline"
          >
            <Eye :size="14" />
            {{ previewLoading ? '加载中...' : pdfPreviewUrl ? '刷新预览' : '在线预览' }}
          </button>
        </div>
        <p class="text-xs text-muted-foreground mb-3 truncate">{{ candidate.fileName }}</p>
        <iframe
          v-if="pdfPreviewUrl"
          :src="pdfPreviewUrl"
          class="w-full rounded-lg border border-border bg-white"
          style="height: min(70vh, 720px)"
          title="简历预览"
        />
        <p v-else class="text-xs text-muted-foreground">点击「在线预览」在页面内查看 PDF；Word 文档将在新窗口打开。</p>
      </div>

      <div class="mb-6">
        <div class="flex items-center gap-2 mb-4">
          <Briefcase :size="16" class="text-brand-blue" />
          <h2 class="text-sm font-bold text-foreground">工作经历</h2>
        </div>
        <div class="space-y-4">
          <div v-for="(w, i) in workExp" :key="i" class="flex gap-4">
            <div class="flex flex-col items-center">
              <div class="w-2 h-2 rounded-full bg-brand-blue flex-shrink-0 mt-1.5" />
              <div v-if="i < workExp.length - 1" class="w-px flex-1 bg-border mt-1" />
            </div>
            <div class="flex-1 pb-4">
              <div class="flex items-center justify-between mb-1">
                <span class="text-sm font-semibold text-foreground">{{ w.company }}</span>
                <span class="text-xs text-muted-foreground">{{ w.period }}</span>
              </div>
              <div class="text-xs text-brand-blue mb-2">{{ w.title }}</div>
              <div class="text-xs text-muted-foreground leading-relaxed">{{ w.desc }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="mb-6">
        <div class="flex items-center gap-2 mb-4">
          <TrendingUp :size="16" class="text-brand-green" />
          <h2 class="text-sm font-bold text-foreground">技能掌握</h2>
        </div>
        <div class="space-y-3">
          <div v-for="s in skills" :key="s.name" class="flex items-center gap-3">
            <div class="text-xs text-foreground w-20 flex-shrink-0">{{ s.name }}</div>
            <div class="flex-1 h-2 bg-muted rounded-full overflow-hidden">
              <div
                class="h-full rounded-full transition-all"
                :style="{
                  width: `${s.level}%`,
                  background: s.level >= 90 ? 'var(--brand-purple)' : 'var(--brand-blue)',
                }"
              />
            </div>
            <div class="text-xs text-muted-foreground w-8 text-right">{{ s.level }}</div>
          </div>
        </div>
      </div>
      <div class="mb-6">
        <div class="flex items-center gap-2 mb-4">
          <Calendar :size="16" class="text-brand-orange" />
          <h2 class="text-sm font-bold text-foreground">面试进程</h2>
        </div>
        <div class="flex items-center gap-0">
          <div v-for="(iv, i) in interviews" :key="iv.round" class="flex items-center flex-1">
            <div class="flex flex-col items-center flex-1">
              <div
                class="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold"
                :class="
                  iv.status === '通过'
                    ? 'bg-brand-green text-white'
                    : iv.status === '待安排'
                      ? 'bg-brand-orange text-white'
                      : 'bg-muted text-muted-foreground'
                "
              >
                {{ i + 1 }}
              </div>
              <div class="text-xs text-foreground mt-1">{{ iv.round }}</div>
              <div
                class="text-xs mt-0.5"
                :class="
                  iv.status === '通过'
                    ? 'text-brand-green'
                    : iv.status === '待安排'
                      ? 'text-brand-orange'
                      : 'text-muted-foreground'
                "
              >
                {{ iv.status }}
              </div>
              <div v-if="iv.score > 0" class="text-xs text-brand-purple font-bold">{{ iv.score }}分</div>
              <div v-else class="text-xs text-muted-foreground">{{ iv.time }}</div>
            </div>
            <div v-if="i < interviews.length - 1" class="h-px w-8 bg-border flex-shrink-0 -mt-8" />
          </div>
        </div>
      </div>
    </div>

    <div class="w-72 flex-shrink-0 border-l border-border bg-card overflow-y-auto scrollbar-thin p-5">
      <div class="flex items-center gap-2 mb-5">
        <div class="w-7 h-7 rounded-lg gradient-purple flex items-center justify-center">
          <Sparkles :size="14" class="text-white" />
        </div>
        <span class="text-sm font-bold text-foreground">AI人才画像</span>
      </div>
      <div class="bg-muted rounded-2xl p-3 mb-4">
        <VChart :option="radarOption" autoresize style="height: 180px" />
      </div>
      <div class="bg-accent rounded-xl p-4 mb-4 border border-brand-border/50">
        <div class="text-xs font-semibold text-brand-purple mb-3">AI综合评价</div>
        <div class="space-y-2">
          <div>
            <div class="text-xs font-medium text-brand-green mb-1.5">✅ 优势</div>
            <div v-for="p in ['技术栈与岗位高度匹配', '名企经历，稳定性好', '有带团队经验']" :key="p" class="flex items-start gap-1.5 mb-1">
              <CheckCircle :size="11" class="text-brand-green mt-0.5 flex-shrink-0" />
              <span class="text-xs text-muted-foreground">{{ p }}</span>
            </div>
          </div>
          <div class="pt-2 border-t border-brand-border/50">
            <div class="text-xs font-medium text-brand-orange mb-1.5">⚠️ 注意</div>
            <div v-for="r in ['薪资期望高于预算15%', '近期有多家公司面试']" :key="r" class="flex items-start gap-1.5 mb-1">
              <AlertTriangle :size="11" class="text-brand-orange mt-0.5 flex-shrink-0" />
              <span class="text-xs text-muted-foreground">{{ r }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="bg-card rounded-xl p-4 border border-border">
        <div class="flex items-center gap-2 mb-3">
          <Zap :size="14" class="text-brand-blue" />
          <span class="text-xs font-semibold text-foreground">推荐面试问题</span>
        </div>
        <div class="space-y-2">
          <div v-for="(q, i) in ['描述一次主导性能优化的经历，提升了多少?', '如何管理和培养前端团队成员?', '当前薪资和期望薪资的具体要求?']" :key="i" class="flex gap-2 text-xs text-muted-foreground">
            <span class="text-brand-blue font-bold flex-shrink-0">Q{{ i + 1 }}.</span>
            <span>{{ q }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
