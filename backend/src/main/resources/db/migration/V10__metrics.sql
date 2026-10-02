-- What the administrator's metrics are counted from (BM-31).

-- Who used the service on which day, and how many times they signed in. One
-- row per user and day, holding nothing else about them; rows older than 13
-- months are deleted every night.
create table user_activity (
    user_id  uuid         not null references app_user (id) on delete cascade,
    day      date         not null,
    role     varchar(255) not null,
    sign_ins integer      not null default 0,
    primary key (user_id, day)
);

create index idx_user_activity_day on user_activity (day);

-- Requests per day, route and status, with their timings. It names no user
-- and no address, so it is kept.
create table api_stat_daily (
    day      date         not null,
    method   varchar(10)  not null,
    route    varchar(255) not null,
    status   smallint     not null,
    requests bigint       not null,
    total_ms bigint       not null,
    max_ms   bigint       not null,
    primary key (day, method, route, status)
);
