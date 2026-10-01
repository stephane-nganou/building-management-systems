package com.bms.user;

/**
 * What an account is, kept on the local record so the application can tell an
 * assistant from an owner or an administrator without another round trip to
 * Keycloak. An owner or administrator is never managed as someone's assistant.
 */
public enum AccountRole {
    OWNER,
    ASSISTANT,
    ADMIN,
    /** Signed in, but the realm gives it no role: it owns nothing and assists no one. */
    NONE
}
