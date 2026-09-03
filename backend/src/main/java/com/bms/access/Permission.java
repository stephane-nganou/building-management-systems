package com.bms.access;

/**
 * Fine grained capabilities an owner can delegate to an assistant.
 * Owners implicitly hold every permission on their own data.
 */
public enum Permission {
    BUILDING_READ,
    BUILDING_WRITE,
    APARTMENT_READ,
    APARTMENT_WRITE,
    TENANT_READ,
    TENANT_WRITE,
    EXPENSE_READ,
    EXPENSE_WRITE,
    INVOICE_READ,
    INVOICE_WRITE,
    REPORT_READ
}
