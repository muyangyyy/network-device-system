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
  DRAFT: { type: 'info', label: '草稿' },
  SUBMITTED: { type: 'warning', label: '已提交' },
  ASSIGNED: { type: '', label: '已分配' },
  PROCESSING: { type: '', label: '处理中' },
  COMPLETED: { type: 'success', label: '已完成' },
  ACCEPTED: { type: 'success', label: '已验收' },
  REJECTED: { type: 'danger', label: '已退回' },
  DELAYED: { type: 'warning', label: '已延期' },
  REPAIR_AGAIN: { type: 'danger', label: '返修' },
  CLOSED: { type: 'info', label: '已关闭' },
  CANCELLED: { type: 'info', label: '已取消' }
}

const tagType = computed(() => (statusMap[props.status]?.type || 'info') as any)
const label = computed(() => statusMap[props.status]?.label || props.status)
</script>
