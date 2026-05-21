import request from './request'

export function login(data) {
  return request.post('/api/login', data)
}

export function register(data) {
  return request.post('/api/register', data)
}

export function getArticles(params) {
  return request.get('/api/articles', { params })
}

export function getArticleDetail(id) {
  return request.get(`/api/articles/${id}`)
}

export function getTags() {
  return request.get('/api/tags')
}

export function getComments(articleId) {
  return request.get(`/api/articles/${articleId}/comments`)
}

export function addComment(articleId, content) {
  return request.post(`/api/articles/${articleId}/comments`, { content })
}

export function getAdminArticles(params) {
  return request.get('/api/admin/articles', { params })
}

export function createArticle(data) {
  return request.post('/api/admin/articles', data)
}

export function updateArticle(id, data) {
  return request.put(`/api/admin/articles/${id}`, data)
}

export function deleteArticle(id) {
  return request.delete(`/api/admin/articles/${id}`)
}

// Admin - comment management
export function getAdminComments(params) {
  return request.get('/api/admin/comments', { params })
}

export function deleteComment(id) {
  return request.delete(`/api/admin/comments/${id}`)
}

// Auth
export function sendCode(email) {
  return request.post('/api/send-code', { email })
}

// User profile
export function getProfile() {
  return request.get('/api/user/profile')
}

export function updateProfile(data) {
  return request.put('/api/user/profile', data)
}

export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/upload/avatar', formData)
}

export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/upload/image', formData)
}

// Search & Archive
export function searchArticles(params) {
  return request.get('/api/articles/search', { params })
}

export function getArchive() {
  return request.get('/api/articles/archive')
}

export function getAbout() {
  return request.get('/api/about')
}

// Likes
export function likeArticle(id) {
  return request.post(`/api/articles/${id}/like`)
}

export function likeComment(id) {
  return request.post(`/api/comments/${id}/like`)
}

// Admin - user management
export function getAdminUsers() {
  return request.get('/api/admin/users')
}

export function createUser(data) {
  return request.post('/api/admin/users', data)
}

export function deleteUser(id) {
  return request.delete(`/api/admin/users/${id}`)
}

// Admin - about page management
export function getAdminAbout() {
  return request.get('/api/admin/about')
}

export function updateAdminAbout(data) {
  return request.put('/api/admin/about', data)
}

export function adminUploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/admin/upload/avatar', formData)
}

export function adminUploadFavicon(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/admin/upload/favicon', formData)
}

export function adminUploadLoginBg(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/admin/upload/login-bg', formData)
}
