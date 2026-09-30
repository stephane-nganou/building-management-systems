-- Ending a subscription on the day its period began leaves that period with no
-- day in it: it ends the day before it starts. The period is kept rather than
-- deleted, because having had a period at all is what makes someone an owner
-- the administrator can see and renew.
alter table subscription_period drop constraint ck_subscription_period_range;
alter table subscription_period
    add constraint ck_subscription_period_range check (ends_on >= starts_on - 1);
