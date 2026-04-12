import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const authApi = axios.create({
  baseURL: API_BASE_URL
})

const TOKEN_KEY = 'authluiz_token'
const USER_KEY = 'authluiz_user'
const EXPIRES_AT_KEY = 'authluiz_expires_at'

const PENDING_GOOGLE_LINK_KEY = 'authluiz_pending_google_link'

export async function cadastrar(dados) {
  const response = await authApi.post('/auth/cadastro', dados)
  return response.data
}

export async function login(dados) {
  const response = await authApi.post('/auth/login', dados)
  return response.data
}

export async function loginComGoogle(dados) {
  const response = await authApi.post('/auth/oauth/google', dados)
  return response.data
}

export async function buscarMinhaConta() {
  const response = await authApi.get('/auth/me', {
    headers: {
      Authorization: `Bearer ${getToken()}`
    }
  })
  return response.data
}

export async function atualizarMeuNome(dados) {
  const response = await authApi.patch('/auth/me/nome', dados, {
    headers: {
      Authorization: `Bearer ${getToken()}`
    }
  })
  return response.data
}

export async function atualizarMeuEmail(dados) {
  const response = await authApi.patch('/auth/me/email', dados, {
    headers: {
      Authorization: `Bearer ${getToken()}`
    }
  })
  return response.data
}

export async function atualizarMinhaSenha(dados) {
  const response = await authApi.patch('/auth/me/senha', dados, {
    headers: {
      Authorization: `Bearer ${getToken()}`
    }
  })
  return response.data
}

export function salvarSessao(loginResponse) {
  const expiresAt = Date.now() + loginResponse.expiresInMinutes * 60 * 1000

  localStorage.setItem(TOKEN_KEY, loginResponse.token)
  localStorage.setItem(USER_KEY, JSON.stringify({
    idUsuario: loginResponse.idUsuario,
    nome: loginResponse.nome,
    email: loginResponse.email,
    temSenhaLocal: Boolean(loginResponse.temSenhaLocal),
    temLoginGoogle: Boolean(loginResponse.temLoginGoogle)
  }))
  localStorage.setItem(EXPIRES_AT_KEY, String(expiresAt))
}

export function atualizarSessaoComConta(conta) {
  const usuarioAtual = getUsuarioLogado() || {}

  localStorage.setItem(USER_KEY, JSON.stringify({
    ...usuarioAtual,
    idUsuario: conta.idUsuario,
    nome: conta.nome,
    email: conta.email,
    temSenhaLocal: Boolean(conta.temSenhaLocal),
    temLoginGoogle: Boolean(conta.temLoginGoogle)
  }))
}

export function marcarSenhaLocalNaSessao() {
  const usuarioAtual = getUsuarioLogado()
  if (!usuarioAtual) return

  localStorage.setItem(USER_KEY, JSON.stringify({
    ...usuarioAtual,
    temSenhaLocal: true
  }))
}


export function salvarPendenciaVinculoGoogle(idToken) {
  if (!idToken) return
  sessionStorage.setItem(PENDING_GOOGLE_LINK_KEY, JSON.stringify({
    idToken,
    criadoEm: Date.now()
  }))
}

export function obterPendenciaVinculoGoogle() {
  const raw = sessionStorage.getItem(PENDING_GOOGLE_LINK_KEY)
  if (!raw) return null

  try {
    const data = JSON.parse(raw)
    if (!data?.idToken) return null
    return data
  } catch {
    return null
  }
}

export function limparPendenciaVinculoGoogle() {
  sessionStorage.removeItem(PENDING_GOOGLE_LINK_KEY)
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getUsuarioLogado() {
  const raw = localStorage.getItem(USER_KEY)
  return raw ? JSON.parse(raw) : null
}

export function getExpiresAt() {
  const raw = localStorage.getItem(EXPIRES_AT_KEY)
  return raw ? Number(raw) : null
}

export function isTokenExpired() {
  const expiresAt = getExpiresAt()
  if (!expiresAt) return true
  return Date.now() >= expiresAt
}

export function logout() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(EXPIRES_AT_KEY)

  if (window.google?.accounts?.id?.disableAutoSelect) {
    window.google.accounts.id.disableAutoSelect()
  }
}

export async function iniciarRecuperacaoSenha(dados) {
  const response = await authApi.post('/auth/recuperacao/iniciar', dados)
  return response.data
}

export async function validarTokenRecuperacao(token) {
  const response = await authApi.get('/auth/recuperacao/validar', {
    params: { token }
  })
  return response.data
}

export async function redefinirSenha(dados) {
  const response = await authApi.post('/auth/recuperacao/redefinir', dados)
  return response.data
}
