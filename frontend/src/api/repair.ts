import request from '@/utils/request'

export interface RepairOrder {
  id: number
  workOrderNo: string
  deviceId: number
  deviceCode: string
  deviceName: string
  repairType: string
  status: string
  priority: string
  faultDescription: string
  faultTime: string
  reporterId: number
  reporterName: string
  repairUserId?: number
  repairUserName?: string
  acceptorId?: number
  acceptorName?: string
  repairSolution?: string
  repairResult?: string
  repairStartTime?: string
  repairEndTime?: string
  completedTime?: string
  acceptTime?: string
  rejectReason?: string
  // 注意：后端返回的是嵌套对象（RepairXxxDetailVO），不是字符串
  hardwareDetail?: HardwareDetail
  debugDetail?: DebugDetail
  opticalDetail?: OpticalDetail
  attachments?: Attachment[]
  logs?: OrderLog[]
  createTime: string
  updateTime: string
}

export interface Attachment {
  id: number
  fileName: string
  filePath: string
  fileSize: number
  uploadTime: string
  uploaderName: string
}

export interface OrderLog {
  id: number
  orderNo: string
  fromStatus?: string
  toStatus: string
  operatorId: number
  operatorName: string
  remark?: string
  createTime: string
}

export interface RepairOrderQuery {
  page: number
  size: number
  workOrderNo?: string
  deviceCode?: string
  deviceName?: string
  repairType?: string
  status?: string
  priority?: string
  faultTimeStart?: string
  faultTimeEnd?: string
}

export function getRepairOrders(params: RepairOrderQuery) {
  return request.get<any, { records: RepairOrder[]; total: number }>('/repairs', { params })
}

export function getRepairOrder(id: number) {
  return request.get<any, RepairOrder>(`/repairs/${id}`)
}

export function createRepairOrder(data: Partial<RepairOrder>) {
  return request.post('/repairs', data)
}

export function updateRepairOrder(id: number, data: Partial<RepairOrder>) {
  return request.put(`/repairs/${id}`, data)
}

export function submitOrder(id: number) {
  return request.put(`/repairs/${id}/submit`)
}

export function assignOrder(id: number, data: { repairUserId: number }) {
  return request.put(`/repairs/${id}/assign`, data)
}

export function startOrder(id: number) {
  return request.put(`/repairs/${id}/start`)
}

/** 硬件维修明细。字段名必须与后端 CompleteDTO.HardwareDetailDTO 完全一致，否则会被静默丢弃 */
export interface HardwareDetail {
  damagedComponent?: string
  replacementPartName?: string
  replacementPartModel?: string
  replacementPartQuantity?: number
  replacementPartCost?: number
  oldPartDisposalMethod?: string
  hardwareFailureCode?: string
  whetherUnderWarranty?: number
  supplier?: string
  remark?: string
}

/** 调试维修明细。字段名必须与后端 CompleteDTO.DebugDetailDTO 完全一致 */
export interface DebugDetail {
  configurationChangeContent?: string
  oldFirmwareVersion?: string
  newFirmwareVersion?: string
  oldIpAddress?: string
  newIpAddress?: string
  oldGateway?: string
  newGateway?: string
  oldVlan?: string
  newVlan?: string
  routeChangeContent?: string
  firewallPolicyChange?: string
  permissionChangeContent?: string
  networkParameterChangeRecord?: string
  rollbackPlan?: string
  testResult?: string
  remark?: string
}

/** 光路维修明细。字段名必须与后端 CompleteDTO.OpticalDetailDTO 完全一致 */
export interface OpticalDetail {
  faultOpticalPoint?: string
  opticalRouteName?: string
  cableSection?: string
  cableLength?: number
  opticalPowerBefore?: number
  opticalPowerAfter?: number
  attenuationBefore?: number
  attenuationAfter?: number
  wavelength?: string
  splitterStatus?: string
  jumperStatus?: string
  cableDamageDescription?: string
  linkTestResult?: string
  testTool?: string
  testPerson?: string
  remark?: string
}

export interface CompleteOrderPayload {
  repairSolution: string
  repairResult: string
  remark?: string
  // 注意：这三项必须是对象。写成 JSON.stringify(...) 的字符串会让 Jackson 反序列化失败并返回 400
  hardwareDetail?: HardwareDetail
  debugDetail?: DebugDetail
  opticalDetail?: OpticalDetail
}

export function completeOrder(id: number, data: CompleteOrderPayload) {
  return request.put(`/repairs/${id}/complete`, data)
}

export function approveOrder(id: number, data?: { remark?: string }) {
  return request.put(`/repairs/${id}/accept`, data)
}

export function rejectOrder(id: number, data: { rejectReason: string }) {
  return request.put(`/repairs/${id}/reject`, data)
}

export function delayOrder(id: number, data: { reason: string }) {
  return request.put(`/repairs/${id}/delay`, data)
}

export function repairAgainOrder(id: number, data?: { remark?: string }) {
  return request.put(`/repairs/${id}/repair-again`, data)
}

/** 关闭工单（后端仅允许「已验收」状态关闭，关闭后可发起返修） */
export function closeOrder(id: number, data?: { remark?: string }) {
  return request.put(`/repairs/${id}/close`, data)
}

export function cancelOrder(id: number, data?: { reason?: string }) {
  return request.put(`/repairs/${id}/cancel`, data)
}

export function getOrderLogs(id: number) {
  return request.get<any, OrderLog[]>(`/repairs/${id}/logs`)
}

export function uploadAttachment(id: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/repairs/${id}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function exportOrders(params: RepairOrderQuery) {
  return request.get<any, RepairOrder[]>('/repairs/export', { params })
}

export function deleteRepairOrder(id: number) {
  return request.delete(`/repairs/${id}`)
}
