<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center py-5" style="background-color: #eef4ff;">
    <div class="card shadow border-0 rounded-4 text-center" style="width: 100%; max-width: 480px;">
      <div class="card-body p-4 p-md-5">

        <i class="bi bi-shield-lock text-primary" style="font-size: 3rem;"></i>
        <h1 class="h4 fw-bold mt-3 mb-1">Verificação adicional</h1>

        <p class="text-muted mb-4">
          <template v-if="tipo === 'TOTP'">
            Digite o código de 6 dígitos do seu aplicativo autenticador.
          </template>
          <template v-else>
            Enviamos um código para <strong>{{ destinoMascarado }}</strong>.
          </template>
        </p>

        <div v-if="canaisDisponiveis.length > 0 && tipo !== 'TOTP' && !usandoBackup" class="mb-3">
          <button
            v-for="canal in canaisDisponiveis"
            :key="canal"
            class="btn btn-sm btn-outline-secondary me-2"
            :disabled="trocandoCanal"
            @click="trocarCanal(canal)"
          >
            {{ trocandoCanal ? 'Enviando...' : canal === 'EMAIL' ? 'Receber por e-mail' : 'Receber por telefone' }}
          </button>
          <div v-if="erroTroca" class="small text-danger mt-1">{{ erroTroca }}</div>
        </div>

        <template v-if="!usandoBackup">
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

            <div class="form-check text-start mb-2">
              <input id="confiarDispositivo" v-model="confiarEsteIp" class="form-check-input" type="checkbox">
              <label class="form-check-label small" for="confiarDispositivo">Confiar neste dispositivo</label>
            </div>

            <div v-if="confiarEsteIp" class="mb-3">
              <input
                v-model="rotuloDispositivo"
                type="text"
                class="form-control form-control-sm"
                placeholder="Rótulo (ex: Notebook pessoal)"
                maxlength="60"
              >
            </div>

            <div class="d-grid mb-3">
              <button type="submit" class="btn btn-primary" :disabled="confirmando || codigoCompleto.length < 6">
                {{ confirmando ? 'Verificando...' : 'Confirmar acesso' }}
              </button>
            </div>
          </form>

          <div v-if="tipo !== 'TOTP'" class="mb-2">
            <button class="btn btn-link btn-sm p-0 text-decoration-none" :disabled="reenviando || cooldown > 0" @click="reenviarCodigo">
              {{ reenviando ? 'Enviando...' : cooldown > 0 ? `Reenviar código (${cooldownFormatado})` : 'Reenviar código' }}
            </button>
          </div>

          <div v-if="mensagemTroca" class="small text-success-emphasis mb-2">{{ mensagemTroca }}</div>

          <div v-if="mensagemReenvio" class="small text-success-emphasis mb-2">{{ mensagemReenvio }}</div>
          <div v-if="erroReenvio" class="small text-danger-emphasis mb-2">{{ erroReenvio }}</div>

          <button v-if="tipoInicial === 'TOTP'" class="btn btn-link btn-sm p-0 text-decoration-none text-muted" @click="usandoBackup = true">
            Usar código de backup
          </button>
        </template>

        <template v-else>
          <form @submit.prevent="confirmarBackup">
            <div class="mb-3">
              <CodigoInput
                v-model="codigoBackup"
                :length="8"
                :numerico="false"
                :separadorApos="4"
                :pequeno="true"
                @submit="confirmarBackup"
              />
            </div>

            <div v-if="erroBackup" class="alert alert-danger py-2 small mb-3">{{ erroBackup }}</div>

            <div class="d-grid mb-3">
              <button type="submit" class="btn btn-primary" :disabled="confirmandoBackup || codigoBackup.length < 9">
                {{ confirmandoBackup ? 'Verificando...' : 'Usar código de backup' }}
              </button>
            </div>
          </form>

          <button class="btn btn-link btn-sm p-0 text-decoration-none text-muted" @click="usandoBackup = false">
            Voltar para código do aplicativo
          </button>
        </template>

        <hr class="my-3">
        <router-link to="/login" class="text-muted small text-decoration-none">Cancelar e voltar ao login</router-link>

      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { salvarSessao } from '../services/autenticacaoService'
import { reenviar, usarCodigoBackup, verificar } from '../services/loginPendenteService'
import { extrairMensagemErro } from '../utils/extrairMensagemErro'
import CodigoInput from '../components/CodigoInput.vue'

const route = useRoute()
const router = useRouter()

const tokenPendente = route.query.token
const tipoInicial = route.query.tipo
const canaisDisponiveis = (route.query.canais || '').split(',').filter(Boolean)

const tipo = ref(tipoInicial)
const destinoMascarado = ref(route.query.destino || null)

const digitos = reactive(Array(6).fill(''))
const inputs = ref([])
const codigoCompleto = computed(() => digitos.join(''))

const confirmando = ref(false)
const erro = ref('')

const reenviando = ref(false)
const mensagemReenvio = ref('')
const erroReenvio = ref('')
const cooldown = ref(0)
let intervalo = null

const confiarEsteIp = ref(false)
const rotuloDispositivo = ref('')

const usandoBackup = ref(false)
const codigoBackup = ref('')
const confirmandoBackup = ref(false)
const erroBackup = ref('')

const trocandoCanal = ref(false)
const erroTroca = ref('')
const mensagemTroca = ref('')

const cooldownFormatado = computed(() => {
  const m = Math.floor(cooldown.value / 60)
  const s = cooldown.value % 60
  return `${m}:${s.toString().padStart(2, '0')}`
})

function aoDigitar(index, event) {
  const valor = event.target.value.replace(/\D/g, '')
  digitos[index] = valor.slice(-1)
  if (valor && index < 5) inputs.value[index + 1]?.focus()
}

function aoApertarTecla(index, event) {
  if (event.key === 'Backspace' && !digitos[index] && index > 0) {
    inputs.value[index - 1]?.focus()
  }
}

function aoColar(event) {
  const texto = (event.clipboardData || window.clipboardData).getData('text').replace(/\D/g, '')
  if (!texto) return
  for (let i = 0; i < 6; i++) digitos[i] = texto[i] || ''
  inputs.value[Math.min(texto.length, 5)]?.focus()
}

function iniciarCooldown() {
  cooldown.value = 60
  clearInterval(intervalo)
  intervalo = setInterval(() => {
    if (cooldown.value > 0) cooldown.value--
    else clearInterval(intervalo)
  }, 1000)
}

async function confirmar() {
  if (codigoCompleto.value.length < 6) return
  erro.value = ''
  confirmando.value = true
  try {
    const resposta = await verificar(
      tokenPendente,
      codigoCompleto.value,
      confiarEsteIp.value,
      rotuloDispositivo.value.trim() || null
    )
    salvarSessao(resposta)
    router.push('/conta')
  } catch (e) {
    erro.value = extrairMensagemErro(e, 'Código inválido ou expirado.')
    for (let i = 0; i < 6; i++) digitos[i] = ''
    inputs.value[0]?.focus()
  } finally {
    confirmando.value = false
  }
}

async function trocarCanal(novoCanal) {
  erroTroca.value = ''
  mensagemTroca.value = ''
  trocandoCanal.value = true
  try {
    const resposta = await reenviar(tokenPendente, novoCanal)
    tipo.value = resposta.tipo
    destinoMascarado.value = resposta.destinoMascarado
    mensagemTroca.value = `Código enviado para ${resposta.destinoMascarado}.`
    for (let i = 0; i < 6; i++) digitos[i] = ''
    inputs.value[0]?.focus()
    iniciarCooldown()
  } catch (e) {
    erroTroca.value = extrairMensagemErro(e, 'Não foi possível trocar o canal.')
  } finally {
    trocandoCanal.value = false
  }
}

async function reenviarCodigo() {
  mensagemReenvio.value = ''
  erroReenvio.value = ''
  reenviando.value = true
  try {
    await reenviar(tokenPendente)
    iniciarCooldown()
    mensagemReenvio.value = 'Novo código enviado!'
  } catch (e) {
    erroReenvio.value = extrairMensagemErro(e, 'Não foi possível reenviar o código.')
  } finally {
    reenviando.value = false
  }
}

async function confirmarBackup() {
  if (codigoBackup.value.length < 9) return
  erroBackup.value = ''
  confirmandoBackup.value = true
  try {
    const resposta = await usarCodigoBackup(tokenPendente, codigoBackup.value)
    salvarSessao(resposta)
    router.push('/conta')
  } catch (e) {
    erroBackup.value = extrairMensagemErro(e, 'Código de backup inválido ou já utilizado.')
    codigoBackup.value = ''
  } finally {
    confirmandoBackup.value = false
  }
}

onMounted(() => {
  if (!tokenPendente) {
    router.push('/login')
    return
  }
  if (tipo.value !== 'TOTP') iniciarCooldown()
})

onUnmounted(() => clearInterval(intervalo))
</script>
