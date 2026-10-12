import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：添加 JWT
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：处理 401
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api

// 认证 API
export const authApi = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  me: () => api.get('/auth/me')
}

// 电影 API
export const movieApi = {
  list: (params) => api.get('/movies', { params }),
  detail: (id) => api.get(`/movies/${id}`),
  analyze: (id) => api.get(`/movies/${id}/analyze`),
  keywords: (id) => api.get(`/movies/${id}/keywords`)
}

// 评论 API
export const commentApi = {
  submit: (data) => api.post('/comments/analyze', data),
  analyzeAsync: (data) => api.post('/comments/analyze-async', data),
  pollResult: (taskId) => api.get(`/comments/result/${taskId}`)
}

// 收藏 API
export const favoriteApi = {
  add: (movieId) => api.post(`/favorites/${movieId}`),
  remove: (movieId) => api.delete(`/favorites/${movieId}`),
  list: () => api.get('/favorites'),
  status: (movieId) => api.get(`/favorites/${movieId}/status`)
}

// 管理 API
export const adminApi = {
  setEnabled: (userId, enabled) => api.put(`/admin/users/${userId}/enabled`, null, { params: { enabled } }),
  resetPassword: (userId, newPassword) => api.put(`/admin/users/${userId}/password`, { newPassword }),
  crawlerTasks: () => api.get('/admin/crawler/tasks'),
  triggerCrawler: (data) => api.post('/admin/crawler/trigger', data),
  rerunAll: () => api.post('/admin/analysis/rerun-all')
}
