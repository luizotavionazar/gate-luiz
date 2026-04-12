# CLAUDE.md

Este arquivo fornece orientações ao Claude Code (claude.ai/code) ao trabalhar com o código deste repositório.

## Visão Geral do Projeto

AuthLuiz é uma API reutilizável de autenticação construída com Spring Boot (backend) e Vue 3 (frontend). Oferece cadastro de usuário, login com JWT, OAuth via Google, recuperação/redefinição de senha e um fluxo de setup inicial para configuração do envio de e-mail.

## Configuração do Ambiente

### Pré-requisitos

- Java 21, Maven
- Node.js + npm
- Docker (para o banco de dados)

### Backend

```bash
# Copiar e configurar o ambiente
cp backend/.env.example backend/.env
# Edite backend/.env com credenciais do banco, segredo JWT, chave mestra e client ID do Google OAuth

# Subir o banco de dados de desenvolvimento (Postgres + pgAdmin)
docker compose -f compose-dev.yaml up -d

# Rodar o backend (Spring Boot sobe o compose-dev.yaml automaticamente se o Docker estiver disponível)
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
npm run dev      # servidor de desenvolvimento em http://localhost:5173
npm run build    # build de produção
npm run preview  # pré-visualização do build de produção
```

O frontend aponta as chamadas de API para `http://localhost:8080` por padrão (configurável via `VITE_API_BASE_URL`).

### Stack completa (modo produção)

```bash
docker compose up --build
```

## Arquitetura

### Backend (`backend/src/main/java/br/com/luizotavionazar/authluiz/`)

**Organização de pacotes:**

- `api/` — Controllers e DTOs, agrupados por feature (`autenticacao`, `conta`, `oauth`, `setup`)
- `domain/` — Lógica de negócio e entidades JPA, agrupados por domínio (`autenticacao`, `configuracao`, `identidadeexterna`, `notificacao`, `usuario`)
- `config/` — Configurações Spring: `security/` (JWT, OAuth2 resource server, CORS) e `setup/` (SetupFilter)

**Fluxos principais:**

- **Guarda de setup:** `SetupFilter` intercepta todas as requisições (exceto `/setup/**`) e redireciona para o setup se `configuracaoAplicacao.setupConcluido = false`. O setup é concluído via `POST /setup` usando `APP_SETUP_MASTER_KEY`.
- **Autenticação:** Spring Security é stateless (sem sessões). O JWT é emitido pelo `JwtService` no login; todos os endpoints protegidos o validam via OAuth2 resource server (`spring-boot-starter-security-oauth2-resource-server`). O CORS está fixo para `http://localhost:5173`.
- **Recuperação de senha:** Limitada por IP via `ControleRecuperacaoSenha`. Os tokens são hasheados antes de serem armazenados (`TokenRecuperacaoSenha`). A limpeza de tokens expirados é feita pelo `TokenRecuperacaoSenhaExpiracaoService`.
- **Google OAuth:** O frontend obtém um Google ID token via Google Identity Services SDK; o backend valida (`GoogleIdTokenValidatorService`/`GoogleAudienceValidator`) e emite seu próprio JWT. Uma identidade Google pode ser vinculada a uma conta existente de e-mail/senha.
- **Configuração de e-mail:** As credenciais SMTP ficam criptografadas na tabela `configuracaoAplicacao` via `CriptografiaConfiguracaoService` (BouncyCastle). O `EmailService` as lê em tempo de execução.

**Migrações de banco:** Flyway (`db/migration/V*.sql`). O schema usa identificadores camelCase entre aspas (Hibernate `PhysicalNamingStrategyStandardImpl` + `globally_quoted_identifiers=true`). O DDL está como `validate` — sempre crie um novo arquivo de migração para alterações no schema.

**Testes:** Utilizam Testcontainers com uma instância real de Postgres (sem mocks).

### Frontend (`frontend/src/`)

- `services/api.js` — Instância Axios com injeção do token Bearer e logout automático em respostas 401.
- `services/autenticacaoService.js` — Utilitários de armazenamento e verificação de expiração do token.
- `router/index.js` — Guards: redireciona para `/setup` se o setup não estiver concluído; exige autenticação nas rotas com `requiresAuth`; redireciona usuários autenticados para longe do login/cadastro.
- `views/` — Um Vue SFC por página (`Login`, `Cadastro`, `Conta`, `RecuperarSenha`, `RedefinirSenha`, `Setup`, `VincularContaGoogle`).
- Interface usa Bootstrap 5 + Bootstrap Icons.

### Resumo dos Endpoints da API

| Método | Caminho | Autenticação |
|--------|---------|--------------|
| POST | `/auth/cadastro` | Pública |
| POST | `/auth/login` | Pública |
| POST | `/auth/oauth/google` | Pública |
| POST | `/auth/recuperacao/iniciar` | Pública |
| GET | `/auth/recuperacao/validar` | Pública |
| POST | `/auth/recuperacao/redefinir` | Pública |
| GET | `/auth/me` | JWT |
| PATCH | `/auth/me/nome` `/email` `/senha` | JWT |
| GET/POST | `/setup/**` | Chave mestra |

## Variáveis de Ambiente

Consulte `backend/.env.example`. Variáveis obrigatórias:

- `APP_SETUP_MASTER_KEY` — usada para concluir o setup inicial
- `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` — conexão com o Postgres
- `JWT_SECRET` — chave de assinatura dos JWTs
- `JWT_EXPIRATION_MINUTES` — padrão: 120
- `GOOGLE_OAUTH_CLIENT_ID` — client ID do Google OAuth

## Fluxo de Trabalho com Claude

- As tarefas são trabalhadas uma por vez, do início ao fim.
- Commits só são realizados quando o usuário solicitar explicitamente.
- Ao ser solicitado um commit, sempre sugerir descrições para o mesmo antes de executar.
- O usuário usa os prefixos `feat` ou `fix` nas mensagens de commit para controle (ex: `feat: adicionar endpoint de logout`).
