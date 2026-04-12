const GOOGLE_SCRIPT_ID = 'google-identity-services'
const GOOGLE_SCRIPT_SRC = 'https://accounts.google.com/gsi/client'
const CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || ''

let scriptPromise = null
let initialized = false
let activeCallback = null

function carregarScript() {
  if (window.google?.accounts?.id) {
    return Promise.resolve(window.google)
  }

  if (scriptPromise) {
    return scriptPromise
  }

  scriptPromise = new Promise((resolve, reject) => {
    const existente = document.getElementById(GOOGLE_SCRIPT_ID)

    if (existente) {
      existente.addEventListener('load', () => resolve(window.google), { once: true })
      existente.addEventListener('error', () => reject(new Error('Não foi possível carregar o Google Identity Services.')), { once: true })
      return
    }

    const script = document.createElement('script')
    script.id = GOOGLE_SCRIPT_ID
    script.src = GOOGLE_SCRIPT_SRC
    script.async = true
    script.defer = true
    script.onload = () => resolve(window.google)
    script.onerror = () => reject(new Error('Não foi possível carregar o Google Identity Services.'))
    document.head.appendChild(script)
  })

  return scriptPromise
}

function garantirClientId() {
  if (!CLIENT_ID) {
    throw new Error('Defina VITE_GOOGLE_CLIENT_ID no frontend para habilitar o login com Google.')
  }
}

export function getGoogleClientId() {
  return CLIENT_ID
}

export async function prepararGoogleIdentity({ onCredential }) {
  garantirClientId()
  await carregarScript()

  activeCallback = onCredential

  if (!initialized) {
    window.google.accounts.id.initialize({
      client_id: CLIENT_ID,
      callback: (response) => activeCallback?.(response),
      auto_select: false,
      use_fedcm_for_prompt: true
    })
    initialized = true
  }

  return window.google
}

export async function renderizarBotaoGoogle(element, onCredential) {
  if (!element) return

  const google = await prepararGoogleIdentity({ onCredential })
  element.innerHTML = ''

  google.accounts.id.renderButton(element, {
    type: 'standard',
    theme: 'outline',
    size: 'large',
    shape: 'pill',
    text: 'signin_with',
    locale: 'pt-BR',
    width: element.offsetWidth || 320
  })
}

export async function exibirOneTap(onCredential) {
  const google = await prepararGoogleIdentity({ onCredential })
  google.accounts.id.prompt()
}

export function cancelarOneTap() {
  if (window.google?.accounts?.id?.cancel) {
    window.google.accounts.id.cancel()
  }
}
