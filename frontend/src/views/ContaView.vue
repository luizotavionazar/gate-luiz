<template>
  <div class="min-vh-100 py-5" style="background-color: #eef4ff;">
    <div class="container" style="max-width: 920px;">
      <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
        <div>
          <h1 class="h3 fw-bold mb-1">Minha conta</h1>
          <p class="text-muted mb-0">Gerencie seus dados, suas formas de acesso e a senha local da AuthLuiz.</p>
        </div>
        <button class="btn btn-outline-danger align-self-start align-self-md-center" @click="sair">Sair</button>
      </div>

      <div v-if="carregando" class="alert alert-info">Carregando conta...</div>
      <div v-else-if="erro" class="alert alert-danger">{{ erro }}</div>

      <template v-else-if="conta">
        <div class="card shadow border-0 rounded-4 mb-4">
          <div class="card-body p-4">
            <div class="d-flex flex-column flex-lg-row justify-content-between gap-3">
              <div>
                <h2 class="h5 mb-2">Resumo da conta</h2>
                <p class="mb-1"><strong>{{ conta.nome }}</strong></p>
                <p class="text-muted mb-0">{{ conta.email }}</p>
              </div>

              <div class="d-flex flex-wrap gap-2 align-content-start">
                <span class="badge rounded-pill text-bg-primary-subtle text-primary-emphasis border">ID {{ conta.idUsuario }}</span>
                <span class="badge rounded-pill" :class="conta.temSenhaLocal ? 'text-bg-success-subtle text-success-emphasis border' : 'text-bg-warning-subtle text-warning-emphasis border'">
                  {{ conta.temSenhaLocal ? 'Senha local ativa' : 'Sem senha local' }}
                </span>
                <span class="badge rounded-pill" :class="conta.temLoginGoogle ? 'text-bg-success-subtle text-success-emphasis border' : 'text-bg-secondary-subtle text-secondary-emphasis border'">
                  {{ conta.temLoginGoogle ? 'Google vinculado' : 'Google não vinculado' }}
                </span>
              </div>
            </div>

            <hr class="my-4" />

            <div class="row g-3 small text-muted">
              <div class="col-md-6">
                <div><strong class="text-dark">Criada em:</strong> {{ formatarDataHora(conta.dataCriacao) }}</div>
              </div>
              <div class="col-md-6">
                <div><strong class="text-dark">Atualizada em:</strong> {{ formatarDataHora(conta.dataAtualiza) }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="row g-4">
          <div class="col-lg-6">
            <div class="card shadow border-0 rounded-4 h-100">
              <div class="card-body p-4">
                <h2 class="h5 mb-3">Alterar nome</h2>
                <form @submit.prevent="salvarNome">
                  <div class="mb-3">
                    <label class="form-label">Nome</label>
                    <input v-model="formNome.nome" class="form-control" placeholder="Seu nome" required />
                  </div>

                  <div v-if="mensagemNome" class="alert alert-success py-2 small">{{ mensagemNome }}</div>
                  <div v-if="erroNome" class="alert alert-danger py-2 small">{{ erroNome }}</div>

                  <div class="d-grid">
                    <button class="btn btn-primary" :disabled="salvandoNome">
                      {{ salvandoNome ? 'Salvando...' : 'Salvar nome' }}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>

          <div class="col-lg-6">
            <div class="card shadow border-0 rounded-4 h-100">
              <div class="card-body p-4">
                <h2 class="h5 mb-3">Alterar e-mail</h2>
                <form @submit.prevent="salvarEmail">
                  <div class="mb-3">
                    <label class="form-label">E-mail</label>
                    <input v-model="formEmail.email" type="email" class="form-control" placeholder="seuemail@exemplo.com" required />
                  </div>

                  <div v-if="mensagemEmail" class="alert alert-success py-2 small">{{ mensagemEmail }}</div>
                  <div v-if="erroEmail" class="alert alert-danger py-2 small">{{ erroEmail }}</div>

                  <div class="d-grid">
                    <button class="btn btn-primary" :disabled="salvandoEmail">
                      {{ salvandoEmail ? 'Salvando...' : 'Salvar e-mail' }}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>

          <div class="col-12">
            <div class="card shadow border-0 rounded-4">
              <div class="card-body p-4">
                <div class="d-flex flex-column flex-lg-row justify-content-between gap-3 mb-3">
                  <div>
                    <h2 class="h5 mb-2">{{ conta.temSenhaLocal ? 'Trocar senha local' : 'Definir senha local' }}</h2>
                    <p class="text-muted mb-0 small">
                      {{ conta.temSenhaLocal
                        ? 'Informe a senha atual e escolha uma nova senha.'
                        : 'Sua conta foi criada sem senha local. Defina uma senha para também poder entrar por e-mail.' }}
                    </p>
                  </div>

                  <div class="small text-muted">
                    <div><strong>Login com Google:</strong> {{ conta.temLoginGoogle ? 'Ativo' : 'Não vinculado' }}</div>
                    <div><strong>Senha local:</strong> {{ conta.temSenhaLocal ? 'Configurada' : 'Ainda não definida' }}</div>
                  </div>
                </div>

                <form @submit.prevent="salvarSenha">
                  <div class="row g-3">
                    <div v-if="conta.temSenhaLocal" class="col-lg-4">
                      <label class="form-label">Senha atual</label>
                      <div class="position-relative">
                        <input v-model="formSenha.senhaAtual" :type="mostrarSenhaAtual ? 'text' : 'password'" class="form-control pe-5 campo-senha" placeholder="Digite a senha atual" />
                        <button type="button" class="btn btn-sm border-0 bg-transparent position-absolute top-50 end-0 translate-middle-y me-2 text-muted" @click="mostrarSenhaAtual = !mostrarSenhaAtual" :aria-label="mostrarSenhaAtual ? 'Ocultar senha atual' : 'Mostrar senha atual'">
                          <i :class="mostrarSenhaAtual ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                        </button>
                      </div>
                    </div>

                    <div :class="conta.temSenhaLocal ? 'col-lg-4' : 'col-lg-6'">
                      <label class="form-label">Nova senha</label>
                      <div class="position-relative">
                        <input v-model="formSenha.novaSenha" :type="mostrarNovaSenha ? 'text' : 'password'" class="form-control pe-5 campo-senha" placeholder="Digite a nova senha" @focus="senhaEmFoco = true" @blur="senhaEmFoco = false" />
                        <button type="button" class="btn btn-sm border-0 bg-transparent position-absolute top-50 end-0 translate-middle-y me-2 text-muted" @click="mostrarNovaSenha = !mostrarNovaSenha" :aria-label="mostrarNovaSenha ? 'Ocultar nova senha' : 'Mostrar nova senha'">
                          <i :class="mostrarNovaSenha ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                        </button>
                      </div>
                    </div>

                    <div :class="conta.temSenhaLocal ? 'col-lg-4' : 'col-lg-6'">
                      <label class="form-label">Confirme a nova senha</label>
                      <div class="position-relative">
                        <input v-model="formSenha.confirmacao" :type="mostrarConfirmacao ? 'text' : 'password'" class="form-control pe-5 campo-senha" placeholder="Repita a nova senha" @focus="confirmacaoEmFoco = true" @blur="confirmacaoEmFoco = false" />
                        <button type="button" class="btn btn-sm border-0 bg-transparent position-absolute top-50 end-0 translate-middle-y me-2 text-muted" @click="mostrarConfirmacao = !mostrarConfirmacao" :aria-label="mostrarConfirmacao ? 'Ocultar confirmação' : 'Mostrar confirmação'">
                          <i :class="mostrarConfirmacao ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                        </button>
                      </div>
                    </div>
                  </div>

                  <ul v-if="mostrarRegrasSenha" class="list-unstyled small mt-3 mb-0">
                    <li :class="senhaRegras.tamanho ? 'text-success' : 'text-danger'">{{ senhaRegras.tamanho ? '✓' : '✕' }} Pelo menos 8 caracteres</li>
                    <li :class="senhaRegras.maiuscula ? 'text-success' : 'text-danger'">{{ senhaRegras.maiuscula ? '✓' : '✕' }} Pelo menos 1 letra maiúscula</li>
                    <li :class="senhaRegras.numero ? 'text-success' : 'text-danger'">{{ senhaRegras.numero ? '✓' : '✕' }} Pelo menos 1 número</li>
                    <li :class="senhaRegras.especial ? 'text-success' : 'text-danger'">{{ senhaRegras.especial ? '✓' : '✕' }} Pelo menos 1 caractere especial</li>
                  </ul>

                  <div v-if="mostrarValidacaoConfirmacao" class="small mt-2" :class="senhasCoincidem ? 'text-success' : 'text-danger'">
                    {{ senhasCoincidem ? '✓ As senhas coincidem' : '✕ As senhas não coincidem' }}
                  </div>

                  <div v-if="mensagemSenha" class="alert alert-success py-2 small mt-3">{{ mensagemSenha }}</div>
                  <div v-if="erroSenha" class="alert alert-danger py-2 small mt-3">{{ erroSenha }}</div>

                  <div class="d-grid d-lg-flex justify-content-lg-end mt-3">
                    <button class="btn btn-primary" :disabled="salvandoSenha">
                      {{ salvandoSenha
                        ? (conta.temSenhaLocal ? 'Alterando...' : 'Definindo...')
                        : (conta.temSenhaLocal ? 'Alterar senha local' : 'Definir senha local') }}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  atualizarMeuEmail,
  atualizarMeuNome,
  atualizarMinhaSenha,
  atualizarSessaoComConta,
  buscarMinhaConta,
  logout,
  marcarSenhaLocalNaSessao
} from '../services/autenticacaoService'
import { extrairMensagemErro } from '../utils/extrairMensagemErro'

const router = useRouter()
const conta = ref(null)
const carregando = ref(true)
const erro = ref('')

const formNome = reactive({ nome: '' })
const formEmail = reactive({ email: '' })
const formSenha = reactive({ senhaAtual: '', novaSenha: '', confirmacao: '' })

const salvandoNome = ref(false)
const salvandoEmail = ref(false)
const salvandoSenha = ref(false)

const mensagemNome = ref('')
const mensagemEmail = ref('')
const mensagemSenha = ref('')
const erroNome = ref('')
const erroEmail = ref('')
const erroSenha = ref('')

const senhaEmFoco = ref(false)
const confirmacaoEmFoco = ref(false)
const mostrarSenhaAtual = ref(false)
const mostrarNovaSenha = ref(false)
const mostrarConfirmacao = ref(false)

const senhaRegras = computed(() => ({
  tamanho: formSenha.novaSenha.length >= 8,
  maiuscula: /\p{Lu}/u.test(formSenha.novaSenha),
  numero: /\d/u.test(formSenha.novaSenha),
  especial: /[^\p{L}\d\s]/u.test(formSenha.novaSenha)
}))

const senhaValida = computed(() => Object.values(senhaRegras.value).every(Boolean))
const senhasCoincidem = computed(() => formSenha.confirmacao.length > 0 && formSenha.novaSenha === formSenha.confirmacao)
const mostrarRegrasSenha = computed(() => senhaEmFoco.value || formSenha.novaSenha.length > 0)
const mostrarValidacaoConfirmacao = computed(() => confirmacaoEmFoco.value || formSenha.confirmacao.length > 0)

function preencherFormularios() {
  formNome.nome = conta.value?.nome || ''
  formEmail.email = conta.value?.email || ''
}

function limparMensagens() {
  mensagemNome.value = ''
  mensagemEmail.value = ''
  mensagemSenha.value = ''
  erroNome.value = ''
  erroEmail.value = ''
  erroSenha.value = ''
}

function formatarDataHora(data) {
  if (!data) return 'Não informado'
  return new Date(data).toLocaleString('pt-BR')
}

function sair() {
  logout()
  router.push('/login')
}

async function carregarConta() {
  carregando.value = true
  erro.value = ''

  try {
    conta.value = await buscarMinhaConta()
    preencherFormularios()
    atualizarSessaoComConta(conta.value)
  } catch (e) {
    erro.value = extrairMensagemErro(e, 'Não foi possível carregar a conta.')
    console.error(e)
  } finally {
    carregando.value = false
  }
}

async function salvarNome() {
  mensagemNome.value = ''
  erroNome.value = ''
  salvandoNome.value = true

  try {
    conta.value = await atualizarMeuNome({ nome: formNome.nome.trim() })
    atualizarSessaoComConta(conta.value)
    mensagemNome.value = 'Nome atualizado com sucesso.'
  } catch (e) {
    erroNome.value = extrairMensagemErro(e, 'Não foi possível atualizar o nome.')
    console.error(e)
  } finally {
    salvandoNome.value = false
  }
}

async function salvarEmail() {
  mensagemEmail.value = ''
  erroEmail.value = ''
  salvandoEmail.value = true

  try {
    conta.value = await atualizarMeuEmail({ email: formEmail.email.trim() })
    atualizarSessaoComConta(conta.value)
    mensagemEmail.value = 'E-mail atualizado com sucesso.'
  } catch (e) {
    erroEmail.value = extrairMensagemErro(e, 'Não foi possível atualizar o e-mail.')
    console.error(e)
  } finally {
    salvandoEmail.value = false
  }
}

async function salvarSenha() {
  mensagemSenha.value = ''
  erroSenha.value = ''

  if (!senhaValida.value) {
    erroSenha.value = 'A senha ainda não atende aos requisitos.'
    return
  }

  if (!senhasCoincidem.value) {
    erroSenha.value = 'As senhas não coincidem.'
    return
  }

  salvandoSenha.value = true

  try {
    const response = await atualizarMinhaSenha({
      senhaAtual: conta.value.temSenhaLocal ? formSenha.senhaAtual : null,
      novaSenha: formSenha.novaSenha
    })

    mensagemSenha.value = response.mensagem
    formSenha.senhaAtual = ''
    formSenha.novaSenha = ''
    formSenha.confirmacao = ''

    if (conta.value) {
      conta.value.temSenhaLocal = true
      conta.value.dataAtualiza = new Date().toISOString()
    }

    marcarSenhaLocalNaSessao()
    await carregarConta()
  } catch (e) {
    erroSenha.value = extrairMensagemErro(e, 'Não foi possível atualizar a senha local.')
    console.error(e)
  } finally {
    salvandoSenha.value = false
  }
}

onMounted(async () => {
  limparMensagens()
  await carregarConta()
})
</script>
