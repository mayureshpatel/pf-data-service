package com.mayureshpatel.pfdataservice.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A reusable icon-and-color pair for entities that need visual presentation metadata (e.g. a
 * category or account type badge). Not currently composed into any domain type -- those model
 * {@code icon}/{@code color} as their own direct fields instead -- kept as a shared shape for
 * whenever a second consumer actually needs one, rather than duplicating it again.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Iconography {

    private String icon;
    private String color;
}
