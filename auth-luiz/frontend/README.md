# AuthLuiz — Frontend

Implementação de referência em Vue 3 + Vite para a API de autenticação AuthLuiz. Demonstra todos os fluxos disponíveis na API.

## Stack

- **Vue 3** (Composition API) + **Vite**
- **Vue Router** — guards de navegação, redirecionamento por estado
- **Axios** — cliente HTTP com injeção de token Bearer e logout automático em 401
- **Bootstrap 5** + **Bootstrap Icons**
- **Google Identity Services SDK** — login e vinculação com Google
- **qrcode** — geração de QR code para setup TOTP (importação dinâmica)

## Estrutura do projeto

```
src/
├── router/
│   └── index.js                  Guards: setup obrigatório, autenticação, redirecionamento
├── services/
│   ├── api.js                    Instância Axios com Bearer token e interceptor 401
│   │                             baseURL: VITE_API_BASE_URL se definida, senão http://localhost:8080
│   ├── autenticacaoService.js    Armazenamento de token, expiração, chamadas de auth (mesma lógica de baseURL)
│   │                             loginComStatus() retorna { status, data } para detectar 202 (login pendente)
│   │                             enviarCodigoExclusaoConta() — dispara OTP para confirmar exclusão (sem TOTP)
│   ├── googleIdentityService.js  Inicialização do SDK Google Identity Services
│   ├── loginPendenteService.js   verificar(), reenviar(), usarCodigoBackup() — 2º fator no login
│   ├── doisFatoresService.js     iniciarTotp(), confirmarTotp(), desativar2fa(), regerarBackupCodes()
│   │                             atualizarVerificacaoExtra(ativo, senha?), obterStatus()
│   │                             listarIpsConfiaveis(), removerIpConfiavel(), removerTodosIps()
│   └── setupService.js           Verificação e conclusão do setup inicial
├── utils/
│   └── extrairMensagemErro.js    Extrai mensagem de erro de respostas Axios
└── views/
    ├── LoginView.vue                  Login local (e-mail, telefone ou username) e login com Google
    │                                  Detecta resposta 202 e redireciona para /login/verificar
    ├── VerificacaoLoginView.vue       2º fator no login: TOTP, OTP (e-mail/SMS/WhatsApp) e backup code
    │                                  Checkbox "Confiar neste dispositivo" com nome opcional
    ├── CadastroView.vue               Cadastro com username, e-mail, senha e nomeExibicao (opcional)
    ├── ContaView.vue                  Gerenciamento da conta autenticada
    │                                  Inclui seção 2FA: setup TOTP em modal 3 etapas (QR → confirmar → backup codes)
    │                                  Preferência de canal (e-mail/SMS/WhatsApp), lista de IPs confiáveis
    ├── RecuperarSenhaView.vue         Solicitação de recuperação de senha (e-mail ou telefone)
    ├── RedefinirSenhaView.vue         Verificação do código de recuperação — chama POST /auth/recuperacao/validar; passa estado via history.state para NovaSenhaView
    ├── NovaSenhaView.vue              Definição da nova senha — lê estado de history.state; redireciona para /recuperar-senha se acessada diretamente
    ├── VerificacaoEmailView.vue       Confirmação de e-mail via código de 6 dígitos (cadastro e alteração)
    └── SetupView.vue                  Setup inicial da aplicação
```

## Variáveis de ambiente

Crie um arquivo `.env` na raiz do frontend com base no `.env.example`:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_PERM_LUIZ_URL=http://localhost:81
VITE_GOOGLE_CLIENT_ID=seu-client-id-do-google.apps.googleusercontent.com
```

### `VITE_API_BASE_URL` — comportamento por cenário

| Cenário | Valor | Comportamento |
|---------|-------|---------------|
| Dev local (`npm run dev`) | não definido | Axios usa `http://localhost:8080` — chama backend direto, sem proxy |
| Docker Compose (`compose.yaml`) | `""` (vazio, via build arg) | Axios usa URLs relativas → chamadas passam pelo nginx do container → nginx injeta `X-Real-IP` e proxia para backend |
| Deploy com URL personalizada | `https://api.seudominio.com` | Axios usa essa URL diretamente |

> O código usa `!== undefined` para distinguir "variável não definida" (usa fallback `http://localhost:8080`) de "variável definida como vazia" (usa string vazia = mesma origem). **Não alterar para `||`** — isso trataria a string vazia como falsy e ignoraria a configuração do Docker.

## Rodando

```bash
npm install

npm run dev      # servidor de desenvolvimento em http://localhost:5173
npm run build    # build de produção
npm run preview  # pré-visualização do build de produção
```

## Fluxos implementados

### Autenticação

- Cadastro com username, e-mail, senha, telefone (opcional) e nome de exibição (opcional)
- Login local (e-mail, telefone **ou** username + senha)
- Login com Google (Google Identity Services)
- **Verificação de 2º fator no login** — quando o backend retorna 202, redireciona automaticamente para `/login/verificar` com o `tokenPendente`
- **`VerificacaoLoginView`** — suporta TOTP (app autenticador), OTP (e-mail/SMS/WhatsApp com reenvio e cooldown de 60s) e código de backup (`XXXX-XXXX`); checkbox "Confiar neste dispositivo" com nome opcional
- Recuperação e redefinição de senha por e-mail ou telefone via código de 6 dígitos
- Cancelamento de token de recuperação via link no e-mail de alerta (canal telefone)

### Conta autenticada (`ContaView`)

- Visualização dos dados da conta (username, nomeExibicao, e-mail e telefone)
- Alteração de username (4–30 chars, validação de formato e palavras reservadas)
- Alteração de nome de exibição (opcional; usado em saudações de e-mail)
- Alteração de e-mail (sempre exige confirmação via código de 6 dígitos; bloqueada para contas com Google vinculado)
- Botão "Confirmar e-mail" no alerta de e-mail não verificado — envia o código sob demanda e redireciona para `/verificar-email`
- Botão "Confirmar alteração" no alerta de e-mail pendente — envia o código e redireciona para `/verificar-email?tipo=alteracao`
- Troca de senha
- Definição de senha para contas criadas via Google
- Atualização ou remoção de telefone
- Vinculação de conta Google (exige e-mail idêntico ao da conta)
- Desvinculação de conta Google com confirmação de senha (bloqueada para contas criadas via Google)
- Exclusão de conta com confirmação de senha; quando 2FA ativo: TOTP exige código do autenticador, e-mail/SMS exige envio de OTP via botão + código recebido
- Desativar verificação extra exige confirmação de senha (modal dedicado); contas sem senha desativam diretamente
- **Seção 2FA:** ativar/desativar TOTP via modal 3 etapas (QR code → confirmar código → backup codes exibidos uma vez), preferência de canal de verificação (e-mail/SMS/WhatsApp), contador de backup codes restantes, regerar backup codes
- **Seção IPs confiáveis:** lista de IPs com rótulo e data, remover individual ou todos
- **Botão "Painel de Controle"** — visível somente ao admin mestre do PermLuiz (verificado via `GET /me/admin`); abre o PermLuiz passando o JWT no fragment da URL (`#token=<jwt>`)

### Setup inicial

- Verificação de status do setup
- Configuração de SMTP via formulário guiado
- Redirecionamento automático enquanto o setup não estiver concluído

## Comportamento dos guards de rota

| Condição | Resultado |
|----------|-----------|
| Setup não concluído | Redireciona para `/setup` (qualquer rota) |
| Não autenticado em rota protegida | Redireciona para `/login` |
| Autenticado tentando acessar `/login` ou `/cadastro` | Redireciona para `/conta` |
| Login retorna 202 (IP desconhecido / 2FA) | `LoginView` redireciona para `/login/verificar` com `tokenPendente` na query |

## nginx como proxy reverso de API

O `nginx.conf` não apenas serve os arquivos estáticos do SPA — em produção Docker, também faz proxy das rotas de API:

```
/auth/*   →  set $backend http://backend:8080; proxy_pass $backend  +  X-Real-IP $remote_addr
/setup/*  →  set $backend http://backend:8080; proxy_pass $backend  +  X-Real-IP $remote_addr
```

**Por que isso importa:** sem o proxy, o browser chamaria `localhost:8080` diretamente e o backend receberia o IP interno do Docker (`172.17.0.1`), classificado como privado — a geolocalização nos e-mails de segurança retornaria vazia. Com o proxy, o IP real do cliente viaja no header `X-Real-IP` e o backend o extrai via `IpUtils.extrairIp()`.

**Por que usar `set $backend` em vez de `proxy_pass http://backend:8080` direto:** o nginx resolve hostnames de `proxy_pass` estático em tempo de startup. Em Docker Compose, se o DNS do `backend` ainda não estiver pronto nesse momento, o nginx falha ao iniciar e o container fica em crash — o browser mostra `chrome-error://chromewebdata/`. A combinação `resolver 127.0.0.11 valid=30s ipv6=off` + variável no `proxy_pass` força a resolução em tempo de requisição, eliminando essa dependência de timing.

Em dev (`npm run dev`), não há nginx e o frontend chama o backend direto — a geo não funciona localmente, o que é esperado.

## Observações

- O frontend é uma **implementação de referência**: o backend foi projetado para ser independente de qualquer frontend.
- Mensagens de erro exibidas ao usuário podem ser customizadas por qualquer implementação — o frontend não é obrigado a usar as mensagens retornadas pela API.
- O token JWT é armazenado no `localStorage` e injetado automaticamente em todas as requisições autenticadas via interceptor Axios.
