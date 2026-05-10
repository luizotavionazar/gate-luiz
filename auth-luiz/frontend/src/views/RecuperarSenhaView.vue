<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center" style="background-color: #eef4ff;">
    <div class="card shadow border-0 rounded-4" style="width: 100%; max-width: 420px;">
      <div class="card-body p-4 p-md-5">
        <h1 class="h4 fw-bold text-center mb-3">Recuperar senha</h1>
        <p class="text-muted text-center mb-4">Preencha o e-mail da sua conta para receber o código de recuperação.</p>

        <form @submit.prevent="enviar">
          <div class="mb-3">
            <label for="email" class="form-label">E-mail</label>
            <input id="email" v-model="email" type="email" class="form-control" placeholder="seuemail@exemplo.com" required />
          </div>

          <div v-if="erro" class="alert alert-danger py-2 small">{{ erro }}</div>

          <div class="d-grid">
            <button type="submit" class="btn btn-primary" :disabled="carregando">{{ carregando ? 'Enviando...' : 'Enviar código de recuperação' }}</button>
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { iniciarRecuperacaoSenha } from '../services/autenticacaoService'
import { extrairMensagemErro } from '../utils/extrairMensagemErro'

const router = useRouter()

const email = ref('')
const erro = ref('')
const carregando = ref(false)

async function enviar() {
  erro.value = ''
  carregando.value = true

  try {
    await iniciarRecuperacaoSenha({ email: email.value.trim() })
    router.push({ path: '/redefinir-senha', query: { email: email.value.trim() } })
  } catch (e) {
    erro.value = extrairMensagemErro(e, 'Não foi possível iniciar a recuperação.')
  } finally {
    carregando.value = false
  }
}
</script>
