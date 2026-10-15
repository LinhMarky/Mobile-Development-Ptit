package com.example.roomly;

import com.example.roomly.data.model.AdminListing;
import com.example.roomly.data.model.HostListing;
import org.junit.Test;
import static org.junit.Assert.*;

public class ListingStatusTest {
    @Test public void bothRolesKeepEveryServerStatus() {
        String[] values = {"DRAFT", "PENDING_REVIEW", "PUBLISHED", "HIDDEN", "REJECTED", "SUSPENDED", "EXPIRED"};
        for (String value : values) {
            assertEquals(value, HostListing.Status.valueOf(value).name());
            assertEquals(value, AdminListing.Status.valueOf(value).name());
        }
        assertNotEquals(HostListing.Status.HIDDEN, HostListing.Status.SUSPENDED);
        assertNotEquals(HostListing.Status.HIDDEN, HostListing.Status.EXPIRED);
    }

    @Test public void suspensionCannotBeEditedSubmittedOrHiddenByHost() {
        HostListing.Status status = HostListing.Status.SUSPENDED;
        assertFalse(status.canEdit());
        assertFalse(status.canSubmit());
        assertFalse(status.canHide());
    }

    @Test public void pendingReviewRemainsReadOnly() {
        HostListing.Status status = HostListing.Status.PENDING_REVIEW;
        assertFalse(status.canEdit());
        assertFalse(status.canSubmit());
        assertFalse(status.canHide());
    }

    @Test public void expiredAndHiddenListingsCanReturnForReview() {
        for (HostListing.Status status : new HostListing.Status[] {HostListing.Status.EXPIRED, HostListing.Status.HIDDEN}) {
            assertTrue(status.canEdit());
            assertTrue(status.canSubmit());
            assertFalse(status.canHide());
        }
    }

    @Test public void publicListingRequiresReviewAfterEditing() {
        HostListing.Status status = HostListing.Status.PUBLISHED;
        assertTrue(status.canEdit());
        assertFalse(status.canSubmit());
        assertTrue(status.canHide());
    }
}
