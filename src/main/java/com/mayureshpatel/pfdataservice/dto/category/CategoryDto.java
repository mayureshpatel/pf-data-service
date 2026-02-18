package com.mayureshpatel.pfdataservice.dto.category;

import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.user.User;

public record CategoryDto(
        Long id,
        User user,
        String name,
        Category parent,
        Iconography iconography
) {
}
