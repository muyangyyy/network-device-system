<template>
  <div class="statistics-page">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true">
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
        <el-form-item label="设备分组">
          <el-tree-select
            v-model="filters.groupId"
            :data="groupTree"
            :props="{ label: 'name', value: 'id', children: 'children' } as any"
            placeholder="全部"
            clearable
            check-strictly
          />
        </el-form-item>
        <el-form-item label="设备类型">
          <el-select v-model="filters.deviceType" placeholder="全部" clearable>
            <el-option
              v-for="item in deviceTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="维修类型">
          <el-select v-model="filters.repairType" placeholder="全部" clearable>
            <el-option
              v-for="item in repairTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="维修人员">
          <el-select v-model="filters.repairUserId" placeholder="全部" clearable filterable>
            <el-option v-for="u in userList" :key="u.id" :label="u.nickname" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <el-button @click="handleExport">导出报表</el-button>
          <el-button @click="handlePrint">打印</el-button>
        </el-form-item>
      </el-form>
      <div class="filter-hint">
        时间范围按记录「创建时间」筛选（左闭右开，结束日期当天计入）；设备维度图表同样按此口径。
        「维修类型」「维修人员」只作用于工单维度的图表。
      </div>
    </el-card>

    <el-row :gutter="16" style="margin-top: 12px">
      <el-col :xs="12" :sm="8" :md="3" v-for="(item, index) in efficiencyCards" :key="index">
        <div class="efficiency-card" :style="{ borderLeftColor: item.color }">
          <div class="eff-label">{{ item.label }}</div>
          <div class="eff-value" :style="{ color: item.color }">{{ item.value }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 12px">
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <template #header><span>维修类型分布</span></template>
          <div ref="repairTypeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <template #header><span>设备状态分布</span></template>
          <div ref="deviceStatusChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 12px">
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

    <el-row :gutter="16" style="margin-top: 12px">
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <template #header><span>分组维修排名</span></template>
          <div ref="groupRankingChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <template #header><span>设备维修排名</span></template>
          <div ref="deviceRankingChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { getEfficiencyStats, getRepairTypeStats, getRepairTrend, getGroupRanking, getDeviceRanking, getDeviceStatusStats } from '@/api/statistics'
import { getGroupTree, type Group } from '@/api/group'
import { getUsers, type User } from '@/api/user'
import { getDictItems } from '@/api/dict'
import { exportSheets } from '@/utils/export'
import { ElMessage } from 'element-plus'

const timeRange = ref<[string, string] | null>(null)
const trendPeriod = ref('daily')

const filters = reactive({
  groupId: undefined as number | undefined,
  deviceType: '',
  repairType: '',
  repairUserId: undefined as number | undefined
})

const groupTree = ref<Group[]>([])
const userList = ref<User[]>([])

/**
 * 设备类型 / 维修类型选项统一取自数据字典。
 *
 * 此前这里是硬编码的：设备类型写成 ROUTER/SWITCH/FIREWALL/SERVER/OPTICAL/OTHER，
 * 而字典里的实际取值是 ROUTER/SWITCH/FIREWALL/SERVER/ONT/AP/OTHER——
 * OPTICAL 不是合法设备类型（筛出来永远是空），ONT（光猫）和 AP 则完全选不到。
 */
const deviceTypeOptions = ref<{ label: string; value: string }[]>([])
const repairTypeOptions = ref<{ label: string; value: string }[]>([])

/** 最近一次查询结果，供「导出报表」复用，避免导出时再打一遍接口 */
const repairTypeData = ref<{ name: string; value: number }[]>([])
const deviceStatusData = ref<{ name: string; value: number }[]>([])
const trendData = ref<{ dates: string[]; values: number[] }>({ dates: [], values: [] })
const groupRankingData = ref<{ name: string; value: number }[]>([])
const deviceRankingData = ref<{ deviceCode: string; deviceName: string; repairCount: number }[]>([])

async function fetchDictOptions() {
  try {
    const [deviceTypes, repairTypes] = await Promise.all([
      getDictItems('device_type'),
      getDictItems('repair_type')
    ])
    deviceTypeOptions.value = deviceTypes.map((item) => ({
      label: item.itemLabel,
      value: item.itemValue
    }))
    repairTypeOptions.value = repairTypes.map((item) => ({
      label: item.itemLabel,
      value: item.itemValue
    }))
  } catch (e) {
    /* 字典取不到时下拉为空，不阻塞页面其余部分 */
  }
}

const efficiencyData = reactive({
  avgResponseTime: 0,
  avgRepairTime: 0,
  completionRate: 0,
  acceptanceRate: 0,
  overdueCount: 0,
  processingCount: 0,
  firstFixRate: 0,
  retryRate: 0
})

const efficiencyCards = computed(() => [
  { label: '平均响应时间', value: `${efficiencyData.avgResponseTime}h`, color: '#409eff' },
  { label: '平均维修时长', value: `${efficiencyData.avgRepairTime}h`, color: '#67c23a' },
  { label: '完成率', value: `${efficiencyData.completionRate}%`, color: '#e6a23c' },
  { label: '验收通过率', value: `${efficiencyData.acceptanceRate}%`, color: '#f56c6c' },
  { label: '超时工单', value: `${efficiencyData.overdueCount}`, color: '#f56c6c' },
  { label: '处理中工单', value: `${efficiencyData.processingCount}`, color: '#409eff' },
  { label: '一次修复率', value: `${efficiencyData.firstFixRate}%`, color: '#67c23a' },
  { label: '返修率', value: `${efficiencyData.retryRate}%`, color: '#e6a23c' }
])

const repairTypeChartRef = ref<HTMLDivElement>()
const deviceStatusChartRef = ref<HTMLDivElement>()
const repairTrendChartRef = ref<HTMLDivElement>()
const groupRankingChartRef = ref<HTMLDivElement>()
const deviceRankingChartRef = ref<HTMLDivElement>()

let repairTypeChart: echarts.ECharts | null = null
let deviceStatusChart: echarts.ECharts | null = null
let repairTrendChart: echarts.ECharts | null = null
let groupRankingChart: echarts.ECharts | null = null
let deviceRankingChart: echarts.ECharts | null = null

function getParams() {
  return {
    startTime: timeRange.value?.[0] || undefined,
    endTime: timeRange.value?.[1] || undefined,
    groupId: filters.groupId || undefined,
    deviceType: filters.deviceType || undefined,
    repairType: filters.repairType || undefined,
    repairUserId: filters.repairUserId || undefined
  }
}

async function fetchEfficiency() {
  try {
    const res = await getEfficiencyStats(getParams())
    Object.assign(efficiencyData, res)
  } catch (e: any) { ElMessage.error(e?.message || '加载统计数据失败') }
}

async function fetchRepairTypeStats() {
  try {
    const res = await getRepairTypeStats(getParams())
    repairTypeData.value = res
    if (repairTypeChart) {
      repairTypeChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 0, type: 'scroll' },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
          label: { show: false, position: 'center' },
          emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
          data: res.map((item: any) => ({ name: item.name, value: item.value }))
        }]
      })
    }
  } catch (e: any) { ElMessage.error(e?.message || '加载统计数据失败') }
}

async function fetchDeviceStatusStats() {
  try {
    const res = await getDeviceStatusStats(getParams())
    deviceStatusData.value = res
    if (deviceStatusChart) {
      deviceStatusChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 0, type: 'scroll' },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
          label: { show: false, position: 'center' },
          emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
          data: res.map((item: any) => ({ name: item.name, value: item.value }))
        }]
      })
    }
  } catch (e: any) { ElMessage.error(e?.message || '加载统计数据失败') }
}

async function fetchRepairTrend() {
  try {
    const res = await getRepairTrend({ ...getParams(), period: trendPeriod.value })
    trendData.value = res
    if (repairTrendChart) {
      repairTrendChart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', boundaryGap: false, data: res.dates },
        yAxis: { type: 'value', minInterval: 1 },
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
  } catch (e: any) { ElMessage.error(e?.message || '加载统计数据失败') }
}

async function fetchGroupRanking() {
  try {
    const res = await getGroupRanking(getParams())
    groupRankingData.value = res
    if (groupRankingChart) {
      groupRankingChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'value', minInterval: 1 },
        yAxis: { type: 'category', data: res.map((item: any) => item.name).reverse() },
        series: [{
          type: 'bar',
          data: res.map((item: any) => item.value).reverse(),
          itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#409eff' }, { offset: 1, color: '#67c23a' }]) },
          barWidth: 20
        }]
      })
    }
  } catch (e: any) { ElMessage.error(e?.message || '加载统计数据失败') }
}

async function fetchDeviceRanking() {
  try {
    const res = await getDeviceRanking(getParams())
    deviceRankingData.value = res
    if (deviceRankingChart) {
      deviceRankingChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'value', minInterval: 1 },
        yAxis: { type: 'category', data: res.map((item: any) => item.deviceName || item.deviceCode).reverse() },
        series: [{
          type: 'bar',
          data: res.map((item: any) => item.repairCount).reverse(),
          itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#e6a23c' }, { offset: 1, color: '#f56c6c' }]) },
          barWidth: 20
        }]
      })
    }
  } catch (e: any) { ElMessage.error(e?.message || '加载统计数据失败') }
}

function initCharts() {
  repairTypeChart = echarts.init(repairTypeChartRef.value!)
  deviceStatusChart = echarts.init(deviceStatusChartRef.value!)
  repairTrendChart = echarts.init(repairTrendChartRef.value!)
  groupRankingChart = echarts.init(groupRankingChartRef.value!)
  deviceRankingChart = echarts.init(deviceRankingChartRef.value!)
}

async function fetchData() {
  await Promise.all([
    fetchEfficiency(),
    fetchRepairTypeStats(),
    fetchDeviceStatusStats(),
    fetchRepairTrend(),
    fetchGroupRanking(),
    fetchDeviceRanking()
  ])
}

function resetFilters() {
  timeRange.value = null
  Object.assign(filters, { groupId: undefined, deviceType: '', repairType: '', repairUserId: undefined })
  fetchData()
}

const periodLabel = computed(() => {
  const map: Record<string, string> = { daily: '按天', weekly: '按周', monthly: '按月' }
  return map[trendPeriod.value] || '按天'
})

function rangeSuffix() {
  const range = timeRange.value
  if (range && range.length === 2 && range[0] && range[1]) {
    return `${range[0].replace(/-/g, '')}-${range[1].replace(/-/g, '')}`
  }
  return dayjs().format('YYYYMMDD')
}

/**
 * 导出当前筛选条件下的统计报表（6 个工作表）。
 *
 * 先 await fetchData() 再导出：否则用户改了筛选条件但没点「查询」时，
 * 导出的会是上一次的结果。
 */
async function handleExport() {
  await fetchData()

  const sheets = [
    {
      name: '效率指标',
      header: ['指标', '数值'],
      rows: [
        ['平均响应时间(小时)', efficiencyData.avgResponseTime],
        ['平均维修时长(小时)', efficiencyData.avgRepairTime],
        ['完成率(%)', efficiencyData.completionRate],
        ['验收通过率(%)', efficiencyData.acceptanceRate],
        ['一次修复率(%)', efficiencyData.firstFixRate],
        ['返修率(%)', efficiencyData.retryRate],
        ['超时工单数', efficiencyData.overdueCount],
        ['处理中工单数', efficiencyData.processingCount]
      ] as (string | number)[][]
    },
    {
      name: '维修类型分布',
      header: ['维修类型', '工单数'],
      rows: repairTypeData.value.map((item) => [item.name, item.value])
    },
    {
      name: '设备状态分布',
      header: ['设备状态', '设备数'],
      rows: deviceStatusData.value.map((item) => [item.name, item.value])
    },
    {
      name: `维修趋势(${periodLabel.value})`,
      header: ['周期', '工单数'],
      rows: trendData.value.dates.map((date, i) => [date, trendData.value.values[i] ?? 0])
    },
    {
      name: '分组维修排名',
      header: ['分组', '工单数'],
      rows: groupRankingData.value.map((item) => [item.name, item.value])
    },
    {
      name: '设备维修排名',
      header: ['设备编码', '设备名称', '维修次数'],
      rows: deviceRankingData.value.map((item) => [
        item.deviceCode || '',
        item.deviceName || '',
        item.repairCount
      ])
    }
  ]

  exportSheets(sheets, `统计报表_${rangeSuffix()}`)
  ElMessage.success('报表已导出，共 6 个工作表')
}

function handlePrint() {
  window.print()
}

function handleResize() {
  repairTypeChart?.resize()
  deviceStatusChart?.resize()
  repairTrendChart?.resize()
  groupRankingChart?.resize()
  deviceRankingChart?.resize()
}

onMounted(async () => {
  try {
    const [groupRes, userRes] = await Promise.all([
      getGroupTree(),
      getUsers({ page: 1, size: 1000 }),
      fetchDictOptions()
    ])
    groupTree.value = groupRes
    userList.value = userRes.records
  } catch (e: any) { ElMessage.error(e?.message || '加载统计数据失败') }

  await nextTick()
  initCharts()
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  repairTypeChart?.dispose()
  deviceStatusChart?.dispose()
  repairTrendChart?.dispose()
  groupRankingChart?.dispose()
  deviceRankingChart?.dispose()
})
</script>

<style lang="scss" scoped>
.filter-card {
  margin-bottom: 0;
}

.filter-hint {
  font-size: 12px;
  line-height: 1.6;
  color: #909399;
}

.efficiency-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  border-left: 4px solid;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 8px;

  .eff-label {
    font-size: 13px;
    color: #909399;
    margin-bottom: 6px;
  }
  .eff-value {
    font-size: 22px;
    font-weight: 600;
  }
}

.chart-card {
  margin-bottom: 12px;
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
</style>

<style lang="scss">
/* 打印样式：隐藏筛选栏与按钮，展开所有图表 */
@media print {
  .statistics-page .filter-card,
  .statistics-page .el-button,
  .statistics-page .card-header-with-action .el-button {
    display: none !important;
  }
  .statistics-page .el-card {
    box-shadow: none !important;
    border: 1px solid #dcdfe6 !important;
    break-inside: avoid;
  }
  .statistics-page .chart-container,
  .statistics-page .chart-container-lg {
    height: 260px !important;
  }
  .statistics-page .efficiency-card {
    box-shadow: none !important;
    border: 1px solid #dcdfe6 !important;
  }
}
</style>
