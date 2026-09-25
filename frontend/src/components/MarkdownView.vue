<template>
  <div class="article-body" v-html="html"></div>
</template>

<script setup>
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'

// html:false = 不渲染原始 HTML → 防 XSS（对应需求文档 6.4 的铁律）
const md = new MarkdownIt({ html: false, linkify: true })

const props = defineProps({ source: { type: String, default: '' } })
const html = computed(() => md.render(props.source || ''))
</script>
