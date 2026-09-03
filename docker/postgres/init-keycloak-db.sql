-- Keycloak stores its own state in a dedicated database on the same Postgres instance.
-- Runs once, on first initialisation of the postgres-data volume.
CREATE DATABASE keycloak;
