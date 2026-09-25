<template>
  <div class="file-upload">
    <el-upload
      ref="uploadRef"
      :action="action"
      :headers="headers"
      :before-upload="beforeUpload"
      :on-success="handleSuccess"
      :on-error="handleError"
      :file-list="fileList"
      :limit="limit"
      :multiple="multiple"
      :on-exceed="handleExceed"
      :on-remove="handleRemove"
    >
      <el-button type="primary" :icon="UploadFilled">上传文件</el-button>
      <template #tip>
        <div class="el-upload__tip">支持上传文件大小不超过 {{ maxSize }}MB</div>
      </template>
    </el-upload>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { getToken } from '@/utils/auth'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadInstance, UploadRawFile } from 'element-plus'

const props = withDefaults(
  defineProps<{
    action?: string
    fileList?: UploadFile[]
    limit?: number
    multiple?: boolean
    maxSize?: number
    accept?: string
  }>(),
  {
    action: '/api/upload',
    fileList: () => [],
    limit: 5,
    multiple: true,
    maxSize: 10
  }
)

const emit = defineEmits(['update:fileList', 'success', 'remove'])

const uploadRef = ref<UploadInstance>()

const headers = computed(() => ({
  Authorization: `Bearer ${getToken()}`
}))

function beforeUpload(file: UploadRawFile) {
  const isLt = file.size / 1024 / 1024 < props.maxSize
  if (!isLt) {
    ElMessage.error(`文件大小不能超过 ${props.maxSize}MB`)
    return false
  }
  return true
}

function handleSuccess(response: any, file: UploadFile) {
  // el-upload 的 onSuccess 只看 HTTP 状态；后端业务异常返回 HTTP 200 + code != 200，
  // 必须再检查业务码，否则「上传失败」会被误报成「上传成功」。
  if (response && typeof response === 'object' && 'code' in response && response.code !== 200) {
    ElMessage.error(response.message || '上传失败')
    return
  }
  ElMessage.success('上传成功')
  emit('success', response, file)
}

function handleError() {
  ElMessage.error('上传失败')
}

function handleExceed() {
  ElMessage.warning(`最多只能上传 ${props.limit} 个文件`)
}

function handleRemove(file: UploadFile) {
  emit('remove', file)
}
</script>

<style lang="scss" scoped>
.file-upload {
  width: 100%;
}
</style>
