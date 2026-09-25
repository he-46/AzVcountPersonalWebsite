<template>
  <div>
    <nav class="nav-bar">
      <div class="nav-inner">
        <router-link class="nav-brand" to="/"><b>AzV</b> <span style="font-weight:400;color:var(--mute)">/admin</span></router-link>
        <div class="nav-links">
          <router-link class="nav-link" to="/admin">概览</router-link>
          <router-link class="nav-link" to="/admin/audit">待审</router-link>
          <router-link class="nav-link" to="/admin/content">内容</router-link>
          <router-link class="nav-link" to="/admin/reports">举报</router-link>
          <router-link class="nav-link" to="/admin/publish">发布</router-link>
          <router-link class="nav-link" to="/admin/resume">简历</router-link>
          <!-- 登出（hover 向下展开"返回前台"，纯 CSS 控制）-->
          <div class="theme-switch admin-exit">
            <button class="nav-link theme-btn" type="button" @click="logout">登出</button>
            <div class="theme-menu">
              <router-link class="theme-opt" to="/">前台</router-link>
            </div>
          </div>
          <!-- 主题切换（最右，和前台一致）-->
          <ThemeSwitcher />
        </div>
      </div>
    </nav>

    <slot />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { api } from '../api'
import ThemeSwitcher from '../components/ThemeSwitcher.vue'

const router = useRouter()
const auth = useAuthStore()

async function logout() {
  try { await api.logout() } catch (e) { /* 忽略 */ }
  auth.setAuthed(false)
  router.push('/login')
}
</script>
