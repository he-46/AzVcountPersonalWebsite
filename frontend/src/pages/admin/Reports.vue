<template>
  <section class="container band" style="max-width:1080px">
    <div class="mono-kicker">// reports — OPEN first</div>
    <h1 class="section-title">举报处理</h1>

    <div v-if="loading" class="muted">加载中…</div>
    <p v-else-if="!list.length" class="muted">没有待处理举报 🎉</p>

    <div v-for="r in list" :key="r.id" class="card" style="margin-bottom:16px">
      <div class="audit-item">
        <div class="ai-info">
          <b>举报 #{{ r.id }} · 内容 #{{ r.contentId }}</b>
          <p>原因：{{ r.reason }} · 举报者 {{ r.reporterIp }}</p>
          <div class="ai-meta">{{ r.createdAt }} · 状态 {{ r.status }}</div>
        </div>
        <div class="ai-actions">
          <!-- 跳到前台查看被举报内容（能看就看，被删了会 404）-->
          <router-link class="btn btn-ghost btn-sm" :to="`/post/${r.contentId}`">查看内容</router-link>
          <button class="btn btn-danger btn-sm" @click="takeDown(r)">下架</button>
          <button class="btn btn-ghost btn-sm" @click="ignore(r)">忽略</button>
        </div>
      </div>
    </div>

    <div v-if="total > size" class="flex-between" style="margin-top:24px">
      <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
      <span class="muted">{{ page }} / {{ totalPages }}</span>
      <button class="btn btn-ghost btn-sm" :disabled="page >= totalPages" @click="page++; load()">下一页</button>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '../../api'

const page = ref(1)
const size = ref(10)
const list = ref([])
const total = ref(0)
const loading = ref(false)
const totalPages = computed(() => Math.ceil(total.value / size.value))

async function load() {
  loading.value = true
  try {
    const data = await api.adminReportsList({ page: page.value, size: size.value })
    list.value = data.records
    total.value = data.total
  } catch (e) { alert(e.message) } finally { loading.value = false }
}

async function takeDown(r) {
  if (!confirm('确认违规？内容会下架，投稿图片文件会被删除')) return
  try {
    await api.adminReportTakeDown(r.id)
    load()
  } catch (e) { alert(e.message) }
}

async function ignore(r) {
  try {
    await api.adminReportIgnore(r.id)
    load()
  } catch (e) { alert(e.message) }
}

onMounted(load)
</script>