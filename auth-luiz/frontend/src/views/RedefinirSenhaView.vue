<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center" style="background-color: #eef4ff;">
    <div class="card shadow border-0 rounded-4" style="width: 100%; max-width: 480px;">
      <div class="card-body p-4 p-md-5">
        <i class="bi bi-key text-primary d-block text-center" style="font-size: 2.5rem;"></i>
        <h1 class="h4 fw-bold text-center mt-2 mb-1">Verificar código</h1>
        <p class="text-muted text-center mb-4">
          Digite o código de 6 dígitos enviado para o seu
          {{ canal === 'telefone' ? 'WhatsApp/SMS' : 'e-mail' }}.
        </p>

        <form @submit.prevent="verificar">
          <div class="mb-3">
            <label :for="canal" class="form-label">
              {{ canal === 'telefone' ? 'Telefone' : 'E-mail' }}
            </label>
            <TelefoneInput
              v-if="canal === 'telefone'"
              v-model="identificador"
              required
            />
            <input
              v-else
              id="email"
              v-model="identificador"
              type="email"
              class="form-control"
              placeholder="seuemail@exemplo.com"
              autocomplete="email"
              required
            />
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

          <div class="text-center mb-3">
            <button
              type="button"
              class="btn btn-link btn-sm text-decoration-none p-0"
              :disabled="cooldown > 0 || reenviando"
              @click="reenviar"
            >
              {{ reenviando ? 'Reenviando...' : cooldown > 0 ? `Reenviar código (${cooldownFormatado})` : 'Reenviar código' }}
            </button>
            <div v-if="erroReenvio" class="text-danger small mt-1">{{ erroReenvio }}</div>
          </div>

          <div v-if="erro" class="alert alert-danger py-2 small mb-3">{{ erro }}</div>

          <div class="d-grid">
            <button type="submit" class="btn btn-primary" :disabled="carregando || codigoCompleto.length < 6">
              {{ carregando ? 'Verificando...' : 'Continuar' }}
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
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { iniciarRecuperacaoSenha, validarCodigoRecuperacao } from '../services/autenticacaoService'
import { extrairMensagemErro } from '../utils/extrairMensagemErro'
import TelefoneInput from '../components/TelefoneInput.vue'

const route = useRoute()
const router = useRouter()

const canal = ref('email')
const identificador = ref('')
const digitos = reactive(Array(6).fill(''))
const inputs = ref([])
const erro = ref('')
const erroReenvio = ref('')
const carregando = ref(false)
const reenviando = ref(false)
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

async function verificar() {
  erro.value = ''
  carregando.value = true

  try {
    const payload = canal.value === 'telefone'
      ? { telefone: identificador.value, codigo: codigoCompleto.value }
      : { email: identificador.value.trim(), codigo: codigoCompleto.value }

    await validarCodigoRecuperacao(payload)

    router.push({
      name: 'nova-senha',
      state: { canal: canal.value, identificador: identificador.value, codigo: codigoCompleto.value }
    })
  } catch (e) {
    erro.value = extrairMensagemErro(e, 'Não foi possível verificar o código.')
    digitos.fill('')
    inputs.value[0]?.focus()
  } finally {
    carregando.value = false
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
  erroReenvio.value = ''
  reenviando.value = true
  try {
    const payload = canal.value === 'telefone'
      ? { telefone: identificador.value }
      : { email: identificador.value.trim() }
    await iniciarRecuperacaoSenha(payload)
    iniciarCooldown()
  } catch (e) {
    erroReenvio.value = extrairMensagemErro(e, 'Não foi possível reenviar o código.')
  } finally {
    reenviando.value = false
  }
}

onMounted(() => {
  if (route.query.telefone) {
    canal.value = 'telefone'
    identificador.value = route.query.telefone
  } else {
    canal.value = 'email'
    identificador.value = route.query.email || ''
  }
  iniciarCooldown()
})

onUnmounted(() => clearInterval(intervalo))
</script>
