<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { get, post, put } from '@/api'
import type { AssessmentTemplate, Patient, PatientSchedule } from '@/api/types'

const router = useRouter()

// Dialog visibility for feature cards that open dialogs
const showAssessmentDialog = ref(false)
const showScheduleDialog = ref(false)
const showSettingsDialog = ref(false)
const showNotificationDialog = ref(false)

interface NavCard {
  title: string
  description: string
  icon: string
  color: string
  action: 'navigate' | 'dialog'
  target?: string
  dialogKey?: string
}

const cards: NavCard[] = [
  { title: '治疗任务', description: '查看和管理每日治疗任务', icon: 'Clock', color: '#1D9E75', action: 'navigate', target: '/tasks' },
  { title: '患者列表', description: '管理在院患者信息与档案', icon: 'UserFilled', color: '#378ADD', action: 'navigate', target: '/patients' },
  { title: '工作量统计', description: '查看个人及小组工作量报表', icon: 'TrendCharts', color: '#E6A23C', action: 'navigate', target: '/workload' },
  { title: '量表评估', description: '对患者进行康复量表评估', icon: 'DocumentChecked', color: '#9B59B6', action: 'dialog', dialogKey: 'assessment' },
  { title: '患者日程', description: '查看患者排班与日程安排', icon: 'Calendar', color: '#3498DB', action: 'dialog', dialogKey: 'schedule' },
  { title: '医嘱查看', description: '查看医生下达的治疗医嘱', icon: 'Document', color: '#E74C3C', action: 'navigate', target: '/orders' },
  { title: '个人设置', description: '修改个人信息与系统偏好', icon: 'Setting', color: '#7F8C8D', action: 'dialog', dialogKey: 'settings' },
  { title: '消息通知', description: '查看系统消息与待办提醒', icon: 'Bell', color: '#F39C12', action: 'dialog', dialogKey: 'notification' },
]

function handleCardClick(card: NavCard) {
  if (card.action === 'navigate' && card.target) {
    router.push(card.target)
  } else if (card.action === 'dialog') {
    switch (card.dialogKey) {
      case 'assessment': showAssessmentDialog.value = true; break
      case 'schedule': showScheduleDialog.value = true; break
      case 'settings': showSettingsDialog.value = true; break
      case 'notification': showNotificationDialog.value = true; break
    }
  }
}

// ---- Assessment dialog state ----
const squarePatients = ref<Patient[]>([])
const squareTemplates = ref<AssessmentTemplate[]>([])
const squareAssessmentLoading = ref(false)
const submittingAssessment = ref(false)
const squareAssessmentForm = reactive({
  patientId: null as number | null,
  templateId: null as number | null,
  assessDate: new Date().toISOString().slice(0, 10),
  totalScore: 0,
  conclusion: '',
})

async function fetchSquareTemplates() {
  try {
    const res = await get<any>('/assessments/templates')
    squareTemplates.value = (res as any).data ?? res
    const pRes = await get<any>('/patients', { size: 500, viewScope: 'all' })
    squarePatients.value = (pRes as any).data?.records ?? []
  } catch { /* ignore */ }
}

async function submitSquareAssessment() {
  if (!squareAssessmentForm.patientId || !squareAssessmentForm.templateId) {
    ElMessage.warning('请选择患者和量表')
    return
  }
  submittingAssessment.value = true
  try {
    await post('/assessments', squareAssessmentForm)
    ElMessage.success('评估已提交')
    showAssessmentDialog.value = false
  } finally {
    submittingAssessment.value = false
  }
}

// ---- Schedule dialog state ----
const squareScheduleLoading = ref(false)
const squareSchedules = ref<PatientSchedule[]>([])
const squareScheduleDateRange = ref<[string, string]>([
  new Date().toISOString().slice(0, 10),
  new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 10),
])

async function fetchMyScheduleForSquare() {
  squareScheduleLoading.value = true
  try {
    const [start, end] = squareScheduleDateRange.value
    const res = await get<any>('/schedule/my', { startDate: start, endDate: end })
    squareSchedules.value = (res as any).data ?? res
  } finally {
    squareScheduleLoading.value = false
  }
}

// ---- Settings dialog ----
function goToSettings() {
  showSettingsDialog.value = false
  router.push('/settings')
}

// ---- Notification dialog state ----
interface SquareNotification {
  id: number; title: string; content: string; type: string; isRead: number; createTime: string
}
const squareNotifications = ref<SquareNotification[]>([])
const squareNotificationsLoading = ref(false)
const hasUnreadNotifs = computed(() => squareNotifications.value.some(n => n.isRead === 0))

async function fetchNotificationsForSquare() {
  squareNotificationsLoading.value = true
  try {
    const res = await get<any>('/notifications', { page: 1, size: 50 })
    squareNotifications.value = (res as any).data?.records ?? []
  } finally {
    squareNotificationsLoading.value = false
  }
}

async function markAllNotifsRead() {
  try {
    await put('/notifications/read-all')
    squareNotifications.value.forEach(n => n.isRead = 1)
    ElMessage.success('已全部标记为已读')
  } catch { /* ignore */ }
}
</script>

<template>
  <div class="square-page">
    <!-- Header area -->
    <div class="square-header">
      <h1 class="square-title">功能广场</h1>
      <p class="square-subtitle">康复治疗管理系统 - 快捷功能入口</p>
    </div>

    <!-- Card grid: 2 rows x 4 columns -->
    <el-row :gutter="20" class="card-grid" justify="center">
      <el-col
        v-for="card in cards"
        :key="card.title"
        :span="6"
        class="card-col"
      >
        <div class="nav-card" @click="handleCardClick(card)">
          <div class="card-icon-wrap" :style="{ background: card.color }">
            <el-icon :size="32" color="#fff">
              <component :is="card.icon" />
            </el-icon>
          </div>
          <div class="card-content">
            <h3 class="card-title">{{ card.title }}</h3>
            <p class="card-desc">{{ card.description }}</p>
          </div>
          <div class="card-arrow">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- ========== Dialogs for feature cards ========== -->

    <!-- 量表评估 dialog (functional) -->
    <el-dialog v-model="showAssessmentDialog" title="量表评估" width="540px" destroy-on-close @open="fetchSquareTemplates">
      <div v-loading="squareAssessmentLoading">
        <el-form label-width="90px">
          <el-form-item label="患者">
            <el-select v-model="squareAssessmentForm.patientId" placeholder="请选择患者" filterable style="width: 100%">
              <el-option v-for="p in squarePatients" :key="p.id" :label="`${p.name} (${p.bedNo || '-'})`" :value="p.id!" />
            </el-select>
          </el-form-item>
          <el-form-item label="量表模板">
            <el-select v-model="squareAssessmentForm.templateId" placeholder="请选择量表" style="width: 100%">
              <el-option v-for="t in squareTemplates" :key="t.id" :label="`${t.templateName} (${t.abbreviation})`" :value="t.id!" />
            </el-select>
          </el-form-item>
          <el-form-item label="评估日期">
            <el-date-picker v-model="squareAssessmentForm.assessDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="总分">
            <el-input-number v-model="squareAssessmentForm.totalScore" :min="0" style="width: 100%" />
          </el-form-item>
          <el-form-item label="结论">
            <el-input v-model="squareAssessmentForm.conclusion" type="textarea" :rows="2" placeholder="评估结论" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showAssessmentDialog = false">取消</el-button>
        <el-button type="primary" :loading="submittingAssessment" @click="submitSquareAssessment">提交评估</el-button>
      </template>
    </el-dialog>

    <!-- 患者日程 dialog (functional) -->
    <el-dialog v-model="showScheduleDialog" title="我的日程" width="700px" destroy-on-close @open="fetchMyScheduleForSquare">
      <div v-loading="squareScheduleLoading">
        <el-date-picker
          v-model="squareScheduleDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="margin-bottom: 16px"
          @change="fetchMyScheduleForSquare"
        />
        <el-timeline v-if="squareSchedules.length">
          <el-timeline-item
            v-for="item in squareSchedules"
            :key="item.id"
            :timestamp="`${item.scheduleDate} ${item.timeSlot || ''}`"
            placement="top"
            size="small"
          >
            <div style="display: flex; align-items: center; gap: 8px;">
              <strong>{{ item.title }}</strong>
              <el-tag size="small">{{ item.status === 'COMPLETED' ? '已完成' : item.status === 'TASK_GENERATED' ? '已生成任务' : '待排程' }}</el-tag>
            </div>
            <p v-if="item.description" style="color: #909399; font-size: 13px; margin: 4px 0 0;">{{ item.description }}</p>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无日程数据" :image-size="80" />
      </div>
      <template #footer>
        <el-button @click="showScheduleDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 个人设置 dialog (redirect) -->
    <el-dialog v-model="showSettingsDialog" title="个人设置" width="440px" destroy-on-close>
      <div class="dialog-placeholder">
        <el-icon :size="48" color="#1d9e75"><Setting /></el-icon>
        <p>修改个人信息、密码与系统偏好</p>
        <el-button type="primary" @click="goToSettings">前往个人设置</el-button>
      </div>
      <template #footer>
        <el-button @click="showSettingsDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 消息通知 dialog (functional) -->
    <el-dialog v-model="showNotificationDialog" title="消息通知" width="520px" destroy-on-close @open="fetchNotificationsForSquare">
      <div v-loading="squareNotificationsLoading">
        <template v-if="squareNotifications.length">
          <div v-for="n in squareNotifications" :key="n.id" class="notif-item" :class="{ unread: n.isRead === 0 }">
            <div class="notif-header">
              <span class="notif-title">{{ n.title }}</span>
              <el-tag size="small" :type="n.isRead === 0 ? 'danger' : 'info'">{{ n.isRead === 0 ? '未读' : '已读' }}</el-tag>
            </div>
            <p class="notif-content">{{ n.content }}</p>
            <span class="notif-time">{{ n.createTime }}</span>
          </div>
        </template>
        <el-empty v-else description="暂无通知" :image-size="80" />
      </div>
      <template #footer>
        <el-button v-if="hasUnreadNotifs" type="primary" text @click="markAllNotifsRead">全部已读</el-button>
        <el-button @click="showNotificationDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import {
  Clock,
  UserFilled,
  TrendCharts,
  DocumentChecked,
  Calendar,
  Document,
  Setting,
  Bell,
  ArrowRight,
} from '@element-plus/icons-vue'

export default {
  components: {
    Clock,
    UserFilled,
    TrendCharts,
    DocumentChecked,
    Calendar,
    Document,
    Setting,
    Bell,
    ArrowRight,
  },
}
</script>

<style scoped>
.square-page {
  padding: 24px 28px;
  min-height: 100%;
  background: #f5f7fa;
}

/* ---- Header ---- */
.square-header {
  text-align: center;
  margin-bottom: 36px;
  animation: fadeInDown 0.6s ease-out;
}
@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-24px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.square-title {
  font-size: 26px;
  color: #1d9e75;
  margin: 0 0 8px;
  font-weight: 700;
  letter-spacing: 2px;
}
.square-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

/* ---- Card grid ---- */
.card-grid {
  max-width: 960px;
  margin: 0 auto;
}
.card-col {
  margin-bottom: 20px;
  animation: fadeInUp 0.5s ease-out both;
}
.card-col:nth-child(1) { animation-delay: 0.05s; }
.card-col:nth-child(2) { animation-delay: 0.1s; }
.card-col:nth-child(3) { animation-delay: 0.15s; }
.card-col:nth-child(4) { animation-delay: 0.2s; }
.card-col:nth-child(5) { animation-delay: 0.25s; }
.card-col:nth-child(6) { animation-delay: 0.3s; }
.card-col:nth-child(7) { animation-delay: 0.35s; }
.card-col:nth-child(8) { animation-delay: 0.4s; }
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ---- Individual card ---- */
.nav-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 28px 20px;
  background: #fff;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #ebeef5;
  position: relative;
  overflow: hidden;
  user-select: none;
}
.nav-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #1d9e75, #378add);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.3s ease;
}
.nav-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(29, 158, 117, 0.15);
  border-color: #c6e2d8;
}
.nav-card:hover::before {
  transform: scaleX(1);
}

.card-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.3s ease;
}
.nav-card:hover .card-icon-wrap {
  transform: scale(1.08);
}

.card-content {
  flex: 1;
  min-width: 0;
  text-align: center;
}
.card-title {
  font-size: 16px;
  color: #303133;
  margin: 0 0 4px;
  font-weight: 600;
}
.card-desc {
  font-size: 12px;
  color: #909399;
  margin: 0;
  line-height: 1.4;
}

.card-arrow {
  display: none;
}

/* ---- Dialogs ---- */
.dialog-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px 0;
  gap: 12px;
  text-align: center;
}
.dialog-placeholder p {
  font-size: 16px;
  color: #606266;
  margin: 0;
}
.dialog-placeholder .placeholder-hint {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

/* Responsive adjustments */
/* Notification items */
.notif-item {
  padding: 12px;
  border-bottom: 1px solid #ebeef5;
}
.notif-item.unread {
  background-color: #f0f9eb;
}
.notif-item .notif-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.notif-item .notif-title {
  font-weight: 600;
  font-size: 14px;
}
.notif-item .notif-content {
  margin: 6px 0;
  font-size: 13px;
  color: #606266;
}
.notif-item .notif-time {
  font-size: 12px;
  color: #c0c4cc;
}

@media (max-width: 768px) {
  .square-page {
    padding: 16px;
  }
  .square-header {
    margin-bottom: 24px;
  }
  .square-title {
    font-size: 22px;
  }
  .nav-card {
    padding: 16px;
  }
  .card-icon-wrap {
    width: 44px;
    height: 44px;
    border-radius: 10px;
  }
}
</style>
