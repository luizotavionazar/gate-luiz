-- Tokens existentes têm hashes SHA-256, inválidos após a mudança — limpeza necessária
DELETE FROM "tokenConfirmacao";
DELETE FROM "tokenRecuperacaoSenha";

ALTER TABLE "tokenConfirmacao"      RENAME COLUMN "codigoHash" TO "codigo";
ALTER TABLE "tokenRecuperacaoSenha" RENAME COLUMN "codigoHash" TO "codigo";

ALTER TABLE "tokenConfirmacao"      ALTER COLUMN "codigo" TYPE VARCHAR(6);
ALTER TABLE "tokenRecuperacaoSenha" ALTER COLUMN "codigo" TYPE VARCHAR(6);

-- Índices nas colunas de código são dead code: nenhuma query busca por eles
DROP INDEX IF EXISTS "idx_token_confirmacao_hash";
DROP INDEX IF EXISTS "idx_token_recuperacao_token_hash";
