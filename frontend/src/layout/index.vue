<template>
  <div class="app-wrapper" :class="{ 'sidebar-collapsed': appStore.sidebarCollapsed }">
    <Sidebar />
    <div class="main-container">
      <Navbar />
      <TagsView />
      <div class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <keep-alive :include="cachedViews">
              <component :is="Component" :key="$route.path" />
            </keep-alive>
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'
import TagsView from './components/TagsView.vue'
import { useAppStore } from '@/store/app'
import { useNotificationStore } from '@/store/notification'

const appStore = useAppStore()
const notificationStore = useNotificationStore()

const cachedViews = computed(() => ['Dashboard', 'DeviceList', 'RepairList'])

onMounted(() => {
  notificationStore.startPolling()
  handleResize()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  notificationStore.stopPolling()
  window.removeEventListener('resize', handleResize)
})

function handleResize() {
  if (window.innerWidth < 768) {
    appStore.setSidebarCollapsed(true)
    appStore.setDevice('mobile')
  } else {
    appStore.setDevice('desktop')
  }
}
</script>

<style lang="scss" scoped>
.app-wrapper {
  position: relative;
  height: 100%;
  width: 100%;
  display: flex;

  &.sidebar-collapsed {
    .sidebar-container {
      width: $sidebar-collapsed-width;
    }
  }
}

.sidebar-container {
  width: $sidebar-width;
  height: 100%;
  transition: width 0.28s ease;
  flex-shrink: 0;
  overflow: hidden;
  z-index: 1001;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.app-main {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: $bg-color;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
