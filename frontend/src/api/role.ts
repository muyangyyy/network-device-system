import request from '@/utils/request'

export interface Role {
  id: number
  name: string
  code: string
  description?: string
  permissionIds: number[]
  status: number
  createTime: string
  updateTime: string
}

export function getRoles(params?: { page?: number; size?: number }) {
  return request.get<any, { records: Role[]; total: number }>('/roles', { params })
}

/** 不分页获取全部角色（用于下拉选择） */
export function getAllRoles() {
  return request.get<any, Role[]>('/roles/all')
}

export function createRole(data: Partial<Role>) {
  return request.post('/roles', data)
}

export function updateRole(id: number, data: Partial<Role>) {
  return request.put(`/roles/${id}`, data)
}

export function deleteRole(id: number) {
  return request.delete(`/roles/${id}`)
}
