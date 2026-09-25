<template>
  <div class="sidebar" :class="{ 'is-collapsed': appStore.sidebarCollapsed }">
    <div class="logo-container">
      <img src="" alt="" class="logo-img" v-if="false" />
      <span class="logo-title" v-if="!appStore.sidebarCollapsed">设备管理系统</span>
      <span class="logo-title" v-else>DSM</span>
    </div>
    <el-scrollbar>
      <!-- :key 绑定 route.path：el-menu 的 default-active 只在初始化时生效，
           SPA 内路由变化不重建组件时，内部 activeIndex 仍由「最后一次点击的菜单项」驱动，
           从「添加设备」→创建→行内进入 /devices/edit/1 这类链路会出现高亮残留
           （该链路上 activeMenu 恒为 /devices，key 不变、重建不发生）。
           以 route.path 作 key 强制每次导航重建，default-active（= meta.activeMenu || 当前路径）
           始终生效——高亮永远表达「当前页面所属的菜单区域」。
           本项目路由全部平铺、无子菜单，重建无展开状态丢失的副作用。 -->
      <el-menu
        :key="route.path"
        :default-active="activeMenu"
        :collapse="appStore.sidebarCollapsed"
        :collapse-transition="false"
        background-color="#001529"
        text-color="#ffffffa6"
        active-text-color="#ffffff"
        router
      >
        <SidebarItem
          v-for="route in menuRoutes"
          :key="route.path"
          :item="route"
          :base-path="toAbsolutePath(route.path)"
        />
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/store/app'
import { useUserStore } from '@/store/user'
import SidebarItem from './SidebarItem.vue'
import type { RouteRecordRaw } from 'vue-router'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const activeMenu = computed(() => {
  const { meta, path } = route
  if (meta.activeMenu) return meta.activeMenu as string
  return path
})

const allMenuRoutes = computed(() => {
  const layoutRoute = router.options.routes.find((r) => r.path === '/')
  return layoutRoute?.children || []
})

const menuRoutes = computed(() => {
  return allMenuRoutes.value.filter((r) => {
    if (r.meta?.hidden) return false
    if (r.meta?.permission && !userStore.hasPermission(r.meta.permission as string)) {
      return false
    }
    return true
  })
})

/**
 * 路由表 children 的 path 是相对布局路由的（如 'dashboard'），
 * 菜单跳转需要绝对路径（如 '/dashboard'）。此前直接传相对 path，
 * 导致 SidebarItem 拼出 'dashboard/dashboard' 这类无效 index。
 */
function toAbsolutePath(path: string) {
  return path.startsWith('/') ? path : `/${path}`
}
</script>

<style lang="scss" scoped>
.sidebar {
  height: 100%;
  background-color: #001529;
  transition: width 0.28s;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  &.is-collapsed {
    .logo-container {
      padding: 12px 0;
      justify-content: center;
    }
    .logo-title {
      display: none;
    }
  }
}

.logo-container {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 0 16px;
  gap: 8px;
  overflow: hidden;
  flex-shrink: 0;
  border-bottom: 1px solid #ffffff1a;
}

.logo-img {
  width: 32px;
  height: 32px;
}

.logo-title {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.el-menu {
  border-right: none;
}
</style>
