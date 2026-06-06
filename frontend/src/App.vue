<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { watch } from 'vue'

const authStore = useAuthStore()
const router = useRouter()

watch(() => authStore.isLoggedIn, (val) => {
  if (!val) {
    router.push('/login')
  }
})
</script>

<template>
  <router-view />
</template>
