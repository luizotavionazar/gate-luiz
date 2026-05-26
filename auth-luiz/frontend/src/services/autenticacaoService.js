import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL !== undefined
  ? import.meta.env.VITE_API_BASE_URL
  : 'http://localhost:8080'
const PERM_LUIZ_API_URL = import.meta.env.VITE_PERM_LUIZ_API_URL || 'http://localhost:8081'

const authApi = axios.create({
  baseURL: API_BASE_URL
})

const TOKEN_KEY = 'authluiz_token'
const USER_KEY = 'authluiz_user'
const EXPIRES_AT_KEY = 'authluiz_expires_at'

export async function cadastrar(dados) {
  const response = await authApi.post('/auth/cadastro', dados)
  return response.data
}

export async function login(dados) {
  const response = await authApi.post('/auth/login', dados)
  return response.data
}

export async function loginComStatus(dados) {
  const response = await authApi.post('/auth/login', dados)
  return { status: response.status, data: response.data }
}

export async function loginComGoogle(dados) {
  const response = await authApi.post('/auth/oauth/google', dados)
  return { status: response.status, data: response.data }
}

export async function vincularGoogle(dados) {
  const response = await authApi.post('/auth/oauth/google/vincular', dados, {
    headers: {
      Authorization: `Bearer ${getToken()}`
    }
  })
  return response.data
}

export async function desvincularGoogle(dados) {
  const response = await authApi.delete('/auth/oauth/google/vincular', {
    headers: {
      Authorization: `Bearer ${getToken()}`
    },
    data: dados
  })
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

export async function atualizarMeuUsername(dados) {
  const response = await authApi.patch('/auth/me/username', dados, {
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

export async function atualizarMeuTelefone(dados) {
  const response = await authApi.patch('/auth/me/telefone', dados, {
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
    publicId: loginResponse.publicId,
    username: loginResponse.username,
    nome: loginResponse.nome,
    email: loginResponse.email,
    temSenha: Boolean(loginResponse.temSenha),
    temLoginGoogle: Boolean(loginResponse.temLoginGoogle)
  }))
  localStorage.setItem(EXPIRES_AT_KEY, String(expiresAt))
}

export function atualizarSessaoComConta(conta) {
  const usuarioAtual = getUsuarioLogado() || {}

  localStorage.setItem(USER_KEY, JSON.stringify({
    ...usuarioAtual,
    publicId: conta.publicId,
    username: conta.username,
    nome: conta.nome,
    email: conta.email,
    temSenha: Boolean(conta.temSenha),
    temLoginGoogle: Boolean(conta.temLoginGoogle)
  }))
}

export function marcarSenhaNaSessao() {
  const usuarioAtual = getUsuarioLogado()
  if (!usuarioAtual) return

  localStorage.setItem(USER_KEY, JSON.stringify({
    ...usuarioAtual,
    temSenha: true
  }))
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

export async function fazerLogout() {
  const token = getToken()
  if (token) {
    try {
      await authApi.post('/auth/logout', {}, {
        headers: { Authorization: `Bearer ${token}` }
      })
    } catch {
      // falha na API não impede a limpeza local
    }
  }
  logout()
}

export async function deletarMinhaConta(dados) {
  const response = await authApi.delete('/auth/me', {
    headers: {
      Authorization: `Bearer ${getToken()}`
    },
    data: dados
  })
  return response.data
}

export async function enviarCodigoExclusaoConta() {
  const response = await authApi.post('/auth/me/exclusao/codigo', null, {
    headers: { Authorization: `Bearer ${getToken()}` }
  })
  return response.data
}

export async function confirmarEmail(codigo) {
  const response = await authApi.post('/auth/verificacao/email/confirmar', { codigo }, {
    headers: {
      Authorization: `Bearer ${getToken()}`
    }
  })
  return response.data
}

export async function reenviarVerificacao() {
  const response = await authApi.post('/auth/verificacao/email/enviar', {}, {
    headers: {
      Authorization: `Bearer ${getToken()}`
    }
  })
  return response.data
}

export async function reenviarConfirmacaoAlteracaoEmail() {
  const response = await authApi.post('/auth/verificacao/email/enviar', {}, {
    headers: {
      Authorization: `Bearer ${getToken()}`
    }
  })
  return response.data
}

export async function confirmarTelefone(codigo) {
  const response = await authApi.post('/auth/verificacao/telefone/confirmar', { codigo }, {
    headers: {
      Authorization: `Bearer ${getToken()}`
    }
  })
  return response.data
}

export async function reenviarVerificacaoTelefone() {
  const response = await authApi.post('/auth/verificacao/telefone/enviar', {}, {
    headers: {
      Authorization: `Bearer ${getToken()}`
    }
  })
  return response.data
}

export async function verificarSePermAdmin() {
  const response = await axios.get(`${PERM_LUIZ_API_URL}/me/admin`, {
    headers: { Authorization: `Bearer ${getToken()}` }
  })
  return response.data
}

export async function iniciarRecuperacaoSenha(dados) {
  const response = await authApi.post('/auth/recuperacao/iniciar', dados)
  return response.data
}

export async function validarCodigoRecuperacao(dados) {
  const response = await authApi.post('/auth/recuperacao/validar', dados)
  return response.data
}

export async function redefinirSenha(dados) {
  const response = await authApi.post('/auth/recuperacao/redefinir', dados)
  return response.data
}
