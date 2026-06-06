<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { get, post } from '@/api'

interface TreatmentPlan {
  id?: number
  patientId: number
  planName: string
  treatmentItems: string
  frequency: string
  dailyCount: number
  periodStart: string
  periodEnd: string
  status: string
  creatorId?: number
  reviewerId?: number
  reviewComment?: string
  submitTime?: string
  patientName?: string
  therapistName?: string
}

const tableData = ref<TreatmentPlan[]>([])
const loading = ref(false)

async function fetchPlans() {
  loading.value = true
  try {
    const res = await get<any>('/treatment-plans/pending-review')
    tableData.value = (res as any).data ?? res ?? []
  } finally {
    loading.value = false
  }
}

const statusMap: Record<string, { text: string; type: '' | 'info' | 'warning' | 'success' | 'danger' }> = {
  DRAFT: { text: '草稿', type: 'info' },
  SUBMITTED: { text: '待审阅', type: 'warning' },
  APPROVED: { text: '已审核(待开医嘱)', type: '' },
  ORDERED: { text: '已开医嘱', type: 'success' },
  REVIEWED: { text: '已审阅', type: 'success' },
}

async function handleApprove(row: TreatmentPlan) {
  try {
    const { value } = await ElMessageBox.prompt('审阅意见（选填）', '审核通过', {
      inputType: 'textarea',
      confirmButtonText: '确认通过',
      cancelButtonText: '取消',
    })
    await post(`/treatment-plans/${row.id}/review`, { reviewComment: value || '' })
    ElMessage.success('方案已审核通过')
    fetchPlans()
  } catch { /* cancelled */ }
}

async function handleReject(row: TreatmentPlan) {
  try {
    const { value } = await ElMessageBox.prompt('请输入退回原因', '退回方案', {
      inputType: 'textarea',
      confirmButtonText: '确认退回',
      cancelButtonText: '取消',
      inputValidator: (val: string) => !!val || '请填写退回原因',
      inputErrorMessage: '退回原因不能为空',
    })
    await post<any>(`/treatment-plans/${row.id}/reject`, { reason: value || '' })
    ElMessage.success('方案已退回给治疗师修改')
    fetchPlans()
  } catch { /* cancelled */ }
}

function formatItems(val: string): string {
  if (!val) return '-'
  try {
    const arr = JSON.parse(val)
    if (Array.isArray(arr)) return arr.join('、')
  } catch {}
  return val
}

onMounted(() => fetchPlans())
</script>

<template>
  <div class="plan-review-page">
    <div class="page-header">
      <h2 class="page-title">待审治疗方案</h2>
      <el-button @click="fetchPlans">刷新</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="planName" label="方案名称" min-width="160" />
        <el-table-column prop="patientName" label="患者" width="100" />
        <el-table-column prop="therapistName" label="制定人" width="100" />
        <el-table-column label="治疗项目" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ formatItems(row.treatmentItems) }}</template>
        </el-table-column>
        <el-table-column label="频次/日次" width="120">
          <template #default="{ row }">{{ row.frequency }} / 日{{ row.dailyCount }}次</template>
        </el-table-column>
        <el-table-column label="周期" width="180">
          <template #default="{ row }">{{ row.periodStart }} ~ {{ row.periodEnd }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ row.submitTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'" size="small">
              {{ statusMap[row.status]?.text || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'SUBMITTED'">
              <el-button type="success" size="small" @click="handleApprove(row)">审核通过</el-button>
              <el-button type="danger" size="small" @click="handleReject(row)">退回</el-button>
            </template>
            <span v-else-if="row.status === 'REVIEWED'" style="color: #67c23a">已处理</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !tableData.length" description="暂无待审治疗方案" />
    </el-card>
  </div>
</template>

<style scoped>
.plan-review-page { padding: 16px; max-width: 1400px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-title { margin: 0; font-size: 20px; color: #333; }
</style>
