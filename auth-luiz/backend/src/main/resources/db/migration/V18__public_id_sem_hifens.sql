ALTER TABLE "usuario" ALTER COLUMN "publicId" TYPE VARCHAR(32) USING REPLACE("publicId"::text, '-', '');
