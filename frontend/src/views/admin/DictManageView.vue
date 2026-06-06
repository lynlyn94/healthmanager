<template>
  <div class="dict-manage">
    <el-row :gutter="16">
      <!-- Left: Dict Types -->
      <el-col :span="6">
        <el-card shadow="never" class="left-card" v-loading="typeLoading">
          <div class="left-header">
            <span class="section-title">字典类型</span>
            <el-button size="small" type="primary" :icon="Plus" @click="handleAddType">新增</el-button>
          </div>
          <div class="type-list">
            <div
              v-for="item in typeList"
              :key="item"
              class="type-item"
              :class="{ active: activeType === item }"
              @click="selectType(item)"
            >
              <span class="type-name">{{ item }}</span>
              <div class="type-actions">
                <el-button link size="small" type="primary" @click.stop="handleEditType(item)">编辑</el-button>
                <el-popconfirm
                  title="确定删除该类型及所有条目？"
                  confirm-button-text="确定"
                  cancel-button-text="取消"
                  @confirm="handleDeleteType(item)"
                >
                  <template #reference>
                    <el-button link size="small" type="danger" @click.stop>删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
            <el-empty v-if="!typeList.length" description="暂无字典类型" :image-size="60" />
          </div>
        </el-card>
      </el-col>

      <!-- Right: Dict Items -->
      <el-col :span="18">
        <el-card shadow="never" class="right-card">
          <template v-if="activeType">
            <div class="right-header">
              <span class="section-title">{{ activeType }} - 字典条目</span>
              <el-button size="small" type="primary" :icon="Plus" @click="handleAddItem">新增条目</el-button>
            </div>
            <el-table :data="itemData" v-loading="itemLoading" stripe border style="width: 100%">
              <el-table-column prop="id" label="ID" width="70" />
              <el-table-column prop="dictCode" label="字典编码" width="140" />
              <el-table-column prop="dictValue" label="字典值" min-width="160" />
              <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
              <el-table-column label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row.status === 1 ? '启用' : '禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="备注" min-width="140" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ row.remark || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" link size="small" @click="handleEditItem(row)">编辑</el-button>
                  <el-popconfirm
                    title="确定删除该条目？"
                    confirm-button-text="确定"
                    cancel-button-text="取消"
                    @confirm="handleDeleteItem(row)"
                  >
                    <template #reference>
                      <el-button type="danger" link size="small">删除</el-button>
                    </template>
                  </el-popconfirm>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!itemLoading && itemData.length === 0" description="暂无字典条目" />
          </template>
          <el-empty v-else description="请从左侧选择一个字典类型" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>

    <!-- Type Dialog -->
    <el-dialog
      v-model="typeDialog.visible"
      :title="typeDialog.isEdit ? '编辑字典类型' : '新增字典类型'"
      width="420px"
      :close-on-click-modal="false"
      @closed="resetTypeForm"
    >
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeFormRules" label-width="80px">
        <el-form-item label="类型编码" prop="dictType">
          <el-input
            v-model="typeForm.dictType"
            placeholder="请输入类型编码（如 GENDER）"
            :disabled="typeDialog.isEdit"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="typeDialog.loading" @click="handleTypeSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Item Dialog -->
    <el-dialog
      v-model="itemDialog.visible"
      :title="itemDialog.isEdit ? '编辑字典条目' : '新增字典条目'"
      width="480px"
      :close-on-click-modal="false"
      @closed="resetItemForm"
    >
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemFormRules" label-width="80px">
        <el-form-item label="字典编码" prop="dictCode">
          <el-input v-model="itemForm.dictCode" placeholder="请输入字典编码" />
        </el-form-item>
        <el-form-item label="字典值" prop="dictValue">
          <el-input v-model="itemForm.dictValue" placeholder="请输入字典值" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="itemForm.sortOrder" :min="0" :max="9999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="itemForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="itemForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="itemDialog.loading" @click="handleItemSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { get, post, put, del } from '@/api'
import type { SystemDict, PageResult } from '@/api/types'

// ── Types ─────────────────────────────────────────────────
const typeList = ref<string[]>([])
const typeLoading = ref(false)
const activeType = ref('')

async function fetchTypes() {
  typeLoading.value = true
  try {
    const res = await get<any>('/admin/dicts/types')
    typeList.value = res.data ?? res
  } catch {
    // ignore
  } finally {
    typeLoading.value = false
  }
}

function selectType(type: string) {
  activeType.value = type
  fetchItems()
}

// ── Items ─────────────────────────────────────────────────
const itemData = ref<SystemDict[]>([])
const itemLoading = ref(false)

async function fetchItems() {
  if (!activeType.value) return
  itemLoading.value = true
  try {
    const res = await get<any>('/admin/dicts', { type: activeType.value })
    const data = res.data ?? res
    itemData.value = Array.isArray(data) ? data : (data.records ?? [])
  } finally {
    itemLoading.value = false
  }
}

// ── Type Dialog ───────────────────────────────────────────
const typeFormRef = ref<FormInstance>()
const typeDialog = reactive({ visible: false, isEdit: false, loading: false, oldType: '' })
const typeForm = reactive({ dictType: '' })
const typeFormRules: FormRules = {
  dictType: [{ required: true, message: '请输入类型编码', trigger: 'blur' }],
}

function handleAddType() {
  typeDialog.visible = true
  typeDialog.isEdit = false
  typeDialog.oldType = ''
}

function handleEditType(type: string) {
  typeDialog.visible = true
  typeDialog.isEdit = true
  typeDialog.oldType = type
  typeForm.dictType = type
}

async function handleTypeSubmit() {
  const valid = await typeFormRef.value?.validate().catch(() => false)
  if (!valid) return

  if (typeDialog.isEdit) {
    ElMessage.warning('字典类型重命名功能暂不支持')
    return
  }

  typeDialog.loading = true
  try {
    // Create a placeholder dict entry to establish the type
    await post('/admin/dicts', {
      dictType: typeForm.dictType,
      dictCode: '_placeholder',
      dictValue: '占位条目（可删除）',
      sortOrder: 0,
      status: 1,
      remark: '',
    })
    ElMessage.success('创建成功')
    typeDialog.visible = false
    fetchTypes()
  } finally {
    typeDialog.loading = false
  }
}

async function handleDeleteType(type: string) {
  ElMessage.warning('请逐个删除该类型下的所有字典条目来移除类型')
}

function resetTypeForm() {
  typeFormRef.value?.resetFields()
  typeForm.dictType = ''
}

// ── Item Dialog ───────────────────────────────────────────
const itemFormRef = ref<FormInstance>()
const itemDialog = reactive({ visible: false, isEdit: false, loading: false, editId: null as number | null })

const itemForm = reactive<SystemDict>({
  dictType: '',
  dictCode: '',
  dictValue: '',
  sortOrder: 0,
  status: 1,
  remark: '',
})

const itemFormRules: FormRules = {
  dictCode: [{ required: true, message: '请输入字典编码', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典值', trigger: 'blur' }],
}

function handleAddItem() {
  itemDialog.visible = true
  itemDialog.isEdit = false
  itemDialog.editId = null
}

function handleEditItem(row: SystemDict) {
  itemDialog.visible = true
  itemDialog.isEdit = true
  itemDialog.editId = row.id!
  itemForm.dictCode = row.dictCode
  itemForm.dictValue = row.dictValue
  itemForm.sortOrder = row.sortOrder
  itemForm.status = row.status
  itemForm.remark = row.remark
}

async function handleItemSubmit() {
  const valid = await itemFormRef.value?.validate().catch(() => false)
  if (!valid) return

  itemDialog.loading = true
  try {
    const payload = {
      dictType: activeType.value,
      dictCode: itemForm.dictCode,
      dictValue: itemForm.dictValue,
      sortOrder: itemForm.sortOrder,
      status: itemForm.status,
      remark: itemForm.remark,
    }
    if (!itemDialog.isEdit) {
      await post('/admin/dicts', payload)
      ElMessage.success('创建成功')
    } else {
      await put(`/admin/dicts/${itemDialog.editId}`, payload)
      ElMessage.success('更新成功')
    }
    itemDialog.visible = false
    fetchItems()
  } finally {
    itemDialog.loading = false
  }
}

async function handleDeleteItem(row: SystemDict) {
  try {
    await del(`/admin/dicts/${row.id}`)
    ElMessage.success('删除成功')
    fetchItems()
  } catch {
    // interceptor handles error
  }
}

function resetItemForm() {
  itemFormRef.value?.resetFields()
  itemForm.dictCode = ''
  itemForm.dictValue = ''
  itemForm.sortOrder = 0
  itemForm.status = 1
  itemForm.remark = ''
}

// ── Init ──────────────────────────────────────────────────
onMounted(() => {
  fetchTypes()
})
</script>

<style scoped>
.dict-manage {
  padding: 16px;
}

.left-card,
.right-card {
  min-height: 500px;
}

.left-header,
.right-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
}

.type-list {
  max-height: 460px;
  overflow-y: auto;
}

.type-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
  margin-bottom: 2px;
}

.type-item:hover {
  background: #f0f2f5;
}

.type-item.active {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 600;
}

.type-name {
  font-size: 14px;
}

.type-actions {
  display: none;
}

.type-item:hover .type-actions {
  display: flex;
  gap: 4px;
}
</style>
