ALTER TABLE "configuracaoAplicacao"
    ADD COLUMN "twilioAccountSidCriptografado" TEXT         NULL,
    ADD COLUMN "twilioAuthTokenCriptografado"  TEXT         NULL,
    ADD COLUMN "twilioFromNumber"              VARCHAR(20)  NULL,
    ADD COLUMN "twilioCanal"                   VARCHAR(10)  NULL;
