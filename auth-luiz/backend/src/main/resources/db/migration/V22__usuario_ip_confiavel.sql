CREATE TABLE "usuarioIpConfiavel" (
    "id"        BIGSERIAL    PRIMARY KEY,
    "idUsuario" INTEGER      NOT NULL REFERENCES "usuario"("id") ON DELETE CASCADE,
    "ip"        VARCHAR(45)  NOT NULL,
    "rotulo"    VARCHAR(100),
    "criadoEm" TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ip_confiavel_usuario
    ON "usuarioIpConfiavel"("idUsuario");

CREATE UNIQUE INDEX idx_ip_confiavel_usuario_ip
    ON "usuarioIpConfiavel"("idUsuario", "ip");
