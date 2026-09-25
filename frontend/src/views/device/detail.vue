<template>
  <div class="device-detail" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>设备详情</span>
          <div>
            <el-button @click="$router.back()">返回</el-button>
            <el-button type="primary" @click="$router.push(`/devices/edit/${deviceId}`)" v-if="userStore.hasPermission('device:edit')">编辑</el-button>
            <el-button @click="handlePrint">打印</el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="设备编码">{{ device.deviceCode }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ device.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="设备类型">{{ getDeviceTypeLabel(device.deviceType) }}</el-descriptions-item>
        <el-descriptions-item label="品牌">{{ device.brand || '-' }}</el-descriptions-item>
        <el-descriptions-item label="型号">{{ device.model || '-' }}</el-descriptions-item>
        <el-descriptions-item label="序列号">{{ device.serialNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ device.ipAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="MAC地址">{{ device.macAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <DeviceStatusTag :status="device.status" />
        </el-descriptions-item>
        <el-descriptions-item label="所属分组">{{ device.groupName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ device.responsibleUserName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="安装位置">{{ device.installationLocation || '-' }}</el-descriptions-item>
        <el-descriptions-item label="采购日期">{{ device.purchaseDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="质保到期">{{ device.warrantyExpireDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="3">{{ device.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ device.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="更新时间" :span="2">{{ device.updatedAt }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="维修统计" name="stats">
          <el-row :gutter="16">
            <el-col :span="8">
              <div class="mini-stat">
                <div class="mini-stat-label">总维修次数</div>
                <div class="mini-stat-value">{{ repairStats.total }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="mini-stat">
                <div class="mini-stat-label">硬件维修</div>
                <div class="mini-stat-value">{{ repairStats.hardware }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="mini-stat">
                <div class="mini-stat-label">调试</div>
                <div class="mini-stat-value">{{ repairStats.debug }}</div>
              </div>
            </el-col>
          </el-row>
        </el-tab-pane>
        <el-tab-pane label="状态变更记录" name="statusLogs">
          <el-table :data="statusLogs" border stripe v-loading="statusLogsLoading">
            <el-table-column prop="fromStatus" label="原状态" width="100">
              <template #default="{ row }">
                <DeviceStatusTag :status="row.fromStatus" v-if="row.fromStatus" />
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="→" width="40" align="center">→</el-table-column>
            <el-table-column prop="toStatus" label="新状态" width="100">
              <template #default="{ row }">
                <DeviceStatusTag :status="row.toStatus" />
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="变更原因" min-width="200" show-overflow-tooltip />
            <el-table-column prop="operatorName" label="操作人" width="100" />
            <el-table-column prop="createTime" label="操作时间" width="170" />
          </el-table>
          <Pagination
            v-model:page="statusLogsPage"
            v-model:limit="statusLogsSize"
            :total="statusLogsTotal"
            @pagination="fetchStatusLogs"
          />
        </el-tab-pane>
        <el-tab-pane label="维修记录" name="repairs">
          <el-table :data="repairs" border stripe v-loading="repairsLoading">
            <el-table-column prop="workOrderNo" label="工单号" width="160" />
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
            <el-table-column prop="faultDescription" label="故障描述" min-width="200" show-overflow-tooltip />
            <el-table-column prop="reporterName" label="报修人" width="90" />
            <el-table-column prop="repairUserName" label="维修人" width="90" />
            <el-table-column prop="faultTime" label="故障时间" width="170" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button type="primary" link @click="$router.push(`/repairs/detail/${row.id}`)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
          <Pagination
            v-model:page="repairsPage"
            v-model:limit="repairsSize"
            :total="repairsTotal"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getDeviceById, getDeviceRepairs, getDeviceStatusLogs, type Device, type DeviceRepair, type DeviceStatusLog } from '@/api/device'
import { useUserStore } from '@/store/user'
import DeviceStatusTag from '@/components/DeviceStatusTag.vue'
import RepairTypeTag from '@/components/RepairTypeTag.vue'
import RepairStatusTag from '@/components/RepairStatusTag.vue'
import Pagination from '@/components/Pagination.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const userStore = useUserStore()
const deviceId = Number(route.params.id)

const loading = ref(false)
const activeTab = ref('stats')
const device = ref<Device>({} as Device)
const deviceTypes = ref<{ label: string; value: string }[]>([])

const repairStats = reactive({ total: 0, hardware: 0, debug: 0, optical: 0 })

const statusLogs = ref<DeviceStatusLog[]>([])
const statusLogsLoading = ref(false)
const statusLogsPage = ref(1)
const statusLogsSize = ref(10)
const statusLogsTotal = ref(0)

const repairsAll = ref<DeviceRepair[]>([])
const repairsLoading = ref(false)
const repairsPage = ref(1)
const repairsSize = ref(10)
const repairsTotal = ref(0)

// 后端 GET /devices/{id}/repairs 忽略前端传入的 page/size，固定按 page=1&pageSize=1000 返回，
// 因此分页只能在前端做——否则分页器可点，但表格内容永远不变
const repairs = computed(() => {
  const start = (repairsPage.value - 1) * repairsSize.value
  return repairsAll.value.slice(start, start + repairsSize.value)
})

function getDeviceTypeLabel(type: string) {
  // 优先用字典（与列表页/表单页同源），字典取不到再退回内置表。
  // 内置表只覆盖常见值：曾经这里写死且写的是 OPTICAL（库里并不存在），
  // 导致 ONT / AP 两类设备在详情页直接显示裸英文。
  const fromDict = deviceTypes.value.find((d) => d.value === type)
  if (fromDict) return fromDict.label
  const fallback: Record<string, string> = {
    ROUTER: '路由器', SWITCH: '交换机', FIREWALL: '防火墙',
    SERVER: '服务器', ONT: '光猫', AP: 'AP', OTHER: '其他'
  }
  return fallback[type] || type
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

async function fetchDevice() {
  loading.value = true
  try {
    device.value = await getDeviceById(deviceId)
  } catch (e: any) { ElMessage.error(e?.message || '加载设备详情失败') }
  finally { loading.value = false }
}

async function fetchStatusLogs() {
  statusLogsLoading.value = true
  try {
    const res = await getDeviceStatusLogs(deviceId, { page: statusLogsPage.value, size: statusLogsSize.value })
    statusLogs.value = res.records
    statusLogsTotal.value = res.total
  } catch (e: any) { ElMessage.error(e?.message || '加载设备详情失败') }
  finally { statusLogsLoading.value = false }
}

async function fetchRepairs() {
  repairsLoading.value = true
  try {
    const res = await getDeviceRepairs(deviceId, { page: 1, size: 1000 })
    repairsAll.value = res.records
    repairsTotal.value = res.total
    // 维修统计：后端没有按类型统计的接口，直接由返回的完整列表计算
    repairStats.total = res.total
    repairStats.hardware = res.records.filter((r) => r.repairType === 'HARDWARE').length
    repairStats.debug = res.records.filter((r) => r.repairType === 'DEBUG').length
    repairStats.optical = res.records.filter((r) => r.repairType === 'OPTICAL').length
  } catch (e: any) { ElMessage.error(e?.message || '加载设备详情失败') }
  finally { repairsLoading.value = false }
}

function handlePrint() {
  window.print()
}

onMounted(() => {
  fetchDictData()
  fetchDevice()
  fetchStatusLogs()
  fetchRepairs()
})
</script>

<style lang="scss" scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.mini-stat {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  .mini-stat-label {
    font-size: 13px;
    color: #909399;
    margin-bottom: 8px;
  }
  .mini-stat-value {
    font-size: 24px;
    font-weight: 600;
    color: #303133;
  }
}
@media print {
  .el-card { box-shadow: none !important; }
}
</style>
