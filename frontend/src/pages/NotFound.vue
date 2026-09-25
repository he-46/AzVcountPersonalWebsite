<template>
  <section class="container not-found-page">
    <div class="mono-kicker">// 404 — page not found</div>
    <div class="state-code">404</div>
    <h1>页面走丢了</h1>
    <p class="muted">你访问的地址不存在，或页面已经移动。</p>
    <div class="not-found-actions">
      <router-link class="btn btn-primary" to="/">返回首页</router-link>
      <router-link class="btn btn-ghost" to="/about">查看作品集</router-link>
    </div>
  </section>
</template>
