import request from '@/utils/request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  refreshToken?: string
}

export interface UserInfo {
  id: number
  username: string
  realName: string
  nickname?: string
  avatar?: string
  email?: string
  phone?: string
  departmentId?: number
  departmentName?: string
  status: number
  lastLoginTime?: string
  roles: string[]
  permissions: string[]
}

export function login(data: LoginParams) {
  return request.post<any, LoginResult>('/auth/login', data)
}

export function logout() {
  return request.post('/auth/logout')
}

export function getCurrentUser() {
  return request.get<any, UserInfo>('/auth/current-user')
}

/**
 * 修改密码。
 *
 * <p>confirmPassword 必须一起提交：后端 ChangePasswordDTO 的 confirmPassword 带
 * {@code @NotBlank}，且 AuthServiceImpl 会做「两次输入是否一致」的比对；
 * 少传该字段会直接 400（确认密码不能为空）。
 */
export function changePassword(data: {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}) {
  return request.put('/auth/change-password', data)
}
