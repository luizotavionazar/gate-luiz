<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center py-5" style="background-color: #eef4ff;">
    <div class="card shadow border-0 rounded-4" style="width: 100%; max-width: 760px;">
      <div class="card-body p-4 p-md-5">
        <div class="text-center mb-4">
          <h1 class="h3 fw-bold mb-2">Configuração inicial</h1>
          <p class="text-muted mb-0">Preencha os dados de envio de e-mail para concluir o setup do AuthLuiz.</p>
        </div>

        <div v-if="bootstrapErro" class="alert alert-warning">{{ bootstrapErro }}</div>

        <form @submit.prevent="salvar">
          <div class="row gx-2">
            <div class="col-md-6 mb-3">
              <label class="form-label">Host SMTP</label>
              <input v-model="form.smtpHost" class="form-control" required />
            </div>
            <div class="col-md-6 mb-3">
              <label class="form-label">Porta SMTP</label>
              <input v-model.number="form.smtpPort" type="number" class="form-control" required />
            </div>
            <div class="col-md-6 mb-3">
              <label class="form-label">Usuário SMTP</label>
              <input v-model="form.smtpUsername" class="form-control" required />
            </div>
            <div class="col-md-6 mb-3">
              <label class="form-label">Senha SMTP</label>
              <input v-model="form.smtpPassword" type="password" class="form-control no-password-reveal" required />
            </div>
            <div class="col-md-6 mb-3">
              <label class="form-label">E-mail remetente</label>
              <input v-model="form.mailFrom" type="email" class="form-control" required />
            </div>
            <div class="col-md-6 mb-3">
              <label class="form-label">URL do frontend</label>
              <input v-model="form.frontendBaseUrl" class="form-control" placeholder="http://localhost:5173" required />
            </div>
            <div class="col-md-6 mb-3">
              <div class="form-check mt-0 pt-2">
                <input v-model="form.smtpAuth" class="form-check-input" type="checkbox" id="smtpAuth" />
                <label class="form-check-label" for="smtpAuth">SMTP exige autenticação</label>
              </div>
            </div>
            <div class="col-md-6 mb-3">
              <div class="form-check mt-0 pt-2">
                <input v-model="form.smtpStarttls" class="form-check-input" type="checkbox" id="smtpStarttls" />
                <label class="form-check-label" for="smtpStarttls">Usar STARTTLS</label>
              </div>
            </div>
            <div class="col-12 mb-3">
              <div class="form-check">
                <input v-model="form.confirmacaoEmailHabilitada" class="form-check-input" type="checkbox" id="confirmacaoEmailHabilitada" />
                <label class="form-check-label" for="confirmacaoEmailHabilitada">
                  Exigir confirmação de e-mail no cadastro e na alteração de e-mail
                </label>
              </div>
              <div class="form-text">
                Quando habilitado, novos usuários precisarão confirmar o e-mail antes de acessar recursos da conta.
                Contas não confirmadas são removidas automaticamente após 7 dias.
              </div>
            </div>
          </div>

          <div v-if="erro" class="alert alert-danger">{{ erro }}</div>
          <div v-if="sucesso" class="alert alert-success">{{ sucesso }}</div>

          <div class="d-grid">
            <button type="submit" class="btn btn-primary" :disabled="carregando || !bootstrapOk">{{ carregando ? 'Salvando...' : 'Concluir configuração' }}</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { obterSetup, obterStatusSetup, salvarSetup } from '../services/setupService'
import { extrairMensagemErro } from '../utils/extrairMensagemErro'

const router = useRouter()
const carregando = ref(false)
const erro = ref('')
const sucesso = ref('')
const bootstrapErro = ref('')
const bootstrapOk = ref(false)

const form = reactive({
  smtpHost: '',
  smtpPort: 587,
  smtpUsername: '',
  smtpPassword: '',
  mailFrom: '',
  frontendBaseUrl: 'http://localhost:5173',
  smtpAuth: true,
  smtpStarttls: true,
  confirmacaoEmailHabilitada: false
})

async function carregar() {
  try {
    const status = await obterStatusSetup()
    bootstrapOk.value = status.bootstrapOk

    if (status.setupConcluido) {
      router.replace('/login')
      return
    }

    if (!status.bootstrapOk) {
      bootstrapErro.value = 'Complete primeiro o arquivo .env da API e reinicie o backend.'
      return
    }

    const config = await obterSetup()
    form.smtpHost = config.smtpHost || ''
    form.smtpPort = config.smtpPort || 587
    form.smtpUsername = config.smtpUsername || ''
    form.mailFrom = config.mailFrom || ''
    form.frontendBaseUrl = config.frontendBaseUrl || 'http://localhost:5173'
    form.smtpAuth = config.smtpAuth ?? true
    form.smtpStarttls = config.smtpStarttls ?? true
  } catch (e) {
    erro.value = extrairMensagemErro(e, 'Não foi possível carregar o setup.')
  }
}

async function salvar() {
  erro.value = ''
  sucesso.value = ''
  carregando.value = true

  try {
    const response = await salvarSetup(form)
    sucesso.value = response.mensagem
    router.push('/login')
  } catch (e) {
    erro.value = extrairMensagemErro(e, 'Não foi possível salvar o setup.')
  } finally {
    carregando.value = false
  }
}

onMounted(carregar)
</script>
