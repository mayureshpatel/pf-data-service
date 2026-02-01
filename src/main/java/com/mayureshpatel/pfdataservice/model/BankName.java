package com.mayureshpatel.pfdataservice.model;

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

    public String getDisplayName() {
        return displayName;
    }

    public static BankName fromString(String text) {
        for (BankName b : BankName.values()) {
            if (b.name().equalsIgnoreCase(text) || b.displayName.equalsIgnoreCase(text)) {
                return b;
            }
        }
        throw new IllegalArgumentException("No enum constant for string: " + text);
    }
}