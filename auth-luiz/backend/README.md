# AuthLuiz — Backend

API REST de autenticação construída com Spring Boot 3 e Java 21. Stateless, baseada em JWT, com suporte a OAuth via Google, confirmação de e-mail, recuperação de senha e setup inicial guiado.

## Stack

- **Java 21** + **Spring Boot 3**
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
│   │   │   │                            POST /auth/recuperacao/**
│   │   │   └── ConfirmacaoController    GET  /auth/verificacao/confirmar
│   │   │                                POST /auth/verificacao/reenviar
│   │   └── dto/                         CadastroRequest/Response, LoginRequest/Response,
│   │                                    RecuperacaoSenhaRequest, RedefinirSenhaRequest,
│   │                                    ContaResponse, MensagemResponse
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
│   ├── autenticacao/
│   │   ├── entity/
│   │   │   ├── TokenRecuperacaoSenha    Token hasheado (SHA-256) para redefinição de senha
│   │   │   ├── ControleRecuperacaoSenha Rate limiting de recuperação por IP
│   │   │   ├── TokenConfirmacao         Token hasheado para verificação de cadastro e alteração de e-mail
│   │   │   ├── TipoTokenConfirmacao     Enum: VERIFICACAO_CADASTRO | ALTERACAO_EMAIL
│   │   │   ├── ControleAlteracaoEmail   Rate limiting de alteração de e-mail por usuário
│   │   │   └── PoliticaSenha            Regras de complexidade de senha
│   │   ├── event/   UsuarioCadastradoEvent
│   │   ├── listener/ UsuarioCadastradoListener — envia e-mail de boas-vindas ou verificação pós-commit
│   │   ├── repository/  (JPA repositories para as entidades acima)
│   │   ├── service/
│   │   │   ├── AutenticacaoService      Cadastro e login local
│   │   │   ├── ConfirmacaoService       Confirmação e reenvio de e-mail de verificação
│   │   │   ├── TokenConfirmacaoService  Criação, validação e encerramento de tokens de confirmação
│   │   │   ├── TokenRecuperacaoSenhaService  Fluxo de recuperação de senha
│   │   │   ├── PoliticaSenhaService     Validação de força de senha
│   │   │   ├── ConfirmacaoEmailExpiracaoService  @Scheduled — remove contas não confirmadas após 7 dias
│   │   │   └── TokenRecuperacaoSenhaExpiracaoService  @Scheduled — limpa tokens expirados
│   │   └── util/
│   │       └── TokenUtils               gerarTokenSeguro() + gerarHash() (SHA-256)
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
│   │   └── service/
│   │       └── EmailService             Envio de e-mails transacionais via JavaMail (todos os métodos são @Async)
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
```

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

| Método      | Caminho                            | Auth         | Descrição                                          |
|-------------|------------------------------------|--------------|----------------------------------------------------|
| POST        | `/auth/cadastro`                   | Pública      | Cadastro com e-mail e senha                        |
| POST        | `/auth/login`                      | Pública      | Login local, retorna JWT                           |
| POST        | `/auth/oauth/google`               | Pública      | Login/cadastro via Google (409 se e-mail já existe)|
| POST        | `/auth/oauth/google/vincular`      | JWT          | Vincula Google à conta (e-mail Google = e-mail conta) |
| DELETE      | `/auth/oauth/google/vincular`      | JWT          | Desvincula Google (exige senha definida; bloqueado para contas criadas via Google) |
| POST        | `/auth/recuperacao/iniciar`        | Pública      | Inicia recuperação de senha                        |
| GET         | `/auth/recuperacao/validar`        | Pública      | Valida token de recuperação                        |
| POST        | `/auth/recuperacao/redefinir`      | Pública      | Redefine senha com token válido                    |
| GET         | `/auth/me`                         | JWT          | Dados da conta autenticada                         |
| PATCH       | `/auth/me/nome`                    | JWT          | Atualiza nome                                      |
| PATCH       | `/auth/me/email`                   | JWT          | Solicita alteração de e-mail (sempre envia confirmação) |
| PATCH       | `/auth/me/senha`                   | JWT          | Altera ou define senha                             |
| PATCH       | `/auth/me/telefone`                | JWT          | Atualiza ou remove telefone (null remove; sempre define telefoneVerificado=false) |
| DELETE      | `/auth/me`                         | JWT          | Exclui a conta                                     |
| GET         | `/auth/verificacao/confirmar`      | Pública      | Confirma e-mail via token (cadastro ou alteração)  |
| POST        | `/auth/verificacao/reenviar`       | JWT          | Reenvia e-mail de verificação de cadastro (cooldown: 2 min) |
| POST        | `/auth/verificacao/reenviar-alteracao-email` | JWT | Reenvia e-mail de confirmação de alteração de e-mail (cooldown: 2 min) |
| GET / POST  | `/setup/**`                        | Chave mestra | Configuração inicial                               |
| GET         | `/auth/.well-known/jwks.json`      | Pública      | Chave pública RSA no formato JWKS (usado pelo PermLuiz para validar JWTs) |
| GET         | `/auth/interno/usuarios`           | X-Service-Key | Lista todos os usuários — endpoint server-to-server, não aceita JWT |
