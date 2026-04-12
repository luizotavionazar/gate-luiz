<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center py-5" style="background-color: #eef4ff;">
    <div class="card shadow border-0 rounded-4" style="width: 100%; max-width: 480px;">
      <div class="card-body p-4 p-md-5">
        <div class="text-center mb-4">
          <h1 class="h3 fw-bold mb-2">Vinculação Google</h1>
          <p class="text-muted mb-0">Mais um passo e você entra na sua conta.</p>
        </div>

        <div class="d-flex align-items-start gap-3">
          <div class="flex-grow-1">
            <h2 class="h6 fw-bold mb-2">Encontramos uma conta cadastrada com o mesmo e-mail</h2>
            <p class="small text-muted mb-3">
              Você pode entrar com Google e vincular essa conta agora. Se preferir, também pode voltar ao login comum
              ou recuperar a senha da conta local.
            </p>

            <div class="rounded-4 border bg-light-subtle p-3 small text-muted mb-3">
              O vínculo só será concluído porque o Google confirmou esse e-mail como verificado.
            </div>

            <div v-if="mensagemVinculo" class="alert alert-danger py-2 small">{{ mensagemVinculo }}</div>

            <div class="d-grid gap-2">
              <button class="btn btn-primary" :disabled="googleCarregando" @click="confirmarVinculoGoogle">
                {{ googleCarregando ? 'Vinculando...' : 'Entrar e vincular com Google' }}
              </button>

              <button type="button" class="btn btn-outline-primary" @click="voltarLogin" :disabled="googleCarregando">
                Entrar com e-mail e senha
              </button>

              <button type="button" class="btn btn-outline-secondary" @click="irParaRecuperacaoSenha" :disabled="googleCarregando">
                Recuperar senha
              </button>

              <button type="button" class="btn btn-link text-decoration-none" @click="cancelarVinculoGoogle" :disabled="googleCarregando">
                Cancelar
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  limparPendenciaVinculoGoogle,
  loginComGoogle,
  obterPendenciaVinculoGoogle,
  salvarSessao
} from '../services/autenticacaoService'
import { extrairMensagemErro } from '../utils/extrairMensagemErro'

const router = useRouter()
const googleCarregando = ref(false)
const mensagemVinculo = ref('')
const pendencia = ref(null)

function voltarLogin() {
  router.push('/login')
}

function irParaRecuperacaoSenha() {
  router.push('/recuperar-senha')
}

function cancelarVinculoGoogle() {
  limparPendenciaVinculoGoogle()
  voltarLogin()
}

async function confirmarVinculoGoogle() {
  if (!pendencia.value?.idToken) {
    mensagemVinculo.value = 'Faça o login com Google novamente para concluir a vinculação.'
    return
  }

  mensagemVinculo.value = ''
  googleCarregando.value = true

  try {
    const resposta = await loginComGoogle({
      idToken: pendencia.value.idToken,
      vincularContaExistente: true
    })

    limparPendenciaVinculoGoogle()
    salvarSessao(resposta)
    router.push('/conta')
  } catch (e) {
    mensagemVinculo.value = extrairMensagemErro(e, 'Não foi possível concluir a vinculação com Google.')
    console.error(e)
  } finally {
    googleCarregando.value = false
  }
}

onMounted(() => {
  pendencia.value = obterPendenciaVinculoGoogle()

  if (!pendencia.value?.idToken) {
    voltarLogin()
  }
})
</script>
