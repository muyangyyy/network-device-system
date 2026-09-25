import request from '@/utils/request'

export interface Permission {
  id: number
  parentId: number
  name: string
  code: string
  type: number
  path?: string
  icon?: string
  sort: number
  status: number
  children?: Permission[]
}

export function getPermissionTree() {
  return request.get<any, Permission[]>('/permissions/tree')
}

export function createPermission(data: Partial<Permission>) {
  return request.post('/permissions', data)
}

export function updatePermission(id: number, data: Partial<Permission>) {
  return request.put(`/permissions/${id}`, data)
}

export function deletePermission(id: number) {
  return request.delete(`/permissions/${id}`)
}
