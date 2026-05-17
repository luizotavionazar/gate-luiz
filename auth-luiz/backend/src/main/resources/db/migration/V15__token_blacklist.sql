CREATE TABLE "tokenBlacklist" (
    "id"          BIGSERIAL    NOT NULL,
    "jti"         VARCHAR(36)  NOT NULL,
    "expiraEm"    TIMESTAMP    NOT NULL,
    "dataCriacao" TIMESTAMP    NOT NULL,
    CONSTRAINT "pk_tokenBlacklist" PRIMARY KEY ("id"),
    CONSTRAINT "uq_tokenBlacklist_jti" UNIQUE ("jti")
);

CREATE INDEX "idx_tokenBlacklist_jti"      ON "tokenBlacklist" ("jti");
CREATE INDEX "idx_tokenBlacklist_expiraEm" ON "tokenBlacklist" ("expiraEm");
