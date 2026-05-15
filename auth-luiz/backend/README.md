# AuthLuiz — Backend

API REST de autenticação construída com Spring Boot 4 e Java 21. Stateless, baseada em JWT, com suporte a OAuth via Google, confirmação de e-mail, recuperação de senha e setup inicial guiado.

## Stack

- **Java 21** + **Spring Boot 4**
- **Spring Security** — OAuth2 Resource Server (JWT), stateless
- **PostgreSQL** + **Flyway** — banco relacional com migrações versionadas
- **Argon2** — hash de senhas (via Spring Security)
- **RS256 / RSA assimétrico (Nimbus)** — assinatura dos JWTs; chave pública exposta via JWKS
- **JavaMail** — envio de e-mail transacional
- **BouncyCastle** — criptografia das credenciais SMTP no banco
- **Lombok** — redução de boilerplate nas entidades e serviços
- **Testcontainers** — testes de integração com PostgreSQL real (sem mocks)

## Estrutura do projeto

```
src/main/java/.../authluiz/
│
├── api/                             Camada HTTP (controllers + DTOs)
│   ├── autenticacao/
│   │   ├── controller/
│   │   │   ├── AutenticacaoController   POST /auth/cadastro, /auth/login
│   │   │   │                            POST /auth/recuperacao/iniciar, /auth/recuperacao/redefinir
│   │   │   └── ConfirmacaoController    POST /auth/verificacao/email/confirmar (JWT)
│   │   │                                POST /auth/verificacao/email/enviar (JWT)
│   │   │                                POST /auth/verificacao/telefone/confirmar (JWT)
│   │   │                                POST /auth/verificacao/telefone/enviar (JWT)
│   │   └── dto/                         CadastroRequest/Response, LoginRequest/Response,
│   │                                    RecuperacaoSenhaRequest, RedefinirSenhaRequest,
│   │                                    ConfirmarEmailRequest, ContaResponse, MensagemResponse
│   ├── common/
│   │   ├── exception/  ExcecaoLimiteTentativas
│   │   └── handler/    ApiExceptionHandler  — trata ResponseStatusException globalmente
│   ├── conta/
│   │   ├── controller/ ContaController      GET/PATCH /auth/me, DELETE /auth/me
│   │   └── dto/        AtualizarNomeRequest, AtualizarEmailRequest,
│   │                   AtualizarSenhaRequest, AtualizarTelefoneRequest, DeletarContaRequest
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
│   │   │   ├── TokenRecuperacaoSenha    Token hasheado (SHA-256) para redefinição de senha
│   │   │   ├── ControleEnvioCodigoIp    Rate limiting por IP para qualquer envio de código (e-mail ou futuro SMS/WhatsApp)
│   │   │   ├── TokenConfirmacao         Token hasheado para verificação de cadastro e alteração de e-mail
│   │   │   ├── TipoTokenConfirmacao     Enum: VERIFICACAO_CADASTRO | ALTERACAO_EMAIL | ALTERACAO_TELEFONE
│   │   │   ├── ControleAlteracaoEmail   Rate limiting de alteração de e-mail por usuário
│   │   │   └── PoliticaSenha            Regras de complexidade de senha
│   │   ├── event/   UsuarioCadastradoEvent
│   │   ├── listener/ UsuarioCadastradoListener — envia e-mail de boas-vindas pós-commit (@Async + @TransactionalEventListener)
│   │   ├── repository/  (JPA repositories para as entidades acima)
│   │   ├── service/
│   │   │   ├── AutenticacaoService      Cadastro e login local
│   │   │   ├── ConfirmacaoService       Confirmação e reenvio de e-mail de verificação
│   │   │   ├── TokenConfirmacaoService  Criação, validação e encerramento de tokens de confirmação
│   │   │   ├── TokenRecuperacaoSenhaService  Fluxo de recuperação de senha
│   │   │   ├── EnvioCodigoRateLimitService  Rate limiting por IP para todos os envios de código (e-mail e futuro SMS/WhatsApp)
│   │   │   ├── PoliticaSenhaService     Validação de força de senha
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
│   │       └── TwilioAdapter            Implementação @Primary de NotificacaoTelefonePort via Twilio SDK (WhatsApp ou SMS)
│   │
│   └── usuario/
│       ├── entity/   Usuario            UserDetails do Spring Security; campo providerOrigem
│       │                                registra qual OAuth provider originou o cadastro
│       ├── repository/
│       └── service/
│           ├── ContaService             Gerenciamento da conta autenticada (nome, e-mail, senha, exclusão)
│           └── UsuarioService           Carregamento do usuário para autenticação
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
AUDITORIA_ATIVIDADE=true          # habilita logs de atividade (padrão: true); logs de segurança sempre ativos
AUDITORIA_RETENCAO_DIAS=90        # dias de retenção dos logs antes da limpeza automática (padrão: 90)
TWILIO_ACCOUNT_SID=...            # Account SID do Twilio (obtenha em console.twilio.com)
TWILIO_AUTH_TOKEN=...             # Auth Token do Twilio
TWILIO_FROM_NUMBER=...            # Número remetente (+14155238886 para WhatsApp Sandbox)
TWILIO_CANAL=whatsapp             # Canal: "whatsapp" ou "sms" (padrão: whatsapp)
```

> Se as variáveis Twilio não estiverem configuradas, endpoints que iniciam ou reenviam código por telefone retornam HTTP 503. Nenhum estado é persistido nesse caso.

> Gere o par de chaves RSA executando `GerarChavesRSA.java` (disponível na raiz do backend). Consulte `backend/.env.example` para o procedimento completo.

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

Cria uma nova conta com e-mail e senha. Envia e-mail de boas-vindas. O código de verificação de e-mail não é enviado aqui — o usuário solicita depois na tela de conta.

```json
{ "nome": "João", "email": "joao@email.com", "senha": "@Senha123" }
```

Resposta: `{ "idUsuario": 1, "nome": "João", "email": "joao@email.com", "mensagem": "Conta criada! Verifique seu e-mail para ativar a conta." }`

---

**`POST /auth/login`** — Pública

Login com e-mail ou telefone + senha. Retorna JWT.

```json
{ "identificador": "joao@email.com", "senha": "@Senha123" }
```

```json
{ "identificador": "+5511999999999", "senha": "@Senha123" }
```

Resposta: `{ "token": "eyJ...", "nome": "João", "email": "...", "emailVerificado": true, "telefone": "...", "telefoneVerificado": false }`

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

Envia um código numérico de 6 dígitos por e-mail para iniciar a recuperação. Rate limiting por IP.

```json
{ "email": "joao@email.com" }
```

---

**`POST /auth/recuperacao/redefinir`** — Pública

Redefine a senha usando o código recebido por e-mail. O código expira em 5 minutos e bloqueia após 5 tentativas erradas.

```json
{ "email": "joao@email.com", "codigo": "123456", "novaSenha": "@NovaSenha123" }
```

---

### Conta autenticada (`/auth/me`)

**`GET /auth/me`** — JWT

Retorna os dados completos da conta autenticada. Limpa automaticamente `emailPendente` e `telefonePendente` expirados.

```json
{
  "idUsuario": 1,
  "nome": "João",
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

**`PATCH /auth/me/nome`** — JWT

Atualiza o nome do usuário. Exige e-mail verificado.

```json
{ "nome": "João Silva" }
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

**`DELETE /auth/me`** — JWT

Exclui permanentemente a conta. Se o usuário tem senha definida, deve confirmá-la.

```json
{ "senha": "@Senha123" }
```

Contas sem senha (criadas via Google sem senha definida): envie body vazio `{}`.

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
