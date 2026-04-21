# PermLuiz — Frontend

Implementação de referência em Vue 3 + Vite para a API de autorização PermLuiz. O login é feito diretamente no AuthLuiz; o JWT obtido é usado aqui para acessar os endpoints de roles e permissões.

## Stack

- **Vue 3** (Composition API) + **Vite**
- **Vue Router** — guards de navegação, redirecionamento por estado
- **Axios** — cliente HTTP com injeção de token Bearer e logout automático em 401
- **Bootstrap 5** + **Bootstrap Icons**

## Estrutura do projeto

```
src/
├── main.js                       Lê token do fragment (#token=) antes do mount; limpa URL
├── router/
│   └── index.js                  Guards: autenticação, redirecionamento
├── services/
│   ├── api.js                    Instância Axios apontando para o PermLuiz (:8081)
│   ├── autenticacaoService.js    Armazenamento e leitura do JWT; salvarSessaoDoFragment()
│   └── setupService.js           Verificação de status do setup (adminConfigurado)
└── views/
    ├── SetupView.vue             Tela informativa — redireciona para o AuthLuiz
    ├── SemAcessoView.vue         Tela de acesso negado (403) — redireciona para o AuthLuiz
    ├── MinhaContaView.vue        Exibe os próprios roles e permissões (GET /me/roles)
    ├── AdminRolesView.vue        CRUD de roles (admin mestre)
    ├── AdminPermissoesView.vue   CRUD de permissões (admin mestre)
    └── AdminUsuariosView.vue     Atribuir/remover roles de usuários (admin mestre)
```

## Variáveis de ambiente

Crie um arquivo `.env` na raiz do frontend com base no `.env.example`:

```env
VITE_PERM_API_URL=http://localhost:8081
VITE_AUTH_LUIZ_URL=http://localhost:8080
```

## Rodando

```bash
npm install

npm run dev      # servidor de desenvolvimento em http://localhost:5174
npm run build    # build de produção
npm run preview  # pré-visualização do build de produção
```

## Fluxos implementados

### Setup inicial

- Verificação de `adminConfigurado` via `GET /setup`
- Tela informativa exibida enquanto nenhum admin está configurado, com botão para ir ao AuthLuiz
- O primeiro usuário autenticado a acessar um endpoint `/admin/**` é promovido automaticamente a admin mestre pelo backend

### Autenticação (SSO via URL fragment)

- O AuthLuiz abre o PermLuiz com o JWT no fragment da URL: `http://localhost:81#token=<jwt>`
- O `main.js` lê o fragment antes do mount, salva a sessão no `localStorage` via `salvarSessaoDoFragment()`, e limpa a URL com `history.replaceState` (o token nunca aparece no histórico nem é enviado ao servidor)
- Não há tela de login própria — a autenticação é delegada integralmente ao AuthLuiz

### Minha conta (`MinhaContaView`)

- Exibição dos roles atribuídos ao usuário autenticado
- Listagem das permissões de cada role

### Admin — Roles (`AdminRolesView`)

- Listagem de todos os roles
- Criação, edição e remoção de roles
- Gerenciamento das permissões de cada role

### Admin — Permissões (`AdminPermissoesView`)

- Listagem de todas as permissões (`recurso:acao`)
- Criação, edição e remoção de permissões

### Admin — Usuários (`AdminUsuariosView`)

- Consulta dos roles atribuídos a qualquer usuário pelo ID
- Atribuição e remoção de roles

## Comportamento dos guards de rota

| Condição | Resultado |
|----------|-----------|
| Não autenticado em rota protegida | Redireciona para `/setup` |
| Token expirado | Faz logout e redireciona para `/setup` |
| Resposta 401 (token inválido/expirado) | Logout e redireciona para o AuthLuiz |
| Resposta 403 (não é admin) | Redireciona para `/sem-acesso` |

## Proxy Nginx (produção)

O `nginx.conf` serve os arquivos estáticos do Vue e encaminha as rotas de API para o backend:

- Requisições para `/me/**`, `/admin/**` e `/setup/**` são repassadas via `proxy_pass` para `http://permluiz-backend:8080`
- Isso permite que o auth-luiz chame `http://localhost:81/me/admin` sem apontar diretamente para a porta 8081 do backend

## Observações

- Não há login próprio — a autenticação é feita no AuthLuiz e o JWT é passado via fragment (#token=).
- O token JWT é armazenado no `localStorage` e injetado automaticamente em todas as requisições autenticadas.
- Este frontend é uma **implementação de referência**: o backend foi projetado para ser independente de qualquer frontend.
