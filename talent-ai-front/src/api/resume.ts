import request from '@/utils/request'

export interface ResumeListItem {
  id: number
  resumeName: string
  isDefault?: number
  attachmentId?: number
  fileName?: string
  fileType?: string
  fileSize?: number
  fileUrl?: string
  updatedAt?: string
}

export interface ResumeUploadResult {
  resumeId: number
  attachmentId: number
  resumeName: string
  fileName: string
  fileType: string
  fileSize: number
  fileUrl: string
  objectKey: string
}

export interface ResumePreviewResult {
  attachmentId: number
  resumeId?: number
  fileName: string
  fileType: string
  presignedUrl: string
  expiresIn: number
}

/** 附件简历列表（投递用） */
export function fetchAttachmentResumes() {
  return request.get<ResumeListItem[]>('/api/resume/attachment/my') as Promise<ResumeListItem[]>
}

/** @deprecated 请使用 fetchAttachmentResumes */
export function fetchMyResumes() {
  return fetchAttachmentResumes()
}

/** 上传简历附件（pdf/doc/docx，最大 10MB）→ MinIO + 落库 */
export function uploadResumeFile(
  file: File,
  options?: { resumeId?: number; resumeName?: string },
) {
  const form = new FormData()
  form.append('file', file)
  if (options?.resumeId != null) {
    form.append('resumeId', String(options.resumeId))
  }
  if (options?.resumeName) {
    form.append('resumeName', options.resumeName)
  }
  return request.post<ResumeUploadResult>('/api/resume/file/upload', form, {
    timeout: 60000,
  }) as Promise<ResumeUploadResult>
}

/** 按附件 ID 获取预签名预览（私有桶） */
export function fetchResumePreview(attachmentId: number) {
  return request.get<ResumePreviewResult>(`/api/resume/file/preview/${attachmentId}`) as Promise<ResumePreviewResult>
}

/** 按简历 ID 获取最新附件预签名预览 */
export function fetchResumePreviewByResume(resumeId: number) {
  return request.get<ResumePreviewResult>(`/api/resume/file/preview/by-resume/${resumeId}`) as Promise<ResumePreviewResult>
}

/** 在新窗口打开预览（PDF 内联，其他格式触发下载） */
export function openResumePreview(preview: ResumePreviewResult) {
  window.open(preview.presignedUrl, '_blank', 'noopener,noreferrer')
}

/** @deprecated 请使用 fetchResumePreview */
export function fetchResumeDownloadUrl(attachmentId: number) {
  return request.get<string>(`/api/resume/file/download/${attachmentId}`) as Promise<string>
}

/** 删除简历（逻辑删除） */
export function deleteResume(resumeId: number) {
  return request.delete(`/api/resume/${resumeId}`)
}
