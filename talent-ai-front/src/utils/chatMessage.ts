export type ChatMessageSegment =
  | { type: 'text'; text: string }
  | { type: 'bold'; text: string }
  | { type: 'attachment'; label: string; attachmentId: number }
  | { type: 'resume'; label: string; resumeId: number }

const ATTACHMENT_LINK_RE = /\[([^\]]+)\]\(attachmentId=(\d+)\)/g
const RESUME_LINK_RE = /\[([^\]]+)\]\(resumeId=(\d+)\)/g
const BOLD_RE = /\*\*([^*]+)\*\*/g

function splitByPattern(
  text: string,
  pattern: RegExp,
  mapMatch: (match: RegExpExecArray) => ChatMessageSegment,
): ChatMessageSegment[] {
  const segments: ChatMessageSegment[] = []
  let lastIndex = 0
  const regex = new RegExp(pattern.source, pattern.flags)
  let match: RegExpExecArray | null

  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      segments.push({ type: 'text', text: text.slice(lastIndex, match.index) })
    }
    segments.push(mapMatch(match))
    lastIndex = regex.lastIndex
  }

  if (lastIndex < text.length) {
    segments.push({ type: 'text', text: text.slice(lastIndex) })
  }

  return segments.length > 0 ? segments : [{ type: 'text', text }]
}

function splitBoldSegments(segments: ChatMessageSegment[]): ChatMessageSegment[] {
  const result: ChatMessageSegment[] = []
  for (const segment of segments) {
    if (segment.type !== 'text' || !segment.text.includes('**')) {
      result.push(segment)
      continue
    }
    let lastIndex = 0
    const text = segment.text
    BOLD_RE.lastIndex = 0
    let match: RegExpExecArray | null
    let matched = false
    while ((match = BOLD_RE.exec(text)) !== null) {
      matched = true
      if (match.index > lastIndex) {
        result.push({ type: 'text', text: text.slice(lastIndex, match.index) })
      }
      result.push({ type: 'bold', text: match[1] })
      lastIndex = BOLD_RE.lastIndex
    }
    if (!matched) {
      result.push(segment)
    } else if (lastIndex < text.length) {
      result.push({ type: 'text', text: text.slice(lastIndex) })
    }
  }
  return result
}

/** 解析 AI 回复中的 Markdown 片段，支持附件/简历可点击链接 */
export function parseChatMessageSegments(content: string): ChatMessageSegment[] {
  if (!content) return [{ type: 'text', text: '' }]

  let segments: ChatMessageSegment[] = [{ type: 'text', text: content }]

  const attachmentParts: ChatMessageSegment[] = []
  for (const segment of segments) {
    if (segment.type !== 'text') {
      attachmentParts.push(segment)
      continue
    }
    attachmentParts.push(
      ...splitByPattern(segment.text, ATTACHMENT_LINK_RE, (match) => ({
        type: 'attachment',
        label: match[1],
        attachmentId: Number(match[2]),
      })),
    )
  }
  segments = attachmentParts

  const resumeParts: ChatMessageSegment[] = []
  for (const segment of segments) {
    if (segment.type !== 'text') {
      resumeParts.push(segment)
      continue
    }
    resumeParts.push(
      ...splitByPattern(segment.text, RESUME_LINK_RE, (match) => ({
        type: 'resume',
        label: match[1],
        resumeId: Number(match[2]),
      })),
    )
  }

  return splitBoldSegments(resumeParts)
}

/** 将「附件ID: 3」「简历ID: 7」转为可点击片段（兼容旧回复） */
export function parseInlineIdSegments(content: string): ChatMessageSegment[] {
  const base = parseChatMessageSegments(content)
  const result: ChatMessageSegment[] = []

  for (const segment of base) {
    if (segment.type !== 'text') {
      result.push(segment)
      continue
    }
    const text = segment.text
    const idPattern = /(附件ID[:：]\s*(\d+))|(简历ID[:：]\s*(\d+))/g
    let lastIndex = 0
    let match: RegExpExecArray | null
    let matched = false
    while ((match = idPattern.exec(text)) !== null) {
      matched = true
      if (match.index > lastIndex) {
        result.push({ type: 'text', text: text.slice(lastIndex, match.index) })
      }
      if (match[2]) {
        result.push({
          type: 'attachment',
          label: `附件#${match[2]}`,
          attachmentId: Number(match[2]),
        })
      } else if (match[4]) {
        result.push({
          type: 'resume',
          label: `简历#${match[4]}`,
          resumeId: Number(match[4]),
        })
      }
      lastIndex = idPattern.lastIndex
    }
    if (!matched) {
      result.push(segment)
    } else if (lastIndex < text.length) {
      result.push({ type: 'text', text: text.slice(lastIndex) })
    }
  }

  return result
}
