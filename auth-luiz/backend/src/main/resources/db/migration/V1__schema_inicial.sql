CREATE TABLE "usuario" (
  "id"               BIGSERIAL    PRIMARY KEY,
  "username"         VARCHAR(50)  NOT NULL,
  "nome"             VARCHAR(100) NOT NULL,
  "email"            VARCHAR(255) NOT NULL,
  "senhaHash"        VARCHAR(255) NULL,
  "dataCriacao"      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "dataAtualiza"     TIMESTAMP    NULL,
  "emailVerificado"  BOOLEAN      NOT NULL DEFAULT TRUE,
  "emailPendente"    VARCHAR(255) NULL,
  "providerOrigem"   VARCHAR(50)  NULL,
  CONSTRAINT "usuario_email_uk"    UNIQUE ("email"),
  CONSTRAINT "usuario_username_uk" UNIQUE ("username")
);

CREATE TABLE "tokenRecuperacaoSenha" (
  "id"             BIGSERIAL   PRIMARY KEY,
  "idUsuario"      BIGINT      NOT NULL,
  "tokenHash"      VARCHAR(64) NOT NULL,
  "expiraEm"       TIMESTAMP   NOT NULL,
  "usadoEm"        TIMESTAMP   NULL,
  "ipSolicitacao"  VARCHAR(45) NULL,
  "encerradoEm"    TIMESTAMP   NULL,
  "dataCriacao"    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "dataAtualiza"   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fk_token_recuperacao_usuario"
    FOREIGN KEY ("idUsuario") REFERENCES "usuario"("id") ON DELETE CASCADE
);

CREATE INDEX "idx_token_recuperacao_token_hash" ON "tokenRecuperacaoSenha" ("tokenHash");
CREATE INDEX "idx_token_recuperacao_usuario"     ON "tokenRecuperacaoSenha" ("idUsuario");

CREATE TABLE "controleRecuperacaoSenha" (
  "id"           BIGSERIAL   PRIMARY KEY,
  "ip"           VARCHAR(45) NOT NULL UNIQUE,
  "janelaInicio" TIMESTAMP   NOT NULL,
  "quantidade"   INTEGER     NOT NULL,
  "bloqueadoAte" TIMESTAMP   NULL,
  "dataCriacao"  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "dataAtualiza" TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "configuracaoAplicacao" (
  "id"                       BIGSERIAL    PRIMARY KEY,
  "setupConcluido"           BOOLEAN      NOT NULL DEFAULT FALSE,
  "smtpHost"                 VARCHAR(255) NULL,
  "smtpPort"                 INTEGER      NULL,
  "smtpUsername"             VARCHAR(255) NULL,
  "smtpPasswordCriptografada" TEXT        NULL,
  "smtpStarttls"             BOOLEAN      NOT NULL DEFAULT TRUE,
  "mailFrom"                 VARCHAR(255) NULL,
  "frontendBaseUrl"          VARCHAR(500) NULL,
  "dataCriacao"              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "dataAtualiza"             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO "configuracaoAplicacao" ("id", "setupConcluido")
VALUES (1, FALSE)
ON CONFLICT ("id") DO NOTHING;

CREATE TABLE "identidadeExterna" (
  "id"                      BIGSERIAL    PRIMARY KEY,
  "idUsuario"               BIGINT       NOT NULL,
  "provider"                VARCHAR(30)  NOT NULL,
  "providerUserId"          VARCHAR(255) NOT NULL,
  "emailProvider"           VARCHAR(255) NULL,
  "emailVerificadoProvider" BOOLEAN      NOT NULL DEFAULT FALSE,
  "dataCriacao"             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "dataAtualiza"            TIMESTAMP    NULL,
  CONSTRAINT "fk_identidade_externa_usuario"
    FOREIGN KEY ("idUsuario") REFERENCES "usuario"("id") ON DELETE CASCADE,
  CONSTRAINT "uk_identidade_externa_provider_usuario"
    UNIQUE ("provider", "providerUserId"),
  CONSTRAINT "uk_identidade_externa_usuario_provider"
    UNIQUE ("idUsuario", "provider")
);

CREATE INDEX "idx_identidade_externa_usuario" ON "identidadeExterna" ("idUsuario");

CREATE TABLE "tokenConfirmacao" (
  "id"            BIGSERIAL    PRIMARY KEY,
  "idUsuario"     BIGINT       NOT NULL,
  "tipo"          VARCHAR(30)  NOT NULL,
  "tokenHash"     VARCHAR(64)  NOT NULL,
  "emailDestino"  VARCHAR(255) NULL,
  "expiraEm"      TIMESTAMP    NOT NULL,
  "confirmadoEm"  TIMESTAMP    NULL,
  "encerradoEm"   TIMESTAMP    NULL,
  "ipSolicitacao" VARCHAR(45)  NULL,
  "dataCriacao"   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "fk_token_confirmacao_usuario"
    FOREIGN KEY ("idUsuario") REFERENCES "usuario"("id") ON DELETE CASCADE
);

CREATE INDEX "idx_token_confirmacao_hash"    ON "tokenConfirmacao" ("tokenHash");
CREATE INDEX "idx_token_confirmacao_usuario" ON "tokenConfirmacao" ("idUsuario");

CREATE TABLE "controleAlteracaoEmail" (
  "id"           BIGSERIAL PRIMARY KEY,
  "idUsuario"    INTEGER   NOT NULL UNIQUE,
  "janelaInicio" TIMESTAMP NOT NULL,
  "quantidade"   INTEGER   NOT NULL DEFAULT 0,
  "bloqueadoAte" TIMESTAMP NULL,
  "dataCriacao"  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "dataAtualiza" TIMESTAMP NULL,
  CONSTRAINT "fk_controle_alteracao_email_usuario"
    FOREIGN KEY ("idUsuario") REFERENCES "usuario"("id") ON DELETE CASCADE
);
