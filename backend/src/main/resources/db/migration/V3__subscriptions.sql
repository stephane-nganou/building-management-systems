-- An owner's use of the application is bound to time: a history of dated
-- periods, both ends inclusive. Outside every period their data is read only.
create table subscription_period (
    id         uuid         primary key,
    owner_id   uuid         not null references app_user (id) on delete cascade,
    starts_on  date         not null,
    ends_on    date         not null,
    note       varchar(500),
    created_at timestamptz  not null,
    updated_at timestamptz  not null,
    constraint ck_subscription_period_range check (ends_on >= starts_on)
);
create index idx_subscription_period_owner on subscription_period (owner_id, ends_on);

-- An administrator can suspend an account outright, whatever its subscription.
alter table app_user
    add column suspended boolean not null default false;

-- Everyone who owns data today keeps working for a trial month. Roles live in
-- Keycloak, so an owner is recognised here as anyone who is nobody's assistant.
insert into subscription_period (id, owner_id, starts_on, ends_on, note, created_at, updated_at)
select gen_random_uuid(), u.id, current_date, current_date + 29, 'Trial', now(), now()
from app_user u
where not exists (select 1 from assistant_assignment a where a.assistant_id = u.id);
