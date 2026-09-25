import { defineStore } from 'pinia'

// 登录态：Cookie（HttpOnly token）由浏览器管理，这里只记"是否已登录"标记
// 真正的权限校验在后端 AdminInterceptor，前端标记只用于路由守卫和 UI
export const useAuthStore = defineStore('auth', {
  state: () => ({
    authed: localStorage.getItem('azv_authed') === '1'
  }),
  actions: {
    setAuthed(v) {
      this.authed = v
      localStorage.setItem('azv_authed', v ? '1' : '0')
    }
  }
})
