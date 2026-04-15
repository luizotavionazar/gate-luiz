# AuthLuiz

API de autenticação pronta para reutilização, construída com Spring Boot e acompanhada de um frontend de referência em Vue 3. O objetivo do projeto é fornecer uma base sólida de autenticação que possa ser reaproveitada em qualquer aplicação, sem precisar reimplementar cadastro, login, recuperação de senha e controle de acesso do zero.

## O que está incluído

### Funcionalidades

- Cadastro de conta com e-mail e senha
- Login com JWT (HMAC-SHA256, stateless)
- Login e vinculação de conta com Google (OAuth via Google Identity Services)
- Confirmação de e-mail no cadastro (feature flag) e na alteração de e-mail (sempre ativa)
- Recuperação e redefinição de senha por e-mail
- Gerenciamento de conta autenticada: alterar nome, e-mail, senha local
- Definição de senha local para contas criadas via Google
- Exclusão de conta
- Setup inicial guiado para configuração de envio de e-mail (SMTP)

### Arquitetura

```
auth-luiz/
├── backend/    Spring Boot 3 + Java 21 — API REST de autenticação
├── frontend/   Vue 3 + Vite            — implementação de referência
├── compose-dev.yaml                    — Postgres + pgAdmin para desenvolvimento
└── docker-compose.yml                  — stack completa para produção
```

O **backend** é o produto principal: uma API independente de frontend que qualquer cliente pode consumir. O **frontend** é uma implementação de referência que demonstra todos os fluxos da API — não é obrigatório usá-lo.

## Stack

| Camada    | Tecnologia                                      |
|-----------|-------------------------------------------------|
| Backend   | Java 21, Spring Boot 3, Spring Security, Flyway |
| Banco     | PostgreSQL                                      |
| Tokens    | JWT HS256 (nimbus), Argon2 (senhas)             |
| OAuth     | Google Identity Services                        |
| E-mail    | JavaMail, credenciais criptografadas (BouncyCastle) |
| Frontend  | Vue 3, Vite, Vue Router, Axios, Bootstrap 5     |

## Início rápido

### Pré-requisitos

- Java 21, Maven
- Node.js + npm
- Docker

### 1. Configurar o backend

```bash
cp backend/.env.example backend/.env
# Edite backend/.env com as credenciais do banco, segredo JWT e client ID do Google
```

### 2. Subir banco de dados

```bash
docker compose -f compose-dev.yaml up -d
```

### 3. Iniciar backend

```bash
cd backend
./mvnw spring-boot:run
# A API sobe em http://localhost:8080
```

### 4. Configurar e iniciar frontend

```bash
cp frontend/.env.example frontend/.env
# Edite frontend/.env com a URL da API e o client ID do Google

cd frontend
npm install
npm run dev
# O frontend sobe em http://localhost:5173
```

### 5. Setup inicial

Acesse `http://localhost:5173/setup` e preencha as configurações de e-mail (SMTP). Isso desbloqueia todos os endpoints da API.

### Stack completa (produção)

```bash
docker compose up --build
```

## Variáveis de ambiente

### Backend (`backend/.env`)

| Variável                    | Descrição                              |
|-----------------------------|----------------------------------------|
| `APP_SETUP_MASTER_KEY`      | Chave para concluir o setup inicial    |
| `SPRING_DATASOURCE_URL`     | URL JDBC do PostgreSQL                 |
| `SPRING_DATASOURCE_USERNAME`| Usuário do banco                       |
| `SPRING_DATASOURCE_PASSWORD`| Senha do banco                         |
| `JWT_SECRET`                | Chave de assinatura dos JWTs           |
| `JWT_EXPIRATION_MINUTES`    | Expiração do token (padrão: 120)       |
| `GOOGLE_OAUTH_CLIENT_ID`    | Client ID do Google OAuth              |

### Frontend (`frontend/.env`)

| Variável                | Descrição                              |
|-------------------------|----------------------------------------|
| `VITE_API_BASE_URL`     | URL da API (padrão: `http://localhost:8080`) |
| `VITE_GOOGLE_CLIENT_ID` | Client ID do Google OAuth              |
