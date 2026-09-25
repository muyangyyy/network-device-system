import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import { getToken, removeToken } from '@/utils/auth'
import { useUserStore } from '@/store/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
      meta: { title: '登录', noAuth: true }
    },
    {
      path: '/',
      component: () => import('@/layout/index.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/index.vue'),
          meta: { title: '仪表盘', icon: 'Odometer', affix: true }
        },
        {
          path: 'devices',
          name: 'DeviceList',
          component: () => import('@/views/device/list.vue'),
          meta: { title: '设备列表', icon: 'Monitor', permission: 'device:list' }
        },
        {
          path: 'devices/add',
          name: 'DeviceAdd',
          component: () => import('@/views/device/form.vue'),
          meta: { title: '添加设备', icon: 'Monitor', activeMenu: '/devices', permission: 'device:add' }
        },
        {
          path: 'devices/edit/:id',
          name: 'DeviceEdit',
          component: () => import('@/views/device/form.vue'),
          meta: { title: '编辑设备', icon: 'Monitor', activeMenu: '/devices', permission: 'device:edit', hidden: true }
        },
        {
          path: 'devices/detail/:id',
          name: 'DeviceDetail',
          component: () => import('@/views/device/detail.vue'),
          meta: { title: '设备详情', icon: 'Monitor', activeMenu: '/devices', permission: 'device:view', hidden: true }
        },
        {
          path: 'groups',
          name: 'DeviceGroups',
          component: () => import('@/views/group/index.vue'),
          meta: { title: '设备分组', icon: 'Folder', permission: 'group:list' }
        },
        {
          path: 'repairs',
          name: 'RepairList',
          component: () => import('@/views/repair/list.vue'),
          meta: { title: '维修记录', icon: 'Tickets', permission: 'repair:list' }
        },
        {
          path: 'repairs/create',
          name: 'RepairCreate',
          component: () => import('@/views/repair/create.vue'),
          meta: { title: '新建维修记录', icon: 'Tickets', activeMenu: '/repairs', permission: 'repair:add', hidden: true }
        },
        {
          path: 'repairs/detail/:id',
          name: 'RepairDetail',
          component: () => import('@/views/repair/detail.vue'),
          meta: { title: '工单详情', icon: 'Tickets', activeMenu: '/repairs', permission: 'repair:view', hidden: true }
        },
        {
          path: 'repairs/process/:id',
          name: 'RepairProcess',
          component: () => import('@/views/repair/process.vue'),
          meta: { title: '处理工单', icon: 'Tickets', activeMenu: '/repairs', permission: 'repair:process', hidden: true }
        },
        {
          path: 'repairs/accept/:id',
          name: 'RepairAccept',
          component: () => import('@/views/repair/accept.vue'),
          meta: { title: '验收工单', icon: 'Tickets', activeMenu: '/repairs', permission: 'repair:accept', hidden: true }
        },
        {
          path: 'statistics',
          name: 'Statistics',
          component: () => import('@/views/statistics/index.vue'),
          meta: { title: '统计分析', icon: 'DataAnalysis', permission: 'statistics:view' }
        },
        {
          path: 'system/users',
          name: 'UserManagement',
          component: () => import('@/views/system/user.vue'),
          meta: { title: '用户管理', icon: 'User', permission: 'system:user' }
        },
        {
          path: 'system/roles',
          name: 'RoleManagement',
          component: () => import('@/views/system/role.vue'),
          meta: { title: '角色管理', icon: 'UserFilled', permission: 'system:role' }
        },
        {
          path: 'system/departments',
          name: 'DepartmentManagement',
          component: () => import('@/views/system/department.vue'),
          meta: { title: '部门管理', icon: 'OfficeBuilding', permission: 'system:department' }
        },
        {
          path: 'system/dicts',
          name: 'DictManagement',
          component: () => import('@/views/system/dict.vue'),
          meta: { title: '字典管理', icon: 'Collection', permission: 'system:dict' }
        },
        {
          path: 'system/logs',
          name: 'OperationLogs',
          component: () => import('@/views/system/log.vue'),
          meta: { title: '操作日志', icon: 'Document', permission: 'system:log' }
        },
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/profile/index.vue'),
          meta: { title: '个人中心', icon: 'User', hidden: true }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/dashboard'
    }
  ]
})

NProgress.configure({ showSpinner: false })

router.beforeEach(async (to, _from, next) => {
  NProgress.start()
  document.title = `${to.meta.title || ''} - 网络设备管理系统`

  const token = getToken()
  if (to.meta.noAuth) {
    if (token) {
      next('/dashboard')
    } else {
      next()
    }
    return
  }

  if (!token) {
    next(`/login?redirect=${to.path}`)
    return
  }

  const userStore = useUserStore()
  if (!userStore.userInfo) {
    try {
      await userStore.getUserInfo()
      next({ ...to, replace: true })
    } catch {
      // 取用户信息失败（token 失效 / 账号被禁用）。
      // 这里必须调用 next()，否则当前这次导航永远不会结束
      // （页面白屏、NProgress 不消失、控制台报 "Navigation cancelled"）。
      // 不要改成调用 userStore.logout()——它内部会再 router.push 一次，与本次导航冲突。
      removeToken()
      next(`/login?redirect=${to.path}`)
    }
    return
  }

  // 权限门控：后端已按权限键拦截接口，这里补上页面级拦截，
  // 避免「手动输入 URL 能进页面，但页面内所有请求都 403」的体验
  const requiredPerm = to.meta.permission as string | undefined
  if (requiredPerm && !userStore.hasPermission(requiredPerm)) {
    ElMessage.error('没有权限访问该页面')
    next('/dashboard')
    return
  }

  next()
})

router.afterEach(() => {
  NProgress.done()
})

// 动态 import 页面 chunk 失败的自愈兜底：
// 前端重新部署后旧 chunk 全部 404，已打开的浏览器会话点击「未访问过」的菜单时
// 动态加载失败 → 跳转静默中断（表现为「点击没反应」）。
// 硬导航到目标路由，强制从服务器拿最新 index.html 与 bundle，一次性自愈。
// sessionStorage 标记防止服务器真故障时的无限 reload 循环。
router.onError((error, to) => {
  const msg = String((error as Error)?.message || error)
  const isChunkLoadError =
    msg.includes('Failed to fetch dynamically imported module') ||
    msg.includes('Importing a module script failed') ||
    msg.includes('Loading chunk')
  if (isChunkLoadError && to) {
    const key = `chunk-reload:${to.fullPath}`
    if (!sessionStorage.getItem(key)) {
      sessionStorage.setItem(key, '1')
      window.location.assign(to.fullPath)
    }
  }
})

export default router
