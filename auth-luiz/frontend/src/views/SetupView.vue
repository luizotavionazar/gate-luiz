<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center py-5" style="background-color: #eef4ff;">
    <div class="card shadow border-0 rounded-4" style="width: 100%; max-width: 760px;">
      <div class="card-body p-4 p-md-5">
        <div class="text-center mb-4">
          <h1 class="h3 fw-bold mb-2">Configuração inicial</h1>
          <p class="text-muted mb-0">Preencha os dados para concluir o setup do AuthLuiz.</p>
        </div>

        <div v-if="bootstrapErro" class="alert alert-warning">{{ bootstrapErro }}</div>

        <form @submit.prevent="salvar">
          <h6 class="fw-semibold text-muted mb-1">Entrega de E-mail</h6>
          <p class="text-muted small mb-3">Necessário para verificação de e-mail.</p>
          <div class="row gx-2">
            <div class="col-md-6 mb-3">
              <label class="form-label">Host SMTP</label>
              <input v-model="form.smtpHost" class="form-control" placeholder="smtp.example.com" required />
            </div>
            <div class="col-md-6 mb-3">
              <label class="form-label">Porta SMTP</label>
              <input v-model.number="form.smtpPort" type="number" class="form-control" placeholder="587" required />
            </div>
            <div class="col-md-6 mb-3">
              <label class="form-label">Usuário SMTP</label>
              <input v-model="form.smtpUsername" class="form-control" placeholder="seu_usuario@example.com" required />
            </div>
            <div class="col-md-6 mb-3">
              <label class="form-label">Senha SMTP</label>
              <input v-model="form.smtpPassword" type="password" class="form-control no-password-reveal" placeholder="sua_senha" required />
            </div>
            <div class="col-md-6 mb-3">
              <label class="form-label">E-mail remetente</label>
              <input v-model="form.mailFrom" type="email" class="form-control" placeholder="seu_email@example.com" required />
            </div>
            <div class="col-md-6 mb-3">
              <label class="form-label">URL do frontend</label>
              <input v-model="form.frontendBaseUrl" class="form-control" placeholder="http://localhost:5173" required />
            </div>
            <div class="col-md-6 mb-3">
              <div class="form-check mt-0 pt-2">
                <input v-model="form.smtpStarttls" class="form-check-input" type="checkbox" id="smtpStarttls" />
                <label class="form-check-label" for="smtpStarttls">Usar STARTTLS</label>
              </div>
            </div>
          </div>

          <hr class="my-3" />
          <h6 class="fw-semibold text-muted mb-1">Envio de WhatsApp e SMS [Twilio]</h6>
          <p class="text-muted small mb-3">Necessário para verificação de telefone.</p>
          <div class="row gx-2">
            <div class="col-md-6 mb-3">
              <label class="form-label">Account SID</label>
              <input v-model="form.twilioAccountSid" class="form-control no-password-reveal" type="password" placeholder="ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" autocomplete="off" required />
            </div>
            <div class="col-md-6 mb-3">
              <label class="form-label">Auth Token</label>
              <input v-model="form.twilioAuthToken" class="form-control no-password-reveal" type="password" placeholder="••••••••••••••••••••••••••••••••" autocomplete="off" required />
            </div>
            <div class="col-md-6 mb-3">
              <label class="form-label">Número de remetente</label>
              <input v-model="form.twilioFromNumber" class="form-control" placeholder="+5538998286294" required />
            </div>
            <div class="col-md-6 mb-3">
              <label class="form-label">Canal Principal</label>
              <select v-model="form.twilioCanal" class="form-select" required>
                <option value="whatsapp">WhatsApp</option>
                <option value="sms">SMS</option>
              </select>
            </div>
          </div>

          <div v-if="twilioConfigurado" class="alert alert-success py-2 small mb-3">
            <i class="bi bi-check-circle me-1"></i> Twilio já configurado. Preencha novamente apenas se quiser alterar as credenciais.
          </div>

          <hr class="my-3" />
          <h6 class="fw-semibold text-muted mb-3">Auditoria de logs</h6>
          <div class="row gx-2 align-items-end">
            <div class="col-md-6 mb-3">
              <label class="form-label">Tempo de Retenção (dias)</label>
              <input v-model.number="form.auditoriaRetencaoDias" type="number" min="1" max="3650" class="form-control" />
            </div>
            <div class="col-md-6 mb-3 d-flex align-items-center" style="padding-bottom: 7px;">
              <div class="form-check">
                <input v-model="form.auditoriaAtividade" class="form-check-input" type="checkbox" id="auditoriaAtividade" />
                <label class="form-check-label" for="auditoriaAtividade">Registrar logs de atividade</label>
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
const twilioConfigurado = ref(false)

const form = reactive({
  smtpHost: '',
  smtpPort: 587,
  smtpUsername: '',
  smtpPassword: '',
  mailFrom: '',
  frontendBaseUrl: 'http://localhost:5173',
  smtpStarttls: true,
  twilioAccountSid: '',
  twilioAuthToken: '',
  twilioFromNumber: '',
  twilioCanal: 'whatsapp',
  auditoriaAtividade: true,
  auditoriaRetencaoDias: 90
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
    form.smtpStarttls = config.smtpStarttls ?? true
    form.twilioFromNumber = config.twilioFromNumber || ''
    form.twilioCanal = config.twilioCanal || 'whatsapp'
    twilioConfigurado.value = config.twilioConfigurado ?? false
    form.auditoriaAtividade = config.auditoriaAtividade ?? true
    form.auditoriaRetencaoDias = config.auditoriaRetencaoDias ?? 90
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
