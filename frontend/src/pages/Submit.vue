<template>
  <section class="container band submit-page" style="max-width:720px">
    <div class="mono-kicker">// submit — review before publish</div>
    <h1 class="section-title">话题投稿</h1>
    <p class="muted" style="margin-bottom:32px">分享你的想法或作品。所有投稿经站长审核后才会公开展示。</p>

    <form class="card" @submit.prevent="doSubmit">
      <div class="field">
        <label for="submit-title">标题（可选）</label>
        <input id="submit-title" v-model="title" class="input" maxlength="120" placeholder="给话题起个标题">
      </div>
      <div class="field">
        <label for="submit-body">正文 *</label>
        <textarea id="submit-body" v-model="body" class="textarea" maxlength="60000" required placeholder="说点什么…（敏感词自动拦截）"></textarea>
      </div>
      <div class="field">
        <label for="submit-files">图片（可选，JPG / PNG / WebP，≤ 10MB）</label>
        <input id="submit-files" ref="fileInput" class="input" type="file" multiple accept="image/jpeg,image/png,image/webp" @change="onFiles">
        
      </div>
      <!-- 图片预览：选了文件才显示 -->
      <div v-if="previews.length" class="field">
        <div style="display:flex;gap:8px;flex-wrap:wrap">
          <img v-for="(u, i) in previews" :key="i" :src="u" alt="预览"
              style="width:120px;height:90px;object-fit:cover;border-radius:var(--r-card);border:1px solid var(--hairline)">
        </div>
      </div>
      <div class="field">
        <label for="submit-nickname">昵称（可选）</label>
        <input id="submit-nickname" v-model="nickname" class="input" maxlength="50" placeholder="展示在话题下方">
      </div>
      <p v-if="message" class="form-message" :class="messageType === 'error' ? 'form-message-error' : 'form-message-success'" :role="messageType === 'error' ? 'alert' : 'status'">
        {{ message }}
      </p>
      <div class="flex-between">
        <span class="hint" style="margin:0">先审后发 · 提交即同意<router-link to="/terms" style="text-decoration:underline;text-underline-offset:2px">用户协议</router-link></span>
        <button class="btn btn-primary" type="submit" :disabled="submitting">
          {{ submitting ? '提交中…' : '提交投稿' }}
        </button>
      </div>
    </form>
  </section>
</template>

<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { api } from '../api'

const title = ref('')
const body = ref('')
const nickname = ref('')
const submitting = ref(false)
const message = ref('')
const messageType = ref('')
const fileInput = ref(null)

// file input 不能用 v-model（二进制对象），用 @change 手动拿
const files = ref([])          // 现在存数组
const previews = ref([])       // 多个预览 URL

function onFiles(e) {
  // 先释放上一轮的预览 URL（内存管理）
  previews.value.forEach(u => URL.revokeObjectURL(u))
  files.value = Array.from(e.target.files || [])
  previews.value = files.value.map(f => URL.createObjectURL(f))
  message.value = ''
}

async function doSubmit() {
  message.value = ''
  messageType.value = 'error'
  if (!body.value.trim()) return (message.value = '正文不能为空')
  if (files.value.length > 3) return (message.value = '最多上传 3 张图片')
  for (const f of files.value) {                                      // 每张校验
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(f.type)) return (message.value = '仅支持 JPG/PNG/WebP')
    if (f.size > 10 * 1024 * 1024) return (message.value = '单张图片不能超过 10MB')
  }

  const fd = new FormData()
  fd.append('body', body.value)
  if (title.value.trim()) fd.append('title', title.value.trim())
  if (nickname.value.trim()) fd.append('nickname', nickname.value.trim())
  files.value.forEach(f => fd.append('files', f))     // ← 同名 files 多份 = 后端 List<MultipartFile>

  submitting.value = true
  try {
    await api.submitImage(fd)
    // 清空 + 释放预览
    previews.value.forEach(u => URL.revokeObjectURL(u))
    previews.value = []; files.value = []
    title.value = ''; body.value = ''; nickname.value = ''
    if (fileInput.value) fileInput.value.value = ''
    messageType.value = 'success'
    message.value = '投稿已提交，站长审核通过后会公开展示。'
  } catch (e) {
    messageType.value = 'error'
    message.value = e.message || '投稿失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

onBeforeUnmount(() => previews.value.forEach(url => URL.revokeObjectURL(url)))
</script>
