import { getToken } from './autenticacaoService'
import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL !== undefined
  ? import.meta.env.VITE_API_BASE_URL
  : 'http://localhost:8080'

const authApi = axios.create({ baseURL: API_BASE_URL })

function headers() {
  return { Authorization: `Bearer ${getToken()}` }
}

export async function obterStatus() {
  const response = await authApi.get('/auth/me/2fa/status', { headers: headers() })
  return response.data
}

export async function iniciarTotp() {
  const response = await authApi.post('/auth/me/2fa/totp/iniciar', {}, { headers: headers() })
  return response.data
}

export async function confirmarTotp(codigo) {
  const response = await authApi.post('/auth/me/2fa/totp/confirmar', { codigo }, { headers: headers() })
  return response.data
}

export async function desativar2fa(senha) {
  const response = await authApi.delete('/auth/me/2fa', {
    headers: headers(),
    data: { senha }
  })
  return response.data
}

export async function regerarBackupCodes(codigo) {
  const response = await authApi.post('/auth/me/2fa/backup-codes/regerar', { codigo }, { headers: headers() })
  return response.data
}

export async function adicionarIpAtual(rotulo = null) {
  const response = await authApi.post('/auth/me/ips-confiaveis', { rotulo }, { headers: headers() })
  return response.data
}

export async function listarIpsConfiaveis() {
  const response = await authApi.get('/auth/me/ips-confiaveis', { headers: headers() })
  return response.data
}

export async function removerIpConfiavel(id) {
  await authApi.delete(`/auth/me/ips-confiaveis/${id}`, { headers: headers() })
}

export async function removerTodosIps() {
  await authApi.delete('/auth/me/ips-confiaveis', { headers: headers() })
}

export async function atualizarVerificacaoExtra(ativo, senha) {
  const body = { ativo }
  if (senha !== undefined) body.senha = senha
  const response = await authApi.patch('/auth/me/2fa/verificacao-extra', body, { headers: headers() })
  return response.data
}
