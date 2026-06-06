<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { post } from '@/api'
import { useAuthStore } from '@/stores/auth'
import type { LoginRequest, LoginResponse } from '@/api/types'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = reactive<LoginRequest>({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await post<{ data: LoginResponse }>('/auth/login', form)
    authStore.setAuth(res.data.token, res.data)
    ElMessage.success('登录成功')
    if (authStore.isDoctor) {
      router.push('/orders')
    } else if (authStore.isTherapist) {
      router.push('/tasks')
    } else {
      router.push('/square')
    }
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="login-title">康复科管理系统</h1>
      <p class="login-subtitle">Rehabilitation Management System</p>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <p class="login-hint">测试账号: admin / doctor1 / therapist1 / nurse1 &nbsp; 密码: rehab123</p>
    </div>
  </div>
</template>

<script lang="ts">
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  components: { User, Lock },
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1D9E75 0%, #378ADD 100%);
}
.login-card {
  width: 420px;
  padding: 48px 40px 32px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}
.login-title {
  text-align: center;
  font-size: 26px;
  color: #1D9E75;
  margin-bottom: 4px;
}
.login-subtitle {
  text-align: center;
  font-size: 13px;
  color: #999;
  margin-bottom: 32px;
}
.login-hint {
  text-align: center;
  font-size: 12px;
  color: #ccc;
  margin-top: 16px;
}
</style>
