-- An account now carries its role, so an owner or administrator can never be
-- managed as somebody's assistant, and the owner who created an assistant, so
-- only they may reset its password.

alter table app_user add column role varchar(20);

-- Every account linked as an assistant becomes one; everything else is an owner,
-- which is how a role-less account was already treated. The single administrator
-- is not linked anywhere, so it reads as an owner here, which only protects it.
update app_user set role = 'ASSISTANT'
    where id in (select assistant_id from assistant_assignment);
update app_user set role = 'OWNER' where role is null;

alter table app_user alter column role set not null;

alter table app_user
    add column created_by_owner_id uuid references app_user (id);
