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
- **Google OAuth:** O frontend obtém um Google ID token via Google Identity Services SDK; o backend valida (`GoogleIdTokenValidatorService`/`GoogleAudienceValidator`) e emite seu próprio JWT. O vínculo com Google é gerenciado na tela de conta (`POST /auth/oauth/google/vincular` e `DELETE /auth/oauth/google/vincular`); o login com Google nunca vincula automaticamente — retorna 409 se o e-mail já existe. Vinculação exige que o e-mail do Google seja idêntico ao da conta. Desvinculação exige senha local definida e confirmação por senha via modal. **Contas criadas via Google (`providerOrigem = GOOGLE`) não podem ser desvinculadas**; o campo `providerOrigem` (nullable `ProviderExterno` enum) no `Usuario` registra qual provider originou o cadastro — null indica e-mail/senha, valor preenchido indica OAuth. Esse campo é extensível para futuros providers (Apple, GitHub, etc.).
- **Configuração de e-mail:** As credenciais SMTP ficam criptografadas na tabela `configuracaoAplicacao` via `CriptografiaConfiguracaoService` (BouncyCastle). O `EmailService` as lê em tempo de execução.
- **Confirmação de e-mail (feature flag):** Controlada por `configuracaoAplicacao.confirmacaoEmailHabilitada`. Quando ativa: cadastro gera token de verificação (7 dias), usuário não confirmado não pode alterar senha, contas expiradas são removidas a cada hora pelo `ConfirmacaoEmailExpiracaoService` (`@Scheduled`). A **alteração de e-mail sempre exige confirmação**, independente da flag — usa `emailPendente` + token (30 min); o e-mail só é trocado após o clique no link. Rate limiting de alteração de e-mail por usuário via `ControleAlteracaoEmail` (máx. 5 por 1440 min, bloqueio de 1440 min). Cooldown de reenvio: 2 minutos (mesmo mecanismo do `TokenConfirmacaoService`). Tokens são hasheados via `TokenUtils.gerarHash()` antes de armazenar. `@EnableScheduling` está ativo na `AuthLuizApplication`.

**Migrações de banco:** Flyway (`db/migration/V*.sql`). O schema usa identificadores camelCase entre aspas (Hibernate `PhysicalNamingStrategyStandardImpl` + `globally_quoted_identifiers=true`). O DDL está como `validate` — sempre crie um novo arquivo de migração para alterações no schema.

**Testes:** Utilizam Testcontainers com uma instância real de Postgres (sem mocks).

### Frontend (`frontend/src/`)

- `services/api.js` — Instância Axios com injeção do token Bearer e logout automático em respostas 401.
- `services/autenticacaoService.js` — Utilitários de armazenamento e verificação de expiração do token.
- `router/index.js` — Guards: redireciona para `/setup` se o setup não estiver concluído; exige autenticação nas rotas com `requiresAuth`; redireciona usuários autenticados para longe do login/cadastro.
- `views/` — Um Vue SFC por página (`Login`, `Cadastro`, `Conta`, `RecuperarSenha`, `RedefinirSenha`, `Setup`, `VerificacaoEmail`).
- Interface usa Bootstrap 5 + Bootstrap Icons.

### Resumo dos Endpoints da API

Manter esta tabela sempre atualizada ao criar, editar ou remover endpoints durante o desenvolvimento.

| Método | Caminho | Autenticação | Descrição |
|--------|---------|--------------|-----------|
| POST | `/auth/cadastro` | Pública | Cadastro de novo usuário |
| POST | `/auth/login` | Pública | Login com e-mail e senha |
| POST | `/auth/oauth/google` | Pública | Login/cadastro via Google (retorna 409 se já existe conta com o e-mail — vincular via conta) |
| POST | `/auth/oauth/google/vincular` | JWT | Vincula Google à conta autenticada (e-mail do Google deve ser igual ao da conta) |
| DELETE | `/auth/oauth/google/vincular` | JWT | Desvincula Google da conta (exige senha local definida para não perder o acesso) |
| POST | `/auth/recuperacao/iniciar` | Pública | Inicia recuperação de senha por e-mail |
| GET | `/auth/recuperacao/validar` | Pública | Valida token de recuperação |
| POST | `/auth/recuperacao/redefinir` | Pública | Redefine senha com token válido |
| GET | `/auth/me` | JWT | Retorna dados da conta autenticada |
| PATCH | `/auth/me/nome` | JWT | Atualiza nome do usuário |
| PATCH | `/auth/me/email` | JWT | Atualiza e-mail (bloqueado para contas com Google vinculado; sempre envia e-mail de confirmação para o novo endereço e salva em `emailPendente`) |
| PATCH | `/auth/me/senha` | JWT | Atualiza ou define senha local (bloqueado se e-mail não verificado e confirmação habilitada) |
| DELETE | `/auth/me` | JWT | Exclui a conta do usuário autenticado (sempre permitido, independente do status de verificação) |
| GET | `/auth/verificacao/confirmar` | Pública | Confirma e-mail via token (cadastro ou alteração de e-mail) |
| POST | `/auth/verificacao/reenviar` | JWT | Reenvia e-mail de verificação de cadastro (cooldown de 2 min) |
| GET/POST | `/setup/**` | Chave mestra | Configuração inicial da aplicação |

## Variáveis de Ambiente

Consulte `backend/.env.example`. Variáveis obrigatórias:

- `APP_SETUP_MASTER_KEY` — usada para concluir o setup inicial
- `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` — conexão com o Postgres
- `JWT_SECRET` — chave de assinatura dos JWTs
- `JWT_EXPIRATION_MINUTES` — padrão: 120
- `GOOGLE_OAUTH_CLIENT_ID` — client ID do Google OAuth

## Frontend como Implementação de Referência

O frontend incluído neste repositório é uma **implementação de referência** — serve para demonstrar o uso da API, mas o backend foi projetado para ser reutilizável com qualquer frontend. Ao implementar funcionalidades:

- O backend é independente de qualquer frontend específico.
- O frontend que o desenvolvedor implementar pode exibir suas próprias mensagens com base nos status HTTP e na estrutura de resposta da API — não é obrigado a usar as mensagens retornadas pelo backend.
- Regras de negócio e validações devem residir no backend; a camada de apresentação fica a cargo de cada frontend.

## Integridade Referencial com Usuário

As tabelas `tokenRecuperacaoSenha`, `identidadeExterna`, `tokenConfirmacao` e `controleAlteracaoEmail` possuem `ON DELETE CASCADE` na FK para `usuario`. Se no futuro forem criadas novas tabelas com FK para `usuario`, garantir que também tenham `ON DELETE CASCADE` para que a deleção do usuário continue funcionando sem erros de integridade referencial.

## Centralização de Mensagens e Validações

Mensagens de erro, textos de validação e lógicas de verificação repetidas devem ser definidas em um único local — seja como constantes, funções ou métodos reutilizáveis — para que alterações futuras exijam mudança em apenas um ponto. Evite duplicar tanto strings quanto sequências de passos ou regras de verificação em múltiplos locais do projeto.

## Testes de API

O usuário utiliza o **API Dog** para testar o backend. Quando solicitado, forneça roteiros de teste detalhados com método, URL, headers, body e resultado esperado para cada cenário (sucesso e erro).

## Manutenção de Documentação

Durante o desenvolvimento, manter os seguintes arquivos sempre atualizados:

- **`CLAUDE.md`** — qualquer informação relevante para o entendimento futuro do projeto: novos endpoints, mudanças de arquitetura, regras de negócio, convenções adotadas, decisões técnicas e restrições. O objetivo é que este arquivo seja sempre uma fonte confiável de contexto.
- **`README.md`** (raiz) — visão geral do projeto, stack, início rápido e variáveis de ambiente.
- **`backend/README.md`** — estrutura de pacotes, migrações de banco, endpoints e instruções de execução do backend.
- **`frontend/README.md`** — estrutura de pastas, fluxos implementados, comandos e variáveis de ambiente do frontend.

Sempre que uma alteração relevante for realizada (novo endpoint, nova view, nova migração, mudança de fluxo, adição de dependência), atualizar todos os arquivos acima que forem afetados.

## Fluxo de Trabalho com Claude

- As tarefas são trabalhadas uma por vez, do início ao fim.
- Commits só são realizados quando o usuário solicitar explicitamente.
- Ao ser solicitado um commit, sempre sugerir descrições para o mesmo antes de executar.
- O usuário usa os prefixos `feat` ou `fix` nas mensagens de commit para controle (ex: `feat: adicionar endpoint de logout`).
