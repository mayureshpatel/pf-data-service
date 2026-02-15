package com.mayureshpatel.pfdataservice.domain.vendor;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorRule {

    private Long id;
    private String keyword;
    private Vendor vendor;
    private Integer priority;
    private User user;

    private TableAudit audit;
}
