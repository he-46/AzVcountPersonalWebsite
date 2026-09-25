<template>
  <section class="container band" style="max-width:820px">
    <div class="mono-kicker">// publish — admin only, no review needed</div>
    <h1 class="section-title">内容发布</h1>
    <p class="muted" style="margin-bottom:32px">站长发布免审核，直接进入公开展示。</p>

    <form class="card" @submit.prevent="publish">
      <div class="field">
        <label for="publish-title">标题 *</label>
        <input id="publish-title" v-model="title" class="input" maxlength="80" placeholder="文章标题" required>
      </div>
      <div class="field">
        <label for="publish-body">正文（Markdown）*</label>
        <textarea id="publish-body" v-model="body" class="textarea" style="min-height:220px" placeholder="# 标题&#10;&#10;支持 **Markdown** 渲染" required></textarea>
      </div>
      <div class="field">
        <label for="publish-files">图片（可选，最多 3 张）</label>
        <input id="publish-files" ref="fileInput" class="input" type="file" multiple accept="image/jpeg,image/png,image/webp" @change="onFiles">
      </div>
      <!-- 多图预览 -->
      <div v-if="previews.length" class="field">
        <div style="display:flex;gap:8px;flex-wrap:wrap">
          <img v-for="(u, i) in previews" :key="u" :src="u" :alt="`待发布图片 ${i + 1} 预览`"
               style="width:120px;height:90px;object-fit:cover;border-radius:var(--r-card);border:1px solid var(--hairline)">
        </div>
      </div>
      <p v-if="message" class="form-message" :class="messageType === 'error' ? 'form-message-error' : 'form-message-success'" :role="messageType === 'error' ? 'alert' : 'status'">
        {{ message }}
      </p>
      <div class="flex-between">
        <span class="hint" style="margin:0">发布即展示，免审核</span>
        <button class="btn btn-primary" type="submit" :disabled="publishing">
          {{ publishing ? '发布中…' : '发布' }}
        </button>
      </div>
    </form>
  </section>
</template>

<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { api } from '../../api'

const title = ref('')
const body = ref('')
const files = ref([])
const previews = ref([])
const publishing = ref(false)
const fileInput = ref(null)
const message = ref('')
const messageType = ref('')

function clearPreviews() {
  previews.value.forEach(url => URL.revokeObjectURL(url))
  previews.value = []
}

function onFiles(e) {
  clearPreviews()
  message.value = ''
  files.value = Array.from(e.target.files || [])
  previews.value = files.value.map(f => URL.createObjectURL(f))
}

async function publish() {
  message.value = ''
  messageType.value = 'error'
  if (!title.value.trim()) return (message.value = '标题不能为空')
  if (!body.value.trim()) return (message.value = '正文不能为空')
  if (files.value.length > 3) return (message.value = '最多上传 3 张图片')
  for (const f of files.value) {
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(f.type)) return (message.value = '仅支持 JPG/PNG/WebP')
    if (f.size > 10 * 1024 * 1024) return (message.value = '单张图片不能超过 10MB')
  }

  const fd = new FormData()
  fd.append('title', title.value.trim())
  fd.append('body', body.value)
  files.value.forEach(f => fd.append('files', f))

  publishing.value = true
  try {
    await api.adminPublishPost(fd)
    clearPreviews()
    files.value = []
    title.value = ''
    body.value = ''
    if (fileInput.value) fileInput.value.value = ''
    messageType.value = 'success'
    message.value = '发布成功，内容已公开展示。'
  } catch (e) {
    messageType.value = 'error'
    message.value = e.message || '发布失败，请稍后重试'
  } finally {
    publishing.value = false
  }
}

onBeforeUnmount(clearPreviews)
</script>
