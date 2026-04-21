import axios from 'axios'
import { getToken, logout } from './autenticacaoService'

const PERMISSOES_API_URL = import.meta.env.VITE_PERM_API_URL || 'http://localhost:8081'
export const AUTH_LUIZ_URL = import.meta.env.VITE_AUTH_LUIZ_URL || 'http://localhost:8080'

const api = axios.create({ baseURL: PERMISSOES_API_URL })

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401 && getToken()) {
      logout()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
