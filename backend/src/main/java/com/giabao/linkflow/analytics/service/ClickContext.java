package com.giabao.linkflow.analytics.service;

/**
 * Request metadata captured at redirect time and handed to {@link ClickTrackingService}.
 * Kept transport-agnostic so the future Kafka pipeline can carry the same shape.
 */
public record ClickContext(String ipAddress, String userAgent, String referer) {
}
