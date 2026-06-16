import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearUser } from '@/store/user'

const request = axios.create({
  baseURL: '/',
  timeout: 15000
})

request.interceptors.request.use(
  config => {
    const token = sessionStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || 'Request failed')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        clearUser()
        ElMessage.error('Login expired, please login again')
        window.location.href = '/login'
      } else if (status === 403) {
        ElMessage.error('Access denied')
      } else {
        ElMessage.error(error.response.data?.message || `Error ${status}`)
      }
    } else {
      ElMessage.error('Network error')
    }
    return Promise.reject(error)
  }
)

export default request
