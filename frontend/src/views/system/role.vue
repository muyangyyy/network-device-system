<template>
  <div class="role-management">
    <el-card shadow="never">
      <div class="table-toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>添加角色
        </el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="角色名称" width="150" />
        <el-table-column prop="code" label="角色编码" width="150" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link size="small" @click="handlePermission(row)">权限</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="roleForm" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleForm.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="roleForm.code" placeholder="请输入角色编码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permDialogVisible" title="分配权限" width="500px">
      <el-tree
        ref="permTreeRef"
        :data="permTree"
        :props="{ label: 'name', children: 'children' }"
        show-checkbox
        node-key="id"
        default-expand-all
        :default-checked-keys="checkedPermIds"
      />
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePermission" :loading="permLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getRoles, createRole, updateRole, deleteRole, type Role } from '@/api/role'
import { getPermissionTree, type Permission } from '@/api/permission'

const loading = ref(false)
const tableData = ref<Role[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const roleForm = reactive({ id: 0, name: '', code: '', description: '' })
const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

const permDialogVisible = ref(false)
const permTreeRef = ref()
const permTree = ref<Permission[]>([])
const checkedPermIds = ref<number[]>([])
const currentRoleId = ref(0)
const permLoading = ref(false)

async function fetchData() {
  loading.value = true
  try {
    const res = await getRoles({ page: 1, size: 100 })
    tableData.value = res.records || res
  } catch (e: any) { ElMessage.error(e?.message || '加载数据失败') }
  finally { loading.value = false }
}

async function fetchPermTree() {
  try { permTree.value = await getPermissionTree() } catch (e: any) { ElMessage.error(e?.message || '加载数据失败') }
}

function handleAdd() {
  Object.assign(roleForm, { id: 0, name: '', code: '', description: '' })
  isEdit.value = false
  dialogTitle.value = '添加角色'
  dialogVisible.value = true
}

function handleEdit(row: any) {
  Object.assign(roleForm, { id: row.id, name: row.name, code: row.code, description: row.description || '' })
  isEdit.value = true
  dialogTitle.value = '编辑角色'
  dialogVisible.value = true
}

async function handleDelete(row: any) {
  // ElMessageBox.confirm 在用户取消时会 reject，必须包在 try/catch 里
  try {
    await ElMessageBox.confirm(`确定要删除角色"${row.name}"吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try { await deleteRole(row.id); ElMessage.success('删除成功'); fetchData() }
  catch (e) { ElMessage.error((e as any)?.message || '删除失败') }
}

function handlePermission(row: any) {
  currentRoleId.value = row.id
  checkedPermIds.value = row.permissionIds || []
  permDialogVisible.value = true
}

async function handleSavePermission() {
  permLoading.value = true
  try {
    const checkedNodes = permTreeRef.value?.getCheckedKeys() || []
    const halfCheckedNodes = permTreeRef.value?.getHalfCheckedKeys() || []
    const permissionIds = [...checkedNodes, ...halfCheckedNodes]
    await updateRole(currentRoleId.value, { permissionIds })
    ElMessage.success('权限保存成功')
    permDialogVisible.value = false
    fetchData()
  } catch (e) { ElMessage.error((e as any)?.message || '保存失败') }
  finally { permLoading.value = false }
}

async function handleSubmit() {
  if (!formRef.value) return
  // 不能写成 await validate(async (valid) => ...)：校验失败时 validate() 返回 rejected promise
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    if (isEdit.value) { await updateRole(roleForm.id, roleForm) }
    else { await createRole(roleForm) }
    ElMessage.success(isEdit.value ? '修改成功' : '创建成功')
    dialogVisible.value = false
    fetchData()
  } catch (e) { ElMessage.error((e as any)?.message || '操作失败') }
  finally { submitLoading.value = false }
}

onMounted(() => { fetchData(); fetchPermTree() })
</script>

<style lang="scss" scoped>
.table-toolbar { margin-bottom: 12px; }
</style>
