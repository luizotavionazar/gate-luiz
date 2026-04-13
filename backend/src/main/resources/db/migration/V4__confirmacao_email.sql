-- Campos de verificação de e-mail no usuário.
-- emailVerificado DEFAULT TRUE garante que contas existentes não sejam bloqueadas.
ALTER TABLE "usuario"
    ADD COLUMN "emailVerificado" BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN "emailPendente"   VARCHAR(255) NULL;

-- Parâmetro de configuração para habilitar/desabilitar confirmação de e-mail.
-- DEFAULT FALSE preserva o comportamento atual para instâncias já configuradas.
ALTER TABLE "configuracaoAplicacao"
    ADD COLUMN "confirmacaoEmailHabilitada" BOOLEAN NOT NULL DEFAULT FALSE;

-- Token genérico de confirmação: usado para verificação de cadastro e alteração de e-mail.
CREATE TABLE "tokenConfirmacao" (
    "id"             BIGSERIAL PRIMARY KEY,
    "idUsuario"      BIGINT        NOT NULL,
    "tipo"           VARCHAR(30)   NOT NULL,
    "tokenHash"      VARCHAR(64)   NOT NULL,
    "emailDestino"   VARCHAR(255)  NULL,
    "expiraEm"       TIMESTAMP     NOT NULL,
    "confirmadoEm"   TIMESTAMP     NULL,
    "encerradoEm"    TIMESTAMP     NULL,
    "ipSolicitacao"  VARCHAR(45)   NULL,
    "dataCriacao"    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "fk_token_confirmacao_usuario"
        FOREIGN KEY ("idUsuario") REFERENCES "usuario"("id") ON DELETE CASCADE
);

CREATE INDEX "idx_token_confirmacao_hash"    ON "tokenConfirmacao" ("tokenHash");
CREATE INDEX "idx_token_confirmacao_usuario" ON "tokenConfirmacao" ("idUsuario");

-- Controle de rate limiting por usuário para solicitações de alteração de e-mail.
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
