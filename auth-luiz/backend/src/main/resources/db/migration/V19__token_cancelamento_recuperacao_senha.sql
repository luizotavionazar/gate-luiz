ALTER TABLE "tokenRecuperacaoSenha"
    ADD COLUMN IF NOT EXISTS "tokenCancelamento" VARCHAR(36);
