package com.giabao.linkflow.link.entity;

import com.giabao.linkflow.common.BaseEntity;
import com.giabao.linkflow.user.enity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "links")
public class Link extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "short_code", nullable = false, unique = true, length = 32)
    private String shortCode;

    @Column(name = "title")
    private String title;

    @Column(name = "click_count", nullable = false)
    private Long clickCount = 0L;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
