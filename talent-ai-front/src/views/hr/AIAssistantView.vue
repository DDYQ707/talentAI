<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Bot,
  Send,
  User,
  Sparkles,
  Briefcase,
  Zap,
  Plus,
  Search,
  ThumbsUp,
  ThumbsDown,
  Copy,
  RefreshCw,
} from 'lucide-vue-next'
import {
  fetchChatMessages,
  fetchChatSessions,
  sendChatMessageStream,
  SCREEN_STATUS_LABEL,
  type ChatCandidateCard,
  type ChatMessage,
  type ChatSession,
} from '@/api/chat'
import { fetchHrResumePreview } from '@/api/hrResume'
import { openResumePreview } from '@/api/resume'
import { parseInlineIdSegments, type ChatMessageSegment } from '@/utils/chatMessage'
import { getErrorMessage } from '@/utils/validators'

const router = useRouter()

type UiMessage = {
  id?: number
  role: 'user' | 'ai'
  content: string
}

const quickCommands = [
  { icon: Search, label: '待初筛简历', cmd: '搜一下待初筛的简历' },
  { icon: Search, label: '智能搜索候选人', cmd: '帮我找姓名包含「张」的候选人' },
  { icon: Briefcase, label: '生成岗位JD', cmd: '帮我写一份高级产品经理的职位描述' },
  { icon: Sparkles, label: '查匹配分', cmd: '查投递单 6 的匹配分和优劣势' },
  { icon: Zap, label: '招聘策略建议', cmd: '我们的面试通过率只有20%，如何提升？' },
  { icon: Sparkles, label: '招聘流程FAQ', cmd: '标准招聘流程是什么？各筛选状态代表什么？' },
]

const candidateResults = ref<ChatCandidateCard[]>([])
const previewingAttachmentId = ref<number | null>(null)

const sessions = ref<ChatSession[]>([])
const currentSessionId = ref<number | null>(null)
const messages = ref<UiMessage[]>([])
const input = ref('')
const sending = ref(false)
const streaming = ref(false)
const loadingSessions = ref(false)
const loadingMessages = ref(false)
const errorMsg = ref('')
const messageBoxRef = ref<HTMLElement | null>(null)

const welcomeText =
  '你好，我是 AI 招聘助手。我可以帮你搜索简历、查询匹配分与投递进度、查看面试安排，也可以协助撰写 JD 和设计面试问题。'

function formatRelativeTime(value?: string) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const diffMs = Date.now() - date.getTime()
  const minutes = Math.floor(diffMs / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString()
}

function toUiMessage(message: ChatMessage): UiMessage {
  return {
    id: message.id,
    role: message.role === 1 ? 'user' : 'ai',
    content: message.content,
  }
}

async function scrollToBottom() {
  await nextTick()
  if (messageBoxRef.value) {
    messageBoxRef.value.scrollTop = messageBoxRef.value.scrollHeight
  }
}

async function loadSessions() {
  loadingSessions.value = true
  try {
    sessions.value = await fetchChatSessions()
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '会话列表加载失败')
  } finally {
    loadingSessions.value = false
  }
}

async function loadMessages(sessionId: number) {
  loadingMessages.value = true
  errorMsg.value = ''
  try {
    const data = await fetchChatMessages(sessionId)
    messages.value = data.map(toUiMessage)
    await scrollToBottom()
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '消息加载失败')
    messages.value = []
  } finally {
    loadingMessages.value = false
  }
}

async function selectSession(sessionId: number) {
  if (currentSessionId.value === sessionId && messages.value.length > 0) return
  currentSessionId.value = sessionId
  await loadMessages(sessionId)
}

function parseMessageSegments(content: string): ChatMessageSegment[] {
  return parseInlineIdSegments(content)
}

function applyCandidates(list?: ChatCandidateCard[]) {
  if (list && list.length > 0) {
    candidateResults.value = list
  }
}

function candidateDisplayName(item: ChatCandidateCard) {
  return item.candidateName || item.resumeName || `简历#${item.resumeId}`
}

function candidateSubtitle(item: ChatCandidateCard) {
  const parts: string[] = []
  if (item.appliedJobTitle) parts.push(item.appliedJobTitle)
  else if (item.currentTitle) parts.push(item.currentTitle)
  if (item.city) parts.push(item.city)
  return parts.join(' · ') || '暂无岗位信息'
}

function screenStatusLabel(status?: number) {
  if (status == null) return ''
  return SCREEN_STATUS_LABEL[status] ?? `状态${status}`
}

function openCandidateDetail(item: ChatCandidateCard) {
  router.push({ path: '/hr/resumes/detail', query: { id: String(item.resumeId) } })
}

async function previewAttachment(attachmentId: number) {
  previewingAttachmentId.value = attachmentId
  try {
    const preview = await fetchHrResumePreview(attachmentId)
    openResumePreview(preview)
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '简历预览失败')
  } finally {
    previewingAttachmentId.value = null
  }
}

function goResumeManagement() {
  router.push('/hr/resumes')
}

function startNewChat() {
  currentSessionId.value = null
  messages.value = []
  input.value = ''
  errorMsg.value = ''
  candidateResults.value = []
}

function applyQuick(cmd: string) {
  input.value = cmd
}

async function handleSend() {
  const text = input.value.trim()
  if (!text || sending.value) return

  sending.value = true
  streaming.value = true
  errorMsg.value = ''
  messages.value.push({ role: 'user', content: text })
  const aiIndex = messages.value.length
  messages.value.push({ role: 'ai', content: '' })
  input.value = ''
  await scrollToBottom()

  try {
    await sendChatMessageStream(
      {
        sessionId: currentSessionId.value ?? undefined,
        message: text,
      },
      {
        onMeta: (meta) => {
          currentSessionId.value = meta.sessionId
        },
        onToken: (token) => {
          const current = messages.value[aiIndex]
          if (current) {
            current.content += token
          }
          void scrollToBottom()
        },
        onDone: async (res) => {
          currentSessionId.value = res.sessionId
          const current = messages.value[aiIndex]
          if (current && res.reply) {
            current.content = res.reply
          }
          applyCandidates(res.candidates)
          await loadSessions()
          await scrollToBottom()
        },
        onError: (message) => {
          const current = messages.value[aiIndex]
          if (current && !current.content) {
            current.content = message
          } else {
            errorMsg.value = message
          }
        },
      },
    )
  } catch (e) {
    const current = messages.value[aiIndex]
    const errText = getErrorMessage(e, '发送失败，请稍后重试')
    if (current && !current.content) {
      current.content = errText
    } else {
      errorMsg.value = errText
    }
    await scrollToBottom()
  } finally {
    sending.value = false
    streaming.value = false
  }
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSend()
  }
}

function copyMessage(content: string) {
  navigator.clipboard?.writeText(content).catch(() => {})
}

const showWelcome = computed(
  () => !loadingMessages.value && messages.value.length === 0 && !sending.value && !streaming.value,
)

onMounted(async () => {
  await loadSessions()
})
</script>

<template>
  <div data-cmp="AIAssistant" class="flex h-full" style="height: calc(100vh - 64px)">
    <div class="w-56 flex-shrink-0 border-r border-border bg-card flex flex-col">
      <div class="p-4 border-b border-border">
        <button
          type="button"
          class="w-full flex items-center justify-center gap-2 py-2.5 rounded-control gradient-blue text-white text-sm font-medium"
          @click="startNewChat"
        >
          <Plus :size="14" />
          <span>新建对话</span>
        </button>
      </div>
      <div class="flex-1 overflow-y-auto scrollbar-thin p-2">
        <div class="text-xs text-muted-foreground px-2 py-2 uppercase tracking-wider">历史记录</div>
        <p v-if="loadingSessions" class="px-3 py-2 text-xs text-muted-foreground">加载中...</p>
        <p v-else-if="sessions.length === 0" class="px-3 py-2 text-xs text-muted-foreground">暂无历史会话</p>
        <div
          v-for="item in sessions"
          :key="item.id"
          class="px-3 py-2.5 rounded-xl cursor-pointer mb-1"
          :class="
            currentSessionId === item.id
              ? 'bg-accent border border-brand-purple/20'
              : 'hover:bg-muted'
          "
          @click="selectSession(item.id)"
        >
          <div
            :class="[
              'text-xs font-medium truncate',
              currentSessionId === item.id ? 'text-brand-purple' : 'text-foreground',
            ]"
          >
            {{ item.sessionTitle || '新对话' }}
          </div>
          <div class="text-xs text-muted-foreground mt-0.5">
            {{ formatRelativeTime(item.updatedAt || item.createdAt) }}
          </div>
        </div>
      </div>
    </div>

    <div class="flex-1 flex flex-col min-w-0">
      <div class="px-4 py-3 border-b border-border bg-muted/50 flex items-center gap-2 overflow-x-auto">
        <span class="text-xs text-muted-foreground flex-shrink-0">快捷指令：</span>
        <button
          v-for="cmd in quickCommands"
          :key="cmd.label"
          type="button"
          class="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-card border border-border text-xs text-foreground hover:border-brand-purple/40 hover:bg-accent transition-colors flex-shrink-0"
          @click="applyQuick(cmd.cmd)"
        >
          <component :is="cmd.icon" :size="12" class="text-brand-purple" />
          <span>{{ cmd.label }}</span>
        </button>
      </div>

      <div ref="messageBoxRef" class="flex-1 overflow-y-auto scrollbar-thin p-6 space-y-6">
        <div
          v-if="showWelcome"
          class="flex gap-3 justify-start"
        >
          <div class="w-8 h-8 rounded-xl gradient-purple flex items-center justify-center flex-shrink-0 mt-1">
            <Bot :size="14" class="text-white" />
          </div>
          <div class="max-w-lg bg-card rounded-2xl rounded-tl-sm px-4 py-3 text-sm text-foreground shadow-card">
            <div class="whitespace-pre-line leading-relaxed">{{ welcomeText }}</div>
          </div>
        </div>

        <p v-if="loadingMessages" class="text-sm text-muted-foreground text-center">加载消息中...</p>
        <p v-if="errorMsg" class="text-xs text-red-600 text-center">{{ errorMsg }}</p>

        <div
          v-for="(msg, i) in messages"
          :key="msg.id ?? i"
          class="flex gap-3"
          :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
        >
          <template v-if="msg.role === 'ai'">
            <div class="w-8 h-8 rounded-xl gradient-purple flex items-center justify-center flex-shrink-0 mt-1">
              <Bot :size="14" class="text-white" />
            </div>
            <div class="max-w-lg">
              <div class="bg-card rounded-2xl rounded-tl-sm px-4 py-3 text-sm text-foreground shadow-card">
                <div class="leading-relaxed whitespace-pre-wrap">
                  <span
                    v-if="streaming && i === messages.length - 1 && !msg.content"
                    class="text-muted-foreground"
                  >思考中...</span>
                  <template v-else v-for="(seg, segIndex) in parseMessageSegments(msg.content)" :key="segIndex">
                    <strong v-if="seg.type === 'bold'">{{ seg.text }}</strong>
                    <button
                      v-else-if="seg.type === 'attachment'"
                      type="button"
                      class="text-brand-blue underline underline-offset-2 hover:text-brand-purple"
                      :disabled="previewingAttachmentId === seg.attachmentId"
                      @click="previewAttachment(seg.attachmentId)"
                    >
                      {{ previewingAttachmentId === seg.attachmentId ? '打开中...' : seg.label }}
                    </button>
                    <button
                      v-else-if="seg.type === 'resume'"
                      type="button"
                      class="text-brand-blue underline underline-offset-2 hover:text-brand-purple"
                      @click="router.push({ path: '/hr/resumes/detail', query: { id: String(seg.resumeId) } })"
                    >
                      {{ seg.label }}
                    </button>
                    <span v-else>{{ seg.text }}</span>
                  </template>
                </div>
              </div>
              <div class="flex items-center gap-2 mt-2">
                <button
                  type="button"
                  class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
                >
                  <ThumbsUp :size="12" />
                </button>
                <button
                  type="button"
                  class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
                >
                  <ThumbsDown :size="12" />
                </button>
                <button
                  type="button"
                  class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
                  @click="copyMessage(msg.content)"
                >
                  <Copy :size="12" />
                </button>
                <button
                  type="button"
                  class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
                >
                  <RefreshCw :size="12" />
                </button>
              </div>
            </div>
          </template>
          <template v-else>
            <div class="max-w-lg order-first flex gap-3 justify-end items-start">
              <div class="gradient-blue text-white rounded-2xl rounded-tr-sm px-4 py-3 text-sm whitespace-pre-line">
                {{ msg.content }}
              </div>
              <div class="w-8 h-8 rounded-control gradient-blue flex items-center justify-center flex-shrink-0 mt-1">
                <User :size="14" class="text-white" />
              </div>
            </div>
          </template>
        </div>

        <div v-if="sending && !streaming" class="flex gap-3 justify-start">
          <div class="w-8 h-8 rounded-xl gradient-purple flex items-center justify-center flex-shrink-0 mt-1">
            <Bot :size="14" class="text-white" />
          </div>
          <div class="bg-card rounded-2xl rounded-tl-sm px-4 py-3 text-sm text-muted-foreground shadow-card">
            思考中...
          </div>
        </div>
      </div>

      <div class="p-4 border-t border-border">
        <div class="flex items-end gap-3 bg-card rounded-2xl border border-border p-3 shadow-card">
          <div class="flex-1">
            <textarea
              v-model="input"
              rows="2"
              class="w-full bg-transparent text-sm outline-none resize-none text-foreground placeholder:text-muted-foreground"
              placeholder="向AI助手发送指令，例如：帮我写一份高级产品经理 JD..."
              :disabled="sending"
              @keydown="handleKeydown"
            />
          </div>
          <button
            type="button"
            class="w-9 h-9 rounded-xl gradient-purple flex items-center justify-center flex-shrink-0 disabled:opacity-50"
            :disabled="sending || !input.trim()"
            @click="handleSend"
          >
            <Send :size="16" class="text-white" />
          </button>
        </div>
        <div class="text-xs text-muted-foreground text-center mt-2">
          AI 助手支持多轮对话，并可查询系统中的简历、岗位、匹配与面试数据
        </div>
      </div>
    </div>

    <div class="w-72 flex-shrink-0 border-l border-border bg-card flex flex-col">
      <div class="p-4 border-b border-border">
        <div class="flex items-center gap-2">
          <Sparkles :size="16" class="text-brand-purple" />
          <span class="text-sm font-semibold text-foreground">推荐候选人</span>
          <span v-if="candidateResults.length" class="ml-auto text-xs text-brand-purple">
            {{ candidateResults.length }} 人
          </span>
        </div>
      </div>
      <div class="flex-1 overflow-y-auto scrollbar-thin p-3 space-y-3">
        <p
          v-if="candidateResults.length === 0"
          class="text-xs text-muted-foreground text-center px-2 py-8 leading-relaxed"
        >
          发送「搜待初筛简历」或「查某某匹配分」等指令后，相关候选人将展示在这里
        </p>
        <div
          v-for="c in candidateResults"
          :key="c.resumeId"
          class="bg-muted rounded-xl p-3 hover:bg-accent transition-colors cursor-pointer"
          @click="openCandidateDetail(c)"
        >
          <div class="flex items-center gap-2 mb-2">
            <div class="w-8 h-8 rounded-full gradient-blue-purple flex items-center justify-center flex-shrink-0">
              <User :size="12" class="text-white" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="text-xs font-semibold text-foreground truncate">
                {{ candidateDisplayName(c) }}
              </div>
              <div class="text-xs text-muted-foreground truncate">{{ candidateSubtitle(c) }}</div>
            </div>
            <div v-if="c.matchScore != null" class="text-sm font-bold text-brand-purple">
              {{ c.matchScore }}%
            </div>
          </div>
          <div class="flex flex-wrap gap-1">
            <span
              v-if="c.screenStatus != null"
              class="text-xs px-1.5 py-0.5 rounded bg-card text-muted-foreground"
            >
              {{ screenStatusLabel(c.screenStatus) }}
            </span>
            <span
              v-if="c.attachmentId"
              class="text-xs px-1.5 py-0.5 rounded bg-card text-brand-blue"
              @click.stop="previewAttachment(c.attachmentId)"
            >
              预览附件
            </span>
          </div>
        </div>
      </div>
      <div class="p-3 border-t border-border">
        <button
          type="button"
          class="w-full py-2 rounded-xl border border-brand-blue text-brand-blue text-xs font-medium hover:bg-brand-tint transition-colors"
          @click="goResumeManagement"
        >
          查看全部候选人
        </button>
      </div>
    </div>
  </div>
</template>
