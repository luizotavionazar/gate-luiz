<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center py-5" style="background-color: #eef4ff;">
    <div class="card shadow border-0 rounded-4 text-center" style="width: 100%; max-width: 480px;">
      <div class="card-body p-4 p-md-5">

        <template v-if="sucesso">
          <i class="bi bi-check-circle-fill text-success" style="font-size: 3rem;"></i>
          <h1 class="h4 fw-bold mt-3 mb-2">E-mail confirmado!</h1>
          <p class="text-muted mb-4">O e-mail foi atualizado na sua conta.</p>
          <router-link to="/conta" class="btn btn-primary">Ir para minha conta</router-link>
        </template>

        <template v-else>
          <i class="bi bi-envelope-check text-primary" style="font-size: 3rem;"></i>
          <h1 class="h4 fw-bold mt-3 mb-1">Verifique seu e-mail</h1>
          <p class="text-muted mb-4">Digite o código de 6 dígitos enviado para <strong>{{ tipoAlteracao ? usuario?.emailPendente : usuario?.email }}</strong>.</p>

          <form @submit.prevent="confirmar">
            <div class="d-flex justify-content-center gap-2 mb-3">
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

            <div v-if="erro" class="alert alert-danger py-2 small mb-3">{{ erro }}</div>

            <div class="d-grid mb-3">
              <button type="submit" class="btn btn-primary" :disabled="confirmando || codigoCompleto.length < 6">
                {{ confirmando ? 'Confirmando...' : 'Confirmar e-mail' }}
              </button>
            </div>
          </form>

          <p class="text-muted small mb-0">
            <button class="btn btn-link btn-sm p-0 text-decoration-none" :disabled="reenviando || cooldown > 0" @click="reenviar">
              {{ reenviando ? 'Enviando...' : cooldown > 0 ? `Reenviar código (${cooldownFormatado})` : 'Reenviar código' }}
            </button>
          </p>
          <div v-if="mensagemReenvio" class="small mt-2 text-success-emphasis">{{ mensagemReenvio }}</div>
          <div v-if="erroReenvio" class="small mt-2 text-danger-emphasis">{{ erroReenvio }}</div>

          <hr class="my-3">
          <router-link to="/conta" class="text-muted small text-decoration-none">Verificar depois</router-link>
        </template>

      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { buscarMinhaConta, confirmarEmail, reenviarConfirmacaoAlteracaoEmail, reenviarVerificacao } from '../services/autenticacaoService'
import { extrairMensagemErro } from '../utils/extrairMensagemErro'

const route = useRoute()
const tipoAlteracao = route.query.tipo === 'alteracao'

const digitos = reactive(Array(6).fill(''))
const inputs = ref([])

const usuario = ref(null)

const sucesso = ref(false)
const confirmando = ref(false)
const erro = ref('')
const reenviando = ref(false)
const mensagemReenvio = ref('')
const erroReenvio = ref('')
const cooldown = ref(0)
let intervalo = null

const codigoCompleto = computed(() => digitos.join(''))
const cooldownFormatado = computed(() => {
  const m = Math.floor(cooldown.value / 60)
  const s = cooldown.value % 60
  return `${m}:${s.toString().padStart(2, '0')}`
})

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

async function confirmar() {
  if (codigoCompleto.value.length < 6) return
  erro.value = ''
  confirmando.value = true

  try {
    await confirmarEmail(codigoCompleto.value)
    sucesso.value = true
  } catch (e) {
    erro.value = extrairMensagemErro(e, 'Código inválido ou expirado. Tente novamente.')
    inputs.value[0]?.focus()
  } finally {
    confirmando.value = false
  }
}

function iniciarCooldown() {
  cooldown.value = 60
  clearInterval(intervalo)
  intervalo = setInterval(() => {
    if (cooldown.value > 0) cooldown.value--
    else clearInterval(intervalo)
  }, 1000)
}

async function reenviar() {
  mensagemReenvio.value = ''
  erroReenvio.value = ''
  reenviando.value = true

  try {
    if (tipoAlteracao) {
      await reenviarConfirmacaoAlteracaoEmail()
    } else {
      await reenviarVerificacao()
    }
    iniciarCooldown()
    mensagemReenvio.value = 'Novo código enviado com sucesso!'
  } catch (e) {
    erroReenvio.value = extrairMensagemErro(e, 'Não foi possível reenviar o código.')
  } finally {
    reenviando.value = false
  }
}

onMounted(async () => {
  iniciarCooldown()
  try {
    usuario.value = await buscarMinhaConta()
  } catch {
    // exibição do e-mail é não-crítica
  }
})
onUnmounted(() => clearInterval(intervalo))
</script>
