package com.bms.user;

import com.bms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    protected AppUser() {
        // for JPA
    }

    public AppUser(String keycloakId, String email, String firstName, String lastName) {
        this(keycloakId, email, firstName, lastName, false);
    }

    public AppUser(String keycloakId, String email, String firstName, String lastName,
                   boolean mustChangePassword) {
        this.keycloakId = keycloakId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
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

    public void updateProfile(String email, String firstName, String lastName) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
