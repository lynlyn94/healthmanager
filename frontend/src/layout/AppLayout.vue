<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import {
  House, List, Clock, DataAnalysis, Grid,
  Document, User, Setting, Management,
  Fold, Expand, Bell, SwitchButton
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const appStore = useAppStore()

const notifiPopVisible = ref(false)

const menuItems = computed(() => {
  const items: any[] = []
  if (authStore.isTherapist || authStore.isAdmin) {
    items.push({ path: '/tasks', title: '治疗任务', icon: Clock })
    items.push({ path: '/patients', title: '患者列表', icon: List })
    items.push({ path: '/workload', title: '工作量统计', icon: DataAnalysis })
    items.push({ path: '/square', title: '功能广场', icon: Grid })
  }
  if (authStore.isDoctor || authStore.isAdmin) {
    items.push({ path: '/orders', title: '医嘱管理', icon: Document })
    items.push({ path: '/doctor/patients', title: '患者列表', icon: List })
  }
  items.push({ path: '/settings', title: '个人设置', icon: Setting })
  if (authStore.isAdmin) {
    items.push({ type: 'divider' })
    items.push({ path: '/admin/users', title: '用户管理', icon: User })
    items.push({ path: '/admin/groups', title: '小组管理', icon: Management })
    items.push({ path: '/admin/dicts', title: '字典管理', icon: Setting })
    items.push({ path: '/admin/logs', title: '操作日志', icon: Document })
    items.push({ path: '/admin/stats', title: '数据统计', icon: DataAnalysis })
  }
  return items
})

function goHome() {
  if (authStore.isDoctor) router.push('/orders')
  else router.push('/square')
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

function isActive(path: string) {
  return route.path === path || route.path.startsWith(path + '/')
}

function formatTime(timeStr: string) {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

async function handleNotificationClick(item: any) {
  if (item.isRead === 0) {
    await appStore.markAsRead(item.id)
  }
  notifiPopVisible.value = false
}

async function handleMarkAllRead() {
  await appStore.markAllRead()
}

onMounted(async () => {
  await appStore.fetchNotifications()
  await appStore.fetchUnreadCount()
})
</script>

<template>
  <el-container class="app-layout">
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '220px'" class="app-sidebar">
      <div class="logo" @click="goHome">
        <span v-if="!appStore.sidebarCollapsed" class="logo-text">康复科管理系统</span>
        <span v-else class="logo-text-mini">康复</span>
      </div>
      <el-menu
        :default-active="route.path"
        :collapse="appStore.sidebarCollapsed"
        background-color="#1D9E75"
        text-color="#ffffff"
        active-text-color="#ffffff"
        router
      >
        <template v-for="item in menuItems" :key="item.path || item.type">
          <div v-if="item.type === 'divider'" style="border-top: 1px solid rgba(255,255,255,0.2); margin: 8px 16px" />
          <el-menu-item v-else :index="item.path" :class="{ 'is-active-menu': isActive(item.path) }">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="appStore.toggleSidebar()">
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
        </div>
        <div class="header-right">
          <el-popover
            placement="bottom-end"
            :width="360"
            trigger="click"
            v-model:visible="notifiPopVisible"
            popper-class="notifi-popover"
          >
            <template #reference>
              <el-badge :value="appStore.unreadCount" :hidden="appStore.unreadCount === 0">
                <el-icon :size="20" style="cursor: pointer"><Bell /></el-icon>
              </el-badge>
            </template>
            <div class="notifi-panel">
              <div class="notifi-header">
                <span class="notifi-title">消息通知</span>
                <el-button text type="primary" size="small" @click="handleMarkAllRead">全部已读</el-button>
              </div>
              <div class="notifi-list" v-if="appStore.notifications.length > 0">
                <div
                  v-for="item in appStore.notifications"
                  :key="item.id"
                  class="notifi-item"
                  :class="{ unread: item.isRead === 0 }"
                  @click="handleNotificationClick(item)"
                >
                  <div class="notifi-dot" v-if="item.isRead === 0"></div>
                  <div class="notifi-body">
                    <div class="notifi-item-title" :class="{ bold: item.isRead === 0 }">{{ item.title }}</div>
                    <div class="notifi-item-content">{{ item.content || '' }}</div>
                    <div class="notifi-item-time">{{ formatTime(item.createTime) }}</div>
                  </div>
                </div>
              </div>
              <div class="notifi-empty" v-else>
                <span>暂无通知</span>
              </div>
            </div>
          </el-popover>
          <el-dropdown trigger="click">
            <span class="user-info">
              {{ authStore.userInfo?.realName }}
              <el-icon><SwitchButton /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/settings')">个人设置</el-dropdown-item>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-layout {
  height: 100vh;
}
.app-sidebar {
  background-color: #1D9E75;
  overflow: hidden;
  transition: width 0.3s;
}
.app-sidebar .logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
}
.logo-text-mini {
  font-size: 16px;
}
.app-sidebar :deep(.el-menu) {
  border-right: none;
}
.app-sidebar :deep(.el-menu-item.is-active),
.app-sidebar :deep(.el-menu-item.is-active-menu) {
  background-color: rgba(255,255,255,0.15) !important;
}
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
  height: 60px;
}
.collapse-btn {
  font-size: 20px;
  cursor: pointer;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}
.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}
.app-main {
  background-color: #F5F7FA;
  padding: 20px;
}
</style>

<style>
.notifi-popover {
  padding: 0 !important;
}
.notifi-panel {
  max-height: 420px;
  display: flex;
  flex-direction: column;
}
.notifi-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
}
.notifi-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.notifi-list {
  overflow-y: auto;
  flex: 1;
}
.notifi-item {
  display: flex;
  align-items: flex-start;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f2f3f5;
  transition: background 0.2s;
}
.notifi-item:hover {
  background-color: #f5f7fa;
}
.notifi-item.unread {
  background-color: #ecf5ff;
}
.notifi-dot {
  width: 8px;
  height: 8px;
  min-width: 8px;
  border-radius: 50%;
  background-color: #409eff;
  margin-top: 6px;
  margin-right: 10px;
}
.notifi-body {
  flex: 1;
  min-width: 0;
}
.notifi-item-title {
  font-size: 14px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notifi-item-title.bold {
  font-weight: 600;
  color: #303133;
}
.notifi-item-content {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notifi-item-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 4px;
}
.notifi-empty {
  padding: 40px 16px;
  text-align: center;
  color: #c0c4cc;
  font-size: 14px;
}
</style>
