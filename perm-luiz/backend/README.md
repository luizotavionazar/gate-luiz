# PermLuiz — Backend

API REST de controle de acesso construída com Spring Boot e Java 21. Stateless, valida JWTs do AuthLuiz via JWKS, e gerencia roles, permissões e atribuições de usuários.

## Stack

- **Java 21** + **Spring Boot**
- **Spring Security** — OAuth2 Resource Server (JWT RS256 via JWKS), stateless
- **PostgreSQL** + **Flyway** — banco relacional com migrações versionadas
- **Lombok** — redução de boilerplate nas entidades e serviços
- **Testcontainers** — testes de integração com PostgreSQL real (sem mocks)

> Não há envio de e-mail, OAuth, criptografia de credenciais SMTP nem hash de senha — estas responsabilidades pertencem ao AuthLuiz.

## Estrutura do projeto

```
src/main/java/.../permluiz/
│
├── api/                             Camada HTTP (controllers + DTOs)
│   ├── admin/
│   │   ├── AdminRoleController      GET/POST /admin/roles
│   │   │                            PUT/DELETE /admin/roles/{id}
│   │   │                            GET/PUT /admin/roles/{id}/permissoes
│   │   ├── AdminPermissaoController GET/POST /admin/permissoes
│   │   │                            PUT/DELETE /admin/permissoes/{id}
│   │   ├── AdminUsuarioController   GET /admin/usuarios (lista todos com roles via AuthLuiz)
│   │   │                            GET /admin/usuarios/{id}/roles
│   │   │                            POST/DELETE /admin/usuarios/{id}/roles/{roleId}
│   │   └── dto/                     RoleRequest/Response, PermissaoRequest/Response,
│   │                                UsuarioComRolesResponse
│   ├── setup/
│   │   ├── SetupController          GET /setup
│   │   └── SetupService             Retorna adminConfigurado (idAdminMestre != null)
│   └── usuario/
│       └── MeController             GET /me/roles
│
├── config/
│   ├── auditoria/
│   │   ├── Auditavel                @interface — anota métodos de controller a auditar
│   │   └── AuditoriaAspect         @Aspect — intercepta @Auditavel, extrai IP/userId e persiste log
│   └── security/
│       ├── SecurityConfig           Regras de autorização, CORS, OAuth2 resource server (JWKS)
│       ├── AdminVerificador         Verifica admin mestre; auto-promove 1º usuário autenticado
│       └── JsonAuthenticationEntryPoint  Resposta JSON para 401
│
├── domain/
│   ├── auditoria/
│   │   ├── entity/   LogAuditoria         Registro de auditoria: ação, categoria, IP, userId, resultado
│   │   ├── enums/
│   │   │   ├── AcaoAuditoria             ROLE_CRIADA, PERMISSAO_CRIADA, ROLE_USUARIO_ATRIBUIDA...
│   │   │   └── CategoriaAuditoria        ATIVIDADE (configurável via AUDITORIA_ATIVIDADE)
│   │   ├── repository/ LogAuditoriaRepository
│   │   └── service/
│   │       ├── AuditoriaService          Persiste registros de log
│   │       └── AuditoriaLimpezaService   @Scheduled (03:00) — exclui logs mais antigos que retencao-dias
│   ├── configuracao/
│   │   ├── entity/  ConfiguracaoAplicacao   Singleton: idAdminMestre (null = sem admin ainda)
│   │   └── ConfiguracaoAplicacaoRepository
│   ├── role/
│   │   ├── entity/  Role                    @ManyToMany com Permissao
│   │   └── RoleRepository
│   ├── permissao/
│   │   ├── entity/  Permissao               recurso + acao (únicos em par)
│   │   └── PermissaoRepository
│   └── usuariorole/
│       ├── entity/  UsuarioRole             @IdClass(UsuarioRoleId); idUsuario é Long (sem FK cross-DB)
│       │            UsuarioRoleId           Chave composta: idUsuario + idRole
│       └── UsuarioRoleRepository
│
└── infra/
    ├── AuthLuizClient       Chamada server-to-server para AuthLuiz (X-Service-Key)
    └── UsuarioAuthResponse  DTO do usuário retornado pelo AuthLuiz
│
└── PermLuizApplication.java
```

## Migrações de banco (Flyway)

| Arquivo                  | Conteúdo                                                                       |
|--------------------------|--------------------------------------------------------------------------------|
| `V1__schema_inicial.sql`     | Schema completo: `configuracaoAplicacao`, `role`, `permissao`, `rolePermissao`, `usuarioRole` |
| `V2__log_auditoria.sql`      | Cria tabela `log_auditoria` com índices em `idUsuario`, `criadoEm` e `acao` |

> O DDL está em modo `validate`. Sempre crie um novo arquivo `V{n}__*.sql` para alterações no schema — nunca edite migrações existentes.

## Configuração

Copie `backend/.env.example` para `backend/.env` e preencha:

```env
SPRING_DATASOURCE_URL=...          # jdbc:postgresql://host:5432/permluiz
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
AUTH_LUIZ_JWKS_URI=...             # http://localhost:8080/auth/.well-known/jwks.json
AUTH_LUIZ_BASE_URL=...             # http://localhost:8080
AUTH_LUIZ_SERVICE_KEY=...          # chave compartilhada com o AuthLuiz (mesma em ambos)
AUDITORIA_ATIVIDADE=true           # habilita logs de atividade de admin (padrão: true)
AUDITORIA_RETENCAO_DIAS=90         # dias de retenção dos logs antes da limpeza automática (padrão: 90)
```

## Rodando

```bash
# Subir o banco de desenvolvimento
docker compose -f ../compose-dev.yaml up -d

# Iniciar a API (porta 8081 por padrão no compose, 8080 internamente)
./mvnw spring-boot:run

# Testes de integração
./mvnw test

# Gerar JAR
./mvnw package -DskipTests
```

## Endpoints

### Setup

| Método | Caminho  | Auth    | Descrição                                                |
|--------|----------|---------|----------------------------------------------------------|
| GET    | `/setup` | Pública | Retorna `{ "adminConfigurado": true/false }`             |

### Usuário autenticado

| Método | Caminho      | Auth | Descrição                                             |
|--------|--------------|------|-------------------------------------------------------|
| GET    | `/me/roles`  | JWT  | Retorna os roles e permissões do usuário autenticado  |
| GET    | `/me/admin`  | JWT  | Retorna `{ "isAdmin": true/false }` — se o usuário é admin mestre |

### Admin mestre

| Método | Caminho                               | Auth      | Descrição                              |
|--------|---------------------------------------|-----------|----------------------------------------|
| GET    | `/admin/roles`                        | JWT+Admin | Lista todos os roles                   |
| POST   | `/admin/roles`                        | JWT+Admin | Cria role                              |
| PUT    | `/admin/roles/{id}`                   | JWT+Admin | Atualiza role                          |
| DELETE | `/admin/roles/{id}`                   | JWT+Admin | Remove role                            |
| GET    | `/admin/roles/{id}/permissoes`        | JWT+Admin | Lista permissões de um role            |
| PUT    | `/admin/roles/{id}/permissoes`        | JWT+Admin | Redefine permissões de um role (lista de IDs) |
| GET    | `/admin/permissoes`                   | JWT+Admin | Lista todas as permissões              |
| POST   | `/admin/permissoes`                   | JWT+Admin | Cria permissão (`recurso` + `acao`)    |
| PUT    | `/admin/permissoes/{id}`              | JWT+Admin | Atualiza permissão                     |
| DELETE | `/admin/permissoes/{id}`              | JWT+Admin | Remove permissão                       |
| GET    | `/admin/usuarios`                     | JWT+Admin | Lista todos os usuários (via AuthLuiz) com seus roles |
| GET    | `/admin/usuarios/{id}/roles`          | JWT+Admin | Lista roles de um usuário              |
| POST   | `/admin/usuarios/{id}/roles`          | JWT+Admin | Atribui role ao usuário                |
| DELETE | `/admin/usuarios/{id}/roles/{roleId}` | JWT+Admin | Remove role do usuário                 |
