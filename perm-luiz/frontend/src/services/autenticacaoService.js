const TOKEN_KEY = 'permluiz_token'
const USER_KEY = 'permluiz_user'
const EXPIRES_AT_KEY = 'permluiz_expires_at'

export function salvarSessao(loginResponse) {
  const expiresAt = Date.now() + loginResponse.expiresInMinutes * 60 * 1000
  localStorage.setItem(TOKEN_KEY, loginResponse.token)
  localStorage.setItem(USER_KEY, JSON.stringify({
    idUsuario: loginResponse.idUsuario,
    nome: loginResponse.nome,
    email: loginResponse.email
  }))
  localStorage.setItem(EXPIRES_AT_KEY, String(expiresAt))
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getUsuarioLogado() {
  const raw = localStorage.getItem(USER_KEY)
  return raw ? JSON.parse(raw) : null
}

export function isTokenExpired() {
  const expiresAt = localStorage.getItem(EXPIRES_AT_KEY)
  if (!expiresAt) return true
  return Date.now() >= Number(expiresAt)
}

export function logout() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(EXPIRES_AT_KEY)
}
