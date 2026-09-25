import { defineStore } from 'pinia'

// 4 套主题，与后端无关，纯前端视觉（对应原型 THEMES）
const THEMES = ['warp', 'light', 'dark', 'green']

export const useThemeStore = defineStore('theme', {
  state: () => ({
    name: localStorage.getItem('azv-theme') || 'warp'
  }),
  actions: {
    init() {
      document.documentElement.setAttribute('data-theme', this.name)
    },
    set(name) {
      if (!THEMES.includes(name)) name = 'warp'
      this.name = name
      localStorage.setItem('azv-theme', name)
      document.documentElement.setAttribute('data-theme', name)
    }
  }
})
