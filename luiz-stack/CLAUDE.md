# CLAUDE.md

Este arquivo fornece orientações ao Claude Code (claude.ai/code) ao trabalhar com o código deste repositório.

## Visão Geral do Projeto

LuizStack é o repositório de orquestração Docker da stack completa formada por AuthLuiz e PermissoesLuiz. Contém um único `compose.yaml` que sobe os dois serviços (e seus bancos de dados) em uma rede compartilhada.

Este repositório **não contém código de aplicação** — apenas configuração de infraestrutura. O código de cada serviço vive nos seus próprios repositórios.

## Ecossistema de Serviços

| Serviço | Repositório | Descrição |
|---------|-------------|-----------|
| **AuthLuiz** | `github.com/luizotavionazar/auth-luiz` | Identidade e autenticação — emite JWTs RS256, expõe JWKS |
| **PermissoesLuiz** | `github.com/luizotavionazar/permissoes-luiz` | Roles, permissões e controle de acesso |
| **LuizStack** | `github.com/luizotavionazar/luiz-stack` | Orquestração Docker dos dois serviços (este repo) |

## Estrutura do Repositório

```
luiz-stack/
├── compose.yaml     ← sobe todos os 6 serviços (2 backends + 2 frontends + 2 bancos)
├── .env.example     ← template com todas as variáveis necessárias
├── .env             ← variáveis reais (não commitado)
├── .gitignore
└── CLAUDE.md
```

## Pré-requisitos

- Docker com Docker Compose
- Os três diretórios devem ser irmãos dentro de `gate-luiz/`:
  ```
  c:\gate-luiz\auth-luiz\
  c:\gate-luiz\permissoes-luiz\
  c:\gate-luiz\luiz-stack\
  ```

## Configuração

```powershell
Copy-Item luiz-stack\.env.example luiz-stack\.env
# Edite luiz-stack\.env com todas as credenciais
```

### Variáveis de Ambiente

Consulte `.env.example`. Variáveis obrigatórias:

**Auth-Luiz:**
- `AUTHLUIZ_DB_USER` / `AUTHLUIZ_DB_PASSWORD` — credenciais do banco do AuthLuiz
- `AUTHLUIZ_SETUP_MASTER_KEY` — chave mestra do setup do AuthLuiz
- `GOOGLE_OAUTH_CLIENT_ID` — client ID do Google OAuth
- `JWT_RSA_PRIVATE_KEY` — chave privada RSA em base64 (**formato PEM completo em base64**, não DER)
- `JWT_RSA_PUBLIC_KEY` — chave pública RSA em base64 (**formato PEM completo em base64**, não DER)
- `JWT_EXPIRATION_MINUTES` — expiração dos JWTs (padrão: 120)

**PermissoesLuiz:**
- `PERMISSOES_DB_USER` / `PERMISSOES_DB_PASSWORD` — credenciais do banco do PermissoesLuiz
- `PERMISSOES_SETUP_MASTER_KEY` — chave mestra do setup do PermissoesLuiz

> `AUTH_LUIZ_JWKS_URI` é injetada diretamente pelo `compose.yaml` com o hostname interno do Docker — não precisa estar no `.env`.

### Geração das Chaves RSA

Use o utilitário `GerarChavesRSA.java` na raiz do repositório `gate-luiz`:

```powershell
cd c:\gate-luiz
java GerarChavesRSA.java
```

Cole os valores gerados em `luiz-stack/.env` **e também** em `auth-luiz/backend/.env` (para execução standalone).

> **Importante:** o formato correto é o arquivo PEM completo (com `-----BEGIN PRIVATE KEY-----`) codificado em base64. O utilitário já gera nesse formato. Não use base64 de DER puro.

## Uso

```bash
# Subir a stack completa (com build das imagens)
docker compose up --build

# Subir em background
docker compose up --build -d

# Ver logs de um serviço específico
docker compose logs -f authluiz-backend

# Parar tudo
docker compose down

# Parar e remover volumes (apaga dados dos bancos)
docker compose down -v
```

## Serviços e Portas

| Serviço | Porta pública | Descrição |
|---------|---------------|-----------|
| `authluiz-backend` | `8080` | API de autenticação |
| `authluiz-frontend` | `80` | Frontend do AuthLuiz |
| `permissoes-backend` | `8081` | API de autorização |
| `permissoes-frontend` | `81` | Frontend do PermissoesLuiz |
| `authluiz-db` | interno | PostgreSQL do AuthLuiz |
| `permissoes-db` | interno | PostgreSQL do PermissoesLuiz |

## Dependências de Inicialização

O `compose.yaml` garante a ordem correta via `depends_on` com `condition: service_healthy`:
1. `authluiz-db` → healthcheck pg_isready
2. `authluiz-backend` → depende de `authluiz-db`
3. `permissoes-db` → healthcheck pg_isready
4. `permissoes-backend` → depende de `permissoes-db` e `authluiz-backend`
5. Frontends → dependem dos respectivos backends

## Fluxo de Trabalho com Claude

- As tarefas são trabalhadas uma por vez, do início ao fim.
- Commits só são realizados quando o usuário solicitar explicitamente.
- Ao ser solicitado um commit, sempre sugerir descrições para o mesmo antes de executar.
- O usuário usa os prefixos `feat` ou `fix` nas mensagens de commit para controle.
