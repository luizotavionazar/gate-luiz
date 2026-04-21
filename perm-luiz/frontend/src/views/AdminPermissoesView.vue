<script setup>
import { ref, onMounted } from 'vue'
import api from '../services/api'

const permissoes = ref([])
const carregando = ref(true)
const erro = ref('')
const sucesso = ref('')

const novoRecurso = ref('')
const novaAcao = ref('')
const novaDescricao = ref('')
const criando = ref(false)

const editandoId = ref(null)
const editRecurso = ref('')
const editAcao = ref('')
const editDescricao = ref('')

onMounted(async () => {
  await carregar()
  carregando.value = false
})

async function carregar() {
  const res = await api.get('/admin/permissoes')
  permissoes.value = res.data
}

async function criar() {
  erro.value = ''
  criando.value = true
  try {
    await api.post('/admin/permissoes', {
      recurso: novoRecurso.value,
      acao: novaAcao.value,
      descricao: novaDescricao.value
    })
    sucesso.value = 'Permissão criada!'
    novoRecurso.value = ''
    novaAcao.value = ''
    novaDescricao.value = ''
    await carregar()
  } catch (e) {
    erro.value = e?.response?.data?.mensagem || 'Erro ao criar permissão.'
  } finally {
    criando.value = false
  }
}

function iniciarEdicao(p) {
  editandoId.value = p.id
  editRecurso.value = p.recurso
  editAcao.value = p.acao
  editDescricao.value = p.descricao || ''
}

async function salvarEdicao(id) {
  try {
    await api.put(`/admin/permissoes/${id}`, {
      recurso: editRecurso.value,
      acao: editAcao.value,
      descricao: editDescricao.value
    })
    editandoId.value = null
    sucesso.value = 'Permissão atualizada!'
    await carregar()
  } catch (e) {
    erro.value = e?.response?.data?.mensagem || 'Erro ao atualizar permissão.'
  }
}

async function remover(id) {
  if (!confirm('Remover esta permissão? Ela será removida de todos os roles que a possuem.')) return
  try {
    await api.delete(`/admin/permissoes/${id}`)
    sucesso.value = 'Permissão removida!'
    await carregar()
  } catch (e) {
    erro.value = e?.response?.data?.mensagem || 'Erro ao remover permissão.'
  }
}
</script>

<template>
  <div class="min-vh-100 bg-light">
    <nav class="navbar navbar-dark bg-dark px-4">
      <router-link to="/conta" class="navbar-brand fw-bold">PermLuiz</router-link>
      <div class="ms-auto d-flex gap-2">
        <router-link to="/admin/roles" class="btn btn-outline-light btn-sm">Roles</router-link>
        <router-link to="/admin/usuarios" class="btn btn-outline-light btn-sm">Usuários</router-link>
      </div>
    </nav>

    <div class="container py-4">
      <h5 class="fw-semibold mb-4">Gerenciar Permissões</h5>

      <div v-if="sucesso" class="alert alert-success small" @click="sucesso=''">{{ sucesso }}</div>
      <div v-if="erro" class="alert alert-danger small" @click="erro=''">{{ erro }}</div>

      <div class="card mb-4">
        <div class="card-body">
          <h6 class="fw-semibold mb-3">Nova permissão</h6>
          <form @submit.prevent="criar" class="row g-2">
            <div class="col-md-3">
              <input v-model="novoRecurso" class="form-control form-control-sm" placeholder="Recurso (ex: artigos)" required />
            </div>
            <div class="col-md-3">
              <input v-model="novaAcao" class="form-control form-control-sm" placeholder="Ação (ex: criar)" required />
            </div>
            <div class="col-md-4">
              <input v-model="novaDescricao" class="form-control form-control-sm" placeholder="Descrição (opcional)" />
            </div>
            <div class="col-md-2">
              <button type="submit" class="btn btn-dark btn-sm w-100" :disabled="criando">Criar</button>
            </div>
          </form>
        </div>
      </div>

      <div v-if="carregando" class="text-center py-4"><div class="spinner-border text-dark"></div></div>

      <div v-else>
        <div v-if="permissoes.length === 0" class="text-muted small">Nenhuma permissão cadastrada.</div>

        <table v-else class="table table-bordered table-sm bg-white">
          <thead class="table-dark">
            <tr>
              <th>Recurso</th>
              <th>Ação</th>
              <th>Descrição</th>
              <th style="width:120px"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in permissoes" :key="p.id">
              <template v-if="editandoId === p.id">
                <td><input v-model="editRecurso" class="form-control form-control-sm" /></td>
                <td><input v-model="editAcao" class="form-control form-control-sm" /></td>
                <td><input v-model="editDescricao" class="form-control form-control-sm" /></td>
                <td>
                  <button class="btn btn-dark btn-sm me-1" @click="salvarEdicao(p.id)">Salvar</button>
                  <button class="btn btn-secondary btn-sm" @click="editandoId=null">×</button>
                </td>
              </template>
              <template v-else>
                <td class="small">{{ p.recurso }}</td>
                <td class="small">{{ p.acao }}</td>
                <td class="small text-muted">{{ p.descricao || '—' }}</td>
                <td>
                  <button class="btn btn-outline-secondary btn-sm me-1" @click="iniciarEdicao(p)">
                    <i class="bi bi-pencil"></i>
                  </button>
                  <button class="btn btn-outline-danger btn-sm" @click="remover(p.id)">
                    <i class="bi bi-trash"></i>
                  </button>
                </td>
              </template>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
