package com.mayureshpatel.pfdataservice.domain.user;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    @EqualsAndHashCode.Include
    private Long id;
    private String username;
    private String passwordHash;
    private String email;

    @ToString.Exclude
    private TableAudit audit;
}
