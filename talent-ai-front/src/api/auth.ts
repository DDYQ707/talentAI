import request from '@/utils/request'

export interface LoginUserInfo {
  userId: number
  nickname: string
  userType: number
}

export interface LoginResponse {
  code: number
  msg: string
  token?: string
  userInfo?: LoginUserInfo
}

export interface RegisterResponse {
  code: number
  msg: string
  userInfo?: LoginUserInfo
}

export interface SendOtpResponse {
  code: number
  msg: string
  expireSeconds?: number
  /** 开发环境后端返回，便于联调 */
  devCode?: string
}

/** 密码登录 — 对接 talent-auth POST /api/auth/login */
export function loginByPassword(username: string, password: string) {
  const params = new URLSearchParams()
  params.append('username', username.trim())
  params.append('password', password)
  return request.post<LoginResponse>('/api/auth/login', params, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  }) as Promise<LoginResponse>
}

/** 发送登录验证码 — POST /api/auth/otp/send */
export function sendLoginOtp(account: string) {
  const params = new URLSearchParams()
  params.append('account', account.trim())
  return request.post<SendOtpResponse>('/api/auth/otp/send', params, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  }) as Promise<SendOtpResponse>
}

/** 验证码登录 — POST /api/auth/login/otp */
export function loginByOtp(account: string, code: string) {
  const params = new URLSearchParams()
  params.append('account', account.trim())
  params.append('code', code.trim())
  return request.post<LoginResponse>('/api/auth/login/otp', params, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  }) as Promise<LoginResponse>
}

/** 求职者自助注册 — POST /api/auth/register */
export function register(account: string, password: string) {
  const params = new URLSearchParams()
  params.append('account', account.trim())
  params.append('password', password)
  return request.post<RegisterResponse>('/api/auth/register', params, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  }) as Promise<RegisterResponse>
}
