import { defineStore } from 'pinia'
import { ref } from 'vue'
import { get, put } from '@/api'

export interface NotificationItem {
  id: number
  userId: number
  title: string
  content: string
  type: string
  isRead: number
  sourceId: number | null
  createTime: string
  updateTime: string
}

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const notifications = ref<NotificationItem[]>([])
  const unreadCount = ref(0)

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  async function fetchNotifications(page = 1, size = 20) {
    try {
      const res: any = await get('/notifications', { page, size })
      notifications.value = res.data?.records || []
      return res.data
    } catch (e) {
      console.error('Failed to fetch notifications', e)
      return null
    }
  }

  async function markAsRead(id: number) {
    try {
      await put(`/notifications/${id}/read`)
      const idx = notifications.value.findIndex((n) => n.id === id)
      if (idx !== -1) {
        notifications.value[idx].isRead = 1
      }
      await fetchUnreadCount()
    } catch (e) {
      console.error('Failed to mark notification as read', e)
    }
  }

  async function markAllRead() {
    try {
      await put('/notifications/read-all')
      notifications.value.forEach((n) => (n.isRead = 1))
      unreadCount.value = 0
    } catch (e) {
      console.error('Failed to mark all as read', e)
    }
  }

  async function fetchUnreadCount() {
    try {
      const res: any = await get('/notifications/unread-count')
      unreadCount.value = res.data ?? 0
    } catch (e) {
      console.error('Failed to fetch unread count', e)
    }
  }

  return {
    sidebarCollapsed,
    notifications,
    unreadCount,
    toggleSidebar,
    fetchNotifications,
    markAsRead,
    markAllRead,
    fetchUnreadCount,
  }
})
