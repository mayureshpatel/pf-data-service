package com.mayureshpatel.pfdataservice.domain.bank;

import lombok.Getter;

// decision (2026-09-03 interview, PF-310): an enum-per-bank is the deliberate design at the
// current scale (5 banks), not an oversight. a lookup/registration mechanism is real effort
// better spent if and when a 6th bank is actually requested, not preemptively.
@Getter
public enum BankName {
    CAPITAL_ONE("Capital One"),
    DISCOVER("Discover"),
    SYNOVUS("Synovus"),
    STANDARD("Standard CSV"),
    UNIVERSAL("Universal CSV");

    private final String displayName;

    BankName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Finds enum constant by the display name.
     *
     * @param text the display name to search for
     * @return the corresponding enum constant
     * @throws IllegalArgumentException if no matching enum constant is found
     */
    public static BankName fromString(String text) {
        for (BankName b : BankName.values()) {
            if (b.name().equalsIgnoreCase(text) || b.displayName.equalsIgnoreCase(text)) {
                return b;
            }
        }
        throw new IllegalArgumentException("No enum constant for string: " + text);
    }
}