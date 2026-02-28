package com.mayureshpatel.pfdataservice.domain.merchant;

import com.mayureshpatel.pfdataservice.domain.TimestampAudit;
import com.mayureshpatel.pfdataservice.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {

    private Long id;
    @ToString.Exclude
    private User user;
    private String originalName;
    private String cleanName;

    @ToString.Exclude
    private TimestampAudit audit;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Merchant merchant = (Merchant) o;
        return id != null && id.equals(merchant.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
