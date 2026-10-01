-- Two corrections to V6.

-- V6 called every account linked as an assistant one. An owner linked as
-- somebody's assistant before BM-19 was exactly the takeover it closes, and
-- would have been marked an assistant too. Owners hold a subscription period
-- (every owner starts on a trial, and an assistant's is forgotten when they sign
-- in), so that is what tells them apart.
update app_user set role = 'OWNER'
where role = 'ASSISTANT'
  and exists (select 1 from subscription_period p where p.owner_id = app_user.id);

-- Deleting the owner who created an assistant must not be refused: the assistant
-- may still work for others, and simply has nobody left who can reset its
-- password.
alter table app_user drop constraint app_user_created_by_owner_id_fkey;
alter table app_user
    add constraint app_user_created_by_owner_id_fkey
        foreign key (created_by_owner_id) references app_user (id) on delete set null;
