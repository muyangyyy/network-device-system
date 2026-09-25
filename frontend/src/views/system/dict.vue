<template>
  <div class="dict-management">
    <el-row :gutter="16">
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>字典列表</span>
              <el-button type="primary" size="small" @click="handleAddDict">
                <el-icon><Plus /></el-icon>添加
              </el-button>
            </div>
          </template>
          <el-table :data="dictList" border stripe highlight-current-row @current-change="handleDictSelect" v-loading="dictLoading">
            <el-table-column prop="dictName" label="字典名称" min-width="120" />
            <el-table-column prop="dictType" label="字典编码" width="140" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleEditDict(row)">编辑</el-button>
                <el-button type="danger" link size="small" @click="handleDeleteDict(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>{{ selectedDict ? `${selectedDict.dictName} - 字典项` : '请选择字典' }}</span>
              <el-button v-if="selectedDict" type="primary" size="small" @click="handleAddItem">
                <el-icon><Plus /></el-icon>添加
              </el-button>
            </div>
          </template>
          <el-table v-if="selectedDict" :data="itemList" border stripe v-loading="itemLoading">
            <el-table-column prop="itemLabel" label="标签" min-width="100" />
            <el-table-column prop="itemValue" label="值" width="120" />
            <el-table-column prop="sort" label="排序" width="70" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleEditItem(row)">编辑</el-button>
                <el-button type="danger" link size="small" @click="handleDeleteItem(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="请在左侧选择字典" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dictDialogVisible" :title="dictDialogTitle" width="450px">
      <el-form ref="dictFormRef" :model="dictForm" :rules="dictRules" label-width="90px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="dictForm.dictName" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="字典编码" prop="dictType">
          <el-input v-model="dictForm.dictType" placeholder="请输入字典编码" :disabled="editingDictId > 0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="dictForm.remark" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dictDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitDict" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="itemDialogVisible" :title="itemDialogTitle" width="450px">
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="90px">
        <el-form-item label="标签" prop="itemLabel">
          <el-input v-model="itemForm.itemLabel" placeholder="请输入标签" />
        </el-form-item>
        <el-form-item label="值" prop="itemValue">
          <el-input v-model="itemForm.itemValue" placeholder="请输入值" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="itemForm.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="itemForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="itemForm.remark" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitItem" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getDicts, createDict, updateDict, deleteDict, getDictItems, createDictItem, updateDictItem, deleteDictItem, type Dict, type DictItem } from '@/api/dict'

const dictLoading = ref(false)
const itemLoading = ref(false)
const dictList = ref<Dict[]>([])
const itemList = ref<DictItem[]>([])
const selectedDict = ref<Dict | null>(null)

const dictDialogVisible = ref(false)
const dictDialogTitle = ref('')
const editingDictId = ref(0)
const itemDialogVisible = ref(false)
const itemDialogTitle = ref('')
const editingItemId = ref(0)
const submitLoading = ref(false)
const dictFormRef = ref<FormInstance>()
const itemFormRef = ref<FormInstance>()

const dictForm = reactive({ dictName: '', dictType: '', remark: '' })
const dictRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入字典编码', trigger: 'blur' }]
}
const itemForm = reactive({ itemLabel: '', itemValue: '', sort: 0, status: 1, remark: '' })
const itemRules = {
  itemLabel: [{ required: true, message: '请输入标签', trigger: 'blur' }],
  itemValue: [{ required: true, message: '请输入值', trigger: 'blur' }]
}

async function fetchDictList() {
  dictLoading.value = true
  try {
    const res = await getDicts({ page: 1, size: 100 })
    dictList.value = res.records
  } catch (e: any) { ElMessage.error(e?.message || '加载数据失败') }
  finally { dictLoading.value = false }
}

async function fetchItemList(dictType: string) {
  itemLoading.value = true
  try { itemList.value = await getDictItems(dictType) }
  catch (e: any) { ElMessage.error(e?.message || '加载数据失败') }
  finally { itemLoading.value = false }
}

function handleDictSelect(row: Dict | null) {
  selectedDict.value = row
  if (row) fetchItemList(row.dictType)
}

function handleAddDict() {
  Object.assign(dictForm, { dictName: '', dictType: '', remark: '' })
  editingDictId.value = 0
  dictDialogTitle.value = '添加字典'
  dictDialogVisible.value = true
}

function handleEditDict(row: any) {
  Object.assign(dictForm, { dictName: row.dictName, dictType: row.dictType, remark: row.remark || '' })
  editingDictId.value = row.id
  dictDialogTitle.value = '编辑字典'
  dictDialogVisible.value = true
}

async function handleDeleteDict(row: any) {
  // ElMessageBox.confirm 在用户取消时会 reject，必须包在 try/catch 里
  try {
    await ElMessageBox.confirm(`确定要删除字典"${row.dictName}"吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteDict(row.id)
    ElMessage.success('删除成功')
    if (selectedDict.value?.id === row.id) {
      selectedDict.value = null
      itemList.value = []
    }
    fetchDictList()
  } catch (e) { ElMessage.error((e as any)?.message || '删除失败') }
}

async function handleSubmitDict() {
  if (!dictFormRef.value) return
  // 不能写成 await validate(async (valid) => ...)：校验失败时 validate() 返回 rejected promise
  try {
    await dictFormRef.value.validate()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    if (editingDictId.value) { await updateDict(editingDictId.value, dictForm) }
    else { await createDict(dictForm) }
    ElMessage.success('操作成功')
    dictDialogVisible.value = false
    fetchDictList()
  } catch (e) { ElMessage.error((e as any)?.message || '操作失败') }
  finally { submitLoading.value = false }
}

function handleAddItem() {
  Object.assign(itemForm, { itemLabel: '', itemValue: '', sort: 0, status: 1, remark: '' })
  editingItemId.value = 0
  itemDialogTitle.value = '添加字典项'
  itemDialogVisible.value = true
}

function handleEditItem(row: any) {
  Object.assign(itemForm, {
    itemLabel: row.itemLabel,
    itemValue: row.itemValue,
    sort: row.sort,
    status: row.status,
    remark: row.remark || ''
  })
  editingItemId.value = row.id
  itemDialogTitle.value = '编辑字典项'
  itemDialogVisible.value = true
}

async function handleDeleteItem(row: any) {
  // ElMessageBox.confirm 在用户取消时会 reject，必须包在 try/catch 里
  try {
    await ElMessageBox.confirm('确定要删除此字典项吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  if (!selectedDict.value) return
  try {
    await deleteDictItem(row.id)
    ElMessage.success('删除成功')
    fetchItemList(selectedDict.value.dictType)
  } catch (e) { ElMessage.error((e as any)?.message || '删除失败') }
}

async function handleSubmitItem() {
  if (!itemFormRef.value || !selectedDict.value) return
  // 不能写成 await validate(async (valid) => ...)：校验失败时 validate() 返回 rejected promise
  try {
    await itemFormRef.value.validate()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    if (editingItemId.value) {
      await updateDictItem(editingItemId.value, itemForm)
    } else {
      await createDictItem(selectedDict.value!.dictType, itemForm)
    }
    ElMessage.success('操作成功')
    itemDialogVisible.value = false
    fetchItemList(selectedDict.value!.dictType)
  } catch (e) { ElMessage.error((e as any)?.message || '操作失败') }
  finally { submitLoading.value = false }
}

onMounted(() => { fetchDictList() })
</script>

<style lang="scss" scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
