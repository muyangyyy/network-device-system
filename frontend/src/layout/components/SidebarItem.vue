<template>
  <div v-if="item.children && item.children.length > 0">
    <template v-if="!item.meta?.hidden">
      <el-sub-menu v-if="item.children.length > 1" :index="basePath">
        <template #title>
          <el-icon v-if="item.meta?.icon">
            <component :is="item.meta.icon" />
          </el-icon>
          <span>{{ item.meta?.title }}</span>
        </template>
        <SidebarItem
          v-for="child in item.children"
          :key="child.path"
          :item="child"
          :base-path="resolvePath(child.path)"
        />
      </el-sub-menu>
      <el-menu-item
        v-else
        :index="resolvePath(item.children[0].path)"
      >
        <el-icon v-if="item.children[0].meta?.icon || item.meta?.icon">
          <component :is="item.children[0].meta?.icon || item.meta?.icon" />
        </el-icon>
        <template #title>{{ item.children[0].meta?.title || item.meta?.title }}</template>
      </el-menu-item>
    </template>
  </div>
  <!-- 叶子菜单项的跳转 index 直接用 basePath。
       Sidebar 传入的 basePath 已是该路由的绝对路径（如 /devices）。
       旧写法 resolvePath(item.path) 会把 item.path 再拼一次：
       basePath='dashboard' + item.path='dashboard' => 'dashboard/dashboard'，
       el-menu router 模式 push 这个无效路径 → 全部落到 catch-all 重定向回仪表盘，
       表现为「点击左侧任何菜单都无法跳转」。 -->
  <el-menu-item v-else :index="basePath">
    <el-icon v-if="item.meta?.icon">
      <component :is="item.meta.icon" />
    </el-icon>
    <template #title>{{ item.meta?.title }}</template>
  </el-menu-item>
</template>

<script setup lang="ts">
import type { RouteRecordRaw } from 'vue-router'

const props = defineProps<{
  item: RouteRecordRaw & { meta?: any }
  basePath: string
}>()

/**
 * 把相对子路径拼到父级绝对路径后面，仅用于嵌套 children 场景。
 * 调用前提：basePath 必须是以 / 开头的绝对路径（Sidebar 已保证）。
 */
function resolvePath(routePath: string) {
  if (routePath.startsWith('/')) return routePath
  const base = props.basePath.replace(/\/$/, '')
  return `${base}/${routePath}`.replace(/\/+/g, '/')
}
</script>
