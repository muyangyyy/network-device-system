<template>
  <div class="repair-accept" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>验收工单 - {{ order.workOrderNo }}</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>

      <el-descriptions :column="3" border size="small" style="margin-bottom: 20px">
        <el-descriptions-item label="工单号">{{ order.workOrderNo }}</el-descriptions-item>
        <el-descriptions-item label="设备编码">{{ order.deviceCode }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ order.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="维修类型">
          <RepairTypeTag :type="order.repairType" />
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <RepairStatusTag :status="order.status" />
        </el-descriptions-item>
        <el-descriptions-item label="维修人">{{ order.repairUserName }}</el-descriptions-item>
      </el-descriptions>

      <el-card shadow="never" style="margin-bottom: 20px">
        <template #header><span>故障描述</span></template>
        <p class="fault-desc">{{ order.faultDescription }}</p>
      </el-card>

      <el-card shadow="never" style="margin-bottom: 20px">
        <template #header><span>维修信息</span></template>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="维修方案">{{ order.repairSolution || '-' }}</el-descriptions-item>
          <el-descriptions-item label="维修结果">{{ order.repairResult || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ order.repairStartTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ order.repairEndTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 后端返回的是嵌套对象，必须按字段渲染；直接插值会显示成一整段 JSON -->
      <el-card shadow="never" style="margin-bottom: 20px" v-if="currentDetail">
        <template #header><span>维修明细</span></template>
        <RepairDetailPanel :type="order.repairType" :detail="currentDetail" />
      </el-card>

      <el-form ref="formRef" :model="acceptForm" :rules="acceptRules" label-width="100px" style="max-width: 600px">
        <el-form-item label="验收结果" prop="result">
          <el-radio-group v-model="acceptForm.result">
            <el-radio value="approve">通过</el-radio>
            <el-radio value="reject">退回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark" v-if="acceptForm.result === 'approve'">
          <el-input v-model="acceptForm.remark" type="textarea" :rows="3" placeholder="请输入验收备注（可选）" />
        </el-form-item>
        <el-form-item label="退回原因" prop="rejectReason" v-if="acceptForm.result === 'reject'">
          <el-input v-model="acceptForm.rejectReason" type="textarea" :rows="3" placeholder="请输入退回原因" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">提交验收</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { getRepairOrder, approveOrder, rejectOrder, type RepairOrder } from '@/api/repair'
import RepairTypeTag from '@/components/RepairTypeTag.vue'
import RepairStatusTag from '@/components/RepairStatusTag.vue'
import RepairDetailPanel from '@/components/RepairDetailPanel.vue'

const route = useRoute()
const router = useRouter()
const orderId = Number(route.params.id)

const loading = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const order = ref<RepairOrder>({} as RepairOrder)

const acceptForm = reactive({
  result: 'approve' as 'approve' | 'reject',
  remark: '',
  rejectReason: ''
})

/** 当前工单类型对应的维修明细对象（后端返回嵌套对象，不是字符串） */
const currentDetail = computed(() => {
  if (order.value.repairType === 'HARDWARE') return order.value.hardwareDetail || null
  if (order.value.repairType === 'DEBUG') return order.value.debugDetail || null
  if (order.value.repairType === 'OPTICAL') return order.value.opticalDetail || null
  return null
})

const acceptRules = {
  result: [{ required: true, message: '请选择验收结果', trigger: 'change' }],
  rejectReason: [{
    validator: (_rule: any, value: string, callback: any) => {
      if (acceptForm.result === 'reject' && !value) {
        callback(new Error('请输入退回原因'))
      } else {
        callback()
      }
    },
    trigger: 'blur'
  }]
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

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    if (acceptForm.result === 'approve') {
      await approveOrder(orderId, { remark: acceptForm.remark })
      ElMessage.success('验收通过')
    } else {
      // 后端 RejectDTO 用 @JsonAlias 兼容 rejectReason
      await rejectOrder(orderId, { rejectReason: acceptForm.rejectReason })
      ElMessage.success('已退回')
    }
    router.push(`/repairs/detail/${orderId}`)
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  fetchOrder()
})
</script>

<style lang="scss" scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.fault-desc {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #303133;
}
</style>
