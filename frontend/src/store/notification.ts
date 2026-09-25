import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getNotifications,
  getUnreadCount,
  markAsRead,
  markAllAsRead,
  type Notification
} from '@/api/notification'
import { getToken } from '@/utils/auth'

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<Notification[]>([])
  const total = ref(0)
  // 未读数由服务端计数返回，**不能**从已拉取的当前页里 filter 出来：
  // 首页只取 20 条，未读超过 20 条时徽标会少算。
  const unreadCount = ref(0)
  let pollingTimer: ReturnType<typeof setInterval> | null = null

  async function fetchNotifications(page = 1, size = 10) {
    if (!getToken()) return
    try {
      const res = await getNotifications({ page, size })
      notifications.value = res.records ?? []
      total.value = res.total ?? 0
    } catch (e) {
      // ignore
    }
  }

  async function fetchUnreadCount() {
    if (!getToken()) return
    try {
      unreadCount.value = (await getUnreadCount()) ?? 0
    } catch (e) {
      // ignore
    }
  }

  /** 列表与未读数一起刷新（打开通知面板 / 轮询时用） */
  async function refresh(page = 1, size = 20) {
    await Promise.all([fetchNotifications(page, size), fetchUnreadCount()])
  }

  async function readNotification(id: number) {
    await markAsRead(id)
    const item = notifications.value.find((n) => n.id === id)
    if (item && !item.isRead) {
      item.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  }

  async function readAll() {
    await markAllAsRead()
    notifications.value.forEach((n) => (n.isRead = true))
    unreadCount.value = 0
  }

  function startPolling(interval = 60000) {
    stopPolling()
    refresh(1, 20)
    pollingTimer = setInterval(() => {
      // 轮询只取未读数即可，避免每次都重拉列表打断用户正在看的列表
      fetchUnreadCount()
    }, interval)
  }

  function stopPolling() {
    if (pollingTimer) {
      clearInterval(pollingTimer)
      pollingTimer = null
    }
  }

  return {
    notifications,
    total,
    unreadCount,
    fetchNotifications,
    fetchUnreadCount,
    refresh,
    readNotification,
    readAll,
    startPolling,
    stopPolling
  }
})
