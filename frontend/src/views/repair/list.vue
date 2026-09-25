<template>
  <div class="repair-list">
    <el-card shadow="never">
      <el-form :model="queryParams" ref="queryFormRef" :inline="true" class="search-form">
        <el-form-item label="工单号" prop="workOrderNo">
          <el-input v-model="queryParams.workOrderNo" placeholder="请输入工单号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="设备编码" prop="deviceCode">
          <el-input v-model="queryParams.deviceCode" placeholder="请输入设备编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="维修类型" prop="repairType">
          <el-select v-model="queryParams.repairType" placeholder="全部" clearable>
            <el-option label="硬件维修" value="HARDWARE" />
            <el-option label="调试维修" value="DEBUG" />
            <el-option label="光路维修" value="OPTICAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部" clearable>
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已提交" value="SUBMITTED" />
            <el-option label="已分配" value="ASSIGNED" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已验收" value="ACCEPTED" />
            <el-option label="已退回" value="REJECTED" />
            <el-option label="已延期" value="DELAYED" />
            <el-option label="返修" value="REPAIR_AGAIN" />
            <el-option label="已关闭" value="CLOSED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="queryParams.priority" placeholder="全部" clearable>
            <el-option label="紧急" value="URGENT" />
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="timeRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-button type="primary" @click="$router.push('/repairs/create')" v-if="userStore.hasPermission('repair:add')">
            <el-icon><Plus /></el-icon>新建维修记录
          </el-button>
          <el-button @click="handleExport" :loading="exportLoading" v-if="userStore.hasPermission('repair:export')">
            <el-icon><Download /></el-icon>导出
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
        <el-table-column prop="workOrderNo" label="工单号" width="160" show-overflow-tooltip fixed="left" />
        <el-table-column prop="deviceCode" label="设备编码" width="140" show-overflow-tooltip />
        <el-table-column prop="deviceName" label="设备名称" min-width="130" show-overflow-tooltip />
        <el-table-column label="维修类型" width="100">
          <template #default="{ row }">
            <RepairTypeTag :type="row.repairType" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <RepairStatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="是否完成" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="isFinished(row) ? 'success' : 'warning'" effect="light">
              {{ isFinished(row) ? '已完成' : '未完成' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="80">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)">{{ getPriorityLabel(row.priority) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reporterName" label="报修人" width="90" />
        <el-table-column label="维修人" width="100">
          <template #default="{ row }">
            {{ row.repairUserName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="维修日期" width="160">
          <template #default="{ row }">
            {{ row.completedTime || row.repairEndTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="faultDescription" label="维修内容" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.faultDescription || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="$router.push(`/repairs/detail/${row.id}`)" v-if="userStore.hasPermission('repair:view')">查看</el-button>
            <el-button
              v-if="canProcess(row) && userStore.hasPermission('repair:process')"
              type="primary" link size="small"
              @click="$router.push(`/repairs/process/${row.id}`)"
            >处理</el-button>
            <el-button
              v-if="canAccept(row) && userStore.hasPermission('repair:accept')"
              type="primary" link size="small"
              @click="$router.push(`/repairs/accept/${row.id}`)"
            >验收</el-button>
            <el-button
              v-if="userStore.hasPermission('repair:delete')"
              type="danger" link size="small"
              @click="handleDelete(row)"
            >删除</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download } from '@element-plus/icons-vue'
import { getRepairOrders, deleteRepairOrder, exportOrders, type RepairOrder, type RepairOrderQuery } from '@/api/repair'
import { exportExcel } from '@/utils/export'
import RepairTypeTag from '@/components/RepairTypeTag.vue'
import RepairStatusTag from '@/components/RepairStatusTag.vue'
import Pagination from '@/components/Pagination.vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const loading = ref(false)
const exportLoading = ref(false)
const tableData = ref<RepairOrder[]>([])
const total = ref(0)
const timeRange = ref<[string, string] | null>(null)

const queryParams = reactive<RepairOrderQuery>({
  page: 1,
  size: 10,
  workOrderNo: '',
  deviceCode: '',
  deviceName: '',
  repairType: '',
  status: '',
  priority: '',
  faultTimeStart: '',
  faultTimeEnd: ''
})

function getPriorityType(p: string) {
  const map: Record<string, string> = { URGENT: 'danger', HIGH: 'warning', MEDIUM: '', LOW: 'info' }
  return (map[p] || 'info') as any
}

function getPriorityLabel(p: string) {
  const map: Record<string, string> = { URGENT: '紧急', HIGH: '高', MEDIUM: '中', LOW: '低' }
  return map[p] || p
}

// 与后端保持一致：startProcessing 接受 ASSIGNED/REJECTED，
// completeOrder 接受 PROCESSING/DELAYED/REPAIR_AGAIN。
// 只放 ASSIGNED/REJECTED 会让「处理中」的工单离开页面后再也回不去。
function canProcess(row: any) {
  return ['ASSIGNED', 'REJECTED', 'PROCESSING', 'DELAYED', 'REPAIR_AGAIN'].includes(row.status)
}

function canAccept(row: any) {
  return row.status === 'COMPLETED'
}

// 已完成判定：完成、已验收、已关闭（含归档）均视为「已完成维修」
function isFinished(row: any) {
  return ['COMPLETED', 'ACCEPTED', 'CLOSED'].includes(row.status)
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(
      `确定要删除维修记录「${row.workOrderNo}」吗？删除后不可恢复。`,
      '删除维修记录',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteRepairOrder(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    ElMessage.error((e as any)?.message || '删除失败')
  }
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = { ...queryParams }
    if (timeRange.value) {
      params.faultTimeStart = timeRange.value[0]
      params.faultTimeEnd = timeRange.value[1]
    }
    const res = await getRepairOrders(params)
    tableData.value = res.records
    total.value = res.total
  } catch (e) {
    ElMessage.error((e as any)?.message || '获取工单列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryParams.page = 1
  fetchData()
}

function resetQuery() {
  Object.assign(queryParams, { page: 1, workOrderNo: '', deviceCode: '', deviceName: '', repairType: '', status: '', priority: '', faultTimeStart: '', faultTimeEnd: '' })
  timeRange.value = null
  fetchData()
}

async function handleExport() {
  const columns = [
    { label: '工单号', prop: 'workOrderNo' },
    { label: '设备编码', prop: 'deviceCode' },
    { label: '设备名称', prop: 'deviceName' },
    { label: '维修类型', prop: 'repairType' },
    { label: '状态', prop: 'status' },
    { label: '是否完成', prop: 'finishedText' },
    { label: '优先级', prop: 'priority' },
    { label: '报修人', prop: 'reporterName' },
    { label: '维修人', prop: 'repairUserName' },
    { label: '维修日期', prop: 'repairDateText' },
    { label: '维修内容', prop: 'faultDescription' },
    { label: '创建时间', prop: 'createTime' }
  ]
  exportLoading.value = true
  try {
    const params: any = { ...queryParams }
    if (timeRange.value) {
      params.faultTimeStart = timeRange.value[0]
      params.faultTimeEnd = timeRange.value[1]
    }
    const rows = await exportOrders(params)
    const list = (Array.isArray(rows) ? rows : tableData.value).map((r: any) => ({
      ...r,
      finishedText: ['COMPLETED', 'ACCEPTED', 'CLOSED'].includes(r.status) ? '已完成' : '未完成',
      repairDateText: r.completedTime || r.repairEndTime || '-'
    }))
    if (!list.length) {
      ElMessage.warning('没有可导出的数据')
      return
    }
    exportExcel(list, columns, '维修记录列表')
  } catch (e) {
    ElMessage.error((e as any)?.message || '导出失败')
  } finally {
    exportLoading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.search-form { padding: 0; }
.table-toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  .toolbar-left { display: flex; gap: 8px; }
}
</style>
