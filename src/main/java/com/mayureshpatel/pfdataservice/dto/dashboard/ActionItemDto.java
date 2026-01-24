package com.mayureshpatel.pfdataservice.dto.dashboard;

public record ActionItemDto(
    ActionType type,
    long count,
    String message,
    String route // Optional: for frontend navigation
) {
    public enum ActionType {
        TRANSFER_REVIEW,
        UNCATEGORIZED,
        STALE_DATA
    }
}
