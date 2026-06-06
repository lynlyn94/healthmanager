import { ref, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

let client: Client | null = null
const connected = ref(false)

export function useWebSocket() {
  const authStore = useAuthStore()
  const appStore = useAppStore()

  function connect() {
    if (client?.active) return

    client = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      connectHeaders: {
        Authorization: `Bearer ${authStore.token}`,
      },
      heartbeatIncoming: 30000,
      heartbeatOutgoing: 30000,
      reconnectDelay: 5000,
      onConnect: () => {
        connected.value = true
        console.log('WebSocket connected')

        const userId = authStore.userInfo?.userId
        const groupId = authStore.userInfo?.groupId

        if (userId) {
          client?.subscribe(`/topic/task/${userId}`, (message) => {
            const data = JSON.parse(message.body)
            appStore.addNotification({ title: '任务通知', message: data.message || data.content })
          })
        }
        if (groupId) {
          client?.subscribe(`/topic/task/group/${groupId}`, (message) => {
            const data = JSON.parse(message.body)
            appStore.addNotification({ title: '小组通知', message: data.message || data.content })
          })
        }
        client?.subscribe('/topic/system/announcement', (message) => {
          const data = JSON.parse(message.body)
          appStore.addNotification({ title: '系统公告', message: data.message || data.content })
        })
      },
      onDisconnect: () => {
        connected.value = false
        console.log('WebSocket disconnected')
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message'])
      },
    })

    client.activate()
  }

  function disconnect() {
    if (client?.active) {
      client.deactivate()
    }
    connected.value = false
  }

  function subscribeOrderNotifications(patientId: number) {
    if (client?.active) {
      client.subscribe(`/topic/order/${patientId}`, (message) => {
        const data = JSON.parse(message.body)
        appStore.addNotification({ title: '医嘱变更', message: data.message || data.content })
      })
    }
  }

  onUnmounted(() => {
    // Don't disconnect on unmount - keep connection alive across page navigation
  })

  return { connected, connect, disconnect, subscribeOrderNotifications }
}
