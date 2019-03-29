SELECT 'Upgrading Sentry store schema from 1.5.0-cdh5 to 1.5.0-cdh5-2' AS Status from dual;
@010-SENTRY-2210.oracle.sql;
@011-SENTRY-2154.oracle.sql;

UPDATE SENTRY_VERSION SET SCHEMA_VERSION='1.5.0-cdh5-2', VERSION_COMMENT='Sentry release version 1.5.0-cdh5-2' WHERE VER_ID=1;
SELECT 'Finished upgrading Sentry store schema from 1.5.0-cdh5 to 1.5.0-cdh5-2' AS Status from dual;