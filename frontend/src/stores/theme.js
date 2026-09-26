import { defineStore } from 'pinia'

export const STYLES = [
  { id: 'editorial', name: '书页', description: '安静的数字花园' },
  { id: 'studio', name: '工作室', description: '明亮的模块画布' },
  { id: 'zine', name: '杂志', description: '大胆的拼贴海报' },
  { id: 'terminal', name: '终端', description: '开发者控制台' }
]

const valid = name => STYLES.some(style => style.id === name)
const legacy = { warp: 'editorial', light: 'editorial', dark: 'terminal', green: 'zine' }

function savedStyle() {
  try {
    const saved = localStorage.getItem('azv-style') || legacy[localStorage.getItem('azv-theme')]
    return valid(saved) ? saved : 'editorial'
  } catch {
    return 'editorial'
  }
}

export const useThemeStore = defineStore('theme', {
  state: () => ({ name: savedStyle() }),
  actions: {
    init() { this.set(this.name) },
    set(name) {
      this.name = valid(name) ? name : 'editorial'
      document.documentElement.removeAttribute('data-theme')
      document.documentElement.setAttribute('data-style', this.name)
      try { localStorage.setItem('azv-style', this.name) } catch { /* private browsing */ }
    }
  }
})
