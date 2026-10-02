package com.mayureshpatel.pfdataservice.repository;

import lombok.NoArgsConstructor;

/**
 * Shared Spring JDBC Client named-parameter names (PF-808). {@code "userId"} alone was
 * independently duplicated as a raw string literal across 9 repository classes -- every
 * ownership-scoped query in this codebase binds it -- so it lives here once rather than as 9
 * near-identical private constants that could quietly drift apart (a typo'd key silently binds
 * nothing and fails at runtime, not at compile time).
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class SqlParams {

    /** The Spring JDBC Client named parameter for the owning user's id. */
    public static final String USER_ID = "userId";
}
