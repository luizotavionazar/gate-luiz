-- Representa um login que passou pela senha (1º fator) e aguarda confirmação por
-- TOTP ou OTP (2º fator). Fica ativo por 10 minutos e encerra após 5 tentativas erradas.
CREATE TABLE "loginPendente" (
    "id"                BIGSERIAL   PRIMARY KEY,
    "tokenPendente"     VARCHAR(64) NOT NULL UNIQUE,
    "idUsuario"         INTEGER     NOT NULL REFERENCES "usuario"("id") ON DELETE CASCADE,
    "tipo"              VARCHAR(20) NOT NULL,
    "codigo"            VARCHAR(6),
    "ipOrigem"          VARCHAR(45) NOT NULL,
    "expiraEm"          TIMESTAMP   NOT NULL,
    "tentativasErradas" INTEGER     NOT NULL DEFAULT 0,
    "confirmadoEm"      TIMESTAMP,
    "encerradoEm"       TIMESTAMP,
    "criadoEm"          TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lp_token   ON "loginPendente"("tokenPendente");
CREATE INDEX idx_lp_usuario ON "loginPendente"("idUsuario");
CREATE INDEX idx_lp_expira  ON "loginPendente"("expiraEm");
