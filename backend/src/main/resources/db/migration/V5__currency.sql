-- A building keeps its books in one currency, so an owner can hold buildings in
-- different countries. Everything before this was in euros.
alter table building add column currency varchar(3) not null default 'EUR';

-- An invoice keeps the currency it was issued in, so changing its building's
-- currency later never rewrites a document a tenant already has.
alter table invoice add column currency varchar(3) not null default 'EUR';
