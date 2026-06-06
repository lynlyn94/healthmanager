<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { put } from '@/api'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const authStore = useAuthStore()

// ---- Tab 1: 个人信息 ----
const profileFormRef = ref<FormInstance>()
const profileForm = reactive({
  realName: authStore.userInfo?.realName || '',
  phone: authStore.userInfo?.phone || '',
  email: authStore.userInfo?.email || '',
  avatar: authStore.userInfo?.avatar || '',
})
const profileSaving = ref(false)

async function saveProfile() {
  if (!profileFormRef.value) return
  try {
    await profileFormRef.value.validate()
  } catch {
    return
  }
  profileSaving.value = true
  try {
    await put('/auth/userinfo', profileForm)
    authStore.updateUserInfo(profileForm)
    ElMessage.success('个人信息更新成功')
  } catch {
    // error handled by interceptor
  } finally {
    profileSaving.value = false
  }
}

// ---- Tab 2: 修改密码 ----
const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const passwordSaving = ref(false)

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码长度不能少于6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

async function savePassword() {
  if (!passwordFormRef.value) return
  try {
    await passwordFormRef.value.validate()
  } catch {
    return
  }
  passwordSaving.value = true
  try {
    await put('/auth/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    ElMessage.success('密码修改成功')
    passwordFormRef.value.resetFields()
  } catch {
    // error handled by interceptor
  } finally {
    passwordSaving.value = false
  }
}
</script>

<template>
  <div class="settings-page">
    <h3 class="page-title">个人设置</h3>

    <el-tabs>
      <!-- 个人信息 -->
      <el-tab-pane label="个人信息">
        <el-card class="settings-card">
          <template #header>
            <span class="card-header-text">基本信息</span>
          </template>

          <el-descriptions :column="2" border class="readonly-info">
            <el-descriptions-item label="用户名">{{ authStore.userInfo?.username }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ authStore.userInfo?.role }}</el-descriptions-item>
          </el-descriptions>

          <el-divider />

          <el-form
            ref="profileFormRef"
            :model="profileForm"
            label-width="100px"
            class="profile-form"
          >
            <el-form-item label="真实姓名">
              <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="手机号码">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号码" />
            </el-form-item>
            <el-form-item label="电子邮箱">
              <el-input v-model="profileForm.email" placeholder="请输入电子邮箱" />
            </el-form-item>
            <el-form-item label="头像URL">
              <el-input v-model="profileForm.avatar" placeholder="请输入头像URL" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="profileSaving" @click="saveProfile">
                保存修改
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- 修改密码 -->
      <el-tab-pane label="修改密码">
        <el-card class="settings-card">
          <template #header>
            <span class="card-header-text">修改密码</span>
          </template>

          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="100px"
            class="password-form"
          >
            <el-form-item label="原密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                placeholder="请输入原密码"
                show-password
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="请输入新密码（至少6位）"
                show-password
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="passwordSaving" @click="savePassword">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.settings-page {
  max-width: 600px;
}
.page-title {
  margin: 0 0 16px 0;
  font-size: 20px;
  color: #303133;
}
.settings-card {
  margin-bottom: 16px;
}
.card-header-text {
  font-weight: 600;
}
.readonly-info {
  margin-bottom: 8px;
}
.profile-form,
.password-form {
  margin-top: 16px;
}
</style>
