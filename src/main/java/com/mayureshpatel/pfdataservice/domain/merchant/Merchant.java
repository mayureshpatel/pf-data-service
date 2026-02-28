package com.mayureshpatel.pfdataservice.domain.merchant;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.merchant.MerchantDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {

    private Long id;
    private User user;
    private String originalName;
    private String name;

    private TableAudit audit;

    public MerchantDto toDto() {
        return new MerchantDto(id, user.getId(), originalName, name);
    }
}
