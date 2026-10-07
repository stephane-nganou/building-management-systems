-- What the administrator tells every signed in user, and when (BM-30).
-- English is required; a missing French or German text falls back to it.
create table announcement (
    id         uuid          primary key,
    created_at timestamptz   not null,
    updated_at timestamptz   not null,
    kind       varchar(16)   not null,
    message_en varchar(1000) not null,
    message_fr varchar(1000),
    message_de varchar(1000),
    starts_at  timestamptz   not null,
    ends_at    timestamptz   not null,
    constraint ck_announcement_range check (ends_at > starts_at)
);

create index idx_announcement_ends_at on announcement (ends_at);
