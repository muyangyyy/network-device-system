<template>
  <div class="repair-process" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>处理工单 - {{ order.workOrderNo }}</span>
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
        <el-descriptions-item label="优先级">
          <el-tag :type="getPriorityType(order.priority)">{{ getPriorityLabel(order.priority) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="报修人">{{ order.reporterName }}</el-descriptions-item>
      </el-descriptions>

      <el-card shadow="never" style="margin-bottom: 20px">
        <template #header><span>故障描述</span></template>
        <p class="fault-desc">{{ order.faultDescription }}</p>
      </el-card>

      <!-- 后端状态机：只有 PROCESSING / DELAYED / REPAIR_AGAIN 允许提交完成，
           ASSIGNED / REJECTED 必须先调用 /repairs/{id}/start 进入「处理中」 -->
      <el-alert
        v-if="canStart"
        type="warning"
        :closable="false"
        show-icon
        title="工单尚未开始处理"
        description="请先点击下方「开始处理」，工单进入「处理中」后再填写维修信息并提交完成。"
        style="margin-bottom: 16px"
      />
      <el-alert
        v-else-if="!canComplete"
        type="info"
        :closable="false"
        show-icon
        :title="`当前状态（${order.status || '未知'}）不允许提交完成`"
        description="仅「处理中」「已延期」「返修」三种状态的工单可以提交完成。"
        style="margin-bottom: 16px"
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 760px">
        <template v-if="canComplete">
          <el-divider content-position="left">维修信息</el-divider>
          <el-form-item label="维修方案" prop="repairSolution">
            <el-input v-model="form.repairSolution" type="textarea" :rows="3" placeholder="请描述维修方案" />
          </el-form-item>
          <el-form-item label="维修结果" prop="repairResult">
            <el-input v-model="form.repairResult" type="textarea" :rows="3" placeholder="请描述维修结果" />
          </el-form-item>
          <!-- 维修开始/结束时间不在此填写：repair_work_order 无对应列，
               后端由「处理中」流转日志与完成时间自动推导 -->

          <template v-if="order.repairType === 'HARDWARE'">
            <el-divider content-position="left">硬件维修详情</el-divider>
            <el-form-item label="损坏部件">
              <el-input v-model="hardwareForm.damagedComponent" placeholder="请输入损坏部件" />
            </el-form-item>
            <el-form-item label="更换配件名称">
              <el-input v-model="hardwareForm.replacementPartName" placeholder="请输入更换配件名称" />
            </el-form-item>
            <el-form-item label="更换配件型号">
              <el-input v-model="hardwareForm.replacementPartModel" placeholder="请输入更换配件型号" />
            </el-form-item>
            <el-form-item label="更换数量">
              <el-input-number v-model="hardwareForm.replacementPartQuantity" :min="0" />
            </el-form-item>
            <el-form-item label="配件费用">
              <el-input-number v-model="hardwareForm.replacementPartCost" :min="0" :precision="2" />
            </el-form-item>
            <el-form-item label="旧件处理方式">
              <el-input v-model="hardwareForm.oldPartDisposalMethod" placeholder="如：回收 / 报废 / 留存" />
            </el-form-item>
            <el-form-item label="硬件故障代码">
              <el-input v-model="hardwareForm.hardwareFailureCode" placeholder="请输入硬件故障代码" />
            </el-form-item>
            <el-form-item label="是否在保">
              <el-select v-model="hardwareForm.whetherUnderWarranty" placeholder="请选择" clearable style="width: 100%">
                <el-option label="在保" :value="1" />
                <el-option label="不在保" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item label="供应商">
              <el-input v-model="hardwareForm.supplier" placeholder="请输入供应商" />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="hardwareForm.remark" type="textarea" :rows="3" placeholder="请详细描述硬件维修情况" />
            </el-form-item>
          </template>

          <template v-if="order.repairType === 'DEBUG'">
            <el-divider content-position="left">调试详情</el-divider>
            <el-form-item label="配置变更内容">
              <el-input v-model="debugForm.configurationChangeContent" type="textarea" :rows="2" placeholder="请描述配置变更内容" />
            </el-form-item>
            <el-form-item label="原固件版本">
              <el-input v-model="debugForm.oldFirmwareVersion" placeholder="请输入原固件版本" />
            </el-form-item>
            <el-form-item label="新固件版本">
              <el-input v-model="debugForm.newFirmwareVersion" placeholder="请输入新固件版本" />
            </el-form-item>
            <el-form-item label="原IP地址">
              <el-input v-model="debugForm.oldIpAddress" placeholder="请输入原IP地址" />
            </el-form-item>
            <el-form-item label="新IP地址">
              <el-input v-model="debugForm.newIpAddress" placeholder="请输入新IP地址" />
            </el-form-item>
            <el-form-item label="原网关">
              <el-input v-model="debugForm.oldGateway" placeholder="请输入原网关" />
            </el-form-item>
            <el-form-item label="新网关">
              <el-input v-model="debugForm.newGateway" placeholder="请输入新网关" />
            </el-form-item>
            <el-form-item label="原VLAN">
              <el-input v-model="debugForm.oldVlan" placeholder="请输入原VLAN" />
            </el-form-item>
            <el-form-item label="新VLAN">
              <el-input v-model="debugForm.newVlan" placeholder="请输入新VLAN" />
            </el-form-item>
            <el-form-item label="路由变更">
              <el-input v-model="debugForm.routeChangeContent" type="textarea" :rows="2" placeholder="请描述路由变更内容" />
            </el-form-item>
            <el-form-item label="防火墙策略变更">
              <el-input v-model="debugForm.firewallPolicyChange" type="textarea" :rows="2" placeholder="请描述防火墙策略变更" />
            </el-form-item>
            <el-form-item label="权限变更">
              <el-input v-model="debugForm.permissionChangeContent" type="textarea" :rows="2" placeholder="请描述权限变更内容" />
            </el-form-item>
            <el-form-item label="网络参数变更记录">
              <el-input v-model="debugForm.networkParameterChangeRecord" type="textarea" :rows="2" placeholder="请记录网络参数变更" />
            </el-form-item>
            <el-form-item label="回滚方案">
              <el-input v-model="debugForm.rollbackPlan" type="textarea" :rows="2" placeholder="请描述回滚方案" />
            </el-form-item>
            <el-form-item label="测试结果">
              <el-input v-model="debugForm.testResult" type="textarea" :rows="2" placeholder="请描述测试结果" />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="debugForm.remark" type="textarea" :rows="3" placeholder="请详细描述调试情况" />
            </el-form-item>
          </template>

          <template v-if="order.repairType === 'OPTICAL'">
            <el-divider content-position="left">光路维修详情</el-divider>
            <el-form-item label="故障光路点">
              <el-input v-model="opticalForm.faultOpticalPoint" placeholder="请输入故障光路点" />
            </el-form-item>
            <el-form-item label="光路名称">
              <el-input v-model="opticalForm.opticalRouteName" placeholder="请输入光路名称" />
            </el-form-item>
            <el-form-item label="光缆段">
              <el-input v-model="opticalForm.cableSection" placeholder="请输入光缆信息" />
            </el-form-item>
            <el-form-item label="光缆长度(km)">
              <el-input-number v-model="opticalForm.cableLength" :min="0" :precision="2" />
            </el-form-item>
            <el-form-item label="维修前光功率">
              <el-input-number v-model="opticalForm.opticalPowerBefore" :precision="2" :step="0.1" />
            </el-form-item>
            <el-form-item label="维修后光功率">
              <el-input-number v-model="opticalForm.opticalPowerAfter" :precision="2" :step="0.1" />
            </el-form-item>
            <el-form-item label="维修前衰减值">
              <el-input-number v-model="opticalForm.attenuationBefore" :precision="2" :step="0.1" />
            </el-form-item>
            <el-form-item label="维修后衰减值">
              <el-input-number v-model="opticalForm.attenuationAfter" :precision="2" :step="0.1" />
            </el-form-item>
            <el-form-item label="波长">
              <el-input v-model="opticalForm.wavelength" placeholder="如：1310nm / 1550nm" />
            </el-form-item>
            <el-form-item label="分光器状态">
              <el-input v-model="opticalForm.splitterStatus" placeholder="请输入分光器状态" />
            </el-form-item>
            <el-form-item label="跳纤状态">
              <el-input v-model="opticalForm.jumperStatus" placeholder="请输入跳纤状态" />
            </el-form-item>
            <el-form-item label="光缆损坏描述">
              <el-input v-model="opticalForm.cableDamageDescription" type="textarea" :rows="2" placeholder="请描述光缆损坏情况" />
            </el-form-item>
            <el-form-item label="链路测试结果">
              <el-input v-model="opticalForm.linkTestResult" type="textarea" :rows="2" placeholder="请描述链路测试结果" />
            </el-form-item>
            <el-form-item label="测试工具">
              <el-input v-model="opticalForm.testTool" placeholder="如：OTDR / 光功率计" />
            </el-form-item>
            <el-form-item label="测试人">
              <el-input v-model="opticalForm.testPerson" placeholder="请输入测试人" />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="opticalForm.remark" type="textarea" :rows="3" placeholder="请详细描述光路维修情况" />
            </el-form-item>
          </template>
        </template>

        <el-divider content-position="left">附件</el-divider>
        <el-form-item>
          <FileUpload :action="`/api/repairs/${orderId}/attachments`" @success="handleUploadSuccess" :file-list="uploadFiles" />
        </el-form-item>

        <el-form-item>
          <el-button v-if="canStart" type="primary" @click="handleStart" :loading="startLoading">开始处理</el-button>
          <el-button v-if="canComplete" type="primary" @click="handleSubmit" :loading="submitLoading">提交完成</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import {
  getRepairOrder,
  startOrder,
  completeOrder,
  type RepairOrder,
  type CompleteOrderPayload
} from '@/api/repair'
import RepairTypeTag from '@/components/RepairTypeTag.vue'
import FileUpload from '@/components/FileUpload.vue'
import type { UploadFile } from 'element-plus'

const route = useRoute()
const router = useRouter()
const orderId = Number(route.params.id)

const loading = ref(false)
const submitLoading = ref(false)
const startLoading = ref(false)
const formRef = ref<FormInstance>()
const order = ref<RepairOrder>({} as RepairOrder)
const uploadFiles = ref<UploadFile[]>([])

/** 后端 startProcessing 仅接受 ASSIGNED / REJECTED */
const canStart = computed(() => ['ASSIGNED', 'REJECTED'].includes(order.value.status))
/** 后端 completeOrder 仅接受 PROCESSING / DELAYED / REPAIR_AGAIN */
const canComplete = computed(() => ['PROCESSING', 'DELAYED', 'REPAIR_AGAIN'].includes(order.value.status))

const form = reactive({
  repairSolution: '',
  repairResult: ''
})

// 以下三组字段名必须与后端 CompleteDTO 内嵌 DTO 完全一致，否则会被 Jackson 静默丢弃
const hardwareForm = reactive({
  damagedComponent: '',
  replacementPartName: '',
  replacementPartModel: '',
  replacementPartQuantity: undefined as number | undefined,
  replacementPartCost: undefined as number | undefined,
  oldPartDisposalMethod: '',
  hardwareFailureCode: '',
  whetherUnderWarranty: undefined as number | undefined,
  supplier: '',
  remark: ''
})

const debugForm = reactive({
  configurationChangeContent: '',
  oldFirmwareVersion: '',
  newFirmwareVersion: '',
  oldIpAddress: '',
  newIpAddress: '',
  oldGateway: '',
  newGateway: '',
  oldVlan: '',
  newVlan: '',
  routeChangeContent: '',
  firewallPolicyChange: '',
  permissionChangeContent: '',
  networkParameterChangeRecord: '',
  rollbackPlan: '',
  testResult: '',
  remark: ''
})

const opticalForm = reactive({
  faultOpticalPoint: '',
  opticalRouteName: '',
  cableSection: '',
  cableLength: undefined as number | undefined,
  opticalPowerBefore: undefined as number | undefined,
  opticalPowerAfter: undefined as number | undefined,
  attenuationBefore: undefined as number | undefined,
  attenuationAfter: undefined as number | undefined,
  wavelength: '',
  splitterStatus: '',
  jumperStatus: '',
  cableDamageDescription: '',
  linkTestResult: '',
  testTool: '',
  testPerson: '',
  remark: ''
})

const rules = {
  repairSolution: [{ required: true, message: '请输入维修方案', trigger: 'blur' }],
  repairResult: [{ required: true, message: '请输入维修结果', trigger: 'blur' }]
}

function getPriorityType(p: string) {
  const map: Record<string, string> = { URGENT: 'danger', HIGH: 'warning', MEDIUM: '', LOW: 'info' }
  return (map[p] || 'info') as any
}

function getPriorityLabel(p: string) {
  const map: Record<string, string> = { URGENT: '紧急', HIGH: '高', MEDIUM: '中', LOW: '低' }
  return map[p] || p
}

function handleUploadSuccess(response: any, file: UploadFile) {
  uploadFiles.value.push(file)
}

async function fetchOrder() {
  loading.value = true
  try {
    order.value = await getRepairOrder(orderId)
    uploadFiles.value = (order.value.attachments || []).map((a) => ({
      name: a.fileName,
      url: a.filePath
    } as UploadFile))
  } catch (e: any) {
    // 主数据加载失败不能静默：页面会空白且无任何提示，用户无法判断发生了什么
    ElMessage.error(e?.message || '加载工单失败')
  } finally { loading.value = false }
}

async function handleStart() {
  startLoading.value = true
  try {
    await startOrder(orderId)
    ElMessage.success('已开始处理，请填写维修信息')
    await fetchOrder()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    startLoading.value = false
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  if (!canComplete.value) {
    ElMessage.warning('当前状态不允许提交完成，请先点击「开始处理」')
    return
  }
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    const data: CompleteOrderPayload = {
      repairSolution: form.repairSolution,
      repairResult: form.repairResult
    }
    // 必须传对象：后端 DTO 是嵌套 POJO，传 JSON.stringify 的字符串会反序列化失败并返回 400
    if (order.value.repairType === 'HARDWARE') {
      data.hardwareDetail = { ...hardwareForm }
    } else if (order.value.repairType === 'DEBUG') {
      data.debugDetail = { ...debugForm }
    } else if (order.value.repairType === 'OPTICAL') {
      data.opticalDetail = { ...opticalForm }
    }
    await completeOrder(orderId, data)
    ElMessage.success('提交成功')
    router.push(`/repairs/detail/${orderId}`)
  } catch (e: any) {
    ElMessage.error(e.message || '提交失败')
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
