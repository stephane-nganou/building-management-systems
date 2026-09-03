package com.bms.access.dto;

import java.util.Set;

import com.bms.access.Permission;
import jakarta.validation.constraints.NotNull;

/** Replaces what an existing assistant is allowed to do. */
public record PermissionsRequest(@NotNull Set<Permission> permissions) {
}
