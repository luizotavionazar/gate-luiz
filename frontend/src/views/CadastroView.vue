<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center py-5" style="background-color: #eef4ff;">
    <div class="card shadow border-0 rounded-4" style="width: 100%; max-width: 720px;">
      <div class="card-body p-4 p-md-5">
        <div class="text-center mb-4">
          <h1 class="h3 fw-bold mb-2">Criar conta</h1>
          <p class="text-muted mb-0">Preencha os dados básicos para começar.</p>
        </div>

        <form @submit.prevent="enviarCadastro">
          <div class="row gx-2">
            <div class="col-md-12 mb-3">
              <label for="nome" class="form-label">Nome</label>
              <input id="nome" v-model="form.nome" type="text" class="form-control" placeholder="Seu nome" required />
            </div>

            <div class="col-md-12 mb-3">
              <label for="email" class="form-label">E-mail</label>
              <input id="email" v-model="form.email" type="email" class="form-control" placeholder="seuemail@exemplo.com" required />
            </div>

            <div class="col-md-6 mb-3">
              <label for="senha" class="form-label">Crie sua senha</label>
              <div class="position-relative">
                <input id="senha" v-model="form.senha" :type="mostrarSenha ? 'text' : 'password'" class="form-control pe-5 campo-senha" placeholder="Digite sua senha" @focus="senhaEmFoco = true" @blur="senhaEmFoco = false" required />
                <button type="button" class="btn btn-sm border-0 bg-transparent position-absolute top-50 end-0 translate-middle-y me-2 text-muted" @click="mostrarSenha = !mostrarSenha" :aria-label="mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'">
                  <i :class="mostrarSenha ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                </button>
              </div>

              <ul v-if="mostrarRegrasSenha" class="list-unstyled small mt-2 mb-0">
                <li :class="senhaRegras.tamanho ? 'text-success' : 'text-danger'">{{ senhaRegras.tamanho ? '✓' : '✕' }} Pelo menos 8 caracteres</li>
                <li :class="senhaRegras.maiuscula ? 'text-success' : 'text-danger'">{{ senhaRegras.maiuscula ? '✓' : '✕' }} Pelo menos 1 letra maiúscula</li>
                <li :class="senhaRegras.numero ? 'text-success' : 'text-danger'">{{ senhaRegras.numero ? '✓' : '✕' }} Pelo menos 1 número</li>
                <li :class="senhaRegras.especial ? 'text-success' : 'text-danger'">{{ senhaRegras.especial ? '✓' : '✕' }} Pelo menos 1 caractere especial</li>
              </ul>
            </div>

            <div class="col-md-6 mb-3">
              <label for="confSenha" class="form-label">Confirme a senha</label>
              <div class="position-relative">
                <input id="confSenha" v-model="form.confSenha" :type="mostrarConfSenha ? 'text' : 'password'" class="form-control pe-5 campo-senha" placeholder="Digite novamente sua senha" @focus="confirmacaoEmFoco = true" @blur="confirmacaoEmFoco = false" required />
                <button type="button" class="btn btn-sm border-0 bg-transparent position-absolute top-50 end-0 translate-middle-y me-2 text-muted" @click="mostrarConfSenha = !mostrarConfSenha" :aria-label="mostrarConfSenha ? 'Ocultar confirmação de senha' : 'Mostrar confirmação de senha'">
                  <i :class="mostrarConfSenha ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                </button>
              </div>

              <div v-if="mostrarValidacaoConfirmacao" class="small mt-2" :class="senhasCoincidem ? 'text-success' : 'text-danger'">
                {{ senhasCoincidem ? '✓ As senhas coincidem' : '✕ As senhas não coincidem' }}
              </div>
            </div>
          </div>

          <div v-if="erro" class="alert alert-danger mt-2" role="alert">{{ erro }}</div>
          <div v-if="sucesso" class="alert alert-success mt-2" role="alert">{{ sucesso }}</div>

          <div class="d-grid mt-3">
            <button type="submit" class="btn btn-primary" :disabled="carregando">{{ carregando ? 'Cadastrando...' : 'Cadastrar' }}</button>
          </div>

          <div class="text-center mt-3">
            <RouterLink to="/login" class="text-decoration-none">Já tem conta? Entrar</RouterLink>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { cadastrar } from '../services/autenticacaoService'
import { extrairMensagemErro } from '../utils/extrairMensagemErro'

const router = useRouter()
const form = reactive({ nome: '', email: '', senha: '', confSenha: '' })
const carregando = ref(false)
const erro = ref('')
const sucesso = ref('')
const senhaEmFoco = ref(false)
const confirmacaoEmFoco = ref(false)
const mostrarSenha = ref(false)
const mostrarConfSenha = ref(false)

const senhaRegras = computed(() => ({
  tamanho: form.senha.length >= 8,
  maiuscula: /\p{Lu}/u.test(form.senha),
  numero: /\d/u.test(form.senha),
  especial: /[^\p{L}\d\s]/u.test(form.senha)
}))

const senhaValida = computed(() => Object.values(senhaRegras.value).every(Boolean))
const senhasCoincidem = computed(() => form.confSenha.length > 0 && form.senha === form.confSenha)
const mostrarRegrasSenha = computed(() => senhaEmFoco.value || form.senha.length > 0)
const mostrarValidacaoConfirmacao = computed(() => confirmacaoEmFoco.value || form.confSenha.length > 0)

async function enviarCadastro() {
  erro.value = ''
  sucesso.value = ''

  if (!senhaValida.value) {
    erro.value = 'A senha ainda não atende aos requisitos.'
    return
  }

  if (!senhasCoincidem.value) {
    erro.value = 'As senhas não coincidem.'
    return
  }

  carregando.value = true

  try {
    await cadastrar({ nome: form.nome.trim(), email: form.email.trim(), senha: form.senha })
    sucesso.value = 'Cadastro realizado com sucesso! Redirecionando para o login...'
    setTimeout(() => router.push('/login'), 3200)
  } catch (e) {
    erro.value = extrairMensagemErro(e, 'Não foi possível realizar o cadastro.')
    console.error(e)
  } finally {
    carregando.value = false
  }
}
</script>
