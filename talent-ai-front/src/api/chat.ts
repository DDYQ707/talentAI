import request from '@/utils/request'

export interface ChatSession {
  id: number
  sessionTitle: string
  createdAt?: string
  updatedAt?: string
}

export interface ChatMessage {
  id: number
  role: 1 | 2
  content: string
  createdAt?: string
}

export interface ChatCandidateCard {
  resumeId: number
  candidateId?: number
  candidateName?: string
  resumeName?: string
  currentTitle?: string
  city?: string
  appliedJobTitle?: string
  matchScore?: number | null
  screenStatus?: number
  attachmentId?: number
  applicationId?: number
}

export interface ChatSendResult {
  sessionId: number
  sessionTitle?: string
  reply: string
  candidates?: ChatCandidateCard[]
}

export function sendChatMessage(data: { sessionId?: number; message: string }) {
  return request.post<ChatSendResult>('/api/ai/chat', data, {
    timeout: 90000,
  }) as Promise<ChatSendResult>
}

export interface ChatStreamMeta {
  sessionId: number
  sessionTitle?: string
}

export interface ChatStreamHandlers {
  onMeta?: (meta: ChatStreamMeta) => void
  onToken?: (text: string) => void
  onDone?: (result: ChatSendResult) => void
  onError?: (message: string) => void
}

function parseSseChunk(chunk: string, handlers: ChatStreamHandlers) {
  let event = 'message'
  const dataLines: string[] = []
  for (const line of chunk.split('\n')) {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim())
    }
  }
  const data = dataLines.join('\n')
  if (!data) return

  const payload = JSON.parse(data) as Record<string, unknown>
  if (event === 'meta') {
    handlers.onMeta?.(payload as ChatStreamMeta)
  } else if (event === 'token') {
    handlers.onToken?.(String(payload.text ?? ''))
  } else if (event === 'done') {
    handlers.onDone?.(payload as ChatSendResult)
  } else if (event === 'error') {
    handlers.onError?.(String(payload.message ?? data))
  }
}

async function consumeSseStream(body: ReadableStream<Uint8Array>, handlers: ChatStreamHandlers) {
  const decoder = new TextDecoder()
  let buffer = ''
  const reader = body.getReader()

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    let boundary = buffer.indexOf('\n\n')
    while (boundary !== -1) {
      const chunk = buffer.slice(0, boundary)
      buffer = buffer.slice(boundary + 2)
      if (chunk.trim()) {
        parseSseChunk(chunk, handlers)
      }
      boundary = buffer.indexOf('\n\n')
    }
  }

  if (buffer.trim()) {
    parseSseChunk(buffer, handlers)
  }
}

export async function sendChatMessageStream(
  data: { sessionId?: number; message: string },
  handlers: ChatStreamHandlers,
  signal?: AbortSignal,
) {
  const token = localStorage.getItem('talent_token')
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream',
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const baseURL = import.meta.env.VITE_API_BASE_URL || ''
  const response = await fetch(`${baseURL}/api/ai/chat/stream`, {
    method: 'POST',
    headers,
    body: JSON.stringify(data),
    signal,
  })

  if (!response.ok) {
    let message = '发送失败，请稍后重试'
    try {
      const json = (await response.json()) as { msg?: string; message?: string }
      message = json.msg || json.message || message
    } catch {
      const text = await response.text()
      if (text) message = text
    }
    throw new Error(message)
  }

  if (!response.body) {
    throw new Error('流式响应为空')
  }

  await consumeSseStream(response.body, handlers)
}

export function fetchChatSessions() {
  return request.get<ChatSession[]>('/api/ai/chat/sessions') as Promise<ChatSession[]>
}

export function fetchChatMessages(sessionId: number) {
  return request.get<ChatMessage[]>(`/api/ai/chat/sessions/${sessionId}/messages`) as Promise<ChatMessage[]>
}

export const SCREEN_STATUS_LABEL: Record<number, string> = {
  1: '待初筛',
  2: '面试中',
  3: '已录用',
  4: '已淘汰',
}
