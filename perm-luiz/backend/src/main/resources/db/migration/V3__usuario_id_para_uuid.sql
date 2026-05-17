-- Reset idAdminMestre: o valor numérico anterior é inválido após a mudança do subject JWT para UUID
UPDATE "configuracaoAplicacao" SET "idAdminMestre" = NULL;
ALTER TABLE "configuracaoAplicacao" ALTER COLUMN "idAdminMestre" TYPE VARCHAR(36);

-- usuarioRole: limpar dados (atribuições anteriores usavam IDs numéricos, agora inválidos)
DELETE FROM "usuarioRole";
ALTER TABLE "usuarioRole" DROP CONSTRAINT "usuarioRole_pkey";
ALTER TABLE "usuarioRole" ALTER COLUMN "idUsuario" TYPE VARCHAR(36);
ALTER TABLE "usuarioRole" ALTER COLUMN "atribuidoPor" TYPE VARCHAR(36);
ALTER TABLE "usuarioRole" ADD PRIMARY KEY ("idUsuario", "idRole");

-- log_auditoria: converter idUsuario para string (registros históricos mantidos como texto)
ALTER TABLE "log_auditoria" ALTER COLUMN "idUsuario" TYPE VARCHAR(36) USING "idUsuario"::text;
