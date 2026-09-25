import axios from 'axios'

// axios 实例：baseURL=/api（dev 走 Vite 代理，生产走 nginx 反代）
// withCredentials：跨域也要带 HttpOnly Cookie（当前同源其实不需要，但保留规范）
const http = axios.create({
  baseURL: '/api',
  withCredentials: true,
  timeout: 10000
})

// 响应拦截：统一解包 R<T>
//  code=0 → 直接返回 data（页面不用再 .data.data）
//  code!=0 → 抛出 Error(message)，页面 catch 后提示
http.interceptors.response.use(
  (resp) => {
    const r = resp.data
    if (r.code === 0) return r.data
    const error = new Error(r.message || '请求失败')
    error.code = r.code
    error.status = resp.status
    return Promise.reject(error)
  },
  (err) => {
    // HTTP 401：登录过期 → 清标记回登录页
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('azv_authed')
      if (!location.pathname.startsWith('/login')) location.href = '/login'
    }
    const msg = err.response?.data?.message || err.message || '网络错误'
    const error = new Error(msg)
    error.code = err.response?.data?.code || err.code
    error.status = err.response?.status
    return Promise.reject(error)
  }
)

export default http
