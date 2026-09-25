<template>
  <section class="container band" style="max-width:1080px">
    <div class="mono-kicker">// admin — logged in as AzV (ADMIN)</div>
    <h1 class="section-title">后台概览</h1>

    <div v-if="loading" class="muted">加载中…</div>
    <div v-else class="stat-grid">
      <div class="card stat-card">
        <div class="num" style="color:#e0b45a">{{ stats.pendingComment }}</div>
        <div class="lbl">待审评论</div>
      </div>
      <div class="card stat-card">
        <div class="num" style="color:#e0b45a">{{ stats.pendingImage }}</div>
        <div class="lbl">待审投稿</div>
      </div>
      <div class="card stat-card">
        <div class="num" style="color:#e05252">{{ stats.openReports }}</div>
        <div class="lbl">待处理举报</div>
      </div>
      <div class="card stat-card">
        <div class="num">{{ stats.total }}</div>
        <div class="lbl">内容总数</div>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div style="display:flex;gap:12px;margin-top:24px">
      <router-link class="btn btn-primary" to="/admin/audit">去审核 →</router-link>
      <router-link class="btn btn-ghost" to="/admin/reports">处理举报 →</router-link>
      <router-link class="btn btn-ghost" to="/admin/publish">发布内容 →</router-link>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../../api'

const stats = ref({})
const loading = ref(true)

onMounted(async () => {
  try {
    stats.value = await api.adminStats()   // 返回 {pendingComment, pendingImage, openReports, total}
  } catch (e) {
    alert(e.message)
  } finally {
    loading.value = false
  }
})
</script>
