package com.bms.access;

import java.util.EnumSet;
import java.util.Set;

import com.bms.common.BaseEntity;
import com.bms.user.AppUser;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "assistant_assignment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "assistant_id"}))
public class AssistantAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private AppUser owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assistant_id", nullable = false, updatable = false)
    private AppUser assistant;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "assistant_permission",
            joinColumns = @JoinColumn(name = "assignment_id"))
    @Column(name = "permission", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Permission> permissions = EnumSet.noneOf(Permission.class);

    protected AssistantAssignment() {
        // for JPA
    }

    public AssistantAssignment(AppUser owner, AppUser assistant, Set<Permission> permissions) {
        this.owner = owner;
        this.assistant = assistant;
        replacePermissions(permissions);
    }

    public AppUser getOwner() {
        return owner;
    }

    public AppUser getAssistant() {
        return assistant;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void replacePermissions(Set<Permission> updated) {
        this.permissions = updated.isEmpty() ? EnumSet.noneOf(Permission.class) : EnumSet.copyOf(updated);
    }
}
