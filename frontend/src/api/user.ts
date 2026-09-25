import request from '@/utils/request'

export interface User {
  id: number
  username: string
  nickname: string
  email?: string
  phone?: string
  avatar?: string
  status: number
  departmentId?: number
  departmentName?: string
  roleIds: number[]
  roleNames: string[]
  createTime: string
  updateTime: string
}

export interface UserQuery {
  page: number
  size: number
  username?: string
  nickname?: string
  status?: number
  departmentId?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  /** 后端 common/PageResult 的字段名是 pageSize，不是 size */
  pageSize: number
}

export function getUsers(params: UserQuery) {
  return request.get<any, PageResult<User>>('/users', { params })
}

export function createUser(data: Partial<User> & { password?: string }) {
  return request.post('/users', data)
}

export function updateUser(id: number, data: Partial<User>) {
  return request.put(`/users/${id}`, data)
}

export function deleteUser(id: number) {
  return request.delete(`/users/${id}`)
}

export function updateUserStatus(id: number, status: number) {
  return request.put(`/users/${id}/status`, { status })
}

export function resetPassword(id: number) {
  return request.put<{ password: string } | string>(`/users/${id}/reset-password`)
}
