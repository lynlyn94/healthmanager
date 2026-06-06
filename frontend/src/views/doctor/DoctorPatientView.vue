<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { get } from '@/api'
import type { Patient, PageResult } from '@/api/types'

const router = useRouter()

// ---------- search ----------
const searchKeyword = ref('')
const filterStatus = ref('')

const statusOptions = [
  { label: '全部', value: '' },
  { label: '在院', value: 'INPATIENT' },
  { label: '出院', value: 'DISCHARGED' },
  { label: '归档', value: 'ARCHIVED' },
]

// ---------- table ----------
const tableData = ref<Patient[]>([])
const tableLoading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

// ---------- gender map ----------
const genderMap: Record<number, string> = {
  0: '女',
  1: '男',
}

const patientStatusMap: Record<string, { text: string; type: 'success' | 'warning' | 'info' | 'danger' }> = {
  INPATIENT: { text: '在院', type: 'success' },
  DISCHARGED: { text: '出院', type: 'info' },
  ARCHIVED: { text: '归档', type: 'warning' },
}

// ---------- API calls ----------
async function fetchPatients() {
  tableLoading.value = true
  try {
    const params: any = {
      page: page.value,
      size: pageSize.value,
      viewScope: 'all',
    }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterStatus.value) params.status = filterStatus.value

    const res = await get<any>('/patients', params)
    const result = res.data ?? res
    tableData.value = result.records || (Array.isArray(result) ? result : [])
    total.value = result.total || 0
  } catch {
    // handled by interceptor
  } finally {
    tableLoading.value = false
  }
}

function handleViewDetail(row: Patient) {
  router.push(`/patients/${row.id}`)
}

// ---------- pagination ----------
function handleSizeChange(size: number) {
  pageSize.value = size
  page.value = 1
  fetchPatients()
}

function handlePageChange(p: number) {
  page.value = p
  fetchPatients()
}

function handleSearch() {
  page.value = 1
  fetchPatients()
}

// ---------- lifecycle ----------
onMounted(() => {
  fetchPatients()
})
</script>

<template>
  <div class="patient-page">
    <!-- header -->
    <div class="page-header">
      <h2 class="page-title">患者列表</h2>
    </div>

    <!-- search & filters -->
    <el-card shadow="hover" class="filter-card">
      <el-form :inline="true" class="filter-form" @keyup.enter="handleSearch">
        <el-form-item label="关键词">
          <el-input
            v-model="searchKeyword"
            placeholder="姓名 / 住院号 / 床位号"
            clearable
            style="width: 240px"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="filterStatus"
            placeholder="全部状态"
            clearable
            style="width: 130px"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in statusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- patient table -->
    <el-card shadow="hover" class="table-card">
      <el-table
        :data="tableData"
        v-loading="tableLoading"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column label="性别" width="70">
          <template #default="{ row }">
            {{ genderMap[row.gender] || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="70" />
        <el-table-column prop="inpatientNo" label="住院号" width="140" />
        <el-table-column prop="bedNo" label="床位" width="90" />
        <el-table-column prop="diagnosis" label="诊断" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag
              :type="patientStatusMap[row.status]?.type || 'info'"
              size="small"
              disable-transitions
            >
              {{ patientStatusMap[row.status]?.text || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click.stop="handleViewDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>

      </el-table>

      <!-- pagination -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.patient-page {
  max-width: 1200px;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.page-title {
  margin: 0;
  font-size: 20px;
  color: #333;
}
.filter-card {
  margin-bottom: 20px;
}
.filter-form {
  margin-bottom: 0;
}
.filter-form .el-form-item {
  margin-bottom: 0;
}
.table-card {
  margin-bottom: 20px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
