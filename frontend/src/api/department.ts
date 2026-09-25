import request from '@/utils/request'

export interface Department {
  id: number
  parentId: number
  name: string
  code?: string
  managerId?: number
  managerName?: string
  sort: number
  status: number
  children?: Department[]
}

export function getDepartmentTree() {
  return request.get<any, Department[]>('/departments/tree')
}

export function createDepartment(data: Partial<Department>) {
  return request.post('/departments', data)
}

export function updateDepartment(id: number, data: Partial<Department>) {
  return request.put(`/departments/${id}`, data)
}

export function deleteDepartment(id: number) {
  return request.delete(`/departments/${id}`)
}
