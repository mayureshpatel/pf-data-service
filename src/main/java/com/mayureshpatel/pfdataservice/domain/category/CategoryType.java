package com.mayureshpatel.pfdataservice.domain.category;

public enum CategoryType {
    INCOME,
    EXPENSE,
    TRANSFER;

    public static CategoryType fromValue(String value) {
        for (CategoryType type : CategoryType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid category type: " + value);
    }
}
