import { useThemeStore } from '../stores/theme'

// 主题切换的薄封装：页面只需 useTheme().toggle() / set()
export function useTheme() {
  const store = useThemeStore()
  return {
    name: store.name,
    set: (n) => store.set(n)
  }
}
