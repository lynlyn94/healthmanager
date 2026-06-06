<template>
  <div class="patient-detail-page">
    <!-- 患者基本信息卡片 -->
    <el-card class="info-card" shadow="never" v-loading="infoLoading">
      <template #header>
        <div class="info-card-header">
          <span>患者基本信息</span>
          <el-tag :type="patient.status === 'IN_HOSPITAL' ? 'success' : 'info'" size="small" disable-transitions>
            {{ statusLabel(patient.status) }}
          </el-tag>
        </div>
      </template>
      <el-skeleton v-if="infoLoading" :rows="4" animated />
      <el-descriptions v-else :column="4" border size="small">
        <el-descriptions-item label="姓名">{{ patient.name || '--' }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ genderLabel(patient.gender) }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ patient.age ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="住院号">{{ patient.inpatientNo || '--' }}</el-descriptions-item>
        <el-descriptions-item label="床号">{{ patient.bedNo || '--' }}</el-descriptions-item>
        <el-descriptions-item label="入院日期">{{ patient.admissionDate || '--' }}</el-descriptions-item>
        <el-descriptions-item label="诊断" :span="2">{{ patient.diagnosis || '--' }}</el-descriptions-item>
        <el-descriptions-item label="过敏史">{{ patient.allergyHistory || '无' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ patient.contactPhone || '--' }}</el-descriptions-item>
        <el-descriptions-item label="紧急联系人">{{ patient.emergencyContact || '--' }}</el-descriptions-item>
        <el-descriptions-item label="紧急电话">{{ patient.emergencyPhone || '--' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 流程进度条 -->
    <el-card class="workflow-card" shadow="never">
      <el-steps :active="workflowStep" align-center finish-status="success">
        <el-step title="量表评估" description="康复评定" />
        <el-step title="治疗目标" description="设定目标" />
        <el-step title="治疗方案" description="制定计划" />
        <el-step title="提交审阅" description="医生审核" />
        <el-step title="医嘱下达" description="执行治疗" />
      </el-steps>
    </el-card>

    <!-- 9 项操作标签页 -->
    <el-card class="tabs-card" shadow="never">
      <el-tabs v-model="activeTab" @tab-change="onTabChange" type="border-card">
        <!-- 1. 患者详情 -->
        <el-tab-pane label="患者详情" name="detail">
          <div v-loading="tabLoading.detail">
            <el-form :model="detailForm" label-width="90px" size="default">
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="姓名">
                    <el-input v-model="detailForm.name" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="性别">
                    <el-radio-group v-model="detailForm.gender">
                      <el-radio :value="1">男</el-radio>
                      <el-radio :value="0">女</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="年龄">
                    <el-input-number v-model="detailForm.age" :min="0" :max="200" style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="住院号">
                    <el-input v-model="detailForm.inpatientNo" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="床号">
                    <el-input v-model="detailForm.bedNo" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="入院日期">
                    <el-date-picker
                      v-model="detailForm.admissionDate"
                      type="date"
                      value-format="YYYY-MM-DD"
                      style="width: 100%"
                    />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="诊断">
                <el-input v-model="detailForm.diagnosis" type="textarea" :rows="2" />
              </el-form-item>
              <el-form-item label="过敏史">
                <el-input v-model="detailForm.allergyHistory" type="textarea" :rows="2" />
              </el-form-item>
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="联系电话">
                    <el-input v-model="detailForm.contactPhone" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="紧急联系人">
                    <el-input v-model="detailForm.emergencyContact" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="紧急电话">
                    <el-input v-model="detailForm.emergencyPhone" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="备注">
                <el-input v-model="detailForm.remark" type="textarea" :rows="2" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="savingDetail" @click="saveDetail">
                  保存修改
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- 2. 编辑患者 -->
        <el-tab-pane label="编辑患者" name="edit">
          <div v-loading="tabLoading.edit">
            <el-empty v-if="!patient.id" description="患者信息未加载" />
            <template v-else>
              <div class="tab-toolbar">
                <el-button type="primary" @click="openEditDialog">
                  <el-icon><Edit /></el-icon>
                  编辑患者信息
                </el-button>
              </div>
              <el-descriptions :column="3" border size="small" style="margin-top: 16px">
                <el-descriptions-item label="姓名">{{ patient.name }}</el-descriptions-item>
                <el-descriptions-item label="性别">{{ genderLabel(patient.gender) }}</el-descriptions-item>
                <el-descriptions-item label="年龄">{{ patient.age ?? '--' }}</el-descriptions-item>
                <el-descriptions-item label="住院号">{{ patient.inpatientNo }}</el-descriptions-item>
                <el-descriptions-item label="床号">{{ patient.bedNo }}</el-descriptions-item>
                <el-descriptions-item label="入院日期">{{ patient.admissionDate }}</el-descriptions-item>
                <el-descriptions-item label="诊断" :span="3">{{ patient.diagnosis }}</el-descriptions-item>
                <el-descriptions-item label="过敏史" :span="3">{{ patient.allergyHistory || '无' }}</el-descriptions-item>
                <el-descriptions-item label="联系电话">{{ patient.contactPhone }}</el-descriptions-item>
                <el-descriptions-item label="紧急联系人">{{ patient.emergencyContact }}</el-descriptions-item>
                <el-descriptions-item label="紧急电话">{{ patient.emergencyPhone }}</el-descriptions-item>
                <el-descriptions-item label="备注" :span="3">{{ patient.remark || '--' }}</el-descriptions-item>
              </el-descriptions>
            </template>
          </div>
        </el-tab-pane>

        <!-- 3. 量表评估 -->
        <el-tab-pane label="量表评估" name="assessment">
          <div v-loading="tabLoading.assessment">
            <div class="tab-toolbar">
              <el-button type="primary" @click="openAssessmentDialog">
                <el-icon><Plus /></el-icon>
                新建评估
              </el-button>
            </div>
            <el-table :data="assessmentList" stripe border style="width: 100%; margin-top: 12px" size="small">
              <el-table-column prop="templateName" label="量表名称" min-width="140" />
              <el-table-column prop="assessorName" label="评估人" width="100" />
              <el-table-column label="评估日期" width="110">
                <template #default="{ row }">{{ row.assessDate }}</template>
              </el-table-column>
              <el-table-column prop="totalScore" label="总分" width="80" align="center" />
              <el-table-column prop="conclusion" label="结论" min-width="160" show-overflow-tooltip />
              <el-table-column label="操作" width="80" align="center">
                <template #default="{ row }">
                  <el-popconfirm title="确认删除该评估记录？" @confirm="deleteAssessment(row.id)">
                    <template #reference>
                      <el-button type="danger" link size="small">删除</el-button>
                    </template>
                  </el-popconfirm>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!assessmentList.length && !tabLoading.assessment" description="暂无评估记录" />
          </div>
        </el-tab-pane>

        <!-- 4. 治疗目标 -->
        <el-tab-pane label="治疗目标" name="goal">
          <div v-loading="tabLoading.goal">
            <div class="tab-toolbar">
              <el-button type="primary" @click="openGoalDialog()">
                <el-icon><Plus /></el-icon>
                新建目标
              </el-button>
            </div>
            <el-table :data="goalList" stripe border style="width: 100%; margin-top: 12px" size="small">
              <el-table-column prop="goalType" label="目标类型" width="100" />
              <el-table-column prop="content" label="目标内容" min-width="200" show-overflow-tooltip />
              <el-table-column label="目标日期" width="110">
                <template #default="{ row }">{{ row.targetDate }}</template>
              </el-table-column>
              <el-table-column label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'ACHIEVED' ? 'success' : row.status === 'IN_PROGRESS' ? 'warning' : 'info'" size="small">
                    {{ row.status === 'ACHIEVED' ? '已达成' : row.status === 'IN_PROGRESS' ? '进行中' : '已放弃' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" align="center">
                <template #default="{ row }">
                  <el-button type="primary" link size="small" @click="openGoalDialog(row)">编辑</el-button>
                  <el-popconfirm title="确认删除？" @confirm="deleteGoal(row.id)">
                    <template #reference>
                      <el-button type="danger" link size="small">删除</el-button>
                    </template>
                  </el-popconfirm>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!goalList.length && !tabLoading.goal" description="暂无治疗目标" />
          </div>
        </el-tab-pane>

        <!-- 5. 治疗方案 -->
        <el-tab-pane label="治疗方案" name="plan">
          <div v-loading="tabLoading.plan">
            <div class="tab-toolbar">
              <el-button type="primary" @click="openPlanDialog()">
                <el-icon><Plus /></el-icon>
                新建方案
              </el-button>
            </div>
            <el-table :data="planList" stripe border style="width: 100%; margin-top: 12px" size="small">
              <el-table-column prop="planName" label="方案名称" min-width="140" />
              <el-table-column label="治疗项目" min-width="160" show-overflow-tooltip>
                <template #default="{ row }">{{ formatTreatmentItems(row.treatmentItems) }}</template>
              </el-table-column>
              <el-table-column prop="frequency" label="频次" width="80" />
              <el-table-column prop="dailyCount" label="日次" width="60" align="center" />
              <el-table-column label="开始日期" width="110">
                <template #default="{ row }">{{ row.periodStart }}</template>
              </el-table-column>
              <el-table-column label="结束日期" width="110">
                <template #default="{ row }">{{ row.periodEnd }}</template>
              </el-table-column>
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="planStatusTag(row.status)" size="small">{{ planStatusLabel(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="160" align="center">
                <template #default="{ row }">
                  <el-button type="primary" link size="small" @click="openPlanDialog(row)">编辑</el-button>
                  <el-button
                    v-if="row.status === 'DRAFT'"
                    type="success"
                    link
                    size="small"
                    @click="submitPlan(row.id)"
                  >
                    提交
                  </el-button>
                  <el-popconfirm title="确认删除？" @confirm="deletePlan(row.id)">
                    <template #reference>
                      <el-button type="danger" link size="small">删除</el-button>
                    </template>
                  </el-popconfirm>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!planList.length && !tabLoading.plan" description="暂无治疗方案" />
          </div>
        </el-tab-pane>

        <!-- 6. 患者医嘱 -->
        <el-tab-pane label="患者医嘱" name="order">
          <div v-loading="tabLoading.order">
            <el-table :data="orderList" stripe border style="width: 100%" size="small">
              <el-table-column prop="orderType" label="医嘱类型" width="100" />
              <el-table-column prop="treatmentItem" label="治疗项目" min-width="150" show-overflow-tooltip />
              <el-table-column prop="frequency" label="频次" width="80" />
              <el-table-column prop="dailyCount" label="日次" width="60" align="center" />
              <el-table-column label="开始" width="110">
                <template #default="{ row }">{{ row.periodStart }}</template>
              </el-table-column>
              <el-table-column label="结束" width="110">
                <template #default="{ row }">{{ row.periodEnd }}</template>
              </el-table-column>
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'APPROVED' ? 'success' : row.status === 'DRAFT' ? 'info' : ''" size="small">{{ orderStatusLabel(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="doctorName" label="开嘱医生" width="100" />
              <el-table-column prop="note" label="备注" min-width="120" show-overflow-tooltip />
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                  <template v-if="row.status === 'APPROVED'">
                    <el-button type="primary" size="small" @click="openCustomScheduleFromOrder(row)">自定义排程</el-button>
                    <el-button type="success" size="small" @click="generateTasksFromOrderBtn(row)">自动排程</el-button>
                  </template>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!orderList.length && !tabLoading.order" description="暂无医嘱" />
          </div>
        </el-tab-pane>

        <!-- 7. 患者日程 -->
        <el-tab-pane label="患者日程" name="schedule">
          <div v-loading="tabLoading.schedule">
            <div class="schedule-container">
              <!-- 简易月历 -->
              <div class="mini-calendar">
                <div class="calendar-header">
                  <el-button size="small" circle @click="prevMonth"><el-icon><ArrowLeft /></el-icon></el-button>
                  <span class="calendar-title">{{ calendarYear }}年 {{ calendarMonth }}月</span>
                  <el-button size="small" circle @click="nextMonth"><el-icon><ArrowRight /></el-icon></el-button>
                </div>
                <div class="calendar-weekdays">
                  <span v-for="d in weekDays" :key="d" class="weekday">{{ d }}</span>
                </div>
                <div class="calendar-grid">
                  <div
                    v-for="(day, i) in calendarDays"
                    :key="i"
                    class="calendar-day"
                    :class="{
                      'is-today': day.isToday,
                      'has-events': day.hasEvents,
                      'is-selected': day.dateStr === selectedDate,
                      'is-other-month': day.isOtherMonth,
                    }"
                    @click="selectDate(day)"
                  >
                    <span class="day-number">{{ day.day }}</span>
                    <span v-if="day.hasEvents" class="event-dot" />
                  </div>
                </div>
              </div>

              <!-- 选中日期的日程列表 -->
              <div class="schedule-list">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <h4 style="margin: 0;">{{ selectedDate ? selectedDate + ' 日程' : '请选择日期' }}</h4>
                  <div style="display: flex; gap: 8px;">
                    <el-button
                      v-if="orderList.some(o => o.status === 'APPROVED')"
                      type="success"
                      size="small"
                      :loading="generatingTasks"
                      @click="batchGenerateAllOrders"
                    >
                      一键排程全部医嘱
                    </el-button>
                  </div>
                </div>
                <el-timeline v-if="filteredSchedules.length">
                  <el-timeline-item
                    v-for="item in filteredSchedules"
                    :key="item.id"
                    :timestamp="item.timeSlot"
                    placement="top"
                    size="small"
                  >
                    <div class="timeline-content">
                      <el-tag size="small" :type="item.status === 'COMPLETED' ? 'success' : 'warning'">
                        {{ item.status }}
                      </el-tag>
                      <span class="timeline-title">{{ item.title }}</span>
                    </div>
                    <p v-if="item.description" class="timeline-desc">{{ item.description }}</p>
                  </el-timeline-item>
                </el-timeline>
                <el-empty v-else description="该日期暂无日程" :image-size="80" />
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 8. 治疗记录 -->
        <el-tab-pane label="治疗记录" name="record">
          <div v-loading="tabLoading.record">
            <el-table :data="recordList" stripe border style="width: 100%" size="small">
              <el-table-column label="日期" width="110" prop="treatmentDate" />
              <el-table-column prop="treatmentItem" label="治疗项目" min-width="140" />
              <el-table-column prop="therapistName" label="治疗师" width="100" />
              <el-table-column label="时间" width="80" prop="timeSlot" />
              <el-table-column label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'COMPLETED' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="note" label="备注" min-width="160" show-overflow-tooltip />
            </el-table>
            <el-empty v-if="!recordList.length && !tabLoading.record" description="暂无治疗记录" />
          </div>
        </el-tab-pane>

        <!-- 9. 患者出院 -->
        <el-tab-pane label="患者出院" name="discharge">
          <div v-loading="tabLoading.discharge">
            <el-result
              v-if="patient.status === 'DISCHARGED'"
              icon="success"
              title="该患者已出院"
              :sub-title="'出院日期：' + (patient.dischargeDate || '--')"
            />
            <template v-else>
              <el-alert
                title="出院操作说明"
                type="warning"
                description="执行出院操作后，患者状态将变更为「已出院」，相关治疗任务将被终止。请在确认所有治疗完成后执行此操作。"
                show-icon
                :closable="false"
                style="margin-bottom: 24px"
              />
              <div style="text-align: center">
                <el-popconfirm
                  title="确认该患者已满足出院条件，执行出院？"
                  confirm-button-text="确认出院"
                  cancel-button-text="取消"
                  @confirm="handleDischarge"
                >
                  <template #reference>
                    <el-button type="danger" size="large" :loading="discharging">
                      <el-icon><SwitchButton /></el-icon>
                      办理出院
                    </el-button>
                  </template>
                </el-popconfirm>
              </div>
            </template>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- ========== 弹窗 ========== -->

    <!-- 编辑患者弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑患者" width="620px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editFormRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="editForm.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="editForm.gender">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="0">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="年龄">
              <el-input-number v-model="editForm.age" :min="0" :max="200" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="住院号" prop="inpatientNo">
              <el-input v-model="editForm.inpatientNo" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="床号" prop="bedNo">
              <el-input v-model="editForm.bedNo" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入院日期">
              <el-date-picker v-model="editForm.admissionDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="诊断" prop="diagnosis">
          <el-input v-model="editForm.diagnosis" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="过敏史">
          <el-input v-model="editForm.allergyHistory" type="textarea" :rows="2" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="editForm.contactPhone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急联系人">
              <el-input v-model="editForm.emergencyContact" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="紧急电话">
              <el-input v-model="editForm.emergencyPhone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注">
              <el-input v-model="editForm.remark" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingEdit" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新建/编辑评估弹窗 -->
    <el-dialog v-model="assessmentDialogVisible" title="新建量表评估" width="520px" destroy-on-close>
      <el-form ref="assessmentFormRef" :model="assessmentForm" :rules="assessmentFormRules" label-width="90px">
        <el-form-item label="量表模板" prop="templateId">
          <el-select v-model="assessmentForm.templateId" placeholder="请选择量表模板" style="width: 100%">
            <el-option
              v-for="t in assessmentTemplates"
              :key="t.id"
              :label="t.templateName + ' (' + t.abbreviation + ')'"
              :value="t.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="评估日期" prop="assessDate">
          <el-date-picker v-model="assessmentForm.assessDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="总分" prop="totalScore">
          <el-input-number v-model="assessmentForm.totalScore" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="评分详情">
          <el-input v-model="assessmentForm.detail" type="textarea" :rows="3" placeholder="JSON 评分明细" />
        </el-form-item>
        <el-form-item label="结论">
          <el-input v-model="assessmentForm.conclusion" type="textarea" :rows="2" placeholder="评估结论" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assessmentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingAssessment" @click="saveAssessment">保存</el-button>
      </template>
    </el-dialog>

    <!-- 治疗目标弹窗 -->
    <el-dialog v-model="goalDialogVisible" :title="editingGoal?.id ? '编辑治疗目标' : '新建治疗目标'" width="500px" destroy-on-close>
      <el-form ref="goalFormRef" :model="goalForm" :rules="goalFormRules" label-width="90px">
        <el-form-item label="目标类型" prop="goalType">
          <el-select v-model="goalForm.goalType" placeholder="请选择类型" style="width: 100%">
            <el-option label="短期目标" value="SHORT_TERM" />
            <el-option label="长期目标" value="LONG_TERM" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标内容" prop="content">
          <el-input v-model="goalForm.content" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="目标日期" prop="targetDate">
          <el-date-picker v-model="goalForm.targetDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="goalForm.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已达成" value="ACHIEVED" />
            <el-option label="已放弃" value="ABANDONED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="goalDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingGoal" @click="saveGoal">保存</el-button>
      </template>
    </el-dialog>

    <!-- 治疗方案弹窗 -->
    <el-dialog v-model="planDialogVisible" :title="editingPlan?.id ? '编辑治疗方案' : '新建治疗方案'" width="520px" destroy-on-close>
      <el-form ref="planFormRef" :model="planForm" :rules="planFormRules" label-width="90px">
        <el-form-item label="方案名称" prop="planName">
          <el-input v-model="planForm.planName" />
        </el-form-item>
        <el-form-item label="治疗项目" prop="treatmentItems">
          <el-input v-model="planForm.treatmentItems" type="textarea" :rows="2" placeholder="多个项目用逗号分隔" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="频次" prop="frequency">
              <el-select v-model="planForm.frequency" style="width: 100%">
                <el-option label="每日" value="DAILY" />
                <el-option label="每周" value="WEEKLY" />
                <el-option label="每月" value="MONTHLY" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="每日次数" prop="dailyCount">
              <el-input-number v-model="planForm.dailyCount" :min="1" :max="10" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始日期" prop="periodStart">
              <el-date-picker v-model="planForm.periodStart" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期" prop="periodEnd">
              <el-date-picker v-model="planForm.periodEnd" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="planDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPlan" @click="savePlan">保存</el-button>
      </template>
    </el-dialog>

    <!-- 自定义排程弹窗 -->
    <el-dialog v-model="scheduleDialogVisible" title="自定义排程" width="560px" destroy-on-close @open="initCustomSchedule">
      <el-form ref="scheduleFormRef" :model="scheduleForm" :rules="scheduleFormRules" label-width="100px">
        <el-form-item label="患者">
          <el-input :value="patient.name" disabled />
        </el-form-item>
        <el-form-item label="治疗师" prop="therapistId">
          <el-select v-model="scheduleForm.therapistId" placeholder="选择治疗师" filterable style="width: 100%">
            <el-option v-for="t in therapistOptions" :key="t.id" :label="t.realName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" prop="taskDate">
          <el-date-picker v-model="scheduleForm.taskDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="时间段" prop="timeSlot">
          <el-select v-model="scheduleForm.timeSlot" placeholder="选择时间段" style="width: 100%">
            <el-option v-for="slot in hourlySlots" :key="slot.value" :label="slot.label" :value="slot.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="治疗项目" prop="treatmentItem">
          <el-select v-model="scheduleForm.treatmentItem" placeholder="选择或输入" filterable allow-create style="width: 100%">
            <el-option v-for="item in treatmentItemOptions2" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联医嘱">
          <el-select v-model="scheduleForm.orderId" placeholder="可选" clearable style="width: 100%">
            <el-option v-for="o in orderList" :key="o.id" :label="`${o.treatmentItem} (${o.periodStart}~${o.periodEnd})`" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-alert v-if="conflictMsg" :title="conflictMsg" type="warning" show-icon :closable="false" style="margin-top: 8px" />
      </el-form>
      <template #footer>
        <el-button @click="scheduleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="schedulingCustom" @click="submitCustomSchedule">确认排程</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Edit, ArrowLeft, ArrowRight, SwitchButton } from '@element-plus/icons-vue'
import { get, post, put, del } from '@/api'
import type {
  Patient,
  PageResult,
  Assessment,
  AssessmentTemplate,
  TreatmentGoal,
  TreatmentPlan,
  MedicalOrder,
  PatientSchedule,
} from '@/api/types'

const route = useRoute()
const patientId = computed(() => Number(route.params.id))

// ---------- 患者信息 ----------
const infoLoading = ref(false)
const patient = ref<Partial<Patient>>({})

async function fetchPatient() {
  infoLoading.value = true
  try {
    const res = await get<Patient>(`/patients/${patientId.value}`)
    patient.value = (res as any).data ?? res
    syncDetailForm()
  } finally {
    infoLoading.value = false
  }
}

fetchPatient()

// ---------- 标签页 ----------
const activeTab = ref('detail')

// 每个 tab 的激活标记（懒加载）
const loadedTabs = reactive<Record<string, boolean>>({
  detail: false,
  edit: false,
  assessment: false,
  goal: false,
  plan: false,
  order: false,
  schedule: false,
  record: false,
  discharge: false,
})

const tabLoading = reactive<Record<string, boolean>>({
  detail: false,
  edit: false,
  assessment: false,
  goal: false,
  plan: false,
  order: false,
  schedule: false,
  record: false,
  discharge: false,
})

function onTabChange(name: string) {
  if (loadedTabs[name]) return
  loadedTabs[name] = true

  switch (name) {
    case 'detail':
      syncDetailForm()
      break
    case 'assessment':
      fetchAssessments()
      break
    case 'goal':
      fetchGoals()
      break
    case 'plan':
      fetchPlans()
      break
    case 'order':
      fetchOrders()
      break
    case 'schedule':
      fetchSchedules()
      break
    case 'record':
      fetchRecords()
      break
    // edit / discharge 不需要异步加载数据
  }
}

// ---------- 工具函数 ----------
function orderStatusLabel(s: string) {
  const map: Record<string, string> = { DRAFT: '草稿', PENDING_REVIEW: '待审核', APPROVED: '已通过', REJECTED: '已退回', CANCELLED: '已作废' }
  return map[s] || s
}

async function generateTasksFromOrderBtn(order: MedicalOrder) {
  try {
    await ElMessageBox.confirm(
      `确认为医嘱「${order.treatmentItem}」排程生成任务？`,
      '确认排程',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'info' },
    )
  } catch { return }
  try {
    const res = await post<any>(`/orders/${order.id}/generate-tasks`)
    const list = (res as any).data ?? res
    ElMessage.success(`已生成 ${Array.isArray(list) ? list.length : 0} 条治疗任务`)
    fetchOrders()
    fetchSchedules()
  } catch { /* handled */ }
}

function genderLabel(g: number | undefined) {
  return g === 1 ? '男' : g === 0 ? '女' : '--'
}

function statusLabel(s: string | undefined) {
  return s === 'IN_HOSPITAL' ? '在院' : s === 'DISCHARGED' ? '已出院' : '--'
}

function planStatusTag(s: string) {
  if (s === 'DRAFT') return 'info'
  if (s === 'SUBMITTED') return 'warning'
  if (s === 'APPROVED') return ''
  if (s === 'ORDERED') return 'success'
  if (s === 'REVIEWED') return 'success'
  return ''
}

function planStatusLabel(s: string) {
  const map: Record<string, string> = { DRAFT: '草稿', SUBMITTED: '已提交', APPROVED: '已审核(待开医嘱)', ORDERED: '已开医嘱', REVIEWED: '已审阅' }
  return map[s] || s
}

function formatTreatmentItems(val: string): string {
  if (!val) return '-'
  try {
    const arr = JSON.parse(val)
    if (Array.isArray(arr)) return arr.join('、')
  } catch {}
  return val
}

function parseTreatmentItemsForEdit(val: string): string {
  if (!val) return ''
  try {
    const arr = JSON.parse(val)
    if (Array.isArray(arr)) return arr.join(', ')
  } catch {}
  return val
}

// Workflow step: 0=未开始, 1=已评估, 2=已设目标, 3=已定方案, 4=已提交/审阅, 5=已有医嘱
const workflowStep = computed(() => {
  if (orderList.value.length > 0) return 5
  if (planList.value.some(p => p.status === 'ORDERED')) return 5
  if (planList.value.some(p => p.status === 'REVIEWED' || p.status === 'APPROVED')) return 4
  if (planList.value.some(p => p.status === 'SUBMITTED')) return 4
  if (planList.value.length > 0) return 3
  if (goalList.value.length > 0) return 2
  if (assessmentList.value.length > 0) return 1
  return 0
})

// ==================== Tab 1: 患者详情（内联编辑）====================
const detailForm = reactive<Partial<Patient>>({})
const savingDetail = ref(false)

function syncDetailForm() {
  Object.assign(detailForm, {
    name: patient.value.name ?? '',
    gender: patient.value.gender ?? 1,
    age: patient.value.age ?? null,
    inpatientNo: patient.value.inpatientNo ?? '',
    bedNo: patient.value.bedNo ?? '',
    admissionDate: patient.value.admissionDate ?? '',
    diagnosis: patient.value.diagnosis ?? '',
    allergyHistory: patient.value.allergyHistory ?? '',
    contactPhone: patient.value.contactPhone ?? '',
    emergencyContact: patient.value.emergencyContact ?? '',
    emergencyPhone: patient.value.emergencyPhone ?? '',
    remark: patient.value.remark ?? '',
  })
}

async function saveDetail() {
  savingDetail.value = true
  try {
    await put(`/patients/${patientId.value}`, detailForm)
    ElMessage.success('保存成功')
    fetchPatient()
  } finally {
    savingDetail.value = false
  }
}

// ==================== Tab 2: 编辑患者弹窗 ====================
const editDialogVisible = ref(false)
const editFormRef = ref<FormInstance>()
const savingEdit = ref(false)

const editForm = reactive<Partial<Patient>>({})

const editFormRules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  inpatientNo: [{ required: true, message: '请输入住院号', trigger: 'blur' }],
  bedNo: [{ required: true, message: '请输入床号', trigger: 'blur' }],
  diagnosis: [{ required: true, message: '请输入诊断', trigger: 'blur' }],
}

function openEditDialog() {
  Object.assign(editForm, {
    name: patient.value.name ?? '',
    gender: patient.value.gender ?? 1,
    age: patient.value.age ?? null,
    inpatientNo: patient.value.inpatientNo ?? '',
    bedNo: patient.value.bedNo ?? '',
    admissionDate: patient.value.admissionDate ?? '',
    diagnosis: patient.value.diagnosis ?? '',
    allergyHistory: patient.value.allergyHistory ?? '',
    contactPhone: patient.value.contactPhone ?? '',
    emergencyContact: patient.value.emergencyContact ?? '',
    emergencyPhone: patient.value.emergencyPhone ?? '',
    remark: patient.value.remark ?? '',
  })
  editDialogVisible.value = true
}

async function saveEdit() {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return
  savingEdit.value = true
  try {
    await put(`/patients/${patientId.value}`, editForm)
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    fetchPatient()
  } finally {
    savingEdit.value = false
  }
}

// ==================== Tab 3: 量表评估 ====================
const assessmentList = ref<Assessment[]>([])
const assessmentTemplates = ref<AssessmentTemplate[]>([])

async function fetchAssessments() {
  tabLoading.assessment = true
  try {
    const res = await get<any>('/assessments', { patientId: patientId.value })
    const data = res.data ?? res
    assessmentList.value = Array.isArray(data) ? data : (data.records ?? [])
  } finally {
    tabLoading.assessment = false
  }
}

async function fetchAssessmentTemplates() {
  const res = await get<any>('/assessments/templates')
  const data = res.data ?? res
  assessmentTemplates.value = Array.isArray(data) ? data : (data.records ?? [])
}

const assessmentDialogVisible = ref(false)
const assessmentFormRef = ref<FormInstance>()
const savingAssessment = ref(false)
const assessmentForm = reactive<Partial<Assessment>>({
  patientId: patientId.value,
  templateId: undefined as any,
  assessDate: '',
  totalScore: 0,
  detail: '',
  conclusion: '',
})

const assessmentFormRules: FormRules = {
  templateId: [{ required: true, message: '请选择量表', trigger: 'change' }],
  assessDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  totalScore: [{ required: true, message: '请输入总分', trigger: 'blur' }],
}

function openAssessmentDialog() {
  if (!assessmentTemplates.value.length) {
    fetchAssessmentTemplates()
  }
  assessmentForm.patientId = patientId.value
  assessmentForm.templateId = undefined as any
  assessmentForm.assessDate = ''
  assessmentForm.totalScore = 0
  assessmentForm.detail = ''
  assessmentForm.conclusion = ''
  assessmentDialogVisible.value = true
}

async function saveAssessment() {
  const valid = await assessmentFormRef.value?.validate().catch(() => false)
  if (!valid) return
  savingAssessment.value = true
  try {
    await post('/assessments', assessmentForm)
    ElMessage.success('评估创建成功')
    assessmentDialogVisible.value = false
    fetchAssessments()
  } finally {
    savingAssessment.value = false
  }
}

async function deleteAssessment(id: number | undefined) {
  if (!id) return
  try {
    await del(`/assessments/${id}`)
    ElMessage.success('已删除')
    fetchAssessments()
  } catch {
    // handled by interceptor
  }
}

// ==================== Tab 4: 治疗目标 ====================
const goalList = ref<TreatmentGoal[]>([])

async function fetchGoals() {
  tabLoading.goal = true
  try {
    const res = await get<any>(`/patients/${patientId.value}/goals`)
    const data = res.data ?? res
    goalList.value = Array.isArray(data) ? data : (data.records ?? [])
  } finally {
    tabLoading.goal = false
  }
}

const goalDialogVisible = ref(false)
const goalFormRef = ref<FormInstance>()
const savingGoal = ref(false)
const editingGoal = ref<Partial<TreatmentGoal>>({})

const goalForm = reactive<Partial<TreatmentGoal>>({
  patientId: patientId.value,
  goalType: 'SHORT_TERM',
  content: '',
  targetDate: '',
  status: 'IN_PROGRESS',
})

const goalFormRules: FormRules = {
  goalType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
  targetDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

function openGoalDialog(row?: TreatmentGoal) {
  if (row) {
    editingGoal.value = row
    Object.assign(goalForm, {
      patientId: patientId.value,
      goalType: row.goalType,
      content: row.content,
      targetDate: row.targetDate,
      status: row.status,
    })
  } else {
    editingGoal.value = {}
    Object.assign(goalForm, {
      patientId: patientId.value,
      goalType: 'SHORT_TERM',
      content: '',
      targetDate: '',
      status: 'IN_PROGRESS',
    })
  }
  goalDialogVisible.value = true
}

async function saveGoal() {
  const valid = await goalFormRef.value?.validate().catch(() => false)
  if (!valid) return
  savingGoal.value = true
  try {
    if (editingGoal.value.id) {
      await put(`/goals/${editingGoal.value.id}`, goalForm)
      ElMessage.success('更新成功')
    } else {
      await post(`/patients/${patientId.value}/goals`, goalForm)
      ElMessage.success('创建成功')
    }
    goalDialogVisible.value = false
    fetchGoals()
  } finally {
    savingGoal.value = false
  }
}

async function deleteGoal(id: number | undefined) {
  if (!id) return
  try {
    await del(`/goals/${id}`)
    ElMessage.success('已删除')
    fetchGoals()
  } catch {
    // handled by interceptor
  }
}

// ==================== Tab 5: 治疗方案 ====================
const planList = ref<TreatmentPlan[]>([])

async function fetchPlans() {
  tabLoading.plan = true
  try {
    const res = await get<any>('/treatment-plans', { patientId: patientId.value })
    const data = res.data ?? res
    planList.value = Array.isArray(data) ? data : (data.records ?? [])
  } finally {
    tabLoading.plan = false
  }
}

const planDialogVisible = ref(false)
const planFormRef = ref<FormInstance>()
const savingPlan = ref(false)
const editingPlan = ref<Partial<TreatmentPlan>>({})
const planForm = reactive<Partial<TreatmentPlan>>({
  patientId: patientId.value,
  planName: '',
  treatmentItems: '',
  frequency: 'DAILY',
  dailyCount: 1,
  periodStart: '',
  periodEnd: '',
})

const planFormRules: FormRules = {
  planName: [{ required: true, message: '请输入方案名称', trigger: 'blur' }],
  treatmentItems: [{ required: true, message: '请输入治疗项目', trigger: 'blur' }],
  frequency: [{ required: true, message: '请选择频次', trigger: 'change' }],
  dailyCount: [{ required: true, message: '请输入次数', trigger: 'blur' }],
  periodStart: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  periodEnd: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
}

function openPlanDialog(row?: TreatmentPlan) {
  if (row) {
    editingPlan.value = row
    Object.assign(planForm, {
      patientId: patientId.value,
      planName: row.planName,
      treatmentItems: parseTreatmentItemsForEdit(row.treatmentItems),
      frequency: row.frequency,
      dailyCount: row.dailyCount,
      periodStart: row.periodStart,
      periodEnd: row.periodEnd,
    })
  } else {
    editingPlan.value = {}
    Object.assign(planForm, {
      patientId: patientId.value,
      planName: '',
      treatmentItems: '',
      frequency: 'DAILY',
      dailyCount: 1,
      periodStart: '',
      periodEnd: '',
    })
  }
  planDialogVisible.value = true
}

async function savePlan() {
  const valid = await planFormRef.value?.validate().catch(() => false)
  if (!valid) return
  savingPlan.value = true
  try {
    if (editingPlan.value.id) {
      await put(`/treatment-plans/${editingPlan.value.id}`, planForm)
      ElMessage.success('更新成功')
    } else {
      await post('/treatment-plans', planForm)
      ElMessage.success('创建成功')
    }
    planDialogVisible.value = false
    fetchPlans()
  } finally {
    savingPlan.value = false
  }
}

async function submitPlan(id: number | undefined) {
  if (!id) return
  try {
    await post(`/treatment-plans/${id}/submit`)
    ElMessage.success('方案已提交')
    fetchPlans()
  } catch {
    // handled by interceptor
  }
}

async function deletePlan(id: number | undefined) {
  if (!id) return
  try {
    await del(`/treatment-plans/${id}`)
    ElMessage.success('已删除')
    fetchPlans()
  } catch {
    // handled by interceptor
  }
}

// ==================== Tab 6: 患者医嘱（只读）====================
const orderList = ref<MedicalOrder[]>([])

async function fetchOrders() {
  tabLoading.order = true
  try {
    const res = await get<any>('/orders', { patientId: patientId.value })
    const data = res.data ?? res
    orderList.value = data.records ?? (Array.isArray(data) ? data : [])
  } finally {
    tabLoading.order = false
  }
}

// ==================== Tab 7: 患者日程 ====================
const scheduleList = ref<PatientSchedule[]>([])

async function fetchSchedules() {
  tabLoading.schedule = true
  try {
    // Fetch a wide date range to populate the calendar
    const y = calendarYear.value
    const m = calendarMonth.value
    const startDate = `${y}-${String(m).padStart(2, '0')}-01`
    const endDate = `${y}-${String(m).padStart(2, '0')}-${new Date(y, m, 0).getDate()}`
    const res = await get<any>(`/patients/${patientId.value}/schedule`, { startDate, endDate })
    const data = res.data ?? res
    scheduleList.value = Array.isArray(data) ? data : (data.records ?? [])
  } finally {
    tabLoading.schedule = false
  }
}

// ---- 简易月历 ----
const weekDays = ['日', '一', '二', '三', '四', '五', '六']
const now = new Date()
const calendarYear = ref(now.getFullYear())
const calendarMonth = ref(now.getMonth() + 1)
const selectedDate = ref('')

interface CalendarDay {
  day: number
  dateStr: string
  isToday: boolean
  isOtherMonth: boolean
  hasEvents: boolean
}

const calendarDays = computed<CalendarDay[]>(() => {
  const year = calendarYear.value
  const month = calendarMonth.value
  const firstDay = new Date(year, month - 1, 1)
  const lastDay = new Date(year, month, 0)
  const startDayOfWeek = firstDay.getDay()
  const daysInMonth = lastDay.getDate()
  const prevMonthLastDay = new Date(year, month - 1, 0).getDate()

  const todayStr = formatDateStr(now)
  const eventDates = new Set(scheduleList.value.map((s) => s.scheduleDate))

  const days: CalendarDay[] = []

  // 上月末尾日期
  for (let i = startDayOfWeek - 1; i >= 0; i--) {
    const d = prevMonthLastDay - i
    const dateStr = formatDateStr(new Date(year, month - 2, d))
    days.push({ day: d, dateStr, isToday: dateStr === todayStr, isOtherMonth: true, hasEvents: eventDates.has(dateStr) })
  }

  // 本月日期
  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = formatDateStr(new Date(year, month - 1, d))
    days.push({ day: d, dateStr, isToday: dateStr === todayStr, isOtherMonth: false, hasEvents: eventDates.has(dateStr) })
  }

  // 下月开头日期补齐 6 行
  const remaining = 42 - days.length
  for (let d = 1; d <= remaining; d++) {
    const dateStr = formatDateStr(new Date(year, month, d))
    days.push({ day: d, dateStr, isToday: dateStr === todayStr, isOtherMonth: true, hasEvents: eventDates.has(dateStr) })
  }

  return days
})

function formatDateStr(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

function selectDate(day: CalendarDay) {
  selectedDate.value = day.dateStr
}

const filteredSchedules = computed(() => {
  if (!selectedDate.value) return []
  return scheduleList.value.filter((s) => s.scheduleDate === selectedDate.value)
})

function prevMonth() {
  if (calendarMonth.value === 1) {
    calendarMonth.value = 12
    calendarYear.value--
  } else {
    calendarMonth.value--
  }
  fetchSchedules()
}

// ---- Custom scheduling ----
const scheduleDialogVisible = ref(false)
const scheduleFormRef = ref<FormInstance>()
const schedulingCustom = ref(false)
const conflictMsg = ref('')
const therapistOptions = ref<{ id: number; realName: string }[]>([])
const treatmentItemOptions2 = ref<string[]>([])

const hourlySlots = [
  { label: '08:00 - 09:00', value: '08:00-09:00' },
  { label: '09:00 - 10:00', value: '09:00-10:00' },
  { label: '10:00 - 11:00', value: '10:00-11:00' },
  { label: '11:00 - 12:00', value: '11:00-12:00' },
  { label: '14:00 - 15:00', value: '14:00-15:00' },
  { label: '15:00 - 16:00', value: '15:00-16:00' },
  { label: '16:00 - 17:00', value: '16:00-17:00' },
  { label: '17:00 - 18:00', value: '17:00-18:00' },
]

const scheduleForm = reactive({
  patientId: 0,
  therapistId: null as number | null,
  taskDate: '',
  timeSlot: '',
  treatmentItem: '',
  orderId: null as number | null,
})

const scheduleFormRules: FormRules = {
  therapistId: [{ required: true, message: '请选择治疗师', trigger: 'change' }],
  taskDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  timeSlot: [{ required: true, message: '请选择时间段', trigger: 'change' }],
  treatmentItem: [{ required: true, message: '请输入治疗项目', trigger: 'blur' }],
}

async function openCustomScheduleDialog() {
  scheduleForm.patientId = patientId.value
  scheduleForm.therapistId = patient.value.attendingTherapistId || null
  scheduleForm.taskDate = selectedDate.value || new Date().toISOString().slice(0, 10)
  scheduleForm.timeSlot = ''
  scheduleForm.treatmentItem = ''
  scheduleForm.orderId = null
  conflictMsg.value = ''
  scheduleDialogVisible.value = true

  // Load therapist options from patient's group or all therapists
  try {
    const res = await get<any>('/admin/therapy-groups')
    const groups = (res as any).data ?? res ?? []
    // Get users in the patient's group
    const usersRes = await get<any>('/admin/users', { size: 200 })
    therapistOptions.value = (usersRes as any).data?.records ?? []
  } catch { /* ignore */ }

  // Load treatment items
  try {
    const res = await get<any>('/tasks/treatment-items')
    treatmentItemOptions2.value = (res as any).data ?? res ?? []
  } catch { /* ignore */ }
}

async function checkConflict() {
  if (!scheduleForm.therapistId || !scheduleForm.taskDate || !scheduleForm.timeSlot) {
    conflictMsg.value = ''
    return
  }
  try {
    const res = await get<any>('/tasks/check-conflict', {
      therapistId: scheduleForm.therapistId,
      taskDate: scheduleForm.taskDate,
      timeSlot: scheduleForm.timeSlot,
    })
    const data = (res as any).data ?? res
    conflictMsg.value = data.conflict ? data.message : ''
  } catch { conflictMsg.value = '' }
}

// Watch for conflict check when therapist/date/slot changes
import { watch } from 'vue'
watch(() => [scheduleForm.therapistId, scheduleForm.taskDate, scheduleForm.timeSlot], () => {
  checkConflict()
})

function initCustomSchedule() {
  conflictMsg.value = ''
}

async function submitCustomSchedule() {
  const valid = await scheduleFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (conflictMsg.value) {
    ElMessage.warning('存在日程冲突，请调整时间或治疗师')
    return
  }
  schedulingCustom.value = true
  try {
    await post('/tasks/schedule-custom', {
      patientId: scheduleForm.patientId,
      therapistId: scheduleForm.therapistId,
      taskDate: scheduleForm.taskDate,
      timeSlot: scheduleForm.timeSlot,
      treatmentItem: scheduleForm.treatmentItem,
      orderId: scheduleForm.orderId || undefined,
    })
    ElMessage.success('排程成功')
    scheduleDialogVisible.value = false
    fetchSchedules()
  } finally {
    schedulingCustom.value = false
  }
}

const generatingTasks = ref(false)

async function batchGenerateAllOrders() {
  const approvedOrders = orderList.value.filter(o => o.status === 'APPROVED')
  if (!approvedOrders.length) {
    ElMessage.warning('没有待排程的已审核医嘱')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认为全部 ${approvedOrders.length} 条已审核医嘱自动排程生成任务？`,
      '一键排程',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'info' },
    )
  } catch { return }
  generatingTasks.value = true
  let total = 0
  try {
    for (const order of approvedOrders) {
      const res = await post<any>(`/orders/${order.id}/generate-tasks`)
      const list = (res as any).data ?? res
      total += Array.isArray(list) ? list.length : 0
    }
    ElMessage.success(`已生成 ${total} 条治疗任务`)
    fetchSchedules()
    fetchOrders()
  } finally {
    generatingTasks.value = false
  }
}

function openCustomScheduleFromOrder(order: MedicalOrder) {
  scheduleForm.patientId = patientId.value
  scheduleForm.therapistId = patient.value.attendingTherapistId || null
  scheduleForm.taskDate = order.periodStart || new Date().toISOString().slice(0, 10)
  scheduleForm.timeSlot = ''
  scheduleForm.treatmentItem = order.treatmentItem
  scheduleForm.orderId = order.id
  conflictMsg.value = ''
  scheduleDialogVisible.value = true
  initCustomSchedule()
}

function nextMonth() {
  if (calendarMonth.value === 12) {
    calendarMonth.value = 1
    calendarYear.value++
  } else {
    calendarMonth.value++
  }
  fetchSchedules()
}

// ==================== Tab 8: 治疗记录 ====================
interface TreatmentRecord {
  id?: number
  treatmentDate: string
  treatmentItem: string
  therapistName: string
  timeSlot: string
  status: string
  note: string
}

const recordList = ref<TreatmentRecord[]>([])

async function fetchRecords() {
  tabLoading.record = true
  try {
    const res = await get<any>(`/patients/${patientId.value}/records`)
    const data = res.data ?? res
    recordList.value = Array.isArray(data) ? data : (data.records ?? [])
  } finally {
    tabLoading.record = false
  }
}

// ==================== Tab 9: 患者出院 ====================
const discharging = ref(false)

async function handleDischarge() {
  discharging.value = true
  try {
    await put(`/patients/${patientId.value}/discharge`)
    ElMessage.success('出院操作已完成')
    fetchPatient()
  } finally {
    discharging.value = false
  }
}
</script>

<style scoped>
.patient-detail-page {
  padding: 16px;
}

.info-card {
  margin-bottom: 16px;
}

.workflow-card {
  margin-bottom: 16px;
}
.workflow-card :deep(.el-step__title) {
  font-size: 13px;
}

.info-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tabs-card {
  flex: 1;
}

.tab-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

/* 日程容器 */
.schedule-container {
  display: flex;
  gap: 24px;
}

.mini-calendar {
  width: 320px;
  flex-shrink: 0;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 12px;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.calendar-title {
  font-weight: 600;
  font-size: 15px;
}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  margin-bottom: 6px;
}

.weekday {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  padding: 4px 0;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.calendar-day {
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  position: relative;
  transition: background-color 0.15s;
}

.calendar-day:hover {
  background-color: var(--el-color-primary-light-9);
}

.calendar-day.is-other-month {
  color: var(--el-text-color-placeholder);
}

.calendar-day.is-today {
  font-weight: 700;
  color: var(--el-color-primary);
}

.calendar-day.is-selected {
  background-color: var(--el-color-primary);
  color: #fff;
}

.calendar-day.is-selected .day-number {
  color: #fff;
}

.day-number {
  font-size: 13px;
  line-height: 1;
}

.event-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background-color: var(--el-color-danger);
  margin-top: 2px;
}

.schedule-list {
  flex: 1;
  min-width: 0;
}

.schedule-list h4 {
  margin: 0 0 12px;
  font-size: 15px;
  color: var(--el-text-color-primary);
}

.timeline-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.timeline-title {
  font-weight: 500;
}

.timeline-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
