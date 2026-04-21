<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { concluirSetup } from '../services/setupService'

const router = useRouter()
const masterKey = ref('')
const idUsuario = ref('')
const erro = ref('')
const sucesso = ref('')
const carregando = ref(false)

async function enviar() {
  erro.value = ''
  sucesso.value = ''
  carregando.value = true
  try {
    await concluirSetup(masterKey.value, Number(idUsuario.value))
    sucesso.value = 'Setup concluído! Redirecionando para o login...'
    setTimeout(() => router.push('/login'), 2000)
  } catch (e) {
    erro.value = e?.response?.data?.mensagem || 'Erro ao concluir o setup.'
  } finally {
    carregando.value = false
  }
}
</script>

<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center bg-light">
    <div class="card shadow-sm" style="width: 400px">
      <div class="card-body p-4">
        <h4 class="card-title mb-1">PermLuiz</h4>
        <p class="text-muted small mb-4">Configuração inicial</p>

        <div v-if="sucesso" class="alert alert-success small">{{ sucesso }}</div>
        <div v-if="erro" class="alert alert-danger small">{{ erro }}</div>

        <form @submit.prevent="enviar">
          <div class="mb-3">
            <label class="form-label small fw-semibold">Chave mestra</label>
            <input v-model="masterKey" type="password" class="form-control" required />
          </div>
          <div class="mb-4">
            <label class="form-label small fw-semibold">ID do admin mestre (Auth-Luiz)</label>
            <input v-model="idUsuario" type="number" class="form-control" required min="1" />
            <div class="form-text">ID do usuário no Auth-Luiz que será o admin mestre.</div>
          </div>
          <button type="submit" class="btn btn-dark w-100" :disabled="carregando">
            <span v-if="carregando" class="spinner-border spinner-border-sm me-2"></span>
            Concluir setup
          </button>
        </form>
      </div>
    </div>
  </div>
</template>
