<template>
  <el-tag :type="tagType" :effect="effect" :size="size">{{ label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  status: string
  effect?: 'dark' | 'light' | 'plain'
  size?: 'large' | 'default' | 'small'
}>()

const statusMap: Record<string, { type: string; label: string }> = {
  NORMAL: { type: 'success', label: '正常' },
  FAULT_REPAIR: { type: 'danger', label: '故障维修' },
  IDLE: { type: 'warning', label: '闲置' },
  SCRAPPED: { type: 'info', label: '已报废' }
}

const tagType = computed(() => (statusMap[props.status]?.type || 'info') as any)
const label = computed(() => statusMap[props.status]?.label || props.status)
</script>
