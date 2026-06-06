<template>
  <div class="user-manage">
    <!-- Search Bar -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="用户名/姓名/手机号"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.role" placeholder="全部角色" clearable style="width: 150px">
            <el-option label="治疗师" value="THERAPIST" />
            <el-option label="护士" value="NURSE" />
            <el-option label="医生" value="DOCTOR" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Table -->
    <el-card class="table-card" shadow="never">
      <div class="table-header">
        <span class="table-title">用户列表</span>
        <el-button type="primary" @click="handleAdd">新增用户</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small">
              {{ roleLabel(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="(val: boolean) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="handleResetPwd(row)">重置密码</el-button>
            <el-popconfirm
              title="确定要删除该用户吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无用户数据" />

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialog.visible"
      :title="dialog.isEdit ? '编辑用户' : '新增用户'"
      width="520px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="dialog.isEdit" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="!dialog.isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="治疗师" value="THERAPIST" />
            <el-option label="护士" value="NURSE" />
            <el-option label="医生" value="DOCTOR" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属小组" prop="groupId">
          <el-select v-model="form.groupId" placeholder="请选择小组" clearable style="width: 100%">
            <el-option
              v-for="g in groupOptions"
              :key="g.id"
              :label="g.groupName"
              :value="g.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.loading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Reset Password Dialog -->
    <el-dialog v-model="pwdDialog.visible" title="重置密码" width="420px" :close-on-click-modal="false">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdFormRules" label-width="80px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="pwdForm.password" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="pwdDialog.loading" @click="handleResetPwdSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { get, post, put, del } from '@/api'
import type { User, PageResult, TherapyGroup } from '@/api/types'

// ── Search ────────────────────────────────────────────────
const searchForm = reactive({ keyword: '', role: '' })

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.role = ''
  pagination.page = 1
  fetchData()
}

// ── Table ─────────────────────────────────────────────────
const tableData = ref<User[]>([])
const loading = ref(false)
const pagination = reactive({ page: 1, size: 10, total: 0 })

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: pagination.page, size: pagination.size }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.role) params.role = searchForm.role
    const res = await get<any>('/admin/users', params)
    const data = res.data ?? res
    tableData.value = data.records ?? []
    pagination.total = data.total ?? 0
  } finally {
    loading.value = false
  }
}

async function handleStatusChange(row: User, val: boolean) {
  try {
    await put(`/admin/users/${row.id}`, { status: val ? 1 : 0 })
    row.status = val ? 1 : 0
    ElMessage.success('状态已更新')
  } catch {
    // interceptor handles error message
  }
}

function roleLabel(role: string): string {
  const map: Record<string, string> = { THERAPIST: '治疗师', NURSE: '护士', DOCTOR: '医生', ADMIN: '管理员' }
  return map[role] || role
}

function roleTagType(role: string): 'success' | 'warning' | 'info' | 'danger' | '' {
  const map: Record<string, 'success' | 'warning' | 'info' | 'danger' | ''> = {
    THERAPIST: 'success',
    NURSE: 'info',
    DOCTOR: 'warning',
    ADMIN: 'danger',
  }
  return map[role] || ''
}

// ── Group options ─────────────────────────────────────────
const groupOptions = ref<TherapyGroup[]>([])

async function fetchGroups() {
  try {
    const res = await get<PageResult<TherapyGroup>>('/admin/therapy-groups', { page: 1, size: 100 })
    groupOptions.value = res.records
  } catch {
    // ignore
  }
}

// ── Add / Edit Dialog ─────────────────────────────────────
const formRef = ref<FormInstance>()
const dialog = reactive({ visible: false, isEdit: false, loading: false, editId: null as number | null })

const form = reactive<User & { password?: string }>({
  username: '',
  password: '',
  realName: '',
  role: '',
  groupId: undefined,
  phone: '',
  email: '',
  status: 1,
})

const formRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }],
}

function handleAdd() {
  dialog.visible = true
  dialog.isEdit = false
  dialog.editId = null
}

function handleEdit(row: User) {
  dialog.visible = true
  dialog.isEdit = true
  dialog.editId = row.id!
  Object.assign(form, {
    username: row.username,
    password: '',
    realName: row.realName,
    role: row.role,
    groupId: row.groupId,
    phone: row.phone,
    email: row.email,
  })
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  dialog.loading = true
  try {
    const payload: Record<string, any> = {
      realName: form.realName,
      role: form.role,
      groupId: form.groupId,
      phone: form.phone,
      email: form.email,
      status: form.status,
    }
    if (!dialog.isEdit) {
      payload.username = form.username
      payload.password = form.password
      await post('/admin/users', payload)
      ElMessage.success('创建成功')
    } else {
      await put(`/admin/users/${dialog.editId}`, payload)
      ElMessage.success('更新成功')
    }
    dialog.visible = false
    fetchData()
  } finally {
    dialog.loading = false
  }
}

function resetForm() {
  formRef.value?.resetFields()
  form.username = ''
  form.password = ''
  form.realName = ''
  form.role = ''
  form.groupId = undefined
  form.phone = ''
  form.email = ''
}

// ── Delete ────────────────────────────────────────────────
async function handleDelete(row: User) {
  try {
    await del(`/admin/users/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // interceptor handles error
  }
}

// ── Reset Password Dialog ─────────────────────────────────
const pwdFormRef = ref<FormInstance>()
const pwdDialog = reactive({ visible: false, loading: false, targetId: null as number | null })
const pwdForm = reactive({ password: '' })
const pwdFormRules: FormRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不少于6位', trigger: 'blur' },
  ],
}

function handleResetPwd(row: User) {
  pwdDialog.targetId = row.id!
  pwdForm.password = ''
  pwdDialog.visible = true
}

async function handleResetPwdSubmit() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return

  pwdDialog.loading = true
  try {
    await put(`/admin/users/${pwdDialog.targetId}/reset-password`)
    ElMessage.success('密码已重置为默认密码 rehab123')
    pwdDialog.visible = false
  } finally {
    pwdDialog.loading = false
  }
}

// ── Init ──────────────────────────────────────────────────
onMounted(() => {
  fetchData()
  fetchGroups()
})
</script>

<style scoped>
.user-manage {
  padding: 16px;
}

.search-card {
  margin-bottom: 16px;
}

.search-form {
  margin-bottom: 0;
}

.table-card {
  min-height: 400px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.table-title {
  font-size: 16px;
  font-weight: 600;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
