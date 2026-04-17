<template>
  <div class="min-vh-100 py-5" style="background-color: #eef4ff;">
    <div class="container" style="max-width: 920px;">
      <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
        <div>
          <h1 class="h3 fw-bold mb-1">Minha conta</h1>
          <p class="text-muted mb-0">Gerencie seus dados, suas formas de acesso e a senha da AuthLuiz.</p>
        </div>
        <button class="btn btn-outline-danger align-self-start align-self-md-center" @click="sair">Sair</button>
      </div>

      <div v-if="carregando" class="alert alert-info">Carregando conta...</div>
      <div v-else-if="erro" class="alert alert-danger">{{ erro }}</div>

      <template v-else-if="conta">
        <div v-if="!conta.emailVerificado" class="alert alert-warning mb-4">
          <div class="d-flex flex-column flex-sm-row align-items-sm-center gap-2">
            <div class="flex-grow-1">
              <strong>E-mail não verificado!</strong><br>
              Verifique sua caixa de entrada em <strong>{{ conta.email }}</strong> e clique no link de ativação enviado.
            </div>
            <button class="btn btn-sm btn-warning flex-shrink-0" :disabled="reenviando" @click="reenviarVerificacaoEmail">
              {{ reenviando ? 'Enviando...' : 'Reenviar e-mail' }}
            </button>
          </div>
          <div v-if="mensagemReenvioVerificacao" class="small mt-2 text-success-emphasis">{{ mensagemReenvioVerificacao }}</div>
          <div v-if="erroReenvioVerificacao" class="small mt-2 text-danger-emphasis">{{ erroReenvioVerificacao }}</div>
        </div>

        <div v-if="conta.emailPendente" class="alert alert-info mb-4">
          <div class="d-flex flex-column flex-sm-row align-items-sm-center gap-2">
            <div class="flex-grow-1">
              <strong>Alteração de e-mail pendente!</strong><br>
              Acesse o link de confirmação enviado para <strong>{{ conta.emailPendente }}</strong>.
            </div>
            <button class="btn btn-sm btn-info flex-shrink-0" :disabled="reenviandoAlteracaoEmail" @click="reenviarEmailAlteracao">
              {{ reenviandoAlteracaoEmail ? 'Enviando...' : 'Reenviar e-mail' }}
            </button>
          </div>
          <div v-if="mensagemReenvioAlteracaoEmail" class="small mt-2 text-success-emphasis">{{ mensagemReenvioAlteracaoEmail }}</div>
          <div v-if="erroReenvioAlteracaoEmail" class="small mt-2 text-danger-emphasis">{{ erroReenvioAlteracaoEmail }}</div>
        </div>
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
                <span class="badge rounded-pill" :class="conta.temSenha ? 'text-bg-success-subtle text-success-emphasis border' : 'text-bg-warning-subtle text-warning-emphasis border'">
                  {{ conta.temSenha ? 'Senha ativa' : 'Sem senha' }}
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
                    <button class="btn btn-primary" :disabled="salvandoNome || !conta.emailVerificado">
                      {{ salvandoNome ? 'Salvando...' : 'Salvar nome' }}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>

          <div v-if="!conta.temLoginGoogle" class="col-lg-6">
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
                    <button class="btn btn-primary" :disabled="salvandoEmail || !conta.emailVerificado">
                      {{ salvandoEmail ? 'Salvando...' : 'Salvar e-mail' }}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>

          <div v-if="conta.providerOrigem !== 'GOOGLE'" class="col-12">
            <div class="card shadow border-0 rounded-4">
              <div class="card-body p-4">
                <div class="d-flex flex-column flex-lg-row justify-content-between gap-3 mb-3">
                  <div>
                    <h2 class="h5 mb-2">Google</h2>
                    <p class="text-muted mb-0 small">
                      {{ conta.temLoginGoogle
                        ? 'Sua conta está vinculada ao Google. Você pode entrar com seu e-mail Google.'
                        : 'Vincule sua conta do Google para poder entrar sem senha.' }}
                    </p>
                  </div>
                </div>

                <template v-if="conta.temLoginGoogle">
                  <div v-if="mensagemGoogle" class="alert alert-success py-2 small mb-3">{{ mensagemGoogle }}</div>
                  <div v-if="erroGoogle" class="alert alert-danger py-2 small mb-3">{{ erroGoogle }}</div>
                  <div class="d-grid d-lg-flex justify-content-lg-end">
                    <button class="btn btn-outline-danger" @click="abrirModalDesvinculoGoogle">
                      Desvincular Google
                    </button>
                  </div>
                </template>

                <template v-else>
                  <div v-if="!conta.emailVerificado" class="alert alert-warning py-2 small mb-0">
                    Confirme seu e-mail para habilitar a vinculação com Google.
                  </div>
                  <template v-else>
                    <div v-if="mensagemGoogle" class="alert alert-success py-2 small mb-3">{{ mensagemGoogle }}</div>
                    <div v-if="erroGoogle" class="alert alert-danger py-2 small mb-3">{{ erroGoogle }}</div>
                    <div ref="googleVincularButtonRef" class="google-button-host"></div>
                    <div v-if="googleVincularIndisponivel" class="alert alert-warning py-2 small mt-2">{{ googleVincularIndisponivel }}</div>
                  </template>
                </template>
              </div>
            </div>
          </div>

          <div class="col-12">
            <div class="card shadow border-0 rounded-4">
              <div class="card-body p-4">
                <div class="d-flex flex-column flex-lg-row justify-content-between gap-3 mb-3">
                  <div>
                    <h2 class="h5 mb-2">{{ conta.temSenha ? 'Trocar senha' : 'Definir senha' }}</h2>
                    <p class="text-muted mb-0 small">
                      {{ conta.temSenha
                        ? 'Informe a senha atual e escolha uma nova senha.'
                        : 'Sua conta foi criada sem senha. Defina uma senha para também poder entrar por e-mail.' }}
                    </p>
                  </div>
                </div>

                <form @submit.prevent="salvarSenha">
                  <div class="row g-3">
                    <div v-if="conta.temSenha" class="col-lg-4">
                      <label class="form-label">Senha atual</label>
                      <div class="position-relative">
                        <input v-model="formSenha.senhaAtual" :type="mostrarSenhaAtual ? 'text' : 'password'" class="form-control pe-5 campo-senha" placeholder="Digite a senha atual" />
                        <button type="button" class="btn btn-sm border-0 bg-transparent position-absolute top-50 end-0 translate-middle-y me-2 text-muted" @click="mostrarSenhaAtual = !mostrarSenhaAtual" :aria-label="mostrarSenhaAtual ? 'Ocultar senha atual' : 'Mostrar senha atual'">
                          <i :class="mostrarSenhaAtual ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                        </button>
                      </div>
                    </div>

                    <div :class="conta.temSenha ? 'col-lg-4' : 'col-lg-6'">
                      <label class="form-label">Nova senha</label>
                      <div class="position-relative">
                        <input v-model="formSenha.novaSenha" :type="mostrarNovaSenha ? 'text' : 'password'" class="form-control pe-5 campo-senha" placeholder="Digite a nova senha" @focus="senhaEmFoco = true" @blur="senhaEmFoco = false" />
                        <button type="button" class="btn btn-sm border-0 bg-transparent position-absolute top-50 end-0 translate-middle-y me-2 text-muted" @click="mostrarNovaSenha = !mostrarNovaSenha" :aria-label="mostrarNovaSenha ? 'Ocultar nova senha' : 'Mostrar nova senha'">
                          <i :class="mostrarNovaSenha ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                        </button>
                      </div>
                      <ul v-if="mostrarRegrasSenha" class="list-unstyled small mt-1 mb-0">
                        <li :class="senhaRegras.tamanho ? 'text-success' : 'text-danger'">{{ senhaRegras.tamanho ? '✓' : '✕' }} Pelo menos 8 caracteres</li>
                        <li :class="senhaRegras.maiuscula ? 'text-success' : 'text-danger'">{{ senhaRegras.maiuscula ? '✓' : '✕' }} Pelo menos 1 letra maiúscula</li>
                        <li :class="senhaRegras.numero ? 'text-success' : 'text-danger'">{{ senhaRegras.numero ? '✓' : '✕' }} Pelo menos 1 número</li>
                        <li :class="senhaRegras.especial ? 'text-success' : 'text-danger'">{{ senhaRegras.especial ? '✓' : '✕' }} Pelo menos 1 caractere especial</li>
                      </ul>
                    </div>

                    <div :class="conta.temSenha ? 'col-lg-4' : 'col-lg-6'">
                      <label class="form-label">Confirme a nova senha</label>
                      <div class="position-relative">
                        <input v-model="formSenha.confirmacao" :type="mostrarConfirmacao ? 'text' : 'password'" class="form-control pe-5 campo-senha" placeholder="Repita a nova senha" @focus="confirmacaoEmFoco = true" @blur="confirmacaoEmFoco = false" />
                        <button type="button" class="btn btn-sm border-0 bg-transparent position-absolute top-50 end-0 translate-middle-y me-2 text-muted" @click="mostrarConfirmacao = !mostrarConfirmacao" :aria-label="mostrarConfirmacao ? 'Ocultar confirmação' : 'Mostrar confirmação'">
                          <i :class="mostrarConfirmacao ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                        </button>
                      </div>
                      <div v-if="mostrarValidacaoConfirmacao" class="small mt-1" :class="senhasCoincidem ? 'text-success' : 'text-danger'">
                        {{ senhasCoincidem ? '✓ As senhas coincidem' : '✕ As senhas não coincidem' }}
                      </div>
                    </div>
                  </div>

                  <div v-if="mensagemSenha" class="alert alert-success py-2 small mt-3">{{ mensagemSenha }}</div>
                  <div v-if="erroSenha" class="alert alert-danger py-2 small mt-3">{{ erroSenha }}</div>

                  <div class="d-grid d-lg-flex justify-content-lg-end mt-3">
                    <button class="btn btn-primary" :disabled="salvandoSenha || !conta.emailVerificado">
                      {{ salvandoSenha
                        ? (conta.temSenha ? 'Alterando...' : 'Definindo...')
                        : (conta.temSenha ? 'Alterar senha' : 'Definir senha') }}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
        <div class="text-end mt-4">
          <button class="btn btn-outline-danger btn-sm" @click="abrirModalExclusao">
            Excluir conta
          </button>
        </div>
      </template>
    </div>
  </div>

  <!-- Modal de confirmação de exclusão -->
  <div v-if="modalExclusaoVisivel" class="modal-overlay d-flex align-items-center justify-content-center">
    <div class="card shadow border-0 rounded-4" style="width: 100%; max-width: 460px;">
      <div class="card-body p-4">
        <h2 class="h5 fw-bold mb-1">Excluir conta</h2>
        <p class="text-muted small mb-4">Esta ação é permanente e não pode ser desfeita.</p>

        <div class="alert alert-warning rounded-3 small mb-4" role="alert">
          <p class="fw-semibold mb-2">Antes de continuar, leia com atenção:</p>
          <p :class="conta.temSenha ? 'mb-0' : 'mb-2'">
            Ao excluir sua conta, <strong>todos os seus dados serão removidos permanentemente</strong> dos nossos servidores — sem possibilidade de recuperação.
          </p>
          <p v-if="!conta.temSenha" class="mb-0">
            Se você entrar com o Google novamente, uma <strong>conta completamente nova</strong> será criada do zero, sem nenhum vínculo, histórico ou configuração da conta atual.
          </p>
        </div>

        <template v-if="conta.temSenha">
          <div class="mb-4">
            <label class="form-label">Para confirmar, informe sua senha atual:</label>
            <div class="position-relative">
              <input
                v-model="senhaExclusao"
                :type="mostrarSenhaExclusao ? 'text' : 'password'"
                class="form-control pe-5 campo-senha"
                placeholder="Digite sua senha"
                @keyup.enter="confirmarExclusao"
              />
              <button type="button" class="btn btn-sm border-0 bg-transparent position-absolute top-50 end-0 translate-middle-y me-2 text-muted" @click="mostrarSenhaExclusao = !mostrarSenhaExclusao" :aria-label="mostrarSenhaExclusao ? 'Ocultar senha' : 'Mostrar senha'">
                <i :class="mostrarSenhaExclusao ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
              </button>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="form-check mb-4">
            <input class="form-check-input" type="checkbox" id="checkLeuExclusao" v-model="confirmouLeitura" />
            <label class="form-check-label small" for="checkLeuExclusao">
              Li e compreendi as consequências.
            </label>
          </div>
        </template>

        <div v-if="erroExclusao" class="alert alert-danger py-2 small mb-3">{{ erroExclusao }}</div>

        <div class="d-flex gap-2 justify-content-end">
          <button class="btn btn-outline-secondary" @click="fecharModalExclusao" :disabled="excluindo">Cancelar</button>
          <button
            class="btn btn-danger"
            @click="confirmarExclusao"
            :disabled="excluindo || (conta.temSenha ? !senhaExclusao : !confirmouLeitura)"
          >
            {{ excluindo ? 'Excluindo...' : 'Excluir conta' }}
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- Modal de confirmação de desvinculação do Google -->
  <div v-if="modalDesvinculoGoogleVisivel" class="modal-overlay d-flex align-items-center justify-content-center">
    <div class="card shadow border-0 rounded-4" style="width: 100%; max-width: 460px;">
      <div class="card-body p-4">
        <h2 class="h5 fw-bold mb-1">Desvincular Google</h2>
        <p class="text-muted small mb-4">Você não conseguirá mais entrar com o Google após a desvinculação.</p>

        <div class="mb-4">
          <label class="form-label">Para confirmar, informe sua senha atual:</label>
          <div class="position-relative">
            <input
              v-model="senhaDesvinculoGoogle"
              :type="mostrarSenhaDesvinculoGoogle ? 'text' : 'password'"
              class="form-control pe-5 campo-senha"
              placeholder="Digite sua senha"
              @keyup.enter="desvinculaGoogle"
            />
            <button type="button" class="btn btn-sm border-0 bg-transparent position-absolute top-50 end-0 translate-middle-y me-2 text-muted" @click="mostrarSenhaDesvinculoGoogle = !mostrarSenhaDesvinculoGoogle" :aria-label="mostrarSenhaDesvinculoGoogle ? 'Ocultar senha' : 'Mostrar senha'">
              <i :class="mostrarSenhaDesvinculoGoogle ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
            </button>
          </div>
        </div>

        <div v-if="erroDesvinculoGoogle" class="alert alert-danger py-2 small mb-3">{{ erroDesvinculoGoogle }}</div>

        <div class="d-flex gap-2 justify-content-end">
          <button class="btn btn-outline-secondary" @click="fecharModalDesvinculoGoogle" :disabled="desvinculandoGoogle">Cancelar</button>
          <button
            class="btn btn-danger"
            @click="desvinculaGoogle"
            :disabled="desvinculandoGoogle || !senhaDesvinculoGoogle"
          >
            {{ desvinculandoGoogle ? 'Desvinculando...' : 'Desvincular Google' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  atualizarMeuEmail,
  atualizarMeuNome,
  atualizarMinhaSenha,
  atualizarSessaoComConta,
  buscarMinhaConta,
  deletarMinhaConta,
  desvincularGoogle,
  logout,
  marcarSenhaNaSessao,
  reenviarConfirmacaoAlteracaoEmail,
  reenviarVerificacao,
  vincularGoogle
} from '../services/autenticacaoService'
import { getGoogleClientId, renderizarBotaoGoogle } from '../services/googleIdentityService'
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

const reenviando = ref(false)
const mensagemReenvioVerificacao = ref('')
const erroReenvioVerificacao = ref('')
const reenviandoAlteracaoEmail = ref(false)
const mensagemReenvioAlteracaoEmail = ref('')
const erroReenvioAlteracaoEmail = ref('')

const googleVincularButtonRef = ref(null)
const mensagemGoogle = ref('')
const erroGoogle = ref('')
const desvinculandoGoogle = ref(false)
const googleVincularIndisponivel = ref('')
const modalDesvinculoGoogleVisivel = ref(false)
const senhaDesvinculoGoogle = ref('')
const mostrarSenhaDesvinculoGoogle = ref(false)
const erroDesvinculoGoogle = ref('')

const modalExclusaoVisivel = ref(false)
const senhaExclusao = ref('')
const mostrarSenhaExclusao = ref(false)
const confirmouLeitura = ref(false)
const excluindo = ref(false)
const erroExclusao = ref('')

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
    mensagemNome.value = 'Nome atualizado com sucesso!'
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
    if (conta.value.emailPendente) {
      mensagemEmail.value = `Verifique sua caixa de entrada!`
    } else {
      mensagemEmail.value = 'E-mail atualizado com sucesso.'
    }
  } catch (e) {
    erroEmail.value = extrairMensagemErro(e, 'Não foi possível atualizar o e-mail.')
    console.error(e)
  } finally {
    salvandoEmail.value = false
  }
}

async function reenviarVerificacaoEmail() {
  mensagemReenvioVerificacao.value = ''
  erroReenvioVerificacao.value = ''
  reenviando.value = true
  try {
    await reenviarVerificacao()
    await carregarConta()
    mensagemReenvioVerificacao.value = 'E-mail reenviado com sucesso!'
  } catch (e) {
    erroReenvioVerificacao.value = extrairMensagemErro(e, 'Não foi possível reenviar o e-mail de verificação.')
  } finally {
    reenviando.value = false
  }
}

async function reenviarEmailAlteracao() {
  mensagemReenvioAlteracaoEmail.value = ''
  erroReenvioAlteracaoEmail.value = ''
  reenviandoAlteracaoEmail.value = true
  try {
    await reenviarConfirmacaoAlteracaoEmail()
    await carregarConta()
    mensagemReenvioAlteracaoEmail.value = 'E-mail reenviado com sucesso!'
  } catch (e) {
    erroReenvioAlteracaoEmail.value = extrairMensagemErro(e, 'Não foi possível reenviar o e-mail de confirmação de alteração.')
  } finally {
    reenviandoAlteracaoEmail.value = false
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
      senhaAtual: conta.value.temSenha ? formSenha.senhaAtual : null,
      novaSenha: formSenha.novaSenha
    })

    mensagemSenha.value = response.mensagem
    formSenha.senhaAtual = ''
    formSenha.novaSenha = ''
    formSenha.confirmacao = ''

    if (conta.value) {
      conta.value.temSenha = true
      conta.value.dataAtualiza = new Date().toISOString()
    }

    marcarSenhaNaSessao()
    await carregarConta()
  } catch (e) {
    erroSenha.value = extrairMensagemErro(e, 'Não foi possível atualizar a senha.')
    console.error(e)
  } finally {
    salvandoSenha.value = false
  }
}

function abrirModalExclusao() {
  senhaExclusao.value = ''
  mostrarSenhaExclusao.value = false
  confirmouLeitura.value = false
  erroExclusao.value = ''
  modalExclusaoVisivel.value = true
}

function fecharModalExclusao() {
  modalExclusaoVisivel.value = false
}

async function confirmarExclusao() {
  erroExclusao.value = ''
  excluindo.value = true

  try {
    await deletarMinhaConta(conta.value.temSenha ? { senha: senhaExclusao.value } : null)
    logout()
    router.push('/login')
  } catch (e) {
    erroExclusao.value = extrairMensagemErro(e, 'Não foi possível excluir a conta.')
  } finally {
    excluindo.value = false
  }
}

async function iniciarGoogleVincular() {
  googleVincularIndisponivel.value = ''

  if (!getGoogleClientId()) {
    googleVincularIndisponivel.value = 'Defina VITE_GOOGLE_CLIENT_ID no frontend para habilitar a vinculação com Google.'
    return
  }

  try {
    await nextTick()
    await renderizarBotaoGoogle(googleVincularButtonRef.value, onGoogleVincularCredential)
  } catch (e) {
    googleVincularIndisponivel.value = e.message || 'Não foi possível carregar o botão do Google.'
    console.error(e)
  }
}

async function onGoogleVincularCredential(response) {
  if (!response?.credential) {
    erroGoogle.value = 'O Google não retornou um idToken válido.'
    return
  }

  mensagemGoogle.value = ''
  erroGoogle.value = ''

  try {
    conta.value = await vincularGoogle({ idToken: response.credential })
    atualizarSessaoComConta(conta.value)
    mensagemGoogle.value = 'Google vinculado com sucesso!'
  } catch (e) {
    erroGoogle.value = extrairMensagemErro(e, 'Não foi possível vincular o Google.')
    console.error(e)
  }
}

function abrirModalDesvinculoGoogle() {
  senhaDesvinculoGoogle.value = ''
  mostrarSenhaDesvinculoGoogle.value = false
  erroDesvinculoGoogle.value = ''
  modalDesvinculoGoogleVisivel.value = true
}

function fecharModalDesvinculoGoogle() {
  modalDesvinculoGoogleVisivel.value = false
  senhaDesvinculoGoogle.value = ''
  erroDesvinculoGoogle.value = ''
}

async function desvinculaGoogle() {
  erroDesvinculoGoogle.value = ''
  desvinculandoGoogle.value = true

  try {
    conta.value = await desvincularGoogle({ senha: senhaDesvinculoGoogle.value })
    atualizarSessaoComConta(conta.value)
    modalDesvinculoGoogleVisivel.value = false
    senhaDesvinculoGoogle.value = ''
    mensagemGoogle.value = 'Google desvinculado com sucesso!'
    await iniciarGoogleVincular()
  } catch (e) {
    erroDesvinculoGoogle.value = extrairMensagemErro(e, 'Não foi possível desvincular o Google.')
    console.error(e)
  } finally {
    desvinculandoGoogle.value = false
  }
}

// Renderiza o botão do Google sempre que o elemento de vínculo aparecer no DOM
// (ocorre após a conta ser carregada sem Google vinculado, ou após um desvínculo)
watch(googleVincularButtonRef, async (el) => {
  if (el && conta.value && !conta.value.temLoginGoogle) {
    await iniciarGoogleVincular()
  }
})

onMounted(async () => {
  limparMensagens()
  await carregarConta()
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 1050;
  padding: 1rem;
}
</style>
