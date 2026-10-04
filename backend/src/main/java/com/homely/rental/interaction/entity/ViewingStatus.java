package com.homely.rental.interaction.entity;

/**
 * Viewing status (§4 Enum, BR-05).
 */
public enum ViewingStatus {
    REQUESTED,
    CONFIRMED,
    COMPLETED,
    CANCELLED_BY_TENANT,
    CANCELLED_BY_HOST,
    NO_SHOW
}
