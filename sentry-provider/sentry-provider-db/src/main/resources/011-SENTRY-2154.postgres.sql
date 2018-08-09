-- Table SENTRY_USER_DB_PRIVILEGE_MAP for join relationship
CREATE TABLE "SENTRY_USER_DB_PRIVILEGE_MAP" (
  "USER_ID" BIGINT NOT NULL,
  "DB_PRIVILEGE_ID" BIGINT NOT NULL,
  "GRANTOR_PRINCIPAL" character varying(128)
);

ALTER TABLE "SENTRY_USER_DB_PRIVILEGE_MAP"
  ADD CONSTRAINT "SENTRY_USR_DB_PRV_MAP_PK" PRIMARY KEY ("USER_ID","DB_PRIVILEGE_ID");

ALTER TABLE ONLY "SENTRY_USER_DB_PRIVILEGE_MAP"
  ADD CONSTRAINT "SN_USR_DB_PRV_MAP_SN_USER_FK"
  FOREIGN KEY ("USER_ID") REFERENCES "SENTRY_USER"("USER_ID") DEFERRABLE;

ALTER TABLE ONLY "SENTRY_USER_DB_PRIVILEGE_MAP"
  ADD CONSTRAINT "SEN_USR_DB_PRV_MAP_DB_PRV_FK"
  FOREIGN KEY ("DB_PRIVILEGE_ID") REFERENCES "SENTRY_DB_PRIVILEGE"("DB_PRIVILEGE_ID") DEFERRABLE;

CREATE INDEX "SEN_USR_DB_PRV_MAP_USR_FK_IDX" ON "SENTRY_USER_DB_PRIVILEGE_MAP" ("USER_ID");

CREATE INDEX "SEN_USR_DB_PRV_MAP_PRV_FK_IDX" ON "SENTRY_USER_DB_PRIVILEGE_MAP" ("DB_PRIVILEGE_ID");