<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center py-5" style="background-color: #eef4ff;">
    <div class="card shadow border-0 rounded-4 text-center" style="width: 100%; max-width: 480px;">
      <div class="card-body p-4 p-md-5">
        <template v-if="carregando">
          <div class="spinner-border text-primary mb-3" role="status"></div>
          <p class="text-muted">Verificando seu e-mail...</p>
        </template>

        <template v-else-if="sucesso">
          <i class="bi bi-check-circle-fill text-success" style="font-size: 3rem;"></i>
          <h1 class="h4 fw-bold mt-3 mb-2">E-mail confirmado!</h1>
          <p class="text-muted mb-4">Sua conta está ativa. Você já pode acessar normalmente.</p>
          <router-link to="/login" class="btn btn-primary">Ir para o login</router-link>
        </template>

        <template v-else>
          <i class="bi bi-x-circle-fill text-danger" style="font-size: 3rem;"></i>
          <h1 class="h4 fw-bold mt-3 mb-2">Link inválido ou expirado</h1>
          <p class="text-muted mb-4">{{ erro }}</p>
          <router-link to="/login" class="btn btn-outline-secondary">Voltar ao login</router-link>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { confirmarEmail } from '../services/autenticacaoService'
import { extrairMensagemErro } from '../utils/extrairMensagemErro'

const route = useRoute()
const carregando = ref(true)
const sucesso = ref(false)
const erro = ref('')

onMounted(async () => {
  const token = route.query.token

  if (!token) {
    erro.value = 'Nenhum token de verificação foi encontrado na URL.'
    carregando.value = false
    return
  }

  try {
    await confirmarEmail(token)
    sucesso.value = true
  } catch (e) {
    erro.value = extrairMensagemErro(e, 'O link é inválido ou já expirou. Solicite um novo e-mail de verificação.')
  } finally {
    carregando.value = false
  }
})
</script>
