-- Issued invoices and recorded expenses feed the profit and loss statement used
-- for tax, so deleting a tenant, apartment or building must never take them
-- along. They used to go with it on delete cascade, whatever the application
-- allowed. The services now refuse such a delete and remove draft invoices
-- themselves; here the database refuses it too, whatever the code path.

alter table invoice drop constraint invoice_apartment_id_fkey;
alter table invoice
    add constraint invoice_apartment_id_fkey
        foreign key (apartment_id) references apartment (id) on delete restrict;

alter table invoice drop constraint invoice_tenant_id_fkey;
alter table invoice
    add constraint invoice_tenant_id_fkey
        foreign key (tenant_id) references tenant (id) on delete restrict;

alter table expense drop constraint expense_building_id_fkey;
alter table expense
    add constraint expense_building_id_fkey
        foreign key (building_id) references building (id) on delete restrict;
