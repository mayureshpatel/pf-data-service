package com.mayureshpatel.pfdataservice.domain.user;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * An application user -- the owner of every other piece of financial data in the system (accounts,
 * transactions, budgets, etc.), all of which are scoped to a {@code userId}. Also the principal
 * Spring Security authenticates against; see {@link com.mayureshpatel.pfdataservice.security.CustomUserDetails}
 * for how this maps onto Spring's own user model.
 */
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    @EqualsAndHashCode.Include
    private Long id;
    private String username;
    @ToString.Exclude
    private String passwordHash;
    private String email;
    private String role;

    @ToString.Exclude
    private TableAudit audit;
}
