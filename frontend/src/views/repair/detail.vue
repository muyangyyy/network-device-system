<template>
  <div class="repair-detail" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>工单详情 - {{ order.workOrderNo }}</span>
          <div>
            <el-button @click="$router.back()">返回</el-button>
            <!-- 按钮状态条件必须与后端 Service 的前置状态校验逐一对应，
                 否则会出现「按钮能点但接口报状态错误」 -->
            <el-button v-if="order.status === 'DRAFT' && userStore.hasPermission('repair:add')" type="primary" @click="handleSubmitOrder">提交工单</el-button>
            <el-button v-if="order.status === 'SUBMITTED' && userStore.hasPermission('repair:assign')" type="warning" @click="showAssignDialog = true">指派</el-button>
            <!-- 处理入口需覆盖 PROCESSING/DELAYED/REPAIR_AGAIN，否则离开处理页后无法再回去提交 -->
            <el-button v-if="canProcess && userStore.hasPermission('repair:process')" type="primary" @click="$router.push(`/repairs/process/${orderId}`)">处理</el-button>
            <el-button v-if="order.status === 'COMPLETED' && userStore.hasPermission('repair:accept')" type="success" @click="$router.push(`/repairs/accept/${orderId}`)">验收</el-button>
            <el-button v-if="order.status === 'ACCEPTED' && userStore.hasPermission('repair:accept')" type="success" @click="handleClose">关闭工单</el-button>
            <el-button v-if="order.status === 'CLOSED' && userStore.hasPermission('repair:process')" type="warning" @click="handleRepairAgain">返修</el-button>
            <!-- 后端 cancelOrder 禁止 CANCELLED / ACCEPTED / CLOSED -->
            <el-button v-if="canCancel && userStore.hasPermission('repair:add')" type="danger" @click="handleCancel">取消工单</el-button>
          </div>
        </div>
      </template>

      <el-steps :active="stepActive" finish-status="success" align-center style="margin-bottom: 32px">
        <el-step title="已提交" :description="getLogTime('SUBMITTED')" />
        <el-step title="已指派" :description="getLogTime('ASSIGNED')" />
        <el-step title="处理中" :description="getLogTime('PROCESSING')" />
        <el-step title="已完成" :description="getLogTime('COMPLETED')" />
        <el-step title="已验收" :description="getLogTime('ACCEPTED')" />
      </el-steps>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-card shadow="never">
            <template #header><span>工单信息</span></template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="工单号">{{ order.workOrderNo }}</el-descriptions-item>
              <el-descriptions-item label="维修类型">
                <RepairTypeTag :type="order.repairType" />
              </el-descriptions-item>
              <el-descriptions-item label="状态">
                <RepairStatusTag :status="order.status" />
              </el-descriptions-item>
              <el-descriptions-item label="优先级">
                <el-tag :type="getPriorityType(order.priority)">{{ getPriorityLabel(order.priority) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="报修人">{{ order.reporterName }}</el-descriptions-item>
              <el-descriptions-item label="维修人">{{ order.repairUserName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="故障时间">{{ order.faultTime }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ order.createTime }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never">
            <template #header><span>设备信息</span></template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="设备编码">{{ order.deviceCode }}</el-descriptions-item>
              <el-descriptions-item label="设备名称">{{ order.deviceName }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" style="margin-top: 12px">
        <template #header><span>故障描述</span></template>
        <p class="fault-description">{{ order.faultDescription }}</p>
      </el-card>

      <el-card shadow="never" style="margin-top: 12px" v-if="order.repairSolution || order.repairResult">
        <template #header><span>维修信息</span></template>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="维修方案">{{ order.repairSolution || '-' }}</el-descriptions-item>
          <el-descriptions-item label="维修结果">{{ order.repairResult || '-' }}</el-descriptions-item>
          <el-descriptions-item label="维修开始时间">{{ order.repairStartTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="维修结束时间">{{ order.repairEndTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 后端返回的是嵌套对象，必须按字段渲染；直接插值会显示成一整段 JSON -->
      <el-card shadow="never" style="margin-top: 12px" v-if="hasDetail">
        <template #header><span>维修明细</span></template>
        <RepairDetailPanel :type="order.repairType" :detail="currentDetail" />
      </el-card>

      <el-card shadow="never" style="margin-top: 12px" v-if="order.rejectReason">
        <template #header><span>退回原因</span></template>
        <p style="color: #f56c6c">{{ order.rejectReason }}</p>
      </el-card>

      <el-card shadow="never" style="margin-top: 12px">
        <template #header><span>操作日志</span></template>
        <el-timeline>
          <el-timeline-item
            v-for="log in order.logs"
            :key="log.id"
            :timestamp="log.createTime"
            placement="top"
          >
            <p><strong>{{ log.operatorName }}</strong> {{ getLogAction(log) }}</p>
            <p v-if="log.remark" style="color: #909399; font-size: 12px">{{ log.remark }}</p>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-if="!order.logs || order.logs.length === 0" description="暂无日志" :image-size="60" />
      </el-card>
    </el-card>

    <el-dialog v-model="showAssignDialog" title="指派维修人员" width="400px">
      <el-form label-width="80px">
        <el-form-item label="维修人员">
          <el-select v-model="assignUserId" placeholder="请选择维修人员" filterable>
            <el-option v-for="u in userList" :key="u.id" :label="u.nickname" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAssignDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAssign" :loading="assignLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRepairOrder, submitOrder, assignOrder, cancelOrder, closeOrder, repairAgainOrder, type RepairOrder } from '@/api/repair'
import { getUsers, type User } from '@/api/user'
import { useUserStore } from '@/store/user'
import RepairTypeTag from '@/components/RepairTypeTag.vue'
import RepairStatusTag from '@/components/RepairStatusTag.vue'
import RepairDetailPanel from '@/components/RepairDetailPanel.vue'

const route = useRoute()
const userStore = useUserStore()
const orderId = Number(route.params.id)

const loading = ref(false)
const order = ref<RepairOrder>({} as RepairOrder)
const showAssignDialog = ref(false)
const assignUserId = ref<number | undefined>()
const assignLoading = ref(false)
const userList = ref<User[]>([])

// 步骤条：后端写入的状态是 SUBMITTED（不是 PENDING）。
// REPAIR_AGAIN 视同「处理中」，CLOSED 视同全部完成。
const stepActive = computed(() => {
  const statusMap: Record<string, number> = {
    SUBMITTED: 0, ASSIGNED: 1, PROCESSING: 2, COMPLETED: 3, ACCEPTED: 4,
    REJECTED: 2, DELAYED: 2, REPAIR_AGAIN: 2, CLOSED: 5, CANCELLED: -1
  }
  return statusMap[order.value.status] ?? 0
})

/** 后端 startProcessing 接受 ASSIGNED/REJECTED；completeOrder 接受 PROCESSING/DELAYED/REPAIR_AGAIN */
const canProcess = computed(() =>
  ['ASSIGNED', 'REJECTED', 'PROCESSING', 'DELAYED', 'REPAIR_AGAIN'].includes(order.value.status)
)

/** 后端 cancelOrder 禁止 CANCELLED / ACCEPTED / CLOSED */
const canCancel = computed(() =>
  !['CANCELLED', 'ACCEPTED', 'CLOSED'].includes(order.value.status)
)

/** 当前工单类型对应的维修明细对象（后端返回嵌套对象，不是字符串） */
const currentDetail = computed(() => {
  if (order.value.repairType === 'HARDWARE') return order.value.hardwareDetail || null
  if (order.value.repairType === 'DEBUG') return order.value.debugDetail || null
  if (order.value.repairType === 'OPTICAL') return order.value.opticalDetail || null
  return null
})

const hasDetail = computed(() => !!currentDetail.value)

function getPriorityType(p: string) {
  const map: Record<string, string> = { URGENT: 'danger', HIGH: 'warning', MEDIUM: '', LOW: 'info' }
  return (map[p] || 'info') as any
}

function getPriorityLabel(p: string) {
  const map: Record<string, string> = { URGENT: '紧急', HIGH: '高', MEDIUM: '中', LOW: '低' }
  return map[p] || p
}

function getLogTime(status: string) {
  const log = order.value.logs?.find((l) => l.toStatus === status)
  return log?.createTime || ''
}

function getLogAction(log: any) {
  const map: Record<string, string> = {
    SUBMITTED: '提交了工单',
    ASSIGNED: '指派了工单',
    PROCESSING: '开始处理工单',
    COMPLETED: '完成了工单',
    ACCEPTED: '验收通过',
    REJECTED: '退回了工单',
    DELAYED: '延期了工单',
    REPAIR_AGAIN: '发起了返修',
    CLOSED: '关闭了工单',
    CANCELLED: '取消了工单'
  }
  return map[log.toStatus] || `将状态变更为 ${log.toStatus}`
}

async function fetchOrder() {
  loading.value = true
  try {
    order.value = await getRepairOrder(orderId)
  } catch (e: any) {
    // 主数据加载失败不能静默：页面会空白且无任何提示，用户无法判断发生了什么
    ElMessage.error(e?.message || '加载工单失败')
  } finally { loading.value = false }
}

async function fetchUserList() {
  try {
    const res = await getUsers({ page: 1, size: 1000 })
    userList.value = res.records
  } catch (e: any) { ElMessage.error(e?.message || '加载数据失败') }
}

async function handleSubmitOrder() {
  try {
    await ElMessageBox.confirm('确定要提交此工单吗？', '提示')
  } catch {
    return
  }
  try {
    await submitOrder(orderId)
    ElMessage.success('提交成功')
    fetchOrder()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleAssign() {
  if (!assignUserId.value) {
    ElMessage.warning('请选择维修人员')
    return
  }
  assignLoading.value = true
  try {
    await assignOrder(orderId, { repairUserId: assignUserId.value })
    ElMessage.success('指派成功')
    showAssignDialog.value = false
    fetchOrder()
  } catch (e: any) {
    ElMessage.error(e.message || '指派失败')
  } finally {
    assignLoading.value = false
  }
}

async function handleCancel() {
  try {
    await ElMessageBox.confirm('确定要取消此工单吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await cancelOrder(orderId)
    ElMessage.success('工单已取消')
    fetchOrder()
  } catch (e: any) {
    ElMessage.error(e.message || '取消失败')
  }
}

async function handleClose() {
  try {
    await ElMessageBox.confirm('关闭后工单将归档，如需再次维修可发起「返修」。确定关闭吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await closeOrder(orderId)
    ElMessage.success('工单已关闭')
    fetchOrder()
  } catch (e: any) {
    ElMessage.error(e.message || '关闭失败')
  }
}

async function handleRepairAgain() {
  try {
    await ElMessageBox.confirm('确定要发起返修吗？工单将回到「返修」状态并需要重新处理。', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await repairAgainOrder(orderId)
    ElMessage.success('已发起返修')
    fetchOrder()
  } catch (e: any) {
    ElMessage.error(e.message || '返修失败')
  }
}

onMounted(() => {
  fetchOrder()
  fetchUserList()
})
</script>

<style lang="scss" scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.fault-description {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #303133;
}
</style>
