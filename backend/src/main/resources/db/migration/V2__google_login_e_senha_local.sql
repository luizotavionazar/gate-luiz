ALTER TABLE "usuario"
  ALTER COLUMN "senhaHash" DROP NOT NULL;

CREATE TABLE "identidadeExterna" (
  "id" BIGSERIAL PRIMARY KEY,
  "idUsuario" BIGINT NOT NULL,
  "provider" VARCHAR(30) NOT NULL,
  "providerUserId" VARCHAR(255) NOT NULL,
  "emailProvider" VARCHAR(255),
  "emailVerificadoProvider" BOOLEAN NOT NULL DEFAULT FALSE,
  "dataCriacao" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "dataAtualiza" TIMESTAMP NULL,
  CONSTRAINT "fk_identidade_externa_usuario"
    FOREIGN KEY ("idUsuario") REFERENCES "usuario"("id"),
  CONSTRAINT "uk_identidade_externa_provider_usuario"
    UNIQUE ("provider", "providerUserId"),
  CONSTRAINT "uk_identidade_externa_usuario_provider"
    UNIQUE ("idUsuario", "provider")
);

CREATE INDEX "idx_identidade_externa_usuario"
  ON "identidadeExterna" ("idUsuario");
