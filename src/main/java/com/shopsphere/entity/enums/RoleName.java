package com.shopsphere.entity.enums;

/**
 * Enumerates the fixed set of roles recognized by the system.
 * Persisted as the "name" column on the roles table (seeded via insert.sql).
 */
public enum RoleName {
    ROLE_ADMIN,
    ROLE_SELLER,
    ROLE_CUSTOMER
}
