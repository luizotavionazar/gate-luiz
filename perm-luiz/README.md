# PermLuiz

API de controle de acesso (roles e permissões) construída com Spring Boot e acompanhada de um frontend de referência em Vue 3. Complementa o AuthLuiz — valida JWTs emitidos por ele via JWKS e gerencia quais recursos cada usuário pode acessar.

## O que está incluído

### Funcionalidades

- Promoção automática do primeiro usuário autenticado a admin mestre
- CRUD de roles (agrupamentos de permissões)
- CRUD de permissões (recurso + ação, ex: `artigos:criar`)
- Atribuição e remoção de roles por usuário
- Consulta das próprias roles e permissões pelo usuário autenticado

### Arquitetura

```
perm-luiz/
├── backend/    Spring Boot + Java 21 — API REST de autorização
├── frontend/   Vue 3 + Vite          — implementação de referência
└── compose.yaml                      — stack completa para produção
```

O **backend** valida JWTs do AuthLuiz automaticamente via endpoint JWKS (sem segredo compartilhado). O **frontend** é uma implementação de referência.

## Stack

| Camada    | Tecnologia                                      |
|-----------|-------------------------------------------------|
| Backend   | Java 21, Spring Boot, Spring Security, Flyway   |
| Banco     | PostgreSQL                                      |
| Tokens    | JWT RS256 validado via JWKS do AuthLuiz         |
| Frontend  | Vue 3, Vite, Vue Router, Axios, Bootstrap 5     |

## Pré-requisitos

- Java 21, Maven
- Node.js + npm
- Docker
- **AuthLuiz em execução** — o PermLuiz valida JWTs via JWKS do AuthLuiz

## Início rápido

### 1. Configurar o backend

```bash
cp backend/.env.example backend/.env
# Edite backend/.env com as credenciais do banco e URI do JWKS
```

### 2. Subir banco de dados de desenvolvimento

```bash
docker compose -f compose-dev.yaml up -d
```

### 3. Iniciar backend

```bash
cd backend
./mvnw spring-boot:run
# A API sobe em http://localhost:8081
```

### 4. Configurar e iniciar frontend

```bash
cd frontend
npm install
npm run dev
# O frontend sobe em http://localhost:5174
```

### 5. Primeiro acesso

Cadastre-se no AuthLuiz. Na tela de conta, clique em **Painel de Permissões** — o JWT é passado automaticamente ao PermLuiz via URL fragment. O primeiro usuário autenticado a acessar qualquer página `/admin/**` é promovido automaticamente a admin mestre.

### Stack completa (produção)

Use o repositório **luiz-stack** (`github.com/luizotavionazar/luiz-stack`) para subir AuthLuiz + PermLuiz juntos com um único `docker compose up`.

## Variáveis de ambiente

### Backend (`backend/.env`)

| Variável                    | Descrição                                          |
|-----------------------------|----------------------------------------------------|
| `SPRING_DATASOURCE_URL`     | URL JDBC do PostgreSQL                             |
| `SPRING_DATASOURCE_USERNAME`| Usuário do banco                                   |
| `SPRING_DATASOURCE_PASSWORD`| Senha do banco                                     |
| `AUTH_LUIZ_JWKS_URI`        | URI do JWKS do AuthLuiz (ex: `http://localhost:8080/auth/.well-known/jwks.json`) |

### Frontend (`frontend/.env`)

| Variável                | Descrição                                          |
|-------------------------|----------------------------------------------------|
| `VITE_PERM_API_URL`     | URL da API (padrão: `http://localhost:8081`)       |
| `VITE_AUTH_LUIZ_URL`    | URL do AuthLuiz (padrão: `http://localhost:8080`)  |

## Ecossistema

| Serviço | Repositório | Descrição |
|---------|-------------|-----------|
| **AuthLuiz** | `github.com/luizotavionazar/auth-luiz` | Identidade e autenticação — emite JWTs RS256 |
| **PermLuiz** | `github.com/luizotavionazar/perm-luiz` | Roles, permissões e controle de acesso (este repo) |
| **LuizStack** | `github.com/luizotavionazar/luiz-stack` | Orquestração Docker dos dois serviços |
