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
├── router/
│   └── index.js                  Guards: setup obrigatório, autenticação, redirecionamento
├── services/
│   ├── api.js                    Instância Axios apontando para o PermLuiz (:8081)
│   ├── autenticacaoService.js    Armazenamento e leitura do JWT (obtido via AuthLuiz)
│   └── setupService.js           Verificação e conclusão do setup inicial
└── views/
    ├── SetupView.vue             Setup inicial (define admin mestre via ID de usuário)
    ├── LoginView.vue             Login via AuthLuiz — armazena JWT localmente
    ├── MinhaContaView.vue        Exibe os próprios roles e permissões (GET /me/roles)
    ├── AdminRolesView.vue        CRUD de roles (admin mestre)
    ├── AdminPermissoesView.vue   CRUD de permissões (admin mestre)
    └── AdminUsuariosView.vue     Atribuir/remover roles de usuários (admin mestre)
```

## Variáveis de ambiente

Crie um arquivo `.env` na raiz do frontend com base no `.env.example`:

```env
VITE_API_BASE_URL=http://localhost:8081
VITE_AUTH_API_BASE_URL=http://localhost:8080
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

- Verificação de status do setup
- Definição do admin mestre por ID de usuário (usuário já deve existir no AuthLuiz)
- Redirecionamento automático enquanto o setup não estiver concluído

### Autenticação

- Login via AuthLuiz (e-mail + senha) — o JWT é armazenado localmente e usado em todas as requisições

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
| Setup não concluído | Redireciona para `/setup` (qualquer rota) |
| Não autenticado em rota protegida | Redireciona para `/login` |
| Autenticado tentando acessar `/login` | Redireciona para `/conta` |

## Observações

- O login é feito **diretamente no AuthLuiz** — o PermLuiz não emite tokens; apenas os valida.
- O token JWT é armazenado no `localStorage` e injetado automaticamente em todas as requisições autenticadas.
- Este frontend é uma **implementação de referência**: o backend foi projetado para ser independente de qualquer frontend.
