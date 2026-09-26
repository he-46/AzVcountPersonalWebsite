<template>
  <section class="container band login-page" style="max-width:420px">
    <div class="mono-kicker">// admin access</div>
    <h1 class="section-title">站长登录</h1>
    <p class="muted" style="margin-bottom:32px">本网站不开放注册，仅站长账号可登录后台。</p>

    <form class="card" @submit.prevent="doLogin">
      <div class="field">
        <label for="username">账号</label>
        <input id="username" v-model="username" class="input" placeholder="azv" autocomplete="username" required>
      </div>
      <div class="field">
        <label for="password">密码</label>
        <input id="password" v-model="password" class="input" type="password" placeholder="••••••••" autocomplete="current-password" required>
      </div>
      <p v-if="error" class="form-message form-message-error" role="alert">{{ error }}</p>
      <button class="btn btn-primary" type="submit" style="width:100%" :disabled="loading || !username.trim() || !password">
        {{ loading ? '登录中…' : '登录' }}
      </button>
    </form>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function doLogin() {
  error.value = ''
  loading.value = true
  try {
    await api.login({ username: username.value.trim(), password: password.value })
    auth.setAuthed(true)
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/admin')
      ? route.query.redirect
      : '/admin'
    router.replace(redirect)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>
