import { createRouter, createWebHistory } from 'vue-router'

const frontMeta = (title, description, extra = {}) => ({
  layout: 'front',
  title,
  description,
  ...extra
})

const adminMeta = (title) => ({
  layout: 'admin',
  requiresAuth: true,
  title,
  description: '个人站点后台管理页面。',
  robots: 'noindex, nofollow'
})

const routes = [
  { path: '/', name: 'home', component: () => import('../pages/Home.vue'), meta: frontMeta('首页', 'AzV 的个人站点，记录 Java 全栈学习、技术思考与项目作品。') },
  { path: '/post/:id', name: 'post-detail', component: () => import('../pages/PostDetail.vue'), meta: frontMeta('文章详情', '阅读 AzV 的技术文章与项目记录。') },
  { path: '/gallery', name: 'gallery', component: () => import('../pages/Gallery.vue'), meta: frontMeta('图片墙', '浏览 AzV 个人站点已通过审核的图片作品。') },
  { path: '/submit', name: 'submit', component: () => import('../pages/Submit.vue'), meta: frontMeta('内容投稿', '向 AzV 个人站点提交图片与内容。') },
  { path: '/about', name: 'about', component: () => import('../pages/About.vue'), meta: frontMeta('作品集与简历', '了解 AzV 的技术能力、项目经验和最新简历。') },
  { path: '/terms', name: 'terms', component: () => import('../pages/Terms.vue'), meta: frontMeta('用户协议', '个人站点的用户协议与免责声明。') },
  { path: '/login', name: 'login', component: () => import('../pages/Login.vue'), meta: frontMeta('站长登录', '个人站点站长登录。', { robots: 'noindex, nofollow' }) },

  { path: '/admin', name: 'admin-home', component: () => import('../pages/admin/AdminHome.vue'), meta: adminMeta('后台概览') },
  { path: '/admin/audit', name: 'admin-audit', component: () => import('../pages/admin/Audit.vue'), meta: adminMeta('内容审核') },
  { path: '/admin/content', name: 'admin-content', component: () => import('../pages/admin/Content.vue'), meta: adminMeta('内容管理') },
  { path: '/admin/reports', name: 'admin-reports', component: () => import('../pages/admin/Reports.vue'), meta: adminMeta('举报管理') },
  { path: '/admin/publish', name: 'admin-publish', component: () => import('../pages/admin/Publish.vue'), meta: adminMeta('内容发布') },
  { path: '/admin/resume', name: 'admin-resume', component: () => import('../pages/admin/Resume.vue'), meta: adminMeta('简历管理') },

  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('../pages/NotFound.vue'), meta: frontMeta('页面未找到', '你访问的页面不存在。', { robots: 'noindex, nofollow' }) }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash, behavior: 'smooth' }
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && localStorage.getItem('azv_authed') !== '1') {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

function setMeta(name, content) {
  let element = document.head.querySelector(`meta[name="${name}"]`)
  if (!element) {
    element = document.createElement('meta')
    element.setAttribute('name', name)
    document.head.appendChild(element)
  }
  element.setAttribute('content', content)
}

function setPropertyMeta(property, content) {
  let element = document.head.querySelector(`meta[property="${property}"]`)
  if (!element) {
    element = document.createElement('meta')
    element.setAttribute('property', property)
    document.head.appendChild(element)
  }
  element.setAttribute('content', content)
}

router.afterEach((to) => {
  const pageTitle = to.meta.title || '个人站点'
  const title = `${pageTitle} — AzV`
  const description = to.meta.description || 'AzV 的个人站点。'
  document.title = title
  setMeta('description', description)
  setMeta('robots', to.meta.robots || 'index, follow')
  setPropertyMeta('og:title', title)
  setPropertyMeta('og:description', description)
})

export default router
