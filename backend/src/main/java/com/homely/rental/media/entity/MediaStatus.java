package com.homely.rental.media.entity;

/**
 * Lifecycle status of a media file (§4 Enum, BR-09).
 * READY → uploaded but not attached to any resource.
 * ATTACHED → linked to a Room/Listing/etc.
 * DELETED → soft-deleted (storage may still exist during retention).
 */
public enum MediaStatus {
    READY,
    ATTACHED,
    DELETED
}
