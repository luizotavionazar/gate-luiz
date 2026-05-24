CREATE TABLE "codigoBackup2fa" (
    "id"         BIGSERIAL    PRIMARY KEY,
    "idUsuario"  INTEGER      NOT NULL REFERENCES "usuario"("id") ON DELETE CASCADE,
    "codigoHash" VARCHAR(255) NOT NULL,
    "usadoEm"    TIMESTAMP,
    "criadoEm"   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_backup_codes_usuario
    ON "codigoBackup2fa"("idUsuario");
