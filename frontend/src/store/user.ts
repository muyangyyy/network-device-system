import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi, getCurrentUser, type UserInfo } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken() || '')
  const userInfo = ref<UserInfo | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])

  async function login(username: string, password: string) {
    const res = await loginApi({ username, password })
    token.value = res.token
    setToken(res.token)
    return res
  }

  async function getUserInfo() {
    const res = await getCurrentUser()
    userInfo.value = res
    roles.value = res.roles || []
    permissions.value = res.permissions || []
    return res
  }

  async function logout() {
    try {
      await logoutApi()
    } catch (e) {
      // ignore
    }
    token.value = ''
    userInfo.value = null
    roles.value = []
    permissions.value = []
    removeToken()
    router.push('/login')
  }

  function hasPermission(perm: string) {
    return permissions.value.includes('*') || permissions.value.includes(perm)
  }

  function hasRole(role: string) {
    // JWT 中的角色是角色标识（ADMIN / OPERATOR / READONLY），做大小写不敏感比较。
    // 注意：权限控制请优先用 hasPermission，角色判断只用于极少数的界面分支。
    const target = role.toUpperCase()
    return roles.value.some((r) => r.toUpperCase() === target)
  }

  return {
    token,
    userInfo,
    roles,
    permissions,
    login,
    getUserInfo,
    logout,
    hasPermission,
    hasRole
  }
})
