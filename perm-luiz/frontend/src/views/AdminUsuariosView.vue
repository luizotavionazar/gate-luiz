<script setup>
import { ref } from 'vue'
import api from '../services/api'

const idUsuarioBusca = ref('')
const roles = ref([])
const todosRoles = ref([])
const carregandoBusca = ref(false)
const idUsuarioAtual = ref(null)
const erro = ref('')
const sucesso = ref('')

async function buscarUsuario() {
  erro.value = ''
  sucesso.value = ''
  carregandoBusca.value = true
  idUsuarioAtual.value = null
  roles.value = []
  try {
    const [resUsuario, resRoles] = await Promise.all([
      api.get(`/admin/usuarios/${idUsuarioBusca.value}/roles`),
      api.get('/admin/roles')
    ])
    roles.value = resUsuario.data
    todosRoles.value = resRoles.data
    idUsuarioAtual.value = Number(idUsuarioBusca.value)
  } catch (e) {
    erro.value = e?.response?.data?.mensagem || 'Erro ao buscar usuário.'
  } finally {
    carregandoBusca.value = false
  }
}

async function atribuirRole(idRole) {
  try {
    await api.post(`/admin/usuarios/${idUsuarioAtual.value}/roles/${idRole}`)
    sucesso.value = 'Role atribuído!'
    await buscarUsuario()
  } catch (e) {
    erro.value = e?.response?.data?.mensagem || 'Erro ao atribuir role.'
  }
}

async function removerRole(idRole) {
  if (!confirm('Remover este role do usuário?')) return
  try {
    await api.delete(`/admin/usuarios/${idUsuarioAtual.value}/roles/${idRole}`)
    sucesso.value = 'Role removido!'
    await buscarUsuario()
  } catch (e) {
    erro.value = e?.response?.data?.mensagem || 'Erro ao remover role.'
  }
}

function possuiRole(idRole) {
  return roles.value.some(r => r.id === idRole)
}
</script>

<template>
  <div class="min-vh-100 bg-light">
    <nav class="navbar navbar-dark bg-dark px-4">
      <router-link to="/conta" class="navbar-brand fw-bold">PermLuiz</router-link>
      <div class="ms-auto d-flex gap-2">
        <router-link to="/admin/roles" class="btn btn-outline-light btn-sm">Roles</router-link>
        <router-link to="/admin/permissoes" class="btn btn-outline-light btn-sm">Permissões</router-link>
      </div>
    </nav>

    <div class="container py-4">
      <h5 class="fw-semibold mb-4">Gerenciar Usuários</h5>

      <div v-if="sucesso" class="alert alert-success small" @click="sucesso=''">{{ sucesso }}</div>
      <div v-if="erro" class="alert alert-danger small" @click="erro=''">{{ erro }}</div>

      <div class="card mb-4">
        <div class="card-body">
          <h6 class="fw-semibold mb-3">Buscar usuário por ID (Auth-Luiz)</h6>
          <form @submit.prevent="buscarUsuario" class="d-flex gap-2">
            <input v-model="idUsuarioBusca" type="number" class="form-control form-control-sm" placeholder="ID do usuário" min="1" required />
            <button type="submit" class="btn btn-dark btn-sm" :disabled="carregandoBusca">
              <span v-if="carregandoBusca" class="spinner-border spinner-border-sm me-1"></span>
              Buscar
            </button>
          </form>
        </div>
      </div>

      <div v-if="idUsuarioAtual !== null">
        <h6 class="fw-semibold mb-3">Usuário #{{ idUsuarioAtual }}</h6>

        <div v-if="todosRoles.length === 0" class="text-muted small">Nenhum role cadastrado no sistema.</div>

        <div v-else class="row g-2">
          <div v-for="role in todosRoles" :key="role.id" class="col-md-4">
            <div class="card" :class="possuiRole(role.id) ? 'border-dark' : ''">
              <div class="card-body d-flex justify-content-between align-items-center py-2">
                <div>
                  <span class="fw-semibold small">{{ role.nome }}</span>
                  <div v-if="role.descricao" class="text-muted" style="font-size:0.75rem">{{ role.descricao }}</div>
                </div>
                <button v-if="possuiRole(role.id)"
                        class="btn btn-outline-danger btn-sm"
                        @click="removerRole(role.id)">
                  <i class="bi bi-dash-circle"></i>
                </button>
                <button v-else
                        class="btn btn-outline-dark btn-sm"
                        @click="atribuirRole(role.id)">
                  <i class="bi bi-plus-circle"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
