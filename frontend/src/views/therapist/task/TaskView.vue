<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { get, post } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { Task, Patient } from '@/api/types'

// ============ Calendar state ============
const calendarDate = ref(new Date())
const selectedDate = ref(new Date())
const selectedDateStr = computed(() => formatDateStr(selectedDate.value))
const taskDateSet = ref<Set<string>>(new Set())
const calendarLoading = ref(false)

// ============ Task list state ============
const tasks = ref<Task[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

// ============ Filters ============
const filters = reactive({
  patientName: '',
  status: '',
  treatmentItem: '',
})

// ============ Patient dropdown ============
const patients = ref<Patient[]>([])

// ============ Create dialog ============
const showCreateDialog = ref(false)
const createFormRef = ref<FormInstance>()
const creating = ref(false)
const newTaskForm = reactive({
  patientId: null as number | null,
  treatmentItem: '',
  taskDate: '',
  timeSlot: '',
})

const createRules: FormRules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }],
  treatmentItem: [{ required: true, message: '请输入治疗项目', trigger: 'blur' }],
  taskDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  timeSlot: [{ required: true, message: '请选择时间段', trigger: 'change' }],
}

// ============ Revoke dialog ============
const showRevokeDialog = ref(false)
const revokingTask = ref<Task | null>(null)
const revokeReason = ref('')

// ============ Date helpers ============
function formatDateStr(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

// ============ API calls ============
async function fetchTasks() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: page.value,
      size: size.value,
      date: formatDateStr(selectedDate.value),
    }
    if (filters.status) params.status = filters.status

    const res = await get<any>('/tasks', params)
    if (res.data) {
      const records = res.data.records || []
      tasks.value = applyClientFilters(records)
      total.value = res.data.total || 0
    }
  } catch {
    // interceptor handles error messages
  } finally {
    loading.value = false
  }
}

async function fetchMonthTaskDates() {
  calendarLoading.value = true
  try {
    const y = calendarDate.value.getFullYear()
    const m = calendarDate.value.getMonth() + 1
    const res = await get<any>('/tasks/calendar', { year: y, month: m })
    const list = res.data ?? res
    if (Array.isArray(list)) {
      const dates = (list as Task[]).map((t) => t.taskDate)
      taskDateSet.value = new Set(dates)
    }
  } catch {
    // swallow - calendar decoration is best-effort
  } finally {
    calendarLoading.value = false
  }
}

async function fetchPatients() {
  try {
    const res = await get<any>('/patients', { size: 500 })
    if (res.data?.records) {
      patients.value = res.data.records
    }
  } catch {
    // swallow
  }
}

// ============ Calendar handlers ============
function onDateCellClick(data: { day: string; date: Date }) {
  selectedDate.value = data.date
  page.value = 1
  fetchTasks()
}

function onDatePickerChange() {
  calendarDate.value = new Date(selectedDate.value)
  page.value = 1
  fetchTasks()
}

// ============ Filter handlers ============
function handleSearch() {
  page.value = 1
  fetchTasks()
}

function handleReset() {
  filters.patientName = ''
  filters.status = ''
  filters.treatmentItem = ''
  page.value = 1
  fetchTasks()
}

// patientName & treatmentItem filters are client-side only (backend doesn't support them yet)
function applyClientFilters(tasks: Task[]): Task[] {
  let result = tasks
  if (filters.patientName) {
    const kw = filters.patientName.toLowerCase()
    result = result.filter((t) => (t.patientName || '').toLowerCase().includes(kw))
  }
  if (filters.treatmentItem) {
    const kw = filters.treatmentItem.toLowerCase()
    result = result.filter((t) => t.treatmentItem.toLowerCase().includes(kw))
  }
  return result
}

function onPageChange(p: number) {
  page.value = p
  fetchTasks()
}

// ============ Create task ============
function openCreateDialog() {
  newTaskForm.patientId = null
  newTaskForm.treatmentItem = ''
  newTaskForm.taskDate = formatDateStr(selectedDate.value)
  newTaskForm.timeSlot = ''
  showCreateDialog.value = true
}

async function handleCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  creating.value = true
  try {
    await post('/tasks', newTaskForm)
    ElMessage.success('任务创建成功')
    showCreateDialog.value = false
    fetchTasks()
  } catch {
    // interceptor handles
  } finally {
    creating.value = false
  }
}

// ============ Start task ============
async function handleStart(task: Task) {
  try {
    await ElMessageBox.confirm(
      `确认开始为「${task.patientName}」执行「${task.treatmentItem}」？`,
      '确认开始',
      { confirmButtonText: '开始', cancelButtonText: '取消', type: 'info' },
    )
  } catch {
    return // user cancelled
  }
  try {
    await post(`/tasks/${task.id}/start`)
    ElMessage.success('任务已开始')
    fetchTasks()
  } catch {
    // interceptor handles
  }
}

// ============ Verify task ============
async function handleVerify(task: Task) {
  try {
    await ElMessageBox.confirm(
      `确认核销「${task.patientName}」的治疗项目「${task.treatmentItem}」？`,
      '确认核销',
      { confirmButtonText: '核销', cancelButtonText: '取消', type: 'info' },
    )
  } catch {
    return // user cancelled
  }
  try {
    await post(`/tasks/${task.id}/verify`)
    ElMessage.success('治疗已核销')
    fetchTasks()
  } catch {
    // interceptor handles
  }
}

// ============ Revoke task ============
function openRevokeDialog(task: Task) {
  revokingTask.value = task
  revokeReason.value = ''
  showRevokeDialog.value = true
}

async function handleRevoke() {
  if (!revokeReason.value.trim()) {
    ElMessage.warning('请输入撤回原因')
    return
  }
  try {
    await post(`/tasks/${revokingTask.value!.id}/revoke`, { reason: revokeReason.value })
    ElMessage.success('任务已撤回')
    showRevokeDialog.value = false
    fetchTasks()
  } catch {
    // interceptor handles
  }
}

// ============ Status helpers ============
const statusConfig: Record<string, { type: 'info' | 'success' | 'danger' | ''; label: string; color?: string }> = {
  PENDING: { type: 'info', label: '待执行' },
  IN_PROGRESS: { type: '', label: '进行中', color: '#409eff' },
  VERIFIED: { type: 'success', label: '已核销' },
  REVOKED: { type: 'danger', label: '已撤回' },
}

function getStatusConfig(status: string) {
  return statusConfig[status] || { type: 'info' as const, label: status }
}

// ============ Watchers ============
watch(calendarDate, () => {
  fetchMonthTaskDates()
})

// ============ Init ============
onMounted(() => {
  fetchTasks()
  fetchPatients()
  fetchMonthTaskDates()
})
</script>

<template>
  <div class="task-page">
    <!-- Page header -->
    <div class="page-header">
      <h2 class="page-title">治疗任务管理</h2>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon style="margin-right: 4px"><Plus /></el-icon>
        新建任务
      </el-button>
    </div>

    <!-- Main content: left calendar + right task list -->
    <div class="page-body">
      <!-- Left panel: Calendar -->
      <div class="left-panel" v-loading="calendarLoading">
        <el-calendar v-model="calendarDate">
          <template #date-cell="{ data }">
            <div
              class="date-cell"
              :class="{
                'has-task': taskDateSet.has(data.day),
                'is-selected': data.day === selectedDateStr,
                'is-today': data.day === todayStr,
              }"
              @click="onDateCellClick(data)"
            >
              {{ data.day.split('-').pop() }}
            </div>
          </template>
        </el-calendar>
        <div class="date-picker-wrap">
          <span class="date-picker-label">选择日期：</span>
          <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择日期"
            size="small"
            style="width: 100%"
            @update:model-value="onDatePickerChange"
          />
        </div>
      </div>

      <!-- Right panel: Task list -->
      <div class="right-panel">
        <!-- Filter bar -->
        <div class="filter-bar">
          <el-input
            v-model="filters.patientName"
            placeholder="患者姓名"
            clearable
            size="default"
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
          <el-select
            v-model="filters.status"
            placeholder="任务状态"
            clearable
            size="default"
            style="width: 140px"
          >
            <el-option label="待执行" value="PENDING" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已核销" value="VERIFIED" />
            <el-option label="已撤回" value="REVOKED" />
          </el-select>
          <el-input
            v-model="filters.treatmentItem"
            placeholder="治疗项目"
            clearable
            size="default"
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">重置</el-button>
        </div>

        <!-- Selected date indicator -->
        <div class="selected-date-badge">
          <el-icon><Clock /></el-icon>
          <span>{{ selectedDateStr }} 的任务</span>
        </div>

        <!-- Task cards -->
        <div v-loading="loading" class="task-list">
          <template v-if="tasks.length > 0">
            <div v-for="task in tasks" :key="task.id" class="task-card">
              <div class="task-card-body">
                <div class="task-info">
                  <div class="task-patient">{{ task.patientName }}</div>
                  <div class="task-treatment">{{ task.treatmentItem }}</div>
                  <div class="task-time">
                    <el-icon><Clock /></el-icon>
                    {{ task.timeSlot }}
                  </div>
                </div>
                <div class="task-meta">
                  <el-tag
                    :type="getStatusConfig(task.status).type"
                    :color="getStatusConfig(task.status).color"
                    size="small"
                    :effect="task.status === 'IN_PROGRESS' ? 'dark' : 'plain'"
                    :class="task.status === 'IN_PROGRESS' ? 'tag-progress' : ''"
                  >
                    {{ getStatusConfig(task.status).label }}
                  </el-tag>
                  <template v-if="task.startTime">
                    <span class="meta-divider">|</span>
                    <span class="meta-text">开始：{{ task.startTime }}</span>
                  </template>
                  <template v-if="task.verificationTime">
                    <span class="meta-divider">|</span>
                    <span class="meta-text">核销：{{ task.verificationTime }}</span>
                  </template>
                </div>
              </div>
              <div class="task-card-actions">
                <el-button
                  v-if="task.status === 'PENDING'"
                  type="primary"
                  size="small"
                  @click="handleStart(task)"
                >
                  开始
                </el-button>
                <el-button
                  v-if="task.status === 'IN_PROGRESS'"
                  type="success"
                  size="small"
                  @click="handleVerify(task)"
                >
                  核销
                </el-button>
                <el-button
                  v-if="task.status === 'VERIFIED'"
                  type="danger"
                  size="small"
                  plain
                  @click="openRevokeDialog(task)"
                >
                  撤回
                </el-button>
              </div>
            </div>
          </template>
          <div v-else-if="!loading" class="empty-state">
            <el-empty description="暂无治疗任务" :image-size="80" />
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="total > 0" class="pagination-wrap">
          <el-pagination
            v-model:current-page="page"
            :page-size="size"
            :total="total"
            layout="total, prev, pager, next"
            background
            @current-change="onPageChange"
          />
        </div>
      </div>
    </div>

    <!-- Create task dialog -->
    <el-dialog v-model="showCreateDialog" title="新建治疗任务" width="520px" destroy-on-close>
      <el-form
        ref="createFormRef"
        :model="newTaskForm"
        :rules="createRules"
        label-width="90px"
        status-icon
      >
        <el-form-item label="患者" prop="patientId">
          <el-select
            v-model="newTaskForm.patientId"
            placeholder="请选择患者"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="p in patients"
              :key="p.id"
              :label="`${p.name} (${p.bedNo})`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="治疗项目" prop="treatmentItem">
          <el-input v-model="newTaskForm.treatmentItem" placeholder="请输入治疗项目名称" />
        </el-form-item>
        <el-form-item label="任务日期" prop="taskDate">
          <el-date-picker
            v-model="newTaskForm.taskDate"
            type="date"
            placeholder="选择任务日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="时间段" prop="timeSlot">
          <el-select v-model="newTaskForm.timeSlot" placeholder="选择时间段" style="width: 100%">
            <el-option label="08:00 - 08:30" value="08:00-08:30" />
            <el-option label="08:30 - 09:00" value="08:30-09:00" />
            <el-option label="09:00 - 09:30" value="09:00-09:30" />
            <el-option label="09:30 - 10:00" value="09:30-10:00" />
            <el-option label="10:00 - 10:30" value="10:00-10:30" />
            <el-option label="10:30 - 11:00" value="10:30-11:00" />
            <el-option label="11:00 - 11:30" value="11:00-11:30" />
            <el-option label="14:00 - 14:30" value="14:00-14:30" />
            <el-option label="14:30 - 15:00" value="14:30-15:00" />
            <el-option label="15:00 - 15:30" value="15:00-15:30" />
            <el-option label="15:30 - 16:00" value="15:30-16:00" />
            <el-option label="16:00 - 16:30" value="16:00-16:30" />
            <el-option label="16:30 - 17:00" value="16:30-17:00" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- Revoke task dialog -->
    <el-dialog v-model="showRevokeDialog" title="撤回任务" width="480px" destroy-on-close>
      <div class="revoke-info">
        <p>
          撤回患者 <strong>{{ revokingTask?.patientName }}</strong> 的治疗项目
          <strong>{{ revokingTask?.treatmentItem }}</strong>
        </p>
      </div>
      <el-input
        v-model="revokeReason"
        type="textarea"
        :rows="3"
        placeholder="请输入撤回原因（必填）"
        maxlength="200"
        show-word-limit
      />
      <template #footer>
        <el-button @click="showRevokeDialog = false">取消</el-button>
        <el-button type="danger" :disabled="!revokeReason.trim()" @click="handleRevoke">
          确认撤回
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { Clock, Plus } from '@element-plus/icons-vue'

// Compute today string once for the calendar highlight
const now = new Date()
const todayStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`

export default {
  components: { Clock, Plus },
  data() {
    return { todayStr }
  },
}
</script>

<style scoped>
.task-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 20px;
  background: #f5f7fa;
  overflow: hidden;
}

/* ---- Page header ---- */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-shrink: 0;
}
.page-title {
  font-size: 20px;
  color: #303133;
  margin: 0;
}

/* ---- Body layout ---- */
.page-body {
  display: flex;
  gap: 16px;
  flex: 1;
  overflow: hidden;
}

/* ---- Left panel (calendar) ---- */
.left-panel {
  width: 30%;
  min-width: 280px;
  max-width: 360px;
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.left-panel :deep(.el-calendar) {
  --el-calendar-border: none;
  --el-calendar-header-border-bottom: 1px solid #ebeef5;
}
.left-panel :deep(.el-calendar__header) {
  padding: 8px 0;
}
.left-panel :deep(.el-calendar__title) {
  font-size: 14px;
  color: #1d9e75;
}
.left-panel :deep(.el-calendar__body) {
  padding: 0;
}
.left-panel :deep(.el-calendar-table td) {
  border: none;
  padding: 2px;
}
.left-panel :deep(.el-calendar-table th) {
  padding: 4px 0;
  font-size: 12px;
  color: #909399;
}

.date-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  margin: 0 auto;
  border-radius: 50%;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}
.date-cell:hover {
  background: #e8f5f0;
  color: #1d9e75;
}
.date-cell.is-selected {
  background: #1d9e75;
  color: #fff;
  font-weight: 600;
}
.date-cell.is-today:not(.is-selected) {
  color: #1d9e75;
  font-weight: 600;
}
.date-cell.has-task::after {
  content: '';
  position: absolute;
  bottom: 2px;
  left: 50%;
  transform: translateX(-50%);
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #f59e0b;
}
.date-cell.is-selected.has-task::after {
  background: #fff;
}

.date-picker-wrap {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}
.date-picker-label {
  display: block;
  font-size: 13px;
  color: #606266;
  margin-bottom: 6px;
}

/* ---- Right panel (task list) ---- */
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.filter-bar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.selected-date-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
  margin-bottom: 12px;
  flex-shrink: 0;
}
.selected-date-badge .el-icon {
  color: #1d9e75;
}

/* ---- Task list ---- */
.task-list {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}
.task-list::-webkit-scrollbar {
  width: 6px;
}
.task-list::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.task-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  margin-bottom: 10px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  transition: box-shadow 0.2s, border-color 0.2s;
  background: #fff;
}
.task-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border-color: #c6e2d8;
}
.task-card:last-child {
  margin-bottom: 0;
}

.task-card-body {
  display: flex;
  align-items: center;
  gap: 24px;
  flex: 1;
  min-width: 0;
}

.task-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.task-patient {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.task-treatment {
  font-size: 13px;
  color: #606266;
}
.task-time {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  margin-left: auto;
}
.meta-divider {
  color: #dcdfe6;
}
.meta-text {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}

.task-card-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  margin-left: 16px;
}

/* IN_PROGRESS tag uses a custom dark blue since el-tag "info" is gray */
.tag-progress {
  --el-tag-bg-color: #409eff;
  --el-tag-text-color: #fff;
  --el-tag-border-color: #409eff;
}

/* ---- Empty state ---- */
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

/* ---- Pagination ---- */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
  flex-shrink: 0;
  border-top: 1px solid #ebeef5;
  margin-top: 12px;
}

/* ---- Revoke dialog ---- */
.revoke-info {
  margin-bottom: 16px;
  font-size: 14px;
  color: #606266;
}
.revoke-info strong {
  color: #303133;
}
</style>
