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
- **Autenticação:** Spring Security é stateless (sem sessões). O JWT é emitido pelo `JwtService` no login usando **RS256** (RSA 2048-bit assimétrico); todos os endpoints protegidos o validam via OAuth2 resource server. A chave privada assina o token; a chave pública está exposta em `GET /auth/.well-known/jwks.json` para que outros serviços (ex: PermLuiz) possam verificar tokens de forma autônoma. O CORS aceita `http://localhost:5173` e `http://localhost:5174` (PermLuiz). O claim `subject` do JWT contém o `publicId` (UUID) do usuário — nunca o ID numérico interno. Controllers extraem o publicId com `UUID.fromString(jwt.getSubject())` e os services o usam para buscar o usuário via `findByPublicId`. **O JWT contém apenas o essencial para autenticação** (`subject`, `jti`, `issuer`, `issuedAt`, `expiresAt`) — dados do usuário como nome, e-mail e username não são incluídos no token para evitar informação desatualizada; quem precisar desses dados deve chamar `GET /auth/me` ou `GET /auth/interno/usuarios`.
- **Identificador híbrido:** A entidade `Usuario` possui três identificadores: `id` (Integer, autoincremento, chave primária, usado apenas internamente — FKs, queries JPQL, operações dentro dos services), `publicId` (UUID v4, imutável, único, gerado via `@PrePersist`, exposto em todos os DTOs de resposta e no JWT `subject`) e `username` (String, 4–30 chars, único, alterável pelo usuário, usado como identificador público legível). O objetivo é nunca vazar o ID numérico em respostas de API ou mensagens de erro. Entidades relacionadas (TokenConfirmacao, TokenRecuperacaoSenha, IdentidadeExterna, etc.) continuam referenciando `usuario.id` nas FKs — o publicId só é relevante na fronteira externa da API. Ao criar novas entidades relacionadas ao usuário, seguir o mesmo padrão: FK interna aponta para `usuario.id`, qualquer exposição externa usa `usuario.publicId`.
- **Username e nome:** O campo `username` é obrigatório, único e serve como identificador de login (4–30 chars; apenas letras, números, ponto e underscore; deve começar com letra). O campo `nome` é obrigatório (3–100 chars) e usado nas saudações de e-mail — use sempre `usuario.getNome()` ao construir e-mails (nunca `getNomeParaEmail()` — esse método não existe). `UsernameValidator` bloqueia palavras reservadas (`admin`, `root`, `system`, `support`, `help`, `api`, `auth`, `me`, `null`, `undefined`, `authluiz`, `login`, `logout`, `cadastro`, `conta`, `usuario`, `user`, `setup`, `interno`). Cadastro via Google OAuth gera username automático a partir da parte local do e-mail (com sufixo `_2`, `_3`... em caso de conflito); `nome` recebe o nome do perfil Google.
- **Login unificado:** `POST /auth/login` aceita e-mail, telefone ou username no campo `identificador`. O `LoginRequest` detecta o tipo via `tipoIdentificador()`: `@` → EMAIL; starts with `+` ou só dígitos → TELEFONE; caso contrário → USERNAME. Resposta de erro é sempre genérica ("Credenciais inválidas!") independente do motivo — previne enumeração de identificadores.
- **Recuperação de senha:** Aceita **e-mail ou telefone** como identificador. O código numérico de 6 dígitos é enviado por e-mail (via `EmailService`) quando o campo `email` é informado, ou via WhatsApp/SMS (via `NotificacaoTelefonePort`) quando o campo `telefone` é informado. O canal de telefone exige `telefoneVerificado = true` e Twilio configurado (503 se ausente). O código é armazenado em texto simples em `TokenRecuperacaoSenha.codigo` (hash seria inútil para 6 dígitos — a segurança vem da expiração de 5 min e do limite de tentativas). A redefinição ocorre em um único passo: `POST /auth/recuperacao/redefinir` recebe `{email|telefone, codigo, novaSenha}`. Proteção contra brute-force: após 5 tentativas erradas o código é bloqueado. A limpeza de tokens expirados é feita pelo `TokenRecuperacaoSenhaExpiracaoService`. Ambos os canais passam pelo mesmo `EnvioCodigoRateLimitService.validarLimitePorIp` — as regras de segurança são uniformes. **Alerta por e-mail no canal telefone:** quando a recuperação é iniciada via telefone, o sistema envia paralelamente um e-mail de alerta informativo ao endereço cadastrado, exibindo o número de telefone usado, o IP e o horário da solicitação. O e-mail não contém botão de cancelamento — a proteção real vem do OTP estar fisicamente no telefone do usuário.
- **Google OAuth:** O frontend obtém um Google ID token via Google Identity Services SDK; o backend valida (`GoogleIdTokenValidatorService`/`GoogleAudienceValidator`) e emite seu próprio JWT. O vínculo com Google é gerenciado na tela de conta (`POST /auth/oauth/google/vincular` e `DELETE /auth/oauth/google/vincular`); o login com Google nunca vincula automaticamente — retorna 409 se o e-mail já existe. Vinculação exige que o e-mail do Google seja idêntico ao da conta. Desvinculação exige senha definida e confirmação por senha via modal. **Contas criadas via Google (`providerOrigem = GOOGLE`) não podem ser desvinculadas**; o campo `providerOrigem` (nullable `ProviderExterno` enum) no `Usuario` registra qual provider originou o cadastro — null indica e-mail/senha, valor preenchido indica OAuth. Esse campo é extensível para futuros providers (Apple, GitHub, etc.).
- **Configuração de e-mail:** As credenciais SMTP ficam criptografadas na tabela `configuracaoAplicacao` via `CriptografiaConfiguracaoService` (BouncyCastle). O `EmailService` as lê em tempo de execução.
- **Envio de e-mail sempre assíncrono:** Todos os métodos públicos do `EmailService` são anotados com `@Async` — o envio ocorre em thread separada e nunca bloqueia a resposta HTTP. `@EnableAsync` está ativo na `AuthLuizApplication`. Ao adicionar novos métodos de envio ao `EmailService`, sempre incluir `@Async`. Os e-mails são enviados em formato **HTML** (`MimeMessage` + `MimeMessageHelper`) via o método interno `construirHtml()`, que gera um layout compartilhado (header, corpo, botão CTA, footer). `SimpleMailMessage` (texto puro) não deve ser usado.
- **Geolocalização por IP nos e-mails de segurança:** Os e-mails de alerta de recuperação de senha por telefone e de confirmação de redefinição de senha incluem uma linha de localização aproximada ("Cidade, Estado, País") obtida dinamicamente via `IpGeolocalizacaoService`. O serviço chama a API gratuita `ip-api.com/json/{ip}` (HTTP, sem autenticação, 45 req/min, resposta em português via `lang=pt-BR`) com timeout de 3 segundos em conexão e leitura. A chamada ocorre dentro do método `@Async` do `EmailService`, portanto nunca bloqueia a thread HTTP principal. Falhas na API são logadas em nível `WARN` e o e-mail é enviado normalmente sem a linha de localização (degradação graciosa). IPs privados e de loopback (127.0.0.1, ::1, 10.x.x.x, 172.16–31.x.x, 192.168.x.x) são detectados previamente e pulam a chamada à API — a linha de localização simplesmente não aparece no e-mail. **Em desenvolvimento local** (sem proxy reverso), o backend recebe o IP da bridge do Docker (`172.17.0.1`) ou localhost — ambos são privados e a geo não é exibida, o que é o comportamento esperado. **Em produção via `docker compose up`**, o nginx do frontend atua como proxy reverso das rotas `/auth/` e `/setup/`, definindo `X-Real-IP $remote_addr` com o IP real do cliente — o backend então lê esse header e a geo funciona corretamente.
- **Extração de IP real — `IpUtils`:** Todos os controllers (`AutenticacaoController`, `ConfirmacaoController`, `ContaController`) e o `AuditoriaAspect` extraem o IP do cliente via `IpUtils.extrairIp(HttpServletRequest)` (classe utilitária em `api/common/`). A prioridade é: `X-Real-IP` → `X-Forwarded-For` (primeiro elemento) → `getRemoteAddr()`. Isso garante que o IP real do cliente seja usado tanto para rate limiting, auditoria e geolocalização em qualquer setup com proxy reverso. **Nunca chamar `getRemoteAddr()` diretamente nos controllers** — sempre usar `IpUtils.extrairIp()`.
- **Confirmação de e-mail:** Sempre obrigatória — não há flag de configuração. Usa **código numérico de 6 dígitos** enviado por e-mail (não links). **Cadastro** envia apenas um e-mail de boas-vindas; o código de verificação é gerado e enviado sob demanda — o usuário solicita clicando em "Confirmar e-mail" na tela de conta, que aciona `POST /auth/verificacao/reenviar`. Usuário não confirmado não pode alterar e-mail, senha ou telefone; contas não confirmadas são verificadas a cada hora pelo `ConfirmacaoEmailExpiracaoService` (`@Scheduled`) e removidas se mais antigas que 7 dias. **Alteração de e-mail** usa `emailPendente` + código (5 min); o e-mail só é trocado após inserção correta do código. Em ambos os casos o usuário insere o código em `POST /auth/verificacao/confirmar` (JWT) — o backend detecta automaticamente o tipo pendente (`VERIFICACAO_CADASTRO` ou `ALTERACAO_EMAIL`). Proteção contra brute-force: após 5 tentativas erradas o código é bloqueado e exige reenvio. Rate limiting de alteração de e-mail por usuário via `ControleAlteracaoEmail` (máx. 5 por 1440 min, bloqueio de 1440 min) — veja nota abaixo sobre a distinção entre os dois controles de rate limit. Cooldown de reenvio: 2 minutos (mesmo mecanismo do `TokenConfirmacaoService`). Códigos expiram em 5 minutos e são invalidados ao serem substituídos por um novo reenvio. Códigos são armazenados em texto simples (não há hash — veja "Recuperação de senha" para o motivo). `@EnableScheduling` está ativo na `AuthLuizApplication`.
- **Rate limiting de envio de código por IP:** Todos os endpoints que disparam envio ou reenvio de código (recuperação de senha, verificação de cadastro, alteração de e-mail) passam pelo `EnvioCodigoRateLimitService.validarLimitePorIp` antes de qualquer envio. O controle é armazenado em `ControleEnvioCodigoIp` (máx. 5 solicitações por 10 min por IP, bloqueio de 2 min). O serviço é agnóstico ao canal — foi projetado para cobrir também o futuro envio de OTP por telefone sem necessidade de refatoração.

**Distinção entre os dois controles de rate limit — não unificar:**
- `ControleAlteracaoEmail` (chave: `idUsuario`) — **regra de negócio por usuário**: impede que um usuário específico troque e-mail repetidamente, independente de qual IP usou.
- `ControleEnvioCodigoIp` (chave: `ip`) — **proteção de rede por IP**: impede que um IP dispare envios em massa pela API, mesmo sem ter conta. Cobre todos os canais (e-mail e telefone).
Os dois são ortogonais: um atacante sem conta é bloqueado pelo IP mas não tem `idUsuario`; um usuário legítimo trocando e-mail de vários IPs é bloqueado pelo usuário mas não pelo IP. Nenhum substitui o outro.

**Logout e blacklist de tokens:** O logout é stateful por design — ao chamar `POST /auth/logout`, o `jti` (UUID único por token, incluído como claim na geração) é inserido na tabela `tokenBlacklist` com seu `expiraEm`. O `JwtBlacklistFilter` (`config/security/`), registrado após o `BearerTokenAuthenticationFilter` no chain, verifica a cada requisição autenticada se o `jti` consta na blacklist — se sim, retorna 401 imediatamente. Tokens sem `jti` (gerados antes desta funcionalidade) simplesmente não são verificáveis e passam normalmente. O frontend (`fazerLogout()` em `autenticacaoService.js`) chama a API antes de limpar o localStorage; se a chamada falhar (ex.: sem rede), o logout local ocorre mesmo assim. O interceptor 401 de `api.js` e a expiração detectada pelo router usam `logout()` simples (sem chamar a API) pois o token já é inválido.

**Proteção por IP reconhecido e 2FA:** Todo login que passa pela validação de senha verifica se o IP é conhecido antes de emitir o JWT. Um IP é considerado conhecido se for igual a `usuario.ultimoIp` OU se estiver na tabela `usuarioIpConfiavel` para aquele usuário. Se o IP for desconhecido, o login retorna HTTP **202** com `LoginPendenteResponse` (contém `tokenPendente`, `tipo`, `destinoMascarado`) em vez do JWT. O cliente deve então chamar `POST /auth/login/verificar` com o código. Após verificação bem-sucedida, `completarLogin()` é chamado (emite JWT, atualiza `ultimoIp`) e opcionalmente o IP é salvo como confiável.

- **TOTP:** segredo Base32 armazenado criptografado (`CriptografiaConfiguracaoService.criptografar()`/`descriptografar()` — AES/GCM). Validação com janela ±1 período (30 s) via `samstevens/totp`. Segredo intermediário em `totpSecretPendente` até confirmação do 1º código.
- **Backup codes:** 8 códigos no formato `XXXX-XXXX` (charset sem ambíguos: sem 0/O, 1/I/L), armazenados como hashes Argon2 (`PasswordEncoder`). Exibidos em texto claro apenas na geração/regeneração.
- **LoginPendente:** entidade com `tokenPendente` (64 chars aleatórios, opaco), `tipo` (TOTP | EMAIL | SMS | WHATSAPP | AGUARDANDO_CANAL), `codigo` (null para TOTP e AGUARDANDO_CANAL), `ipOrigem`, expira em 5 min, máx 5 tentativas erradas — após exceder, `encerradoEm` é preenchido e novo login é necessário. **AGUARDANDO_CANAL:** tipo intermediário criado quando o usuário possui telefone verificado — nenhum código é enviado automaticamente; o frontend exibe uma tela de seleção de canal (Email / WhatsApp / SMS) e, ao escolher, chama `POST /auth/login/reenviar` com o canal desejado, que muda o tipo e envia o código.
- **IPs confiáveis removidos ao trocar senha:** `ContaService.atualizarSenha()` chama `ipConfiavelService.removerTodos()` em ambos os ramos (alterar senha existente e definir pela primeira vez), para que o próximo login de qualquer IP peça verificação.
- **Limpeza agendada:** `LimpezaAgendadaService` delega para `loginPendenteService.limparExpirados()` (remove registros com `expiraEm` > 1h atrás).
- **Variável de ambiente:** `APP_TOTP_ISSUER` (padrão: `AuthLuiz`) — nome exibido no app autenticador. Registrada em `additional-spring-configuration-metadata.json`.

**Migrações de banco:** Flyway (`db/migration/V*.sql`). O schema usa identificadores camelCase entre aspas (Hibernate `PhysicalNamingStrategyStandardImpl` + `globally_quoted_identifiers=true`). O DDL está como `validate` — sempre crie um novo arquivo de migração para alterações no schema. `V1__schema_inicial.sql` contém o schema base; V2–V20 aplicam alterações incrementais. A V17–V18 refina o tipo da coluna `publicId` na tabela `usuario`. A V20 remove a coluna `tokenCancelamento` da tabela `tokenRecuperacaoSenha`. V21–V24 implementam 2FA: colunas TOTP/IP no `usuario`, tabelas `usuarioIpConfiavel`, `codigoBackup2fa` e `loginPendente`.

**Testes:** Utilizam Testcontainers com uma instância real de Postgres (sem mocks).

### Frontend (`frontend/src/`)

- `services/api.js` — Instância Axios com injeção do token Bearer e logout automático em respostas 401. A `baseURL` é definida por `VITE_API_BASE_URL`: se não definida → `http://localhost:8080` (dev direto); se definida como string vazia (build Docker) → URLs relativas, roteadas pelo nginx.
- `services/autenticacaoService.js` — Utilitários de armazenamento e verificação de expiração do token. Mesma lógica de `VITE_API_BASE_URL`.
- `router/index.js` — Guards: redireciona para `/setup` se o setup não estiver concluído; exige autenticação nas rotas com `requiresAuth`; redireciona usuários autenticados para longe do login/cadastro.
- `views/` — Um Vue SFC por página (`Login`, `Cadastro`, `Conta`, `RecuperarSenha`, `RedefinirSenha`, `NovaSenha`, `Setup`, `VerificacaoEmail`, `VerificacaoTelefone`). A recuperação de senha usa duas telas: `RedefinirSenha` valida o código (chama `POST /auth/recuperacao/validar`) e passa o estado via `history.state` para `NovaSenha`, que define a nova senha (chama `POST /auth/recuperacao/redefinir`). `ContaView` inclui botão "Painel de Controle" visível apenas ao admin mestre do PermLuiz (verificado via `GET /me/admin` em `VITE_PERM_LUIZ_URL`); ao clicar, abre o PermLuiz com o JWT no fragment da URL (`#token=<jwt>`).
- `nginx.conf` — Serve os arquivos estáticos **e** atua como proxy reverso para `/auth/` e `/setup/` (proxy_pass para `http://backend:8080`). Injeta os headers `X-Real-IP $remote_addr` e `X-Forwarded-For` em todas as requisições proxiadas — isso é o que permite o backend obter o IP real do cliente em vez do IP interno do Docker. Também define `Cross-Origin-Opener-Policy: unsafe-none` para compatibilidade com o popup do Google OAuth. **Importante:** o `proxy_pass` usa `set $backend http://backend:8080` (variável) combinado com `resolver 127.0.0.11 valid=30s ipv6=off` — sem isso, o nginx tentaria resolver o hostname `backend` em tempo de startup do processo, falharia se o container ainda não estivesse pronto e o container frontend não subiria (resultando em `chrome-error://` no browser).
- Interface usa Bootstrap 5 + Bootstrap Icons.

**Por que o nginx faz proxy da API:** Em Docker, sem proxy, o browser chama `http://localhost:8080` diretamente — o backend recebe o IP da bridge do Docker (`172.17.0.1`, faixa privada), e a geolocalização retorna vazia. Com o nginx proxiando `/auth/` e `/setup/`, o IP real do cliente viaja no header `X-Real-IP`, que o `IpUtils.extrairIp()` lê com prioridade. Em modo dev (`npm run dev`), o frontend chama `http://localhost:8080` diretamente (sem nginx) e a geo não funciona — comportamento esperado para desenvolvimento local.

### Resumo dos Endpoints da API

Manter esta tabela sempre atualizada ao criar, editar ou remover endpoints durante o desenvolvimento.

| Método | Caminho | Autenticação | Descrição |
|--------|---------|--------------|-----------|
| POST | `/auth/cadastro` | Pública | Cadastro de novo usuário |
| POST | `/auth/login` | Pública | Login local: e-mail, telefone ou username + senha — retorna 200 (JWT) se IP conhecido, ou 202 (`LoginPendenteResponse`) se IP desconhecido |
| POST | `/auth/login/verificar` | Pública (tokenPendente) | Confirma o 2º fator (TOTP, OTP ou backup code) e retorna JWT; opcionalmente salva IP como confiável |
| POST | `/auth/login/reenviar` | Pública (tokenPendente) | Reenvia o código OTP para o destino original (não disponível para TOTP) |
| POST | `/auth/login/codigo-backup` | Pública (tokenPendente) | Usa código de backup 2FA (`XXXX-XXXX`) para concluir login pendente |
| POST | `/auth/logout` | JWT | Invalida o token atual inserindo seu `jti` na blacklist — o token é rejeitado em todas as requisições subsequentes até expirar naturalmente |
| POST | `/auth/oauth/google` | Pública | Login/cadastro via Google (retorna 409 se já existe conta com o e-mail — vincular via conta) |
| POST | `/auth/oauth/google/vincular` | JWT | Vincula Google à conta autenticada (e-mail do Google deve ser igual ao da conta) |
| DELETE | `/auth/oauth/google/vincular` | JWT | Desvincula Google da conta (exige senha definida para não perder o acesso) |
| POST | `/auth/recuperacao/iniciar` | Pública | Inicia recuperação de senha — body: `{email}` envia código por e-mail ou `{telefone}` envia via WhatsApp/SMS (exige telefoneVerificado e Twilio configurado); canal telefone também envia e-mail de alerta informativo |
| POST | `/auth/recuperacao/validar` | Pública | Valida o código de recuperação sem alterar a senha — body: `{email|telefone, codigo}`; decrementa tentativas em caso de erro; bloqueia após 5 tentativas erradas |
| POST | `/auth/recuperacao/redefinir` | Pública | Redefine senha com código válido — body: `{email|telefone, codigo, novaSenha}` |
| GET | `/auth/me` | JWT | Retorna dados da conta autenticada |
| PATCH | `/auth/me/username` | JWT | Atualiza username (4–30 chars, único; usernames reservados são rejeitados) |
| PATCH | `/auth/me/nome` | JWT | Atualiza o nome do usuário (3–100 chars) |
| PATCH | `/auth/me/email` | JWT | Atualiza e-mail (bloqueado para contas com Google vinculado; novo e-mail deve ser diferente do atual; sempre envia e-mail de confirmação para o novo endereço e salva em `emailPendente`) |
| PATCH | `/auth/me/senha` | JWT | Atualiza ou define senha (bloqueado se e-mail não verificado) |
| PATCH | `/auth/me/telefone` | JWT | Atualiza ou remove telefone (null remove; sempre define `telefoneVerificado=false`; bloqueado se e-mail não verificado) |
| POST | `/auth/me/exclusao/codigo` | JWT | Envia código OTP para confirmação de exclusão de conta (somente para usuários sem TOTP, mas com verificacaoExtraAtiva) |
| DELETE | `/auth/me` | JWT | Exclui a conta do usuário autenticado. Se `verificacaoExtraAtiva`: TOTP ativo → exige `codigo` (TOTP) no body; sem TOTP → exige `tokenPendente` + `codigo` (OTP enviado via `POST /auth/me/exclusao/codigo`) |
| POST | `/auth/verificacao/email/confirmar` | JWT | Confirma e-mail via código de 6 dígitos — body: `{codigo}`; detecta automaticamente o tipo pendente (cadastro ou alteração de e-mail) |
| POST | `/auth/verificacao/email/enviar` | JWT | Envia código de e-mail — detecta automaticamente o pendente: cadastro (`emailVerificado=false`) ou alteração (`emailPendente!=null`); cooldown de 2 min |
| POST | `/auth/verificacao/telefone/confirmar` | JWT | Confirma alteração de telefone via código de 6 dígitos — body: `{codigo}` |
| POST | `/auth/verificacao/telefone/enviar` | JWT | Envia código de verificação de telefone via WhatsApp/SMS (cooldown de 2 min) |
| GET/POST | `/setup/**` | Chave mestra | Configuração inicial da aplicação |
| GET | `/auth/.well-known/jwks.json` | Pública | Chave pública RSA em formato JWKS (usada por serviços externos para validar JWTs) |
| POST | `/auth/me/2fa/totp/iniciar` | JWT | Inicia setup TOTP: gera segredo, retorna URI `otpauth://` para QR code |
| POST | `/auth/me/2fa/totp/confirmar` | JWT | Confirma setup com 1º código TOTP; ativa TOTP e retorna 8 backup codes (exibidos uma única vez) |
| DELETE | `/auth/me/2fa` | JWT | Desativa TOTP e remove backup codes (exige senha) |
| POST | `/auth/me/2fa/backup-codes/regerar` | JWT | Gera novos 8 backup codes, invalidando os anteriores (exige código TOTP atual) |
| GET | `/auth/me/2fa/status` | JWT | Retorna `{ totpAtivo, codigosRestantes, preferencia2fa }` |
| PATCH | `/auth/me/2fa/verificacao-extra` | JWT | Ativa/desativa verificação extra — body: `{ ativo, senha? }`; ao desativar (`ativo: false`), exige `senha` para contas com senha definida |
| PATCH | `/auth/me/2fa/preferencia` | JWT | Define canal de verificação para contas sem TOTP: `EMAIL` \| `SMS` \| `WHATSAPP` |
| GET | `/auth/me/ips-confiaveis` | JWT | Lista IPs confiáveis da conta |
| POST | `/auth/me/ips-confiaveis` | JWT | Adiciona IP manualmente como confiável |
| DELETE | `/auth/me/ips-confiaveis/{id}` | JWT | Remove um IP confiável |
| DELETE | `/auth/me/ips-confiaveis` | JWT | Remove todos os IPs confiáveis |
| GET | `/auth/interno/usuarios` | X-Service-Key | Lista todos os usuários — endpoint server-to-server, protegido por header `X-Service-Key` (não aceita JWT) |
| GET | `/auth/interno/usuarios/{publicId}/existe` | X-Service-Key | Verifica se um usuário com o publicId informado existe — usado pelo PermLuiz antes de atribuir roles |

## Convenção de Respostas REST

Ao criar ou modificar endpoints, seguir sempre estas regras:

- **`POST` de criação (ex: cadastro)** → `201 Created` + recurso completo (`ContaResponse`). Nunca retornar resposta parcial.
- **`PATCH`/`DELETE` que mutam estado do usuário autenticado** → recurso completo atualizado (`ContaResponse` para dados da conta; `DoisFatoresStatusResponse` para estado 2FA). Nunca retornar `MensagemResponse` para operações com efeito colateral no recurso.
- **`MensagemResponse`** → reservado para ações sem recurso associado: logout, envio de código, recuperação de senha, validação de código. Esses endpoints não alteram o estado de um recurso específico — comunicam apenas o resultado da ação.
- **`GET`** → sempre retorna o recurso atual; nunca modifica estado.

**Endpoints que **atualmente** retornam recurso completo:**
- `POST /auth/cadastro` → `201 Created` + `ContaResponse`
- `GET /auth/me` → `ContaResponse`
- `PATCH /auth/me/{username,nome,email,telefone}` → `ContaResponse`
- `POST /auth/verificacao/email/confirmar` → `ContaResponse`
- `POST /auth/verificacao/telefone/confirmar` → `ContaResponse`
- `PATCH /auth/me/senha` → `ContaResponse`
- `DELETE /auth/me/2fa` → `DoisFatoresStatusResponse`
- `PATCH /auth/me/2fa/verificacao-extra` → `DoisFatoresStatusResponse`

## Variáveis de Ambiente

Consulte `backend/.env.example`. Variáveis obrigatórias:

- `APP_SETUP_MASTER_KEY` — usada para concluir o setup inicial
- `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` — conexão com o Postgres
- `JWT_RSA_PRIVATE_KEY` — chave privada RSA (PKCS#8) em base64, usada para assinar JWTs (RS256)
- `JWT_RSA_PUBLIC_KEY` — chave pública RSA em base64, exposta via JWKS e usada para verificar JWTs
- `JWT_EXPIRATION_MINUTES` — padrão: 120
- `GOOGLE_OAUTH_CLIENT_ID` — client ID do Google OAuth
- `AUTH_LUIZ_SERVICE_KEY` — chave compartilhada com o PermLuiz para autenticar chamadas internas (`/auth/interno/**`)
- `APP_TOTP_ISSUER` — nome exibido no app autenticador TOTP (padrão: `AuthLuiz`)

**Geração do par de chaves RSA:**
```bash
openssl genpkey -algorithm RSA -out private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -in private.pem -pubout -out public.pem
# Converter para base64 linha única (Linux/WSL):
base64 -w 0 private.pem > private.b64
base64 -w 0 public.pem > public.b64
```

**Variáveis do frontend (`frontend/.env`):**
- `VITE_API_BASE_URL` — URL base da API. Padrão (não definida): `http://localhost:8080` (dev, chama backend direto). No `compose.yaml` é definida como string vazia → Axios usa URLs relativas → chamadas passam pelo nginx do próprio container, que injeta `X-Real-IP` e proxia para o backend. **Não usar `||` para fallback** — a verificação em `api.js` e `autenticacaoService.js` usa `!== undefined` para distinguir "não definida" de "definida como vazia".
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

A entidade `Usuario` possui os campos `telefone` (VARCHAR 20, nullable, único, formato E.164: `+5511987654321`), `telefoneVerificado` (boolean, default false) e `telefonePendente` (VARCHAR 20, nullable). O campo é opcional no cadastro e atualizável via `PATCH /auth/me/telefone`.

**Fluxo de verificação de telefone (espelhando o e-mail):**

`POST /auth/verificacao/telefone/enviar` detecta automaticamente o tipo pendente:
- `telefonePendente != null` → envia código de **alteração** (`ALTERACAO_TELEFONE`) para o número pendente
- `telefone != null && !telefoneVerificado` → envia código de **verificação inicial** (`VERIFICACAO_TELEFONE`) para o telefone cadastrado
- Nenhum dos casos → 400

`POST /auth/verificacao/telefone/confirmar` detecta automaticamente o tipo ativo (`VERIFICACAO_TELEFONE` ou `ALTERACAO_TELEFONE`) e aplica a ação correta:
- `VERIFICACAO_TELEFONE` → define `telefoneVerificado = true`
- `ALTERACAO_TELEFONE` → move `telefonePendente` → `telefone`, define `telefoneVerificado = true`

**Fluxo de alteração de telefone (2 etapas):**
1. `PATCH /auth/me/telefone` — valida disponibilidade do Twilio (503 se não configurado), salva o novo número em `telefonePendente`, cria token `ALTERACAO_TELEFONE` e envia código via Twilio. O campo `telefone` permanece inalterado até a confirmação.
2. `POST /auth/verificacao/telefone/confirmar` — valida o código e aplica a alteração.

Regras: requer `emailVerificado = true`; código de 6 dígitos expira em 5 min; máx 5 tentativas erradas bloqueiam o token; remoção de telefone (body `{telefone: null}`) é direta, sem verificação. Dois usuários não podem ter o mesmo `telefone` — unicidade validada no serviço (409) e no banco (`uq_usuario_telefone`). `telefonePendente` expirado é limpo automaticamente em `GET /auth/me`.

**Provider SMS:** Twilio (SDK `com.twilio.sdk:twilio`). Interface `NotificacaoTelefonePort` em `domain/notificacao/port/` — `TwilioAdapter` é a implementação atual (`@Primary`). A interface define dois métodos: `validarDisponibilidade()` (síncrono — lança HTTP 503 se credenciais não configuradas) e `enviarCodigoVerificacao()` (`@Async` — erros de API Twilio são logados, não propagados ao HTTP). **Credenciais armazenadas no banco:** `twilioAccountSidCriptografado`, `twilioAuthTokenCriptografado`, `twilioFromNumber`, `twilioCanal` são colunas da tabela `configuracaoAplicacao`, configuradas via `POST /setup` (igual ao SMTP). O `TwilioAdapter` lê as credenciais via `SetupService` em tempo de execução — não há variáveis de ambiente para o Twilio. **Comportamento quando credenciais ausentes:** `validarDisponibilidade()` é chamado sincronamente antes de qualquer persistência (antes de salvar `telefonePendente` ou criar token) — retorna HTTP 503 ao cliente e não salva nenhum estado. Falhas na API Twilio (credenciais configuradas mas envio falha) são logadas sem interromper o fluxo. Trocar de provedor = novo adapter implementando `NotificacaoTelefonePort`, sem alterar domínio.

**Formato de número brasileiro no WhatsApp:** contas criadas antes de 2012 podem estar registradas no WhatsApp sem o 9º dígito (ex: `+553898286294` em vez de `+5538998286294`). O Twilio envia para o número exatamente como cadastrado no banco — se o usuário cadastrou com o 9 mas o WhatsApp reconhece sem ele (ou vice-versa), a mensagem não será entregue. Não há como detectar isso automaticamente; o usuário deve cadastrar o número no formato em que está registrado no WhatsApp.

**Recuperação de senha via telefone:** implementada. Fluxo descrito na seção "Recuperação de senha" acima.

## Auditoria de Logs

O sistema de auditoria registra automaticamente ações dos usuários na tabela `log_auditoria` (banco do próprio serviço), capturando: `acao`, `categoria`, `idUsuario`, `ipOrigem`, `metodoHttp`, `uri`, `statusHttp` e `sucesso`.

**Mecanismo:** anotação `@Auditavel` nos métodos de controller + `AuditoriaAspect` (`@Around`) que intercepta a chamada, extrai os dados do request via `RequestContextHolder` e do JWT via `SecurityContextHolder`, e persiste o log. Em caso de exceção, registra `sucesso=false` e substitui a ação conforme o mapa de falha (ex: `LOGIN_SUCESSO` → `LOGIN_FALHA`). O `AuditoriaService.registrar` usa `@Transactional(propagation = REQUIRES_NEW)` — o log é sempre salvo em uma transação independente, mesmo quando a transação principal foi revertida (rollback).

**Categorias:**
- `SEGURANCA` — sempre registrado (login, cadastro, senha, conta deletada, OAuth). Não tem toggle.
- `ATIVIDADE` — configurável via setup (campo `auditoriaAtividade` na tabela `configuracaoAplicacao`, padrão: `true`). Lido pelo `AuditoriaAspect` em cada requisição via `SetupService`.

**Limpeza automática:** centralizada no `LimpezaAgendadaService` (`config/agendamento/`), que roda diariamente às 03:00 e delega para cada serviço de domínio. Cada domínio controla sua própria regra de retenção; o agendador apenas orquestra. Falhas são isoladas por domínio — se um job falhar, os demais continuam. Para adicionar nova limpeza: implemente o método no serviço de domínio e registre a chamada em `LimpezaAgendadaService.limpar()`.

Retenções atuais:
- **Log de auditoria** (`AuditoriaLimpezaService`): `auditoriaRetencaoDias` dias (padrão 90, configurável via setup)
- **Blacklist de tokens** (`LogoutService`): até `expiraEm` do token (máx. `jwt.expiration-minutes`)

**Detalhes do log (`AuditoriaService.definirDetalhes`):**
- Endpoints **anônimos** (login, cadastro, recuperação de senha, confirmação de e-mail, login Google): incluir `"E-mail: x@y.com"` ou `"Telefone: +55..."` conforme o identificador usado — é a única forma de identificar a conta, pois `idUsuario` é null.
- Endpoints **autenticados**: descrever apenas a ação (ex: `"Nome alterado de 'X' para 'Y'"`). O `idUsuario` (interno) já identifica a conta e é extraído do `publicId` presente no JWT.
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
