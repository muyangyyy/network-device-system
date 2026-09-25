<template>
  <div class="device-list">
    <el-card shadow="never">
      <el-form :model="queryParams" ref="queryFormRef" :inline="true" class="search-form">
        <el-form-item label="设备编码" prop="deviceCode">
          <el-input v-model="queryParams.deviceCode" placeholder="请输入设备编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="设备名称" prop="deviceName">
          <el-input v-model="queryParams.deviceName" placeholder="请输入设备名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="设备类型" prop="deviceType">
          <el-select v-model="queryParams.deviceType" placeholder="全部" clearable>
            <el-option v-for="item in deviceTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="品牌" prop="brand">
          <el-input v-model="queryParams.brand" placeholder="请输入品牌" clearable />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部" clearable>
            <el-option label="正常运行" value="NORMAL" />
            <el-option label="故障维修" value="FAULT_REPAIR" />
            <el-option label="闲置" value="IDLE" />
            <el-option label="已报废" value="SCRAPPED" />
          </el-select>
        </el-form-item>
        <el-form-item label="分组" prop="groupId">
          <el-tree-select
            v-model="queryParams.groupId"
            :data="groupTree"
            :props="{ label: 'name', value: 'id', children: 'children' } as any"
            placeholder="全部"
            clearable
            check-strictly
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button type="primary" link @click="showAdvanced = !showAdvanced">
            {{ showAdvanced ? '收起' : '展开' }}
            <el-icon><ArrowDown v-if="!showAdvanced" /><ArrowUp v-else /></el-icon>
          </el-button>
        </el-form-item>
        <template v-if="showAdvanced">
          <el-form-item label="IP地址" prop="ipAddress">
            <el-input v-model="queryParams.ipAddress" placeholder="请输入IP地址" clearable />
          </el-form-item>
          <el-form-item label="序列号" prop="serialNumber">
            <el-input v-model="queryParams.serialNumber" placeholder="请输入序列号" clearable />
          </el-form-item>
          <el-form-item label="负责人" prop="responsibleUserId">
            <el-select v-model="queryParams.responsibleUserId" placeholder="全部" clearable filterable>
              <el-option v-for="u in userList" :key="u.id" :label="u.nickname" :value="u.id" />
            </el-select>
          </el-form-item>
        </template>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-button type="primary" @click="$router.push('/devices/add')" v-if="userStore.hasPermission('device:add')">
            <el-icon><Plus /></el-icon>添加设备
          </el-button>
          <el-button @click="handleImport" v-if="userStore.hasPermission('device:import')">
            <el-icon><Upload /></el-icon>导入
          </el-button>
          <el-button @click="handleExport" v-if="userStore.hasPermission('device:export')">
            <el-icon><Download /></el-icon>导出
          </el-button>
        </div>
        <div class="toolbar-right">
          <el-button :disabled="selectedIds.length === 0" @click="handleBatchGroup" v-if="userStore.hasPermission('device:edit')">
            <el-icon><Folder /></el-icon>批量分组
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        @selection-change="handleSelectionChange"
        row-key="id"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="deviceCode" label="设备编码" width="140" show-overflow-tooltip fixed="left" />
        <el-table-column prop="deviceName" label="设备名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="设备类型" width="100">
          <template #default="{ row }">
            {{ getDeviceTypeLabel(row.deviceType) }}
          </template>
        </el-table-column>
        <el-table-column prop="brand" label="品牌" width="100" show-overflow-tooltip />
        <el-table-column prop="model" label="型号" width="120" show-overflow-tooltip />
        <el-table-column prop="serialNumber" label="序列号" width="140" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP地址" width="140" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <DeviceStatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="groupName" label="所属分组" width="120" show-overflow-tooltip />
        <el-table-column prop="responsibleUserName" label="负责人" width="90" />
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="$router.push(`/devices/detail/${row.id}`)" v-if="userStore.hasPermission('device:view')">查看</el-button>
            <el-button type="primary" link size="small" @click="$router.push(`/devices/edit/${row.id}`)" v-if="userStore.hasPermission('device:edit')">编辑</el-button>
            <el-dropdown trigger="click" @command="(cmd: string) => handleCommand(cmd, row)">
              <el-button type="primary" link size="small">更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="status" v-if="userStore.hasPermission('device:edit')">变更状态</el-dropdown-item>
                  <el-dropdown-item command="repairs" v-if="userStore.hasPermission('repair:list')">维修记录</el-dropdown-item>
                  <el-dropdown-item command="delete" divided v-if="userStore.hasPermission('device:delete')">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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

    <el-dialog v-model="statusDialogVisible" title="变更设备状态" width="500px">
      <el-form :model="statusForm" label-width="80px">
        <el-form-item label="当前状态">
          <DeviceStatusTag :status="currentDevice?.status || ''" />
        </el-form-item>
        <el-form-item label="新状态" required>
          <el-select v-model="statusForm.status" placeholder="请选择">
            <el-option label="正常运行" value="NORMAL" />
            <el-option label="故障维修" value="FAULT_REPAIR" />
            <el-option label="闲置" value="IDLE" />
            <el-option label="已报废" value="SCRAPPED" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="statusForm.reason" type="textarea" placeholder="请输入变更原因" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStatusChange" :loading="statusLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchGroupDialogVisible" title="批量设置分组" width="500px">
      <el-form label-width="80px">
        <el-form-item label="目标分组">
          <el-tree-select
            v-model="batchGroupId"
            :data="groupTree"
            :props="{ label: 'name', value: 'id', children: 'children' } as any"
            placeholder="请选择分组"
            check-strictly
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchGroupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatchGroup" :loading="batchLoading">确定</el-button>
      </template>
    </el-dialog>

    <input ref="importInputRef" type="file" accept=".xlsx,.xls" style="display: none" @change="handleImportFile" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download, Upload, ArrowDown, ArrowUp, Folder } from '@element-plus/icons-vue'
import { getDevices, updateDeviceStatus, batchGroup, importDevices, exportDevices, type Device, type DeviceQuery } from '@/api/device'
import { getGroupTree, type Group } from '@/api/group'
import { getUsers, type User } from '@/api/user'
import DeviceStatusTag from '@/components/DeviceStatusTag.vue'
import Pagination from '@/components/Pagination.vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const tableData = ref<Device[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])
const showAdvanced = ref(false)

const queryParams = reactive<DeviceQuery>({
  page: 1,
  size: 10,
  deviceCode: '',
  deviceName: '',
  deviceType: '',
  brand: '',
  status: '',
  groupId: undefined,
  responsibleUserId: undefined,
  ipAddress: '',
  serialNumber: ''
})

const deviceTypes = ref<{ label: string; value: string }[]>([])
const groupTree = ref<Group[]>([])
const userList = ref<User[]>([])

const statusDialogVisible = ref(false)
const statusLoading = ref(false)
const currentDevice = ref<Device | null>(null)
const statusForm = reactive({ status: '', reason: '' })

const batchGroupDialogVisible = ref(false)
const batchLoading = ref(false)
const batchGroupId = ref<number | undefined>()

const importInputRef = ref<HTMLInputElement>()

function getDeviceTypeLabel(type: string) {
  const item = deviceTypes.value.find((d) => d.value === type)
  return item?.label || type
}

async function fetchDictData() {
  try {
    const dicts = await import('@/api/dict')
    const items = await dicts.getDictItems('device_type')
    deviceTypes.value = items.map((item) => ({ label: item.itemLabel, value: item.itemValue }))
  } catch (e) {
    deviceTypes.value = [
      { label: '路由器', value: 'ROUTER' },
      { label: '交换机', value: 'SWITCH' },
      { label: '防火墙', value: 'FIREWALL' },
      { label: '服务器', value: 'SERVER' },
      { label: '光猫', value: 'ONT' },
      { label: 'AP', value: 'AP' },
      { label: '其他', value: 'OTHER' }
    ]
  }
}

async function fetchGroupTree() {
  try {
    groupTree.value = await getGroupTree()
  } catch (e: any) { ElMessage.error(e?.message || '加载数据失败') }
}

async function fetchUserList() {
  try {
    const res = await getUsers({ page: 1, size: 1000 })
    userList.value = res.records
  } catch (e: any) { ElMessage.error(e?.message || '加载数据失败') }
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = { ...queryParams }
    if (!params.deviceType) delete params.deviceType
    if (!params.status) delete params.status
    if (!params.groupId) delete params.groupId
    if (!params.responsibleUserId) delete params.responsibleUserId
    if (!params.ipAddress) delete params.ipAddress
    if (!params.serialNumber) delete params.serialNumber
    if (!params.brand) delete params.brand
    const res = await getDevices(params)
    tableData.value = res.records
    total.value = res.total
  } catch (e) {
    ElMessage.error((e as any)?.message || '获取设备列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryParams.page = 1
  fetchData()
}

function resetQuery() {
  Object.assign(queryParams, {
    page: 1,
    deviceCode: '',
    deviceName: '',
    deviceType: '',
    brand: '',
    status: '',
    groupId: undefined,
    responsibleUserId: undefined,
    ipAddress: '',
    serialNumber: ''
  })
  fetchData()
}

function handleSelectionChange(val: Device[]) {
  selectedIds.value = val.map((d) => d.id)
}

async function handleCommand(cmd: string, row: any) {
  if (cmd === 'status') {
    currentDevice.value = row
    statusForm.status = ''
    statusForm.reason = ''
    statusDialogVisible.value = true
  } else if (cmd === 'repairs') {
    router.push(`/devices/detail/${row.id}`)
  } else if (cmd === 'delete') {
    // ElMessageBox.confirm 在用户取消时会 reject，必须包在 try/catch 里
    try {
      await ElMessageBox.confirm(`确定要删除设备"${row.deviceName}"吗？`, '提示', { type: 'warning' })
    } catch {
      return
    }
    try {
      const { deleteDevice } = await import('@/api/device')
      await deleteDevice(row.id)
      ElMessage.success('删除成功')
      fetchData()
    } catch (e) {
      ElMessage.error((e as any)?.message || '删除失败')
    }
  }
}

async function submitStatusChange() {
  if (!currentDevice.value || !statusForm.status) {
    ElMessage.warning('请选择新状态')
    return
  }
  statusLoading.value = true
  try {
    await updateDeviceStatus(currentDevice.value.id, { status: statusForm.status, reason: statusForm.reason })
    ElMessage.success('状态变更成功')
    statusDialogVisible.value = false
    fetchData()
  } catch (e) {
    ElMessage.error((e as any)?.message || '状态变更失败')
  } finally {
    statusLoading.value = false
  }
}

function handleBatchGroup() {
  batchGroupId.value = undefined
  batchGroupDialogVisible.value = true
}

async function submitBatchGroup() {
  if (!batchGroupId.value) {
    ElMessage.warning('请选择目标分组')
    return
  }
  batchLoading.value = true
  try {
    await batchGroup({ deviceIds: selectedIds.value, groupId: batchGroupId.value })
    ElMessage.success('批量分组成功')
    batchGroupDialogVisible.value = false
    fetchData()
  } catch (e) {
    ElMessage.error((e as any)?.message || '批量分组失败')
  } finally {
    batchLoading.value = false
  }
}

function handleImport() {
  importInputRef.value?.click()
}

async function handleImportFile(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files?.length) return
  try {
    await importDevices(input.files[0])
    ElMessage.success('导入成功')
    fetchData()
  } catch (e) {
    ElMessage.error((e as any)?.message || '导入失败')
  }
  input.value = ''
}

async function handleExport() {
  try {
    const blob: any = await exportDevices({ ...queryParams } as DeviceQuery)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `设备列表_${new Date().toISOString().slice(0, 10)}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error((e as any)?.message || '导出失败')
  }
}

onMounted(() => {
  fetchDictData()
  fetchGroupTree()
  fetchUserList()
  fetchData()
})
</script>

<style lang="scss" scoped>
.search-form {
  padding: 0;
}
.table-toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  .toolbar-left, .toolbar-right {
    display: flex;
    gap: 8px;
  }
}
</style>
