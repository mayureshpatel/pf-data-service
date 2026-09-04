package com.mayureshpatel.pfdataservice.domain.category;

/**
 * How a {@link CategoryRule}'s keyword set combines when matching a transaction description
 * (PF-315). {@code OR} matches today's original single-keyword behavior exactly when a rule has
 * only one keyword.
 */
public enum MatchType {
    AND,
    OR
}
