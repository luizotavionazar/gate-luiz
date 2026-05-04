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
- **Autenticação:** Spring Security é stateless (sem sessões). O JWT é emitido pelo `JwtService` no login usando **RS256** (RSA 2048-bit assimétrico); todos os endpoints protegidos o validam via OAuth2 resource server. A chave privada assina o token; a chave pública está exposta em `GET /auth/.well-known/jwks.json` para que outros serviços (ex: PermLuiz) possam verificar tokens de forma autônoma. O CORS aceita `http://localhost:5173` e `http://localhost:5174` (PermLuiz).
- **Recuperação de senha:** Limitada por IP via `ControleRecuperacaoSenha`. Os tokens são hasheados antes de serem armazenados (`TokenRecuperacaoSenha`). A limpeza de tokens expirados é feita pelo `TokenRecuperacaoSenhaExpiracaoService`.
- **Google OAuth:** O frontend obtém um Google ID token via Google Identity Services SDK; o backend valida (`GoogleIdTokenValidatorService`/`GoogleAudienceValidator`) e emite seu próprio JWT. O vínculo com Google é gerenciado na tela de conta (`POST /auth/oauth/google/vincular` e `DELETE /auth/oauth/google/vincular`); o login com Google nunca vincula automaticamente — retorna 409 se o e-mail já existe. Vinculação exige que o e-mail do Google seja idêntico ao da conta. Desvinculação exige senha definida e confirmação por senha via modal. **Contas criadas via Google (`providerOrigem = GOOGLE`) não podem ser desvinculadas**; o campo `providerOrigem` (nullable `ProviderExterno` enum) no `Usuario` registra qual provider originou o cadastro — null indica e-mail/senha, valor preenchido indica OAuth. Esse campo é extensível para futuros providers (Apple, GitHub, etc.).
- **Configuração de e-mail:** As credenciais SMTP ficam criptografadas na tabela `configuracaoAplicacao` via `CriptografiaConfiguracaoService` (BouncyCastle). O `EmailService` as lê em tempo de execução.
- **Envio de e-mail sempre assíncrono:** Todos os métodos públicos do `EmailService` são anotados com `@Async` — o envio ocorre em thread separada e nunca bloqueia a resposta HTTP. `@EnableAsync` está ativo na `AuthLuizApplication`. Ao adicionar novos métodos de envio ao `EmailService`, sempre incluir `@Async`. Os e-mails são enviados em formato **HTML** (`MimeMessage` + `MimeMessageHelper`) via o método interno `construirHtml()`, que gera um layout compartilhado (header, corpo, botão CTA, footer). `SimpleMailMessage` (texto puro) não deve ser usado.
- **Confirmação de e-mail:** Sempre obrigatória — não há flag de configuração. Cadastro gera token de verificação (7 dias), usuário não confirmado não pode alterar e-mail nem senha; contas não confirmadas são removidas a cada hora pelo `ConfirmacaoEmailExpiracaoService` (`@Scheduled`). **Alteração de e-mail** usa `emailPendente` + token (30 min); o e-mail só é trocado após o clique no link. Rate limiting de alteração de e-mail por usuário via `ControleAlteracaoEmail` (máx. 5 por 1440 min, bloqueio de 1440 min). Cooldown de reenvio: 2 minutos (mesmo mecanismo do `TokenConfirmacaoService`). Tokens são hasheados via `TokenUtils.gerarHash()` antes de armazenar. `@EnableScheduling` está ativo na `AuthLuizApplication`.

**Migrações de banco:** Flyway (`db/migration/V*.sql`). O schema usa identificadores camelCase entre aspas (Hibernate `PhysicalNamingStrategyStandardImpl` + `globally_quoted_identifiers=true`). O DDL está como `validate` — sempre crie um novo arquivo de migração para alterações no schema. As migrations V1–V7 originais foram consolidadas em um único `V1__schema_inicial.sql` (schema final).

**Testes:** Utilizam Testcontainers com uma instância real de Postgres (sem mocks).

### Frontend (`frontend/src/`)

- `services/api.js` — Instância Axios com injeção do token Bearer e logout automático em respostas 401.
- `services/autenticacaoService.js` — Utilitários de armazenamento e verificação de expiração do token.
- `router/index.js` — Guards: redireciona para `/setup` se o setup não estiver concluído; exige autenticação nas rotas com `requiresAuth`; redireciona usuários autenticados para longe do login/cadastro.
- `views/` — Um Vue SFC por página (`Login`, `Cadastro`, `Conta`, `RecuperarSenha`, `RedefinirSenha`, `Setup`, `VerificacaoEmail`). `ContaView` inclui botão "Painel de Controle" visível apenas ao admin mestre do PermLuiz (verificado via `GET /me/admin` em `VITE_PERM_LUIZ_URL`); ao clicar, abre o PermLuiz com o JWT no fragment da URL (`#token=<jwt>`).
- `nginx.conf` — serve os arquivos estáticos com o header `Cross-Origin-Opener-Policy: unsafe-none`, necessário para que o popup do Google OAuth consiga enviar `postMessage` à janela pai.
- Interface usa Bootstrap 5 + Bootstrap Icons.

### Resumo dos Endpoints da API

Manter esta tabela sempre atualizada ao criar, editar ou remover endpoints durante o desenvolvimento.

| Método | Caminho | Autenticação | Descrição |
|--------|---------|--------------|-----------|
| POST | `/auth/cadastro` | Pública | Cadastro de novo usuário |
| POST | `/auth/login` | Pública | Login com e-mail e senha |
| POST | `/auth/oauth/google` | Pública | Login/cadastro via Google (retorna 409 se já existe conta com o e-mail — vincular via conta) |
| POST | `/auth/oauth/google/vincular` | JWT | Vincula Google à conta autenticada (e-mail do Google deve ser igual ao da conta) |
| DELETE | `/auth/oauth/google/vincular` | JWT | Desvincula Google da conta (exige senha definida para não perder o acesso) |
| POST | `/auth/recuperacao/iniciar` | Pública | Inicia recuperação de senha por e-mail |
| GET | `/auth/recuperacao/validar` | Pública | Valida token de recuperação |
| POST | `/auth/recuperacao/redefinir` | Pública | Redefine senha com token válido |
| GET | `/auth/me` | JWT | Retorna dados da conta autenticada |
| PATCH | `/auth/me/nome` | JWT | Atualiza nome do usuário |
| PATCH | `/auth/me/email` | JWT | Atualiza e-mail (bloqueado para contas com Google vinculado; novo e-mail deve ser diferente do atual; sempre envia e-mail de confirmação para o novo endereço e salva em `emailPendente`) |
| PATCH | `/auth/me/senha` | JWT | Atualiza ou define senha (bloqueado se e-mail não verificado) |
| PATCH | `/auth/me/telefone` | JWT | Atualiza ou remove telefone (null remove; sempre define `telefoneVerificado=false`; bloqueado se e-mail não verificado) |
| DELETE | `/auth/me` | JWT | Exclui a conta do usuário autenticado (sempre permitido, independente do status de verificação) |
| GET | `/auth/verificacao/confirmar` | Pública | Confirma e-mail via token (cadastro ou alteração de e-mail) |
| POST | `/auth/verificacao/reenviar` | JWT | Reenvia e-mail de verificação de cadastro (cooldown de 2 min) |
| POST | `/auth/verificacao/reenviar-alteracao-email` | JWT | Reenvia e-mail de confirmação de alteração de e-mail (cooldown de 2 min) |
| GET/POST | `/setup/**` | Chave mestra | Configuração inicial da aplicação |
| GET | `/auth/.well-known/jwks.json` | Pública | Chave pública RSA em formato JWKS (usada por serviços externos para validar JWTs) |
| GET | `/auth/interno/usuarios` | X-Service-Key | Lista todos os usuários — endpoint server-to-server, protegido por header `X-Service-Key` (não aceita JWT) |

## Variáveis de Ambiente

Consulte `backend/.env.example`. Variáveis obrigatórias:

- `APP_SETUP_MASTER_KEY` — usada para concluir o setup inicial
- `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` — conexão com o Postgres
- `JWT_RSA_PRIVATE_KEY` — chave privada RSA (PKCS#8) em base64, usada para assinar JWTs (RS256)
- `JWT_RSA_PUBLIC_KEY` — chave pública RSA em base64, exposta via JWKS e usada para verificar JWTs
- `JWT_EXPIRATION_MINUTES` — padrão: 120
- `GOOGLE_OAUTH_CLIENT_ID` — client ID do Google OAuth
- `AUTH_LUIZ_SERVICE_KEY` — chave compartilhada com o PermLuiz para autenticar chamadas internas (`/auth/interno/**`)

**Geração do par de chaves RSA:**
```bash
openssl genpkey -algorithm RSA -out private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -in private.pem -pubout -out public.pem
# Converter para base64 linha única (Linux/WSL):
base64 -w 0 private.pem > private.b64
base64 -w 0 public.pem > public.b64
```

**Variáveis do frontend (`frontend/.env`):**
- `VITE_API_BASE_URL` — URL da API do AuthLuiz (padrão: `http://localhost:8080`)
- `VITE_PERM_LUIZ_URL` — URL do PermLuiz (padrão: `http://localhost:81`); usada para verificar admin e abrir o painel de controle
- `VITE_GOOGLE_CLIENT_ID` — client ID do Google OAuth

## Frontend como Implementação de Referência

O frontend incluído neste repositório é uma **implementação de referência** — serve para demonstrar o uso da API, mas o backend foi projetado para ser reutilizável com qualquer frontend. Ao implementar funcionalidades:

- O backend é independente de qualquer frontend específico.
- O frontend que o desenvolvedor implementar pode exibir suas próprias mensagens com base nos status HTTP e na estrutura de resposta da API — não é obrigado a usar as mensagens retornadas pelo backend.
- Regras de negócio e validações devem residir no backend; a camada de apresentação fica a cargo de cada frontend.

## Ecossistema de Serviços

Auth-Luiz é parte de um ecossistema de APIs reutilizáveis independentes:

- **auth-luiz** — autenticação e identidade (este repositório): `github.com/luizotavionazar/auth-luiz`
- **perm-luiz** — roles e permissões; verifica JWTs do Auth-Luiz via JWKS: `github.com/luizotavionazar/perm-luiz`
- **luiz-stack** — orquestração Docker que sobe ambos os serviços juntos: `github.com/luizotavionazar/luiz-stack`

O PermLuiz usa `GET /auth/.well-known/jwks.json` para obter a chave pública e verificar tokens de forma autônoma — sem compartilhar segredos.

## CORS

O backend aceita requisições das seguintes origens:
- `http://localhost` e `http://localhost:80` — frontend do AuthLuiz via Docker (porta 80)
- `http://localhost:81` — frontend do PermLuiz via Docker (porta 81)
- `http://localhost:5173` — frontend do AuthLuiz em modo dev
- `http://localhost:5174` — frontend do PermLuiz em modo dev

## Formato das Chaves RSA

`JWT_RSA_PRIVATE_KEY` e `JWT_RSA_PUBLIC_KEY` devem ser o **arquivo PEM completo codificado em base64** (incluindo os headers `-----BEGIN PRIVATE KEY-----` / `-----BEGIN PUBLIC KEY-----`). Use o utilitário `GerarChavesRSA.java` na raiz do `gate-luiz` para gerar os valores corretos. Base64 de DER puro não funciona.

## Verificação de Bootstrap

O endpoint `GET /setup/status` retorna `bootstrapOk: true` quando `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `JWT_RSA_PRIVATE_KEY` e `APP_SETUP_MASTER_KEY` estão todos preenchidos no `.env`.

## Integridade Referencial com Usuário

As tabelas `tokenRecuperacaoSenha`, `identidadeExterna`, `tokenConfirmacao` e `controleAlteracaoEmail` possuem `ON DELETE CASCADE` na FK para `usuario`. Se no futuro forem criadas novas tabelas com FK para `usuario`, garantir que também tenham `ON DELETE CASCADE` para que a deleção do usuário continue funcionando sem erros de integridade referencial.

## Telefone do Usuário

A entidade `Usuario` possui os campos `telefone` (VARCHAR 20, nullable, único, formato E.164: `+5511987654321`) e `telefoneVerificado` (boolean, default false). O campo é opcional no cadastro e atualizável via `PATCH /auth/me/telefone` (null remove o número). Sempre que o número é alterado, `telefoneVerificado` volta a `false`. Dois usuários não podem ter o mesmo telefone — a unicidade é validada na camada de serviço (409 Conflict) e reforçada por constraint no banco (`uq_usuario_telefone`). Múltiplos `NULL` são permitidos (comportamento padrão do PostgreSQL).

**Verificação e recuperação de senha via telefone ainda não implementadas.** A estrutura foi adicionada antecipadamente. Quando implementar:
- Provider recomendado: **Twilio** (suporta WhatsApp Business API + SMS com o mesmo SDK) ou **Zenvia** (alternativa BR).
- Fluxo de verificação: OTP de 6 dígitos, expiração 10 min, `telefoneVerificado = true` ao confirmar.
- Recuperação: se `telefoneVerificado = true`, oferecer link/código via WhatsApp (fallback SMS).
- Credenciais do provider devem ser adicionadas à `configuracaoAplicacao` e criptografadas como o SMTP.
- Envio sempre `@Async`, seguindo o padrão do `EmailService`.

## Auditoria de Logs

O sistema de auditoria registra automaticamente ações dos usuários na tabela `log_auditoria` (banco do próprio serviço), capturando: `acao`, `categoria`, `idUsuario`, `ipOrigem`, `metodoHttp`, `uri`, `statusHttp` e `sucesso`.

**Mecanismo:** anotação `@Auditavel` nos métodos de controller + `AuditoriaAspect` (`@Around`) que intercepta a chamada, extrai os dados do request via `RequestContextHolder` e do JWT via `SecurityContextHolder`, e persiste o log. Em caso de exceção, registra `sucesso=false` e substitui a ação conforme o mapa de falha (ex: `LOGIN_SUCESSO` → `LOGIN_FALHA`).

**Categorias:**
- `SEGURANCA` — sempre registrado (login, cadastro, senha, conta deletada, OAuth). Não tem toggle.
- `ATIVIDADE` — configurável via `AUDITORIA_ATIVIDADE=false` (nome, e-mail, telefone, vínculos Google). Padrão: `true`.

**Limpeza automática:** `AuditoriaLimpezaService` roda diariamente às 03:00 e exclui logs com `criadoEm < agora - AUDITORIA_RETENCAO_DIAS` (padrão: 90 dias).

**Detalhes do log (`AuditoriaService.definirDetalhes`):**
- Endpoints **anônimos** (login, cadastro, recuperação de senha, confirmação de e-mail, login Google): incluir `"E-mail: x@y.com"` — é a única forma de identificar a conta, pois `idUsuario` é null.
- Endpoints **autenticados**: descrever apenas a ação (ex: `"Nome alterado de 'X' para 'Y'"`). O `idUsuario` já identifica a conta via JWT.
- Exceção: `ALTERAR_EMAIL_SOLICITADO` mantém `"E-mail alterado de 'old' para 'new'"` porque os dois endereços são a informação relevante da ação.

**Adicionar nova ação auditada:**
1. Adicionar valor ao enum `AcaoAuditoria` (e opcionalmente ao mapa de falha no `AuditoriaAspect`)
2. Anotar o método do controller: `@Auditavel(acao = AcaoAuditoria.X, categoria = CategoriaAuditoria.Y)`

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
