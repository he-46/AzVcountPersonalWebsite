<template>
  <section class="container band" style="max-width:1080px">
    <div class="mono-kicker">// audit queue — PENDING only</div>
    <h1 class="section-title">待审队列</h1>

    <!-- ① 类型 tab（全部/评论/投稿）-->
    <div class="tabs">
      <button v-for="t in tabs" :key="t.value" class="tab" :class="{ active: type === t.value }"
              @click="switchTab(t.value)">{{ t.label }}</button>
    </div>

    <!-- ② 列表 -->
    <div v-if="loading" class="muted" style="padding:24px 0">加载中…</div>
    <div v-else-if="!list.length" class="muted" style="padding:24px 0">没有待审内容 🎉</div>

    <div v-for="item in list" :key="item.id" class="card audit-item" style="margin-bottom:12px">
      <!-- 投稿带缩略图预览 -->
      <div v-if="item.thumbnailUrl" class="ph" style="width:120px;flex:none">
        <img :src="item.thumbnailUrl" alt="缩略图"
             style="width:100%;height:100%;object-fit:cover;border-radius:var(--r-card)">
      </div>
      <div class="ai-info">
        <b>{{ item.title || item.body?.slice(0, 30) }}</b>
        <p>{{ item.body }}</p>
        <div class="ai-meta">{{ item.createdAt }} · {{ item.ip }} · {{ item.authorLabel }}</div>
      </div>
      <div class="ai-actions">
        <button class="btn btn-primary btn-sm" @click="approve(item)">通过</button>
        <button class="btn btn-danger btn-sm" @click="reject(item)">驳回</button>
      </div>
    </div>

    <!-- ③ 分页 -->
    <div v-if="total > size" class="flex-between" style="margin-top:24px">
      <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
      <span class="muted" style="font-size:13px">{{ page }} / {{ totalPages }}</span>
      <button class="btn btn-ghost btn-sm" :disabled="page >= totalPages" @click="page++; load()">下一页</button>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '../../api'

const tabs = [
  { value: '', label: '全部' },
  { value: 'COMMENT', label: '评论' },
  { value: 'POST', label: '投稿' }
]
const type = ref('')
const page = ref(1)
const size = ref(10)
const list = ref([])
const total = ref(0)
const loading = ref(false)

// computed：根据 total 和 size 自动算出总页数（数据变了它自动变）
const totalPages = computed(() => Math.ceil(total.value / size.value))

async function load() {
  loading.value = true
  try {
    const data = await api.adminAuditList({ page: page.value, size: size.value, type: type.value })
    list.value = data.records
    total.value = data.total
  } catch (e) {
    alert(e.message)
  } finally {
    loading.value = false
  }
}

function switchTab(t) {
  type.value = t
  page.value = 1        // 切 tab 回到第一页
  load()
}

async function approve(item) {
  try {
    await api.adminAuditApprove(item.id)
    load()              // 重新加载列表（这条消失）
  } catch (e) {
    alert(e.message)
  }
}

async function reject(item) {
  if (!confirm(`驳回这条${item.type === 'COMMENT' ? '评论' : '投稿'}？投稿会删除文件`)) return
  try {
    await api.adminAuditReject(item.id)
    load()
  } catch (e) {
    alert(e.message)
  }
}

onMounted(load)
</script>
