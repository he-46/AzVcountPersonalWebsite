<template>
  <section class="hero container about-hero">
    <div class="mono-kicker">// portfolio — 2027届</div>
    <h1>AzV</h1>
    <div class="serif-italic" style="margin-bottom:24px">把技术讲清楚，把系统搭结实</div>
    <p class="sub">武汉理工大学 软件工程 2027 届本科生。Java 全栈方向，擅长 Spring Boot 生态与 MySQL/Redis 原理，有完整全栈项目落地经验。</p>
    <div class="flex-between" style="margin-top:32px">
      <div class="skill-tags">
        <span v-for="s in skills" :key="s">{{ s }}</span>
      </div>
      <a
        v-if="resumeAvailable"
        class="btn btn-primary btn-sm"
        :href="resumeDownloadUrl"
      >下载简历 PDF</a>
      <span v-else class="btn btn-ghost btn-sm resume-disabled" aria-disabled="true">
        {{ resumeLoading ? '简历加载中…' : '简历待更新' }}
      </span>
    </div>
  </section>

  <section class="container band resume-section" style="padding-top:0">
    <div class="resume-heading">
      <div>
        <div class="mono-kicker">// resume — latest version</div>
        <h2 class="section-title">在线简历</h2>
      </div>
      <div v-if="resumeAvailable" class="resume-actions">
        <a class="btn btn-ghost btn-sm" :href="resumePreviewUrl" target="_blank" rel="noopener">新窗口查看</a>
        <a class="btn btn-primary btn-sm" :href="resumeDownloadUrl">下载 PDF</a>
      </div>
    </div>

    <div v-if="resumeLoading" class="card resume-state" aria-live="polite">
      <span class="resume-spinner" aria-hidden="true"></span>
      <span>正在加载简历…</span>
    </div>

    <div v-else-if="resumeError" class="card resume-state resume-state-error" role="alert">
      <div>
        <b>简历暂时无法加载</b>
        <p>{{ resumeError }}</p>
      </div>
      <button class="btn btn-ghost btn-sm" type="button" @click="loadResume">重试</button>
    </div>

    <div v-else-if="!resumeAvailable" class="card resume-empty">
      <div class="resume-file-icon" aria-hidden="true">PDF</div>
      <h3>简历正在整理中</h3>
      <p class="muted">最新版本上传后会直接在这里开放预览与下载。</p>
    </div>

    <div v-else>
      <div class="resume-meta" aria-label="简历文件信息">
        <span><b>{{ resume.fileName }}</b></span>
        <span v-if="resume.size">{{ formatFileSize(resume.size) }}</span>
        <span v-if="resume.updatedAt">更新于 {{ formatDate(resume.updatedAt) }}</span>
      </div>
      <iframe
        class="resume-frame"
        :src="resumePreviewUrl"
        title="AzV 简历 PDF 预览"
      ></iframe>
      <p class="resume-fallback hint">
        如果浏览器无法显示 PDF，可
        <a :href="resumePreviewUrl" target="_blank" rel="noopener">在新窗口查看</a>
        或 <a :href="resumeDownloadUrl">直接下载</a>。
      </p>
    </div>
  </section>

  <section class="container band portfolio-section" style="padding-top:0">
    <h2 class="section-title">项目经验</h2>
    <div v-if="projectsLoading" class="muted" aria-live="polite">加载中…</div>
    <div v-else-if="projectsError" class="card resume-state resume-state-error" role="alert">
      <span>{{ projectsError }}</span>
      <button class="btn btn-ghost btn-sm" type="button" @click="loadProjects">重试</button>
    </div>
    <div v-else class="portfolio-list">
      <div v-for="p in projects" :key="p.id" class="portfolio-item">
        <h3>
          <a
            v-if="projectLink(p)"
            :href="projectLink(p)"
            target="_blank"
            rel="noopener noreferrer"
            class="project-title-link"
          >{{ p.title }} <span aria-hidden="true">↗</span></a>
          <template v-else>{{ p.title }}</template>
        </h3>
        <p>{{ p.summary }}</p>
        <div class="tech">{{ p.techStack }}</div>
      </div>
      <p v-if="!projects.length" class="muted">项目内容正在整理中。</p>
    </div>
  </section>

  <section class="container contact-section" style="padding-bottom:96px">
    <h2 class="section-title">联系方式</h2>
    <div class="card" style="display:flex;flex-wrap:wrap;gap:24px">
      <span class="muted">邮箱：<a href="mailto:2890966805@qq.com" class="contact-link">2890966805@qq.com</a></span>
      <span class="muted">电话：<a href="tel:+8618932350380" class="contact-link">18932350380</a></span>
      <span class="muted">GitHub：<a href="https://github.com/he-46" target="_blank" rel="noopener noreferrer" class="contact-link">github.com/he-46</a></span>
      <span class="muted">所在城市：武汉</span>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '../api'

const skills = ['Java 21', 'Spring Boot 3', 'MyBatis-Plus', 'MySQL', 'Redis', 'JWT', 'Vue3', 'Linux', 'Git']

const projects = ref([])
const projectsLoading = ref(true)
const projectsError = ref('')
const resume = ref(null)
const resumeLoading = ref(true)
const resumeError = ref('')

const resumeAvailable = computed(() => Boolean(resume.value?.available && resume.value?.previewUrl))
const resumePreviewUrl = computed(() => withVersion(resume.value?.previewUrl))
const resumeDownloadUrl = computed(() => withVersion(resume.value?.downloadUrl))

function withVersion(url) {
  if (!url) return ''
  const version = resume.value?.updatedAt || resume.value?.size || ''
  if (!version) return url
  return `${url}${url.includes('?') ? '&' : '?'}v=${encodeURIComponent(version)}`
}

function projectLink(project) {
  const value = project?.link?.trim()
  if (!value) return ''
  try {
    const url = new URL(value, window.location.origin)
    if (!['http:', 'https:'].includes(url.protocol)) return ''
    return url.href
  } catch {
    return ''
  }
}

function formatFileSize(bytes) {
  const value = Number(bytes)
  if (!Number.isFinite(value) || value <= 0) return ''
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}

function formatDate(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  }).format(date)
}

async function loadProjects() {
  projectsLoading.value = true
  projectsError.value = ''
  try {
    projects.value = await api.listPortfolio()
  } catch (e) {
    projectsError.value = e.message || '项目列表加载失败'
  } finally {
    projectsLoading.value = false
  }
}

async function loadResume() {
  resumeLoading.value = true
  resumeError.value = ''
  try {
    resume.value = await api.getResume()
  } catch (e) {
    resume.value = null
    resumeError.value = e.message || '简历加载失败'
  } finally {
    resumeLoading.value = false
  }
}

onMounted(() => {
  loadProjects()
  loadResume()
})
</script>
