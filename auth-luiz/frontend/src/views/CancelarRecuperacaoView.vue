<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center" style="background-color: #eef4ff;">
    <div class="card shadow border-0 rounded-4" style="width: 100%; max-width: 420px;">
      <div class="card-body p-4 p-md-5 text-center">
        <div v-if="carregando">
          <div class="spinner-border text-primary mb-3" role="status">
            <span class="visually-hidden">Cancelando...</span>
          </div>
          <p class="text-muted">Cancelando recuperação de senha...</p>
        </div>

        <div v-else-if="sucesso">
          <i class="bi bi-shield-check text-success" style="font-size: 3rem;"></i>
          <h1 class="h4 fw-bold mt-3 mb-2">Recuperação cancelada</h1>
          <p class="text-muted small mb-4">O código de recuperação foi invalidado com sucesso. Sua conta está segura.</p>
          <RouterLink to="/login" class="btn btn-primary">Ir para o login</RouterLink>
        </div>

        <div v-else>
          <i class="bi bi-exclamation-triangle text-warning" style="font-size: 3rem;"></i>
          <h1 class="h4 fw-bold mt-3 mb-2">Não foi possível cancelar</h1>
          <p class="text-muted small mb-4">{{ erro }}</p>
          <RouterLink to="/login" class="btn btn-outline-secondary">Ir para o login</RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { cancelarRecuperacaoSenha } from '../services/autenticacaoService'
import { extrairMensagemErro } from '../utils/extrairMensagemErro'

const route = useRoute()
const carregando = ref(true)
const sucesso = ref(false)
const erro = ref('')

onMounted(async () => {
  const token = route.query.t
  if (!token) {
    erro.value = 'Link de cancelamento inválido.'
    carregando.value = false
    return
  }

  try {
    await cancelarRecuperacaoSenha(token)
    sucesso.value = true
  } catch (e) {
    erro.value = extrairMensagemErro(e, 'O link de cancelamento é inválido ou já foi utilizado.')
  } finally {
    carregando.value = false
  }
})
</script>
