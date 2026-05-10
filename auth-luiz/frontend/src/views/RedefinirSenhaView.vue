<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center" style="background-color: #eef4ff;">
    <div class="card shadow border-0 rounded-4" style="width: 100%; max-width: 480px;">
      <div class="card-body p-4 p-md-5">
        <i class="bi bi-key text-primary d-block text-center" style="font-size: 2.5rem;"></i>
        <h1 class="h4 fw-bold text-center mt-2 mb-1">Redefinir senha</h1>
        <p class="text-muted text-center mb-4">Digite o código de 6 dígitos enviado para o seu e-mail e escolha uma nova senha.</p>

        <template v-if="sucesso">
          <div class="alert alert-success text-center">
            <i class="bi bi-check-circle me-2"></i>Senha redefinida com sucesso!
          </div>
          <div class="text-center mt-3">
            <RouterLink to="/login" class="btn btn-primary">Ir para o login</RouterLink>
          </div>
        </template>

        <form v-else @submit.prevent="enviar">
          <div class="mb-3">
            <label for="email" class="form-label">E-mail</label>
            <input id="email" v-model="email" type="email" class="form-control" placeholder="seuemail@exemplo.com" required />
          </div>

          <div class="mb-3">
            <label class="form-label">Código de verificação</label>
            <div class="d-flex justify-content-center gap-2">
              <input
                v-for="(_, i) in digitos"
                :key="i"
                :ref="el => { if (el) inputs[i] = el }"
                v-model="digitos[i]"
                type="text"
                inputmode="numeric"
                maxlength="1"
                class="form-control text-center fw-bold fs-4 p-2"
                style="width: 48px; height: 56px;"
                @input="aoDigitar(i, $event)"
                @keydown="aoApertarTecla(i, $event)"
                @paste.prevent="aoColar($event)"
              />
            </div>
          </div>

          <div class="mb-3">
            <label for="novaSenha" class="form-label">Nova senha</label>
            <div class="position-relative">
              <input
                id="novaSenha"
                v-model="novaSenha"
                :type="mostrarSenha ? 'text' : 'password'"
                class="form-control pe-5 campo-senha"
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
                class="form-control pe-5 campo-senha"
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
            <button type="submit" class="btn btn-primary" :disabled="carregando || codigoCompleto.length < 6">
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { redefinirSenha } from '../services/autenticacaoService'
import { extrairMensagemErro } from '../utils/extrairMensagemErro'

const route = useRoute()
const router = useRouter()

const email = ref('')
const digitos = reactive(Array(6).fill(''))
const inputs = ref([])
const novaSenha = ref('')
const confirmacao = ref('')
const erro = ref('')
const carregando = ref(false)
const sucesso = ref(false)
const senhaEmFoco = ref(false)
const mostrarSenha = ref(false)
const mostrarConfirmacao = ref(false)

const codigoCompleto = computed(() => digitos.join(''))

const senhaRegras = computed(() => ({
  tamanho: novaSenha.value.length >= 8,
  maiuscula: /\p{Lu}/u.test(novaSenha.value),
  numero: /\d/u.test(novaSenha.value),
  especial: /[^\p{L}\d\s]/u.test(novaSenha.value)
}))

const senhaValida = computed(() => Object.values(senhaRegras.value).every(Boolean))
const senhasCoincidem = computed(() => confirmacao.value.length > 0 && novaSenha.value === confirmacao.value)
const mostrarRegrasSenha = computed(() => senhaEmFoco.value || novaSenha.value.length > 0)

function aoDigitar(index, event) {
  const valor = event.target.value.replace(/\D/g, '')
  digitos[index] = valor.slice(-1)
  if (valor && index < 5) {
    inputs.value[index + 1]?.focus()
  }
}

function aoApertarTecla(index, event) {
  if (event.key === 'Backspace' && !digitos[index] && index > 0) {
    inputs.value[index - 1]?.focus()
  }
}

function aoColar(event) {
  const texto = (event.clipboardData || window.clipboardData).getData('text').replace(/\D/g, '')
  if (!texto) return
  for (let i = 0; i < 6; i++) {
    digitos[i] = texto[i] || ''
  }
  inputs.value[Math.min(texto.length, 5)]?.focus()
}

async function enviar() {
  erro.value = ''

  if (codigoCompleto.value.length < 6) {
    erro.value = 'Preencha o código de 6 dígitos.'
    return
  }

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
    await redefinirSenha({
      email: email.value.trim(),
      codigo: codigoCompleto.value,
      novaSenha: novaSenha.value
    })
    sucesso.value = true
  } catch (e) {
    erro.value = extrairMensagemErro(e, 'Não foi possível redefinir a senha.')
    digitos.fill('')
    inputs.value[0]?.focus()
  } finally {
    carregando.value = false
  }
}

onMounted(() => {
  email.value = route.query.email || ''
})
</script>
