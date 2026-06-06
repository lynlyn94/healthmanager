<template>
  <div class="log-view">
    <!-- Search Filters -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input
            v-model="searchForm.username"
            placeholder="请输入用户名"
            clearable
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="searchForm.action" placeholder="全部" clearable style="width: 140px">
            <el-option label="新增" value="CREATE" />
            <el-option label="更新" value="UPDATE" />
            <el-option label="删除" value="DELETE" />
            <el-option label="查询" value="QUERY" />
            <el-option label="登录" value="LOGIN" />
            <el-option label="导出" value="EXPORT" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Log Table -->
    <el-card class="table-card" shadow="never">
      <div class="table-header">
        <span class="table-title">操作日志</span>
      </div>

      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
        @expand-change="handleExpand"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content">
              <el-descriptions :column="2" border size="small" title="请求详情">
                <el-descriptions-item label="请求URL" :span="2">
                  <el-tag size="small">{{ row.requestUrl || '-' }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="请求参数" :span="2">
                  <pre class="json-block">{{ formatJson(row.requestParam) }}</pre>
                </el-descriptions-item>
                <el-descriptions-item label="耗时">
                  {{ row.costTime != null ? `${row.costTime}ms` : '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="请求IP">
                  {{ row.requestIp || '-' }}
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column prop="username" label="用户名" width="110" />
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-tag :type="actionTag(row.action)" size="small">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetType" label="目标类型" width="100" />
        <el-table-column prop="targetId" label="目标ID" width="80" />
        <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
        <el-table-column prop="requestIp" label="请求IP" width="140" />
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无操作日志" />

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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { get } from '@/api'
import type { PageResult } from '@/api/types'

interface LogRecord {
  id: number
  userId?: number
  username: string
  module: string
  action: string
  targetType: string
  targetId: number | string
  description: string
  requestUrl: string
  requestParam: string
  requestIp: string
  costTime: number
  createTime: string
}

// ── Search ────────────────────────────────────────────────
const searchForm = reactive({
  username: '',
  action: '',
  dateRange: null as [string, string] | null,
})

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleReset() {
  searchForm.username = ''
  searchForm.action = ''
  searchForm.dateRange = null
  pagination.page = 1
  fetchData()
}

// ── Table ─────────────────────────────────────────────────
const tableData = ref<LogRecord[]>([])
const loading = ref(false)
const pagination = reactive({ page: 1, size: 10, total: 0 })

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: pagination.page, size: pagination.size }
    if (searchForm.action) params.action = searchForm.action
    if (searchForm.dateRange) {
      params.startDate = searchForm.dateRange[0] + 'T00:00:00'
      params.endDate = searchForm.dateRange[1] + 'T23:59:59'
    }
    const res = await get<any>('/admin/logs', params)
    const result = res.data ?? res
    let records = result.records || (Array.isArray(result) ? result : [])
    // Client-side username filter (backend expects numeric userId)
    if (searchForm.username) {
      const kw = searchForm.username.toLowerCase()
      records = records.filter((r: LogRecord) => (r.username || '').toLowerCase().includes(kw))
    }
    tableData.value = records
    pagination.total = result.total || records.length
  } finally {
    loading.value = false
  }
}

function handleExpand(row: LogRecord, expandedRows: LogRecord[]) {
  // expandedRows contains the currently expanded set
}

function formatJson(val: string | null | undefined): string {
  if (!val) return '-'
  try {
    const obj = JSON.parse(val)
    return JSON.stringify(obj, null, 2)
  } catch {
    return val
  }
}

function actionTag(action: string): 'success' | 'warning' | 'danger' | 'info' | '' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info' | ''> = {
    CREATE: 'success',
    UPDATE: 'warning',
    DELETE: 'danger',
    QUERY: 'info',
    LOGIN: '',
    EXPORT: 'success',
  }
  return map[action] || 'info'
}

// ── Init ──────────────────────────────────────────────────
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.log-view {
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

.expand-content {
  padding: 12px 24px;
}

.json-block {
  margin: 0;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  max-height: 200px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
