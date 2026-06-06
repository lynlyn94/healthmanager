<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Download } from '@element-plus/icons-vue'
import axios from 'axios'
import { get } from '@/api'
import type { WorkloadStat, PageResult } from '@/api/types'

// ---------- loading ----------
const loading = ref(false)
const exportLoading = ref(false)

// ---------- stat cards ----------
const todayCount = ref(0)
const weekCount = ref(0)
const monthCount = ref(0)

// ---------- date range ----------
const dateRange = ref<[Date, Date]>()
const shortcuts = [
  { text: '本周',  value: () => { const [s, e] = getWeekRange(); return [s, e] } },
  { text: '本月',  value: () => { const [s, e] = getMonthRange(); return [s, e] } },
  { text: '上月',  value: () => { const [s, e] = getLastMonthRange(); return [s, e] } },
]

// ---------- treatment type filter ----------
const typeFilter = ref('')
const typeOptions = ref<{ label: string; value: string }[]>([])

// ---------- echart ----------
const chartContainer = ref<HTMLDivElement>()
let chartInstance: echarts.ECharts | null = null

// ---------- detail table ----------
const tableData = ref<WorkloadStat[]>([])
const tableLoading = ref(false)

// ---------- helpers ----------
function formatDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function getWeekRange(): [Date, Date] {
  const now = new Date()
  const dayOfWeek = now.getDay() || 7
  const start = new Date(now)
  start.setDate(now.getDate() - dayOfWeek + 1)
  start.setHours(0, 0, 0, 0)
  const end = new Date(now)
  end.setDate(now.getDate() + (7 - dayOfWeek))
  end.setHours(23, 59, 59, 999)
  return [start, end]
}

function getMonthRange(): [Date, Date] {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0)
  const end = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59, 999)
  return [start, end]
}

function getLastMonthRange(): [Date, Date] {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth() - 1, 1, 0, 0, 0)
  const end = new Date(now.getFullYear(), now.getMonth(), 0, 23, 59, 59, 999)
  return [start, end]
}

// ---------- API calls ----------
async function fetchStats() {
  const today = formatDate(new Date())
  const [weekS, weekE] = getWeekRange()
  const [monthS, monthE] = getMonthRange()

  try {
    const [todayRes, weekRes, monthRes] = await Promise.all([
      get<any>('/workload/personal', { startDate: today, endDate: today }),
      get<any>('/workload/personal', { startDate: formatDate(weekS), endDate: formatDate(weekE) }),
      get<any>('/workload/personal', { startDate: formatDate(monthS), endDate: formatDate(monthE) }),
    ])

    const extract = (res: any) => {
      const data = res.data ?? res
      return data?.totalTreatmentCount ?? data?.treatmentCount ?? data?.totalCount
        ?? (data?.records ? data.records.reduce((s: number, r: any) => s + (r.treatmentCount || 0), 0) : 0)
    }

    todayCount.value = extract(todayRes)
    weekCount.value = extract(weekRes)
    monthCount.value = extract(monthRes)
  } catch {
    // handled by interceptor
  }
}

async function fetchTrend() {
  if (!chartContainer.value || !dateRange.value) return

  const [startDate, endDate] = dateRange.value
  const params: any = {
    startDate: formatDate(startDate),
    endDate: formatDate(endDate),
  }

  try {
    const res = await get<any>('/workload/trend', params)
    const records: any[] = res.data ?? res ?? []

    if (!chartInstance) {
      chartInstance = echarts.init(chartContainer.value)
    }

    chartInstance.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['治疗次数', '患者数'], bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '12%', top: '10%', containLabel: true },
      xAxis: {
        type: 'category',
        data: records.map((r) => r.date || r.statDate || ''),
        axisLabel: { rotate: 30 },
      },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        {
          name: '治疗次数',
          type: 'bar',
          data: records.map((r) => r.treatmentCount || r.count || 0),
          itemStyle: { color: '#1D9E75' },
        },
        {
          name: '患者数',
          type: 'line',
          data: records.map((r) => r.patientCount || r.patients || 0),
          itemStyle: { color: '#378ADD' },
          smooth: true,
        },
      ],
    })
  } catch {
    // handled by interceptor
  }
}

async function fetchTable() {
  if (!dateRange.value) return
  const [startDate, endDate] = dateRange.value
  tableLoading.value = true
  try {
    const res = await get<any>('/workload/personal', {
      startDate: formatDate(startDate),
      endDate: formatDate(endDate),
    })
    const data = res.data ?? res
    // Backend returns Map with aggregated stats; if records present, use those, else make a single-row summary
    if (data.records) {
      tableData.value = data.records
    } else if (typeof data === 'object') {
      tableData.value = [{
        date: `${formatDate(startDate)} ~ ${formatDate(endDate)}`,
        treatmentCount: data.totalTreatmentCount ?? data.treatmentCount ?? data.totalCount ?? 0,
        patientCount: data.totalPatientCount ?? data.patientCount ?? 0,
        treatmentType: typeFilter.value || '-',
      }] as any
    }
  } catch {
    // handled by interceptor
  } finally {
    tableLoading.value = false
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

async function handleExport() {
  if (!dateRange.value) return
  exportLoading.value = true
  try {
    const [startDate, endDate] = dateRange.value
    await exportExcel(
      '/api/v1/workload/export',
      { startDate: formatDate(startDate), endDate: formatDate(endDate) },
      '工作量统计.xlsx'
    )
  } finally {
    exportLoading.value = false
  }
}

async function reloadAll() {
  if (!dateRange.value) return
  loading.value = true
  try {
    await Promise.all([fetchStats(), fetchTrend(), fetchTable()])
  } finally {
    loading.value = false
  }
}

// ---------- watchers ----------
watch(dateRange, () => {
  reloadAll()
})

watch(typeFilter, () => {
  if (dateRange.value) {
    fetchTrend()
    fetchTable()
  }
})

// ---------- resize handling ----------
function handleResize() {
  chartInstance?.resize()
}

// ---------- lifecycle ----------
onMounted(async () => {
  // default to current week
  const [s, e] = getWeekRange()
  dateRange.value = [s, e]

  await nextTick()
  reloadAll()
  window.addEventListener('resize', handleResize)

  // load treatment type options from dict or dedup
  try {
    const res = await get<any>('/workload/personal', {
      startDate: formatDate(getMonthRange()[0]),
      endDate: formatDate(getMonthRange()[1]),
    })
    const records: any[] = res.data?.records || []
    const types = [...new Set(records.map((r: any) => r.treatmentType || r.type).filter(Boolean))]
    typeOptions.value = types.map((t) => ({ label: t as string, value: t as string }))
  } catch { /* ignore */ }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  chartInstance = null
})
</script>

<template>
  <div class="workload-page" v-loading="loading">
    <!-- header row -->
    <div class="page-header">
      <h2 class="page-title">工作量统计</h2>
      <div class="header-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          :shortcuts="shortcuts"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          style="width: 280px"
        />
        <el-button type="primary" :loading="exportLoading" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出报表
        </el-button>
      </div>
    </div>

    <!-- stat cards -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">今日完成数</div>
          <div class="stat-value">{{ todayCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">本周累计</div>
          <div class="stat-value">{{ weekCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">本月累计</div>
          <div class="stat-value">{{ monthCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- chart -->
    <el-card shadow="hover" class="chart-card">
      <template #header>
        <div class="card-header">
          <span>治疗趋势</span>
          <el-select
            v-model="typeFilter"
            placeholder="治疗类型筛选"
            clearable
            style="width: 180px"
          >
            <el-option
              v-for="item in typeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </div>
      </template>
      <div ref="chartContainer" class="chart-container" />
      <el-empty v-if="!loading && tableData.length === 0" description="暂无趋势数据" />
    </el-card>

    <!-- detail table -->
    <el-card shadow="hover" class="table-card">
      <template #header>
        <span>每日明细</span>
      </template>
      <el-table :data="tableData" v-loading="tableLoading" stripe style="width: 100%">
        <el-table-column prop="date" label="日期" width="140" />
        <el-table-column prop="treatmentCount" label="治疗次数" width="120" />
        <el-table-column prop="patientCount" label="患者数" width="120" />
        <el-table-column prop="treatmentType" label="治疗类型" min-width="160">
          <template #default="{ row }">
            {{ row.treatmentType || '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.workload-page {
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
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.stat-row {
  margin-bottom: 20px;
}
.stat-card {
  text-align: center;
}
.stat-label {
  font-size: 14px;
  color: #999;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 36px;
  font-weight: bold;
  color: #1D9E75;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.chart-card {
  margin-bottom: 20px;
}
.chart-container {
  width: 100%;
  height: 360px;
}
.table-card {
  margin-bottom: 20px;
}
</style>
