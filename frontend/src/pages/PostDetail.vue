<template>
  <section class="container band-sm post-detail" style="max-width:820px">
    <router-link class="muted" to="/" style="font-size:13px">← 返回首页</router-link>

    <div v-if="loading" class="card page-state" aria-live="polite">
      <span class="state-spinner" aria-hidden="true"></span>
      <span>正在加载文章与评论…</span>
    </div>

    <div v-else-if="notFound" class="card page-state page-state-column">
      <div class="state-code">404</div>
      <h1>这篇内容不存在</h1>
      <p class="muted">内容可能已被移除，或链接地址有误。</p>
      <router-link class="btn btn-primary btn-sm" to="/">返回首页</router-link>
    </div>

    <div v-else-if="error" class="card page-state page-state-error" role="alert">
      <div>
        <b>内容加载失败</b>
        <p>{{ error }}</p>
      </div>
      <button class="btn btn-ghost btn-sm" type="button" @click="loadPage">重试</button>
    </div>

    <article v-else-if="post">
      <div style="margin-top:16px" class="post-meta">
        <span>{{ post.createdAt }} · {{ post.authorLabel || 'AzV（站长）' }}</span>
        <span v-if="post.type" class="tag">{{ post.type }}</span>
        <span>♥ {{ post.likeCount || 0 }}</span>
      </div>
      <h1 class="post-detail-title">{{ post.title }}</h1>

      <MarkdownView :source="post.body" />
      <div v-if="images.length" class="post-images">
        <img
          v-for="(img, index) in images"
          :key="img.id || img.url || index"
          :src="img.url || img.mediaUrl"
          :alt="`${post.title || '文章'}配图 ${index + 1}`"
          loading="lazy"
          decoding="async"
        >
      </div>

      <div class="flex-between post-actions">
        <div style="display:flex;gap:12px">
          <button class="btn btn-ghost btn-sm" type="button" @click="doLike" :disabled="liking">
            ♥ <span>{{ post.likeCount || 0 }}</span>
          </button>
          <button class="btn btn-ghost btn-sm" type="button" @click="reportOpen = !reportOpen" :aria-expanded="reportOpen">
            举报
          </button>
        </div>
        <span class="muted" style="font-size:12px">浏览 {{ post.viewCount || 0 }}</span>
      </div>

      <form v-if="reportOpen" class="card report-form" @submit.prevent="doReport">
        <div class="field">
          <label for="report-reason">举报原因</label>
          <textarea
            id="report-reason"
            v-model="reportReason"
            class="textarea"
            maxlength="300"
            placeholder="请简要说明问题"
            required
          ></textarea>
        </div>
        <div class="flex-between">
          <span class="hint" style="margin:0">仅用于内容审核，最多 300 字</span>
          <div style="display:flex;gap:8px">
            <button class="btn btn-ghost btn-sm" type="button" @click="cancelReport">取消</button>
            <button class="btn btn-primary btn-sm" type="submit" :disabled="reporting || !reportReason.trim()">
              {{ reporting ? '提交中…' : '提交举报' }}
            </button>
          </div>
        </div>
      </form>

      <p
        v-if="actionMessage"
        class="form-message"
        :class="actionMessageType === 'error' ? 'form-message-error' : 'form-message-success'"
        :role="actionMessageType === 'error' ? 'alert' : 'status'"
      >
        {{ actionMessage }}
      </p>
    </article>
  </section>

  <section v-if="!loading && !error && !notFound && post" class="container comments-section" style="max-width:820px;padding-bottom:96px">
    <h2>
      评论 <span class="muted">（游客评论需站长审核后展示）</span>
    </h2>

    <div v-for="c in comments" :key="c.id" class="comment">
      <div class="c-head">
        <b>{{ c.authorLabel || '游客' }}</b>
        <span>{{ c.createdAt }}</span>
      </div>
      <p style="color:var(--body);font-size:14px">{{ c.body }}</p>
    </div>
    <p v-if="!comments.length" class="muted" style="padding:16px 0">还没有评论，来抢沙发～</p>

    <form class="card" @submit.prevent="submitComment">
      <div class="field">
        <label for="comment-nickname">昵称（可选）</label>
        <input id="comment-nickname" v-model="nickname" class="input" maxlength="20" placeholder="不填则显示 游客 #xxxx">
      </div>
      <div class="field">
        <label for="comment-body">评论内容</label>
        <textarea id="comment-body" v-model="commentText" class="textarea" maxlength="1000" placeholder="友善交流，敏感词会被拦截…" required></textarea>
      </div>
      <p
        v-if="commentMessage"
        class="form-message"
        :class="commentMessageType === 'error' ? 'form-message-error' : 'form-message-success'"
        :role="commentMessageType === 'error' ? 'alert' : 'status'"
      >
        {{ commentMessage }}
      </p>
      <div class="flex-between">
        <span class="hint" style="margin:0">先审后发 · 提交后由站长审核</span>
        <button class="btn btn-primary btn-sm" type="submit" :disabled="submitting || !commentText.trim()">
          {{ submitting ? '提交中…' : '提交评论' }}
        </button>
      </div>
    </form>
  </section>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'
import MarkdownView from '../components/MarkdownView.vue'

const route = useRoute()
const post = ref(null)
const comments = ref([])
const images = ref([])
const loading = ref(true)
const error = ref('')
const notFound = ref(false)

const nickname = ref('')
const commentText = ref('')
const submitting = ref(false)
const commentMessage = ref('')
const commentMessageType = ref('')

const liking = ref(false)
const reportOpen = ref(false)
const reportReason = ref('')
const reporting = ref(false)
const actionMessage = ref('')
const actionMessageType = ref('')

function isNotFoundError(err) {
  return err?.status === 404 || /(?:不存在|已删除|not\s*found)/i.test(err?.message || '')
}

async function loadPage() {
  loading.value = true
  error.value = ''
  notFound.value = false
  post.value = null
  comments.value = []
  images.value = []

  try {
    const [detailData, commentData] = await Promise.all([
      api.getContent(route.params.id),
      api.listComments(route.params.id)
    ])
    const content = detailData?.content || detailData
    post.value = content
    images.value = detailData?.images || content?.images || []
    comments.value = Array.isArray(commentData) ? commentData : (commentData?.records || [])
    if (content?.title) document.title = `${content.title} — AzV`
  } catch (e) {
    if (isNotFoundError(e)) notFound.value = true
    else error.value = e.message || '网络异常，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function submitComment() {
  const text = commentText.value.trim()
  if (!text) return
  commentMessage.value = ''
  submitting.value = true
  try {
    await api.postComment({
      contentId: route.params.id,
      text,
      nickname: nickname.value.trim()
    })
    commentText.value = ''
    commentMessageType.value = 'success'
    commentMessage.value = '评论已提交，站长审核通过后会显示在这里。'
  } catch (e) {
    commentMessageType.value = 'error'
    commentMessage.value = e.message || '评论提交失败'
  } finally {
    submitting.value = false
  }
}

async function doLike() {
  actionMessage.value = ''
  liking.value = true
  try {
    const result = await api.like(route.params.id)
    post.value.likeCount = typeof result === 'number' ? result : (result?.likeCount ?? post.value.likeCount)
  } catch (e) {
    actionMessageType.value = 'error'
    actionMessage.value = e.message || '点赞失败'
  } finally {
    liking.value = false
  }
}

function cancelReport() {
  reportOpen.value = false
  reportReason.value = ''
}

async function doReport() {
  const reason = reportReason.value.trim()
  if (!reason) return
  actionMessage.value = ''
  reporting.value = true
  try {
    await api.report({ contentId: route.params.id, reason })
    cancelReport()
    actionMessageType.value = 'success'
    actionMessage.value = '举报已提交，站长会尽快处理。'
  } catch (e) {
    actionMessageType.value = 'error'
    actionMessage.value = e.message || '举报提交失败'
  } finally {
    reporting.value = false
  }
}

watch(() => route.params.id, loadPage, { immediate: true })
</script>
