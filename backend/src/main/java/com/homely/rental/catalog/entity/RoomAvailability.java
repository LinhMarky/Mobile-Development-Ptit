package com.homely.rental.catalog.entity;

/**
 * Room availability status.
 * AVAILABLE → HELD (booking approved) → RENTED (booking confirmed/completed)
 */
public enum RoomAvailability {
    AVAILABLE,
    HELD,
    RENTED
}
