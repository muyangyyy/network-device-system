<template>
  <div class="dashboard-container">
    <div class="filter-bar">
      <el-date-picker
        v-model="timeRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        @change="fetchData"
      />
      <el-button type="primary" @click="fetchData">刷新</el-button>
      <span class="filter-hint">时间范围按记录「创建时间」筛选（左闭右开，结束日期当天计入）</span>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="6" :md="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
          <div class="stat-info">
            <div class="stat-title">设备总数</div>
            <div class="stat-value">{{ overview.totalDevices }}</div>
          </div>
          <el-icon class="stat-icon"><Monitor /></el-icon>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #67c23a 0%, #5daf34 100%)">
          <div class="stat-info">
            <div class="stat-title">正常运行</div>
            <div class="stat-value">{{ overview.normalDevices }}</div>
          </div>
          <el-icon class="stat-icon"><CircleCheck /></el-icon>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #f56c6c 0%, #e04b4b 100%)">
          <div class="stat-info">
            <div class="stat-title">故障设备</div>
            <div class="stat-value">{{ overview.faultDevices }}</div>
          </div>
          <el-icon class="stat-icon"><Warning /></el-icon>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #e6a23c 0%, #cf8d31 100%)">
          <div class="stat-info">
            <div class="stat-title">闲置设备</div>
            <div class="stat-value">{{ overview.idleDevices }}</div>
          </div>
          <el-icon class="stat-icon"><Remove /></el-icon>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #909399 0%, #73767a 100%)">
          <div class="stat-info">
            <div class="stat-title">已报废</div>
            <div class="stat-value">{{ overview.scrappedDevices }}</div>
          </div>
          <el-icon class="stat-icon"><Delete /></el-icon>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6" :md="4">
        <div class="stat-card" style="background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%)">
          <div class="stat-info">
            <div class="stat-title">待处理工单</div>
            <div class="stat-value">{{ overview.pendingOrders }}</div>
          </div>
          <el-icon class="stat-icon"><Tickets /></el-icon>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="8" :md="6">
        <div class="info-card">
          <div class="info-label">本月工单数</div>
          <div class="info-value">{{ overview.monthlyOrders }}</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6">
        <div class="info-card">
          <div class="info-label">平均维修时长</div>
          <div class="info-value">{{ overview.avgRepairTime }}h</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6">
        <div class="info-card">
          <div class="info-label">完成率</div>
          <div class="info-value">{{ overview.completionRate }}%</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6">
        <div class="info-card">
          <div class="info-label">验收通过率</div>
          <div class="info-value">{{ overview.acceptanceRate }}%</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>设备类型分布</span>
          </template>
          <div ref="deviceTypeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>维修类型分布</span>
          </template>
          <div ref="repairTypeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="24">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header-with-action">
              <span>维修趋势</span>
              <el-radio-group v-model="trendPeriod" size="small" @change="fetchRepairTrend">
                <el-radio-button value="daily">按天</el-radio-button>
                <el-radio-button value="weekly">按周</el-radio-button>
                <el-radio-button value="monthly">按月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="repairTrendChartRef" class="chart-container-lg"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :md="16">
        <el-card shadow="hover">
          <template #header>
            <span>最近维修记录</span>
          </template>
          <el-table :data="recentOrders" size="small" stripe>
            <el-table-column prop="workOrderNo" label="工单号" width="160" />
            <el-table-column prop="deviceCode" label="设备编码" width="140" />
            <el-table-column prop="deviceName" label="设备名称" min-width="120" show-overflow-tooltip />
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
            <el-table-column prop="reporterName" label="报修人" width="90" />
            <el-table-column prop="createTime" label="创建时间" width="160" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <template #header>
            <span>待验收工单</span>
          </template>
          <el-scrollbar height="300px">
            <div v-for="order in pendingAcceptOrders" :key="order.id" class="pending-item" @click="$router.push(`/repairs/accept/${order.id}`)">
              <div class="pending-title">{{ order.workOrderNo }}</div>
              <div class="pending-desc">{{ order.deviceCode }} - {{ order.deviceName }}</div>
              <div class="pending-time">{{ order.createTime }}</div>
            </div>
            <el-empty v-if="pendingAcceptOrders.length === 0" description="暂无待验收工单" :image-size="60" />
          </el-scrollbar>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { getOverview, getRepairTypeStats, getRepairTrend, getDeviceStatusStats } from '@/api/statistics'
import { getRepairOrders } from '@/api/repair'
import RepairTypeTag from '@/components/RepairTypeTag.vue'
import RepairStatusTag from '@/components/RepairStatusTag.vue'
import type { OverviewData, RepairTypeStats as RepairTypeStatsType, RepairTrend as RepairTrendType } from '@/api/statistics'
import { ElMessage } from 'element-plus'

const timeRange = ref<[string, string] | null>(null)
const trendPeriod = ref('daily')

const overview = reactive<OverviewData>({
  totalDevices: 0,
  normalDevices: 0,
  faultDevices: 0,
  idleDevices: 0,
  scrappedDevices: 0,
  pendingOrders: 0,
  monthlyOrders: 0,
  avgRepairTime: 0,
  completionRate: 0,
  acceptanceRate: 0
})

const recentOrders = ref<any[]>([])
const pendingAcceptOrders = ref<any[]>([])

const deviceTypeChartRef = ref<HTMLDivElement>()
const repairTypeChartRef = ref<HTMLDivElement>()
const repairTrendChartRef = ref<HTMLDivElement>()

let deviceTypeChart: echarts.ECharts | null = null
let repairTypeChart: echarts.ECharts | null = null
let repairTrendChart: echarts.ECharts | null = null

function getParams() {
  return {
    startTime: timeRange.value?.[0] || undefined,
    endTime: timeRange.value?.[1] || undefined
  }
}

async function fetchOverview() {
  try {
    const res = await getOverview(getParams())
    Object.assign(overview, res)
  } catch (e: any) { ElMessage.error(e?.message || '加载仪表盘数据失败') }
}

async function fetchDeviceTypeStats() {
  try {
    const res = await getDeviceStatusStats(getParams())
    if (deviceTypeChart) {
      deviceTypeChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 0, type: 'scroll' },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
          label: { show: false, position: 'center' },
          emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
          data: res.map((item: any) => ({ name: item.name, value: item.value }))
        }]
      })
    }
  } catch (e: any) { ElMessage.error(e?.message || '加载仪表盘数据失败') }
}

async function fetchRepairTypeStats() {
  try {
    const res = await getRepairTypeStats(getParams())
    if (repairTypeChart) {
      repairTypeChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 0, type: 'scroll' },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
          label: { show: false, position: 'center' },
          emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
          data: res.map((item: any) => ({ name: item.name, value: item.value }))
        }]
      })
    }
  } catch (e: any) { ElMessage.error(e?.message || '加载仪表盘数据失败') }
}

async function fetchRepairTrend() {
  try {
    const res = await getRepairTrend({ ...getParams(), period: trendPeriod.value })
    if (repairTrendChart) {
      repairTrendChart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: res.dates
        },
        yAxis: {
          type: 'value',
          minInterval: 1
        },
        series: [{
          name: '维修记录数',
          type: 'line',
          smooth: true,
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(64,158,255,0.3)' },
              { offset: 1, color: 'rgba(64,158,255,0.05)' }
            ])
          },
          itemStyle: { color: '#409eff' },
          data: res.values
        }]
      })
    }
  } catch (e: any) { ElMessage.error(e?.message || '加载仪表盘数据失败') }
}

async function fetchRecentOrders() {
  try {
    const res = await getRepairOrders({ page: 1, size: 8 })
    recentOrders.value = res.records
  } catch (e: any) { ElMessage.error(e?.message || '加载仪表盘数据失败') }
}

async function fetchPendingAcceptOrders() {
  try {
    const res = await getRepairOrders({ page: 1, size: 10, status: 'COMPLETED' })
    pendingAcceptOrders.value = res.records
  } catch (e: any) { ElMessage.error(e?.message || '加载仪表盘数据失败') }
}

function initCharts() {
  deviceTypeChart = echarts.init(deviceTypeChartRef.value!)
  repairTypeChart = echarts.init(repairTypeChartRef.value!)
  repairTrendChart = echarts.init(repairTrendChartRef.value!)
}

async function fetchData() {
  await Promise.all([
    fetchOverview(),
    fetchDeviceTypeStats(),
    fetchRepairTypeStats(),
    fetchRepairTrend(),
    fetchRecentOrders(),
    fetchPendingAcceptOrders()
  ])
}

function handleResize() {
  deviceTypeChart?.resize()
  repairTypeChart?.resize()
  repairTrendChart?.resize()
}

onMounted(async () => {
  await nextTick()
  initCharts()
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  deviceTypeChart?.dispose()
  repairTypeChart?.dispose()
  repairTrendChart?.dispose()
})
</script>

<style lang="scss" scoped>
.dashboard-container {
  padding: 0;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.filter-hint {
  font-size: 12px;
  color: #909399;
}

.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  height: 110px;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
  margin-bottom: 8px;

  .stat-info {
    .stat-title {
      font-size: 13px;
      opacity: 0.85;
      margin-bottom: 8px;
    }
    .stat-value {
      font-size: 28px;
      font-weight: 600;
    }
  }
  .stat-icon {
    font-size: 42px;
    opacity: 0.6;
  }
}

.info-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

  .info-label {
    font-size: 13px;
    color: #909399;
    margin-bottom: 8px;
  }
  .info-value {
    font-size: 22px;
    font-weight: 600;
    color: #303133;
  }
}

.chart-card {
  margin-bottom: 16px;
}

.chart-container {
  height: 300px;
  width: 100%;
}

.chart-container-lg {
  height: 350px;
  width: 100%;
}

.card-header-with-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pending-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;

  &:hover { background-color: #f5f7fa; }
  &:last-child { border-bottom: none; }

  .pending-title {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
    margin-bottom: 4px;
  }
  .pending-desc {
    font-size: 12px;
    color: #909399;
    margin-bottom: 4px;
  }
  .pending-time {
    font-size: 12px;
    color: #c0c4cc;
  }
}
</style>
