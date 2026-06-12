package com.giabao.linkflow.link.dto.request;

import com.giabao.linkflow.common.dto.PageableRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class LinkFilterRequest extends PageableRequest {
    public static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt",
            "updatedAt",
            "clickCount",
            "title"
    );
    public static final String DEFAULT_SORT_BY = "createdAt";

    private String title;
    private Boolean active;
}
