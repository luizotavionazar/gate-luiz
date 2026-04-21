<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../services/api'
import { getUsuarioLogado, logout } from '../services/autenticacaoService'

const router = useRouter()
const usuario = getUsuarioLogado()
const roles = ref([])
const carregando = ref(true)
const erro = ref('')

onMounted(async () => {
  try {
    const response = await api.get('/me/roles')
    roles.value = response.data.roles
  } catch {
    erro.value = 'Erro ao carregar seus roles.'
  } finally {
    carregando.value = false
  }
})

function sair() {
  logout()
  router.push('/login')
}
</script>

<template>
  <div class="min-vh-100 bg-light">
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark px-4">
      <span class="navbar-brand fw-bold">PermLuiz</span>
      <div class="ms-auto d-flex gap-2">
        <router-link to="/admin/roles" class="btn btn-outline-light btn-sm">Roles</router-link>
        <router-link to="/admin/permissoes" class="btn btn-outline-light btn-sm">Permissões</router-link>
        <router-link to="/admin/usuarios" class="btn btn-outline-light btn-sm">Usuários</router-link>
        <button class="btn btn-outline-danger btn-sm" @click="sair">Sair</button>
      </div>
    </nav>

    <div class="container py-4">
      <h5 class="mb-1">Olá, {{ usuario?.nome }}</h5>
      <p class="text-muted small mb-4">{{ usuario?.email }}</p>

      <div v-if="carregando" class="text-center py-4">
        <div class="spinner-border text-dark"></div>
      </div>

      <div v-else-if="erro" class="alert alert-danger small">{{ erro }}</div>

      <div v-else>
        <h6 class="fw-semibold mb-3">Seus roles</h6>

        <div v-if="roles.length === 0" class="text-muted small">
          Nenhum role atribuído.
        </div>

        <div v-for="role in roles" :key="role.id" class="card mb-3">
          <div class="card-body">
            <div class="fw-semibold">{{ role.nome }}</div>
            <div v-if="role.descricao" class="text-muted small">{{ role.descricao }}</div>
            <div v-if="role.permissoes.length" class="mt-2 d-flex flex-wrap gap-1">
              <span v-for="p in role.permissoes" :key="p.id" class="badge bg-secondary">
                {{ p.recurso }}:{{ p.acao }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
