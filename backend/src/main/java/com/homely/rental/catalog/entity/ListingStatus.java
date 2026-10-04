package com.homely.rental.catalog.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Listing lifecycle status (BR-02).
 *
 * State machine:
 *   DRAFT ──submit──→ PENDING_REVIEW
 *   PENDING_REVIEW ──approve──→ PUBLISHED
 *   PENDING_REVIEW ──reject──→ REJECTED
 *   PUBLISHED ──hide──→ HIDDEN
 *   PUBLISHED ──expire(job)──→ EXPIRED
 *   HIDDEN ──submit──→ PENDING_REVIEW
 *   REJECTED ──submit──→ PENDING_REVIEW (after edit)
 *   EXPIRED ──submit──→ PENDING_REVIEW (after edit)
 *   Any except ARCHIVED ──archive──→ ARCHIVED (if no active booking)
 *
 * Editable statuses: DRAFT, HIDDEN, REJECTED, EXPIRED
 * Submittable statuses: DRAFT, HIDDEN, REJECTED, EXPIRED
 */
public enum ListingStatus {
    DRAFT,
    PENDING_REVIEW,
    REJECTED,
    PUBLISHED,
    HIDDEN,
    SUSPENDED,
    EXPIRED,
    ARCHIVED;

    /** Statuses from which the listing can be edited by the host. */
    public static final Set<ListingStatus> EDITABLE = EnumSet.of(DRAFT, HIDDEN, REJECTED, EXPIRED);

    /** Statuses from which the listing can be submitted for review. */
    public static final Set<ListingStatus> SUBMITTABLE = EnumSet.of(DRAFT, HIDDEN, REJECTED, EXPIRED);
}
