package com.mayureshpatel.pfdataservice.dto.dashboard;

/**
 * Represents an action item displayed on the dashboard.
 *
 * @param type    action item type
 * @param count   action item count
 * @param message action item message
 * @param route   action item route
 */
public record ActionItemDto(
        ActionType type,
        long count,
        String message,
        String route
) {
    /** The kind of dashboard action item being surfaced, one per distinct nudge the UI can show. */
    public enum ActionType {
        TRANSFER_REVIEW,
        UNCATEGORIZED,
        STALE_DATA
    }
}
