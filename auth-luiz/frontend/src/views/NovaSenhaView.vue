<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center" style="background-color: #eef4ff;">
    <div class="card shadow border-0 rounded-4" style="width: 100%; max-width: 480px;">
      <div class="card-body p-4 p-md-5">
        <i class="bi bi-lock text-primary d-block text-center" style="font-size: 2.5rem;"></i>
        <h1 class="h4 fw-bold text-center mt-2 mb-1">Nova senha</h1>
        <p class="text-muted text-center mb-4">Escolha uma nova senha para a sua conta.</p>

        <template v-if="sucesso">
          <div class="alert alert-success text-center">
            <i class="bi bi-check-circle me-2"></i>Senha redefinida com sucesso! Redirecionando para o login...
          </div>
        </template>

        <form v-else @submit.prevent="enviar">
          <div class="mb-3">
            <label for="novaSenha" class="form-label">Nova senha</label>
            <div class="position-relative">
              <input
                id="novaSenha"
                v-model="novaSenha"
                :type="mostrarSenha ? 'text' : 'password'"
                class="form-control pe-5"
                placeholder="Digite a nova senha"
                @focus="senhaEmFoco = true"
                @blur="senhaEmFoco = false"
                required
              />
              <button
                type="button"
                class="btn btn-sm border-0 bg-transparent position-absolute top-50 end-0 translate-middle-y me-2 text-muted"
                @click="mostrarSenha = !mostrarSenha"
                :aria-label="mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'"
              >
                <i :class="mostrarSenha ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
              </button>
            </div>
            <ul v-if="mostrarRegrasSenha" class="list-unstyled small mt-2 mb-0">
              <li :class="senhaRegras.tamanho ? 'text-success' : 'text-danger'">{{ senhaRegras.tamanho ? '✓' : '✕' }} Pelo menos 8 caracteres</li>
              <li :class="senhaRegras.maiuscula ? 'text-success' : 'text-danger'">{{ senhaRegras.maiuscula ? '✓' : '✕' }} Pelo menos 1 letra maiúscula</li>
              <li :class="senhaRegras.numero ? 'text-success' : 'text-danger'">{{ senhaRegras.numero ? '✓' : '✕' }} Pelo menos 1 número</li>
              <li :class="senhaRegras.especial ? 'text-success' : 'text-danger'">{{ senhaRegras.especial ? '✓' : '✕' }} Pelo menos 1 caractere especial</li>
            </ul>
          </div>

          <div class="mb-3">
            <label for="confirmacao" class="form-label">Confirme a nova senha</label>
            <div class="position-relative">
              <input
                id="confirmacao"
                v-model="confirmacao"
                :type="mostrarConfirmacao ? 'text' : 'password'"
                class="form-control pe-5"
                placeholder="Digite novamente a nova senha"
                required
              />
              <button
                type="button"
                class="btn btn-sm border-0 bg-transparent position-absolute top-50 end-0 translate-middle-y me-2 text-muted"
                @click="mostrarConfirmacao = !mostrarConfirmacao"
                :aria-label="mostrarConfirmacao ? 'Ocultar confirmação' : 'Mostrar confirmação'"
              >
                <i :class="mostrarConfirmacao ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
              </button>
            </div>
            <div v-if="confirmacao.length > 0" class="small mt-1" :class="senhasCoincidem ? 'text-success' : 'text-danger'">
              {{ senhasCoincidem ? '✓ As senhas coincidem' : '✕ As senhas não coincidem' }}
            </div>
          </div>

          <div v-if="erro" class="alert alert-danger py-2 small mb-3">{{ erro }}</div>

          <div class="d-grid">
            <button type="submit" class="btn btn-primary" :disabled="carregando">
              {{ carregando ? 'Salvando...' : 'Redefinir senha' }}
            </button>
          </div>

          <div class="text-center mt-3">
            <RouterLink to="/login" class="text-decoration-none">Voltar para o login</RouterLink>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { redefinirSenha } from '../services/autenticacaoService'
import { extrairMensagemErro } from '../utils/extrairMensagemErro'

const router = useRouter()

const novaSenha = ref('')
const confirmacao = ref('')
const erro = ref('')
const carregando = ref(false)
const sucesso = ref(false)
const senhaEmFoco = ref(false)
const mostrarSenha = ref(false)
const mostrarConfirmacao = ref(false)

let canal = ''
let identificador = ''
let codigo = ''

const senhaRegras = computed(() => ({
  tamanho: novaSenha.value.length >= 8,
  maiuscula: /\p{Lu}/u.test(novaSenha.value),
  numero: /\d/u.test(novaSenha.value),
  especial: /[^\p{L}\d\s]/u.test(novaSenha.value)
}))

const senhaValida = computed(() => Object.values(senhaRegras.value).every(Boolean))
const senhasCoincidem = computed(() => confirmacao.value.length > 0 && novaSenha.value === confirmacao.value)
const mostrarRegrasSenha = computed(() => senhaEmFoco.value || novaSenha.value.length > 0)

async function enviar() {
  erro.value = ''

  if (!senhaValida.value) {
    erro.value = 'A senha ainda não atende aos requisitos.'
    return
  }

  if (!senhasCoincidem.value) {
    erro.value = 'As senhas não coincidem.'
    return
  }

  carregando.value = true

  try {
    const identificadorPayload = canal === 'telefone'
      ? { telefone: identificador }
      : { email: identificador.trim() }

    await redefinirSenha({
      ...identificadorPayload,
      codigo,
      novaSenha: novaSenha.value
    })
    sucesso.value = true
    setTimeout(() => router.push('/login'), 3200)
  } catch (e) {
    erro.value = extrairMensagemErro(e, 'Não foi possível redefinir a senha.')
  } finally {
    carregando.value = false
  }
}

onMounted(() => {
  const state = window.history.state
  if (!state?.canal || !state?.identificador || !state?.codigo) {
    router.replace('/recuperar-senha')
    return
  }
  canal = state.canal
  identificador = state.identificador
  codigo = state.codigo
})
</script>
