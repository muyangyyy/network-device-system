<template>
  <div class="department-management">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>部门管理</span>
          <el-button type="primary" size="small" @click="handleAdd(null)">
            <el-icon><Plus /></el-icon>添加部门
          </el-button>
        </div>
      </template>
      <el-tree
        :data="deptTree"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        default-expand-all
        v-loading="loading"
      >
        <template #default="{ node, data }">
          <div class="tree-node">
            <span>{{ data.name }}</span>
            <span class="tree-node-actions">
              <el-icon @click.stop="handleAdd(data.id)"><Plus /></el-icon>
              <el-icon @click.stop="handleEdit(data)"><Edit /></el-icon>
              <el-icon @click.stop="handleDelete(data)"><Delete /></el-icon>
            </span>
          </div>
        </template>
      </el-tree>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="deptForm" :rules="rules" label-width="90px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="deptForm.parentId"
            :data="deptTree"
            :props="{ label: 'name', value: 'id', children: 'children' } as any"
            placeholder="无（顶级部门）"
            check-strictly
            clearable
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="deptForm.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="deptForm.leaderId" placeholder="请选择负责人" filterable clearable style="width: 100%">
            <el-option v-for="u in userList" :key="u.id" :label="u.nickname" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="deptForm.sort" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { getDepartmentTree, createDepartment, updateDepartment, deleteDepartment, type Department } from '@/api/department'
import { getUsers, type User } from '@/api/user'

const loading = ref(false)
const deptTree = ref<Department[]>([])
const userList = ref<User[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const editingId = ref(0)
const formRef = ref<FormInstance>()

// 部门表(sys_department)只有 dept_name/parent_id/sort/leader_id/status/remark，
// 没有 code / phone / managerName 列，这三个字段此前在表单里填了不会保存、编辑时也永远空白，故移除。
// 「负责人」改为绑定 leaderId（实体真实存在的列）。
const deptForm = reactive({ parentId: undefined as number | undefined, name: '', leaderId: undefined as number | undefined, sort: 0 })
const rules = { name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }] }

async function fetchTree() {
  loading.value = true
  try { deptTree.value = await getDepartmentTree() }
  catch (e: any) { ElMessage.error(e?.message || '加载数据失败') }
  finally { loading.value = false }
}

async function fetchUsers() {
  try {
    const res = await getUsers({ page: 1, size: 1000 })
    userList.value = res.records
  } catch (e: any) { ElMessage.error(e?.message || '加载数据失败') }
}

function handleAdd(parentId: number | null) {
  Object.assign(deptForm, { parentId: parentId || undefined, name: '', leaderId: undefined, sort: 0 })
  editingId.value = 0
  dialogTitle.value = '添加部门'
  dialogVisible.value = true
}

function handleEdit(data: Department) {
  // 树接口把 leaderId 映射到 VO 的 managerId
  Object.assign(deptForm, { parentId: data.parentId || undefined, name: data.name, leaderId: data.managerId || undefined, sort: data.sort })
  editingId.value = data.id
  dialogTitle.value = '编辑部门'
  dialogVisible.value = true
}

async function handleDelete(data: Department) {
  // ElMessageBox.confirm 在用户取消时会 reject，必须包在 try/catch 里，否则是未处理的 promise rejection
  try {
    await ElMessageBox.confirm(`确定要删除部门"${data.name}"吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try { await deleteDepartment(data.id); ElMessage.success('删除成功'); fetchTree() }
  catch (e: any) { ElMessage.error(e.message || '删除失败') }
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
    // 清空上级部门时 tree-select 的值是 undefined；直接提交会被 JSON 序列化省略，
    // 后端 updateById 跳过 null 字段，清空操作静默失效。显式传 0 表示顶级部门。
    const payload = { ...deptForm, parentId: deptForm.parentId ?? 0 }
    if (editingId.value) { await updateDepartment(editingId.value, payload) }
    else { await createDepartment(payload) }
    ElMessage.success(editingId.value ? '修改成功' : '创建成功')
    dialogVisible.value = false
    fetchTree()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') }
  finally { submitLoading.value = false }
}

onMounted(() => { fetchTree(); fetchUsers() })
</script>

<style lang="scss" scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.tree-node { display: flex; justify-content: space-between; align-items: center; width: 100%; padding-right: 8px; }
.tree-node-actions { display: flex; gap: 4px; opacity: 0; transition: opacity 0.2s; }
.tree-node-actions .el-icon { cursor: pointer; color: #909399; }
.tree-node-actions .el-icon:hover { color: #409eff; }
.tree-node:hover .tree-node-actions { opacity: 1; }
</style>
