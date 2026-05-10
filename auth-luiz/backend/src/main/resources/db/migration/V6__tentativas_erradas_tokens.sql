ALTER TABLE "tokenConfirmacao" ADD COLUMN "tentativasErradas" SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE "tokenRecuperacaoSenha" ADD COLUMN "tentativasErradas" SMALLINT NOT NULL DEFAULT 0;
