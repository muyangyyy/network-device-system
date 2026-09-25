<template>
  <div class="group-page">
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>设备分组</span>
              <el-button type="primary" size="small" @click="handleAddGroup(null)">
                <el-icon><Plus /></el-icon>添加
              </el-button>
            </div>
          </template>
          <el-input v-model="filterText" placeholder="搜索分组" clearable prefix-icon="Search" style="margin-bottom: 12px" />
          <el-tree
            ref="treeRef"
            :data="groupTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            highlight-current
            default-expand-all
            :filter-node-method="filterNode"
            @node-click="handleNodeClick"
            v-loading="treeLoading"
          >
            <template #default="{ node, data }">
              <div class="tree-node">
                <span>{{ data.name }}</span>
                <span class="tree-node-actions">
                  <el-icon @click.stop="handleAddGroup(data.id)"><Plus /></el-icon>
                  <el-icon @click.stop="handleEditGroup(data)"><Edit /></el-icon>
                  <el-icon @click.stop="handleDeleteGroup(data)"><Delete /></el-icon>
                </span>
              </div>
            </template>
          </el-tree>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>{{ selectedGroup ? `${selectedGroup.name} - 设备列表` : '请选择分组查看设备' }}</span>
            </div>
          </template>
          <el-table :data="deviceList" border stripe v-loading="deviceLoading" v-if="selectedGroup">
            <el-table-column prop="deviceCode" label="设备编码" width="140" />
            <el-table-column prop="deviceName" label="设备名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="deviceType" label="设备类型" width="100" />
            <el-table-column prop="brand" label="品牌" width="100" />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <DeviceStatusTag :status="row.status" />
              </template>
            </el-table-column>
            <el-table-column prop="responsibleUserName" label="负责人" width="90" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button type="primary" link @click="$router.push(`/devices/detail/${row.id}`)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="请在左侧选择一个分组" />
          <Pagination
            v-if="selectedGroup"
            v-model:page="devicePage"
            v-model:limit="deviceSize"
            :total="deviceTotal"
            @pagination="fetchDevices"
          />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="groupForm" :rules="rules" label-width="100px">
        <el-form-item label="上级分组">
          <el-tree-select
            v-model="groupForm.parentId"
            :data="groupTree"
            :props="{ label: 'name', value: 'id', children: 'children' } as any"
            placeholder="无（顶级分组）"
            check-strictly
            clearable
          />
        </el-form-item>
        <el-form-item label="分组名称" prop="name">
          <el-input v-model="groupForm.name" placeholder="请输入分组名称" />
        </el-form-item>
        <el-form-item label="分组编码" prop="code">
          <el-input v-model="groupForm.code" placeholder="请输入分组编码" />
        </el-form-item>
        <el-form-item label="分组描述">
          <el-input v-model="groupForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="groupForm.managerId" placeholder="请选择负责人" filterable clearable>
            <el-option v-for="u in userList" :key="u.id" :label="u.nickname" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="groupForm.sort" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitGroup" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { getGroupTree, createGroup, updateGroup, deleteGroup, type Group } from '@/api/group'
import { getDevices, type Device } from '@/api/device'
import { getUsers, type User } from '@/api/user'
import DeviceStatusTag from '@/components/DeviceStatusTag.vue'
import Pagination from '@/components/Pagination.vue'

const treeRef = ref()
const formRef = ref<FormInstance>()
const treeLoading = ref(false)
const groupTree = ref<Group[]>([])
const filterText = ref('')

const selectedGroup = ref<Group | null>(null)
const deviceList = ref<Device[]>([])
const deviceLoading = ref(false)
const devicePage = ref(1)
const deviceSize = ref(10)
const deviceTotal = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const editingGroupId = ref<number | null>(null)
const userList = ref<User[]>([])

const groupForm = reactive({
  parentId: undefined as number | undefined,
  name: '',
  code: '',
  description: '',
  managerId: undefined as number | undefined,
  sort: 0
})

const rules = {
  name: [{ required: true, message: '请输入分组名称', trigger: 'blur' }]
}

watch(filterText, (val) => {
  treeRef.value?.filter(val)
})

function filterNode(value: string, data: any) {
  if (!value) return true
  return data.name.includes(value)
}

async function fetchGroupTree() {
  treeLoading.value = true
  try {
    groupTree.value = await getGroupTree()
  } catch (e: any) { ElMessage.error(e?.message || '加载数据失败') }
  finally { treeLoading.value = false }
}

async function fetchUserList() {
  try {
    const res = await getUsers({ page: 1, size: 1000 })
    userList.value = res.records
  } catch (e: any) { ElMessage.error(e?.message || '加载数据失败') }
}

async function handleNodeClick(data: Group) {
  selectedGroup.value = data
  devicePage.value = 1
  await fetchDevices()
}

async function fetchDevices() {
  if (!selectedGroup.value) return
  deviceLoading.value = true
  try {
    const res = await getDevices({ page: devicePage.value, size: deviceSize.value, groupId: selectedGroup.value.id })
    deviceList.value = res.records
    deviceTotal.value = res.total
  } catch (e: any) { ElMessage.error(e?.message || '加载数据失败') }
  finally { deviceLoading.value = false }
}

function resetForm() {
  Object.assign(groupForm, { parentId: undefined, name: '', code: '', description: '', managerId: undefined, sort: 0 })
  editingGroupId.value = null
}

function handleAddGroup(parentId: number | null) {
  resetForm()
  groupForm.parentId = parentId || undefined
  dialogTitle.value = '添加分组'
  dialogVisible.value = true
}

function handleEditGroup(data: Group) {
  resetForm()
  editingGroupId.value = data.id
  Object.assign(groupForm, {
    parentId: data.parentId || undefined,
    name: data.name,
    code: data.code || '',
    description: data.description || '',
    managerId: data.managerId || undefined,
    sort: data.sort
  })
  dialogTitle.value = '编辑分组'
  dialogVisible.value = true
}

async function handleDeleteGroup(data: Group) {
  // ElMessageBox.confirm 在用户取消时会 reject，必须包在 try/catch 里
  try {
    await ElMessageBox.confirm(`确定要删除分组"${data.name}"吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteGroup(data.id)
    ElMessage.success('删除成功')
    fetchGroupTree()
  } catch (e) {
    ElMessage.error((e as any)?.message || '删除失败')
  }
}

async function handleSubmitGroup() {
  if (!formRef.value) return
  // 不能写成 await validate(async (valid) => ...)：校验失败时 validate() 返回 rejected promise
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    // 清空父分组时 tree-select 的值是 undefined；直接提交会被 JSON 序列化省略，
    // 后端按「非空字段更新」语义跳过 parentId，清空操作静默失效。
    // 显式传 0 表示顶级，后端把 0 当顶级分组处理（validateNoCycle 对 0 短路）。
    // 负责人同理：0 是「清空负责人」哨兵，后端把 0 翻译成 NULL（创建时按未选择处理）。
    const payload = {
      ...groupForm,
      parentId: groupForm.parentId ?? 0,
      managerId: groupForm.managerId ?? 0
    }
    if (editingGroupId.value) {
      await updateGroup(editingGroupId.value, payload)
      ElMessage.success('修改成功')
    } else {
      await createGroup(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchGroupTree()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  fetchGroupTree()
  fetchUserList()
})
</script>

<style lang="scss" scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: 8px;
}
.tree-node-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
  .el-icon {
    cursor: pointer;
    color: #909399;
    &:hover { color: #409eff; }
  }
}
.tree-node:hover .tree-node-actions { opacity: 1; }
</style>
