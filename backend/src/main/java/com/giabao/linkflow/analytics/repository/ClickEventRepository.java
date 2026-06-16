package com.giabao.linkflow.analytics.repository;

import com.giabao.linkflow.analytics.dto.response.ClickSummaryResponse;
import com.giabao.linkflow.analytics.entity.ClickEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, UUID> {

    Page<ClickEvent> findByLinkId(UUID linkId, Pageable pageable);

    @Query("""
            select new com.giabao.linkflow.analytics.dto.response.ClickSummaryResponse(
                   count(c), count(distinct c.ipHash), max(c.clickedAt))
            from ClickEvent c
            where c.link.id = :linkId
            """)
    ClickSummaryResponse summarize(@Param("linkId") UUID linkId);
}
