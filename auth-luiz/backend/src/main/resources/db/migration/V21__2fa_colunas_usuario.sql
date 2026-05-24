ALTER TABLE "usuario"
    ADD COLUMN "ultimoIp"           VARCHAR(45),
    ADD COLUMN "totpSecretPendente" VARCHAR(500),
    ADD COLUMN "totpSecret"         VARCHAR(500),
    ADD COLUMN "totpAtivo"          BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN "preferencia2fa"     VARCHAR(20);
