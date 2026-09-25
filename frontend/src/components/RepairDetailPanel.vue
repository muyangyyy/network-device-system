<template>
  <el-descriptions v-if="detail" :column="2" border size="small">
    <el-descriptions-item v-for="row in rows" :key="row.key" :label="row.label">
      {{ row.value }}
    </el-descriptions-item>
  </el-descriptions>
  <el-empty v-else description="暂无明细" :image-size="60" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { HardwareDetail, DebugDetail, OpticalDetail } from '@/api/repair'

/**
 * 工单维修明细展示面板。
 *
 * 后端 RepairWorkOrderVO 的 hardwareDetail / debugDetail / opticalDetail 是**嵌套对象**
 * （RepairXxxDetailVO）。早期版本把它们当字符串插值，页面上会渲染成一整段 JSON。
 * 这里按维修类型挑字段，渲染成可读的键值表。
 *
 * 字段名与后端 VO / CompleteDTO 内嵌 DTO 完全一致。
 */
const props = defineProps<{
  type: string
  detail?: HardwareDetail | DebugDetail | OpticalDetail | null
}>()

interface Field {
  key: string
  label: string
}

const FIELDS: Record<string, Field[]> = {
  HARDWARE: [
    { key: 'damagedComponent', label: '损坏部件' },
    { key: 'replacementPartName', label: '更换配件名称' },
    { key: 'replacementPartModel', label: '更换配件型号' },
    { key: 'replacementPartQuantity', label: '更换数量' },
    { key: 'replacementPartCost', label: '配件费用' },
    { key: 'oldPartDisposalMethod', label: '旧件处理方式' },
    { key: 'hardwareFailureCode', label: '硬件故障代码' },
    { key: 'whetherUnderWarranty', label: '是否在保' },
    { key: 'supplier', label: '供应商' },
    { key: 'remark', label: '备注' }
  ],
  DEBUG: [
    { key: 'configurationChangeContent', label: '配置变更内容' },
    { key: 'oldFirmwareVersion', label: '原固件版本' },
    { key: 'newFirmwareVersion', label: '新固件版本' },
    { key: 'oldIpAddress', label: '原IP地址' },
    { key: 'newIpAddress', label: '新IP地址' },
    { key: 'oldGateway', label: '原网关' },
    { key: 'newGateway', label: '新网关' },
    { key: 'oldVlan', label: '原VLAN' },
    { key: 'newVlan', label: '新VLAN' },
    { key: 'routeChangeContent', label: '路由变更' },
    { key: 'firewallPolicyChange', label: '防火墙策略变更' },
    { key: 'permissionChangeContent', label: '权限变更' },
    { key: 'networkParameterChangeRecord', label: '网络参数变更记录' },
    { key: 'rollbackPlan', label: '回滚方案' },
    { key: 'testResult', label: '测试结果' },
    { key: 'remark', label: '备注' }
  ],
  OPTICAL: [
    { key: 'faultOpticalPoint', label: '故障光路点' },
    { key: 'opticalRouteName', label: '光路名称' },
    { key: 'cableSection', label: '光缆段' },
    { key: 'cableLength', label: '光缆长度(km)' },
    { key: 'opticalPowerBefore', label: '维修前光功率' },
    { key: 'opticalPowerAfter', label: '维修后光功率' },
    { key: 'attenuationBefore', label: '维修前衰减值' },
    { key: 'attenuationAfter', label: '维修后衰减值' },
    { key: 'wavelength', label: '波长' },
    { key: 'splitterStatus', label: '分光器状态' },
    { key: 'jumperStatus', label: '跳纤状态' },
    { key: 'cableDamageDescription', label: '光缆损坏描述' },
    { key: 'linkTestResult', label: '链路测试结果' },
    { key: 'testTool', label: '测试工具' },
    { key: 'testPerson', label: '测试人' },
    { key: 'remark', label: '备注' }
  ]
}

// 在脚本里统一按 Record 取值，避免在模板中对联合类型做下标访问（vue-tsc 会报错）
const rows = computed(() => {
  const src = (props.detail || {}) as Record<string, any>
  const list = FIELDS[props.type] || []
  return list.map((f) => ({ key: f.key, label: f.label, value: format(f.key, src[f.key]) }))
})

function format(key: string, val: any): string {
  if (val === null || val === undefined || val === '') return '-'
  // 后端 whether_under_warranty 是 TINYINT(0/1)
  if (key === 'whetherUnderWarranty') {
    return val === 1 || val === true || val === '1' ? '在保' : '不在保'
  }
  return String(val)
}
</script>
