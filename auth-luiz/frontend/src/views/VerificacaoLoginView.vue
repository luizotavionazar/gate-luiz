<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center py-5" style="background-color: #eef4ff;">
    <div class="card shadow border-0 rounded-4 text-center" style="width: 100%; max-width: 480px;">
      <div class="card-body p-4 p-md-5">

        <!-- Tela de seleção de canal -->
        <template v-if="mostraSelecaoCanal">
          <i class="bi bi-shield-lock text-primary" style="font-size: 3rem;"></i>
          <h1 class="h4 fw-bold mt-3 mb-1">Verificação adicional</h1>
          <p class="text-muted mb-4">Escolha como quer receber seu código de verificação.</p>

          <div class="d-flex flex-column gap-3 mb-3">
            <button
              v-for="canal in canaisDisponiveis"
              :key="canal"
              class="btn btn-outline-secondary text-start d-flex align-items-center gap-3 p-3 rounded-3"
              :disabled="selecionandoCanal"
              @click="selecionarCanal(canal)"
            >
              <i :class="iconePorCanal(canal)" style="font-size: 1.5rem; width: 1.75rem; flex-shrink: 0;"></i>
              <div>
                <div class="fw-semibold">{{ labelPorCanal(canal) }}</div>
                <div class="small text-muted">{{ descricaoPorCanal(canal) }}</div>
              </div>
            </button>
          </div>

          <div v-if="erroSelecao" class="alert alert-danger py-2 small mb-0">{{ erroSelecao }}</div>
        </template>

        <!-- Tela de preenchimento do código -->
        <template v-else>
          <div v-if="voltarSelecaoDisponivel" class="text-start mb-3">
            <button class="btn btn-sm btn-link p-0 text-muted text-decoration-none" @click="voltarSelecao">
              <i class="bi bi-arrow-left me-1"></i>Escolher outro canal
            </button>
          </div>

          <i class="bi bi-shield-lock text-primary" style="font-size: 3rem;"></i>
          <h1 class="h4 fw-bold mt-3 mb-1">Verificação adicional</h1>

          <p class="text-muted mb-4">
            <template v-if="usandoBackup">
              Digite um dos seus códigos de backup de 8 dígitos.
            </template>
            <template v-else-if="tipo === 'TOTP'">
              Digite o código de 6 dígitos do seu aplicativo autenticador.
            </template>
            <template v-else>
              Enviamos um código para <strong>{{ destinoMascarado }}</strong>.
            </template>
          </p>

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
        </template>

        <hr class="my-3">
        <router-link to="/login" class="text-muted small text-decoration-none">Cancelar e voltar ao login</router-link>

      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
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

const mostraSelecaoCanal = ref(tipoInicial === 'AGUARDANDO_CANAL')
const voltarSelecaoDisponivel = ref(tipoInicial === 'AGUARDANDO_CANAL')
const selecionandoCanal = ref(false)
const erroSelecao = ref('')

const cooldownFormatado = computed(() => {
  const m = Math.floor(cooldown.value / 60)
  const s = cooldown.value % 60
  return `${m}:${s.toString().padStart(2, '0')}`
})

function iconePorCanal(canal) {
  const icones = { EMAIL: 'bi bi-envelope-fill', WHATSAPP: 'bi bi-whatsapp', SMS: 'bi bi-chat-dots-fill' }
  return icones[canal] || 'bi bi-shield-lock'
}

function labelPorCanal(canal) {
  const labels = { EMAIL: 'E-mail', WHATSAPP: 'WhatsApp', SMS: 'SMS' }
  return labels[canal] || canal
}

function descricaoPorCanal(canal) {
  const desc = {
    EMAIL: 'Receber o código no seu e-mail',
    WHATSAPP: 'Receber o código pelo WhatsApp',
    SMS: 'Receber o código por SMS'
  }
  return desc[canal] || ''
}

async function selecionarCanal(canal) {
  erroSelecao.value = ''
  selecionandoCanal.value = true
  try {
    const resposta = await reenviar(tokenPendente, canal)
    tipo.value = resposta.tipo
    destinoMascarado.value = resposta.destinoMascarado
    mostraSelecaoCanal.value = false
    iniciarCooldown()
    await nextTick()
    inputs.value[0]?.focus()
  } catch (e) {
    erroSelecao.value = extrairMensagemErro(e, 'Não foi possível enviar o código.')
  } finally {
    selecionandoCanal.value = false
  }
}

function voltarSelecao() {
  mostraSelecaoCanal.value = true
  clearInterval(intervalo)
  cooldown.value = 0
  mensagemReenvio.value = ''
  erroReenvio.value = ''
  erro.value = ''
  for (let i = 0; i < 6; i++) digitos[i] = ''
}

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
  if (tipo.value !== 'TOTP' && tipo.value !== 'AGUARDANDO_CANAL') iniciarCooldown()
})

onUnmounted(() => clearInterval(intervalo))
</script>
