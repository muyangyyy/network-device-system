<template>
  <div class="device-form">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑设备' : '添加设备' }}</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        style="max-width: 800px"
        v-loading="pageLoading"
      >
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备编码" prop="deviceCode">
              <!-- 设备编码由后端 generateDeviceCode() 生成（NET-yyyyMMdd-序号）。
                   NetworkDeviceDTO 里没有 deviceCode 字段，用户填了也会被 Jackson 丢弃，
                   所以这里恒为只读，避免「填了却不生效」。 -->
              <el-input v-model="form.deviceCode" placeholder="保存后由系统自动生成" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备名称" prop="deviceName">
              <el-input v-model="form.deviceName" placeholder="请输入设备名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备类型" prop="deviceType">
              <el-select v-model="form.deviceType" placeholder="请选择设备类型">
                <el-option v-for="item in deviceTypes" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌" prop="brand">
              <el-input v-model="form.brand" placeholder="请输入品牌" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="型号" prop="model">
              <el-input v-model="form.model" placeholder="请输入型号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="序列号" prop="serialNumber">
              <el-input v-model="form.serialNumber" placeholder="请输入序列号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">网络信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="IP地址" prop="ipAddress">
              <el-input v-model="form.ipAddress" placeholder="请输入IP地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="MAC地址" prop="macAddress">
              <el-input v-model="form.macAddress" placeholder="请输入MAC地址" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">管理信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属分组" prop="groupId">
              <el-tree-select
                v-model="form.groupId"
                :data="groupTree"
                :props="{ label: 'name', value: 'id', children: 'children' } as any"
                placeholder="请选择分组"
                check-strictly
                clearable
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人" prop="responsibleUserId">
              <el-select v-model="form.responsibleUserId" placeholder="请选择负责人" filterable clearable>
                <el-option v-for="u in userList" :key="u.id" :label="u.nickname" :value="u.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <!-- 编辑时禁止直接改状态：状态变更必须走列表页「变更状态」，
                   只有那条路径会写 device_status_log 变更日志；直接改会丢失审计记录。
                   取值必须与 DeviceStatus 枚举 / DeviceStatusTag / 列表筛选器一致。 -->
              <el-select v-model="form.status" placeholder="请选择状态" :disabled="isEdit">
                <el-option label="正常运行" value="NORMAL" />
                <el-option label="故障维修" value="FAULT_REPAIR" />
                <el-option label="闲置" value="IDLE" />
                <el-option label="已报废" value="SCRAPPED" />
              </el-select>
              <span v-if="isEdit" class="form-hint">如需修改状态，请返回列表使用「变更状态」</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="安装位置" prop="installationLocation">
              <el-input v-model="form.installationLocation" placeholder="请输入安装位置" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">其他信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="采购日期" prop="purchaseDate">
              <el-date-picker v-model="form.purchaseDate" type="date" placeholder="请选择采购日期" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="质保到期" prop="warrantyExpireDate">
              <el-date-picker v-model="form.warrantyExpireDate" type="date" placeholder="请选择质保到期日" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="描述" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入描述信息" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">{{ isEdit ? '保存修改' : '创建设备' }}</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { getDeviceById, createDevice, updateDevice, type Device } from '@/api/device'
import { getGroupTree, type Group } from '@/api/group'
import { getUsers, type User } from '@/api/user'
import { isValidIP, isValidMAC } from '@/utils/validate'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const pageLoading = ref(false)
const submitLoading = ref(false)

const isEdit = computed(() => !!route.params.id)
const deviceId = computed(() => Number(route.params.id))

// 字段名必须与后端 NetworkDeviceDTO 一致：installationLocation / warrantyExpireDate / remark。
// 曾用 location / warrantyExpiry / description，后端没有这些字段，Jackson 会静默丢弃，导致填写内容不保存。
const form = reactive<Partial<Device>>({
  deviceCode: '',
  deviceName: '',
  deviceType: '',
  brand: '',
  model: '',
  serialNumber: '',
  ipAddress: '',
  macAddress: '',
  groupId: undefined,
  responsibleUserId: undefined,
  status: 'NORMAL',
  purchaseDate: '',
  warrantyExpireDate: '',
  installationLocation: '',
  remark: ''
})

const rules = {
  // deviceCode 不在校验规则里：该字段只读且由后端生成，校验它只会让表单永远提交不了
  deviceName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  deviceType: [{ required: true, message: '请选择设备类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  ipAddress: [
    { validator: (_rule: any, value: string, callback: any) => {
      if (value && !isValidIP(value)) {
        callback(new Error('请输入正确的IP地址格式'))
      } else {
        callback()
      }
    }, trigger: 'blur' }
  ],
  macAddress: [
    { validator: (_rule: any, value: string, callback: any) => {
      if (value && !isValidMAC(value)) {
        callback(new Error('请输入正确的MAC地址格式'))
      } else {
        callback()
      }
    }, trigger: 'blur' }
  ]
}

const deviceTypes = ref<{ label: string; value: string }[]>([
  { label: '路由器', value: 'ROUTER' },
  { label: '交换机', value: 'SWITCH' },
  { label: '防火墙', value: 'FIREWALL' },
  { label: '服务器', value: 'SERVER' },
  { label: '光模块', value: 'OPTICAL' },
  { label: '其他', value: 'OTHER' }
])

const groupTree = ref<Group[]>([])
const userList = ref<User[]>([])

async function fetchDictData() {
  try {
    const dicts = await import('@/api/dict')
    const items = await dicts.getDictItems('device_type')
    deviceTypes.value = items.map((item) => ({ label: item.itemLabel, value: item.itemValue }))
  } catch (e: any) { ElMessage.error(e?.message || '加载表单数据失败') }
}

async function fetchGroupTree() {
  try { groupTree.value = await getGroupTree() } catch (e: any) { ElMessage.error(e?.message || '加载表单数据失败') }
}

async function fetchUserList() {
  try {
    const res = await getUsers({ page: 1, size: 1000 })
    userList.value = res.records
  } catch (e: any) { ElMessage.error(e?.message || '加载表单数据失败') }
}

async function fetchDevice() {
  if (!isEdit.value) return
  pageLoading.value = true
  try {
    const res = await getDeviceById(deviceId.value)
    Object.assign(form, res)
  } catch (e) {
    ElMessage.error((e as any)?.message || '获取设备信息失败')
  } finally {
    pageLoading.value = false
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  // 不要写成 await validate(async (valid) => {...})：校验失败时 validate() 返回 rejected promise，
  // 外层没有 catch 会产生未处理的 promise rejection
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    if (isEdit.value) {
      // 清空分组/负责人时 tree-select/select 的值是 undefined，JSON 序列化会省略字段，
      // 后端按「未传」跳过更新，清空操作静默失效。归一化为 0（0 不是合法 id），
      // 后端把 0 解释为「置空该关联」。
      const payload = {
        ...form,
        groupId: form.groupId ?? 0,
        responsibleUserId: form.responsibleUserId ?? 0
      }
      await updateDevice(deviceId.value, payload)
      ElMessage.success('修改成功')
    } else {
      await createDevice(form)
      ElMessage.success('创建成功')
    }
    router.push('/devices')
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  fetchDictData()
  fetchGroupTree()
  fetchUserList()
  fetchDevice()
})
</script>

<style lang="scss" scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.form-hint {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
