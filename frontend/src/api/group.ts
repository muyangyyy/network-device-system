import request from '@/utils/request'

export interface Group {
  id: number
  parentId: number
  name: string
  code?: string
  managerId?: number
  managerName?: string
  description?: string
  sort: number
  status: number
  children?: Group[]
}

export function getGroupTree() {
  return request.get<any, Group[]>('/device-groups/tree')
}

export function createGroup(data: Partial<Group>) {
  return request.post('/device-groups', data)
}

export function updateGroup(id: number, data: Partial<Group>) {
  return request.put(`/device-groups/${id}`, data)
}

export function deleteGroup(id: number) {
  return request.delete(`/device-groups/${id}`)
}

export function updateGroupManager(id: number, data: { managerId: number }) {
  return request.put(`/device-groups/${id}/manager`, null, { params: data })
}

export function migrateDevices(data: { deviceIds: number[]; targetGroupId: number }) {
  return request.post('/device-groups/migrate-devices', data)
}
