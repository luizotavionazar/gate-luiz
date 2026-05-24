<template>
  <div class="min-vh-100 py-5" style="background-color: #eef4ff;">
    <div class="container" style="max-width: 920px;">
      <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
        <div>
          <h1 class="h3 fw-bold mb-1">Minha conta</h1>
          <p class="text-muted mb-0">Gerencie seus dados, suas formas de acesso e a senha da AuthLuiz.</p>
        </div>
        <div class="d-flex gap-2 align-self-start align-self-md-center">
          <button v-if="isPermAdmin" class="btn btn-outline-secondary" @click="abrirPermLuiz">Painel de Controle</button>
          <button class="btn btn-outline-danger" @click="sair">Sair</button>
        </div>
      </div>

      <div v-if="carregando" class="alert alert-info">Carregando conta...</div>
      <div v-else-if="erro" class="alert alert-danger">{{ erro }}</div>

      <template v-else-if="conta">
        <div v-if="!conta.emailVerificado" class="alert alert-warning mb-4">
          <div class="d-flex flex-column flex-sm-row align-items-sm-center gap-2">
            <div class="flex-grow-1">
              <strong>E-mail não verificado!</strong><br>
              Confirme seu e-mail para liberar todas as funcionalidades da conta.
            </div>
            <button class="btn btn-sm btn-warning flex-shrink-0" :disabled="solicitandoVerificacao" @click="solicitarVerificacao">
              {{ solicitandoVerificacao ? 'Enviando...' : 'Confirmar e-mail' }}
            </button>
          </div>
          <div v-if="erroSolicitarVerificacao" class="small mt-2 text-danger-emphasis">{{ erroSolicitarVerificacao }}</div>
        </div>

        <div v-if="conta.emailPendente" class="alert alert-info mb-4">
          <div class="d-flex flex-column flex-sm-row align-items-sm-center gap-2">
            <div class="flex-grow-1">
              <strong>Alteração de e-mail pendente!</strong><br>
              Confirme a alteração para <strong>{{ conta.emailPendente }}</strong>.
            </div>
            <button class="btn btn-sm btn-info flex-shrink-0" @click="solicitarConfirmacaoAlteracao">
              Confirmar alteração
            </button>
          </div>
        </div>

        <div v-if="conta.telefonePendente" class="alert alert-info mb-4">
          <div class="d-flex flex-column flex-sm-row align-items-sm-center gap-2">
            <div class="flex-grow-1">
              <strong>Alteração de telefone pendente!</strong><br>
              Confirme a alteração para <strong>{{ conta.telefonePendente }}</strong>.
            </div>
            <button class="btn btn-sm btn-info flex-shrink-0" @click="solicitarConfirmacaoAlteracaoTelefone">
              Confirmar alteração
            </button>
          </div>
        </div>

        <div v-else-if="conta.telefone && !conta.telefoneVerificado" class="alert alert-warning mb-4">
          <div class="d-flex flex-column flex-sm-row align-items-sm-center gap-2">
            <div class="flex-grow-1">
              <strong>Telefone não verificado!</strong><br>
              Confirme o número <strong>{{ conta.telefone }}</strong> para ativá-lo.
            </div>
            <button class="btn btn-sm btn-warning flex-shrink-0" :disabled="solicitandoVerificacaoTelefone" @click="solicitarVerificacaoTelefone">
              {{ solicitandoVerificacaoTelefone ? 'Enviando...' : 'Verificar telefone' }}
            </button>
          </div>
          <div v-if="erroSolicitarVerificacaoTelefone" class="small mt-2 text-danger-emphasis">{{ erroSolicitarVerificacaoTelefone }}</div>
        </div>

        <div class="card shadow border-0 rounded-4 mb-4">
          <div class="card-body p-4">
            <div class="d-flex flex-column flex-lg-row justify-content-between gap-3">
              <div>
                <h2 class="h5 mb-2">Resumo da conta</h2>
                <p class="mb-1"><strong>{{ conta.nome }}</strong></p>
                <p class="text-muted mb-0">{{ conta.email }}</p>
                <p class="text-muted mb-0">{{ conta.telefone }}</p>
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
                    <div v-if="conta.emailPendente" class="form-text text-warning-emphasis">
                      <i class="bi bi-clock me-1"></i>Aguardando confirmação.
                    </div>
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

          <div class="col-lg-6">
            <div class="card shadow border-0 rounded-4 h-100">
              <div class="card-body p-4">
                <h2 class="h5 mb-3">Alterar telefone</h2>
                <form @submit.prevent="salvarTelefone">
                  <div class="mb-3">
                    <label class="form-label">Telefone</label>
                    <TelefoneInput v-model="formTelefone.telefone" />
                    <div v-if="conta.telefonePendente" class="form-text text-warning-emphasis">
                      <i class="bi bi-clock me-1"></i>Aguardando confirmação.
                    </div>
                  </div>

                  <div v-if="mensagemTelefone" class="alert alert-success py-2 small">{{ mensagemTelefone }}</div>
                  <div v-if="erroTelefone" class="alert alert-danger py-2 small">{{ erroTelefone }}</div>

                  <div class="d-grid">
                    <button class="btn btn-primary" :disabled="salvandoTelefone || !conta.emailVerificado">
                      {{ salvandoTelefone ? 'Salvando...' : 'Salvar telefone' }}
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

          <!-- Seção 2FA -->
          <div class="col-12">
            <div class="card shadow border-0 rounded-4">
              <div class="card-body p-4">
                <h2 class="h5 mb-3">Autenticação 2FA</h2>

                <!-- Toggle verificação extra -->
                <div class="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-2">
                  <div>
                    <strong class="small">Verificação em dispositivos desconhecidos</strong>
                    <div class="text-muted small">Ao logar de um IP desconhecido, será solicitado um código de confirmação.</div>
                  </div>
                  <div class="form-check form-switch form-switch-lg ms-auto flex-shrink-0">
                    <input
                      class="form-check-input"
                      type="checkbox"
                      role="switch"
                      :checked="status2fa?.verificacaoExtraAtiva"
                      :disabled="carregando2fa || !conta.emailVerificado || status2fa?.totpAtivo"
                      :title="!conta.emailVerificado ? 'Confirme seu e-mail para ativar esta opção' : status2fa?.totpAtivo ? 'Desative o autenticador antes de desabilitar' : ''"
                      @change="toggleVerificacaoExtra"
                    />
                  </div>
                </div>

                <template v-if="status2fa?.verificacaoExtraAtiva">
                  <hr class="my-4" />

                  <div v-if="!status2fa?.totpAtivo" class="alert alert-info py-2 small mb-4">
                    <i class="bi bi-info-circle me-1"></i>
                    Sem autenticador ativo, a verificação usa seu e-mail ou telefone cadastrado.
                    Ative o autenticador abaixo para maior segurança.
                  </div>

                  <div class="mb-4">
                    <div class="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-2 mb-2">
                      <div>
                        <strong class="small">Aplicativo autenticador (TOTP)</strong>
                        <div class="text-muted small">
                          {{ status2fa?.totpAtivo ? 'Ativo — use Google Authenticator ou similar.' : 'Desativado.' }}
                        </div>
                      </div>
                      <div class="d-flex gap-2 flex-shrink-0">
                        <button v-if="!status2fa?.totpAtivo" class="btn btn-primary" @click="iniciarSetupTotp" :disabled="carregando2fa || !conta.emailVerificado" :title="!conta.emailVerificado ? 'Confirme seu e-mail para ativar o 2FA' : ''">
                          Ativar autenticador
                        </button>
                        <button v-else class="btn btn-sm btn-outline-danger" @click="abrirModalDesativar2fa" :disabled="carregando2fa">
                          Desativar
                        </button>
                      </div>
                    </div>

                    <template v-if="status2fa?.totpAtivo">
                      <div class="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-2 mt-3">
                        <div class="small text-muted">
                          Códigos de backup: <strong>{{ status2fa.codigosRestantes }}</strong> restantes
                        </div>
                        <button class="btn btn-sm btn-outline-secondary" @click="abrirModalRegerarBackup" :disabled="carregando2fa">
                          Regerar códigos de backup
                        </button>
                      </div>
                    </template>
                  </div>

                  <hr class="my-4" />

                  <div class="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-2 mb-3">
                    <div>
                      <strong class="small">Dispositivos confiáveis</strong>
                      <div class="text-muted small">Acessos destes IPs não exigem verificação adicional.</div>
                    </div>
                    <button v-if="ipsConfiaveis.length > 0" class="btn btn-sm btn-outline-danger flex-shrink-0" @click="removerTodosIps" :disabled="removendoIps">
                      Remover todos
                    </button>
                  </div>

                  <div class="d-flex gap-2 mb-3">
                    <input
                      v-model="rotuloIpAtual"
                      type="text"
                      class="form-control form-control-sm"
                      placeholder="Nome do dispositivo (opcional)"
                      maxlength="100"
                    />
                    <button class="btn btn-sm btn-outline-primary flex-shrink-0" @click="adicionarDispositivoAtual" :disabled="adicionandoIpAtual">
                      {{ adicionandoIpAtual ? 'Adicionando...' : 'Adicionar este dispositivo' }}
                    </button>
                  </div>

                  <div v-if="ipsConfiaveis.length === 0" class="text-muted small">Nenhum dispositivo confiável cadastrado.</div>

                  <ul v-else class="list-group list-group-flush">
                    <li v-for="ip in ipsConfiaveis" :key="ip.id" class="list-group-item px-0 d-flex justify-content-between align-items-center">
                      <div>
                        <code class="small">{{ ip.ip }}</code>
                        <span v-if="ip.rotulo" class="text-muted small ms-2">— {{ ip.rotulo }}</span>
                        <div class="text-muted" style="font-size: 11px;">{{ formatarDataHora(ip.criadoEm) }}</div>
                      </div>
                      <button class="btn btn-sm btn-outline-danger" @click="removerIp(ip.id)" :disabled="removendoIps">
                        <i class="bi bi-trash"></i>
                      </button>
                    </li>
                  </ul>

                  <div v-if="erroIps" class="alert alert-danger py-2 small mt-3">{{ erroIps }}</div>
                </template>

                <div v-if="mensagem2fa" class="alert alert-success py-2 small mt-3">{{ mensagem2fa }}</div>
                <div v-if="erro2fa" class="alert alert-danger py-2 small mt-3">{{ erro2fa }}</div>
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

  <!-- Modal setup TOTP -->
  <div v-if="modalTotpVisivel" class="modal-overlay d-flex align-items-center justify-content-center">
    <div class="card shadow border-0 rounded-4" style="width: 100%; max-width: 500px;">
      <div class="card-body p-4">
        <h2 class="h5 fw-bold mb-1">Ativar autenticador</h2>

        <template v-if="etapaTotp === 1">
          <p class="text-muted small mb-3">Escaneie o QR code com Google Authenticator, Authy ou similar.</p>
          <div v-if="qrDataUrl" class="text-center mb-3">
            <img :src="qrDataUrl" alt="QR Code TOTP" style="width: 200px; height: 200px;" />
          </div>
          <div v-else class="text-center text-muted small mb-3">Gerando QR Code...</div>
          <div class="d-flex gap-2 justify-content-end">
            <button class="btn btn-outline-secondary" @click="fecharModalTotp">Cancelar</button>
            <button class="btn btn-primary" @click="etapaTotp = 2">Próximo</button>
          </div>
        </template>

        <template v-else-if="etapaTotp === 2">
          <p class="text-muted small mb-3">Digite o código de 6 dígitos do aplicativo para confirmar.</p>
          <div class="mb-3">
            <CodigoInput v-model="codigoTotp" @submit="confirmarSetupTotp" />
          </div>
          <div v-if="erroTotp" class="alert alert-danger py-2 small mb-3">{{ erroTotp }}</div>
          <div class="d-flex gap-2 justify-content-end">
            <button class="btn btn-outline-secondary" @click="etapaTotp = 1" :disabled="confirmandoTotp">Voltar</button>
            <button class="btn btn-primary" @click="confirmarSetupTotp" :disabled="confirmandoTotp || codigoTotp.length < 6">
              {{ confirmandoTotp ? 'Confirmando...' : 'Confirmar' }}
            </button>
          </div>
        </template>

        <template v-else-if="etapaTotp === 3">
          <p class="text-muted small mb-2">TOTP ativado! Guarde estes códigos de backup em local seguro. Eles só aparecem uma vez.</p>
          <div class="bg-light rounded p-3 mb-3 font-monospace small">
            <div v-for="(c, i) in codigosBackup" :key="i">{{ c }}</div>
          </div>
          <div class="d-grid">
            <button class="btn btn-primary" @click="fecharModalTotp">Concluir</button>
          </div>
        </template>
      </div>
    </div>
  </div>

  <!-- Modal desativar 2FA -->
  <div v-if="modalDesativar2faVisivel" class="modal-overlay d-flex align-items-center justify-content-center">
    <div class="card shadow border-0 rounded-4" style="width: 100%; max-width: 460px;">
      <div class="card-body p-4">
        <h2 class="h5 fw-bold mb-1">Desativar autenticador</h2>
        <p class="text-muted small mb-3">Confirme sua senha para desativar o TOTP.</p>
        <div class="mb-3">
          <input v-model="senhaDesativar2fa" type="password" class="form-control" placeholder="Sua senha atual" />
        </div>
        <div v-if="erroDesativar2fa" class="alert alert-danger py-2 small mb-3">{{ erroDesativar2fa }}</div>
        <div class="d-flex gap-2 justify-content-end">
          <button class="btn btn-outline-secondary" @click="modalDesativar2faVisivel = false" :disabled="desativando2fa">Cancelar</button>
          <button class="btn btn-danger" @click="confirmarDesativar2fa" :disabled="desativando2fa || !senhaDesativar2fa">
            {{ desativando2fa ? 'Desativando...' : 'Desativar' }}
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- Modal regerar backup codes -->
  <div v-if="modalRegerarBackupVisivel" class="modal-overlay d-flex align-items-center justify-content-center">
    <div class="card shadow border-0 rounded-4" style="width: 100%; max-width: 460px;">
      <div class="card-body p-4">
        <template v-if="!codigosBackupRegenerados.length">
          <h2 class="h5 fw-bold mb-1">Regerar códigos de backup</h2>
          <p class="text-muted small mb-3">Os códigos antigos serão invalidados. Confirme com seu código TOTP.</p>
          <div class="mb-3">
            <CodigoInput v-model="codigoTotpRegerar" @submit="confirmarRegerarBackup" />
          </div>
          <div v-if="erroRegerarBackup" class="alert alert-danger py-2 small mb-3">{{ erroRegerarBackup }}</div>
          <div class="d-flex gap-2 justify-content-end">
            <button class="btn btn-outline-secondary" @click="fecharModalRegerarBackup" :disabled="regenerandoBackup">Cancelar</button>
            <button class="btn btn-primary" @click="confirmarRegerarBackup" :disabled="regenerandoBackup || codigoTotpRegerar.length < 6">
              {{ regenerandoBackup ? 'Gerando...' : 'Regerar' }}
            </button>
          </div>
        </template>
        <template v-else>
          <h2 class="h5 fw-bold mb-1">Novos códigos de backup</h2>
          <p class="text-muted small mb-2">Guarde em local seguro. Eles só aparecem uma vez.</p>
          <div class="bg-light rounded p-3 mb-3 font-monospace small">
            <div v-for="(c, i) in codigosBackupRegenerados" :key="i">{{ c }}</div>
          </div>
          <div class="d-grid">
            <button class="btn btn-primary" @click="fecharModalRegerarBackup">Concluir</button>
          </div>
        </template>
      </div>
    </div>
  </div>

  <!-- Modal desativar verificação extra -->
  <div v-if="modalDesativarVerificacaoExtraVisivel" class="modal-overlay d-flex align-items-center justify-content-center">
    <div class="card shadow border-0 rounded-4" style="width: 100%; max-width: 420px;">
      <div class="card-body p-4">
        <h2 class="h5 fw-bold mb-1">Desativar verificação extra</h2>
        <p class="text-muted small mb-4">Confirme sua senha para desativar a proteção por dois fatores.</p>

        <div class="mb-4">
          <label class="form-label">Senha atual:</label>
          <div class="position-relative">
            <input
              v-model="senhaDesativarVerificacaoExtra"
              :type="mostrarSenhaDesativarVerificacaoExtra ? 'text' : 'password'"
              class="form-control pe-5 campo-senha"
              placeholder="Digite sua senha"
              @keyup.enter="confirmarDesativarVerificacaoExtra"
              :disabled="desativandoVerificacaoExtra"
            />
            <button type="button" class="btn btn-sm border-0 bg-transparent position-absolute top-50 end-0 translate-middle-y me-2 text-muted" @click="mostrarSenhaDesativarVerificacaoExtra = !mostrarSenhaDesativarVerificacaoExtra" :aria-label="mostrarSenhaDesativarVerificacaoExtra ? 'Ocultar senha' : 'Mostrar senha'">
              <i :class="mostrarSenhaDesativarVerificacaoExtra ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
            </button>
          </div>
        </div>

        <div v-if="erroDesativarVerificacaoExtra" class="alert alert-danger py-2 small mb-3">{{ erroDesativarVerificacaoExtra }}</div>

        <div class="d-flex gap-2 justify-content-end">
          <button class="btn btn-outline-secondary" @click="modalDesativarVerificacaoExtraVisivel = false" :disabled="desativandoVerificacaoExtra">Cancelar</button>
          <button
            class="btn btn-warning"
            @click="confirmarDesativarVerificacaoExtra"
            :disabled="desativandoVerificacaoExtra || !senhaDesativarVerificacaoExtra"
          >
            {{ desativandoVerificacaoExtra ? 'Desativando...' : 'Confirmar' }}
          </button>
        </div>
      </div>
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

        <!-- Verificação 2FA (quando ativa) -->
        <template v-if="status2fa?.verificacaoExtraAtiva">
          <hr class="my-3" />
          <p class="small fw-semibold mb-2">Verificação em dois fatores</p>

          <!-- TOTP ativo: digitar código do app -->
          <template v-if="status2fa?.totpAtivo">
            <label class="form-label small text-muted mb-1">Código do autenticador (6 dígitos):</label>
            <CodigoInput v-model="codigoExclusao" :disabled="excluindo" class="mb-3" />
          </template>

          <!-- Email/SMS: enviar código primeiro -->
          <template v-else>
            <template v-if="!codigoExclusaoEnviado">
              <button
                class="btn btn-outline-primary btn-sm w-100 mb-2"
                @click="enviarCodigoParaExclusao"
                :disabled="enviandoCodigoExclusao"
              >
                {{ enviandoCodigoExclusao ? 'Enviando...' : 'Enviar código de verificação' }}
              </button>
            </template>
            <template v-else>
              <p class="small text-muted mb-2">
                Código enviado para <strong>{{ exclusaoDestinoMascarado }}</strong>.
              </p>
              <CodigoInput v-model="codigoExclusao" :disabled="excluindo" class="mb-3" />
            </template>
          </template>
        </template>

        <div v-if="erroExclusao" class="alert alert-danger py-2 small mb-3">{{ erroExclusao }}</div>

        <div class="d-flex gap-2 justify-content-end">
          <button class="btn btn-outline-secondary" @click="fecharModalExclusao" :disabled="excluindo">Cancelar</button>
          <button
            class="btn btn-danger"
            @click="confirmarExclusao"
            :disabled="excluindo
              || (conta.temSenha ? !senhaExclusao : !confirmouLeitura)
              || (status2fa?.verificacaoExtraAtiva && codigoExclusao.length < 6)
              || (status2fa?.verificacaoExtraAtiva && !status2fa?.totpAtivo && !codigoExclusaoEnviado)"
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
import api from '../services/api'
import {
  atualizarMeuEmail,
  atualizarMeuNome,
  atualizarMeuTelefone,
  atualizarMinhaSenha,
  atualizarSessaoComConta,
  buscarMinhaConta,
  deletarMinhaConta,
  enviarCodigoExclusaoConta,
  desvincularGoogle,
  fazerLogout,
  getToken,
  logout,
  marcarSenhaNaSessao,
  reenviarVerificacao,
  reenviarVerificacaoTelefone,
  verificarSePermAdmin,
  vincularGoogle
} from '../services/autenticacaoService'
import { getGoogleClientId, renderizarBotaoGoogle } from '../services/googleIdentityService'
import { extrairMensagemErro } from '../utils/extrairMensagemErro'
import TelefoneInput from '../components/TelefoneInput.vue'
import CodigoInput from '../components/CodigoInput.vue'
import {
  atualizarVerificacaoExtra,
  confirmarTotp,
  desativar2fa,
  iniciarTotp,
  adicionarIpAtual,
  listarIpsConfiaveis,
  obterStatus,
  regerarBackupCodes,
  removerIpConfiavel,
  removerTodosIps as removerTodosIpsService
} from '../services/doisFatoresService'

const router = useRouter()
const conta = ref(null)
const carregando = ref(true)
const erro = ref('')

const formNome = reactive({ nome: '' })
const formEmail = reactive({ email: '' })
const formSenha = reactive({ senhaAtual: '', novaSenha: '', confirmacao: '' })
const formTelefone = reactive({ telefone: '' })

const salvandoNome = ref(false)
const salvandoEmail = ref(false)
const salvandoSenha = ref(false)
const salvandoTelefone = ref(false)

const mensagemNome = ref('')
const mensagemEmail = ref('')
const mensagemSenha = ref('')
const mensagemTelefone = ref('')
const erroNome = ref('')
const erroEmail = ref('')
const erroSenha = ref('')
const erroTelefone = ref('')

const solicitandoVerificacao = ref(false)
const erroSolicitarVerificacao = ref('')

const solicitandoVerificacaoTelefone = ref(false)
const erroSolicitarVerificacaoTelefone = ref('')

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
const codigoExclusao = ref('')
const exclusaoTokenPendente = ref('')
const exclusaoDestinoMascarado = ref('')
const enviandoCodigoExclusao = ref(false)
const codigoExclusaoEnviado = ref(false)

// 2FA state
const status2fa = ref(null)
const carregando2fa = ref(false)
const mensagem2fa = ref('')
const erro2fa = ref('')

const modalDesativarVerificacaoExtraVisivel = ref(false)
const senhaDesativarVerificacaoExtra = ref('')
const mostrarSenhaDesativarVerificacaoExtra = ref(false)
const erroDesativarVerificacaoExtra = ref('')
const desativandoVerificacaoExtra = ref(false)

const modalTotpVisivel = ref(false)
const etapaTotp = ref(1)
const qrDataUrl = ref('')
const codigoTotp = ref('')
const erroTotp = ref('')
const confirmandoTotp = ref(false)
const codigosBackup = ref([])

const modalDesativar2faVisivel = ref(false)
const senhaDesativar2fa = ref('')
const erroDesativar2fa = ref('')
const desativando2fa = ref(false)

const modalRegerarBackupVisivel = ref(false)
const codigoTotpRegerar = ref('')
const erroRegerarBackup = ref('')
const regenerandoBackup = ref(false)
const codigosBackupRegenerados = ref([])

// IPs confiáveis state
const ipsConfiaveis = ref([])
const removendoIps = ref(false)
const adicionandoIpAtual = ref(false)
const rotuloIpAtual = ref('')
const erroIps = ref('')

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
  formEmail.email = conta.value?.emailPendente || conta.value?.email || ''
  formTelefone.telefone = conta.value?.telefonePendente || conta.value?.telefone || ''
}

function limparMensagens() {
  mensagemNome.value = ''
  mensagemEmail.value = ''
  mensagemSenha.value = ''
  mensagemTelefone.value = ''
  erroNome.value = ''
  erroEmail.value = ''
  erroSenha.value = ''
  erroTelefone.value = ''
}

function formatarDataHora(data) {
  if (!data) return 'Não informado'
  return new Date(data).toLocaleString('pt-BR')
}

const PERM_LUIZ_URL = import.meta.env.VITE_PERM_LUIZ_URL || 'http://localhost:81'
const isPermAdmin = ref(false)

async function verificarSeEAdmin() {
  try {
    const data = await verificarSePermAdmin()
    isPermAdmin.value = data?.isAdmin === true
  } catch {
    isPermAdmin.value = false
  }
}

function abrirPermLuiz() {
  window.open(`${PERM_LUIZ_URL}#token=${getToken()}`, '_blank')
}

async function sair() {
  await fazerLogout()
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
      router.push({ path: '/verificar-email', query: { tipo: 'alteracao' } })
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

async function salvarTelefone() {
  mensagemTelefone.value = ''
  erroTelefone.value = ''
  salvandoTelefone.value = true

  const telefoneParaEnviar = formTelefone.telefone.trim() || null

  try {
    conta.value = await atualizarMeuTelefone({ telefone: telefoneParaEnviar })
    atualizarSessaoComConta(conta.value)
    preencherFormularios()
    if (conta.value.telefonePendente) {
      router.push('/verificar-telefone')
    } else {
      mensagemTelefone.value = 'Telefone removido com sucesso!'
    }
  } catch (e) {
    erroTelefone.value = extrairMensagemErro(e, 'Não foi possível atualizar o telefone.')
    console.error(e)
  } finally {
    salvandoTelefone.value = false
  }
}

async function solicitarVerificacao() {
  erroSolicitarVerificacao.value = ''
  solicitandoVerificacao.value = true
  try {
    await reenviarVerificacao()
    router.push('/verificar-email')
  } catch (e) {
    erroSolicitarVerificacao.value = extrairMensagemErro(e, 'Não foi possível enviar o código de verificação.')
  } finally {
    solicitandoVerificacao.value = false
  }
}

function solicitarConfirmacaoAlteracao() {
  router.push({ path: '/verificar-email', query: { tipo: 'alteracao' } })
}

function solicitarConfirmacaoAlteracaoTelefone() {
  router.push('/verificar-telefone')
}

async function solicitarVerificacaoTelefone() {
  erroSolicitarVerificacaoTelefone.value = ''
  solicitandoVerificacaoTelefone.value = true
  try {
    await reenviarVerificacaoTelefone()
    router.push('/verificar-telefone')
  } catch (e) {
    erroSolicitarVerificacaoTelefone.value = extrairMensagemErro(e, 'Não foi possível enviar o código de verificação.')
  } finally {
    solicitandoVerificacaoTelefone.value = false
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
  codigoExclusao.value = ''
  exclusaoTokenPendente.value = ''
  exclusaoDestinoMascarado.value = ''
  enviandoCodigoExclusao.value = false
  codigoExclusaoEnviado.value = false
  modalExclusaoVisivel.value = true
}

function fecharModalExclusao() {
  modalExclusaoVisivel.value = false
}

async function enviarCodigoParaExclusao() {
  erroExclusao.value = ''
  enviandoCodigoExclusao.value = true
  try {
    const resp = await enviarCodigoExclusaoConta()
    exclusaoTokenPendente.value = resp.tokenPendente
    exclusaoDestinoMascarado.value = resp.destinoMascarado
    codigoExclusaoEnviado.value = true
  } catch (e) {
    erroExclusao.value = extrairMensagemErro(e, 'Não foi possível enviar o código.')
  } finally {
    enviandoCodigoExclusao.value = false
  }
}

async function confirmarExclusao() {
  erroExclusao.value = ''
  excluindo.value = true

  try {
    const payload = conta.value.temSenha ? { senha: senhaExclusao.value } : null
    if (status2fa.value?.verificacaoExtraAtiva) {
      const dados = payload ?? {}
      dados.codigo = codigoExclusao.value
      if (!status2fa.value?.totpAtivo) {
        dados.tokenPendente = exclusaoTokenPendente.value
      }
      await deletarMinhaConta(dados)
    } else {
      await deletarMinhaConta(payload)
    }
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

// 2FA functions
async function carregarStatus2fa() {
  try {
    status2fa.value = await obterStatus()
  } catch { /* silently fail */ }
}

async function toggleVerificacaoExtra(event) {
  const ativo = event.target.checked
  erro2fa.value = ''
  mensagem2fa.value = ''

  if (!ativo && conta.value?.temSenha) {
    event.target.checked = true
    senhaDesativarVerificacaoExtra.value = ''
    mostrarSenhaDesativarVerificacaoExtra.value = false
    erroDesativarVerificacaoExtra.value = ''
    modalDesativarVerificacaoExtraVisivel.value = true
    return
  }

  carregando2fa.value = true
  try {
    await atualizarVerificacaoExtra(ativo)
    await carregarStatus2fa()
    if (ativo) await carregarIpsConfiaveis()
    mensagem2fa.value = ativo ? 'Verificação extra ativada.' : 'Verificação extra desativada.'
  } catch (e) {
    erro2fa.value = extrairMensagemErro(e, 'Não foi possível alterar a configuração.')
    event.target.checked = !ativo
  } finally {
    carregando2fa.value = false
  }
}

async function confirmarDesativarVerificacaoExtra() {
  erroDesativarVerificacaoExtra.value = ''
  desativandoVerificacaoExtra.value = true
  try {
    await atualizarVerificacaoExtra(false, senhaDesativarVerificacaoExtra.value)
    await carregarStatus2fa()
    modalDesativarVerificacaoExtraVisivel.value = false
    mensagem2fa.value = 'Verificação extra desativada.'
  } catch (e) {
    erroDesativarVerificacaoExtra.value = extrairMensagemErro(e, 'Não foi possível desativar.')
  } finally {
    desativandoVerificacaoExtra.value = false
  }
}

async function iniciarSetupTotp() {
  erro2fa.value = ''
  carregando2fa.value = true
  try {
    const { otpauthUri } = await iniciarTotp()
    const QRCode = (await import('qrcode')).default
    qrDataUrl.value = await QRCode.toDataURL(otpauthUri)
    codigoTotp.value = ''
    erroTotp.value = ''
    etapaTotp.value = 1
    modalTotpVisivel.value = true
  } catch (e) {
    erro2fa.value = extrairMensagemErro(e, 'Não foi possível iniciar o setup TOTP.')
  } finally {
    carregando2fa.value = false
  }
}

async function confirmarSetupTotp() {
  erroTotp.value = ''
  confirmandoTotp.value = true
  try {
    const { codigosBackup: codigos } = await confirmarTotp(codigoTotp.value)
    codigosBackup.value = codigos
    etapaTotp.value = 3
    await carregarStatus2fa()
  } catch (e) {
    erroTotp.value = extrairMensagemErro(e, 'Código inválido.')
    codigoTotp.value = ''
  } finally {
    confirmandoTotp.value = false
  }
}

function fecharModalTotp() {
  modalTotpVisivel.value = false
  codigosBackup.value = []
  codigoTotp.value = ''
  qrDataUrl.value = ''
}

function abrirModalDesativar2fa() {
  senhaDesativar2fa.value = ''
  erroDesativar2fa.value = ''
  modalDesativar2faVisivel.value = true
}

async function confirmarDesativar2fa() {
  erroDesativar2fa.value = ''
  desativando2fa.value = true
  try {
    await desativar2fa(senhaDesativar2fa.value)
    modalDesativar2faVisivel.value = false
    mensagem2fa.value = 'Autenticação 2FA desativada.'
    await carregarStatus2fa()
  } catch (e) {
    erroDesativar2fa.value = extrairMensagemErro(e, 'Não foi possível desativar.')
  } finally {
    desativando2fa.value = false
  }
}

function abrirModalRegerarBackup() {
  codigoTotpRegerar.value = ''
  erroRegerarBackup.value = ''
  codigosBackupRegenerados.value = []
  modalRegerarBackupVisivel.value = true
}

function fecharModalRegerarBackup() {
  modalRegerarBackupVisivel.value = false
  codigosBackupRegenerados.value = []
}

async function confirmarRegerarBackup() {
  erroRegerarBackup.value = ''
  regenerandoBackup.value = true
  try {
    const { codigosBackup: codigos } = await regerarBackupCodes(codigoTotpRegerar.value)
    codigosBackupRegenerados.value = codigos
    await carregarStatus2fa()
  } catch (e) {
    erroRegerarBackup.value = extrairMensagemErro(e, 'Código inválido.')
    codigoTotpRegerar.value = ''
  } finally {
    regenerandoBackup.value = false
  }
}

// IPs confiáveis functions
async function adicionarDispositivoAtual() {
  adicionandoIpAtual.value = true
  erroIps.value = ''
  try {
    await adicionarIpAtual(rotuloIpAtual.value.trim() || null)
    rotuloIpAtual.value = ''
    await carregarIpsConfiaveis()
  } catch (e) {
    erroIps.value = extrairMensagemErro(e, 'Não foi possível adicionar o dispositivo.')
  } finally {
    adicionandoIpAtual.value = false
  }
}

async function carregarIpsConfiaveis() {
  try {
    ipsConfiaveis.value = await listarIpsConfiaveis()
  } catch { /* silently fail */ }
}

async function removerIp(id) {
  removendoIps.value = true
  erroIps.value = ''
  try {
    await removerIpConfiavel(id)
    await carregarIpsConfiaveis()
  } catch (e) {
    erroIps.value = extrairMensagemErro(e, 'Não foi possível remover o IP.')
  } finally {
    removendoIps.value = false
  }
}

async function removerTodosIps() {
  removendoIps.value = true
  erroIps.value = ''
  try {
    await removerTodosIpsService()
    ipsConfiaveis.value = []
  } catch (e) {
    erroIps.value = extrairMensagemErro(e, 'Não foi possível remover os IPs.')
  } finally {
    removendoIps.value = false
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
  verificarSeEAdmin()
  carregarStatus2fa()
  carregarIpsConfiaveis()
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

.form-switch-lg .form-check-input {
  width: 3em;
  height: 1.5em;
  cursor: pointer;
}

</style>
