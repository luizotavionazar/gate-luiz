<script setup>
import { ref, onMounted } from 'vue'
import api from '../services/api'

const usuarios = ref([])
const todosRoles = ref([])
const carregando = ref(false)
const erro = ref('')
const sucesso = ref('')

const usuarioSelecionado = ref(null)
const modalAberto = ref(false)

const usuarioDetalhes = ref(null)
const modalDetalhesAberto = ref(false)

onMounted(async () => {
  carregando.value = true
  try {
    const [resUsuarios, resRoles] = await Promise.all([
      api.get('/admin/usuarios'),
      api.get('/admin/roles')
    ])
    usuarios.value = resUsuarios.data
    todosRoles.value = resRoles.data
  } catch (e) {
    erro.value = e?.response?.data?.mensagem || 'Erro ao carregar usuários.'
  } finally {
    carregando.value = false
  }
})

function abrirModal(usuario) {
  usuarioSelecionado.value = { ...usuario, roles: [...usuario.roles] }
  modalAberto.value = true
  sucesso.value = ''
  erro.value = ''
}

function fecharModal() {
  modalAberto.value = false
  usuarioSelecionado.value = null
}

function possuiRole(idRole) {
  return usuarioSelecionado.value?.roles.some(r => r.id === idRole)
}

async function atribuirRole(idRole) {
  try {
    await api.post(`/admin/usuarios/${usuarioSelecionado.value.idUsuario}/roles/${idRole}`)
    sucesso.value = 'Role atribuído!'
    await recarregarUsuario()
  } catch (e) {
    erro.value = e?.response?.data?.mensagem || 'Erro ao atribuir role.'
  }
}

async function removerRole(idRole) {
  if (!confirm('Remover este role do usuário?')) return
  try {
    await api.delete(`/admin/usuarios/${usuarioSelecionado.value.idUsuario}/roles/${idRole}`)
    sucesso.value = 'Role removido!'
    await recarregarUsuario()
  } catch (e) {
    erro.value = e?.response?.data?.mensagem || 'Erro ao remover role.'
  }
}

async function recarregarUsuario() {
  const res = await api.get('/admin/usuarios')
  usuarios.value = res.data
  const atualizado = res.data.find(u => u.idUsuario === usuarioSelecionado.value.idUsuario)
  if (atualizado) usuarioSelecionado.value = { ...atualizado }
}

function abrirDetalhes(usuario) {
  usuarioDetalhes.value = usuario
  modalDetalhesAberto.value = true
}

function fecharDetalhes() {
  modalDetalhesAberto.value = false
  usuarioDetalhes.value = null
}

function formatarData(data) {
  if (!data) return '—'
  return new Date(data).toLocaleString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
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
      <h5 class="fw-semibold mb-4">Usuários</h5>

      <div v-if="sucesso" class="alert alert-success small" @click="sucesso=''">{{ sucesso }}</div>
      <div v-if="erro" class="alert alert-danger small" @click="erro=''">{{ erro }}</div>

      <div v-if="carregando" class="text-center py-5 text-muted small">
        <span class="spinner-border spinner-border-sm me-2"></span>Carregando usuários...
      </div>

      <div v-else-if="usuarios.length === 0" class="text-muted small">Nenhum usuário encontrado.</div>

      <div v-else class="card">
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead class="table-dark">
              <tr>
                <th class="fw-semibold small">Nome</th>
                <th class="fw-semibold small">Email</th>
                <th class="fw-semibold small">Telefone</th>
                <th class="fw-semibold small">Cadastro</th>
                <th class="fw-semibold small">Último login</th>
                <th class="fw-semibold small"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="usuario in usuarios" :key="usuario.idUsuario">
                <td class="small fw-semibold">{{ usuario.nome }}</td>
                <td class="small text-muted">{{ usuario.email }}</td>
                <td class="small text-muted">{{ usuario.telefone || '—' }}</td>
                <td class="small text-muted">{{ formatarData(usuario.dataCriacao) }}</td>
                <td class="small text-muted">{{ formatarData(usuario.ultimoLogin) }}</td>
                <td>
                  <button class="btn btn-outline-secondary btn-sm me-1" @click="abrirDetalhes(usuario)" title="Detalhes">
                    <i class="bi bi-info-circle"></i>
                  </button>
                  <button class="btn btn-outline-dark btn-sm" @click="abrirModal(usuario)">
                    <i class="bi bi-pencil me-1"></i>Editar roles
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Modal editar roles -->
    <div v-if="modalAberto" class="modal d-block" tabindex="-1" style="background:rgba(0,0,0,.4)">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h6 class="modal-title fw-semibold">
              Roles de {{ usuarioSelecionado?.nome }}
            </h6>
            <button type="button" class="btn-close" @click="fecharModal"></button>
          </div>
          <div class="modal-body">
            <div v-if="sucesso" class="alert alert-success small py-2" @click="sucesso=''">{{ sucesso }}</div>
            <div v-if="erro" class="alert alert-danger small py-2" @click="erro=''">{{ erro }}</div>

            <div v-if="todosRoles.length === 0" class="text-muted small">Nenhum role cadastrado no sistema.</div>

            <div v-else class="d-flex flex-column gap-2">
              <div v-for="role in todosRoles" :key="role.id"
                   class="d-flex justify-content-between align-items-center border rounded px-3 py-2"
                   :class="possuiRole(role.id) ? 'border-dark bg-light' : ''">
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
          <div class="modal-footer">
            <button class="btn btn-dark btn-sm" @click="fecharModal">Fechar</button>
          </div>
        </div>
      </div>
    </div>
    <!-- Modal detalhes do usuário -->
    <div v-if="modalDetalhesAberto" class="modal d-block" tabindex="-1" style="background:rgba(0,0,0,.4)">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h6 class="modal-title fw-semibold">Detalhes — {{ usuarioDetalhes?.nome }}</h6>
            <button type="button" class="btn-close" @click="fecharDetalhes"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <div class="text-muted small mb-1">Última alteração</div>
              <div class="small fw-semibold">{{ formatarData(usuarioDetalhes?.dataAtualiza) }}</div>
            </div>
            <div class="d-flex flex-column gap-2">
              <div class="d-flex justify-content-between align-items-center border rounded px-3 py-2">
                <span class="small">Email verificado</span>
                <span :class="usuarioDetalhes?.emailVerificado ? 'badge bg-success' : 'badge bg-danger'">
                  {{ usuarioDetalhes?.emailVerificado ? 'Sim' : 'Não' }}
                </span>
              </div>
              <div class="d-flex justify-content-between align-items-center border rounded px-3 py-2">
                <span class="small">Telefone verificado</span>
                <span v-if="!usuarioDetalhes?.telefone" class="badge bg-secondary">Sem telefone</span>
                <span v-else :class="usuarioDetalhes?.telefoneVerificado ? 'badge bg-success' : 'badge bg-warning text-dark'">
                  {{ usuarioDetalhes?.telefoneVerificado ? 'Sim' : 'Não' }}
                </span>
              </div>
              <div class="d-flex justify-content-between align-items-center border rounded px-3 py-2">
                <span class="small">Senha definida</span>
                <span :class="usuarioDetalhes?.possuiSenha ? 'badge bg-success' : 'badge bg-secondary'">
                  {{ usuarioDetalhes?.possuiSenha ? 'Sim' : 'Não' }}
                </span>
              </div>
              <div class="d-flex justify-content-between align-items-center border rounded px-3 py-2">
                <span class="small">Google vinculado</span>
                <span :class="usuarioDetalhes?.googleVinculado ? 'badge bg-success' : 'badge bg-secondary'">
                  {{ usuarioDetalhes?.googleVinculado ? 'Sim' : 'Não' }}
                </span>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-dark btn-sm" @click="fecharDetalhes">Fechar</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
