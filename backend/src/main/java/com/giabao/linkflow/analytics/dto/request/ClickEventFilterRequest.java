package com.giabao.linkflow.analytics.dto.request;

import com.giabao.linkflow.common.dto.PageableRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class ClickEventFilterRequest extends PageableRequest {
    public static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "clickedAt",
            "createdAt"
    );
    public static final String DEFAULT_SORT_BY = "clickedAt";
}
