package com.giabao.linkflow.common.specification;

import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class CommonSpecifications {
    private CommonSpecifications() {
    }

    @SafeVarargs
    public static <T> Specification<T> allOf(Specification<T>... specifications) {
        Specification<T> result = alwaysTrue();
        for (Specification<T> specification : specifications) {
            result = result.and(specification);
        }

        return result;
    }

    public static <T> Specification<T> containsIgnoreCase(String field, String value) {
        return (root, query, criteriaBuilder) -> {
            if (!hasText(value)) {
                return criteriaBuilder.conjunction();
            }

            String pattern = "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
            return criteriaBuilder.like(criteriaBuilder.lower(root.get(field)), pattern);
        };
    }

    public static <T> Specification<T> equal(String field, Object value) {
        return (root, query, criteriaBuilder) -> {
            if (value == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(root.get(field), value);
        };
    }

    private static <T> Specification<T> alwaysTrue() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
