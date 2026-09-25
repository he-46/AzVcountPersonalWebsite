<template>
  <section class="container band" style="max-width:1080px">
    <div class="mono-kicker">// content — all statuses</div>
    <h1 class="section-title">内容管理</h1>

    <!-- ① 搜索 + 筛选（共同作用于下面的表格）-->
    <div style="display:flex;gap:12px;margin-bottom:16px">
      <input v-model="keyword" class="input" style="max-width:280px"
             placeholder="搜索标题 / 内容 / 发布者" @keyup.enter="doSearch">
      <button class="btn btn-ghost" @click="doSearch">搜索</button>
      <select v-model="type" class="input" style="max-width:150px" @change="resetAndLoad">
        <option value="">全部类型</option>
        <option value="POST">话题</option>
        <option value="IMAGE">图片</option>
        <option value="COMMENT">评论</option>
      </select>
      <select v-model="status" class="input" style="max-width:150px" @change="resetAndLoad">
        <option value="">全部状态</option>
        <option value="PENDING">待审</option>
        <option value="APPROVED">已发布</option>
        <option value="REJECTED">已驳回</option>
        <option value="DELETED">已删除</option>
      </select>
    </div>

    <!-- ② 搜索状态提示 -->
    <p v-if="keyword" class="hint" style="margin:0 0 16px">
      正在搜索"{{ keyword }}"的结果（{{ total }} 条）
      <a href="#" style="color:var(--primary)" @click.prevent="clearSearch">清除搜索</a>
    </p>

    <div v-if="loading" class="muted">加载中…</div>
    <table v-else class="table">
      <thead>
        <tr><th>类型</th><th>标题</th><th>来源</th><th>状态</th><th>时间</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="c in list" :key="c.id">
          <td><span class="badge">{{ c.type }}</span></td>
          <td><b>{{ c.title || c.body?.slice(0, 30) }}</b></td>
          <td>{{ c.source }}</td>
          <td><span class="badge" :class="badgeClass(c.status)">{{ c.status }}</span></td>
          <td style="font-size:12px">{{ c.createdAt?.slice(0, 16) }}</td>
          <td>
            <router-link class="btn btn-ghost btn-sm" :to="`/post/${c.id}`">查看</router-link>
            <button v-if="c.type !== 'COMMENT'" class="btn btn-ghost btn-sm" @click="pin(c)">
              {{ c.sortWeight ? '取消置顶' : '置顶' }}
            </button>
            <button class="btn btn-ghost btn-sm" @click="hide(c)">隐藏</button>
            <button class="btn btn-danger btn-sm" @click="remove(c)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
    <p v-if="!loading && !list.length" class="muted">没有内容</p>

    <!-- 分页 -->
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

const type = ref('')
const status = ref('')
const keyword = ref('')          // 搜索词（表格查询条件之一）
const page = ref(1)
const size = ref(10)
const list = ref([])
const total = ref(0)
const loading = ref(false)

const totalPages = computed(() => Math.ceil(total.value / size.value))

async function load() {
  loading.value = true
  try {
    const data = await api.adminContentList({
      page: page.value, size: size.value,
      type: type.value, status: status.value,
      keyword: keyword.value             // 搜索和筛选一起进查询
    })
    list.value = data.records
    total.value = data.total
  } catch (e) {
    alert(e.message)
  } finally {
    loading.value = false
  }
}

function resetAndLoad() { page.value = 1; load() }

function doSearch() { page.value = 1; load() }        // 搜索 = 带 keyword 重新加载表格

function clearSearch() { keyword.value = ''; resetAndLoad() }

function badgeClass(s) {
  return { APPROVED: 'badge-ok', PENDING: 'badge-pending' }[s] || ''
}

async function pin(c) {
  try { await api.adminContentPin(c.id, !c.sortWeight); load() }
  catch (e) { alert(e.message) }
}

async function hide(c) {
  try { await api.adminContentHide(c.id); load() }
  catch (e) { alert(e.message) }
}

async function remove(c) {
  if (!confirm('确定删除？投稿的图片文件会被物理删除')) return
  try { await api.adminContentDelete(c.id); load() }
  catch (e) { alert(e.message) }
}

onMounted(load)
</script>