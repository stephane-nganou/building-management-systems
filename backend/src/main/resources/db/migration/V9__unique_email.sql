-- Accounts are found by email, ignoring case, at registration, at sign in and
-- when an assistant is granted access. Two rows sharing an address would make
-- each of those lookups fail, so the database refuses the second one. Keycloak
-- already refuses duplicate emails; this guards the local copy of them.

create unique index app_user_email_lower_key on app_user (lower(email));
