ALTER TABLE "usuario" ADD COLUMN "publicId" VARCHAR(36);
UPDATE "usuario" SET "publicId" = gen_random_uuid()::text WHERE "publicId" IS NULL;
ALTER TABLE "usuario" ALTER COLUMN "publicId" SET NOT NULL;
ALTER TABLE "usuario" ADD CONSTRAINT "uq_usuario_public_id" UNIQUE ("publicId");
