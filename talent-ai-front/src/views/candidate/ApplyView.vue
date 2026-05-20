<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChevronLeft, FileText, Sparkles, CheckCircle, Upload, X, PenLine } from 'lucide-vue-next'
import { submitApplication } from '@/api/delivery'
import { fetchOnlineResumeList, type OnlineResumeListItem } from '@/api/onlineResume'
import { deleteResume, fetchAttachmentResumes, uploadResumeFile, type ResumeListItem } from '@/api/resume'
import { getErrorMessage } from '@/utils/validators'

interface ResumeOption {
  id: number
  name: string
  date: string
}

const router = useRouter()
const route = useRoute()

const activeTab = ref<'attachment' | 'online'>('attachment')
const resumes = ref<ResumeOption[]>([])
const onlineResumes = ref<OnlineResumeListItem[]>([])
const selectedResumeId = ref<number | null>(null)
const loadingOnline = ref(false)
const pendingFile = ref<File | null>(null)
const loadingResumes = ref(false)
const uploading = ref(false)
const deletingId = ref<number | null>(null)
const submitting = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const fileInputRef = ref<HTMLInputElement | null>(null)

const jobId = computed(() => {
  const id = Number(route.query.jobId)
  return Number.isFinite(id) && id > 0 ? id : null
})

const jobTitle = computed(() => String(route.query.title || '岗位'))
const jobSalary = computed(() => String(route.query.salary || ''))
const jobDept = computed(() => String(route.query.dept || ''))

function formatResumeDate(iso?: string) {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `更新于 ${y}-${m}-${day}`
}

function mapResumeItem(item: ResumeListItem): ResumeOption {
  const name = item.fileName ? item.resumeName || item.fileName : item.resumeName
  return {
    id: item.id,
    name,
    date: formatResumeDate(item.updatedAt),
  }
}

async function loadResumes(keepSelection = true) {
  loadingResumes.value = true
  try {
    const list = await fetchAttachmentResumes()
    const prev = selectedResumeId.value
    resumes.value = (list ?? []).map(mapResumeItem)
    if (resumes.value.length === 0) {
      selectedResumeId.value = null
    } else if (keepSelection && prev != null && resumes.value.some((r) => r.id === prev)) {
      selectedResumeId.value = prev
    } else {
      selectedResumeId.value = resumes.value[0].id
    }
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '加载简历列表失败')
  } finally {
    loadingResumes.value = false
  }
}

function selectResume(id: number) {
  selectedResumeId.value = selectedResumeId.value === id ? null : id
}

function triggerUpload() {
  if (uploading.value) return
  fileInputRef.value?.click()
}

function clearPendingFile() {
  pendingFile.value = null
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

function onFileSelected(ev: Event) {
  const input = ev.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  errorMsg.value = ''
  const ext = file.name.split('.').pop()?.toLowerCase() ?? ''
  if (!['pdf', 'doc', 'docx'].includes(ext)) {
    errorMsg.value = '仅支持 pdf、doc、docx 格式'
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    errorMsg.value = '文件大小不能超过 10MB'
    return
  }

  pendingFile.value = file
}

async function confirmUpload() {
  if (!pendingFile.value || uploading.value) return

  uploading.value = true
  errorMsg.value = ''
  const file = pendingFile.value
  try {
    const result = await uploadResumeFile(file)
    clearPendingFile()
    await loadResumes(false)
    selectedResumeId.value = result.resumeId
    successMsg.value = '简历上传成功'
    setTimeout(() => {
      successMsg.value = ''
    }, 2000)
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '上传失败，请稍后重试')
  } finally {
    uploading.value = false
  }
}

async function handleRemoveResume(id: number) {
  if (deletingId.value != null) return
  if (!confirm('确定移除该简历？')) return

  deletingId.value = id
  errorMsg.value = ''
  try {
    await deleteResume(id)
    if (selectedResumeId.value === id) {
      selectedResumeId.value = null
    }
    await loadResumes(false)
    successMsg.value = '已移除'
    setTimeout(() => {
      successMsg.value = ''
    }, 1500)
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '移除失败')
  } finally {
    deletingId.value = null
  }
}

async function handleSubmit() {
  errorMsg.value = ''
  successMsg.value = ''

  if (!jobId.value) {
    errorMsg.value = '缺少岗位信息，请从岗位详情进入'
    return
  }
  if (pendingFile.value) {
    errorMsg.value = '请先确认上传或取消已选文件'
    return
  }
  if (!selectedResumeId.value) {
    errorMsg.value = '请先上传或选择一份简历'
    return
  }

  submitting.value = true
  try {
    await submitApplication({
      jobId: jobId.value,
      resumeId: selectedResumeId.value,
      channel: 5,
    })
    successMsg.value = '投递成功'
    setTimeout(() => {
      router.push('/candidate/applications')
    }, 400)
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '投递失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

async function loadOnlineResumes() {
  loadingOnline.value = true
  try {
    onlineResumes.value = (await fetchOnlineResumeList()) ?? []
  } catch {
    onlineResumes.value = []
  } finally {
    loadingOnline.value = false
  }
}

function switchTab(tab: 'attachment' | 'online') {
  activeTab.value = tab
  if (tab === 'online') loadOnlineResumes()
}

onMounted(() => {
  loadResumes(false)
})
</script>

<template>
  <div data-cmp="Apply" class="flex h-full flex-col bg-[#EBF4F0]">
    <div class="flex items-center gap-3 px-4 py-3 border-b border-border bg-card flex-shrink-0">
      <button type="button" class="p-1.5 rounded-lg hover:bg-muted" @click="router.back()">
        <ChevronLeft :size="20" />
      </button>
      <span class="text-sm font-semibold text-foreground flex-1">投递简历</span>
    </div>

    <div class="flex-1 overflow-y-auto scrollbar-thin px-4 py-4 pb-24 space-y-4">
      <div v-if="!jobId" class="text-xs text-red-600 bg-red-50 border border-red-100 rounded-xl px-3 py-2">
        缺少岗位信息，请返回岗位列表选择岗位
      </div>

      <div class="bg-card p-4 shadow-card border border-border">
        <div class="text-xs text-muted-foreground mb-1">投递岗位</div>
        <div class="flex items-center justify-between">
          <div>
            <div class="text-sm font-bold text-foreground">{{ jobTitle }}</div>
            <div class="text-xs text-muted-foreground">
              <span v-if="jobDept">{{ jobDept }}</span>
              <span v-if="jobDept && jobSalary"> · </span>
              <span v-if="jobSalary">{{ jobSalary }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="bg-card p-4 shadow-card border border-border">
        <h3 class="text-sm font-semibold text-foreground mb-2">选择简历</h3>
        <div class="flex border border-border rounded-lg overflow-hidden mb-3">
          <button
            type="button"
            class="flex-1 flex items-center justify-center gap-1 py-2 text-xs"
            :class="activeTab === 'attachment' ? 'bg-brand-tint text-brand-blue font-medium' : 'text-muted-foreground'"
            @click="switchTab('attachment')"
          >
            <FileText :size="13" />
            附件简历
          </button>
          <button
            type="button"
            class="flex-1 flex items-center justify-center gap-1 py-2 text-xs"
            :class="activeTab === 'online' ? 'bg-brand-tint text-brand-blue font-medium' : 'text-muted-foreground'"
            @click="switchTab('online')"
          >
            <PenLine :size="13" />
            在线简历
          </button>
        </div>

        <template v-if="activeTab === 'attachment'">
        <p v-if="loadingResumes" class="text-xs text-muted-foreground py-2">加载中...</p>
        <p v-else-if="resumes.length === 0 && !pendingFile" class="text-xs text-muted-foreground py-2">
          暂无附件，请上传 pdf/doc/docx 用于投递
        </p>

        <div
          v-if="pendingFile"
          class="flex items-center gap-2 p-3 rounded-xl mb-2 border border-brand-blue/40 bg-brand-tint"
        >
          <FileText :size="16" class="text-brand-blue flex-shrink-0" />
          <div class="flex-1 min-w-0">
            <div class="text-xs font-medium text-foreground truncate">{{ pendingFile.name }}</div>
            <div class="text-[11px] text-muted-foreground">待上传，可取消后重新选择</div>
          </div>
          <button
            type="button"
            class="p-1.5 rounded-lg hover:bg-white/60 flex-shrink-0"
            title="取消选择"
            :disabled="uploading"
            @click="clearPendingFile"
          >
            <X :size="16" class="text-muted-foreground" />
          </button>
        </div>

        <div
          v-for="r in resumes"
          :key="r.id"
          class="flex items-center gap-3 p-3 rounded-xl mb-2 border transition-colors cursor-pointer"
          :class="selectedResumeId === r.id ? 'border-brand-blue bg-brand-tint' : 'border-border hover:border-brand-blue/30'"
          @click="selectResume(r.id)"
        >
          <FileText :size="16" :class="selectedResumeId === r.id ? 'text-brand-blue' : 'text-muted-foreground'" />
          <div class="flex-1 min-w-0">
            <div :class="['text-xs font-medium truncate', selectedResumeId === r.id ? 'text-brand-blue' : 'text-foreground']">
              {{ r.name }}
            </div>
            <div class="text-xs text-muted-foreground">{{ r.date }}</div>
          </div>
          <CheckCircle v-if="selectedResumeId === r.id" :size="16" class="text-brand-blue flex-shrink-0" />
          <button
            type="button"
            class="p-1.5 rounded-lg hover:bg-muted flex-shrink-0"
            title="移除简历"
            :disabled="deletingId === r.id || uploading"
            @click.stop="handleRemoveResume(r.id)"
          >
            <X :size="16" class="text-muted-foreground hover:text-red-500" />
          </button>
        </div>

        <input
          ref="fileInputRef"
          type="file"
          accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
          class="hidden"
          @change="onFileSelected"
        />

        <div v-if="pendingFile" class="flex gap-2 mt-2">
          <button
            type="button"
            class="flex-1 py-2.5 rounded-xl border border-border text-xs text-muted-foreground hover:bg-muted"
            :disabled="uploading"
            @click="clearPendingFile"
          >
            取消
          </button>
          <button
            type="button"
            class="flex-1 py-2.5 rounded-xl gradient-blue text-white text-xs font-medium disabled:opacity-50"
            :disabled="uploading"
            @click="confirmUpload"
          >
            {{ uploading ? '上传中...' : '确认上传' }}
          </button>
        </div>

        <button
          v-else
          type="button"
          class="w-full flex items-center justify-center gap-2 py-2.5 border border-dashed border-border rounded-xl text-xs text-muted-foreground hover:border-brand-blue/40 mt-1 disabled:opacity-50"
          :disabled="uploading"
          @click="triggerUpload"
        >
          <Upload :size="14" />
          <span>上传新简历</span>
        </button>
        </template>

        <template v-else>
          <p class="text-xs text-amber-700 bg-amber-50 border border-amber-100 rounded-lg px-3 py-2 mb-3">
            在线简历仅供编辑完善，岗位投递请切换到「附件简历」并选择已上传的文件。
          </p>
          <p v-if="loadingOnline" class="text-xs text-muted-foreground py-2">加载中...</p>
          <p v-else-if="!onlineResumes.length" class="text-xs text-muted-foreground py-2">
            暂无在线简历，请前往「我的简历 → 在线简历」创建
          </p>
          <div
            v-for="o in onlineResumes"
            :key="o.id"
            class="flex items-center gap-3 p-3 rounded-xl mb-2 border border-border opacity-75"
          >
            <PenLine :size="16" class="text-muted-foreground" />
            <div class="flex-1 min-w-0">
              <div class="text-xs font-medium text-foreground truncate">{{ o.resumeName }}</div>
              <div class="text-[11px] text-muted-foreground">
                完整度 {{ o.completeness ?? 0 }}% · 不可用于投递
              </div>
            </div>
          </div>
        </template>
      </div>

      <div class="bg-accent rounded-2xl p-4 border border-brand-border/50">
        <div class="flex items-center gap-2 mb-3">
          <Sparkles :size="14" class="text-brand-purple" />
          <span class="text-xs font-semibold text-brand-purple">AI简历优化</span>
        </div>
        <p class="text-xs text-muted-foreground leading-relaxed mb-3">AI分析发现，针对此岗位可优化以下内容，提升匹配度</p>
        <button type="button" class="mt-3 w-full py-2 rounded-xl gradient-purple text-white text-xs font-medium">
          AI一键优化简历
        </button>
      </div>

      <p v-if="errorMsg" class="text-xs text-red-600" role="alert">{{ errorMsg }}</p>
      <p v-if="successMsg" class="text-xs text-brand-green">{{ successMsg }}</p>
    </div>

    <div class="flex-shrink-0 px-4 py-4 bg-card border-t border-border">
      <button
        type="button"
        class="w-full py-3.5 rounded-control gradient-blue text-white text-sm font-bold shadow-custom disabled:opacity-60"
        :disabled="submitting || uploading || !!pendingFile || !jobId || !selectedResumeId || activeTab !== 'attachment'"
        @click="handleSubmit"
      >
        {{ submitting ? '投递中...' : '确认投递' }}
      </button>
    </div>
  </div>
</template>
