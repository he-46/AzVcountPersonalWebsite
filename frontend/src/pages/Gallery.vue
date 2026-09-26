<template>
  <section class="band container gallery-page">
    <div class="mono-kicker">// gallery — approved only</div>
    <h1 class="section-title" style="margin-bottom:8px">图片墙</h1>
    <p class="muted" style="margin-bottom:32px">
      仅展示已通过审核的图片 · 想分享你的作品？
      <router-link to="/submit" style="text-decoration:underline;text-underline-offset:2px">去投稿</router-link>
    </p>

    <div v-if="loading" class="card page-state" aria-live="polite">
      <span class="state-spinner" aria-hidden="true"></span>
      <span>正在加载图片…</span>
    </div>
    <div v-else-if="error && !items.length" class="card page-state page-state-error" role="alert">
      <div>
        <b>图片墙加载失败</b>
        <p>{{ error }}</p>
      </div>
      <button class="btn btn-ghost btn-sm" type="button" @click="load(true)">重试</button>
    </div>
    <p v-else-if="!items.length" class="muted">还没有图片</p>

    <template v-else>
      <div class="gallery-grid">
        <router-link
          v-for="item in items"
          :key="item.id"
          :to="`/post/${item.id}`"
          class="card gallery-card"
        >
          <img
            :src="imageFor(item)"
            :alt="item.title || '未命名图片'"
            loading="lazy"
            decoding="async"
          >
          <div class="gallery-card-copy">
            <div>{{ item.title || '未命名' }}</div>
            <div class="hint">{{ item.authorLabel || 'AzV' }} · {{ item.createdAt?.slice(0, 10) }}</div>
          </div>
        </router-link>
      </div>

      <div class="load-more-row">
        <p v-if="error" class="form-message form-message-error" role="alert">{{ error }}</p>
        <button v-if="hasMore" class="btn btn-ghost" type="button" :disabled="loadingMore" @click="loadMore">
          {{ loadingMore ? '加载中…' : '加载更多图片' }}
        </button>
        <span v-else class="hint">已展示全部图片</span>
      </div>
    </template>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'

const items = ref([])
const loading = ref(true)
const loadingMore = ref(false)
const error = ref('')
const page = ref(1)
const size = 12
const hasMore = ref(false)

function pageRecords(data) {
  return Array.isArray(data) ? data : (data?.records || [])
}

function pageHasMore(data, records, requestedPage) {
  if (typeof data?.hasNext === 'boolean') return data.hasNext
  const totalPages = Number(data?.pages)
  const current = Number(data?.current || requestedPage)
  if (Number.isFinite(totalPages) && totalPages > 0) return current < totalPages
  return records.length === size
}

function imageFor(item) {
  return item.thumbnailUrl || item.mediaUrl || item.coverUrl || item.images?.[0]?.thumbnailUrl || item.images?.[0]?.url || ''
}

async function requestPage(requestedPage) {
  return api.listContent({ page: requestedPage, size, type: 'POST', withMedia: true })
}

async function load(reset = false) {
  const requestedPage = reset ? 1 : page.value
  if (reset) loading.value = true
  error.value = ''
  try {
    const data = await requestPage(requestedPage)
    const pageItems = pageRecords(data)
    const records = pageItems.filter(imageFor)
    items.value = reset ? records : [...items.value, ...records]
    page.value = requestedPage
    hasMore.value = pageHasMore(data, pageItems, requestedPage)
  } catch (e) {
    error.value = e.message || '图片加载失败'
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  error.value = ''
  const requestedPage = page.value + 1
  try {
    const data = await requestPage(requestedPage)
    const pageItems = pageRecords(data)
    const records = pageItems.filter(imageFor)
    const knownIds = new Set(items.value.map(item => item.id))
    items.value.push(...records.filter(item => !knownIds.has(item.id)))
    page.value = requestedPage
    hasMore.value = pageHasMore(data, pageItems, requestedPage)
  } catch (e) {
    error.value = e.message || '更多图片加载失败'
  } finally {
    loadingMore.value = false
  }
}

onMounted(() => load(true))
</script>
