import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginResponse } from '@/api/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<LoginResponse | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.role || '')
  const isAdmin = computed(() => role.value === 'ADMIN')
  const isDoctor = computed(() => role.value === 'DOCTOR')
  const isTherapist = computed(() => role.value === 'THERAPIST' || role.value === 'NURSE')
  const groupId = computed(() => userInfo.value?.groupId || null)

  function setAuth(t: string, info: LoginResponse) {
    token.value = t
    userInfo.value = info
    localStorage.setItem('token', t)
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  function loadFromStorage() {
    const stored = localStorage.getItem('userInfo')
    if (stored) {
      try {
        userInfo.value = JSON.parse(stored)
      } catch { /* ignore */ }
    }
  }

  function updateUserInfo(info: Partial<LoginResponse>) {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, ...info }
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  loadFromStorage()

  return { token, userInfo, isLoggedIn, role, isAdmin, isDoctor, isTherapist, groupId, setAuth, updateUserInfo, logout }
})
