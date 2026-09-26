<template>
  <section class="container band resume-admin">
    <div class="mono-kicker">// resume — replace &amp; publish</div>
    <h1 class="section-title">简历管理</h1>
    <p class="muted resume-admin-intro">上传新的 PDF 后会立即替换公开页面中的版本，访客无需刷新部署即可看到最新简历。</p>

    <div class="resume-admin-grid">
      <div>
        <section class="card" aria-labelledby="current-resume-title">
          <div class="resume-card-title">
            <h2 id="current-resume-title">当前文件</h2>
            <span v-if="resume?.available" class="badge badge-ok">公开中</span>
            <span v-else-if="!loading && !loadError" class="badge">未上传</span>
          </div>

          <div v-if="loading" class="resume-state" aria-live="polite">
            <span class="resume-spinner" aria-hidden="true"></span>
            <span>正在读取文件状态…</span>
          </div>

          <div v-else-if="loadError" class="resume-inline-error" role="alert">
            <p>{{ loadError }}</p>
            <button class="btn btn-ghost btn-sm" type="button" @click="loadResume">重试</button>
          </div>

          <div v-else-if="resume?.available" class="resume-current-file">
            <div class="resume-file-icon" aria-hidden="true">PDF</div>
            <div class="resume-current-info">
              <b>{{ resume.fileName }}</b>
              <span>{{ formatFileSize(resume.size) }}</span>
              <span v-if="resume.updatedAt">更新于 {{ formatDate(resume.updatedAt) }}</span>
            </div>
            <div class="resume-actions">
              <a class="btn btn-ghost btn-sm" :href="serverPreviewUrl" target="_blank" rel="noopener">查看</a>
              <a class="btn btn-ghost btn-sm" :href="serverDownloadUrl">下载</a>
            </div>
          </div>

          <div v-else class="resume-admin-empty">
            <p>尚未上传简历，选择一个 PDF 即可发布。</p>
          </div>
        </section>

        <form class="card resume-upload-card" @submit.prevent="uploadResume">
          <div class="resume-card-title">
            <h2>{{ resume?.available ? '替换简历' : '上传简历' }}</h2>
          </div>

          <div class="field">
            <label for="resume-file">选择 PDF 文件</label>
            <input
              id="resume-file"
              ref="fileInput"
              class="input resume-file-input"
              type="file"
              accept="application/pdf,.pdf"
              :disabled="uploading"
              @change="onFileChange"
            >
            <p class="hint">仅支持 PDF，文件大小不超过 20 MB。上传成功后旧版本会被替换。</p>
          </div>

          <div v-if="selectedFile" class="resume-selected-file">
            <div class="resume-file-icon" aria-hidden="true">PDF</div>
            <div>
              <b>{{ selectedFile.name }}</b>
              <span>{{ formatFileSize(selectedFile.size) }} · 等待上传</span>
            </div>
            <button class="btn btn-ghost btn-sm" type="button" :disabled="uploading" @click="clearSelection">移除</button>
          </div>

          <p v-if="formError" class="resume-message resume-message-error" role="alert">{{ formError }}</p>
          <p v-if="successMessage" class="resume-message resume-message-success" role="status">{{ successMessage }}</p>

          <div class="resume-submit-row">
            <span class="hint">确认前可在下方预览所选文件。</span>
            <button class="btn btn-primary" type="submit" :disabled="!selectedFile || uploading">
              {{ uploading ? '正在替换…' : (resume?.available ? '确认替换' : '上传并公开') }}
            </button>
          </div>
        </form>
      </div>

      <aside class="resume-admin-note">
        <div class="mono-kicker">// publish notes</div>
        <h2>替换说明</h2>
        <ol>
          <li>选择 PDF 后先核对右侧或下方预览。</li>
          <li>点击确认后，新文件立即对访客公开。</li>
          <li>文件名、大小和更新时间会同步展示。</li>
        </ol>
        <p>建议导出 PDF 时嵌入字体，并检查链接、电话号码与项目时间是否正确。</p>
      </aside>
    </div>

    <section v-if="previewUrl" class="resume-admin-preview" aria-labelledby="resume-preview-title">
      <div class="resume-heading">
        <div>
          <div class="mono-kicker">// preview</div>
          <h2 id="resume-preview-title" class="section-title">
            {{ selectedFile ? '待上传预览' : '当前简历预览' }}
          </h2>
        </div>
        <a class="btn btn-ghost btn-sm" :href="previewUrl" target="_blank" rel="noopener">新窗口查看</a>
      </div>
      <iframe
        :key="previewUrl"
        class="resume-frame"
        :src="previewUrl"
        :title="selectedFile ? '待上传简历 PDF 预览' : '当前简历 PDF 预览'"
      ></iframe>
      <p class="resume-fallback hint">浏览器未显示 PDF？可点击“新窗口查看”。</p>
    </section>

    <section class="portfolio-admin" aria-labelledby="portfolio-admin-title">
      <div class="resume-heading">
        <div>
          <div class="mono-kicker">// portfolio — project experience</div>
          <h2 id="portfolio-admin-title" class="section-title">项目经历管理</h2>
          <p class="muted">已公开的项目会按排序数字从小到大显示在“关于”页面。</p>
        </div>
        <button class="btn btn-ghost btn-sm" type="button" :disabled="projectsLoading || projectSaving" @click="loadProjects">刷新列表</button>
      </div>

      <p v-if="projectsError" class="resume-message resume-message-error" role="alert">{{ projectsError }}</p>
      <p v-if="projectSuccess" class="resume-message resume-message-success" role="status">{{ projectSuccess }}</p>

      <div class="portfolio-admin-grid">
        <div class="card portfolio-admin-list">
          <h3>现有项目</h3>
          <p v-if="projectsLoading" class="muted" aria-live="polite">正在加载项目…</p>
          <p v-else-if="!projects.length" class="muted">还没有项目经历，请在右侧添加。</p>
          <div v-for="project in projects" :key="project.id" class="portfolio-admin-item">
            <div class="portfolio-admin-item-head">
              <strong>{{ project.title }}</strong>
              <span :class="['badge', project.status === 'APPROVED' ? 'badge-ok' : '']">{{ project.status === 'APPROVED' ? '已公开' : '未公开' }}</span>
            </div>
            <p>{{ project.summary || '暂无简介' }}</p>
            <div class="portfolio-admin-meta">排序 {{ project.sortOrder ?? 0 }} · {{ project.techStack || '未填技术栈' }}</div>
            <div class="portfolio-admin-actions">
              <button class="btn btn-ghost btn-sm" type="button" :disabled="projectSaving" @click="editProject(project)">编辑</button>
              <button class="btn btn-ghost btn-sm" type="button" :disabled="projectSaving" @click="deleteProject(project)">删除</button>
            </div>
          </div>
        </div>

        <form class="card portfolio-admin-form" @submit.prevent="saveProject">
          <div class="resume-card-title">
            <h3>{{ editingProjectId ? '编辑项目' : '新增项目' }}</h3>
            <button v-if="editingProjectId" class="btn btn-ghost btn-sm" type="button" :disabled="projectSaving" @click="resetProjectForm">取消编辑</button>
          </div>
          <div class="field">
            <label for="project-title">项目名称 *</label>
            <input id="project-title" v-model.trim="projectForm.title" class="input" required maxlength="120" :disabled="projectSaving">
          </div>
          <div class="field">
            <label for="project-summary">项目简介</label>
            <textarea id="project-summary" v-model.trim="projectForm.summary" class="textarea" maxlength="500" :disabled="projectSaving"></textarea>
          </div>
          <div class="field">
            <label for="project-tech">技术栈</label>
            <input id="project-tech" v-model.trim="projectForm.techStack" class="input" maxlength="255" placeholder="例如：Vue 3、Spring Boot、MySQL" :disabled="projectSaving">
          </div>
          <div class="field">
            <label for="project-link">项目链接</label>
            <input id="project-link" v-model.trim="projectForm.link" class="input" type="url" maxlength="255" placeholder="https://example.com" :disabled="projectSaving">
          </div>
          <div class="portfolio-admin-form-row">
            <div class="field">
              <label for="project-order">排序数字</label>
              <input id="project-order" v-model.number="projectForm.sortOrder" class="input" type="number" step="1" min="0" max="9999" required :disabled="projectSaving">
            </div>
            <div class="field">
              <label for="project-status">展示状态</label>
              <select id="project-status" v-model="projectForm.status" class="input" :disabled="projectSaving">
                <option value="APPROVED">公开</option>
                <option value="HIDDEN">隐藏</option>
              </select>
            </div>
          </div>
          <p v-if="projectFormError" class="resume-message resume-message-error" role="alert">{{ projectFormError }}</p>
          <button class="btn btn-primary" type="submit" :disabled="projectSaving">{{ projectSaving ? '正在保存…' : (editingProjectId ? '保存修改' : '添加项目') }}</button>
        </form>
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { api } from '../../api'

const MAX_FILE_SIZE = 20 * 1024 * 1024

const resume = ref(null)
const loading = ref(true)
const loadError = ref('')
const selectedFile = ref(null)
const selectedPreviewUrl = ref('')
const fileInput = ref(null)
const uploading = ref(false)
const formError = ref('')
const successMessage = ref('')
const projects = ref([])
const projectsLoading = ref(true)
const projectsError = ref('')
const projectSuccess = ref('')
const projectFormError = ref('')
const projectSaving = ref(false)
const editingProjectId = ref(null)
const emptyProjectForm = () => ({ title: '', summary: '', techStack: '', link: '', sortOrder: 0, status: 'APPROVED' })
const projectForm = ref(emptyProjectForm())

const serverPreviewUrl = computed(() => withVersion(resume.value?.previewUrl))
const serverDownloadUrl = computed(() => withVersion(resume.value?.downloadUrl))
const previewUrl = computed(() => selectedPreviewUrl.value || (resume.value?.available ? serverPreviewUrl.value : ''))

function withVersion(url) {
  if (!url) return ''
  const version = resume.value?.updatedAt || resume.value?.size || ''
  if (!version) return url
  return `${url}${url.includes('?') ? '&' : '?'}v=${encodeURIComponent(version)}`
}

function formatFileSize(bytes) {
  const value = Number(bytes)
  if (!Number.isFinite(value) || value <= 0) return '未知大小'
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

async function loadResume() {
  loading.value = true
  loadError.value = ''
  try {
    resume.value = await api.getResume()
  } catch (e) {
    resume.value = null
    loadError.value = e.message || '读取简历状态失败'
  } finally {
    loading.value = false
  }
}

function revokeSelectedPreview() {
  if (selectedPreviewUrl.value) URL.revokeObjectURL(selectedPreviewUrl.value)
  selectedPreviewUrl.value = ''
}

function resetInput() {
  if (fileInput.value) fileInput.value.value = ''
}

function clearSelection() {
  revokeSelectedPreview()
  selectedFile.value = null
  formError.value = ''
  resetInput()
}

function onFileChange(event) {
  revokeSelectedPreview()
  selectedFile.value = null
  formError.value = ''
  successMessage.value = ''

  const file = event.target.files?.[0]
  if (!file) return

  const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf')
  if (!isPdf) {
    formError.value = '请选择 PDF 文件。'
    resetInput()
    return
  }
  if (file.size > MAX_FILE_SIZE) {
    formError.value = 'PDF 文件不能超过 20 MB。'
    resetInput()
    return
  }
  if (file.size === 0) {
    formError.value = '所选文件为空，请重新选择。'
    resetInput()
    return
  }

  selectedFile.value = file
  selectedPreviewUrl.value = URL.createObjectURL(file)
}

async function uploadResume() {
  if (!selectedFile.value || uploading.value) return

  const uploadedName = selectedFile.value.name
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  uploading.value = true
  formError.value = ''
  successMessage.value = ''

  try {
    resume.value = await api.adminResumeUpload(formData)
    revokeSelectedPreview()
    selectedFile.value = null
    resetInput()
    successMessage.value = `“${resume.value?.fileName || uploadedName}”已发布，公开页面现已使用新版本。`
  } catch (e) {
    formError.value = e.message || '简历上传失败，请稍后重试。'
  } finally {
    uploading.value = false
  }
}

async function loadProjects() {
  projectsLoading.value = true
  projectsError.value = ''
  try {
    projects.value = await api.adminPortfolioList()
  } catch (e) {
    projectsError.value = e.message || '读取项目经历失败'
  } finally {
    projectsLoading.value = false
  }
}

function resetProjectForm() {
  editingProjectId.value = null
  projectForm.value = emptyProjectForm()
  projectFormError.value = ''
}

function editProject(project) {
  editingProjectId.value = project.id
  projectForm.value = {
    title: project.title || '', summary: project.summary || '',
    techStack: project.techStack || '', link: project.link || '',
    sortOrder: project.sortOrder ?? 0, status: project.status === 'APPROVED' ? 'APPROVED' : 'HIDDEN'
  }
  projectFormError.value = ''
  projectSuccess.value = ''
}

async function saveProject() {
  if (projectSaving.value) return
  projectFormError.value = ''
  projectSuccess.value = ''
  const data = { ...projectForm.value, title: projectForm.value.title.trim() }
  if (!data.title) {
    projectFormError.value = '请填写项目名称。'
    return
  }
  if (!Number.isInteger(data.sortOrder) || data.sortOrder < 0 || data.sortOrder > 9999) {
    projectFormError.value = '排序数字须为 0 到 9999 的整数。'
    return
  }
  if (data.link && !/^https?:\/\//i.test(data.link)) {
    projectFormError.value = '项目链接须以 http:// 或 https:// 开头。'
    return
  }
  projectSaving.value = true
  try {
    if (editingProjectId.value) {
      await api.adminPortfolioUpdate(editingProjectId.value, data)
      projectSuccess.value = '项目经历已更新。'
    } else {
      await api.adminPortfolioAdd(data)
      projectSuccess.value = '项目经历已添加。'
    }
    resetProjectForm()
    await loadProjects()
  } catch (e) {
    projectFormError.value = e.message || '保存项目经历失败'
  } finally {
    projectSaving.value = false
  }
}

async function deleteProject(project) {
  if (projectSaving.value || !window.confirm(`确定永久删除“${project.title}”吗？此操作无法撤销。`)) return
  projectSaving.value = true
  projectsError.value = ''
  projectSuccess.value = ''
  try {
    await api.adminPortfolioDelete(project.id)
    if (editingProjectId.value === project.id) resetProjectForm()
    projectSuccess.value = '项目经历已删除。'
    await loadProjects()
  } catch (e) {
    projectsError.value = e.message || '删除项目经历失败'
  } finally {
    projectSaving.value = false
  }
}

onMounted(() => {
  loadResume()
  loadProjects()
})
onBeforeUnmount(revokeSelectedPreview)
</script>

<style scoped>
.portfolio-admin { margin-top: 64px; }
.portfolio-admin-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(300px, 420px); gap: 24px; align-items: start; }
.portfolio-admin-list h3, .portfolio-admin-form h3 { margin-bottom: 20px; font-size: 18px; }
.portfolio-admin-item { padding: 18px 0; border-top: 1px solid var(--hairline); }
.portfolio-admin-item-head, .portfolio-admin-actions { display: flex; align-items: center; gap: 12px; }
.portfolio-admin-item-head { justify-content: space-between; }
.portfolio-admin-item p { margin: 8px 0; color: var(--body); }
.portfolio-admin-meta { color: var(--mute); font-size: 12px; }
.portfolio-admin-actions { margin-top: 12px; }
.portfolio-admin-form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.portfolio-admin-form .input, .portfolio-admin-form .textarea { width: 100%; }
@media (max-width: 800px) { .portfolio-admin-grid { grid-template-columns: 1fr; } }
</style>
