<template>
  <div class="navbar">
    <div class="left-section">
      <el-icon class="hamburger" @click="appStore.toggleSidebar">
        <Fold v-if="!appStore.sidebarCollapsed" />
        <Expand v-else />
      </el-icon>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
          <span v-if="item.redirect === 'noRedirect' || item === breadcrumbs[breadcrumbs.length - 1]">
            {{ item.meta?.title }}
          </span>
          <router-link v-else :to="item.path">{{ item.meta?.title }}</router-link>
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="right-section">
      <el-badge :value="notificationStore.unreadCount" :hidden="notificationStore.unreadCount === 0" :max="99">
        <el-icon class="notification-icon" @click="showNotifications = true">
          <Bell />
        </el-icon>
      </el-badge>
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-info">
          <el-avatar :size="28" :src="userStore.userInfo?.avatar">
            {{ userStore.userInfo?.realName?.charAt(0) }}
          </el-avatar>
          <span class="username">{{ userStore.userInfo?.realName }}</span>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>个人中心
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <el-drawer v-model="showNotifications" title="通知消息" size="400px">
      <div class="notification-header">
        <el-button type="primary" link @click="notificationStore.readAll()">全部已读</el-button>
      </div>
      <el-scrollbar height="calc(100vh - 200px)">
        <div v-for="n in notificationStore.notifications" :key="n.id" class="notification-item" :class="{ unread: !n.isRead }" @click="handleNotificationClick(n)">
          <div class="notification-title">{{ n.title }}</div>
          <div class="notification-content">{{ n.content }}</div>
          <div class="notification-time">{{ n.createTime }}</div>
        </div>
        <el-empty v-if="notificationStore.notifications.length === 0" description="暂无通知" />
      </el-scrollbar>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/store/app'
import { useUserStore } from '@/store/user'
import { useNotificationStore } from '@/store/notification'
import { Fold, Expand, Bell, ArrowDown, User, SwitchButton } from '@element-plus/icons-vue'
import type { Notification } from '@/api/notification'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const showNotifications = ref(false)

// 抽屉打开时才拉列表：轮询只刷新未读数，不再反复重拉列表。
// 若不在这里拉取，抽屉里会永远显示「暂无通知」。
watch(showNotifications, (open) => {
  if (open) notificationStore.refresh(1, 20)
})

const breadcrumbs = computed(() => {
  const matched = route.matched.filter((item) => item.meta && item.meta.title)
  return matched
})

function handleCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.logout()
    })
  }
}

function handleNotificationClick(n: Notification) {
  notificationStore.readNotification(n.id)
  if (n.relatedId && n.relatedType) {
    showNotifications.value = false
    if (n.relatedType === 'repair') {
      router.push(`/repairs/detail/${n.relatedId}`)
    } else if (n.relatedType === 'device') {
      router.push(`/devices/detail/${n.relatedId}`)
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  height: $navbar-height;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  flex-shrink: 0;
}

.left-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hamburger {
  font-size: 20px;
  cursor: pointer;
  color: #333;
  transition: color 0.2s;
  &:hover { color: $primary-color; }
}

.right-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.notification-icon {
  font-size: 20px;
  cursor: pointer;
  color: #666;
  &:hover { color: $primary-color; }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  color: #333;
}

.notification-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.notification-item {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;

  &:hover { background-color: #f5f7fa; }
  &.unread { background-color: #ecf5ff; }

  .notification-title {
    font-size: 14px;
    font-weight: 500;
    margin-bottom: 4px;
  }
  .notification-content {
    font-size: 12px;
    color: #999;
    margin-bottom: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .notification-time {
    font-size: 12px;
    color: #c0c4cc;
  }
}
</style>
