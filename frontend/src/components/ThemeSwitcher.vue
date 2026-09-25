<template>
  <div class="theme-switch" :class="{ open }">
    <button class="nav-link theme-btn" type="button" @click.stop="open = !open">
      <span>{{ label }}</span>
    </button>
    <div class="theme-menu">
      <button v-for="t in THEMES" :key="t.id" class="theme-opt"
              :class="{ active: theme.name === t.id }" type="button" @click="pick(t.id)">
        {{ t.name }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useThemeStore } from '../stores/theme'

const THEMES = [
  { id: 'warp', name: '暗色' },
  { id: 'light', name: '白色' },
  { id: 'dark', name: '黑色' },
  { id: 'green', name: '浅绿' }
]
const theme = useThemeStore()
const open = ref(false)
const label = computed(() => THEMES.find(t => t.id === theme.name)?.name || '主题')
function pick(id) { theme.set(id); open.value = false }
</script>
