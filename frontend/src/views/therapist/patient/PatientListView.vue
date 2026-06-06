<template>
  <div class="patient-list-page">
    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="filter" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="filter.keyword"
            placeholder="姓名/住院号/床号"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="filter.status"
            placeholder="全部状态"
            clearable
            style="width: 140px"
            @change="handleSearch"
          >
            <el-option label="全部" value="" />
            <el-option label="在院" value="IN_HOSPITAL" />
            <el-option label="已出院" value="DISCHARGED" />
          </el-select>
        </el-form-item>
        <el-form-item label="查看范围">
          <el-radio-group v-model="filter.viewScope" @change="handleSearch">
            <el-radio-button value="mine">我的患者</el-radio-button>
            <el-radio-button value="group">本组患者</el-radio-button>
            <el-radio-button value="all">全部患者</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格卡片 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>患者列表</span>
          <div style="display: flex; gap: 8px;">
            <el-upload
              :before-upload="() => false"
              :http-request="handlePatientImport"
              :show-file-list="false"
              accept=".xlsx,.xls"
            >
              <el-button>
                <el-icon><Upload /></el-icon>
                导入患者
              </el-button>
            </el-upload>
            <el-button @click="handleExport">
              <el-icon><Download /></el-icon>
              导出患者
            </el-button>
            <el-button type="primary" @click="openCreateDialog">
              <el-icon><Plus /></el-icon>
              新增患者
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column prop="name" label="姓名" min-width="80" />
        <el-table-column label="性别" width="60" align="center">
          <template #default="{ row }">
            {{ genderLabel(row.gender) }}
          </template>
        </el-table-column>
        <el-table-column label="年龄" width="60" align="center">
          <template #default="{ row }">
            {{ row.age ?? '--' }}
          </template>
        </el-table-column>
        <el-table-column prop="inpatientNo" label="住院号" min-width="120" />
        <el-table-column prop="bedNo" label="床号" width="80" align="center" />
        <el-table-column prop="diagnosis" label="诊断" min-width="160" show-overflow-tooltip />
        <el-table-column label="入院日期" width="110" align="center">
          <template #default="{ row }">
            {{ row.admissionDate ?? '--' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" disable-transitions>
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="goDetail(row.id)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无患者数据" />

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSearch"
          @current-change="handleSearch"
        />
      </div>
    </el-card>

    <!-- 新增患者弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="新增患者"
      width="620px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="100px"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="0">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="年龄" prop="age">
              <el-input-number v-model="form.age" :min="0" :max="200" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="住院号" prop="inpatientNo">
              <el-input v-model="form.inpatientNo" placeholder="请输入住院号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="床号" prop="bedNo">
              <el-input v-model="form.bedNo" placeholder="请输入床号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入院日期" prop="admissionDate">
              <el-date-picker
                v-model="form.admissionDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="诊断" prop="diagnosis">
          <el-input
            v-model="form.diagnosis"
            type="textarea"
            :rows="2"
            placeholder="请输入诊断信息"
          />
        </el-form-item>
        <el-form-item label="过敏史">
          <el-input
            v-model="form.allergyHistory"
            type="textarea"
            :rows="2"
            placeholder="请输入过敏史"
          />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.contactPhone" placeholder="联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急联系人">
              <el-input v-model="form.emergencyContact" placeholder="紧急联系人姓名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="紧急电话">
              <el-input v-model="form.emergencyPhone" placeholder="紧急联系人电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">
          确认新增
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Search, Plus, Download, Upload } from '@element-plus/icons-vue'
import axios from 'axios'
import { get, post } from '@/api'
import type { Patient, PageResult } from '@/api/types'

const router = useRouter()

// ---------- 筛选 ----------
const filter = reactive({
  keyword: '',
  status: '',
  viewScope: 'mine',
})

function handleSearch() {
  pagination.page = 1
  fetchList()
}

function handleReset() {
  filter.keyword = ''
  filter.status = ''
  filter.viewScope = 'mine'
  pagination.page = 1
  fetchList()
}

// ---------- 表格 ----------
const loading = ref(false)
const tableData = ref<Patient[]>([])

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

async function fetchList() {
  loading.value = true
  try {
    const res = await get<PageResult<Patient>>('/patients', {
      keyword: filter.keyword || undefined,
      status: filter.status || undefined,
      viewScope: filter.viewScope,
      page: pagination.page,
      size: pagination.size,
    })
    const result = res.data ?? (res as any)
    tableData.value = result.records ?? []
    pagination.total = result.total ?? 0
  } finally {
    loading.value = false
  }
}

fetchList()

// ---------- 工具函数 ----------
function genderLabel(g: number) {
  return g === 1 ? '男' : '女'
}

function statusLabel(s: string) {
  return s === 'IN_HOSPITAL' ? '在院' : '已出院'
}

function statusTagType(s: string) {
  return s === 'IN_HOSPITAL' ? 'success' : 'info'
}

function goDetail(id: number | undefined) {
  if (id) {
    router.push(`/patients/${id}`)
  }
}

async function exportExcel(url: string, params: any, filename: string) {
  const res = await axios.get(url, { params, responseType: 'blob' })
  const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  link.click()
  URL.revokeObjectURL(link.href)
}

async function handlePatientImport(options: any) {
  const formData = new FormData()
  formData.append('file', options.file)
  try {
    const token = localStorage.getItem('token')
    const res = await axios.post('/api/v1/patients/import', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        Authorization: token ? `Bearer ${token}` : '',
      },
    })
    if (res.data?.code === 200) {
      const result = res.data.data
      ElMessage.success(`导入完成：共 ${result.total} 条，成功 ${result.success} 条，失败 ${result.fail} 条`)
      fetchList()
    }
  } catch {
    ElMessage.error('导入失败')
  }
}

async function handleExport() {
  await exportExcel(
    '/api/v1/patients/export',
    {
      keyword: filter.keyword || undefined,
      status: filter.status || undefined,
      viewScope: filter.viewScope,
    },
    '患者列表.xlsx'
  )
}

// ---------- 新增弹窗 ----------
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<Omit<Patient, 'id' | 'status' | 'dischargeDate' | 'attendingTherapistId' | 'attendingDoctorId'>>({
  name: '',
  gender: 1,
  age: null,
  inpatientNo: '',
  bedNo: '',
  admissionDate: '',
  diagnosis: '',
  allergyHistory: '',
  contactPhone: '',
  emergencyContact: '',
  emergencyPhone: '',
  remark: '',
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  inpatientNo: [{ required: true, message: '请输入住院号', trigger: 'blur' }],
  bedNo: [{ required: true, message: '请输入床号', trigger: 'blur' }],
  diagnosis: [{ required: true, message: '请输入诊断', trigger: 'blur' }],
}

function resetForm() {
  Object.assign(form, {
    name: '',
    gender: 1,
    age: null,
    inpatientNo: '',
    bedNo: '',
    admissionDate: '',
    diagnosis: '',
    allergyHistory: '',
    contactPhone: '',
    emergencyContact: '',
    emergencyPhone: '',
    remark: '',
  })
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

async function handleCreate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await post('/patients', form)
    ElMessage.success('新增患者成功')
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.patient-list-page {
  padding: 16px;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-card .el-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.filter-card .el-form-item {
  margin-bottom: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-card {
  flex: 1;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
