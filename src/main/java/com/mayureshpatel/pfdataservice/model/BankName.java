package com.mayureshpatel.pfdataservice.model;

public enum BankName {
    CAPITAL_ONE,
    DISCOVER,
    SYNOVUS,
    STANDARD;

    public static BankName fromString(String text) {
        for (BankName b : BankName.values()) {
            if (b.name().equalsIgnoreCase(text)) {
                return b;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }
}
