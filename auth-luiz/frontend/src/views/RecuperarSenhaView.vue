<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center" style="background-color: #eef4ff;">
    <div class="card shadow border-0 rounded-4" style="width: 100%; max-width: 420px;">
      <div class="card-body p-4 p-md-5">
        <h1 class="h4 fw-bold text-center mb-2">Recuperar senha</h1>
        <p class="text-muted text-center mb-4 small">Escolha como deseja receber o código de recuperação.</p>

        <div class="d-flex gap-2 mb-4">
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

        <form @submit.prevent="enviar">
          <div class="mb-3">
            <label :for="canal" class="form-label">
              {{ canal === 'email' ? 'E-mail' : 'Telefone' }}
            </label>
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

          <div v-if="erro" class="alert alert-danger py-2 small">{{ erro }}</div>

          <div class="d-grid">
            <button type="submit" class="btn btn-primary" :disabled="carregando">
              {{ carregando ? 'Enviando...' : 'Enviar código de recuperação' }}
            </button>
          </div>
        </form>

        <div class="text-center mt-3">
          <RouterLink to="/login" class="text-decoration-none small">Voltar para o login</RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { iniciarRecuperacaoSenha } from '../services/autenticacaoService'
import { extrairMensagemErro } from '../utils/extrairMensagemErro'
import TelefoneInput from '../components/TelefoneInput.vue'

const router = useRouter()

const canal = ref('email')
const identificador = ref('')
const erro = ref('')
const carregando = ref(false)

function selecionarCanal(novoCanal) {
  canal.value = novoCanal
  identificador.value = ''
  erro.value = ''
}

async function enviar() {
  erro.value = ''
  carregando.value = true

  try {
    const payload = canal.value === 'email'
      ? { email: identificador.value.trim() }
      : { telefone: identificador.value }

    await iniciarRecuperacaoSenha(payload)

    router.push({ path: '/redefinir-senha', query: payload })
  } catch (e) {
    erro.value = extrairMensagemErro(e, 'Não foi possível iniciar a recuperação.')
  } finally {
    carregando.value = false
  }
}
</script>
