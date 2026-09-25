<template>
  <div class="log-management">
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="操作日志" name="operation">
          <el-form :inline="true" :model="queryParams" class="search-form">
            <el-form-item label="操作人">
              <el-input v-model="queryParams.operatorName" placeholder="请输入操作人" clearable />
            </el-form-item>
            <el-form-item label="操作类型">
              <el-select v-model="queryParams.operationType" placeholder="全部" clearable>
                <el-option label="登录" value="LOGIN" />
                <el-option label="登出" value="LOGOUT" />
                <el-option label="创建" value="CREATE" />
                <el-option label="修改" value="UPDATE" />
                <el-option label="删除" value="DELETE" />
              </el-select>
            </el-form-item>
            <el-form-item label="时间范围">
              <el-date-picker
                v-model="timeRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="fetchLogs">搜索</el-button>
              <el-button @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
          <el-table v-loading="loading" :data="logs" border stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="operatorName" label="操作人" width="100" />
            <el-table-column prop="operationType" label="操作类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="getOpTypeTag(row.operationType)">{{ getOpTypeLabel(row.operationType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="module" label="模块" width="120" />
            <el-table-column prop="description" label="操作描述" min-width="200" show-overflow-tooltip />
            <el-table-column prop="ip" label="IP地址" width="140" />
            <el-table-column prop="createTime" label="操作时间" width="170" />
          </el-table>
          <Pagination
            v-model:page="queryParams.page"
            v-model:limit="queryParams.size"
            :total="total"
            @pagination="fetchLogs"
          />
        </el-tab-pane>
        <el-tab-pane label="登录日志" name="login">
          <el-table v-loading="loginLoading" :data="loginLogs" border stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column prop="ip" label="登录IP" width="140" />
            <el-table-column prop="browser" label="浏览器" width="140" show-overflow-tooltip />
            <el-table-column prop="os" label="操作系统" width="140" show-overflow-tooltip />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.success ? 'success' : 'danger'" size="small">{{ row.success ? '成功' : '失败' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="消息" min-width="150" show-overflow-tooltip />
            <el-table-column prop="createTime" label="登录时间" width="170" />
          </el-table>
          <Pagination
            v-model:page="loginPage"
            v-model:limit="loginSize"
            :total="loginTotal"
            @pagination="fetchLoginLogs"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import Pagination from '@/components/Pagination.vue'
import { ElMessage } from 'element-plus'

const activeTab = ref('operation')
const loading = ref(false)
const logs = ref<any[]>([])
const total = ref(0)
const timeRange = ref<[string, string] | null>(null)
const queryParams = reactive({ page: 1, size: 10, operatorName: '', operationType: '' })

const loginLoading = ref(false)
const loginLogs = ref<any[]>([])
const loginTotal = ref(0)
const loginPage = ref(1)
const loginSize = ref(10)

function getOpTypeTag(t: string) {
  const map: Record<string, string> = { LOGIN: 'success', LOGOUT: 'info', CREATE: '', UPDATE: 'warning', DELETE: 'danger' }
  return (map[t] || 'info') as any
}

function getOpTypeLabel(t: string) {
  const map: Record<string, string> = { LOGIN: '登录', LOGOUT: '登出', CREATE: '创建', UPDATE: '修改', DELETE: '删除' }
  return map[t] || t
}

async function fetchLogs() {
  loading.value = true
  try {
    const params: any = { ...queryParams }
    if (timeRange.value) { params.startTime = timeRange.value[0]; params.endTime = timeRange.value[1] }
    const res: any = await request.get('/system-logs', { params })
    logs.value = res.records || []
    total.value = res.total || 0
  } catch (e: any) { ElMessage.error(e?.message || '加载日志失败') }
  finally { loading.value = false }
}

function resetQuery() {
  Object.assign(queryParams, { page: 1, operatorName: '', operationType: '' })
  timeRange.value = null
  fetchLogs()
}

async function fetchLoginLogs() {
  loginLoading.value = true
  try {
    const res: any = await request.get('/system-logs/login', { params: { page: loginPage.value, size: loginSize.value } })
    loginLogs.value = res.records || []
    loginTotal.value = res.total || 0
  } catch (e: any) { ElMessage.error(e?.message || '加载日志失败') }
  finally { loginLoading.value = false }
}

onMounted(() => { fetchLogs(); fetchLoginLogs() })
</script>

<style lang="scss" scoped>
.search-form { padding: 0; }
</style>
