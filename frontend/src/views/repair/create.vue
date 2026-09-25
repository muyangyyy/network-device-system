<template>
  <div class="repair-create">
    <el-card shadow="never">
      <div class="page-header">
        <span class="title">新建维修记录</span>
        <el-button @click="$router.push('/repairs')">返回</el-button>
      </div>

      <el-steps :active="step" align-center finish-status="success" class="steps">
        <el-step title="选择设备" />
        <el-step title="填写故障信息" />
        <el-step title="确认提交" />
      </el-steps>

      <!-- 第一步：选择设备 -->
      <div v-show="step === 0">
        <el-form :inline="true" class="search-form">
          <el-form-item label="搜索设备">
            <el-input
              v-model="deviceKeyword"
              placeholder="输入设备编码或名称搜索"
              clearable
              style="width: 260px"
              @keyup.enter="searchDevices"
              @input="onKeywordInput"
            >
              <template #suffix>
                <el-icon style="cursor: pointer" @click="searchDevices"><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>
        </el-form>

        <el-form label-width="90px">
          <el-form-item label="选中设备" required>
            <span class="selected-device">
              {{ selectedDevice ? `${selectedDevice.deviceCode} - ${selectedDevice.deviceName}` : '未选择设备' }}
            </span>
          </el-form-item>
        </el-form>

        <el-table
          v-loading="deviceLoading"
          :data="deviceOptions"
          border
          stripe
          max-height="320"
          highlight-current-row
          :row-class-name="deviceRowClass"
          @row-click="selectDevice"
        >
          <el-table-column width="50" align="center">
            <template #default="{ row }">
              <el-radio :model-value="selectedDevice?.id" :value="row.id" @change="selectDevice(row)"><span></span></el-radio>
            </template>
          </el-table-column>
          <el-table-column prop="deviceCode" label="设备编码" width="180" show-overflow-tooltip />
          <el-table-column prop="deviceName" label="设备名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="deviceType" label="设备类型" width="110">
            <template #default="{ row }">
              {{ deviceTypeLabel(row.deviceType) }}
            </template>
          </el-table-column>
          <el-table-column prop="ipAddress" label="IP地址" width="150" />
          <el-table-column prop="statusName" label="状态" width="100" />
        </el-table>
      </div>

      <!-- 第二步：填写故障信息 -->
      <div v-show="step === 1">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 640px">
          <el-form-item label="选中设备">
            <span class="selected-device">
              {{ selectedDevice ? `${selectedDevice.deviceCode} - ${selectedDevice.deviceName}` : '-' }}
            </span>
          </el-form-item>
          <el-form-item label="故障时间" prop="faultTime">
            <el-date-picker
              v-model="form.faultTime"
              type="datetime"
              placeholder="请选择故障时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="维修类型" prop="repairType">
            <el-select v-model="form.repairType" placeholder="请选择维修类型" style="width: 100%">
              <el-option label="硬件维修" value="HARDWARE" />
              <el-option label="调试维修" value="DEBUG" />
              <el-option label="光路维修" value="OPTICAL" />
            </el-select>
          </el-form-item>
          <el-form-item label="优先级" prop="priority">
            <el-select v-model="form.priority" placeholder="请选择优先级" style="width: 100%">
              <el-option label="紧急" value="URGENT" />
              <el-option label="高" value="HIGH" />
              <el-option label="中" value="MEDIUM" />
              <el-option label="低" value="LOW" />
            </el-select>
          </el-form-item>
          <el-form-item label="故障描述" prop="faultDescription">
            <el-input
              v-model="form.faultDescription"
              type="textarea"
              :rows="4"
              placeholder="请详细描述故障现象"
            />
          </el-form-item>
        </el-form>
      </div>

      <!-- 第三步：确认提交 -->
      <div v-show="step === 2">
        <el-descriptions :column="1" border style="max-width: 720px" class="confirm-box">
          <el-descriptions-item label="设备编码">{{ selectedDevice?.deviceCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="设备名称">{{ selectedDevice?.deviceName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="故障时间">{{ form.faultTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="维修类型">{{ repairTypeLabel }}</el-descriptions-item>
          <el-descriptions-item label="优先级">{{ priorityLabel }}</el-descriptions-item>
          <el-descriptions-item label="故障描述">{{ form.faultDescription || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="footer-btns">
        <el-button v-if="step === 0" @click="$router.push('/repairs')">返回</el-button>
        <el-button v-if="step > 0" @click="step--">上一步</el-button>
        <el-button v-if="step === 0" type="primary" @click="toStep2">下一步</el-button>
        <el-button v-else-if="step === 1" type="primary" @click="toStep3">下一步</el-button>
        <el-button v-else type="primary" :loading="submitting" @click="handleSubmit">提交工单</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getDevices } from '@/api/device'
import { createRepairOrder } from '@/api/repair'

const router = useRouter()
const step = ref(0)
const deviceKeyword = ref('')
const deviceOptions = ref<any[]>([])
const deviceLoading = ref(false)
const selectedDevice = ref<any>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  faultTime: '',
  repairType: '',
  priority: 'MEDIUM',
  faultDescription: ''
})

const rules: FormRules = {
  faultTime: [{ required: true, message: '请选择故障时间', trigger: 'change' }],
  repairType: [{ required: true, message: '请选择维修类型', trigger: 'change' }],
  faultDescription: [{ required: true, message: '请填写故障描述', trigger: 'blur' }]
}

const repairTypeMap: Record<string, string> = { HARDWARE: '硬件维修', DEBUG: '调试维修', OPTICAL: '光路维修' }
const priorityMap: Record<string, string> = { URGENT: '紧急', HIGH: '高', MEDIUM: '中', LOW: '低' }
const deviceTypeMap: Record<string, string> = {
  ROUTER: '路由器', SWITCH: '交换机', FIREWALL: '防火墙', SERVER: '服务器',
  ONU: '光猫', AP: 'AP', OTHER: '其他'
}

const repairTypeLabel = computed(() => repairTypeMap[form.repairType] || form.repairType || '-')
const priorityLabel = computed(() => priorityMap[form.priority] || form.priority || '-')

function deviceTypeLabel(t: string) {
  return deviceTypeMap[t] || t
}

let searchTimer: number | undefined
function onKeywordInput() {
  if (searchTimer) window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(searchDevices, 500)
}

function deviceRowClass({ row }: { row: any }) {
  return selectedDevice.value?.id === row.id ? 'selected-row' : ''
}

// 空关键词加载全部设备；有关键词时同时按编码、名称模糊搜索（后端均为 LIKE），合并去重
async function loadDeviceOptions() {
  deviceLoading.value = true
  try {
    const kw = deviceKeyword.value.trim()
    let records: any[] = []
    if (!kw) {
      const res = await getDevices({ page: 1, size: 20 } as any)
      records = res.records || []
    } else {
      const [byCode, byName] = await Promise.all([
        getDevices({ page: 1, size: 20, deviceCode: kw } as any),
        getDevices({ page: 1, size: 20, deviceName: kw } as any)
      ])
      const map = new Map<number, any>()
      for (const d of [...(byCode.records || []), ...(byName.records || [])]) {
        map.set(d.id, d)
      }
      records = [...map.values()]
      if (!records.length) {
        ElMessage.warning('未找到匹配设备（已支持按设备编码或名称模糊搜索）')
      }
    }
    deviceOptions.value = records
  } catch (e) {
    ElMessage.error((e as any)?.message || '搜索设备失败')
  } finally {
    deviceLoading.value = false
  }
}

const searchDevices = loadDeviceOptions

onMounted(loadDeviceOptions)

function selectDevice(row: any) {
  selectedDevice.value = row
}

function toStep2() {
  if (!selectedDevice.value) {
    ElMessage.warning('请先搜索并选择设备')
    return
  }
  step.value = 1
}

async function toStep3() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  step.value = 2
}

async function handleSubmit() {
  if (!selectedDevice.value) {
    ElMessage.warning('请先选择设备')
    return
  }
  submitting.value = true
  try {
    await createRepairOrder({
      deviceId: selectedDevice.value.id,
      faultTime: form.faultTime || undefined,
      repairType: form.repairType,
      priority: form.priority,
      faultDescription: form.faultDescription
    } as any)
    ElMessage.success('工单创建成功')
    router.push('/repairs')
  } catch (e) {
    ElMessage.error((e as any)?.message || '创建失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.repair-create {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    .title { font-size: 16px; font-weight: 600; }
  }
  .steps { margin: 8px 0 24px; }
  .search-form { padding: 0; margin-bottom: 8px; }
  .selected-device { color: var(--el-color-primary); }
  .confirm-box { margin-bottom: 8px; }
  :deep(.selected-row) {
    cursor: pointer;
    td { background-color: var(--el-color-primary-light-9) !important; }
  }
  .el-table :deep(tbody tr) { cursor: pointer; }
  .footer-btns {
    margin-top: 24px;
    display: flex;
    justify-content: center;
  }
}
</style>
