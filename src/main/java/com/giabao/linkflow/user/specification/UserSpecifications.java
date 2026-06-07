package com.giabao.linkflow.user.specification;

import com.giabao.linkflow.common.specification.CommonSpecifications;
import com.giabao.linkflow.user.dto.req.UserFilterRequest;
import com.giabao.linkflow.user.enity.User;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {
    private UserSpecifications() {
    }

    public static Specification<User> byFilter(UserFilterRequest filter) {
        return CommonSpecifications.allOf(
                CommonSpecifications.containsIgnoreCase("email", filter.getEmail()),
                CommonSpecifications.containsIgnoreCase("fullName", filter.getFullName()),
                CommonSpecifications.equal("role", filter.getRole())
        );
    }
}
