<script setup lang="ts">
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/dialog/style/css'
import 'element-plus/es/components/form/style/css'
import 'element-plus/es/components/form-item/style/css'
import 'element-plus/es/components/input/style/css'
import 'element-plus/es/components/select/style/css'
import 'element-plus/es/components/option/style/css'
import { computed, onMounted, ref } from 'vue'
import {
  AlertCircle,
  Eye,
  Library,
  Loader2,
  Plus,
  RefreshCw,
  Search,
  Sparkles,
} from 'lucide-vue-next'
import {
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
} from 'element-plus'
import {
  getKnowledgeDoc,
  importKnowledgeDoc,
  knowledgeCategoryLabel,
  KNOWLEDGE_CATEGORY_OPTIONS,
  listKnowledgeDocs,
  reindexKnowledgeDoc,
  updateKnowledgeDoc,
  type KnowledgeDoc,
} from '@/api/adminKnowledge'
import { getErrorMessage } from '@/utils/validators'

const loading = ref(false)
const docs = ref<KnowledgeDoc[]>([])
const keyword = ref('')
const categoryFilter = ref('')

const importOpen = ref(false)
const importSaving = ref(false)
const detailOpen = ref(false)
const detailLoading = ref(false)
const detailSaving = ref(false)
const reindexingId = ref<number | null>(null)
const importForm = ref({
  title: '',
  category: 'faq',
  content: '',
  sourcePath: '',
})
const detailForm = ref({
  id: 0,
  title: '',
  category: 'faq',
  content: '',
  sourcePath: '',
  chunkCount: 0,
  updatedAt: '',
})

const filteredDocs = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const cat = categoryFilter.value.trim().toLowerCase()
  return docs.value.filter((doc) => {
    if (cat && doc.category?.toLowerCase() !== cat) return false
    if (!kw) return true
    return (
      doc.title?.toLowerCase().includes(kw)
      || doc.category?.toLowerCase().includes(kw)
      || doc.sourcePath?.toLowerCase().includes(kw)
    )
  })
})

function formatTime(value?: string | null) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

async function loadDocs() {
  loading.value = true
  try {
    const data = await listKnowledgeDocs()
    docs.value = Array.isArray(data) ? data : []
  } catch (e) {
    docs.value = []
    ElMessage.error(getErrorMessage(e, '知识库列表加载失败'))
  } finally {
    loading.value = false
  }
}

function openImportDialog() {
  importForm.value = {
    title: '',
    category: 'faq',
    content: '',
    sourcePath: '',
  }
  importOpen.value = true
}

async function openDetail(doc: KnowledgeDoc) {
  detailOpen.value = true
  detailLoading.value = true
  detailForm.value = {
    id: doc.id,
    title: doc.title,
    category: doc.category,
    content: '',
    sourcePath: doc.sourcePath ?? '',
    chunkCount: doc.chunkCount ?? 0,
    updatedAt: doc.updatedAt ?? '',
  }
  try {
    const full = await getKnowledgeDoc(doc.id)
    detailForm.value = {
      id: full.id,
      title: full.title,
      category: full.category,
      content: full.content ?? '',
      sourcePath: full.sourcePath ?? '',
      chunkCount: full.chunkCount ?? 0,
      updatedAt: full.updatedAt ?? '',
    }
  } catch (e) {
    detailOpen.value = false
    ElMessage.error(getErrorMessage(e, '文档详情加载失败'))
  } finally {
    detailLoading.value = false
  }
}

async function handleSaveDetail() {
  const title = detailForm.value.title.trim()
  const content = detailForm.value.content.trim()
  if (!title || !content) {
    ElMessage.warning('请填写标题与正文内容')
    return
  }
  detailSaving.value = true
  try {
    await updateKnowledgeDoc(detailForm.value.id, {
      title,
      category: detailForm.value.category,
      content,
      sourcePath: detailForm.value.sourcePath.trim() || undefined,
      reindex: true,
    })
    ElMessage.success('已保存并重新向量化')
    detailOpen.value = false
    await loadDocs()
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '保存失败'))
  } finally {
    detailSaving.value = false
  }
}

async function handleImport() {
  const title = importForm.value.title.trim()
  const content = importForm.value.content.trim()
  if (!title || !content) {
    ElMessage.warning('请填写标题与正文内容')
    return
  }
  importSaving.value = true
  try {
    await importKnowledgeDoc({
      title,
      category: importForm.value.category,
      content,
      sourcePath: importForm.value.sourcePath.trim() || undefined,
    })
    ElMessage.success('文档已导入并完成向量化')
    importOpen.value = false
    await loadDocs()
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '导入失败'))
  } finally {
    importSaving.value = false
  }
}

async function handleReindex(doc: KnowledgeDoc) {
  try {
    await ElMessageBox.confirm(
      `将对「${doc.title}」重新切分并向量化，耗时可能较长，是否继续？`,
      '重新索引',
      { type: 'warning', confirmButtonText: '开始 reindex', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  reindexingId.value = doc.id
  try {
    await reindexKnowledgeDoc(doc.id)
    ElMessage.success('重新索引完成')
    await loadDocs()
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '重新索引失败'))
  } finally {
    reindexingId.value = null
  }
}

onMounted(() => loadDocs())
</script>

<template>
  <div data-cmp="KnowledgeBase" class="p-6 sm:p-8">
    <div class="max-w-6xl mx-auto">
      <div class="flex flex-wrap items-start justify-between gap-4 mb-6">
        <div>
          <div class="flex items-center gap-2 mb-1">
            <Library :size="22" class="text-brand-purple" />
            <h1 class="text-2xl font-black text-foreground">AI 知识库</h1>
          </div>
          <p class="text-sm text-muted-foreground max-w-2xl">
            供 HR「AI 招聘助手」RAG 检索使用。点击标题可查看/编辑正文；保存后会自动重新切分并向量化。
          </p>
        </div>
        <button
          type="button"
          class="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl gradient-purple text-white text-sm font-medium shadow-custom"
          @click="openImportDialog"
        >
          <Plus :size="16" />
          导入文档
        </button>
      </div>

      <div
        class="mb-5 flex items-start gap-3 rounded-2xl border border-brand-purple/20 bg-brand-tint-2/50 px-4 py-3 text-sm text-muted-foreground"
      >
        <Sparkles :size="16" class="text-brand-purple flex-shrink-0 mt-0.5" />
        <div>
          首次部署时服务会自动加载 <code class="text-xs bg-muted px-1 rounded">knowledge/*.md</code> 种子文档。
          此处用于在线追加制度、FAQ、流程说明；需配置 DashScope 才能完成向量化。
        </div>
      </div>

      <div class="bg-card border border-border rounded-2xl shadow-card overflow-hidden">
        <div class="flex flex-wrap items-center gap-3 p-4 border-b border-border">
          <div class="relative flex-1 min-w-[200px]">
            <Search :size="15" class="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
            <input
              v-model="keyword"
              type="text"
              placeholder="搜索标题、分类、来源..."
              class="w-full pl-9 pr-3 py-2 rounded-xl bg-muted text-sm outline-none border border-transparent focus:border-brand-blue/40"
            />
          </div>
          <select
            v-model="categoryFilter"
            class="px-3 py-2 rounded-xl bg-muted text-sm outline-none border border-transparent focus:border-brand-blue/40"
          >
            <option value="">全部分类</option>
            <option v-for="opt in KNOWLEDGE_CATEGORY_OPTIONS" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
          <button
            type="button"
            class="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl border border-border text-sm hover:bg-muted disabled:opacity-50"
            :disabled="loading"
            @click="loadDocs"
          >
            <Loader2 v-if="loading" :size="14" class="animate-spin" />
            <RefreshCw v-else :size="14" />
            刷新
          </button>
        </div>

        <div v-if="loading" class="p-10 text-center text-sm text-muted-foreground">加载中...</div>
        <div v-else-if="filteredDocs.length === 0" class="p-10 text-center">
          <AlertCircle :size="28" class="mx-auto text-muted-foreground mb-2" />
          <p class="text-sm text-muted-foreground">暂无知识文档，可导入或等待启动时种子加载</p>
        </div>
        <div v-else class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="text-left text-xs text-muted-foreground border-b border-border bg-muted/40">
                <th class="px-4 py-3 font-medium">标题</th>
                <th class="px-4 py-3 font-medium">分类</th>
                <th class="px-4 py-3 font-medium">片段数</th>
                <th class="px-4 py-3 font-medium">来源</th>
                <th class="px-4 py-3 font-medium">更新时间</th>
                <th class="px-4 py-3 font-medium text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="doc in filteredDocs"
                :key="doc.id"
                class="border-b border-border last:border-0 hover:bg-muted/30"
              >
                <td class="px-4 py-3 font-medium text-foreground">
                  <button
                    type="button"
                    class="text-left hover:text-brand-purple hover:underline underline-offset-2"
                    @click="openDetail(doc)"
                  >
                    {{ doc.title }}
                  </button>
                </td>
                <td class="px-4 py-3">
                  <span class="text-xs px-2 py-0.5 rounded-full bg-brand-tint text-brand-purple border border-brand-purple/20">
                    {{ knowledgeCategoryLabel(doc.category) }}
                  </span>
                </td>
                <td class="px-4 py-3 text-muted-foreground">{{ doc.chunkCount ?? '—' }}</td>
                <td class="px-4 py-3 text-muted-foreground max-w-[180px] truncate" :title="doc.sourcePath ?? ''">
                  {{ doc.sourcePath || '—' }}
                </td>
                <td class="px-4 py-3 text-muted-foreground">{{ formatTime(doc.updatedAt) }}</td>
                <td class="px-4 py-3 text-right">
                  <div class="inline-flex items-center gap-1.5">
                    <button
                      type="button"
                      class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-medium border border-border hover:bg-muted"
                      @click="openDetail(doc)"
                    >
                      <Eye :size="12" />
                      查看/编辑
                    </button>
                    <button
                      type="button"
                      class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-medium border border-border hover:bg-muted disabled:opacity-50"
                      :disabled="reindexingId === doc.id"
                      @click="handleReindex(doc)"
                    >
                      <Loader2 v-if="reindexingId === doc.id" :size="12" class="animate-spin" />
                      <RefreshCw v-else :size="12" />
                      {{ reindexingId === doc.id ? '索引中...' : 'Reindex' }}
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <ElDialog v-model="importOpen" title="导入知识文档" width="640px" destroy-on-close>
      <ElForm label-position="top">
        <ElFormItem label="标题" required>
          <ElInput v-model="importForm.title" placeholder="例如：Offer 审批流程说明" maxlength="128" show-word-limit />
        </ElFormItem>
        <ElFormItem label="分类" required>
          <ElSelect v-model="importForm.category" class="w-full">
            <ElOption
              v-for="opt in KNOWLEDGE_CATEGORY_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="来源路径（可选）">
          <ElInput v-model="importForm.sourcePath" placeholder="例如：admin/import/offer-policy.md" />
        </ElFormItem>
        <ElFormItem label="正文（Markdown 或纯文本）" required>
          <ElInput
            v-model="importForm.content"
            type="textarea"
            :rows="12"
            placeholder="粘贴制度、FAQ、流程说明全文..."
          />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <button
          type="button"
          class="px-4 py-2 rounded-xl border border-border text-sm mr-2"
          :disabled="importSaving"
          @click="importOpen = false"
        >
          取消
        </button>
        <button
          type="button"
          class="px-4 py-2 rounded-xl gradient-purple text-white text-sm font-medium disabled:opacity-50"
          :disabled="importSaving"
          @click="handleImport"
        >
          {{ importSaving ? '导入并向量化中...' : '确认导入' }}
        </button>
      </template>
    </ElDialog>

    <ElDialog
      v-model="detailOpen"
      :title="detailLoading ? '加载文档...' : `编辑：${detailForm.title || '知识文档'}`"
      width="720px"
      destroy-on-close
    >
      <div v-if="detailLoading" class="py-12 text-center text-sm text-muted-foreground">加载正文中...</div>
      <ElForm v-else label-position="top">
        <div class="mb-3 flex flex-wrap gap-3 text-xs text-muted-foreground">
          <span>片段数：{{ detailForm.chunkCount ?? '—' }}</span>
          <span>更新时间：{{ formatTime(detailForm.updatedAt) }}</span>
        </div>
        <ElFormItem label="标题" required>
          <ElInput v-model="detailForm.title" maxlength="128" show-word-limit />
        </ElFormItem>
        <ElFormItem label="分类" required>
          <ElSelect v-model="detailForm.category" class="w-full">
            <ElOption
              v-for="opt in KNOWLEDGE_CATEGORY_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="来源路径（可选）">
          <ElInput v-model="detailForm.sourcePath" placeholder="例如：classpath:knowledge/02-faq-screen-status.md" />
        </ElFormItem>
        <ElFormItem label="正文（Markdown 或纯文本）" required>
          <ElInput
            v-model="detailForm.content"
            type="textarea"
            :rows="16"
            placeholder="知识库正文..."
          />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <button
          type="button"
          class="px-4 py-2 rounded-xl border border-border text-sm mr-2"
          :disabled="detailSaving"
          @click="detailOpen = false"
        >
          取消
        </button>
        <button
          type="button"
          class="px-4 py-2 rounded-xl gradient-purple text-white text-sm font-medium disabled:opacity-50"
          :disabled="detailLoading || detailSaving"
          @click="handleSaveDetail"
        >
          {{ detailSaving ? '保存并向量化中...' : '保存并重新向量化' }}
        </button>
      </template>
    </ElDialog>
  </div>
</template>
