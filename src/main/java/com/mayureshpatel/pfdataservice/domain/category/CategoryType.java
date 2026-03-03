package com.mayureshpatel.pfdataservice.domain.category;

public enum CategoryType {
    INCOME,
    EXPENSE,
    BOTH,
    TRANSFER;

    /**
     * Gets the category type from the type string.
     *
     * @param value the type string
     * @return the category type
     */
    public static CategoryType fromValue(String value) {
        for (CategoryType type : CategoryType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid category type: " + value);
    }
}
