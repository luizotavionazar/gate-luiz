CREATE TABLE "configuracaoAplicacao" (
    "id"             BIGSERIAL PRIMARY KEY,
    "setupConcluido" BOOLEAN   NOT NULL DEFAULT FALSE,
    "idAdminMestre"  BIGINT    NULL,
    "dataCriacao"    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "dataAtualiza"   TIMESTAMP NULL
);

INSERT INTO "configuracaoAplicacao" ("id", "setupConcluido")
VALUES (1, FALSE)
ON CONFLICT ("id") DO NOTHING;

CREATE TABLE "role" (
    "id"          BIGSERIAL    PRIMARY KEY,
    "nome"        VARCHAR(100) NOT NULL UNIQUE,
    "descricao"   VARCHAR(255) NULL,
    "dataCriacao" TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "dataAtualiza" TIMESTAMP   NULL
);

CREATE TABLE "permissao" (
    "id"          BIGSERIAL    PRIMARY KEY,
    "recurso"     VARCHAR(100) NOT NULL,
    "acao"        VARCHAR(50)  NOT NULL,
    "descricao"   VARCHAR(255) NULL,
    "dataCriacao" TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "uk_permissao_recurso_acao" UNIQUE ("recurso", "acao")
);

CREATE TABLE "rolePermissao" (
    "idRole"      BIGINT NOT NULL REFERENCES "role"("id") ON DELETE CASCADE,
    "idPermissao" BIGINT NOT NULL REFERENCES "permissao"("id") ON DELETE CASCADE,
    PRIMARY KEY ("idRole", "idPermissao")
);

CREATE TABLE "usuarioRole" (
    "idUsuario"    BIGINT    NOT NULL,
    "idRole"       BIGINT    NOT NULL REFERENCES "role"("id") ON DELETE CASCADE,
    "atribuidoEm"  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "atribuidoPor" BIGINT    NOT NULL,
    PRIMARY KEY ("idUsuario", "idRole")
);

CREATE INDEX "idx_usuario_role_usuario" ON "usuarioRole" ("idUsuario");
