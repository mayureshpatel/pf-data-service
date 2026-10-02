package com.mayureshpatel.pfdataservice.domain.category;

/**
 * The kind of transaction a {@code Category} is meant to classify. {@code BOTH} lets a single
 * category (e.g. a shared "Reimbursable" bucket) apply to income and expense transactions alike,
 * and {@code TRANSFER} exists for categories specific to inter-account transfers.
 */
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
