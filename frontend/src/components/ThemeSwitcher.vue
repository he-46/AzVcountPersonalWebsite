<template>
  <div ref="root" class="theme-switch" :class="{ open }">
    <button class="nav-link theme-btn" type="button" aria-haspopup="menu" :aria-expanded="open" @click="open = !open">
      <span class="style-current-dot" :data-style-preview="theme.name" aria-hidden="true"></span>
      风格 · {{ label }} <span aria-hidden="true">⌄</span>
    </button>
    <div v-if="open" class="style-menu" role="menu" aria-label="选择网站风格">
      <button v-for="style in STYLES" :key="style.id" type="button" role="menuitemradio"
              class="style-option" :aria-checked="theme.name === style.id" @click="pick(style.id)">
        <span class="style-preview" :data-style-preview="style.id" aria-hidden="true">AZV</span>
        <span class="style-option-copy"><strong>{{ style.name }}</strong><small>{{ style.description }}</small></span>
        <span v-if="theme.name === style.id" class="style-selected" aria-hidden="true">✓</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { STYLES, useThemeStore } from '../stores/theme'

const theme = useThemeStore()
const root = ref(null)
const open = ref(false)
const label = computed(() => STYLES.find(style => style.id === theme.name)?.name || '书页')
function pick(id) { theme.set(id); open.value = false }
function onOutside(event) { if (!root.value?.contains(event.target)) open.value = false }
function onKeydown(event) { if (event.key === 'Escape') open.value = false }
onMounted(() => { document.addEventListener('pointerdown', onOutside); document.addEventListener('keydown', onKeydown) })
onUnmounted(() => { document.removeEventListener('pointerdown', onOutside); document.removeEventListener('keydown', onKeydown) })
</script>
