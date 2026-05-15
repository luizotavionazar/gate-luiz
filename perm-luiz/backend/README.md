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
│   │   │                            GET/PUT /admin/roles/{id}/permissions
│   │   ├── AdminPermissaoController GET/POST /admin/permissions
│   │   │                            PUT/DELETE /admin/permissions/{id}
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

**Base URL:** `http://localhost:8081`
**Autenticação padrão:** `Authorization: Bearer <JWT emitido pelo AuthLuiz>`
**JWT+Admin:** exige que o subject do JWT seja o `idAdminMestre` registrado no banco.

---

### Setup

**`GET /setup`** — Pública

Retorna se o admin mestre já foi configurado. O frontend usa para saber se deve exibir o aviso de primeiro acesso.

```json
{ "adminConfigurado": false }
```

---

### Minha conta (`/me`)

**`GET /me/admin`** — JWT

Verifica se o usuário autenticado é o admin mestre. Comportamento especial: se ainda não existe nenhum admin configurado, o primeiro usuário a chamar esse endpoint é automaticamente promovido a admin. O frontend do AuthLuiz usa isso para decidir se exibe o botão "Painel de Controle".

```json
{ "isAdmin": true }
```

**`DELETE /me/admin`** — JWT

Reseta o admin mestre (define `idAdminMestre` como nulo). Existe para o caso em que o admin deleta sua própria conta no AuthLuiz — o AuthLuiz chama esse endpoint automaticamente para não deixar o sistema preso com um admin inexistente.

**`GET /me/roles`** — JWT

Retorna as roles do usuário autenticado e as permissões de cada role. É o endpoint que uma aplicação usa em tempo real para verificar o que o usuário pode fazer — ex: "esse usuário tem a permissão `ingresso:comprar`?"

```json
{
  "idUsuario": 1,
  "roles": [
    {
      "id": 2,
      "nome": "Comprador",
      "permissions": [
        { "id": 1, "recurso": "ingresso", "acao": "comprar" }
      ]
    }
  ]
}
```

---

### Roles (`/admin/roles`)

**`GET /admin/roles`** — JWT+Admin

Lista todas as roles cadastradas no sistema (ex: "Comprador", "Organizador", "Fiscal").

**`POST /admin/roles`** — JWT+Admin

Cria uma nova role.

```json
{ "nome": "Organizador", "descricao": "Pode criar e gerenciar eventos" }
```

**`PUT /admin/roles/{id}`** — JWT+Admin

Atualiza nome e descrição de uma role existente. Mesmo body do POST.

**`DELETE /admin/roles/{id}`** — JWT+Admin

Remove uma role. Retorna `409` se ainda está atribuída a algum usuário — remova de todos os usuários antes de deletar.

**`GET /admin/roles/{id}/permissions`** — JWT+Admin

Lista as permissões vinculadas a uma role específica.

**`PUT /admin/roles/{id}/permissions`** — JWT+Admin

Redefine completamente as permissões de uma role. Envia um array com os IDs das permissões que a role deve ter — o conteúdo anterior é substituído por inteiro.

```json
[1, 3, 5]
```

---

### Permissions (`/admin/permissions`)

**`GET /admin/permissions`** — JWT+Admin

Lista todas as permissões cadastradas. Cada permissão representa uma ação sobre um recurso, no formato `recurso:acao` (ex: `ingresso:comprar`).

**`POST /admin/permissions`** — JWT+Admin

Cria uma nova permissão. `recurso` e `acao` são salvos em minúsculo automaticamente. O par `recurso + acao` deve ser único.

```json
{ "recurso": "ingresso", "acao": "comprar", "descricao": "Permite comprar ingressos" }
```

**`PUT /admin/permissions/{id}`** — JWT+Admin

Atualiza os dados de uma permissão. Mesmo body do POST.

**`DELETE /admin/permissions/{id}`** — JWT+Admin

Remove uma permissão. Retorna `409` se ainda está vinculada a alguma role — remova das roles antes de deletar.

---

### Usuários (`/admin/usuarios`)

**`GET /admin/usuarios`** — JWT+Admin

Lista todos os usuários cadastrados no AuthLuiz (via chamada server-to-server com `X-Service-Key`) junto com as roles que cada um tem no PermLuiz. É a tela principal do painel de controle.

**`GET /admin/usuarios/{idUsuario}/roles`** — JWT+Admin

Retorna apenas as roles de um usuário específico.

**`POST /admin/usuarios/{idUsuario}/roles/{idRole}`** — JWT+Admin

Atribui uma role a um usuário. Retorna `409` se o usuário já possui essa role.

**`DELETE /admin/usuarios/{idUsuario}/roles/{idRole}`** — JWT+Admin

Remove uma role de um usuário.

---

### Fluxo de uso típico

1. `POST /admin/permissions` — criar as permissões do sistema
2. `POST /admin/roles` — criar as roles
3. `PUT /admin/roles/{id}/permissions` — vincular permissões às roles
4. `POST /admin/usuarios/{id}/roles/{idRole}` — atribuir roles aos usuários
5. `GET /me/roles` — consultado pela aplicação para saber o que o usuário logado pode fazer
