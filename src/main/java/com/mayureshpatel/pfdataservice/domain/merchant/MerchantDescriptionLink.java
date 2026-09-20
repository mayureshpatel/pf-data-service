package com.mayureshpatel.pfdataservice.domain.merchant;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MerchantDescriptionLink {

    @EqualsAndHashCode.Include
    private Long id;
    private Long userId;
    private Long merchantId;
    private String description;
    private String normalizedDescription;

    @ToString.Exclude
    private TableAudit audit;
}
