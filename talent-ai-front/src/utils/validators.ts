const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PHONE_RE = /^1[3-9]\d{9}$/
/** 企业端常用登录名（如 admin、hr01） */
const USERNAME_RE = /^[a-zA-Z0-9_.-]{3,64}$/

export function isPhoneOrEmail(value: string): boolean {
  const v = value.trim()
  if (!v) return false
  return EMAIL_RE.test(v) || PHONE_RE.test(v)
}

export function isLoginAccount(value: string): boolean {
  const v = value.trim()
  if (!v) return false
  return isPhoneOrEmail(v) || USERNAME_RE.test(v)
}

/** 注册：仅手机号或邮箱 */
export function validateAccount(value: string): string | null {
  const v = value.trim()
  if (!v) return '请填写手机号或邮箱'
  if (!isPhoneOrEmail(v)) {
    if (/^1\d{10}$/.test(v) && !PHONE_RE.test(v)) {
      return '手机号格式不正确：需为大陆 11 位号段（1 开头，第 2 位为 3–9，如 13800138000）'
    }
    return '请输入正确的手机号（11 位大陆号段）或邮箱'
  }
  return null
}

/** 登录：用户名 / 手机号 / 邮箱 */
export function validateLoginAccount(value: string): string | null {
  const v = value.trim()
  if (!v) return '请输入用户名、手机号或邮箱'
  if (!isLoginAccount(v)) return '请输入正确的用户名、手机号或邮箱'
  return null
}

export function validatePassword(password: string, minLen = 6): string | null {
  if (!password) return '请输入密码'
  if (password.length < minLen) return `密码至少 ${minLen} 位`
  return null
}

export function validatePasswordConfirm(password: string, confirm: string): string | null {
  if (password !== confirm) return '两次输入的密码不一致'
  return null
}

export function getErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof Error && error.message) return error.message
  return fallback
}
