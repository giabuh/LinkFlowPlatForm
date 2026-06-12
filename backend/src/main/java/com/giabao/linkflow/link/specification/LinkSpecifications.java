package com.giabao.linkflow.link.specification;

import com.giabao.linkflow.common.specification.CommonSpecifications;
import com.giabao.linkflow.link.dto.request.LinkFilterRequest;
import com.giabao.linkflow.link.entity.Link;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class LinkSpecifications {
    private LinkSpecifications() {
    }

    public static Specification<Link> byFilter(UUID ownerId, LinkFilterRequest filter) {
        return CommonSpecifications.allOf(
                ownedBy(ownerId),
                CommonSpecifications.containsIgnoreCase("title", filter.getTitle()),
                CommonSpecifications.equal("active", filter.getActive())
        );
    }

    private static Specification<Link> ownedBy(UUID ownerId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("user").get("id"), ownerId);
    }
}
