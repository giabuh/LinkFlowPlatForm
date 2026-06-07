package com.giabao.linkflow.user.dto.req;

import com.giabao.linkflow.common.dto.PageableRequest;
import com.giabao.linkflow.user.enity.UserRole;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UserFilterRequest extends PageableRequest {
    public static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt",
            "updatedAt",
            "email",
            "fullName",
            "role"
    );
    public static final String DEFAULT_SORT_BY = "createdAt";

    private String email;
    private String fullName;
    private UserRole role;
}
