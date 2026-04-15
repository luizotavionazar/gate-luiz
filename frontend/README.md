# AuthLuiz — Frontend

Implementação de referência em Vue 3 + Vite para a API de autenticação AuthLuiz. Demonstra todos os fluxos disponíveis na API.

## Stack

- **Vue 3** (Composition API) + **Vite**
- **Vue Router** — guards de navegação, redirecionamento por estado
- **Axios** — cliente HTTP com injeção de token Bearer e logout automático em 401
- **Bootstrap 5** + **Bootstrap Icons**
- **Google Identity Services SDK** — login e vinculação com Google

## Estrutura do projeto

```
src/
├── router/
│   └── index.js                  Guards: setup obrigatório, autenticação, redirecionamento
├── services/
│   ├── api.js                    Instância Axios com Bearer token e interceptor 401
│   ├── autenticacaoService.js    Armazenamento de token, expiração, chamadas de auth
│   ├── googleIdentityService.js  Inicialização do SDK Google Identity Services
│   └── setupService.js           Verificação e conclusão do setup inicial
├── utils/
│   └── extrairMensagemErro.js    Extrai mensagem de erro de respostas Axios
└── views/
    ├── LoginView.vue             Login local e login com Google
    ├── CadastroView.vue          Cadastro com e-mail e senha
    ├── ContaView.vue             Gerenciamento da conta autenticada
    ├── RecuperarSenhaView.vue    Solicitação de recuperação de senha
    ├── RedefinirSenhaView.vue    Redefinição de senha via token
    ├── VerificacaoEmailView.vue  Confirmação de e-mail (cadastro e alteração)
    └── SetupView.vue             Setup inicial da aplicação
```

## Variáveis de ambiente

Crie um arquivo `.env` na raiz do frontend com base no `.env.example`:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_GOOGLE_CLIENT_ID=seu-client-id-do-google.apps.googleusercontent.com
```

## Rodando

```bash
npm install

npm run dev      # servidor de desenvolvimento em http://localhost:5173
npm run build    # build de produção
npm run preview  # pré-visualização do build de produção
```

## Fluxos implementados

### Autenticação

- Cadastro com e-mail e senha
- Login local (e-mail + senha)
- Login com Google (Google Identity Services)
- Recuperação e redefinição de senha por e-mail

### Conta autenticada (`ContaView`)

- Visualização dos dados da conta
- Alteração de nome
- Alteração de e-mail (sempre exige confirmação; bloqueada para contas com Google vinculado)
- Troca de senha local
- Definição de senha local para contas criadas via Google
- Reenvio de e-mail de verificação
- Vinculação de conta Google (exige e-mail idêntico ao da conta)
- Desvinculação de conta Google com confirmação de senha (bloqueada para contas criadas via Google)
- Exclusão de conta com confirmação de senha

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

## Observações

- O frontend é uma **implementação de referência**: o backend foi projetado para ser independente de qualquer frontend.
- Mensagens de erro exibidas ao usuário podem ser customizadas por qualquer implementação — o frontend não é obrigado a usar as mensagens retornadas pela API.
- O token JWT é armazenado no `localStorage` e injetado automaticamente em todas as requisições autenticadas via interceptor Axios.
