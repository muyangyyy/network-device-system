<template>
  <div class="user-management">
    <el-card shadow="never">
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="queryParams.username" placeholder="请输入用户名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="queryParams.nickname" placeholder="请输入昵称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px">
      <div class="table-toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>添加用户
        </el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="departmentName" label="部门" width="120" />
        <el-table-column label="角色" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="r in row.roleNames" :key="r" size="small" style="margin: 2px">{{ r }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="(val: any) => handleStatusChange(row, val)" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="handleResetPassword(row)">重置密码</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination
        v-model:page="queryParams.page"
        v-model:limit="queryParams.size"
        :total="total"
        @pagination="fetchData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="userForm" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="userForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="userForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="部门">
          <el-tree-select
            v-model="userForm.departmentId"
            :data="departmentTree"
            :props="{ label: 'name', value: 'id', children: 'children' } as any"
            placeholder="请选择部门"
            check-strictly
            clearable
          />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="userForm.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="r in roleList" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
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
import { ref, reactive, onMounted, h } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getUsers, createUser, updateUser, deleteUser, updateUserStatus, resetPassword, type User } from '@/api/user'
import { getAllRoles, type Role } from '@/api/role'
import { getDepartmentTree, type Department } from '@/api/department'
import Pagination from '@/components/Pagination.vue'
import { isValidEmail, isValidPhone } from '@/utils/validate'

const loading = ref(false)
const tableData = ref<User[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const queryParams = reactive({ page: 1, size: 10, username: '', nickname: '', status: undefined as number | undefined })

const userForm = reactive({
  id: 0,
  username: '',
  nickname: '',
  password: '',
  email: '',
  phone: '',
  departmentId: undefined as number | undefined,
  roleIds: [] as number[]
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  email: [{ validator: (_r: any, v: string, cb: any) => { if (v && !isValidEmail(v)) cb(new Error('邮箱格式不正确')); else cb() }, trigger: 'blur' }],
  phone: [{ validator: (_r: any, v: string, cb: any) => { if (v && !isValidPhone(v)) cb(new Error('手机号格式不正确')); else cb() }, trigger: 'blur' }]
}

const roleList = ref<Role[]>([])
const departmentTree = ref<Department[]>([])

async function fetchData() {
  loading.value = true
  try {
    const res = await getUsers(queryParams)
    tableData.value = res.records
    total.value = res.total
  } catch (e: any) { ElMessage.error(e?.message || '加载数据失败') }
  finally { loading.value = false }
}

function handleSearch() { queryParams.page = 1; fetchData() }
function resetQuery() { Object.assign(queryParams, { page: 1, username: '', nickname: '', status: undefined }); fetchData() }

function resetForm() {
  Object.assign(userForm, { id: 0, username: '', nickname: '', password: '', email: '', phone: '', departmentId: undefined, roleIds: [] })
}

function handleAdd() {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '添加用户'
  dialogVisible.value = true
}

function handleEdit(row: any) {
  resetForm()
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  Object.assign(userForm, { id: row.id, username: row.username, nickname: row.nickname, email: row.email || '', phone: row.phone || '', departmentId: row.departmentId, roleIds: row.roleIds || [] })
  dialogVisible.value = true
}

async function handleDelete(row: any) {
  // ElMessageBox.confirm 在用户取消时会 reject，必须包在 try/catch 里
  try {
    await ElMessageBox.confirm(`确定要删除用户"${row.username}"吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { ElMessage.error((e as any)?.message || '删除失败') }
}

async function handleStatusChange(row: any, enabled: boolean) {
  try {
    await updateUserStatus(row.id, enabled ? 1 : 0)
    ElMessage.success('状态变更成功')
    fetchData()
  } catch (e) { ElMessage.error((e as any)?.message || '操作失败') }
}

async function handleResetPassword(row: any) {
  // ElMessageBox.confirm 在用户取消时会 reject，必须包在 try/catch 里
  try {
    await ElMessageBox.confirm(`确定要重置用户"${row.username}"的密码吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await resetPassword(row.id)
    // 后端返回随机明文密码，仅展示一次。
    // 必须用 VNode 渲染而不是 dangerouslyUseHTMLString：
    // username 只有 @NotBlank 校验、可以含任意字符（如 <img onerror=...>），
    // HTML 字符串插值用户名就是存储型 XSS；VNode 的文本子节点会被 Vue 自动转义。
    const pwd = typeof res === 'string' ? res : (res as any)?.password || ''
    if (pwd) {
      ElMessageBox.alert(
        h('div', null, [
          h('p', { style: 'margin-bottom:8px' }, `用户「${row.username}」的密码已重置为：`),
          h('p', {
            style: 'font-size:1.3em;letter-spacing:1px;font-weight:600;color:#409eff;margin-bottom:8px'
          }, pwd),
          h('p', { style: 'color:#909399;font-size:12px' }, '请立即将密码告知用户，此密码仅显示一次。')
        ]),
        '重置成功',
        { confirmButtonText: '我已知晓' }
      )
    } else {
      ElMessage.success('密码已重置')
    }
  } catch (e) { ElMessage.error((e as any)?.message || '操作失败') }
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
    if (isEdit.value) {
      // 清空部门时 tree-select 的值是 undefined，JSON 序列化会省略字段，
      // 后端按「未传」跳过更新，清空操作静默失效。归一化为 0（0 不是合法 id），
      // 后端把 0 解释为「置空部门」（用户调岗到未分配状态）。
      const payload = { ...userForm, departmentId: userForm.departmentId ?? 0 }
      await updateUser(userForm.id, payload)
      ElMessage.success('修改成功')
    } else {
      await createUser(userForm)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') }
  finally { submitLoading.value = false }
}

onMounted(async () => {
  fetchData()
  try {
    const [rolesRes, deptRes] = await Promise.all([getAllRoles(), getDepartmentTree()])
    roleList.value = (rolesRes as any)?.records || rolesRes || []
    departmentTree.value = deptRes
  } catch (e: any) { ElMessage.error(e?.message || '加载数据失败') }
})
</script>

<style lang="scss" scoped>
.search-form { padding: 0; }
.table-toolbar { margin-bottom: 12px; }
</style>
