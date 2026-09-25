<template>
  <div class="profile-page">
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header><span>个人信息</span></template>
          <div class="profile-avatar">
            <el-avatar :size="80" :src="userStore.userInfo?.avatar">
              {{ userStore.userInfo?.nickname?.charAt(0) }}
            </el-avatar>
            <h3>{{ userStore.userInfo?.nickname }}</h3>
            <p class="profile-role">{{ userStore.userInfo?.roles?.join(', ') }}</p>
          </div>
          <el-descriptions :column="1" border size="small" style="margin-top: 20px">
            <el-descriptions-item label="用户名">{{ userStore.userInfo?.username }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ userStore.userInfo?.nickname }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ userStore.userInfo?.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机">{{ userStore.userInfo?.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="部门">{{ userStore.userInfo?.departmentName || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card shadow="never">
          <template #header><span>修改密码</span></template>
          <el-form ref="formRef" :model="passwordForm" :rules="passwordRules" label-width="100px" style="max-width: 500px">
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入旧密码" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleChangePassword" :loading="submitLoading">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { useUserStore } from '@/store/user'
import { changePassword } from '@/api/auth'

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    // 与后端 ChangePasswordDTO 的 @Size(min=6, max=32) 对齐，否则超长时只能拿到 400
    { min: 6, max: 32, message: '密码长度必须在6-32位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

async function handleChangePassword() {
  if (!formRef.value) return
  // 不能写成 await validate(async (valid) => ...)：校验失败时 validate() 返回 rejected promise
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      // 必须提交：后端 @NotBlank + 一致性校验都要用到它
      confirmPassword: passwordForm.confirmPassword
    })
    ElMessage.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    formRef.value.resetFields()
  } catch (e: any) {
    ElMessage.error(e.message || '修改失败')
  } finally {
    submitLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.profile-avatar {
  text-align: center;
  padding: 20px 0;
  h3 {
    margin: 12px 0 4px;
    font-size: 18px;
    color: #303133;
  }
  .profile-role {
    color: #909399;
    font-size: 13px;
  }
}
</style>
