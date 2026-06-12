package com.giabao.linkflow.common.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

@Getter
@Setter
public class PageableRequest {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT_BY = "createdAt";
    private static final String DESC_DIRECTION = "desc";

    private Integer page = DEFAULT_PAGE;
    private Integer size = DEFAULT_SIZE;
    private String sortBy = DEFAULT_SORT_BY;
    private String sortDirection = DESC_DIRECTION;

    public Pageable toPageable() {
        return toPageable(Set.of(DEFAULT_SORT_BY), DEFAULT_SORT_BY);
    }

    public Pageable toPageable(Set<String> allowedSortFields, String defaultSortBy) {
        return PageRequest.of(
                getPage() - 1,
                getSize(),
                Sort.by(resolveDirection(), resolveSortBy(allowedSortFields, defaultSortBy))
        );
    }

    public int getPage() {
        if (page == null || page < DEFAULT_PAGE) {
            return DEFAULT_PAGE;
        }

        return page;
    }

    public int getSize() {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }

        return Math.min(size, MAX_SIZE);
    }

    private Sort.Direction resolveDirection() {
        if ("asc".equalsIgnoreCase(sortDirection)) {
            return Sort.Direction.ASC;
        }

        return Sort.Direction.DESC;
    }

    private String resolveSortBy(Set<String> allowedSortFields, String defaultSortBy) {
        String fallback = hasText(defaultSortBy) ? defaultSortBy.trim() : DEFAULT_SORT_BY;
        if (!hasText(sortBy)) {
            return fallback;
        }

        String candidate = sortBy.trim();
        if (allowedSortFields == null || allowedSortFields.isEmpty() || allowedSortFields.contains(candidate)) {
            return candidate;
        }

        return fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
