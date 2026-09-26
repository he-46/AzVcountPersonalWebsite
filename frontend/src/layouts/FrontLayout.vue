<template>
  <div class="front-shell">
    <nav class="nav-bar">
      <div class="nav-inner">
        <router-link class="nav-brand" to="/"><b>AzV</b></router-link>
        <div class="nav-links">
          <router-link class="nav-link" to="/">文章</router-link>
          <router-link class="nav-link" to="/gallery">图库</router-link>
          <router-link class="nav-link" to="/about">作品集</router-link>
          <router-link class="nav-link" to="/submit">投稿</router-link>
          <span class="nav-sep"></span>
          <router-link v-if="auth.authed" class="nav-link" to="/admin">后台</router-link>
          <router-link v-else class="nav-link" to="/login">登录</router-link>
          <ThemeSwitcher />
        </div>
      </div>
    </nav>

    <slot />

    <footer class="footer">
      <div class="footer-inner">
        <div>© 2026 AzV · 求职作品集与个人博客</div>
        <div class="icp"><a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener" style="text-decoration:underline;text-underline-offset:2px">苏ICP备2026056580号-1</a> ·
          <router-link to="/terms" style="text-decoration:underline;text-underline-offset:2px">用户协议与免责声明</router-link></div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { useAuthStore } from '../stores/auth'
import ThemeSwitcher from '../components/ThemeSwitcher.vue'

const auth = useAuthStore()
</script>
