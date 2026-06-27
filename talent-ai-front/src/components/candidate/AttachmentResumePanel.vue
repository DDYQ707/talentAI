<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { Download, Upload, FileText, Trash2 } from 'lucide-vue-next'
import {
  deleteResume,
  fetchAttachmentResumes,
  fetchResumePreview,
  openResumePreview,
  uploadResumeFile,
  type ResumeListItem,
} from '@/api/resume'
import { getErrorMessage } from '@/utils/validators'

const emit = defineEmits<{ changed: [] }>()

const props = withDefaults(
  defineProps<{
    embedded?: boolean
    active?: boolean
  }>(),
  { embedded: false, active: true },
)

const attachments = ref<ResumeListItem[]>([])
const loading = ref(false)
const uploading = ref(false)
const message = ref('')
const errorMsg = ref('')
const fileInputRef = ref<HTMLInputElement | null>(null)

function formatSize(bytes?: number) {
  if (!bytes) return ''
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

async function loadAttachments() {
  loading.value = true
  errorMsg.value = ''
  try {
    attachments.value = (await fetchAttachmentResumes()) ?? []
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '加载简历失败')
  } finally {
    loading.value = false
  }
}

function triggerUpload() {
  fileInputRef.value?.click()
}

async function onFileSelected(ev: Event) {
  const input = ev.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  uploading.value = true
  errorMsg.value = ''
  message.value = ''
  try {
    const existingId = attachments.value[0]?.id
    await uploadResumeFile(file, existingId != null ? { resumeId: existingId } : undefined)
    message.value = '上传成功'
    await loadAttachments()
    emit('changed')
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '上传失败')
  } finally {
    uploading.value = false
  }
}

async function handlePreview(item: ResumeListItem) {
  if (!item.attachmentId) return
  try {
    const preview = await fetchResumePreview(item.attachmentId)
    openResumePreview(preview)
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '无法打开预览')
  }
}

async function handleDelete(id: number) {
  if (!confirm('确定删除该附件简历？')) return
  try {
    await deleteResume(id)
    message.value = '已删除'
    await loadAttachments()
    emit('changed')
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '删除失败')
  }
}

watch(
  () => props.active,
  (v) => {
    if (v) loadAttachments()
  },
)

onMounted(() => {
  if (props.embedded || props.active !== false) loadAttachments()
})
</script>

<template>
  <div :class="embedded ? '' : 'space-y-4'">
    <div class="bg-card shadow-card p-4 border border-border">
      <div class="flex items-center justify-between mb-3">
        <span v-if="!embedded" class="text-sm text-muted-foreground">上传 pdf / doc / docx，最大 10MB，用于岗位投递</span>
        <span v-else class="text-xs text-muted-foreground">上传文件后可用于岗位投递</span>
        <button
          type="button"
          class="flex items-center gap-1 text-xs text-brand-blue disabled:opacity-50"
          :disabled="uploading"
          @click="triggerUpload"
        >
          <Upload :size="14" />
          {{ uploading ? '上传中' : '上传' }}
        </button>
      </div>
      <input ref="fileInputRef" type="file" accept=".pdf,.doc,.docx" class="hidden" @change="onFileSelected" />
      <p v-if="loading" class="text-xs text-muted-foreground">加载中...</p>
      <p v-else-if="attachments.length === 0" class="text-xs text-muted-foreground">暂无附件，请先上传</p>
      <div
        v-for="item in attachments"
        :key="item.id"
        class="flex items-center gap-3 py-2 border-b border-border last:border-0"
      >
        <FileText :size="16" class="text-brand-blue flex-shrink-0" />
        <div class="flex-1 min-w-0">
          <div class="text-xs font-medium text-foreground truncate">{{ item.resumeName }}</div>
          <div class="text-[11px] text-muted-foreground">
            <span v-if="item.fileType">{{ item.fileType.toUpperCase() }}</span>
            <span v-if="item.fileSize"> · {{ formatSize(item.fileSize) }}</span>
          </div>
        </div>
        <button
          v-if="item.attachmentId"
          type="button"
          class="p-1.5 rounded-lg hover:bg-muted"
          title="预览"
          @click="handlePreview(item)"
        >
          <Download :size="14" class="text-muted-foreground" />
        </button>
        <button type="button" class="p-1.5 rounded-lg hover:bg-muted" title="删除" @click="handleDelete(item.id)">
          <Trash2 :size="14" class="text-red-500" />
        </button>
      </div>
      <p v-if="message" class="text-xs text-brand-green mt-2">{{ message }}</p>
      <p v-if="errorMsg" class="text-xs text-red-600 mt-2">{{ errorMsg }}</p>
    </div>
  </div>
</template>
