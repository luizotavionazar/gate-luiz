<script setup>
import { ref, onMounted } from 'vue'
import api from '../services/api'

const roles = ref([])
const permissoes = ref([])
const carregando = ref(true)
const erro = ref('')
const sucesso = ref('')

const novoNome = ref('')
const novaDescricao = ref('')
const criando = ref(false)

const editandoId = ref(null)
const editNome = ref('')
const editDescricao = ref('')

const gerenciandoId = ref(null)
const permissoesSelecionadas = ref([])

onMounted(async () => {
  await Promise.all([carregarRoles(), carregarPermissoes()])
  carregando.value = false
})

async function carregarRoles() {
  const res = await api.get('/admin/roles')
  roles.value = res.data
}

async function carregarPermissoes() {
  const res = await api.get('/admin/permissoes')
  permissoes.value = res.data
}

async function criar() {
  erro.value = ''
  criando.value = true
  try {
    await api.post('/admin/roles', { nome: novoNome.value, descricao: novaDescricao.value })
    sucesso.value = 'Role criado!'
    novoNome.value = ''
    novaDescricao.value = ''
    await carregarRoles()
  } catch (e) {
    erro.value = e?.response?.data?.mensagem || 'Erro ao criar role.'
  } finally {
    criando.value = false
  }
}

function iniciarEdicao(role) {
  editandoId.value = role.id
  editNome.value = role.nome
  editDescricao.value = role.descricao || ''
}

async function salvarEdicao(id) {
  try {
    await api.put(`/admin/roles/${id}`, { nome: editNome.value, descricao: editDescricao.value })
    editandoId.value = null
    sucesso.value = 'Role atualizado!'
    await carregarRoles()
  } catch (e) {
    erro.value = e?.response?.data?.mensagem || 'Erro ao atualizar role.'
  }
}

async function remover(id) {
  if (!confirm('Remover este role?')) return
  try {
    await api.delete(`/admin/roles/${id}`)
    sucesso.value = 'Role removido!'
    await carregarRoles()
  } catch (e) {
    erro.value = e?.response?.data?.mensagem || 'Erro ao remover role.'
  }
}

function abrirGerenciarPermissoes(role) {
  gerenciandoId.value = role.id
  permissoesSelecionadas.value = role.permissoes.map(p => p.id)
}

async function salvarPermissoes() {
  try {
    await api.put(`/admin/roles/${gerenciandoId.value}/permissoes`, permissoesSelecionadas.value)
    sucesso.value = 'Permissões atualizadas!'
    gerenciandoId.value = null
    await carregarRoles()
  } catch (e) {
    erro.value = e?.response?.data?.mensagem || 'Erro ao atualizar permissões.'
  }
}
</script>

<template>
  <div class="min-vh-100 bg-light">
    <nav class="navbar navbar-dark bg-dark px-4">
      <router-link to="/conta" class="navbar-brand fw-bold">PermLuiz</router-link>
      <div class="ms-auto d-flex gap-2">
        <router-link to="/admin/permissoes" class="btn btn-outline-light btn-sm">Permissões</router-link>
        <router-link to="/admin/usuarios" class="btn btn-outline-light btn-sm">Usuários</router-link>
      </div>
    </nav>

    <div class="container py-4">
      <h5 class="fw-semibold mb-4">Gerenciar Roles</h5>

      <div v-if="sucesso" class="alert alert-success alert-dismissible small" @click="sucesso=''">{{ sucesso }}</div>
      <div v-if="erro" class="alert alert-danger alert-dismissible small" @click="erro=''">{{ erro }}</div>

      <div class="card mb-4">
        <div class="card-body">
          <h6 class="fw-semibold mb-3">Novo role</h6>
          <form @submit.prevent="criar" class="row g-2">
            <div class="col-md-4">
              <input v-model="novoNome" class="form-control form-control-sm" placeholder="Nome (ex: EDITOR)" required />
            </div>
            <div class="col-md-5">
              <input v-model="novaDescricao" class="form-control form-control-sm" placeholder="Descrição (opcional)" />
            </div>
            <div class="col-md-3">
              <button type="submit" class="btn btn-dark btn-sm w-100" :disabled="criando">Criar</button>
            </div>
          </form>
        </div>
      </div>

      <div v-if="carregando" class="text-center py-4"><div class="spinner-border text-dark"></div></div>

      <div v-else>
        <div v-if="roles.length === 0" class="text-muted small">Nenhum role cadastrado.</div>

        <div v-for="role in roles" :key="role.id" class="card mb-2">
          <div class="card-body">
            <template v-if="editandoId === role.id">
              <div class="row g-2">
                <div class="col-md-4">
                  <input v-model="editNome" class="form-control form-control-sm" />
                </div>
                <div class="col-md-5">
                  <input v-model="editDescricao" class="form-control form-control-sm" />
                </div>
                <div class="col-md-3 d-flex gap-1">
                  <button class="btn btn-dark btn-sm" @click="salvarEdicao(role.id)">Salvar</button>
                  <button class="btn btn-secondary btn-sm" @click="editandoId=null">Cancelar</button>
                </div>
              </div>
            </template>
            <template v-else>
              <div class="d-flex align-items-center justify-content-between">
                <div>
                  <span class="fw-semibold">{{ role.nome }}</span>
                  <span v-if="role.descricao" class="text-muted small ms-2">{{ role.descricao }}</span>
                  <div class="mt-1 d-flex flex-wrap gap-1">
                    <span v-for="p in role.permissoes" :key="p.id" class="badge bg-secondary">
                      {{ p.recurso }}:{{ p.acao }}
                    </span>
                    <span v-if="!role.permissoes.length" class="text-muted small">Sem permissões</span>
                  </div>
                </div>
                <div class="d-flex gap-1">
                  <button class="btn btn-outline-secondary btn-sm" @click="abrirGerenciarPermissoes(role)">
                    <i class="bi bi-shield-check"></i>
                  </button>
                  <button class="btn btn-outline-secondary btn-sm" @click="iniciarEdicao(role)">
                    <i class="bi bi-pencil"></i>
                  </button>
                  <button class="btn btn-outline-danger btn-sm" @click="remover(role.id)">
                    <i class="bi bi-trash"></i>
                  </button>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>

      <!-- Modal gerenciar permissões -->
      <div v-if="gerenciandoId" class="modal d-block bg-black bg-opacity-50" tabindex="-1">
        <div class="modal-dialog">
          <div class="modal-content">
            <div class="modal-header">
              <h6 class="modal-title">Permissões do role</h6>
              <button class="btn-close" @click="gerenciandoId=null"></button>
            </div>
            <div class="modal-body">
              <div v-for="p in permissoes" :key="p.id" class="form-check">
                <input class="form-check-input" type="checkbox" :value="p.id" v-model="permissoesSelecionadas" :id="'p'+p.id" />
                <label class="form-check-label small" :for="'p'+p.id">
                  {{ p.recurso }}:{{ p.acao }}
                  <span v-if="p.descricao" class="text-muted"> — {{ p.descricao }}</span>
                </label>
              </div>
              <div v-if="!permissoes.length" class="text-muted small">Nenhuma permissão cadastrada.</div>
            </div>
            <div class="modal-footer">
              <button class="btn btn-secondary btn-sm" @click="gerenciandoId=null">Cancelar</button>
              <button class="btn btn-dark btn-sm" @click="salvarPermissoes">Salvar</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
