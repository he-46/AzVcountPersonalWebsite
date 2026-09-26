import http from './http'

// 全部接口定义（与后端 API 契约对齐）
// 注：认证数据使用 JSON body，文件上传使用 FormData，其余旧接口仍保留 query 参数。
export const api = {
  // ===== 公开 =====
  listContent: (params) => http.get('/content/list', { params }),          // {page,size,type}
  getContent: (id) => http.get(`/content/${id}`),
  listComments: (id, params) => http.get(`/content/${id}/comments`, { params }),
  postComment: (data) => http.post('/comment', data),                     // JSON {contentId,text,nickname}
  submitImage: (formData) => http.post('/submit', formData),               // multipart
  like: (id) => http.post(`/like/${id}`),
  report: (data) => http.post('/report', data),                           // JSON {contentId,reason}
  listPortfolio: () => http.get('/portfolio'),
  getResume: () => http.get('/resume'),

  // ===== 认证 =====
  login: (data) => http.post('/auth/login', data),                         // {username,password}
  logout: () => http.post('/auth/logout'),

  // ===== 后台 =====
  adminAuditList: (params) => http.get('/admin/audit/list', { params }),   // {page,size,type}
  adminAuditApprove: (id) => http.post(`/admin/audit/approve/${id}`),
  adminAuditReject: (id) => http.post(`/admin/audit/reject/${id}`),
  adminStats: () => http.get('/admin/audit/stats'),
  adminContentList: (params) => http.get('/admin/content/list', { params }),
  adminContentHide: (id) => http.post(`/admin/content/${id}/hide`),
  adminContentDelete: (id) => http.post(`/admin/content/${id}/delete`),
  adminContentPin: (id, pin) => http.post(`/admin/content/${id}/pin`, null, { params: { pin } }),
  adminReportsList: (params) => http.get('/admin/reports/list', { params }),
  adminReportTakeDown: (id) => http.post(`/admin/reports/${id}/take-down`),
  adminReportIgnore: (id) => http.post(`/admin/reports/${id}/ignore`),
  adminWords: () => http.get('/admin/words/list'),
  adminWordAdd: (word) => http.post('/admin/words', null, { params: { word } }),
  adminWordDelete: (id) => http.delete(`/admin/words/${id}`),
  adminPublishPost: (formData) => http.post('/admin/publish/post', formData),
  adminPublishImage: (formData) => http.post('/admin/publish/image', formData),
  adminPortfolioList: () => http.get('/admin/portfolio/list'),
  adminPortfolioAdd: (data) => http.post('/admin/portfolio', null, { params: data }),
  // PDF 体积可达 20MB，单独放宽上传超时，避免慢速网络误报失败
  adminResumeUpload: (formData) => http.post('/admin/resume', formData, { timeout: 120000 })
}
