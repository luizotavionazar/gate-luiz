# CLAUDE.md

Este arquivo fornece orientações ao Claude Code (claude.ai/code) ao trabalhar com o código deste repositório.

## Visão Geral do Projeto

PermLuiz é uma API de controle de acesso (roles e permissões) construída com Spring Boot (backend) e Vue 3 (frontend). Complementa o AuthLuiz: valida JWTs emitidos por ele via JWKS e gerencia quais recursos cada usuário pode acessar. Não emite tokens, não faz autenticação — essa responsabilidade é do AuthLuiz.

## Ecossistema de Serviços

| Serviço | Repositório | Descrição |
|---------|-------------|-----------|
| **AuthLuiz** | `github.com/luizotavionazar/auth-luiz` | Identidade e autenticação — emite JWTs RS256, expõe JWKS |
| **PermLuiz** | `github.com/luizotavionazar/perm-luiz` | Roles, permissões e controle de acesso (este repo) |
| **LuizStack** | `github.com/luizotavionazar/luiz-stack` | Orquestração Docker dos dois serviços |

O PermLuiz valida JWTs automaticamente buscando a chave pública do AuthLuiz via `AUTH_LUIZ_JWKS_URI`. O Spring Security cacheia essa chave — não há segredo compartilhado.

## Configuração do Ambiente

### Pré-requisitos

- Java 21, Maven
- Node.js + npm
- Docker
- AuthLuiz em execução (para validação de JWTs)

### Backend

```bash
cp backend/.env.example backend/.env
# Edite backend/.env com credenciais do banco, master key e URI do JWKS

# Subir banco de desenvolvimento
docker compose -f compose-dev.yaml up -d

# Rodar o backend (porta 8080 internamente, mapeada para 8081 no luiz-stack)
cd backend
./mvnw spring-boot:run

# Rodar todos os testes
./mvnw test

# Rodar uma única classe de teste
./mvnw test -Dtest=NomeDaClasse

# Gerar o JAR
./mvnw package -DskipTests
```

### Frontend

```bash
cd frontend
npm install
npm run dev      # servidor de desenvolvimento em http://localhost:5174
npm run build    # build de produção
npm run preview  # pré-visualização do build de produção
```

## Arquitetura

### Backend (`backend/src/main/java/br/com/luizotavionazar/permluiz/`)

**Organização de pacotes:**

- `api/` — Controllers e DTOs, agrupados por feature (`admin`, `setup`, `usuario`)
- `domain/` — Entidades JPA e repositórios, agrupados por domínio (`configuracao`, `role`, `permissao`, `usuariorole`)
- `config/` — Configurações Spring: `security/` (JWKS, admin verificador) e `setup/` (SetupFilter)

**Fluxos principais:**

- **Guarda de setup:** `SetupFilter` retorna 503 com JSON para qualquer requisição se `configuracaoAplicacao.setupConcluido = false`. O setup é concluído via `POST /setup` usando `APP_SETUP_MASTER_KEY`, que recebe `{ "idUsuario": N }` e define o admin mestre.
- **Validação de JWT:** Spring Security busca e cacheia a chave pública do AuthLuiz via `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`. Todos os endpoints (exceto `/setup/**` e `/error`) exigem JWT válido. O `subject` do JWT é o `id` do usuário no AuthLuiz (como String).
- **Verificação de admin:** `AdminVerificador.exigirAdmin(jwt)` extrai o subject do JWT, converte para Long e compara com `configuracaoAplicacao.idAdminMestre`. Qualquer diferença retorna 403.
- **Modelo de dados:** Roles agrupam permissões (N:N). Usuários recebem roles (N:N). O `idUsuario` em `usuarioRole` é apenas um Long — sem FK cross-database para o AuthLuiz.
- **Permissões:** Armazenadas como `recurso` + `acao` em minúsculas (ex: `artigos`, `criar`). A constraint `UNIQUE(recurso, acao)` garante unicidade.

**Migrações de banco:** Flyway (`db/migration/V*.sql`). Schema usa identificadores camelCase entre aspas (mesmo padrão do AuthLuiz). O DDL está como `validate` — sempre crie um novo arquivo de migração para alterações no schema.

**Testes:** Testcontainers com PostgreSQL real (sem mocks).

### Frontend (`frontend/src/`)

- `services/api.js` — Instância Axios apontando para o PermLuiz com injeção de Bearer token e logout automático em 401.
- `services/autenticacaoService.js` — Armazenamento e leitura do JWT (obtido via AuthLuiz diretamente).
- `router/index.js` — Guards: redireciona para `/setup` se setup não concluído; exige autenticação nas rotas com `requiresAuth`.
- `views/` — Um Vue SFC por página (`Setup`, `Login`, `MinhaConta`, `AdminRoles`, `AdminPermissoes`, `AdminUsuarios`).
- Interface usa Bootstrap 5 + Bootstrap Icons.

### Resumo dos Endpoints da API

Manter esta tabela sempre atualizada ao criar, editar ou remover endpoints durante o desenvolvimento.

| Método | Caminho | Autenticação | Descrição |
|--------|---------|--------------|-----------|
| GET | `/setup` | Pública | Verifica se o setup foi concluído |
| POST | `/setup` | Chave mestra | Define o admin mestre (`{ "idUsuario": N }`) |
| GET | `/me/roles` | JWT | Retorna os roles e permissões do usuário autenticado |
| GET | `/admin/roles` | JWT+Admin | Lista todos os roles |
| POST | `/admin/roles` | JWT+Admin | Cria role |
| PUT | `/admin/roles/{id}` | JWT+Admin | Atualiza role |
| DELETE | `/admin/roles/{id}` | JWT+Admin | Remove role |
| GET | `/admin/roles/{id}/permissoes` | JWT+Admin | Lista permissões de um role |
| PUT | `/admin/roles/{id}/permissoes` | JWT+Admin | Redefine permissões de um role (lista de IDs) |
| GET | `/admin/permissoes` | JWT+Admin | Lista todas as permissões |
| POST | `/admin/permissoes` | JWT+Admin | Cria permissão |
| PUT | `/admin/permissoes/{id}` | JWT+Admin | Atualiza permissão |
| DELETE | `/admin/permissoes/{id}` | JWT+Admin | Remove permissão |
| GET | `/admin/usuarios/{id}/roles` | JWT+Admin | Lista roles de um usuário |
| POST | `/admin/usuarios/{id}/roles` | JWT+Admin | Atribui role ao usuário |
| DELETE | `/admin/usuarios/{id}/roles/{roleId}` | JWT+Admin | Remove role do usuário |

## Variáveis de Ambiente

Consulte `backend/.env.example`. Variáveis obrigatórias:

- `APP_SETUP_MASTER_KEY` — usada para concluir o setup inicial
- `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` — conexão com o Postgres
- `AUTH_LUIZ_JWKS_URI` — URI do JWKS do AuthLuiz para validação dos JWTs

## Frontend como Implementação de Referência

O frontend incluído neste repositório é uma **implementação de referência** — serve para demonstrar o uso da API, mas o backend foi projetado para ser reutilizável com qualquer frontend.

## CORS

O backend aceita requisições das seguintes origens:
- `http://localhost:81` — frontend do PermLuiz via Docker (porta 81)
- `http://localhost:5174` — frontend do PermLuiz em modo dev

## Padrões de Mensagens

Todas as mensagens de resposta da API devem terminar com `!`. Isso é um padrão do projeto — verificar sempre antes de retornar strings de erro ou sucesso nos services e controllers.

## Integridade Referencial

A tabela `usuarioRole` usa `ON DELETE CASCADE` na FK para `role`. O `idUsuario` não tem FK para o banco do AuthLuiz (cross-database) — a integridade é garantida pela aplicação. Se novas tabelas forem criadas com FK para `role` ou `permissao`, garantir `ON DELETE CASCADE`.

## Centralização de Mensagens e Validações

Mensagens de erro, textos de validação e lógicas de verificação repetidas devem ser definidas em um único local. Evite duplicar strings ou regras em múltiplos locais do projeto.

## Testes de API

O usuário utiliza o **API Dog** para testar o backend. Quando solicitado, forneça roteiros de teste detalhados com método, URL, headers, body e resultado esperado para cada cenário (sucesso e erro).

## Manutenção de Documentação

Durante o desenvolvimento, manter os seguintes arquivos sempre atualizados:

- **`CLAUDE.md`** — qualquer informação relevante para o entendimento futuro do projeto: novos endpoints, mudanças de arquitetura, regras de negócio, convenções adotadas, decisões técnicas e restrições.
- **`README.md`** (raiz) — visão geral do projeto, stack, início rápido e variáveis de ambiente.
- **`backend/README.md`** — estrutura de pacotes, migrações de banco, endpoints e instruções de execução do backend.
- **`frontend/README.md`** — estrutura de pastas, fluxos implementados, comandos e variáveis de ambiente do frontend.

## Fluxo de Trabalho com Claude

- As tarefas são trabalhadas uma por vez, do início ao fim.
- Commits só são realizados quando o usuário solicitar explicitamente.
- Ao ser solicitado um commit, sempre sugerir descrições para o mesmo antes de executar.
- O usuário usa os prefixos `feat` ou `fix` nas mensagens de commit para controle (ex: `feat: adicionar endpoint de logout`).
