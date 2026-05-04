CREATE TABLE "log_auditoria" (
    "id"          BIGSERIAL PRIMARY KEY,
    "idUsuario"   BIGINT,
    "acao"        VARCHAR(100) NOT NULL,
    "categoria"   VARCHAR(20)  NOT NULL,
    "ipOrigem"    VARCHAR(50),
    "metodoHttp"  VARCHAR(10),
    "uri"         VARCHAR(500),
    "statusHttp"  INTEGER,
    "sucesso"     BOOLEAN      NOT NULL,
    "detalhes"    TEXT,
    "criadoEm"    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX "idx_log_auditoria_idUsuario" ON "log_auditoria" ("idUsuario");
CREATE INDEX "idx_log_auditoria_criadoEm"  ON "log_auditoria" ("criadoEm");
CREATE INDEX "idx_log_auditoria_acao"       ON "log_auditoria" ("acao");
