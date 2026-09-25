import request from '@/utils/request'

export interface Notification {
  id: number
  title: string
  content: string
  type: string
  isRead: boolean
  relatedId?: number
  relatedType?: string
  createTime: string
}

export function getNotifications(params?: { page?: number; size?: number; isRead?: boolean }) {
  return request.get<any, { records: Notification[]; total: number }>('/notifications', { params })
}

/** 未读总数（服务端计数，不受分页影响） */
export function getUnreadCount() {
  return request.get<any, number>('/notifications/unread-count')
}

export function markAsRead(id: number) {
  return request.put(`/notifications/${id}/read`)
}

export function markAllAsRead() {
  return request.put('/notifications/read-all')
}
