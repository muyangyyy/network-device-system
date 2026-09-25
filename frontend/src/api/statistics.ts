import request from '@/utils/request'

export interface OverviewData {
  totalDevices: number
  normalDevices: number
  faultDevices: number
  idleDevices: number
  scrappedDevices: number
  pendingOrders: number
  monthlyOrders: number
  avgRepairTime: number
  completionRate: number
  acceptanceRate: number
}

export interface RepairTypeStats {
  name: string
  value: number
}

export interface RepairTrend {
  dates: string[]
  values: number[]
}

export interface GroupRanking {
  name: string
  value: number
}

export interface DeviceRanking {
  deviceCode: string
  deviceName: string
  repairCount: number
}

export interface EfficiencyStats {
  avgResponseTime: number
  avgRepairTime: number
  completionRate: number
  acceptanceRate: number
  overdueCount: number
  processingCount: number
  firstFixRate: number
  retryRate: number
}

export interface DeviceStatusStats {
  name: string
  value: number
}

export interface StatsQuery {
  startTime?: string
  endTime?: string
  groupId?: number
  deviceType?: string
  repairType?: string
  repairUserId?: number
}

export function getOverview(params?: StatsQuery) {
  return request.get<any, OverviewData>('/statistics/overview', { params })
}

export function getRepairTypeStats(params?: StatsQuery) {
  return request.get<any, RepairTypeStats[]>('/statistics/repair-type', { params })
}

export function getRepairTrend(params?: StatsQuery & { period?: string }) {
  return request.get<any, RepairTrend>('/statistics/repair-trend', { params })
}

export function getGroupRanking(params?: StatsQuery) {
  return request.get<any, GroupRanking[]>('/statistics/group-ranking', { params })
}

export function getDeviceRanking(params?: StatsQuery) {
  return request.get<any, DeviceRanking[]>('/statistics/device-ranking', { params })
}

export function getEfficiencyStats(params?: StatsQuery) {
  return request.get<any, EfficiencyStats>('/statistics/efficiency', { params })
}

export function getDeviceStatusStats(params?: StatsQuery) {
  return request.get<any, DeviceStatusStats[]>('/statistics/device-status', { params })
}
