export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  realName: string
  role: string
  groupId: number | null
  phone: string | null
  email: string | null
  avatar: string | null
}

export interface PageResult<T> {
  total: number
  page: number
  size: number
  records: T[]
}

export interface Patient {
  id?: number
  name: string
  gender: number
  age: number | null
  inpatientNo: string
  bedNo: string
  admissionDate: string
  diagnosis: string
  allergyHistory: string
  contactPhone: string
  emergencyContact: string
  emergencyPhone: string
  attendingTherapistId: number | null
  attendingDoctorId: number | null
  status: string
  dischargeDate: string | null
  remark: string
}

export interface Task {
  id?: number
  patientId: number
  orderId?: number
  therapistId: number
  groupId?: number
  taskDate: string
  timeSlot: string
  treatmentItem: string
  status: string
  startTime?: string
  verificationTime?: string
  revokeTime?: string
  revokeReason?: string
  note?: string
  patientName?: string
  therapistName?: string
}

export interface MedicalOrder {
  id?: number
  patientId: number
  doctorId?: number
  planId?: number
  orderType: string
  treatmentItem: string
  frequency: string
  dailyCount: number
  periodStart: string
  periodEnd: string
  note: string
  status: string
  reviewComment?: string
  revokeCount?: number
  patientName?: string
  doctorName?: string
  therapistName?: string
}

export interface Assessment {
  id?: number
  patientId: number
  templateId: number
  assessorId?: number
  assessDate: string
  totalScore: number
  detail: string
  conclusion: string
  templateName?: string
  assessorName?: string
}

export interface AssessmentTemplate {
  id?: number
  templateName: string
  abbreviation: string
  category: string
  items: string
  scoringRule: string
  maxScore: number
  status: number
}

export interface TreatmentGoal {
  id?: number
  patientId: number
  goalType: string
  content: string
  targetDate: string
  status: string
  creatorId?: number
}

export interface TreatmentPlan {
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
}

export interface PatientSchedule {
  id?: number
  patientId: number
  scheduleDate: string
  timeSlot: string
  eventType: string
  sourceId?: number
  therapistId?: number
  title: string
  description?: string
  status: string
}

export interface WorkloadStat {
  date: string
  treatmentCount: number
  patientCount: number
}

export interface User {
  id?: number
  username: string
  password?: string
  realName: string
  role: string
  groupId?: number
  phone: string
  email: string
  status: number
}

export interface TherapyGroup {
  id?: number
  groupName: string
  leaderId?: number
  description: string
}

export interface SystemDict {
  id?: number
  dictType: string
  dictCode: string
  dictValue: string
  sortOrder: number
  status: number
  remark: string
}
