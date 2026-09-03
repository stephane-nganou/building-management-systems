-- Initial schema for the building management system.

create table app_user (
    id          uuid         primary key,
    keycloak_id varchar(255) not null unique,
    email       varchar(255) not null,
    first_name  varchar(255),
    last_name   varchar(255),
    created_at  timestamptz  not null,
    updated_at  timestamptz  not null
);

create table assistant_assignment (
    id           uuid        primary key,
    owner_id     uuid        not null references app_user (id) on delete cascade,
    assistant_id uuid        not null references app_user (id) on delete cascade,
    created_at   timestamptz not null,
    updated_at   timestamptz not null,
    constraint uq_assignment_owner_assistant unique (owner_id, assistant_id),
    constraint ck_assignment_not_self check (owner_id <> assistant_id)
);

create table assistant_permission (
    assignment_id uuid         not null references assistant_assignment (id) on delete cascade,
    permission    varchar(255) not null,
    primary key (assignment_id, permission)
);

create table building (
    id          uuid         primary key,
    owner_id    uuid         not null references app_user (id) on delete cascade,
    name        varchar(255) not null,
    street      varchar(255),
    city        varchar(255),
    postal_code varchar(255),
    country     varchar(255),
    notes       varchar(1000),
    created_at  timestamptz  not null,
    updated_at  timestamptz  not null
);
create index idx_building_owner on building (owner_id);

create table apartment (
    id                uuid           primary key,
    building_id       uuid           not null references building (id) on delete cascade,
    label             varchar(255)   not null,
    floor             integer,
    size_sqm          numeric(8, 2),
    rooms             integer        not null,
    bedrooms          integer        not null,
    bathrooms         integer        not null,
    kitchens          integer        not null,
    toilets           integer        not null,
    base_rent         numeric(12, 2) not null,
    utilities_advance numeric(12, 2) not null,
    status            varchar(255)   not null,
    created_at        timestamptz    not null,
    updated_at        timestamptz    not null,
    constraint uq_apartment_building_label unique (building_id, label)
);
create index idx_apartment_building on apartment (building_id);

create table tenant (
    id           uuid           primary key,
    apartment_id uuid           not null references apartment (id) on delete cascade,
    first_name   varchar(255)   not null,
    last_name    varchar(255)   not null,
    email        varchar(255),
    phone        varchar(255),
    lease_start  date           not null,
    lease_end    date,
    deposit      numeric(12, 2),
    active       boolean        not null,
    created_at   timestamptz    not null,
    updated_at   timestamptz    not null,
    constraint ck_tenant_lease_range check (lease_end is null or lease_end >= lease_start)
);
create index idx_tenant_apartment on tenant (apartment_id);

create table expense (
    id           uuid           primary key,
    building_id  uuid           not null references building (id) on delete cascade,
    apartment_id uuid           references apartment (id) on delete set null,
    category     varchar(255)   not null,
    amount       numeric(12, 2) not null,
    incurred_on  date           not null,
    description  varchar(1000)  not null,
    vendor       varchar(255),
    created_at   timestamptz    not null,
    updated_at   timestamptz    not null
);
create index idx_expense_building_date on expense (building_id, incurred_on);
create index idx_expense_apartment on expense (apartment_id);

-- Invoice numbers are drawn from a sequence so they stay unique and gap-free
-- under concurrent creation, rather than being derived from a count.
create sequence invoice_number_seq start with 1 increment by 1;

create table invoice (
    id             uuid          primary key,
    apartment_id   uuid          not null references apartment (id) on delete cascade,
    tenant_id      uuid          not null references tenant (id) on delete cascade,
    invoice_number varchar(255)  not null unique,
    type           varchar(255)  not null,
    status         varchar(255)  not null,
    period_start   date          not null,
    period_end     date          not null,
    issue_date     date          not null,
    due_date       date          not null,
    notes          varchar(1000),
    created_at     timestamptz   not null,
    updated_at     timestamptz   not null,
    constraint ck_invoice_period check (period_end >= period_start),
    constraint ck_invoice_due check (due_date >= issue_date)
);
create index idx_invoice_apartment_period on invoice (apartment_id, period_start);
create index idx_invoice_tenant on invoice (tenant_id);

create table invoice_line (
    id          uuid           primary key,
    invoice_id  uuid           not null references invoice (id) on delete cascade,
    description varchar(500)   not null,
    quantity    numeric(12, 3) not null,
    unit_price  numeric(12, 2) not null,
    unit        varchar(255),
    created_at  timestamptz    not null,
    updated_at  timestamptz    not null
);
create index idx_invoice_line_invoice on invoice_line (invoice_id);
