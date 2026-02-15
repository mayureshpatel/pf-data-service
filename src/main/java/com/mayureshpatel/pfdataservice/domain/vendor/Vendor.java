package com.mayureshpatel.pfdataservice.domain.vendor;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vendor {

    private Long id;
    private String name;
    private Set<String> aliases;

    private TableAudit audit;
}
