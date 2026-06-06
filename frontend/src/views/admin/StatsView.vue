<template>
  <div class="stats-view" v-loading="loading">
    <!-- Date Range Picker -->
    <el-row :gutter="16" class="filter-row">
      <el-col :span="24" class="filter-right">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          :shortcuts="dateShortcuts"
          @change="onDateRangeChange"
        />
      </el-col>
    </el-row>

    <!-- 6 Stat Cards Row -->
    <el-row :gutter="16" class="stat-cards-row">
      <el-col :span="4">
        <div class="stat-card">
          <div class="stat-card-icon" style="background: #ecf5ff; color: #409eff">
            <el-icon :size="28"><User /></el-icon>
          </div>
          <div class="stat-card-info">
            <div class="stat-card-value">{{ stats.totalUsers }}</div>
            <div class="stat-card-label">总用户数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="stat-card">
          <div class="stat-card-icon" style="background: #f0f9eb; color: #67c23a">
            <el-icon :size="28"><Avatar /></el-icon>
          </div>
          <div class="stat-card-info">
            <div class="stat-card-value">{{ stats.activePatients }}</div>
            <div class="stat-card-label">在院患者数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="stat-card">
          <div class="stat-card-icon" style="background: #fdf6ec; color: #e6a23c">
            <el-icon :size="28"><Tickets /></el-icon>
          </div>
          <div class="stat-card-info">
            <div class="stat-card-value">{{ stats.todayTasks }}</div>
            <div class="stat-card-label">今日任务数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="stat-card">
          <div class="stat-card-icon" style="background: #fef0f0; color: #f56c6c">
            <el-icon :size="28"><CircleCheck /></el-icon>
          </div>
          <div class="stat-card-info">
            <div class="stat-card-value">{{ stats.monthlyVerifications }}</div>
            <div class="stat-card-label">本月核销数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="stat-card">
          <div class="stat-card-icon" style="background: #f4f0fe; color: #7c5cfc">
            <el-icon :size="28"><OfficeBuilding /></el-icon>
          </div>
          <div class="stat-card-info">
            <div class="stat-card-value">{{ stats.groupsCount }}</div>
            <div class="stat-card-label">治疗组数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="stat-card">
          <div class="stat-card-icon" style="background: #e8f8f0; color: #10b981">
            <el-icon :size="28"><Select /></el-icon>
          </div>
          <div class="stat-card-info">
            <div class="stat-card-value">{{ stats.todayVerified }}</div>
            <div class="stat-card-label">今日核销数</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- Middle Charts Row: Bar + Pie -->
    <el-row :gutter="16" class="charts-row">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span class="chart-title">各角色工作量对比</span>
          </template>
          <div ref="barChartRef" class="chart-container"></div>
          <el-empty v-if="barEmpty && !loading" description="暂无数据" :image-size="80" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span class="chart-title">治疗类型分布</span>
          </template>
          <div ref="pieChartRef" class="chart-container"></div>
          <el-empty v-if="pieEmpty && !loading" description="暂无数据" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>

    <!-- Bottom Line Chart: Daily Trend -->
    <el-row :gutter="16" class="charts-row">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>
            <span class="chart-title">每日核销量趋势</span>
          </template>
          <div ref="lineChartRef" class="chart-container" style="height: 320px"></div>
          <el-empty v-if="lineEmpty && !loading" description="暂无数据" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { User, Avatar, Tickets, CircleCheck, OfficeBuilding, Select } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'
import { get } from '@/api'

// ── Date Range ─────────────────────────────────────────────
const dateRange = ref<[string, string]>([getMonthStart(), getToday()])

function getToday(): string {
  const d = new Date()
  return d.toISOString().split('T')[0]
}

function getMonthStart(): string {
  const d = new Date()
  d.setDate(1)
  return d.toISOString().split('T')[0]
}

const dateShortcuts = [
  {
    text: '本月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(1)
      return [start, end] as [Date, Date]
    },
  },
  {
    text: '上月',
    value: () => {
      const end = new Date()
      end.setDate(0)
      const start = new Date()
      start.setDate(0)
      start.setDate(1)
      return [start, end] as [Date, Date]
    },
  },
  {
    text: '近7天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 6)
      return [start, end] as [Date, Date]
    },
  },
  {
    text: '近30天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 29)
      return [start, end] as [Date, Date]
    },
  },
]

// ── Loading ───────────────────────────────────────────────
const loading = ref(false)

// ── Stats ─────────────────────────────────────────────────
const stats = reactive({
  totalUsers: 0,
  activePatients: 0,
  todayTasks: 0,
  monthlyVerifications: 0,
  groupsCount: 0,
  todayVerified: 0,
})

async function fetchStats() {
  try {
    const res = await get<any>('/admin/stats')
    const data = (res as any)?.data ?? res
    if (data && typeof data === 'object') {
      stats.totalUsers = data.totalUsers ?? 0
      stats.activePatients = data.activePatients ?? 0
      stats.todayTasks = data.todayTasks ?? 0
      stats.monthlyVerifications = data.monthlyVerifications ?? 0
      stats.groupsCount = data.groupsCount ?? 0
      stats.todayVerified = data.todayVerified ?? 0
    }
  } catch {
    // use defaults
  }
}

// ── Charts ────────────────────────────────────────────────
const barChartRef = ref<HTMLDivElement>()
const pieChartRef = ref<HTMLDivElement>()
const lineChartRef = ref<HTMLDivElement>()

let barChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null
let lineChart: echarts.ECharts | null = null

const barEmpty = ref(false)
const pieEmpty = ref(false)
const lineEmpty = ref(false)

// Bar chart: workload by role
async function updateBarChart() {
  if (!barChart || !dateRange.value) return
  const [startDate, endDate] = dateRange.value

  try {
    const res = await get<any>('/admin/stats/workload-by-role', { startDate, endDate })
    const data = (res as any)?.data ?? res
    const names = (data ?? []).map((d: any) => d.role ?? '-')
    const values = (data ?? []).map((d: any) => d.count ?? 0)
    barEmpty.value = (data ?? []).length === 0

    const option: EChartsOption = {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '8%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        data: names,
        axisLabel: { rotate: names.length > 5 ? 30 : 0 },
      },
      yAxis: { type: 'value', name: '工作量', minInterval: 1 },
      series: [
        {
          type: 'bar',
          data: values,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#409eff' },
              { offset: 1, color: '#79bbff' },
            ]),
            borderRadius: [6, 6, 0, 0],
          },
          barWidth: '50%',
        },
      ],
    }
    barChart.setOption(option)
  } catch {
    barEmpty.value = true
    barChart.setOption({
      title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#999', fontSize: 14 } },
    })
  }
}

// Pie chart: treatment type distribution
async function updatePieChart() {
  if (!pieChart || !dateRange.value) return
  const [startDate, endDate] = dateRange.value

  try {
    const res = await get<any>('/admin/stats/treatment-distribution', { startDate, endDate })
    const data = (res as any)?.data ?? res
    const pieData = (data ?? []).map((d: any) => ({
      name: d.type ?? d.treatmentType ?? '-',
      value: d.count ?? 0,
    }))
    pieEmpty.value = (data ?? []).length === 0

    const option: EChartsOption = {
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { orient: 'vertical', right: '5%', top: 'center' },
      series: [
        {
          type: 'pie',
          radius: ['45%', '72%'],
          center: ['38%', '50%'],
          avoidLabelOverlap: false,
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          emphasis: {
            label: { show: true, fontSize: 14, fontWeight: 'bold' },
          },
          data: pieData,
        },
      ],
    }
    pieChart.setOption(option)
  } catch {
    pieEmpty.value = true
    pieChart.setOption({
      title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#999', fontSize: 14 } },
    })
  }
}

// Line chart: daily verification trend
async function updateLineChart() {
  if (!lineChart || !dateRange.value) return
  const [startDate, endDate] = dateRange.value

  try {
    const res = await get<any>('/admin/stats/daily-trend', { startDate, endDate })
    const data = (res as any)?.data ?? res
    const dates = (data ?? []).map((d: any) => d.date ?? '-')
    const values = (data ?? []).map((d: any) => d.count ?? 0)
    lineEmpty.value = (data ?? []).length === 0

    const option: EChartsOption = {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross', label: { backgroundColor: '#6a7985' } },
      },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '8%', containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: dates,
      },
      yAxis: {
        type: 'value',
        name: '核销量',
        minInterval: 1,
      },
      series: [
        {
          type: 'line',
          data: values,
          smooth: true,
          lineStyle: { color: '#409eff', width: 3 },
          itemStyle: { color: '#409eff' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(64,158,255,0.35)' },
              { offset: 1, color: 'rgba(64,158,255,0.05)' },
            ]),
          },
          symbol: 'circle',
          symbolSize: 6,
        },
      ],
    }
    lineChart.setOption(option)
  } catch {
    lineEmpty.value = true
    lineChart.setOption({
      title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#999', fontSize: 14 } },
    })
  }
}

async function updateAllCharts() {
  loading.value = true
  try {
    await Promise.all([updateBarChart(), updatePieChart(), updateLineChart()])
  } finally {
    loading.value = false
  }
}

function onDateRangeChange() {
  updateAllCharts()
}

// ── Watch dateRange ───────────────────────────────────────
watch(dateRange, () => {
  updateAllCharts()
}, { deep: true })

// ── Resize ────────────────────────────────────────────────
function resizeCharts() {
  barChart?.resize()
  pieChart?.resize()
  lineChart?.resize()
}

// ── Lifecycle ─────────────────────────────────────────────
onMounted(async () => {
  await fetchStats()

  // Init chart instances
  await nextTick()
  if (barChartRef.value) barChart = echarts.init(barChartRef.value)
  if (pieChartRef.value) pieChart = echarts.init(pieChartRef.value)
  if (lineChartRef.value) lineChart = echarts.init(lineChartRef.value)

  // Load initial chart data
  updateAllCharts()

  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  barChart?.dispose()
  pieChart?.dispose()
  lineChart?.dispose()
})
</script>

<style scoped>
.stats-view {
  padding: 16px;
}

/* Filter Row */
.filter-row {
  margin-bottom: 16px;
}

.filter-right {
  display: flex;
  justify-content: flex-end;
}

/* Stat Cards */
.stat-cards-row {
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.3s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.stat-card-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card-info {
  display: flex;
  flex-direction: column;
}

.stat-card-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-card-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

/* Charts */
.charts-row {
  margin-bottom: 16px;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
}

.chart-container {
  width: 100%;
  height: 300px;
}
</style>
