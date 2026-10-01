package com.bms.user;

import java.util.UUID;

import com.bms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class AppUser extends BaseEntity {

    /** The Keycloak subject claim. Stable identity across email or name changes. */
    @Column(name = "keycloak_id", nullable = false, unique = true, updatable = false)
    private String keycloakId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    /**
     * Set when an owner creates this account, or issues it a new password. The
     * account works normally until it is cleared, but the application shows its
     * holder nothing else until they have chosen a password of their own.
     */
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    /** Set by an administrator. The account is refused everywhere until it is cleared. */
    @Column(name = "suspended", nullable = false)
    private boolean suspended;

    /**
     * What this account is. An owner or administrator is never managed as an
     * assistant, so it can neither be linked as one nor have its password reset
     * by one. Defaults to owner, which is how a role-less account is treated.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private AccountRole role = AccountRole.OWNER;

    /** The owner who created this assistant, and the only one who may reset its password. */
    @Column(name = "created_by_owner_id")
    private UUID createdByOwnerId;

    protected AppUser() {
        // for JPA
    }

    public AppUser(String keycloakId, String email, String firstName, String lastName) {
        this(keycloakId, email, firstName, lastName, false);
    }

    public AppUser(String keycloakId, String email, String firstName, String lastName,
                   boolean mustChangePassword) {
        this(keycloakId, email, firstName, lastName, AccountRole.OWNER, mustChangePassword);
    }

    public AppUser(String keycloakId, String email, String firstName, String lastName,
                   AccountRole role, boolean mustChangePassword) {
        this.keycloakId = keycloakId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.mustChangePassword = mustChangePassword;
    }

    public String getKeycloakId() {
        return keycloakId;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        if (firstName == null && lastName == null) {
            return email;
        }
        return String.join(" ", firstName == null ? "" : firstName, lastName == null ? "" : lastName).trim();
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    /** Called when this account is handed a password somebody else chose. */
    public void requirePasswordChange() {
        this.mustChangePassword = true;
    }

    /** Called once the holder has set a password only they know. */
    public void passwordChosen() {
        this.mustChangePassword = false;
    }

    public AccountRole getRole() {
        return role;
    }

    public boolean isAssistant() {
        return role == AccountRole.ASSISTANT;
    }

    /** Keeps the local role in step with the realm when the account signs in. */
    public void assignRole(AccountRole role) {
        this.role = role;
    }

    public UUID getCreatedByOwnerId() {
        return createdByOwnerId;
    }

    /** Records which owner created this assistant; only they may reset its password. */
    public void createdBy(UUID ownerId) {
        this.createdByOwnerId = ownerId;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void suspend() {
        this.suspended = true;
    }

    public void reactivate() {
        this.suspended = false;
    }

    public void updateProfile(String email, String firstName, String lastName) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
