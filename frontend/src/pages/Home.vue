<template>
  <section class="hero container home-hero">
    <div class="home-hero-copy">
      <div class="mono-kicker">// PERSONAL SITE · CODE / IDEAS / WORK</div>
      <h1>记录<span class="hero-accent">想法</span>，<br>构建作品。</h1>
      <p class="home-hero-sub">你好，我是 AzV。一名 Java 全栈学习者，在这里分享技术笔记、思考和正在打磨的作品。</p>
      <div class="home-hero-actions">
        <a class="btn btn-primary" href="#latest">阅读文章 ↗</a>
        <router-link class="btn btn-ghost" to="/about">了解我 →</router-link>
      </div>
    </div>
    <div class="home-hero-art" aria-hidden="true"><span class="art-index">FIG. 01 / AZV</span><span class="art-note">IDEAS INTO INTERFACES</span></div>
  </section>

  <form class="container search-row" role="search" @submit.prevent="search">
    <label class="sr-only" for="home-search">搜索文章</label>
    <input
      id="home-search"
      v-model="keyword"
      class="input"
      style="max-width:320px"
      placeholder="搜索标题 / 内容 / 发布者"
    >
    <button class="btn btn-ghost" type="submit" :disabled="searchLoading || !keyword.trim()">
      {{ searchLoading ? '搜索中…' : '搜索' }}
    </button>
    <button v-if="searched" class="btn btn-ghost" type="button" @click="clearSearch">清除</button>
  </form>

  <div v-if="searched" class="container search-results">
    <div class="card" style="padding:8px 16px" aria-live="polite">
      <p v-if="searchLoading" class="muted" style="padding:12px 0">正在搜索…</p>
      <div v-else-if="searchError" class="inline-error" role="alert">
        <span>{{ searchError }}</span>
        <button class="btn btn-ghost btn-sm" type="button" @click="search">重试</button>
      </div>
      <p v-else-if="!searchResults.length" class="muted" style="padding:12px 0">
        没有找到“{{ searchedKeyword }}”相关内容
      </p>
      <router-link
        v-for="r in searchResults"
        v-else
        :key="r.id"
        :to="`/post/${r.id}`"
        class="post-card search-result-item"
      >
        <div style="font-weight:500">{{ r.title || '未命名' }}</div>
        <p v-if="excerptFor(r)" class="search-excerpt">{{ excerptFor(r) }}</p>
        <div class="hint" style="margin-top:2px">{{ r.authorLabel || 'AzV' }} · {{ r.createdAt?.slice(0, 10) }}</div>
      </router-link>
    </div>
  </div>

  <section id="latest" class="container content-feed home-content-grid">
    <div class="home-feed-column">
      <div class="home-section-heading">
        <div><span class="mono-kicker">01 / JOURNAL</span><h2 class="section-title">最新内容</h2></div>
        <span class="home-heading-note">近期发布的文章与记录</span>
      </div>

    <div v-if="loading" class="card page-state" aria-live="polite">
      <span class="state-spinner" aria-hidden="true"></span>
      <span>正在加载最新内容…</span>
    </div>
    <div v-else-if="loadError && !contents.length" class="card page-state page-state-error" role="alert">
      <div>
        <b>内容加载失败</b>
        <p>{{ loadError }}</p>
      </div>
      <button class="btn btn-ghost btn-sm" type="button" @click="load(true)">重试</button>
    </div>

    <template v-else>
      <router-link v-for="c in contents" :key="c.id" class="home-post" :to="`/post/${c.id}`">
        <span class="home-post-date">{{ c.createdAt?.slice(0, 10) || '—' }}</span>
        <span class="home-post-body"><strong>{{ c.title || '未命名' }}</strong><small>{{ excerptFor(c) }}</small><span class="home-post-meta">{{ c.authorLabel || 'AzV' }} · ♥ {{ c.likeCount || 0 }}</span></span>
        <span class="home-post-arrow" aria-hidden="true">↗</span>
      </router-link>

      <p v-if="!contents.length" class="muted" style="padding:24px 0">暂无内容</p>

      <div v-if="contents.length" class="load-more-row">
        <p v-if="loadError" class="form-message form-message-error" role="alert">{{ loadError }}</p>
        <button v-if="hasMore" class="btn btn-ghost" type="button" :disabled="loadingMore" @click="loadMore">
          {{ loadingMore ? '加载中…' : '加载更多' }}
        </button>
        <span v-else class="hint">已经到底了</span>
      </div>
    </template>
    </div>
    <aside class="home-side-stack" aria-label="关于与图库">
      <div class="home-panel home-profile-panel">
        <span class="mono-kicker">02 / ABOUT</span>
        <div class="home-profile-row"><span class="home-avatar">A</span><div><strong>AzV</strong><small>Java 全栈学习者</small></div></div>
        <p>持续学习，认真创造。这个站点是我的作品集，也是开放的个人笔记。</p>
        <router-link class="home-text-link" to="/about">探索作品集 ↗</router-link>
      </div>
      <div class="home-panel home-gallery-panel">
        <div class="home-panel-heading"><span class="mono-kicker">03 / VISUALS</span><router-link class="home-text-link" to="/gallery">全部 →</router-link></div>
        <h3>图片墙</h3>
        <div v-if="galleryPreview.length" class="home-gallery-preview">
          <router-link v-for="item in galleryPreview" :key="item.id" :to="`/post/${item.id}`" :aria-label="item.title || '查看图片'">
            <img :src="imageFor(item)" :alt="item.title || '图片墙作品'" loading="lazy" decoding="async">
          </router-link>
        </div>
        <p v-else class="muted">{{ galleryError ? '图库暂时无法加载' : '更多视觉作品，正在路上。' }}</p>
      </div>
    </aside>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'

const contents = ref([])
const loading = ref(true)
const loadingMore = ref(false)
const loadError = ref('')
const page = ref(1)
const size = 8
const hasMore = ref(false)
const galleryPreview = ref([])
const galleryError = ref(false)

function imageFor(item) {
  return item.thumbnailUrl || item.mediaUrl || item.coverUrl || item.images?.[0]?.thumbnailUrl || item.images?.[0]?.url || ''
}

async function loadGalleryPreview() {
  try {
    const data = await api.listContent({ page: 1, size: 12, type: 'POST', withMedia: true })
    galleryPreview.value = pageRecords(data).filter(imageFor).slice(0, 4)
  } catch {
    galleryError.value = true
  }
}

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

function excerptFor(content) {
  const text = content?.excerpt ?? content?.body ?? ''
  return text.length > 180 ? `${text.slice(0, 180).trim()}…` : text
}

async function load(reset = false) {
  const requestedPage = reset ? 1 : page.value
  if (reset) loading.value = true
  loadError.value = ''
  try {
    const data = await api.listContent({ page: requestedPage, size, type: 'POST' })
    const records = pageRecords(data)
    contents.value = reset ? records : [...contents.value, ...records]
    page.value = requestedPage
    hasMore.value = pageHasMore(data, records, requestedPage)
  } catch (e) {
    loadError.value = e.message || '网络异常，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  loadError.value = ''
  const requestedPage = page.value + 1
  try {
    const data = await api.listContent({ page: requestedPage, size, type: 'POST' })
    const records = pageRecords(data)
    const knownIds = new Set(contents.value.map(item => item.id))
    contents.value.push(...records.filter(item => !knownIds.has(item.id)))
    page.value = requestedPage
    hasMore.value = pageHasMore(data, records, requestedPage)
  } catch (e) {
    loadError.value = e.message || '更多内容加载失败'
  } finally {
    loadingMore.value = false
  }
}

const keyword = ref('')
const searchedKeyword = ref('')
const searched = ref(false)
const searchLoading = ref(false)
const searchError = ref('')
const searchResults = ref([])

async function search() {
  const term = keyword.value.trim()
  if (!term) return
  searched.value = true
  searchedKeyword.value = term
  searchLoading.value = true
  searchError.value = ''
  try {
    const data = await api.listContent({ page: 1, size: 20, type: 'POST', keyword: term })
    searchResults.value = pageRecords(data)
  } catch (e) {
    searchResults.value = []
    searchError.value = e.message || '搜索失败，请稍后重试'
  } finally {
    searchLoading.value = false
  }
}

function clearSearch() {
  keyword.value = ''
  searchedKeyword.value = ''
  searched.value = false
  searchError.value = ''
  searchResults.value = []
}

onMounted(() => { load(true); loadGalleryPreview() })
</script>
