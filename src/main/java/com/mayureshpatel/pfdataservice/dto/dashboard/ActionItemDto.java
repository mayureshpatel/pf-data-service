package com.mayureshpatel.pfdataservice.dto.dashboard;

/**
 * Represents an action item displayed on the dashboard.
 * @param type action item type
 * @param count action item count
 * @param message action item message
 * @param route action item route
 */
public record ActionItemDto(
    ActionType type,
    long count,
    String message,
    String route
) {
    public enum ActionType {
        TRANSFER_REVIEW,
        UNCATEGORIZED,
        STALE_DATA
    }
}
