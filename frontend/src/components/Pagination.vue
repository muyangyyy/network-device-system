<template>
  <div class="pagination-container">
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :page-sizes="[10, 20, 50, 100]"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  total: number
  page?: number
  limit?: number
}>()

const emit = defineEmits(['update:page', 'update:limit', 'pagination'])

const currentPage = computed({
  get: () => props.page || 1,
  set: (val) => emit('update:page', val)
})

const pageSize = computed({
  get: () => props.limit || 10,
  set: (val) => emit('update:limit', val)
})

function handleSizeChange(val: number) {
  emit('pagination', { page: currentPage.value, limit: val })
}

function handleCurrentChange(val: number) {
  emit('pagination', { page: val, limit: pageSize.value })
}
</script>

<style lang="scss" scoped>
.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding: 8px 0;
}
</style>
