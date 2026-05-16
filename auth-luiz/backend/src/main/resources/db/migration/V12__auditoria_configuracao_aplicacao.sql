ALTER TABLE "configuracaoAplicacao"
    ADD COLUMN "auditoriaAtividade"   BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN "auditoriaRetencaoDias" INTEGER NOT NULL DEFAULT 90;
