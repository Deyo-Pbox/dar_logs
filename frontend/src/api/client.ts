import axios from 'axios'
import { useAuthStore } from '@/stores/authStore'

const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let isRedirecting = false

apiClient.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && !isRedirecting) {
      isRedirecting = true
      useAuthStore.getState().logout()
      setTimeout(() => {
        window.location.hash = '#/login'
        isRedirecting = false
      }, 0)
    }
    return Promise.reject(err)
  }
)

export default apiClient
