import request from '@/utils/request'

export interface Device {
  id: number
  deviceCode: string
  deviceName: string
  deviceType: string
  brand?: string
  model?: string
  serialNumber?: string
  ipAddress?: string
  macAddress?: string
  groupId?: number
  groupName?: string
  responsibleUserId?: number
  responsibleUserName?: string
  status: string
  purchaseDate?: string
  warrantyExpireDate?: string
  installationLocation?: string
  supplier?: string
  department?: string
  remark?: string
  /** 后端 NetworkDeviceVO 返回的是 createdByName，没有 createdBy */
  createdByName?: string
  createdAt: string
  updatedAt: string
}

export interface DeviceQuery {
  page: number
  size: number
  deviceCode?: string
  deviceName?: string
  deviceType?: string
  brand?: string
  status?: string
  groupId?: number
  responsibleUserId?: number
  ipAddress?: string
  serialNumber?: string
}

export interface DeviceRepair {
  id: number
  workOrderNo: string
  repairType: string
  status: string
  faultDescription: string
  repairSolution?: string
  reporterName: string
  repairUserName?: string
  faultTime: string
  createTime: string
}

export interface DeviceStatusLog {
  id: number
  deviceId: number
  fromStatus: string
  toStatus: string
  reason: string
  operatorName: string
  createTime: string
}

export function getDevices(params: DeviceQuery) {
  return request.get<any, { records: Device[]; total: number }>('/devices', { params })
}

export function getDeviceById(id: number) {
  return request.get<any, Device>(`/devices/${id}`)
}

export function createDevice(data: Partial<Device>) {
  return request.post('/devices', data)
}

export function updateDevice(id: number, data: Partial<Device>) {
  return request.put(`/devices/${id}`, data)
}

export function deleteDevice(id: number) {
  return request.delete(`/devices/${id}`)
}

export function updateDeviceStatus(id: number, data: { status: string; reason?: string }) {
  return request.put(`/devices/${id}/status`, data)
}

export function batchGroup(data: { deviceIds: number[]; groupId: number }) {
  return request.put('/devices/batch-group', data)
}

export function importDevices(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/devices/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function exportDevices(params: DeviceQuery) {
  return request.get('/devices/export', {
    params,
    responseType: 'blob'
  })
}

export function getDeviceRepairs(id: number, params?: { page?: number; size?: number }) {
  return request.get<any, { records: DeviceRepair[]; total: number }>(`/devices/${id}/repairs`, { params })
}

export function getDeviceStatusLogs(id: number, params?: { page?: number; size?: number }) {
  return request.get<any, { records: DeviceStatusLog[]; total: number }>(`/devices/${id}/status-logs`, { params })
}
