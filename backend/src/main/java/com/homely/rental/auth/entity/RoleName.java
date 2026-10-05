package com.homely.rental.auth.entity;

/**
 * Supported user roles (SEC-01).
 * TENANT: default role for all users.
 * HOST: requires activation, can manage rooms and listings.
 * ADMIN: internal staff, can approve listings and manage system.
 */
public enum RoleName {
    ROLE_TENANT,
    ROLE_HOST,
    ROLE_ADMIN
}
