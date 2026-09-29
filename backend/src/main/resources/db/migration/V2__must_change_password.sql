-- An assistant is created with a password their owner hands over, so the
-- application makes them replace it before they can work. The rule lives here
-- rather than in Keycloak's required actions: those are resolved on Keycloak's
-- own pages, and the browser only ever talks to this application.
alter table app_user
    add column must_change_password boolean not null default false;
