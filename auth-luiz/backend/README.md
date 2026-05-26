# AuthLuiz — Backend

API REST de autenticação construída com Spring Boot 4 e Java 21. Stateless, baseada em JWT, com suporte a OAuth via Google, confirmação de e-mail, recuperação de senha e setup inicial guiado.

## Stack

- **Java 21** + **Spring Boot 4**
- **Spring Security** — OAuth2 Resource Server (JWT), stateless
- **PostgreSQL** + **Flyway** — banco relacional com migrações versionadas
- **Argon2** — hash de senhas e backup codes 2FA (via Spring Security)
- **RS256 / RSA assimétrico (Nimbus)** — assinatura dos JWTs; chave pública exposta via JWKS
- **JavaMail** — envio de e-mail transacional
- **BouncyCastle** — criptografia das credenciais SMTP e segredos TOTP no banco
- **samstevens/totp 1.7.1** — geração e validação de TOTP RFC 6238 (compatível com Google Authenticator)
- **Lombok** — redução de boilerplate nas entidades e serviços
- **Testcontainers** — testes de integração com PostgreSQL real (sem mocks)

## Estrutura do projeto

```
src/main/java/.../authluiz/
│
├── api/                             Camada HTTP (controllers + DTOs)
│   ├── autenticacao/
│   │   ├── controller/
│   │   │   ├── AutenticacaoController   POST /auth/cadastro, /auth/login (→ 200 ou 202 com LoginPendente)
│   │   │   │                            POST /auth/recuperacao/iniciar, /auth/recuperacao/redefinir
│   │   │   ├── LoginPendenteController  POST /auth/login/verificar, /auth/login/reenviar
│   │   │   │                            POST /auth/login/codigo-backup
│   │   │   └── ConfirmacaoController    POST /auth/verificacao/email/confirmar (JWT)
│   │   │                                POST /auth/verificacao/email/enviar (JWT)
│   │   │                                POST /auth/verificacao/telefone/confirmar (JWT)
│   │   │                                POST /auth/verificacao/telefone/enviar (JWT)
│   │   └── dto/                         CadastroRequest/Response, LoginRequest/Response,
│   │                                    LoginPendenteResponse, VerificarLoginPendenteRequest,
│   │                                    ReenviarVerificacaoRequest, UsarCodigoBackupRequest,
│   │                                    RecuperacaoSenhaRequest, RedefinirSenhaRequest,
│   │                                    ConfirmarEmailRequest, ContaResponse, MensagemResponse
│   ├── common/
│   │   ├── exception/  ExcecaoLimiteTentativas
│   │   ├── handler/    ApiExceptionHandler  — trata ResponseStatusException globalmente
│   │   └── IpUtils     Utilitário estático extrairIp(HttpServletRequest): X-Real-IP → X-Forwarded-For → getRemoteAddr()
│   ├── conta/
│   │   ├── controller/ ContaController      GET/PATCH /auth/me, POST /auth/me/exclusao/codigo, DELETE /auth/me
│   │   │               DoisFatoresController POST /auth/me/2fa/totp/iniciar, /confirmar
│   │   │                                    DELETE /auth/me/2fa
│   │   │                                    POST /auth/me/2fa/backup-codes/regerar
│   │   │                                    GET /auth/me/2fa/status
│   │   │                                    PATCH /auth/me/2fa/verificacao-extra (senha obrigatória ao desativar)
│   │   │                                    PATCH /auth/me/2fa/preferencia
│   │   │               IPConfiavelController GET/POST/DELETE /auth/me/ips-confiaveis/**
│   │   └── dto/        AtualizarUsernameRequest, AtualizarNomeExibicaoRequest,
│   │                   AtualizarEmailRequest, AtualizarSenhaRequest,
│   │                   AtualizarTelefoneRequest, DeletarContaRequest
│   │                   AtualizarVerificacaoExtraRequest (ativo + senha opcional)
│   ├── oauth/
│   │   ├── controller/ OAuthController      POST /auth/oauth/google
│   │   │                                    POST/DELETE /auth/oauth/google/vincular
│   │   └── dto/        GoogleLoginRequest, DesvincularGoogleRequest
│   ├── jwks/
│   │   └── JwksController               GET /auth/.well-known/jwks.json (público, sem auth)
│   └── setup/
│       ├── controller/ SetupController      GET/POST /setup/**
│       └── dto/        SalvarSetupRequest, StatusSetupResponse,
│                       ConfiguracaoEmailPublicaResponse
│
├── config/
│   ├── auditoria/
│   │   ├── Auditavel                    @interface — anota métodos de controller a auditar
│   │   └── AuditoriaAspect             @Aspect — intercepta @Auditavel, extrai IP/userId e persiste log
│   ├── security/
│   │   ├── SecurityConfig               Regras de autorização, CORS, OAuth2 resource server
│   │   ├── SecurityBeansConfig          Beans: PasswordEncoder (Argon2), RSAPublicKey/RSAPrivateKey, JwtEncoder/Decoder (RS256)
│   │   ├── JwtService                   Geração e leitura de JWTs
│   │   ├── GoogleAudienceValidator      Validação do audience nos tokens do Google
│   │   ├── JsonAuthenticationEntryPoint Resposta JSON para 401
│   │   └── JsonAccessDeniedHandler      Resposta JSON para 403
│   └── setup/
│       └── SetupFilter                  Intercepta requisições e redireciona ao setup se não concluído
│
├── domain/                          Regras de negócio e entidades
│   │
│   ├── auditoria/
│   │   ├── entity/   LogAuditoria         Registro de auditoria: ação, categoria, IP, userId, resultado
│   │   ├── enums/
│   │   │   ├── AcaoAuditoria             LOGIN_SUCESSO, LOGIN_FALHA, CADASTRO, ALTERAR_SENHA...
│   │   │   └── CategoriaAuditoria        SEGURANCA (sempre ativo) | ATIVIDADE (configurável)
│   │   ├── repository/ LogAuditoriaRepository
│   │   └── service/
│   │       ├── AuditoriaService          Persiste registros de log (REQUIRES_NEW — independe da transação principal)
│   │       └── AuditoriaLimpezaService   @Scheduled (03:00) — exclui logs mais antigos que retencao-dias
│   │
│   ├── autenticacao/
│   │   ├── entity/
│   │   │   ├── TokenRecuperacaoSenha    Token de redefinição de senha (código 6 dígitos, expira em 5 min)
│   │   │   ├── ControleEnvioCodigoIp    Rate limiting por IP para qualquer envio de código (e-mail ou SMS/WhatsApp)
│   │   │   ├── TokenConfirmacao         Token hasheado para verificação de cadastro e alteração de e-mail
│   │   │   ├── TipoTokenConfirmacao     Enum: VERIFICACAO_CADASTRO | ALTERACAO_EMAIL | ALTERACAO_TELEFONE
│   │   │   ├── ControleAlteracaoEmail   Rate limiting de alteração de e-mail por usuário
│   │   │   ├── PoliticaSenha            Regras de complexidade de senha
│   │   │   ├── LoginPendente            Login/ação aguardando 2º fator (TOTP/OTP); expira em 5 min, máx 5 tentativas
│   │   │   ├── UsuarioIPConfiavel       IP marcado como confiável pelo usuário (sem verificação extra no próximo login)
│   │   │   └── CodigoBackup2fa          Backup codes 2FA hasheados com Argon2 (8 por usuário, uso único)
│   │   ├── event/   UsuarioCadastradoEvent
│   │   ├── listener/ UsuarioCadastradoListener — envia e-mail de boas-vindas pós-commit (@Async + @TransactionalEventListener)
│   │   ├── repository/  (JPA repositories para as entidades acima)
│   │   ├── service/
│   │   │   ├── AutenticacaoService      Cadastro e login local; retorna LoginPendenteResponse (202) se IP desconhecido
│   │   │   ├── ConfirmacaoService       Confirmação e reenvio de e-mail de verificação
│   │   │   ├── TokenConfirmacaoService  Criação, validação e encerramento de tokens de confirmação
│   │   │   ├── TokenRecuperacaoSenhaService  Fluxo de recuperação de senha
│   │   │   ├── EnvioCodigoRateLimitService  Rate limiting por IP para todos os envios de código (e-mail e SMS/WhatsApp)
│   │   │   ├── PoliticaSenhaService     Validação de força de senha
│   │   │   ├── LoginPendenteService     Criação, verificação, reenvio e limpeza de logins pendentes
│   │   │   ├── IpConfiavelService       Gerenciamento de IPs confiáveis por usuário
│   │   │   ├── CodigoBackupService      Geração (8 códigos XXXX-XXXX), uso e contagem de backup codes
│   │   │   ├── TotpService              Geração de segredo, URI otpauth://, validação de código TOTP (±1 período)
│   │   │   ├── DoisFatoresService       Orquestra setup, confirmação e desativação do TOTP; status e preferência
│   │   │   ├── ConfirmacaoEmailExpiracaoService  @Scheduled — remove contas não confirmadas após 7 dias
│   │   │   └── TokenRecuperacaoSenhaExpiracaoService  @Scheduled — limpa tokens expirados
│   │   └── util/
│   │       └── TokenUtils               gerarTokenSeguro() + gerarCodigoNumerico6Digitos() + gerarHash() (SHA-256)
│   │
│   ├── configuracao/
│   │   ├── entity/   ConfiguracaoAplicacao  — setup SMTP + flag setupConcluido
│   │   ├── repository/
│   │   └── service/
│   │       ├── SetupService             Leitura e persistência do setup
│   │       └── CriptografiaConfiguracaoService  Criptografia AES das credenciais SMTP (BouncyCastle)
│   │
│   ├── identidadeexterna/
│   │   ├── entity/
│   │   │   ├── IdentidadeExterna        Vínculo usuário ↔ provider externo (Google, etc.)
│   │   │   └── ProviderExterno          Enum: GOOGLE (extensível para Apple, GitHub...)
│   │   ├── repository/
│   │   └── service/
│   │       ├── GoogleAuthService        Login, vinculação e desvinculação com Google
│   │       └── GoogleIdTokenValidatorService  Validação do idToken emitido pelo Google
│   │
│   ├── notificacao/
│   │   ├── port/
│   │   │   └── NotificacaoTelefonePort  Interface: validarDisponibilidade() (síncrono, 503 se sem credenciais) + enviarCodigoVerificacao() (@Async)
│   │   └── service/
│   │       ├── EmailService             Envio de e-mails transacionais HTML via JavaMail (todos os métodos são @Async)
│   │       ├── IpGeolocalizacaoService  Geolocalização de IP via ip-api.com — retorna Optional<String> com "Cidade, Estado, País"
│   │       └── TwilioAdapter            Implementação @Primary de NotificacaoTelefonePort via Twilio SDK (WhatsApp ou SMS)
│   │
│   └── usuario/
│       ├── entity/   Usuario            UserDetails do Spring Security; campo providerOrigem
│       │                                registra qual OAuth provider originou o cadastro;
│       │                                getNomeParaEmail() retorna nomeExibicao ?: username
│       ├── repository/
│       └── service/
│           ├── ContaService             Gerenciamento da conta autenticada (username, nomeExibicao, e-mail, senha, exclusão com 2FA opcional)
│           ├── UsuarioService           Carregamento do usuário para autenticação; cadastrar() aceita username + nomeExibicao
│           └── UsernameValidator        Valida formato e bloqueia palavras reservadas
│
└── AuthLuizApplication.java            @SpringBootApplication + @EnableScheduling
```

## Migrações de banco (Flyway)

| Arquivo                      | Conteúdo                                                                 |
|------------------------------|--------------------------------------------------------------------------|
| `V1__schema_inicial.sql`     | Schema completo: `usuario`, `tokenRecuperacaoSenha`, `controleRecuperacaoSenha`, `configuracaoAplicacao`, `identidadeExterna`, `tokenConfirmacao`, `controleAlteracaoEmail` — com todos os `ON DELETE CASCADE` |
| `V2__adicionar_telefone_usuario.sql` | Adiciona colunas `telefone` (VARCHAR 20, nullable) e `telefoneVerificado` (boolean) à tabela `usuario` |
| `V3__unique_telefone_usuario.sql`    | Adiciona constraint `uq_usuario_telefone` — unicidade de telefone (NULLs múltiplos permitidos pelo PostgreSQL) |
| `V4__log_auditoria.sql`             | Cria tabela `log_auditoria` com índices em `idUsuario`, `criadoEm` e `acao` |
| `V5__ultimo_login_usuario.sql`      | Adiciona coluna `ultimoLogin` (TIMESTAMP WITH TIME ZONE, nullable) à tabela `usuario` |
| `V6__tentativas_erradas_tokens.sql` | Adiciona coluna `tentativasErradas` (SMALLINT, default 0) às tabelas `tokenConfirmacao` e `tokenRecuperacaoSenha` |
| `V7__tentativas_erradas_integer.sql` | Converte `tentativasErradas` de SMALLINT para INTEGER nas duas tabelas (alinha com o tipo Java `int`) |
| `V8__renomear_controle_recuperacao_senha.sql` | Renomeia tabela `controleRecuperacaoSenha` → `controleEnvioCodigoIp` (rate limiting agora é agnóstico ao canal) |
| `V9__telefone_pendente_usuario.sql`           | Adiciona coluna `telefonePendente` (VARCHAR 20, nullable) à tabela `usuario` |
| `V10__token_confirmacao_telefone_destino.sql` | Adiciona coluna `telefoneDestino` (VARCHAR 20, nullable) à tabela `tokenConfirmacao` |
| `V11`–`V18` | Configuração Twilio, auditoria configurável, renomeações de colunas, token blacklist, `publicId` na tabela `usuario` |
| `V19__token_cancelamento_recuperacao_senha.sql` | Adiciona coluna `tokenCancelamento` (VARCHAR 36, nullable) à tabela `tokenRecuperacaoSenha` |
| `V20__remover_token_cancelamento_recuperacao_senha.sql` | Remove coluna `tokenCancelamento` da tabela `tokenRecuperacaoSenha` |
| `V21__2fa_colunas_usuario.sql` | Adiciona colunas `ultimoIp`, `totpSecretPendente`, `totpSecret`, `totpAtivo`, `preferencia2fa` à tabela `usuario` |
| `V22__usuario_ip_confiavel.sql` | Cria tabela `usuarioIpConfiavel` com índice único em `(idUsuario, ip)` e `ON DELETE CASCADE` |
| `V23__codigo_backup_2fa.sql` | Cria tabela `codigoBackup2fa` para armazenar backup codes hasheados (Argon2) |
| `V24__login_pendente.sql` | Cria tabela `loginPendente` para o estado de login aguardando 2º fator; índices em `tokenPendente`, `idUsuario` e `expiraEm` |

> O DDL está em modo `validate`. Sempre crie um novo arquivo `V{n}__*.sql` para alterações no schema — nunca edite migrações existentes.

## Configuração

Copie `backend/.env.example` para `backend/.env` e preencha:

```env
APP_SETUP_MASTER_KEY=...          # chave para concluir o setup via POST /setup
SPRING_DATASOURCE_URL=...         # jdbc:postgresql://host:5432/db
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
JWT_RSA_PRIVATE_KEY=...           # chave privada RSA em base64 (PKCS#8)
JWT_RSA_PUBLIC_KEY=...            # chave pública RSA em base64 (X.509)
JWT_EXPIRATION_MINUTES=120
GOOGLE_OAUTH_CLIENT_ID=...        # client ID do Google Cloud Console
AUTH_LUIZ_SERVICE_KEY=...         # chave compartilhada com o PermLuiz para chamadas internas
APP_TOTP_ISSUER=AuthLuiz          # nome exibido no app autenticador TOTP (padrão: AuthLuiz)
AUDITORIA_ATIVIDADE=true          # habilita logs de atividade (padrão: true); logs de segurança sempre ativos
AUDITORIA_RETENCAO_DIAS=90        # dias de retenção dos logs antes da limpeza automática (padrão: 90)
TWILIO_ACCOUNT_SID=...            # Account SID do Twilio (obtenha em console.twilio.com)
TWILIO_AUTH_TOKEN=...             # Auth Token do Twilio
TWILIO_FROM_NUMBER=...            # Número remetente (+14155238886 para WhatsApp Sandbox)
TWILIO_CANAL=whatsapp             # Canal: "whatsapp" ou "sms" (padrão: whatsapp)
```

> Se as variáveis Twilio não estiverem configuradas, endpoints que iniciam ou reenviam código por telefone retornam HTTP 503. Nenhum estado é persistido nesse caso.

> Gere o par de chaves RSA executando `GerarChavesRSA.java` (disponível na raiz do backend). Consulte `backend/.env.example` para o procedimento completo.

## Geolocalização por IP

Dois e-mails de segurança exibem uma linha de **localização aproximada** (cidade, estado, país) baseada no IP do solicitante: o alerta de recuperação via telefone e a confirmação de redefinição de senha.

### Como funciona

1. `IpUtils.extrairIp(HttpServletRequest)` resolve o IP real do cliente na seguinte ordem de prioridade:
   - Header `X-Real-IP` (definido pelo nginx em produção Docker)
   - Primeiro elemento de `X-Forwarded-For` (proxies em geral)
   - `request.getRemoteAddr()` (fallback — IP direto da conexão TCP)

2. `IpGeolocalizacaoService.obterLocalizacao(ip)` recebe o IP e retorna `Optional<String>`:
   - Se o IP for **privado ou loopback** (127.0.0.1, ::1, 10.x, 172.16–31.x, 192.168.x) → retorna `empty()` imediatamente, sem chamada de rede.
   - Caso contrário, chama `http://ip-api.com/json/{ip}?fields=status,city,regionName,country&lang=pt-BR` com timeout de 3 s em conexão e leitura.
   - Se a API retornar `"status":"success"` → monta a string `"Cidade, Estado, País"` com os campos disponíveis.
   - Se a chamada falhar (timeout, erro de rede, status ≠ success) → loga `WARN` e retorna `empty()`.

3. O `EmailService` monta a linha de localização via `construirLinhaLocalizacao(ip)`: se `obterLocalizacao` retornar valor, insere uma `<tr>` na tabela de detalhes do e-mail; se retornar vazio, a string é `""` e o e-mail é enviado sem essa linha.

### Dependência externa

| API | `ip-api.com` |
|-----|-------------|
| Plano | Gratuito (sem autenticação) |
| Protocolo | HTTP (HTTPS exige plano pago — aceitável para lookup de cidade/país no lado servidor) |
| Limite | 45 requisições/minuto por IP de saída |
| Fallback | Sim — falhas não interrompem o envio do e-mail |

> Não há nenhuma credencial para configurar. O serviço funciona out-of-the-box em qualquer ambiente com acesso à internet de saída.

### Comportamento por ambiente

| Ambiente | IP visto pelo backend | Geo |
|----------|----------------------|-----|
| Dev local (`./mvnw spring-boot:run`) | `127.0.0.1` | ✗ privado, linha omitida |
| Docker Compose sem proxy | `172.17.0.1` (bridge Docker) | ✗ privado, linha omitida |
| Docker Compose com nginx proxy (`compose.yaml`) | IP real do cliente (via `X-Real-IP`) | ✓ funciona |
| Produção com proxy reverso próprio | IP real (via `X-Real-IP` ou `X-Forwarded-For`) | ✓ funciona |

### Adicionando geo a novos e-mails

Para incluir localização em um futuro e-mail de segurança:
1. Injete `IpGeolocalizacaoService` no `EmailService` (já injetado via `@RequiredArgsConstructor`).
2. Chame `construirLinhaLocalizacao(ip)` (método privado no `EmailService`) — retorna a `<tr>` HTML ou string vazia.
3. Passe o resultado como `%s` na posição desejada do template de tabela.
4. Certifique-se de que o IP chegou via `IpUtils.extrairIp()` no controller.

## Rodando

```bash
# Subir o banco
docker compose -f ../compose-dev.yaml up -d

# Iniciar a API (porta 8080)
./mvnw spring-boot:run

# Testes de integração
./mvnw test

# Gerar JAR
./mvnw package -DskipTests
```

## Endpoints

**Base URL:** `http://localhost:8080`
**Autenticação padrão:** `Authorization: Bearer <JWT>`
**Setup:** `X-Master-Key: <APP_SETUP_MASTER_KEY>`
**Interno:** `X-Service-Key: <AUTH_LUIZ_SERVICE_KEY>`

---

### Cadastro e login

**`POST /auth/cadastro`** — Pública

Cria uma nova conta. Envia e-mail de boas-vindas. O código de verificação de e-mail não é enviado aqui — o usuário solicita depois na tela de conta.

```json
{ "username": "joao_silva", "email": "joao@email.com", "senha": "@Senha123", "nomeExibicao": "João Silva" }
```

`nomeExibicao` é opcional. `username` deve ter 4–30 chars, começar com letra e conter apenas letras, números, ponto e underscore.

Resposta: `{ "publicId": "550e8400...", "username": "joao_silva", "nomeExibicao": "João Silva", "email": "joao@email.com", "mensagem": "Conta criada! Verifique seu e-mail para ativar a conta." }`

---

**`POST /auth/login`** — Pública

Login com e-mail, telefone **ou** username + senha. O backend detecta o tipo do `identificador` automaticamente (`@` → e-mail; `+` ou apenas dígitos → telefone; demais → username). Retorna JWT.

```json
{ "identificador": "joao@email.com", "senha": "@Senha123" }
```

```json
{ "identificador": "+5511999999999", "senha": "@Senha123" }
```

```json
{ "identificador": "joao_silva", "senha": "@Senha123" }
```

Resposta: `{ "token": "eyJ...", "username": "joao_silva", "nomeExibicao": "João Silva", "email": "...", "emailVerificado": true, "telefone": "...", "telefoneVerificado": false }`

---

### Google OAuth

**`POST /auth/oauth/google`** — Pública

Login ou cadastro via Google. O frontend obtém um `idToken` do Google Identity Services SDK e envia aqui. Se o e-mail já existe em uma conta local, retorna `409` — o usuário deve vincular pelo painel da conta, não fazer login pelo Google.

```json
{ "idToken": "eyJ..." }
```

---

**`POST /auth/oauth/google/vincular`** — JWT

Vincula o Google à conta autenticada. O e-mail da conta Google precisa ser idêntico ao e-mail da conta. Após vincular, o usuário pode fazer login pelo Google ou por senha (se tiver definida).

```json
{ "idToken": "eyJ..." }
```

---

**`DELETE /auth/oauth/google/vincular`** — JWT

Desvincula o Google da conta. Exige senha definida (para não perder o acesso) e confirmação por senha. Bloqueado para contas que foram **criadas** via Google (`providerOrigem = GOOGLE`) — essas contas não podem ser desvinculadas.

```json
{ "senha": "@Senha123" }
```

---

### Recuperação de senha

**`POST /auth/recuperacao/iniciar`** — Pública

Envia um código numérico de 6 dígitos para iniciar a recuperação. Informe `email` **ou** `telefone` (exatamente um dos dois). Rate limiting por IP. Para o canal de telefone, exige `telefoneVerificado = true` e credenciais Twilio configuradas (retorna `503` caso contrário).

```json
{ "email": "joao@email.com" }
```

```json
{ "telefone": "+5511999999999" }
```

---

**`POST /auth/recuperacao/validar`** — Pública

Valida o código de recuperação sem alterar a senha. Deve ser chamado antes de `/redefinir` para confirmar que o código está correto. Decrementa tentativas em caso de erro e bloqueia o token após 5 tentativas erradas (exige novo código via `/iniciar`).

```json
{ "email": "joao@email.com", "codigo": "123456" }
```

```json
{ "telefone": "+5511999999999", "codigo": "123456" }
```

---

**`POST /auth/recuperacao/redefinir`** — Pública

Redefine a senha usando o código previamente validado. Informe o mesmo identificador (`email` ou `telefone`) e o mesmo código da etapa anterior. O código expira em 5 minutos. Após redefinição bem-sucedida, um e-mail de confirmação é sempre enviado ao endereço cadastrado.

```json
{ "email": "joao@email.com", "codigo": "123456", "novaSenha": "@NovaSenha123" }
```

```json
{ "telefone": "+5511999999999", "codigo": "123456", "novaSenha": "@NovaSenha123" }
```

---

### Conta autenticada (`/auth/me`)

**`GET /auth/me`** — JWT

Retorna os dados completos da conta autenticada. Limpa automaticamente `emailPendente` e `telefonePendente` expirados.

```json
{
  "publicId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "joao_silva",
  "nomeExibicao": "João Silva",
  "email": "joao@email.com",
  "emailVerificado": true,
  "emailPendente": null,
  "telefone": "+5511999999999",
  "telefoneVerificado": true,
  "telefonePendente": null,
  "temLoginGoogle": false,
  "dataCriacao": "2026-01-01T00:00:00"
}
```

---

**`PATCH /auth/me/username`** — JWT

Atualiza o username da conta. Deve ter 4–30 chars, começar com letra, conter apenas letras, números, ponto e underscore. Usernames reservados são rejeitados.

```json
{ "username": "novo_username" }
```

---

**`PATCH /auth/me/nomeExibicao`** — JWT

Atualiza o nome de exibição (opcional, max 100 chars), usado nas saudações de e-mail. Enviar `null` ou string vazia remove o nome de exibição.

```json
{ "nomeExibicao": "João Silva" }
```

---

**`PATCH /auth/me/email`** — JWT

Solicita alteração de e-mail. O novo e-mail recebe um código de confirmação — o e-mail atual só é trocado após o código ser confirmado em `/auth/verificacao/email/confirmar`. Bloqueado para contas com Google vinculado. Exige e-mail verificado. Rate limiting por usuário (máx. 5 alterações por 24h) e por IP.

```json
{ "email": "novo@email.com" }
```

---

**`PATCH /auth/me/senha`** — JWT

Altera a senha existente ou define uma pela primeira vez (contas criadas via Google). Se o usuário já tem senha, `senhaAtual` é obrigatória. Exige e-mail verificado.

```json
{ "senhaAtual": "@SenhaAtual1", "novaSenha": "@NovaSenha1" }
```

Para definir senha pela primeira vez (sem senha atual): omita `senhaAtual`.

---

**`PATCH /auth/me/telefone`** — JWT

Inicia a alteração de telefone: salva o número em `telefonePendente` e envia código de verificação via WhatsApp. O campo `telefone` só é atualizado após a confirmação. Enviar `null` remove o telefone diretamente, sem verificação. Retorna `503` se as credenciais Twilio não estiverem configuradas. Exige e-mail verificado.

```json
{ "telefone": "+5511999999999" }
```

```json
{ "telefone": null }
```

---

**`POST /auth/me/exclusao/codigo`** — JWT

Envia um código OTP por e-mail ou SMS para confirmar a exclusão de conta. Só disponível para usuários com `verificacaoExtraAtiva = true` e **sem** TOTP ativo (usuários com TOTP usam o código do app diretamente). Retorna `LoginPendenteResponse` com `tokenPendente` a ser usado no `DELETE /auth/me`.

---

**`DELETE /auth/me`** — JWT

Exclui permanentemente a conta. Se o usuário tem senha definida, deve confirmá-la. Se `verificacaoExtraAtiva = true`, também é obrigatório informar o código 2FA:

- **TOTP ativo:** `{ "senha": "...", "codigo": "<código do app>" }`
- **E-mail/SMS 2FA (sem TOTP):** `{ "senha": "...", "tokenPendente": "...", "codigo": "<OTP do e-mail>" }` — o `tokenPendente` vem do `POST /auth/me/exclusao/codigo`

```json
{ "senha": "@Senha123" }
```

```json
{ "senha": "@Senha123", "codigo": "123456" }
```

```json
{ "senha": "@Senha123", "tokenPendente": "abc123...", "codigo": "654321" }
```

Contas sem senha (criadas via Google sem senha definida): omita o campo `senha`.

---

### Verificação de e-mail (`/auth/verificacao/email`)

**`POST /auth/verificacao/email/enviar`** — JWT

Envia o código de verificação por e-mail. Detecta automaticamente o tipo pendente:
- Se `emailVerificado = false` → envia código de verificação de cadastro
- Se `emailPendente != null` → envia código de confirmação da alteração de e-mail

Cooldown de 2 minutos entre envios. Rate limiting por IP.

---

**`POST /auth/verificacao/email/confirmar`** — JWT

Confirma o e-mail com o código recebido. Detecta automaticamente o tipo pendente (cadastro ou alteração) — o mesmo endpoint serve para ambos os casos. Bloqueia após 5 tentativas erradas (novo código necessário).

```json
{ "codigo": "123456" }
```

---

### Verificação de telefone (`/auth/verificacao/telefone`)

**`POST /auth/verificacao/telefone/enviar`** — JWT

Reenvia o código de verificação para o `telefonePendente` via WhatsApp. Retorna `503` se as credenciais Twilio não estiverem configuradas (nenhum token é criado nesse caso). Cooldown de 2 minutos entre envios. Rate limiting por IP.

---

**`POST /auth/verificacao/telefone/confirmar`** — JWT

Confirma a alteração de telefone com o código recebido. Move `telefonePendente` → `telefone` e define `telefoneVerificado = true`. Bloqueia após 5 tentativas erradas.

```json
{ "codigo": "123456" }
```

---

### Setup (`/setup`)

**`GET /setup`** e **`POST /setup`** — `X-Master-Key`

Configuração inicial da aplicação (credenciais SMTP, etc.). Protegido pela chave mestra `APP_SETUP_MASTER_KEY`. Todos os outros endpoints ficam bloqueados até o setup ser concluído.

---

### Endpoints de sistema

**`GET /auth/.well-known/jwks.json`** — Pública

Expõe a chave pública RSA no formato JWKS. Usado pelo PermLuiz (e qualquer serviço externo) para validar JWTs do AuthLuiz sem compartilhar segredos.

---

**`GET /auth/interno/usuarios`** — `X-Service-Key`

Lista todos os usuários. Endpoint server-to-server — não aceita JWT, apenas o header `X-Service-Key`. Usado pelo PermLuiz para exibir a lista de usuários no painel de admin.

---

### Verificação de login pendente (`/auth/login`)

**`POST /auth/login/verificar`** — Pública (usa `tokenPendente`)

Confirma o 2º fator após um login que retornou 202. Opcionalmente marca o IP como confiável.

```json
{ "tokenPendente": "...", "codigo": "123456", "confiarEsteIp": true, "rotuloDispositivo": "Notebook pessoal" }
```

Retorna `LoginResponse` com JWT (igual ao `POST /auth/login` 200). Retorna 401 com tentativas restantes se o código for inválido.

---

**`POST /auth/login/reenviar`** — Pública (usa `tokenPendente`)

Envia novo código para o destino original. Não disponível para tipo TOTP (400). Rate limiting por IP.

```json
{ "tokenPendente": "..." }
```

---

**`POST /auth/login/codigo-backup`** — Pública (usa `tokenPendente`)

Usa um código de backup 2FA (formato `XXXX-XXXX`) para concluir o login pendente. O código é invalidado após uso.

```json
{ "tokenPendente": "...", "codigoBackup": "ABCD-EF12" }
```

Retorna `LoginResponse` com JWT.

---

### Autenticação de dois fatores (`/auth/me/2fa`)

**`POST /auth/me/2fa/totp/iniciar`** — JWT

Gera um segredo TOTP e retorna a URI `otpauth://` para gerar o QR code no frontend.

Resposta: `{ "otpauthUri": "otpauth://totp/AuthLuiz:email@...?secret=...&issuer=AuthLuiz" }`

---

**`POST /auth/me/2fa/totp/confirmar`** — JWT

Confirma o setup TOTP com o primeiro código do app autenticador. Ativa o TOTP e gera 8 backup codes.

```json
{ "codigo": "123456" }
```

Resposta: `{ "codigosBackup": ["ABCD-1234", ...] }` — exibidos apenas uma vez.

---

**`DELETE /auth/me/2fa`** — JWT

Desativa o 2FA (TOTP + backup codes removidos). Exige senha da conta para confirmar.

```json
{ "senha": "@Senha123" }
```

---

**`POST /auth/me/2fa/backup-codes/regerar`** — JWT

Gera novos 8 backup codes, invalidando os anteriores. Exige código TOTP atual.

```json
{ "codigo": "123456" }
```

---

**`GET /auth/me/2fa/status`** — JWT

Retorna o status atual do 2FA da conta.

```json
{ "totpAtivo": true, "codigosRestantes": 6, "preferencia2fa": "EMAIL" }
```

---

**`PATCH /auth/me/2fa/verificacao-extra`** — JWT

Ativa ou desativa a verificação extra por IP desconhecido. Ao **desativar** (`ativo: false`), exige confirmação de senha para contas com senha definida. Ativar não requer senha.

```json
{ "ativo": true }
```

```json
{ "ativo": false, "senha": "@Senha123" }
```

---

**`PATCH /auth/me/2fa/preferencia`** — JWT

Define o canal de verificação para contas sem TOTP (usado quando o IP é desconhecido).

```json
{ "canal": "EMAIL" }
```

Valores aceitos: `EMAIL`, `SMS`, `WHATSAPP`.

---

### IPs confiáveis (`/auth/me/ips-confiaveis`)

**`GET /auth/me/ips-confiaveis`** — JWT

Lista os IPs confiáveis da conta.

---

**`POST /auth/me/ips-confiaveis`** — JWT

Adiciona um IP manualmente.

```json
{ "ip": "189.0.0.1", "rotulo": "Escritório" }
```

---

**`DELETE /auth/me/ips-confiaveis/{id}`** — JWT

Remove um IP confiável específico.

---

**`DELETE /auth/me/ips-confiaveis`** — JWT

Remove todos os IPs confiáveis da conta.
