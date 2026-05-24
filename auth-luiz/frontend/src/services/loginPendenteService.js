import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL !== undefined
  ? import.meta.env.VITE_API_BASE_URL
  : 'http://localhost:8080'

const authApi = axios.create({ baseURL: API_BASE_URL })

export async function verificar(tokenPendente, codigo, confiarEsteIp, rotuloDispositivo = null) {
  const response = await authApi.post('/auth/login/verificar', {
    tokenPendente,
    codigo,
    confiarEsteIp,
    rotuloDispositivo
  })
  return response.data
}

export async function reenviar(tokenPendente, canal = null) {
  const response = await authApi.post('/auth/login/reenviar', { tokenPendente, canal })
  return response.data
}

export async function usarCodigoBackup(tokenPendente, codigoBackup) {
  const response = await authApi.post('/auth/login/codigo-backup', {
    tokenPendente,
    codigoBackup
  })
  return response.data
}
