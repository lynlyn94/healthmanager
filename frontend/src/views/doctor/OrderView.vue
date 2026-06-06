<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { get, post, put } from '@/api'
import type { MedicalOrder, Patient, PageResult } from '@/api/types'

// ---------- filters ----------
const searchName = ref('')
const filterStatus = ref('')
const statusOptions = [
  { label: '全部', value: '' },
  { label: '草稿', value: 'DRAFT' },
  { label: '待审核', value: 'PENDING_REVIEW' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已退回', value: 'REJECTED' },
  { label: '已作废', value: 'CANCELLED' },
]

// ---------- table ----------
const tableData = ref<MedicalOrder[]>([])
const tableLoading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

// ---------- dialog ----------
const dialogVisible = ref(false)
const dialogTitle = ref('新建医嘱')
const isEdit = ref(false)
const editId = ref<number | undefined>()
const formLoading = ref(false)
const formRef = ref()

const form = reactive<MedicalOrder & { planId?: number }>({
  patientId: 0,
  planId: undefined,
  orderType: '',
  treatmentItem: '',
  frequency: '',
  dailyCount: 1,
  periodStart: '',
  periodEnd: '',
  note: '',
  status: 'DRAFT',
})

const formRules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }],
  orderType: [{ required: true, message: '请选择医嘱类型', trigger: 'change' }],
  treatmentItem: [{ required: true, message: '请输入治疗项目', trigger: 'blur' }],
  frequency: [{ required: true, message: '请输入频次', trigger: 'blur' }],
  dailyCount: [{ required: true, message: '请输入每日次数', trigger: 'blur' }],
  periodStart: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  periodEnd: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
}

// ---------- patient selector options ----------
const patientOptions = ref<{ label: string; value: number }[]>([])
const patientLoading = ref(false)

const orderTypeOptions = [
  { label: '长期医嘱', value: 'LONG_TERM' },
  { label: '临时医嘱', value: 'TEMPORARY' },
  { label: '康复医嘱', value: 'REHAB' },
]

const treatmentItemOptions = ref<string[]>([])

async function fetchTreatmentItems() {
  try {
    const res = await get<any>('/tasks/treatment-items')
    treatmentItemOptions.value = (res as any).data ?? res ?? []
  } catch { /* ignore */ }
}

const frequencyOptions = [
  { label: '每日1次', value: 'QD' },
  { label: '每日2次', value: 'BID' },
  { label: '每日3次', value: 'TID' },
  { label: '每日4次', value: 'QID' },
  { label: '每周1次', value: 'QW' },
  { label: '每周2次', value: 'BIW' },
  { label: '每周3次', value: 'TIW' },
  { label: '每周5次', value: 'FIVE_W' },
]

// ---------- status display ----------
const statusMap: Record<string, { text: string; type: '' | 'info' | 'success' | 'warning' | 'danger' }> = {
  DRAFT: { text: '草稿', type: 'info' },
  PENDING_REVIEW: { text: '待审核', type: 'warning' },
  APPROVED: { text: '已通过', type: 'success' },
  REJECTED: { text: '已退回', type: 'danger' },
  CANCELLED: { text: '已作废', type: 'info' },
}

function formatPeriod(row: MedicalOrder) {
  return `${row.periodStart || ''} ~ ${row.periodEnd || ''}`
}

// ---------- API calls ----------
async function fetchOrders() {
  tableLoading.value = true
  try {
    const params: any = { page: page.value, size: pageSize.value }
    if (filterStatus.value) params.status = filterStatus.value

    const res = await get<any>('/orders', params)
    const result = res.data ?? res
    let records = result.records || (Array.isArray(result) ? result : [])
    total.value = result.total || records.length

    // Client-side patient name filter (backend doesn't support patientName yet)
    if (searchName.value) {
      const kw = searchName.value.toLowerCase()
      records = records.filter((o: MedicalOrder) =>
        (o.patientName || '').toLowerCase().includes(kw)
      )
    }
    tableData.value = records
  } catch {
    // handled by interceptor
  } finally {
    tableLoading.value = false
  }
}

async function searchPatients(keyword: string) {
  patientLoading.value = true
  try {
    const res = await get<{ data: PageResult<Patient> }>('/patients', {
      keyword,
      page: 1,
      size: 50,
      viewScope: 'all',
    })
    const records = res.data?.records || []
    patientOptions.value = records.map((p) => ({
      label: `${p.name} - ${p.inpatientNo || ''} ${p.bedNo ? '(' + p.bedNo + ')' : ''}`,
      value: p.id!,
    }))
  } catch {
    // handled by interceptor
  } finally {
    patientLoading.value = false
  }
}

// ---------- dialog actions ----------
function openCreateDialog() {
  isEdit.value = false
  editId.value = undefined
  dialogTitle.value = '新建医嘱'
  Object.assign(form, {
    patientId: 0,
    planId: undefined,
    orderType: '',
    treatmentItem: '',
    frequency: '',
    dailyCount: 1,
    periodStart: '',
    periodEnd: '',
    note: '',
    status: 'DRAFT',
  })
  dialogVisible.value = true
}

function openEditDialog(row: MedicalOrder) {
  isEdit.value = true
  editId.value = row.id
  dialogTitle.value = '编辑医嘱'
  Object.assign(form, {
    patientId: row.patientId,
    orderType: row.orderType,
    treatmentItem: row.treatmentItem,
    frequency: row.frequency,
    dailyCount: row.dailyCount,
    periodStart: row.periodStart,
    periodEnd: row.periodEnd,
    note: row.note,
    status: row.status,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  formLoading.value = true
  try {
    if (isEdit.value) {
      await put(`/orders/${editId.value}`, form)
      ElMessage.success('医嘱更新成功')
    } else {
      const payload: any = { ...form }
      if (!payload.planId) delete payload.planId
      await post('/orders', payload)
      ElMessage.success('医嘱创建成功')
    }
    dialogVisible.value = false
    fetchOrders()
    fetchApprovedPlans()  // refresh approved plans list
  } catch {
    // handled by interceptor
  } finally {
    formLoading.value = false
  }
}

async function handleApprove(row: MedicalOrder) {
  try {
    const { value } = await ElMessageBox.prompt('审核意见（选填）', '审核通过', {
      inputType: 'textarea',
      confirmButtonText: '确认通过',
      cancelButtonText: '取消',
      inputPlaceholder: '可选填写审核意见',
    })
    await put(`/orders/${row.id}/approve`, { comment: value || '' })
    ElMessage.success('医嘱已通过')
    fetchOrders()
  } catch {
    // cancelled or error
  }
}

async function handleReject(row: MedicalOrder) {
  try {
    const { value } = await ElMessageBox.prompt('请输入退回原因', '审核退回', {
      inputType: 'textarea',
      confirmButtonText: '确认退回',
      cancelButtonText: '取消',
      inputValidator: (val: string) => !!val || '请填写退回原因',
      inputErrorMessage: '退回原因不能为空',
    })
    // value is guaranteed to be non-empty at this point (inputValidator ensures it)
    await put(`/orders/${row.id}/reject`, { reason: value || '' })
    ElMessage.success('医嘱已退回')
    fetchOrders()
  } catch {
    // cancelled
  }
}

async function handleApproveDirect(row: MedicalOrder) {
  try {
    await put(`/orders/${row.id}/approve-direct`)
    ElMessage.success('审核已通过')
    fetchOrders()
  } catch { /* handled */ }
}

async function handleRevokeApproval(row: MedicalOrder) {
  try {
    await ElMessageBox.confirm(
      `确认撤销医嘱「${row.treatmentItem}」的审核？\n撤销后治疗师将无法排程。`,
      '确认撤销审核',
      { confirmButtonText: '确认撤销', cancelButtonText: '取消', type: 'warning' },
    )
  } catch { return }
  try {
    await put(`/orders/${row.id}/revoke-approval`)
    ElMessage.success('审核已撤销')
    fetchOrders()
  } catch { /* interceptor handles */ }
}

async function handleCancel(row: MedicalOrder) {
  try {
    await ElMessageBox.confirm('确认作废该医嘱？', '确认作废', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
    })
    await put(`/orders/${row.id}/cancel`)
    ElMessage.success('医嘱已作废')
    fetchOrders()
  } catch {
    // cancelled
  }
}

// ---------- pagination ----------
function handleSizeChange(size: number) {
  pageSize.value = size
  page.value = 1
  fetchOrders()
}

function handlePageChange(p: number) {
  page.value = p
  fetchOrders()
}

// ---------- lifecycle ----------
// ---- Approved plans (待开医嘱) ----
interface TreatmentPlan {
  id?: number; patientId: number; planName: string; treatmentItems: string
  frequency: string; dailyCount: number; periodStart: string; periodEnd: string
  status: string; submitTime?: string; patientName?: string; therapistName?: string
}
const approvedPlans = ref<TreatmentPlan[]>([])

async function fetchApprovedPlans() {
  try {
    const res = await get<any>('/treatment-plans/approved-by-me')
    approvedPlans.value = (res as any).data ?? res ?? []
  } catch { /* ignore */ }
}

function formatPlanItems(val: string): string {
  if (!val) return '-'
  try { const arr = JSON.parse(val); if (Array.isArray(arr)) return arr.join('、') } catch {}
  return val
}

function openOrderFromPlan(plan: TreatmentPlan) {
  Object.assign(form, {
    patientId: plan.patientId,
    planId: plan.id,
    orderType: 'REHAB',
    treatmentItem: formatPlanItems(plan.treatmentItems),
    frequency: plan.frequency,
    dailyCount: plan.dailyCount,
    periodStart: plan.periodStart,
    periodEnd: plan.periodEnd,
    note: '',
    status: 'DRAFT',
  })
  isEdit.value = false
  editId.value = undefined
  dialogTitle.value = '开具医嘱（来自方案: ' + plan.planName + '）'
  dialogVisible.value = true
}

onMounted(() => {
  fetchOrders()
  searchPatients('')
  fetchTreatmentItems()
  fetchApprovedPlans()
})
</script>

<template>
  <div class="order-page">
    <!-- header -->
    <div class="page-header">
      <h2 class="page-title">医嘱管理</h2>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon style="margin-right: 4px"><Plus /></el-icon>
        新建医嘱
      </el-button>
    </div>

    <!-- filters -->
    <el-card shadow="hover" class="filter-card">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="患者姓名">
          <el-input
            v-model="searchName"
            placeholder="输入患者姓名搜索"
            clearable
            style="width: 200px"
            @keyup.enter="fetchOrders"
            @clear="fetchOrders"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="filterStatus"
            placeholder="选择状态"
            clearable
            style="width: 140px"
            @change="fetchOrders"
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
          <el-button type="default" @click="fetchOrders">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 待开医嘱 — approved treatment plans needing orders -->
    <el-card v-if="approvedPlans.length" shadow="hover" class="filter-card">
      <template #header>
        <div style="display: flex; align-items: center; gap: 8px;">
          <el-icon color="#e6a23c"><WarningFilled /></el-icon>
          <span style="font-weight: 600;">待开医嘱</span>
          <el-tag type="warning" size="small">{{ approvedPlans.length }} 个方案已审核通过</el-tag>
        </div>
      </template>
      <el-table :data="approvedPlans" stripe size="small" style="width: 100%">
        <el-table-column prop="planName" label="方案名称" min-width="140" />
        <el-table-column label="治疗项目" min-width="180">
          <template #default="{ row }">{{ formatPlanItems(row.treatmentItems) }}</template>
        </el-table-column>
        <el-table-column label="频次/日次" width="110">
          <template #default="{ row }">{{ row.frequency }} / {{ row.dailyCount }}次/日</template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="160" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openOrderFromPlan(row)">开具医嘱</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- table -->
    <el-card shadow="hover" class="table-card">
      <el-table :data="tableData" v-loading="tableLoading" stripe style="width: 100%">
        <el-table-column prop="patientName" label="患者" min-width="80" />
        <el-table-column prop="therapistName" label="责任治疗师" min-width="100" />
        <el-table-column prop="treatmentItem" label="治疗项目" min-width="140" />
        <el-table-column prop="frequency" label="频次" width="100" />
        <el-table-column label="周期" min-width="180">
          <template #default="{ row }">
            {{ formatPeriod(row) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag
              :type="statusMap[row.status]?.type || 'info'"
              size="small"
              disable-transitions
            >
              {{ statusMap[row.status]?.text || row.status }}
            </el-tag>
            <el-tag v-if="row.revokeCount > 0" type="danger" size="small" style="margin-left: 4px">
              撤销{{ row.revokeCount }}次
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING_REVIEW'"
              type="success"
              size="small"
              link
              @click="handleApprove(row)"
            >
              审核通过
            </el-button>
            <el-button
              v-if="row.status === 'PENDING_REVIEW'"
              type="warning"
              size="small"
              link
              @click="handleReject(row)"
            >
              审核退回
            </el-button>
            <el-button
              v-if="row.status !== 'APPROVED'"
              type="danger"
              size="small"
              link
              @click="handleCancel(row)"
            >
              作废
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              type="success"
              size="small"
              link
              @click="handleApproveDirect(row)"
            >
              审核通过
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              type="primary"
              size="small"
              link
              @click="openEditDialog(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'APPROVED'"
              type="warning"
              size="small"
              link
              @click="handleRevokeApproval(row)"
            >
              撤销审核
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!tableLoading && tableData.length === 0" description="暂无医嘱数据" />

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

    <!-- create / edit dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      :close-on-click-modal="false"
      width="600px"
      @open="() => searchPatients('')"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="100px"
        :disabled="formLoading"
      >
        <el-form-item label="患者" prop="patientId">
          <el-select
            v-model="form.patientId"
            placeholder="搜索并选择患者"
            filterable
            remote
            :remote-method="searchPatients"
            :loading="patientLoading"
            style="width: 100%"
          >
            <el-option
              v-for="item in patientOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="医嘱类型" prop="orderType">
          <el-select v-model="form.orderType" placeholder="选择医嘱类型" style="width: 100%">
            <el-option
              v-for="opt in orderTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="治疗项目" prop="treatmentItem">
          <el-select v-model="form.treatmentItem" placeholder="请选择或输入治疗项目" filterable allow-create style="width: 100%">
            <el-option v-for="item in treatmentItemOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="频次" prop="frequency">
          <el-select v-model="form.frequency" placeholder="选择频次" style="width: 100%">
            <el-option
              v-for="opt in frequencyOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="每日次数" prop="dailyCount">
          <el-input-number v-model="form.dailyCount" :min="1" :max="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="开始日期" prop="periodStart">
          <el-date-picker
            v-model="form.periodStart"
            type="date"
            placeholder="选择开始日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束日期" prop="periodEnd">
          <el-date-picker
            v-model="form.periodEnd"
            type="date"
            placeholder="选择结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注" prop="note">
          <el-input v-model="form.note" type="textarea" :rows="3" placeholder="可选填写备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="handleSubmit">
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { Plus, WarningFilled } from '@element-plus/icons-vue'

export default {
  components: { Plus, WarningFilled },
}
</script>

<style scoped>
.order-page {
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
