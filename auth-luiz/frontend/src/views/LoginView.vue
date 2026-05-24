<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center py-5" style="background-color: #eef4ff;">
    <div class="card shadow border-0 rounded-4" style="width: 100%; max-width: 460px;">
      <div class="card-body p-4 p-md-5">
        <div class="text-center mb-4">
          <h1 class="h3 fw-bold mb-2">AuthLuiz</h1>
          <p class="text-muted mb-0">Entre na sua conta</p>
        </div>

        <div class="d-grid gap-2 mb-3">
          <div ref="googleButtonRef" class="google-button-host w-100"></div>
          <div v-if="googleIndisponivel" class="alert alert-warning py-2 small mb-0" role="alert">
            {{ googleIndisponivel }}
          </div>
        </div>

        <div class="position-relative text-center my-4">
          <hr class="my-0" />
          <span class="badge text-bg-light border position-absolute top-50 start-50 translate-middle px-3 py-2">ou entre com</span>
        </div>

        <div class="d-flex gap-2 mb-3">
          <button
            type="button"
            class="btn flex-fill"
            :class="canal === 'email' ? 'btn-primary' : 'btn-outline-secondary'"
            @click="selecionarCanal('email')"
          >
            <i class="bi bi-envelope me-2"></i>E-mail
          </button>
          <button
            type="button"
            class="btn flex-fill"
            :class="canal === 'telefone' ? 'btn-primary' : 'btn-outline-secondary'"
            @click="selecionarCanal('telefone')"
          >
            <i class="bi bi-phone me-2"></i>Telefone
          </button>
        </div>

        <form @submit.prevent="fazerLogin">
          <div class="mb-3">
            <label :for="canal" class="form-label">{{ canal === 'email' ? 'E-mail' : 'Telefone' }}</label>
            <input
              v-if="canal === 'email'"
              id="email"
              v-model="identificador"
              type="email"
              class="form-control"
              placeholder="seuemail@exemplo.com"
              autocomplete="email"
              required
            />
            <TelefoneInput
              v-else
              v-model="identificador"
              required
            />
          </div>

          <div class="mb-3">
            <label for="senha" class="form-label">Senha</label>
            <div class="position-relative">
              <input id="senha" v-model="senha" :type="mostrarSenha ? 'text' : 'password'" class="form-control pe-5 campo-senha" placeholder="Digite sua senha" required />
              <button type="button" class="btn btn-sm border-0 bg-transparent position-absolute top-50 end-0 translate-middle-y me-2 text-muted" @click="mostrarSenha = !mostrarSenha" :aria-label="mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'">
                <i :class="mostrarSenha ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
              </button>
            </div>
            <div class="text-left mt-3">
              <RouterLink to="/recuperar-senha" class="text-decoration-none">Esqueceu a senha? Recupere aqui</RouterLink>
            </div>
          </div>

          <div v-if="mensagem" class="alert alert-danger py-2 small" role="alert">{{ mensagem }}</div>
          <div v-if="googleMensagem" class="alert alert-danger py-2 small" role="alert">{{ googleMensagem }}</div>

          <div class="d-grid mb-3">
            <button type="submit" class="btn btn-primary" :disabled="carregando || googleCarregando">
              {{ carregando ? 'Entrando...' : 'Entrar' }}
            </button>
          </div>

          <div class="text-center mt-3">
            <RouterLink to="/cadastro" class="text-decoration-none">Não tem conta? Cadastre-se</RouterLink>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { loginComGoogle, loginComStatus, salvarSessao } from '../services/autenticacaoService'
import { cancelarOneTap, exibirOneTap, getGoogleClientId, renderizarBotaoGoogle } from '../services/googleIdentityService'
import { extrairMensagemErro } from '../utils/extrairMensagemErro'
import TelefoneInput from '../components/TelefoneInput.vue'

const router = useRouter()
const canal = ref('email')
const identificador = ref('')
const senha = ref('')
const mensagem = ref('')
const googleMensagem = ref('')
const carregando = ref(false)
const googleCarregando = ref(false)
const mostrarSenha = ref(false)
const googleButtonRef = ref(null)
const googleIndisponivel = ref('')

function selecionarCanal(novoCanal) {
  canal.value = novoCanal
  identificador.value = ''
  mensagem.value = ''
}

function redirecionarConta() {
  router.push('/conta')
}

async function fazerLogin() {
  mensagem.value = ''
  googleMensagem.value = ''

  if (!identificador.value || !senha.value) {
    mensagem.value = canal.value === 'email'
      ? 'Preencha o e-mail e a senha.'
      : 'Preencha o telefone e a senha.'
    return
  }

  carregando.value = true

  try {
    const { status, data } = await loginComStatus({ identificador: identificador.value.trim(), senha: senha.value })
    if (status === 202) {
      router.push({
        name: 'verificacaoLogin',
        query: {
          token: data.tokenPendente,
          tipo: data.tipo,
          destino: data.destinoMascarado,
          canais: (data.canaisDisponiveis ?? []).join(',')
        }
      })
    } else {
      salvarSessao(data)
      redirecionarConta()
    }
  } catch (e) {
    mensagem.value = extrairMensagemErro(e, 'Não foi possível realizar o login.')
    console.error(e)
  } finally {
    carregando.value = false
  }
}

async function autenticarComGoogle(idToken) {
  googleMensagem.value = ''
  googleCarregando.value = true

  try {
    const resposta = await loginComGoogle({ idToken })
    salvarSessao(resposta)
    redirecionarConta()
  } catch (e) {
    googleMensagem.value = extrairMensagemErro(e, 'Não foi possível entrar com Google.')
    console.error(e)
  } finally {
    googleCarregando.value = false
  }
}

async function onGoogleCredentialResponse(response) {
  if (!response?.credential) {
    googleMensagem.value = 'O Google não retornou um idToken válido.'
    return
  }

  await autenticarComGoogle(response.credential)
}

async function iniciarGoogle() {
  googleIndisponivel.value = ''

  try {
    if (!getGoogleClientId()) {
      googleIndisponivel.value = 'Defina VITE_GOOGLE_CLIENT_ID no frontend para habilitar o login com Google.'
      return
    }

    await nextTick()
    await renderizarBotaoGoogle(googleButtonRef.value, onGoogleCredentialResponse)
    await exibirOneTap(onGoogleCredentialResponse)
  } catch (e) {
    googleIndisponivel.value = e.message || 'Não foi possível carregar o login com Google.'
    console.error(e)
  }
}

onMounted(iniciarGoogle)
onBeforeUnmount(() => {
  cancelarOneTap()
})
</script>
