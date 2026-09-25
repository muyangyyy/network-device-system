<template>
  <el-tag :type="tagType" :effect="effect" :size="size">{{ label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  type: string
  effect?: 'dark' | 'light' | 'plain'
  size?: 'large' | 'default' | 'small'
}>()

const typeMap: Record<string, { type: string; label: string }> = {
  HARDWARE: { type: 'danger', label: '硬件维修' },
  DEBUG: { type: '', label: '调试' },
  OPTICAL: { type: 'warning', label: '光路维修' },
  OTHER: { type: 'info', label: '其他' }
}

const tagType = computed(() => (typeMap[props.type]?.type || 'info') as any)
const label = computed(() => typeMap[props.type]?.label || props.type)
</script>
