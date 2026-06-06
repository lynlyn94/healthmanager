<template>
  <div class="group-manage">
    <el-card shadow="never">
      <div class="table-header">
        <span class="table-title">治疗小组管理</span>
        <el-button type="primary" @click="handleAdd">新增小组</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="groupName" label="小组名称" width="160" />
        <el-table-column label="组长" width="120">
          <template #default="{ row }">
            {{ row.leaderName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm
              title="确定要删除该小组吗？"
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

      <el-empty v-if="!loading && tableData.length === 0" description="暂无小组数据" />

    </el-card>

    <!-- Add / Edit Dialog -->
    <el-dialog
      v-model="dialog.visible"
      :title="dialog.isEdit ? '编辑小组' : '新增小组'"
      width="480px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="小组名称" prop="groupName">
          <el-input v-model="form.groupName" placeholder="请输入小组名称" />
        </el-form-item>
        <el-form-item label="组长" prop="leaderId">
          <el-select v-model="form.leaderId" placeholder="请选择组长" clearable style="width: 100%" filterable>
            <el-option
              v-for="u in userOptions"
              :key="u.id"
              :label="`${u.realName} (${u.username})`"
              :value="u.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入小组描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.loading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { get, post, put, del } from '@/api'
import type { TherapyGroup, User, PageResult } from '@/api/types'

// ── Table ─────────────────────────────────────────────────
const tableData = ref<(TherapyGroup & { leaderName?: string })[]>([])
const loading = ref(false)
const userOptions = ref<User[]>([])

async function fetchData() {
  loading.value = true
  try {
    const res = await get<any>('/admin/therapy-groups')
    const data = res.data ?? res
    tableData.value = Array.isArray(data) ? data : (data.records ?? [])
    // resolve leader names
    for (const g of tableData.value) {
      if (g.leaderId && userOptions.value.length) {
        const u = userOptions.value.find((u) => u.id === g.leaderId)
        g.leaderName = u?.realName
      }
    }
  } finally {
    loading.value = false
  }
}

async function fetchUsers() {
  try {
    const res = await get<any>('/admin/users', { page: 1, size: 500 })
    const data = res.data ?? res
    userOptions.value = data.records ?? (Array.isArray(data) ? data : [])
  } catch {
    // ignore
  }
}

// ── Add / Edit Dialog ─────────────────────────────────────
const formRef = ref<FormInstance>()
const dialog = reactive({ visible: false, isEdit: false, loading: false, editId: null as number | null })

const form = reactive<TherapyGroup>({
  groupName: '',
  leaderId: undefined,
  description: '',
})

const formRules: FormRules = {
  groupName: [{ required: true, message: '请输入小组名称', trigger: 'blur' }],
}

function handleAdd() {
  dialog.visible = true
  dialog.isEdit = false
  dialog.editId = null
}

function handleEdit(row: TherapyGroup) {
  dialog.visible = true
  dialog.isEdit = true
  dialog.editId = row.id!
  form.groupName = row.groupName
  form.leaderId = row.leaderId
  form.description = row.description
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  dialog.loading = true
  try {
    const payload = {
      groupName: form.groupName,
      leaderId: form.leaderId,
      description: form.description,
    }
    if (!dialog.isEdit) {
      await post('/admin/therapy-groups', payload)
      ElMessage.success('创建成功')
    } else {
      await put(`/admin/therapy-groups/${dialog.editId}`, payload)
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
  form.groupName = ''
  form.leaderId = undefined
  form.description = ''
}

// ── Delete ────────────────────────────────────────────────
async function handleDelete(row: TherapyGroup) {
  try {
    await del(`/admin/therapy-groups/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // interceptor handles error
  }
}

// ── Init ──────────────────────────────────────────────────
onMounted(async () => {
  await fetchUsers()
  fetchData()
})
</script>

<style scoped>
.group-manage {
  padding: 16px;
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
</style>
