<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { salvarSessao } from '../services/autenticacaoService'
import { AUTH_LUIZ_URL } from '../services/api'

const router = useRouter()
const email = ref('')
const senha = ref('')
const erro = ref('')
const carregando = ref(false)

async function entrar() {
  erro.value = ''
  carregando.value = true
  try {
    const response = await axios.post(`${AUTH_LUIZ_URL}/auth/login`, {
      email: email.value,
      senha: senha.value
    })
    salvarSessao(response.data)
    router.push('/conta')
  } catch (e) {
    erro.value = e?.response?.data?.mensagem || 'E-mail ou senha incorretos.'
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
        <p class="text-muted small mb-4">Entre com sua conta Auth-Luiz</p>

        <div v-if="erro" class="alert alert-danger small">{{ erro }}</div>

        <form @submit.prevent="entrar">
          <div class="mb-3">
            <label class="form-label small fw-semibold">E-mail</label>
            <input v-model="email" type="email" class="form-control" required />
          </div>
          <div class="mb-4">
            <label class="form-label small fw-semibold">Senha</label>
            <input v-model="senha" type="password" class="form-control" required />
          </div>
          <button type="submit" class="btn btn-dark w-100" :disabled="carregando">
            <span v-if="carregando" class="spinner-border spinner-border-sm me-2"></span>
            Entrar
          </button>
        </form>
      </div>
    </div>
  </div>
</template>
