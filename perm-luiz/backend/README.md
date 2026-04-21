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
│   │   ├── AdminUsuarioController   GET /admin/usuarios/{id}/roles
│   │   │                            POST/DELETE /admin/usuarios/{id}/roles/{roleId}
│   │   └── dto/                     RoleRequest/Response, PermissaoRequest/Response
│   ├── setup/
│   │   ├── SetupController          GET/POST /setup
│   │   ├── SetupService             Lógica do setup inicial
│   │   └── SetupRequest             { "idUsuario": 1 }
│   └── usuario/
│       └── MeController             GET /me/roles
│
├── config/
│   ├── security/
│   │   ├── SecurityConfig           Regras de autorização, CORS, OAuth2 resource server (JWKS)
│   │   ├── AdminVerificador         Verifica se o JWT pertence ao admin mestre
│   │   └── JsonAuthenticationEntryPoint  Resposta JSON para 401
│   └── setup/
│       └── SetupFilter              Retorna 503 se setup não estiver concluído
│
├── domain/
│   ├── configuracao/
│   │   ├── entity/  ConfiguracaoAplicacao   Singleton: setupConcluido + idAdminMestre
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
└── PermLuizApplication.java
```

## Migrações de banco (Flyway)

| Arquivo                  | Conteúdo                                                                       |
|--------------------------|--------------------------------------------------------------------------------|
| `V1__schema_inicial.sql` | Schema completo: `configuracaoAplicacao`, `role`, `permissao`, `rolePermissao`, `usuarioRole` |

> O DDL está em modo `validate`. Sempre crie um novo arquivo `V{n}__*.sql` para alterações no schema — nunca edite migrações existentes.

## Configuração

Copie `backend/.env.example` para `backend/.env` e preencha:

```env
APP_SETUP_MASTER_KEY=...           # chave para concluir o setup via POST /setup
SPRING_DATASOURCE_URL=...          # jdbc:postgresql://host:5432/permluiz
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
AUTH_LUIZ_JWKS_URI=...             # http://localhost:8080/auth/.well-known/jwks.json
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

| Método | Caminho  | Auth         | Descrição                                      |
|--------|----------|--------------|------------------------------------------------|
| GET    | `/setup` | Pública      | Verifica se o setup foi concluído              |
| POST   | `/setup` | Chave mestra | Define o admin mestre (`{ "idUsuario": 1 }`)   |

### Usuário autenticado

| Método | Caminho     | Auth | Descrição                                             |
|--------|-------------|------|-------------------------------------------------------|
| GET    | `/me/roles` | JWT  | Retorna os roles e permissões do usuário autenticado  |

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
| GET    | `/admin/usuarios/{id}/roles`          | JWT+Admin | Lista roles de um usuário              |
| POST   | `/admin/usuarios/{id}/roles`          | JWT+Admin | Atribui role ao usuário                |
| DELETE | `/admin/usuarios/{id}/roles/{roleId}` | JWT+Admin | Remove role do usuário                 |
